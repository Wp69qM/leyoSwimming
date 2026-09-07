## Context

本 US 为 `ai-service` 建立 MCP 服务基础并暴露第一个工具 `query_knowledge`。MCP endpoint 与既有 REST API 同进程共存，共享 Service 层但互不影响。

## Goals / Non-Goals

**Goals:**

- FastMCP 挂载到现有 FastAPI app，Streamable HTTP 传输
- `query_knowledge` 工具直调 `KnowledgeService`，返回结构含 content/source/source_type/category/score
- `MCP_API_TOKEN` 独立鉴权（401 拒绝无效/缺失 token）
- `InternalAuthMiddleware` 放行 `/mcp-server/**`，既有链路零回归
- MCP 路径限流覆盖；`top_k` 截断至 10

**Non-Goals:**

- 不暴露推荐工具（US-067）、不做 Trae 注册验证（US-068）
- 不暴露 `web_search` / `ingest` / `chat`（需求文档 §2.2 排除项）
- 不做远程部署与 OAuth 2.1

## Data Model

无表变更。读取 Chroma `knowledge_base` collection（metadata 含 document_id/title/category/chunk_index）。

## API Design

### MCP endpoint `/mcp-server/mcp`

- **协议**：MCP Streamable HTTP（JSON-RPC：initialize / tools/list / tools/call；GET 仅用于协议内 SSE 流）
- **鉴权**：`Authorization: Bearer <MCP_API_TOKEN>`，独立于 `INTERNAL_API_TOKEN`
- **限流**：复用 `RateLimitMiddleware`（IP 维度）
- **工具 schema**：`query_knowledge(query: string, top_k?: int <= 10)`

## Key Design Decisions

| 决策 | 结论 | 理由 |
|------|------|------|
| 部署形态 | 同进程 mount | 单端口零运维增量，复用日志/配置 |
| 共享层 | 直调 KnowledgeService | LangChain @tool 闭包与 MCP 注册模型不兼容 |
| 传输 | Streamable HTTP | 远程可用，SSE 已 deprecated，stdio 不支持远程 |
| 阈值 | 复用 `KNOWLEDGE_SIMILARITY_THRESHOLD` | 两条协议链路口径一致 |

## Risks

- MCP SDK 版本迭代快：锁定 minor 版本；Trae 不兼容时降级 SSE（仅改挂载方式）
- lifespan 管理：FastMCP session manager 与主 app lifespan 合并，实现时以官方 SDK 文档为准
