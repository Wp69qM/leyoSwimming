# US-054 教练手机号验证码登录 — 测试计划

> **状态**：待填充（本 US 进入 TDD 实现阶段由开发+QA 共同补充）
> **对应 user-story**：[./user-story.md](./user-story.md)

---

## 1. 测试范围

| 层级 | 工具 | 说明 |
|------|------|------|
| 单元测试 | Jest | 手机号格式校验、验证码校验、状态分流映射 |
| 集成测试 | Jest + Supertest | `POST /api/common/sms/send`、`POST /api/coach/auth/phone-login` |
| E2E 测试 | 微信小程序自动化 | 手机号登录页完整流程 |

---

## 2. TDD 任务清单

### Task 1：发送短信验证码接口 [P0]

**Files:**
- Create/Update: `backend/src/controllers/sms.ts`
- Create/Update: `backend/src/services/sms.ts`
- Create/Update: `backend/src/routes/sms.ts`
- Test: `backend/tests/controllers/coach/sms_send.test.ts`

**对应 GWT:** 场景 5（60 秒内重复获取验证码）

- [ ] **RED**：编写失败测试 — 手机号格式错误返回 `INVALID_PHONE`；60 秒内重复请求返回 `RATE_LIMITED`；正常请求返回 `expire_seconds=300`。
- [ ] **GREEN**：实现 `POST /api/common/sms/send` — 校验手机号格式、限流（60 秒 1 条、24 小时 10 条）、生成 6 位验证码、写入 `sms_code` 表、调用短信服务。
- [ ] **REFACTOR**：将限流逻辑提取为可复用中间件。
- [ ] **COMMIT**：`feat(sms): add POST /api/common/sms/send for coach login`

### Task 2：手机号验证码登录接口 [P0]

**Files:**
- Create/Update: `backend/src/controllers/coach/auth.ts`
- Create/Update: `backend/src/services/coach/auth.ts`
- Test: `backend/tests/controllers/coach/phone_login.test.ts`

**对应 GWT:** 场景 1、4

- [ ] **RED**：编写失败测试 — 未勾选协议返回 `TERMS_NOT_ACCEPTED`；验证码错误返回 `INVALID_SMS_CODE`；验证码过期返回 `SMS_CODE_EXPIRED`；正常登录返回 token、`is_new_coach`、`coach_status`。
- [ ] **GREEN**：实现 `POST /api/coach/auth/phone-login` — 校验协议、校验手机号与验证码、按手机号查询/创建 coach 记录、签发 JWT、返回 coach_status。
- [ ] **REFACTOR**：复用 US-051 的 coach 查询与 JWT 签发逻辑。
- [ ] **COMMIT**：`feat(auth): add POST /api/coach/auth/phone-login`

### Task 3：自动注册与已离职账号复用 [P0]

**Files:**
- Create/Update: `backend/src/repositories/coach.ts`
- Test: `backend/tests/repositories/coach.test.ts`

**对应 GWT:** 场景 2、6

- [ ] **RED**：编写失败测试 — 未注册手机号登录后创建 `status=-1` 的新 coach 记录；已离职手机号登录后复用原记录并返回 `coach_status=3`；已注册正常手机号登录更新 `last_login_at`。
- [ ] **GREEN**：实现 coach 记录的查询/创建逻辑：手机号不存在时 INSERT；手机号存在且 `status=3` 时复用原记录；其他状态更新 `last_login_at`/`login_ip`。
- [ ] **COMMIT**：`feat(coach): phone login auto-register and resigned account reuse`

### Task 4：教练端手机号登录页 [P1]

**Files:**
- Create: `miniapp-coach/src/pages/phone-login/index.tsx`
- Create: `miniapp-coach/src/pages/phone-login/index.test.tsx`

**对应 GWT:** 场景 1、4、5

- [ ] **RED**：编写失败测试 — 手机号输入框、验证码输入框、获取验证码按钮、登录按钮均存在；未勾选协议点击登录前端拦截；验证码错误展示提示。
- [ ] **GREEN**：实现手机号登录页 UI 与接口调用（手机号输入、验证码获取、协议勾选、登录提交）。
- [ ] **COMMIT**：`feat(miniapp-coach): add coach phone login page`

### Task 5：登录成功后按 coach_status 分流 [P1]

**Files:**
- Update: `miniapp-coach/src/utils/route.ts` 或 `miniapp-coach/src/app.tsx`
- Test: `miniapp-coach/src/app.test.tsx`

**对应 GWT:** 场景 1、2、6

- [ ] **RED**：编写失败测试 — 登录成功后按 `coach_status` 跳转到对应页面（-1→US-010，0→等待审核页，1→教练首页，2→US-010 展示驳回原因条，3→US-010 展示重新入驻说明条，4→教练首页）。
- [ ] **GREEN**：实现前端跳转逻辑，复用 US-051 的状态分流映射。
- [ ] **COMMIT**：`feat(miniapp-coach): redirect by coach_status after phone login`

### Task 6：集成测试与验收 [P1]

**Files:**
- Test: `backend/tests/integration/coach_phone_login.test.ts`

**对应 GWT:** 全部场景

- [ ] **RED**：编写失败测试 — 完整流程：发送验证码 → 登录 → 跳转；已离职账号复用 → 跳转 US-010。
- [ ] **GREEN**：跑通完整登录流程集成测试。
- [ ] **COMMIT**：`test(integration): coach phone login e2e flow`

---

## 4. 验收标准映射

| GWT 场景 | 测试方法 |
|----------|----------|
| 场景 1：已注册教练手机号验证码登录成功 | test_phone_login_existing_coach |
| 场景 2：未注册手机号首次验证码登录并自动注册 | test_phone_login_new_coach |
| 场景 3：未勾选《用户须知》或《隐私协议》 | test_phone_login_terms_not_accepted |
| 场景 4：验证码错误 | test_phone_login_invalid_sms_code |
| 场景 5：60 秒内重复获取验证码 | test_sms_send_rate_limit |
| 场景 6：已离职教练手机号登录后进入重新入驻 | test_phone_login_resigned_coach |
| 场景 7：网络异常导致登录失败 | E2E / 集成测试覆盖 |

---

## 5. 变更日志

| 版本 | 日期 | 变更 |
|------|------|------|
| v1.0 | 2026-08-08 | 补充测试范围、任务清单、用例占位与验收标准映射 |
| v1.1 | 2026-08-08 | 细化 TDD 任务清单，明确 RED→GREEN→COMMIT 步骤、对应文件、GWT 场景与 commit message |
