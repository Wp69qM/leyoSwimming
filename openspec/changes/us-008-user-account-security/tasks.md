# Tasks: US-008 用户账号安全设置

> 本文档对应 `docs/stories/US-008-用户-账号安全设置/test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: 换绑手机号接口 [P0]

**Files:**
- Create: `backend/src/controllers/user/security.ts`
- Create: `backend/src/routes/user/security.ts`
- Test: `backend/tests/controllers/user/security.test.ts`

**Spec coverage:** REQ-001 Scenario "正常换绑手机号"

- [ ] **RED:** Write 2 failing tests — 新手机号未绑定+验证码正确返回 200；验证码错误返回 400
- [ ] **GREEN:** Implement `PUT /api/user/security/phone` with SMS verify and phone update
- [ ] **COMMIT:** `feat(user): add phone change endpoint`

## Task 2: 修改密码接口 [P0]

**Files:**
- Modify: `backend/src/controllers/user/security.ts`
- Create: `backend/src/services/user/password_change.ts`
- Test: `backend/tests/services/user/password_change.test.ts`

**Spec coverage:** REQ-002 Scenarios "正常修改密码", "原密码错误"

- [ ] **RED:** Write 3 failing tests — 原密码正确返回 200；原密码错误返回 `INVALID_OLD_PASSWORD`；弱密码返回 `WEAK_PASSWORD`
- [ ] **GREEN:** Implement `PUT /api/user/security/password` with bcrypt verify and hash update
- [ ] **COMMIT:** `feat(user): add password change endpoint`

## Task 3: 邮箱绑定接口 [P1]

**Files:**
- Modify: `backend/src/controllers/user/security.ts`
- Test: `backend/tests/controllers/user/security.test.ts`

**Spec coverage:** REQ-001 / REQ-002 相关绑定能力

- [ ] **RED:** Write 2 failing tests — 有效邮箱+验证码返回 200；无效邮箱格式返回 400
- [ ] **GREEN:** Implement `PUT /api/user/security/email` with email validation and update
- [ ] **COMMIT:** `feat(user): add email bind endpoint`

## Task 4: 设备列表查询 [P0]

**Files:**
- Create: `backend/src/repositories/user_session.ts`（扩展）
- Modify: `backend/src/controllers/user/security.ts`
- Test: `backend/tests/controllers/user/security.test.ts`

**Spec coverage:** REQ-003 Scenario "下线登录设备"（列表部分）

- [ ] **RED:** Write 1 failing test — `GET /api/user/security/devices` 返回最近 30 天设备列表
- [ ] **GREEN:** Implement device list query with `is_current` flag
- [ ] **COMMIT:** `feat(user): add login device list endpoint`

## Task 5: 下线设备功能 [P0]

**Files:**
- Modify: `backend/src/controllers/user/security.ts`
- Modify: `backend/src/repositories/user_session.ts`
- Test: `backend/tests/controllers/user/security.test.ts`

**Spec coverage:** REQ-003 Scenario "下线登录设备"

- [ ] **RED:** Write 2 failing tests — 下线其他设备返回 204；被下线 token 再次访问返回 401
- [ ] **GREEN:** Implement device revoke and Redis session deletion
- [ ] **COMMIT:** `feat(user): add device revoke endpoint`

## Task 6: 换绑频率限制 [P1]

**Files:**
- Modify: `backend/src/services/user/phone_change.ts`
- Test: `backend/tests/services/user/phone_change.test.ts`

**Spec coverage:** REQ-001 Scenario "24 小时内频繁换绑"

- [ ] **RED:** Write 1 failing test — 24 小时内第二次换绑返回 `PHONE_CHANGE_LIMIT`
- [ ] **GREEN:** Implement Redis-based 24h rate limit for phone change
- [ ] **COMMIT:** `feat(user): add phone change rate limit`

## Task 7: 小程序账号安全页 [P1]

**Files:**
- Create: `miniapp-user/src/pages/security/index.tsx`
- Create: `miniapp-user/src/pages/security/devices.tsx`
- Test: `miniapp-user/src/pages/security/index.test.tsx`

**Spec coverage:** REQ-001 / REQ-002 / REQ-003 正常场景

- [ ] **RED:** Write 3 failing tests — 换绑手机成功后显示新手机号；修改密码成功后提示；下线设备后列表刷新
- [ ] **GREEN:** Implement security settings page and device management page
- [ ] **COMMIT:** `feat(miniapp): add account security settings pages`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-2, 4-5），P1 选做（Task 3, 6-7）

## GWT Coverage Matrix

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 | Task 5 | Task 6 | Task 7 |
|---------|--------|--------|--------|--------|--------|--------|--------|
| §6.1 正常换绑手机号（正常） | ✅ | — | — | — | — | ✅ | ✅ |
| §6.2 正常修改密码（正常） | — | ✅ | — | — | — | — | ✅ |
| §6.3 新手机号已被绑定（异常） | ✅ | — | — | — | — | — | — |
| §6.4 原密码错误（异常） | — | ✅ | — | — | — | — | — |
| §6.5 下线登录设备（异常） | — | — | — | ✅ | ✅ | — | ✅ |
| 24 小时频繁换绑（边界） | — | — | — | — | — | ✅ | — |
| 新密码与旧密码相同（边界） | — | ✅ | — | — | — | — | — |
