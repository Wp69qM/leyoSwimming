> **OpenSpec Spec | 映射自 `docs/stories/US-066-系统-通过MCP暴露知识库检索工具/user-story.md` §6**

## Capability

通过 MCP 协议向外部 Agent 客户端暴露知识库检索工具

## ADDED Requirements

### Requirement: REQ-001 MCP 握手与工具发现

系统 MUST 在 `/mcp-server/mcp` 提供 MCP Streamable HTTP endpoint，外部客户端携带有效 `MCP_API_TOKEN` 可完成 initialize 握手并通过 tools/list 获得 `query_knowledge` 工具清单（含 description 与参数 schema）。

#### Scenario: MCP 握手与工具发现

- **GIVEN** ai-service 运行中且已配置 MCP_API_TOKEN
- **WHEN** 客户端携带有效 token 发起 initialize 与 tools/list
- **THEN** 握手成功且返回工具清单
- **AND** 清单包含 query_knowledge
- **AND** 工具 description 说明用途与返回结构

### Requirement: REQ-002 知识检索返回带来源的片段

系统 MUST 在 `query_knowledge` 工具调用时复用 `KnowledgeService.query()` 检索 Chroma 并应用 `KNOWLEDGE_SIMILARITY_THRESHOLD` 阈值，返回包含 content/source/source_type/category/score 的结构化片段。

#### Scenario: 知识检索返回带来源的片段

- **GIVEN** 知识库存在启用的"野泳安全指南"文档
- **WHEN** 客户端调用 tools/call query_knowledge(query="野泳的注意事项")
- **THEN** 返回 HTTP 200
- **AND** 片段数组非空且每条包含 content/source/source_type/category/score
- **AND** source 为来源文档标题

#### Scenario: 知识库无匹配

- **GIVEN** 知识库中无与 query 相关的启用文档
- **WHEN** 客户端调用 query_knowledge(query="\<无关问题\>")
- **THEN** 返回 HTTP 200
- **AND** 片段数组为空

### Requirement: REQ-003 MCP 独立 token 鉴权

系统 MUST 对 MCP endpoint 独立校验 `Authorization: Bearer <MCP_API_TOKEN>`，无效或缺失时拒绝（HTTP 401）且不执行工具逻辑；`MCP_API_TOKEN` 与 `INTERNAL_API_TOKEN` 相互独立。

#### Scenario: 无 token 请求被拒

- **GIVEN** 客户端未携带 Authorization header
- **WHEN** 发起 initialize 或 tools/call
- **THEN** 返回 HTTP 401
- **AND** 不执行任何工具逻辑

### Requirement: REQ-004 既有 REST 链路零回归

系统 MUST 保持既有 REST 链路（`/api/ai-assistant/*` 与 `/api/internal/ai/*`）在 MCP 挂载后行为不变。

#### Scenario: 既有 REST 链路不受影响

- **GIVEN** MCP endpoint 已挂载
- **WHEN** backend 按 INTERNAL_API_TOKEN 调用 /api/internal/ai/knowledge/ingest
- **AND** 小程序调用 /api/ai-assistant/chat
- **THEN** 两条既有链路行为与挂载前完全一致

### Requirement: REQ-005 参数与限流防护

系统 MUST 将 `top_k` 截断至 10，并保持 `RateLimitMiddleware` 对 MCP 路径的限流覆盖；`MCP_API_TOKEN` 长度 < 32 时服务启动失败。

#### Scenario: top_k 超限被截断

- **GIVEN** 客户端传 top_k=100
- **WHEN** 调用 query_knowledge
- **THEN** 实际检索条数 ≤ 10

#### Scenario: 限流触发

- **GIVEN** 同一 IP 高频调用 MCP 工具
- **WHEN** 超过限流阈值
- **THEN** 返回 HTTP 429
