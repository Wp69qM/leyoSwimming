# Tasks: US-005 用户补充注册资料

> 本文档对应 `docs/stories/US-005-用户-补充注册资料/test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: 资料补充接口基础结构 [P0]

**Files:**
- Create: `backend/src/controllers/user/profile.ts`
- Create: `backend/src/routes/user.ts`
- Test: `backend/tests/controllers/user/profile.test.ts`

**Spec coverage:** REQ-001 Scenario "正常补充注册资料"

- [ ] **RED:** Write 2 failing tests — `PUT /api/user/profile` 未登录返回 401；已登录提交完整资料（含隐私协议同意）返回 200 且 `profile_completed=true`、`identity_status` 保持 '注册用户' 不变
- [ ] **GREEN:** Implement `PUT /api/user/profile` controller skeleton with auth middleware, route registration, and basic response shape
- [ ] **COMMIT:** `feat(user): add PUT /api/user/profile endpoint skeleton`

## Task 2: 手机号唯一性校验 [P0]

**Files:**
- Create: `backend/src/repositories/user.ts`（扩展 findByPhone）
- Test: `backend/tests/repositories/user.test.ts`

**Spec coverage:** REQ-001 Scenario "手机号已被注册"

- [ ] **RED:** Write 2 failing tests — 已存在手机号返回 `PHONE_ALREADY_BOUND`；未存在手机号允许继续
- [ ] **GREEN:** Implement `UserRepository.findByPhone(phone)` with encrypted lookup; integrate into profile controller
- [ ] **COMMIT:** `feat(user): add phone uniqueness check for profile completion`

## Task 3: 用户名唯一性校验 [P0]

**Files:**
- Modify: `backend/src/repositories/user.ts`
- Test: `backend/tests/repositories/user.test.ts`

**Spec coverage:** REQ-001 Scenario "用户名已被占用"

- [ ] **RED:** Write 2 failing tests — 已存在用户名返回 `USERNAME_TAKEN`；未存在用户名允许继续
- [ ] **GREEN:** Implement `UserRepository.findByUsername(username)` and integrate uniqueness check
- [ ] **COMMIT:** `feat(user): add username uniqueness check`

## Task 4: 密码强度校验 [P0]

**Files:**
- Create: `backend/src/services/user/password.ts`
- Test: `backend/tests/services/user/password.test.ts`

**Spec coverage:** REQ-001 Scenario "密码强度不足"

- [ ] **RED:** Write 3 failing tests — 弱密码 `123456` 返回 `WEAK_PASSWORD`；短密码返回 `WEAK_PASSWORD`；合规密码通过
- [ ] **GREEN:** Implement password validator (8-20 chars, letters + digits) and bcrypt hash service
- [ ] **COMMIT:** `feat(user): add password strength validation and bcrypt hashing`

## Task 5: 资料完成状态转换 [P0]

**Files:**
- Create: `backend/src/services/user/profile_completion.ts`
- Modify: `backend/src/controllers/user/profile.ts`
- Test: `backend/tests/services/user/profile_completion.test.ts`

**Spec coverage:** REQ-001 Scenario "正常补充注册资料"（资料完成状态转换部分）

- [ ] **RED:** Write 2 failing tests — 资料保存成功后 `user.profile_completed` 从 false 变为 true；`identity_status` 保持 '注册用户' 不变
- [ ] **GREEN:** Implement profile completion service; wrap update + cache invalidation in transaction
- [ ] **COMMIT:** `feat(user): implement profile_completed false → true transition`

## Task 6: 幂等性处理 [P0]

**Files:**
- Create: `backend/src/middleware/idempotency.ts`
- Modify: `backend/src/controllers/user/profile.ts`
- Test: `backend/tests/middleware/idempotency.test.ts`

**Spec coverage:** REQ-001 Scenario "重复提交幂等"

- [ ] **RED:** Write 2 failing tests — 相同 Idempotency-Key 重复提交返回首次结果；不同请求体返回 409
- [ ] **GREEN:** Implement Redis-backed idempotency middleware; attach to profile route
- [ ] **COMMIT:** `feat(user): add idempotency handling for profile completion`

## Task 7: 手机号存在性查询接口 [P1]

**Files:**
- Modify: `backend/src/routes/user.ts`
- Modify: `backend/src/controllers/user/profile.ts`
- Test: `backend/tests/controllers/user/profile.test.ts`

**Spec coverage:** REQ-002 Scenarios "查询未注册手机号", "查询已注册手机号"

- [ ] **RED:** Write 2 failing tests — 未注册手机号返回 `exists=false`；已注册手机号返回 `exists=true`
- [ ] **GREEN:** Implement `GET /api/user/phone/exists` with phone format validation and rate limit
- [ ] **COMMIT:** `feat(user): add GET /api/user/phone/exists endpoint`

## Task 8: 小程序资料补充页 [P1]

**Files:**
- Create: `miniapp-user/src/pages/complete-profile/index.tsx`
- Test: `miniapp-user/src/pages/complete-profile/index.test.tsx`

**Spec coverage:** REQ-001 Scenarios "正常补充注册资料", "必填项缺失"

- [ ] **RED:** Write 4 failing tests — 未填手机号阻止提交；未勾选隐私协议阻止提交；提交成功后跳转首页；接口错误展示提示
- [ ] **GREEN:** Implement complete-profile page with form validation, privacy consent checkbox, API call, and redirect
- [ ] **COMMIT:** `feat(miniapp): add complete profile page`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-6），P1 选做（Task 7-8）
- **状态机验证**：Task 5 必须显式断言 `user.profile_completed=true` 且 `identity_status` 保持 '注册用户' 不变（profile_completed false→true 转换）

## GWT Coverage Matrix

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 | Task 5 | Task 6 | Task 7 | Task 8 |
|---------|--------|--------|--------|--------|--------|--------|--------|--------|
| §6.1 正常补充（正常） | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | — | ✅ |
| §6.2 手机号已被注册（异常） | — | ✅ | — | — | — | — | — | — |
| §6.3 必填项缺失（异常） | — | — | — | — | — | — | — | ✅ |
| 用户名已被占用（异常） | — | — | ✅ | — | — | — | — | — |
| 密码强度不足（异常） | — | — | — | ✅ | — | — | — | — |
| 重复提交幂等（边界） | — | — | — | — | — | ✅ | — | — |
