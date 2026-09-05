## Context

本 US 在现有 AI 助理推荐能力基础上，为 `ai-service` 增加 RAG 知识检索能力。用户在 AI 助理页提出游泳安全、急救、训练等知识类问题时，系统调用 `query_knowledge` 工具检索 Chroma 向量知识库，并基于检索结果生成回答。

## Goals / Non-Goals

**Goals:**
- 知识类问题触发 `query_knowledge` 并生成基于检索结果的自然语言回答
- 推荐类问题不调用 `query_knowledge`，继续使用 `recommendation_tools`
- 仅检索 `status=0` 的启用文档
- 相似度阈值 0.7，低于阈值视为无匹配
- 复用 `POST /api/ai-assistant/chat`，不破坏现有对话能力

**Non-Goals:**
- 不实现联网搜索兜底（由 US-065 负责）
- 不实现知识库文档上传/管理（由 US-062 负责）
- 不新增用户端页面或前端组件

## Data Model

### 复用/读取的表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `ai_chat_session` | 读/写 | `id`, `user_id`, `session_id`, `created_at`, `updated_at` |
| `ai_chat_message` | 新增 | `id`, `session_id`, `role`, `content`, `tool_calls`, `created_at` |
| `ai_knowledge_document` | 读取 | `id`, `title`, `category`, `status`（0=启用/1=禁用） |

### 向量存储

- Collection name: `knowledge_base`
- 每个 chunk 作为一条 Document，metadata 包含 `document_id`、`title`、`category`、`chunk_index`
- 检索时按 `document_id` 关联 `ai_knowledge_document.status=0` 做过滤

## API Design

### POST /api/ai-assistant/chat

- **鉴权**：游客或已登录用户 JWT（复用 US-058 鉴权）
- **请求体**：复用现有结构，例如 `{ "sessionId": "...", "message": "野泳的注意事项" }`
- **响应 200**：`ChatResponse` 结构不变，`reply.text` 中自然融入知识库内容
- **业务规则**：
  - LLM 判断意图为游泳知识类问题 → 调用 `query_knowledge`
  - LLM 判断意图为推荐类问题 → 调用现有 `recommendation_tools`
  - `query_knowledge` 仅返回 `score >= 0.7` 且来源文档 `status=0` 的知识片段

## Function Calling Tool Design

### `query_knowledge`

```python
@tool
async def query_knowledge(query: str, top_k: int = 3) -> list[dict]:
    """
    从游泳知识库中检索与用户问题相关的知识片段。
    当用户询问游泳安全、急救、训练知识等非业务问题时调用。
    """
    results = vector_store.similarity_search(query, top_k=top_k)
    return [
        {
            "content": doc.page_content,
            "source": doc.metadata["title"],
            "category": doc.metadata["category"],
            "score": score,
        }
        for doc, score in results
        if score >= 0.7
    ]
```

## Intent Classification Rules

| 意图类型 | 关键词示例 | 调用工具 |
|----------|-----------|---------|
| 游泳知识 | 安全、急救、技巧、健康、训练方法 | `query_knowledge` |
| 业务推荐 | 教练、课程、套餐、价格、报名 | `recommendation_tools` |

- LLM 负责最终意图判断
- 用户问题同时包含两类关键词时，由 LLM 根据上下文决定优先工具

## Performance

- `query_knowledge` 向量检索 P99 < 300ms
- 单轮对话端到端 P99 < 3s

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-058 | 被本 US 依赖 | 复用 AI 助理会话与 `POST /api/ai-assistant/chat` |
| US-062 | 被本 US 依赖 | 复用 `ai_knowledge_document` 元数据与 Chroma `knowledge_base` collection |
| US-065 | 反向依赖 | 本 US 中 `query_knowledge` 无匹配或失败时进入 US-065 联网搜索兜底 |
