## Why

ai-service 已具备 Chroma 向量知识库检索能力（US-062 ~ US-064），但仅在自身 chat 链路内可用。MCP（Model Context Protocol）是 AI 工具互操作的事实标准，Trae、Claude Desktop 等客户端均原生支持。将知识库检索能力通过 MCP 暴露后，任何 MCP 客户端的 Agent 都能直接复用，形成 AI 能力平台化入口与 dogfooding 闭环。

## What Changes

- `ai-service` 引入 MCP 官方 Python SDK（FastMCP），以 Streamable HTTP 传输挂载到现有 FastAPI app（`app.mount("/mcp-server", ...)`，同进程同端口）
- 新增 MCP `query_knowledge` 工具，直调 `KnowledgeService.query()`（复用 `KNOWLEDGE_SIMILARITY_THRESHOLD` 阈值），返回与内部 LangChain tool 一致的结构
- 新增 `MCP_API_TOKEN`（≥32 位）独立鉴权：`InternalAuthMiddleware` 放行 `/mcp-server/**`，MCP 层校验 `Authorization: Bearer` header
- MCP 路径保持 `RateLimitMiddleware` 限流覆盖；`top_k` 上限 10
- 既有 REST 链路（`/api/ai-assistant/*`、`/api/internal/ai/*`）行为不变

## Capabilities

### New Capabilities

- `mcp-knowledge-tool`: 通过 MCP 协议向外部 Agent 客户端暴露知识库检索工具

### Modified Capabilities

- 无

## Impact

- **数据表**：无新增/修改；读取 Chroma `knowledge_base` collection
- **API**：新增 MCP endpoint `/mcp-server/mcp`（JSON-RPC 协议，非 REST，不受 api-convention 三段式 URL 约束）；`InternalAuthMiddleware` 增加放行规则
- **状态机**：无
- **前端**：无
- **依赖**：US-062（知识库文档入库）、US-064（KnowledgeService 检索能力）
- **影响**：为 US-067（推荐工具暴露）提供 MCP 基础设施，为 US-068（Trae 注册验证）提供被验证对象
