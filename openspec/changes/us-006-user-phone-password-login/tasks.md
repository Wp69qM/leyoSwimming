# Tasks: US-006 用户手机号/账号密码登录

> 本文档对应 `docs/stories/US-006-用户-手机号账号密码登录/test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: 账号密码登录接口 [P0]

**Files:**
- Create: `backend/src/controllers/auth/login.ts`
- Create: `backend/src/services/auth/password.ts`
- Test: `backend/tests/controllers/auth/login.test.ts`

**Spec coverage:** REQ-002 Scenario "账号密码登录成功"

- [ ] **RED:** Write 2 failing tests — 正确账号密码返回 200 与 token；账号不存在返回 401
- [ ] **GREEN:** Implement `POST /api/auth/login/password` controller with bcrypt verify and token generation
- [ ] **COMMIT:** `feat(auth): add password login endpoint`

## Task 2: 手机号验证码登录接口 [P0]

**Files:**
- Modify: `backend/src/controllers/auth/login.ts`
- Create: `backend/src/services/auth/phone.ts`
- Test: `backend/tests/controllers/auth/login.test.ts`

**Spec coverage:** REQ-001 Scenario "手机号验证码登录成功"

- [ ] **RED:** Write 2 failing tests — 正确验证码返回 200 与 token；错误验证码返回 401
- [ ] **GREEN:** Implement `POST /api/auth/login/phone` with sms_code validation
- [ ] **COMMIT:** `feat(auth): add phone code login endpoint`

## Task 3: 验证码发送与校验 [P0]

**Files:**
- Create: `backend/src/services/auth/sms.ts`
- Create: `backend/src/repositories/sms_code.ts`
- Test: `backend/tests/services/auth/sms.test.ts`

**Spec coverage:** REQ-003 Scenario "正常发送登录验证码"；REQ-001 Scenario "验证码 60 秒内重复获取"

- [ ] **RED:** Write 3 failing tests — 发送成功写入 sms_code；60 秒内重复发送被拒绝；验证码过期返回 CODE_EXPIRED
- [ ] **GREEN:** Implement SMS code generation, persistence, TTL, and rate limit
- [ ] **COMMIT:** `feat(auth): add SMS code send and verify service`

## Task 4: 连续失败锁定机制 [P0]

**Files:**
- Modify: `backend/src/services/auth/password.ts`
- Modify: `backend/src/repositories/user.ts`
- Test: `backend/tests/services/auth/password.test.ts`

**Spec coverage:** REQ-002 Scenarios "密码错误", "连续 5 次密码错误后账号锁定"

- [ ] **RED:** Write 3 failing tests — 密码错误增加失败计数；第 5 次错误返回 ACCOUNT_LOCKED；锁定期间登录返回 ACCOUNT_LOCKED
- [ ] **GREEN:** Implement failed login counter and 30-minute lock logic
- [ ] **COMMIT:** `feat(auth): add account lock after 5 failed password attempts`

## Task 5: 登录态生成与校验 [P0]

**Files:**
- Create: `backend/src/services/auth/session.ts`
- Modify: `backend/src/controllers/auth/login.ts`
- Test: `backend/tests/services/auth/session.test.ts`

**Spec coverage:** REQ-001 / REQ-002 所有成功登录场景

- [ ] **RED:** Write 2 failing tests — 登录成功后生成 30 天有效 token；token 可校验出 user_id
- [ ] **GREEN:** Implement JWT/session service with 30-day expiry and Redis storage
- [ ] **COMMIT:** `feat(auth): add session/token service for login`

## Task 6: 账号状态拦截（注销/封禁） [P1]

**Files:**
- Modify: `backend/src/services/auth/password.ts`
- Modify: `backend/src/services/auth/phone.ts`
- Test: `backend/tests/controllers/auth/login.test.ts`

**Spec coverage:** REQ-002 Scenario "账号已注销"

- [ ] **RED:** Write 2 failing tests — 已注销账号登录返回 ACCOUNT_DEACTIVATED；已封禁账号登录返回 ACCOUNT_BANNED
- [ ] **GREEN:** Add user status check before password/phone login
- [ ] **COMMIT:** `feat(auth): reject login for deactivated or banned accounts`

## Task 7: 小程序登录页 Tab 切换 [P1]

**Files:**
- Create: `miniapp-user/src/pages/login/index.tsx`（扩展）
- Test: `miniapp-user/src/pages/login/index.test.tsx`

**Spec coverage:** REQ-001 / REQ-002 正常登录场景

- [ ] **RED:** Write 2 failing tests — Tab 切换保留已填手机号；密码登录成功后跳转首页
- [ ] **GREEN:** Implement login page with phone/password tabs and API integration
- [ ] **COMMIT:** `feat(miniapp): add phone/password login tabs`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-5），P1 选做（Task 6-7）

## GWT Coverage Matrix

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 | Task 5 | Task 6 | Task 7 |
|---------|--------|--------|--------|--------|--------|--------|--------|
| §6.1 手机号验证码登录成功（正常） | — | ✅ | ✅ | — | ✅ | — | ✅ |
| §6.2 账号密码登录成功（正常） | ✅ | — | — | — | ✅ | — | ✅ |
| §6.3 密码错误（异常） | ✅ | — | — | ✅ | — | — | — |
| 连续 5 次失败锁定（边界） | — | — | — | ✅ | — | — | — |
| 验证码错误（异常） | — | ✅ | ✅ | — | — | — | — |
| 60 秒重复获取（边界） | — | — | ✅ | — | — | — | — |
| 账号已注销（异常） | — | — | — | — | — | ✅ | — |
