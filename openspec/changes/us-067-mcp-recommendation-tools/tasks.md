> **OpenSpec Tasks | 映射自 `docs/stories/US-067-系统-通过MCP暴露业务推荐查询工具/test-plan.md`**
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: 归一化函数提取共享 [P0]

**Files:**
- Create: `ai-service/app/tools/normalizers.py`
- Update: `ai-service/app/tools/recommendation_tools.py`
- Test: `ai-service/tests/tools/test_normalizers.py`

**Spec coverage:** REQ-001 / REQ-005

- [x] **RED:** Write failing tests — `normalize_stroke("蛙泳")="breaststroke"`、`normalize_gender("女")="female"`、`normalize_package_mode("体验")="experience"`、未知输入返回 None；LangChain 链路行为不变（回归）
- [x] **GREEN:** 从 `recommendation_tools.py` 原样迁移三个归一化函数到 `normalizers.py`，原文件改为引用
- [x] **COMMIT:** `refactor(ai-service): extract shared normalizers`

## Task 2: MCP 推荐工具注册 [P0]

**Files:**
- Update: `ai-service/app/mcp/server.py`
- Test: `ai-service/tests/mcp/test_recommendation_tools.py`

**Spec coverage:** REQ-001 / REQ-002 / REQ-004 / REQ-005

- [x] **RED:** Write failing tests — tools/list 含 3 个推荐工具且不含 get_user_profile/get_user_packages；query_coaches 中文归一化（mock JavaInternalClient 断言收到 breaststroke/female）；package_mode 归一化；limit=1000 截断为 20；未知泳姿归一化为 None
- [x] **GREEN:** 实现三个 `@mcp.tool()`（白名单显式注册，直调 JavaInternalClient + normalizers）
- [x] **COMMIT:** `feat(ai-service): add mcp recommendation tools`

## Task 3: backend 故障隔离 [P0]

**Files:**
- Test: `ai-service/tests/mcp/test_recommendation_tools.py`
- Test: `ai-service/tests/integration/test_mcp_tool_isolation.py`

**Spec coverage:** REQ-003

- [x] **RED:** Write failing tests — mock backend 不可达时 query_coaches 返回 `[{"error": ...}]` 不抛异常；同会话内 query_knowledge 不受影响；backend 超时返回结构化错误且连接保持
- [x] **GREEN:** 工具层 try/catch 统一转结构化错误（实现修正而非改测试）
- [x] **COMMIT:** `feat(ai-service): structured error for backend unavailability`

## Task 4: 并发与 E2E 验证 [P1]

**Files:**
- Test: `ai-service/tests/integration/test_mcp_concurrent_tools.py`

**Spec coverage:** REQ-001 / REQ-002 / REQ-003

- [x] **RED:** Write failing tests — 并发调用 query_coaches 与 query_packages 独立正确返回；SDK client 依次调用 3 个推荐工具全通（依赖本地 backend）
- [x] **GREEN:** 确认工具无共享可变状态
- [x] **COMMIT:** `test(ai-service): mcp recommendation e2e`

## Task 5: 验证

- [x] **5.1** Run `pytest ai-service/tests` and confirm coverage >= 80%
- [x] **5.2** 双协议层归一化一致性测试通过（LangChain/MCP 同输入同输出）
