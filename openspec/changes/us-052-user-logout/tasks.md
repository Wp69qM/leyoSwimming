# Tasks: US-052 用户退出登录

> 本文档对应 `docs/stories/US-052-.../test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: 后端退出登录接口 [P0]

**Files:**
- Create/Update: `backend/src/controllers/auth.ts`
- Create/Update: `backend/src/routes/auth.ts`
- Create/Update: `backend/src/services/session.ts`
- Test: `backend/tests/controllers/logout.test.ts`

**Spec coverage:** REQ-001 Scenarios "用户正常退出登录", "本地 token 已过期或不存在"; REQ-002 Scenario "退出登录后端会话失效"

- [ ] **RED:** Write 3 failing tests — 正常退出使 refresh_token 失效；access_token 无效返回 401；重复调用幂等返回 200
- [ ] **GREEN:** Implement `POST /api/v1/auth/logout` — 校验 access_token，按 refresh_token hash 标记 session 失效
- [ ] **COMMIT:** `feat(auth): add POST /auth/logout endpoint to revoke session`

## Task 2: 小程序「我的」页面退出登录逻辑 [P0]

**Files:**
- Create/Update: `miniapp-user/src/pages/mine/index.tsx`
- Create/Update: `miniapp-user/src/utils/auth.ts`
- Test: `miniapp-user/src/pages/mine/index.test.tsx`

**Spec coverage:** REQ-001 Scenarios "用户正常退出登录", "用户取消退出", "本地 token 已过期或不存在"

- [ ] **RED:** Write 3 failing tests — 确认退出后清除 token 并刷新「我的」页面为未登录态；取消退出不调用接口；本地无 token 直接刷新为未登录态
- [ ] **GREEN:** Implement 「我的」页面退出登录按钮 + 二次确认弹窗 + token 清除 + 刷新页面为未登录态
- [ ] **COMMIT:** `feat(miniapp): add logout button on mine page with token cleanup`

---

## Execution Discipline

- 严格顺序：Task 1 → Task 2
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-2）

## GWT Coverage Matrix

| GWT 场景 | Task 1 | Task 2 |
|---------|--------|--------|
| §6.1 正常退出 | ✅ | ✅ |
| §6.2 取消退出 | — | ✅ |
| §6.3 本地 token 已过期/不存在 | ✅ | ✅ |
