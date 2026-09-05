# Tech Design: US-065 系统-联网搜索兜底

## 1. 总体架构

```
ai-service ChatService
    │
    ├── query_knowledge 返回空或低相关
    │
    └── web_search 工具
            │
            ▼
        Tavily API
            │
            ▼
        LLM 生成回答（标注来自网络）
```

## 2. 新增文件

| 文件 | 说明 |
|------|------|
| `ai-service/app/clients/tavily_client.py` | Tavily API 客户端 |
| `ai-service/app/tools/knowledge_tools.py` | 新增 web_search 工具 |

## 3. Tavily 客户端

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

## 4. Function Calling 工具

```python
@tool
async def web_search(query: str, max_results: int = 3) -> list[dict]:
    try:
        results = await tavily_client.search(query, max_results)
        return [
            {"title": r["title"], "content": r["content"], "url": r["url"]}
            for r in results
        ]
    except Exception:
        return {"error": "搜索服务暂时不可用"}
```

## 5. 触发条件

在 `ChatService` 中：

```python
knowledge_results = await query_knowledge(query)
if not knowledge_results:
    search_results = await web_search(query)
    source_note = "以下内容来自网络，仅供参考"
```

## 6. 降级策略

- Tavily 失败 → 返回友好提示
- Tavily 超时 → 返回"搜索超时"
- 无搜索结果 → 返回"暂时无法回答"

## 7. 配置

```
LEYO_TAVILY_API_KEY=
LEYO_TAVILY_MAX_RESULTS=3
LEYO_TAVILY_TIMEOUT_SECONDS=5
```
