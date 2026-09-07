> **OpenSpec Tasks | 映射自 `docs/stories/US-068-系统-MCP服务注册Trae并完成端到端验证/test-plan.md`**
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环（本 US 以验证为主，代码改动仅限配置与文档）。

## Task 1: Trae MCP 注册与连接 [P0]

**Files:**
- Create/Update: `.trae/mcp.json`（或 Trae 当前版本 MCP 配置入口，token 用占位符）

**Spec coverage:** REQ-001 / REQ-002 / REQ-003

- [x] **RED:** 记录验证前状态 — 未注册时 Trae 无 leyo-ai-service server
- [x] **GREEN:** 写入项目级配置（URL + Bearer header），重载后连接状态正常、4 工具可见
- [x] **COMMIT:** `chore(trae): register leyo-ai-service mcp server`

## Task 2: 4 工具端到端真实调用 [P0]

**Files:**
- Update: `docs/stories/US-068-系统-MCP服务注册Trae并完成端到端验证/user-story.md`（§15 回填验证记录）

**Spec coverage:** REQ-001

- [x] **RED:** 无（验证型任务，以 US §15 记录表为验收物）
- [x] **GREEN:** 在 Trae 会话中依次调用 query_knowledge("野泳的注意事项") / query_coaches(stroke="蛙泳") / query_packages / get_hot_recommendations，全部成功
- [x] **COMMIT:** `docs(us-068): archive mcp verification records`

## Task 3: 异常路径与恢复性验证 [P0]

**Files:**
- Test: `ai-service/scripts/verify_mcp_tools.py`（SDK 全工具巡检脚本，可选）

**Spec coverage:** REQ-002 / REQ-003 / REQ-004

- [x] **RED:** 记录预期 — 错误 token 401、服务未启动连接拒绝、backend 停止时推荐工具结构化降级
- [x] **GREEN:** 逐项验证通过；token 轮换与服务重启恢复场景通过
- [x] **COMMIT:** `test(us-068): mcp failure and recovery paths`

## Task 4: 配置与部署文档同步 [P1]

**Files:**
- Update: `ai-service/.env.example`
- Update: 部署文档（`deploy/` 相关）

**Spec coverage:** REQ-004

- [x] **RED:** 无（文档任务）
- [x] **GREEN:** `MCP_API_TOKEN` 生成说明（≥32 位强随机）与 Trae 注册步骤写入文档
- [x] **COMMIT:** `docs(deploy): add mcp token and trae registration guide`

## Task 5: 验证

- [x] **5.1** Trae 连接状态正常且 4 工具全部调用成功
- [x] **5.2** 验证记录已归档到 US §15
- [x] **5.3** 异常与恢复场景全部通过
