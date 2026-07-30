# Tasks: US-007 用户账号注销

> 本文档对应 `docs/stories/US-007-用户-账号注销/test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: 注销条件检查接口 [P0]

**Files:**
- Create: `backend/src/controllers/user/account.ts`
- Create: `backend/src/routes/user/account.ts`
- Test: `backend/tests/controllers/user/account.test.ts`

**Spec coverage:** REQ-002 Scenarios "查询满足注销条件", "查询不满足注销条件"

- [ ] **RED:** Write 2 failing tests — 无套餐/订单返回 `can_cancel=true`；有套餐/订单返回 `can_cancel=false` 及数量
- [ ] **GREEN:** Implement `GET /api/user/account/cancel/check` with package/order queries
- [ ] **COMMIT:** `feat(user): add account cancel check endpoint`

## Task 2: 正常注销流程 [P0]

**Files:**
- Modify: `backend/src/controllers/user/account.ts`
- Create: `backend/src/services/user/cancel.ts`
- Test: `backend/tests/services/user/cancel.test.ts`

**Spec coverage:** REQ-001 Scenario "正常注销账号"

- [ ] **RED:** Write 2 failing tests — 满足条件且验证通过返回 200，`user.status=2`；会话被清除
- [ ] **GREEN:** Implement cancel service with transaction: update user, delete sessions, insert audit log
- [ ] **COMMIT:** `feat(user): implement account cancellation flow`

## Task 3: active 套餐拦截 [P0]

**Files:**
- Modify: `backend/src/services/user/cancel.ts`
- Test: `backend/tests/services/user/cancel.test.ts`

**Spec coverage:** REQ-001 Scenario "存在 active 套餐时注销"

- [ ] **RED:** Write 1 failing test — 存在 active 套餐返回 `ACTIVE_PACKAGE_EXISTS`
- [ ] **GREEN:** Add active package check before cancel
- [ ] **COMMIT:** `feat(user): block cancellation when active package exists`

## Task 4: 未完成订单拦截 [P0]

**Files:**
- Modify: `backend/src/services/user/cancel.ts`
- Test: `backend/tests/services/user/cancel.test.ts`

**Spec coverage:** REQ-001 Scenario "存在未完成订单时注销"

- [ ] **RED:** Write 1 failing test — 存在待支付订单返回 `PENDING_ORDER_EXISTS`
- [ ] **GREEN:** Add pending order check before cancel
- [ ] **COMMIT:** `feat(user): block cancellation when pending order exists`

## Task 5: 注销后登录态失效 [P0]

**Files:**
- Modify: `backend/src/services/user/cancel.ts`
- Create: `backend/src/repositories/user_session.ts`
- Test: `backend/tests/repositories/user_session.test.ts`

**Spec coverage:** REQ-001 Scenario "正常注销账号"（会话清除部分）

- [ ] **RED:** Write 2 failing tests — 注销后所有 token 无法校验；Redis 中 session key 被删除
- [ ] **GREEN:** Implement session revocation for all user tokens
- [ ] **COMMIT:** `feat(user): revoke all sessions after account cancellation`

## Task 6: 二次验证 [P1]

**Files:**
- Modify: `backend/src/services/user/cancel.ts`
- Test: `backend/tests/services/user/cancel.test.ts`

**Spec coverage:** REQ-001 Scenario "二次验证失败"

- [ ] **RED:** Write 1 failing test — 错误验证码返回 `INVALID_CREDENTIALS`
- [ ] **GREEN:** Add verify_code validation before cancel
- [ ] **COMMIT:** `feat(user): add secondary verification for account cancellation`

## Task 7: 小程序注销确认页 [P1]

**Files:**
- Create: `miniapp-user/src/pages/account-cancel/index.tsx`
- Test: `miniapp-user/src/pages/account-cancel/index.test.tsx`

**Spec coverage:** REQ-001 Scenario "正常注销账号"

- [ ] **RED:** Write 2 failing tests — 不满足条件时禁用确认按钮；满足条件注销成功后跳转登录页
- [ ] **GREEN:** Implement account cancel page with condition checklist and confirmation
- [ ] **COMMIT:** `feat(miniapp): add account cancellation page`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-5），P1 选做（Task 6-7）

## GWT Coverage Matrix

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 | Task 5 | Task 6 | Task 7 |
|---------|--------|--------|--------|--------|--------|--------|--------|
| §6.1 正常注销账号（正常） | — | ✅ | — | — | ✅ | — | ✅ |
| §6.2 存在 active 套餐（异常） | ✅ | — | ✅ | — | — | — | — |
| §6.3 存在未完成订单（异常） | ✅ | — | — | ✅ | — | — | — |
| 二次验证失败（异常） | — | — | — | — | — | ✅ | — |
| 重复提交幂等（边界） | — | ✅ | — | — | — | — | — |
