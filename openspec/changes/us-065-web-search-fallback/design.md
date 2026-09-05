## Context

本 US 在 `ai-service` 内部实现联网搜索兜底能力。当 `query_knowledge` 返回空或最高相似度低于阈值时，`ChatService` 调用新增的 `web_search` function calling 工具，通过 Tavily Search API 获取搜索结果，并由 LLM 生成带来源标注的回答。 Tavily 调用失败、超时或无结果时均需优雅降级。

## Goals / Non-Goals

**Goals:**
- 当 `query_knowledge` 无有效匹配时自动触发 `web_search`
- 基于 Tavily 搜索结果生成回答并标注"来自网络"
- Tavily 失败、超时、无结果时返回友好降级提示
- 降级不抛出异常，不阻塞后续对话
- 搜索调用默认 5 秒超时，可配置

**Non-Goals:**
- 不新增后端管理端 API 或数据库表
- 不改变 AI 助理现有推荐类业务逻辑
- 不实现 Tavily 结果持久化或搜索历史
- 不实现多搜索引擎切换（MVP 仅 Tavily）

## Data Model

无新增或修改的数据表。

## API Design

本 US 不新增对外 HTTP API。新增的能力通过 `ai-service` 内部 function calling 工具暴露：

### `web_search` Function Calling Tool

```python
@tool
async def web_search(query: str, max_results: int = 3) -> list[dict] | dict:
    """
    当知识库没有相关内容时，联网搜索补充信息。

    Args:
        query: 用户原始问题或提炼后的搜索关键词。
        max_results: 最大返回结果数，默认 3。

    Returns:
        正常返回时：list[dict]，每个元素包含 title、content、url。
        异常返回时：{"error": "搜索服务暂时不可用"}。
    """
```

- **调用方**：`ai-service` 的 `ChatService`
- **触发条件**：`query_knowledge` 返回空或最高 score < `LEYO_KNOWLEDGE_SIMILARITY_THRESHOLD`（默认 0.7）
- **外部依赖**：Tavily Search API (`https://api.tavily.com/search`)
- **超时**：默认 5 秒

## Function Calling Decision Flow

```
用户提问
    │
    ▼
LLM 意图判断 → 游泳知识类问题
    │
    ▼
query_knowledge
    │
    ├── 最高 score >= 0.7 ──▶ 用知识库结果生成回答
    │
    └── 无结果或 score < 0.7 ──▶ web_search
                │
                ├── Tavily 返回有效结果 ──▶ LLM 生成回答 + 标注"来自网络"
                │
                ├── Tavily 失败/超时 ──▶ 返回"暂时无法获取该知识，请换个方式提问"
                │
                └── Tavily 无结果 ──▶ 返回"这个问题我暂时无法回答"
```

## Tavily Client Design

```python
import httpx

class TavilyClient:
    def __init__(self, api_key: str, base_url: str = "https://api.tavily.com"):
        self.api_key = api_key
        self.base_url = base_url

    async def search(self, query: str, max_results: int = 3) -> list[dict]:
        async with httpx.AsyncClient(timeout=5.0) as client:
            resp = await client.post(
                f"{self.base_url}/search",
                json={
                    "api_key": self.api_key,
                    "query": query,
                    "max_results": max_results,
                    "include_answer": True,
                },
            )
            resp.raise_for_status()
            data = resp.json()
            return data.get("results", [])
```

- 请求参数包含 `include_answer=True`，优先使用 Tavily 已清洗的摘要内容
- 返回结果统一清洗为 `{"title", "content", "url"}` 结构

## Configuration

新增 `ai-service` 环境变量：

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `LEYO_TAVILY_API_KEY` | Tavily API 密钥 | 必填 |
| `LEYO_TAVILY_MAX_RESULTS` | 单次搜索最大返回结果数 | 3 |
| `LEYO_TAVILY_TIMEOUT_SECONDS` | Tavily 调用超时（秒） | 5 |
| `LEYO_KNOWLEDGE_SIMILARITY_THRESHOLD` | 触发联网搜索的相似度阈值 | 0.7 |

## Performance

- `web_search` 端到端调用 P99 < 5.5 秒（含 5 秒 Tavily 超时 + 网络/处理开销）
- Tavily 超时后应立即返回降级提示，不阻塞后续对话

## Error Handling

| 场景 | 行为 |
|------|------|
| Tavily HTTP 错误/鉴权失败 | 返回 `{"error": "搜索服务暂时不可用"}`，最终提示"暂时无法获取该知识，请换个方式提问" |
| Tavily 调用超时 | 返回 `{"error": "搜索超时"}`，最终提示"搜索超时，请稍后再试" |
| Tavily 返回空结果 | 最终提示"这个问题我暂时无法回答" |
| Tavily API Key 未配置 | 启动时校验失败，记录错误日志；运行时跳过联网搜索直接降级 |

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-064 | 被本 US 依赖 | 提供 `query_knowledge` 工具与知识库检索触发条件 |
| US-062 | 被本 US 依赖 | 提供知识库文档上传与索引能力 |
