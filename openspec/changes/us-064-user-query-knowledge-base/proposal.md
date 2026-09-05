## Why

用户在 AI 助理页提问游泳安全、急救、训练方法等知识类问题时，现有推荐工具无法给出专业答案。通过在 `ai-service` 中接入 Chroma 向量知识库，AI 助理可基于后台维护的知识文档生成可信回答，提升可回答范围与用户信任度。

## What Changes

- `ai-service` 新增 `query_knowledge` Function Calling 工具，从 Chroma `knowledge_base` collection 检索相关知识片段
- `ai-service` 扩展 `ChatService` 的意图判断与工具注册，将游泳知识类问题路由到 `query_knowledge`
- 仅检索 `status=0` 的启用文档，相似度阈值 0.7，低于阈值进入 US-065 联网搜索兜底
- 推荐类问题（教练/课程/套餐）仍走现有 `recommendation_tools`，不调用 `query_knowledge`
- 复用现有 `POST /api/ai-assistant/chat`，不对 API 路径做破坏性变更

## Capabilities

### New Capabilities

- `user-query-knowledge-base`: 用户向 AI 助理提问时从向量知识库检索游泳知识

### Modified Capabilities

- 无

## Impact

- **数据表**：复用 `ai_chat_session`、`ai_chat_message` 表；读取 `ai_knowledge_document` 元数据；检索 Chroma `knowledge_base` collection
- **API**：复用 `POST /api/ai-assistant/chat`，`ChatResponse` 结构不变
- **状态机**：无
- **前端**：无改动，AI 助理页直接复用现有对话能力
- **依赖**：US-058（AI 助理会话）、US-062（知识库文档管理）
- **影响**：为 US-065（联网搜索兜底）奠定 RAG 检索失败后的 fallback 基础
