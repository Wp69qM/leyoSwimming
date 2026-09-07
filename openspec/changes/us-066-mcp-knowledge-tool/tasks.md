> **OpenSpec Tasks | 映射自 `docs/stories/US-066-系统-通过MCP暴露知识库检索工具/test-plan.md`**
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: 安装 MCP SDK 并新增配置 [P0]

**Files:**
- Update: `ai-service/requirements.txt`
- Update: `ai-service/app/config.py`
- Update: `ai-service/.env.example`
- Test: `ai-service/tests/test_config.py`

**Spec coverage:** REQ-003 / REQ-005

- [x] **RED:** Write failing tests — `mcp_api_token` 默认为空、`mcp_top_k_max` 默认 10
- [x] **GREEN:** Install `mcp` SDK（锁定 minor 版本）；config 新增 `mcp_api_token` / `mcp_top_k_max`
- [x] **COMMIT:** `chore(ai-service): add mcp sdk and config`

## Task 2: MCP 鉴权中间件与 InternalAuthMiddleware 放行 [P0]

**Files:**
- Create: `ai-service/app/mcp/auth.py`
- Update: `ai-service/app/middleware/auth.py`
- Update: `ai-service/app/main.py`（lifespan token 长度校验）
- Test: `ai-service/tests/mcp/test_auth.py`

**Spec coverage:** REQ-003 / REQ-004

- [x] **RED:** Write failing tests — 无 token 401、错误 token 401、有效 token 放行；`/mcp-server/**` 绕过 InternalAuthMiddleware；`MCP_API_TOKEN` < 32 启动报错
- [x] **GREEN:** 实现 Bearer 校验 ASGI 中间件；InternalAuthMiddleware 增加放行前缀；lifespan 增加校验
- [x] **COMMIT:** `feat(ai-service): add mcp token auth and middleware bypass`

## Task 3: FastMCP 挂载与 query_knowledge 工具 [P0]

**Files:**
- Create: `ai-service/app/mcp/server.py`
- Update: `ai-service/app/main.py`（mount）
- Test: `ai-service/tests/mcp/test_server.py`

**Spec coverage:** REQ-001 / REQ-002 / REQ-005

- [x] **RED:** Write failing tests — initialize/tools/list 含 query_knowledge；query_knowledge 返回结构化片段（mock KnowledgeService）；无匹配返回空数组；top_k=100 截断为 10
- [x] **GREEN:** 实现 FastMCP 实例 + `@mcp.tool() query_knowledge`（直调 KnowledgeService，阈值复用配置）；`streamable_http_app()` mount 到 `/mcp-server`
- [x] **COMMIT:** `feat(ai-service): mount fastmcp with query_knowledge tool`

## Task 4: 既有链路回归与限流验证 [P0]

**Files:**
- Test: `ai-service/tests/integration/test_mcp_regression.py`

**Spec coverage:** REQ-004 / REQ-005

- [x] **RED:** Write failing integration tests — `/api/ai-assistant/chat` 与 `/api/internal/ai/knowledge/ingest` 行为不变；MCP 路径超限返回 429
- [x] **GREEN:** 验证挂载后中间件顺序与限流覆盖正确（实现修正而非改测试）
- [x] **COMMIT:** `test(ai-service): mcp regression and rate limit coverage`

## Task 5: 验证

- [x] **5.1** Run `pytest ai-service/tests` and confirm coverage >= 80%
- [x] **5.2** 用 MCP SDK client 脚本完成 initialize → tools/list → tools/call 全链路冒烟
