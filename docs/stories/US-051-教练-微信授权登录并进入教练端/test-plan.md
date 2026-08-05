# US-051 教练微信授权登录并进入教练端 — 测试计划

> **状态**：待开发/QA 填写
> **对应用户故事**：[./user-story.md](./user-story.md)
> **最后更新**：2026-08-05

---

## 1. 测试目标

验证教练端微信授权登录按 `coach.status` 正确分流，且教练端账号体系完全独立于用户端（不查询、不复用 `user` 表）。

---

## 2. 测试范围

| 范围 | 说明 |
|------|------|
| 单元测试 | `redirect_page` 计算逻辑、协议勾选校验、幂等逻辑 |
| 集成测试 | `POST /api/v1/auth/wechat-login`（`app_type=coach`）、`GET /api/v1/coach/me/status` |
| E2E 测试 | 教练端登录 → 按状态跳转完整流程 |

---

## 3. TDD 任务清单

### Task 1：Coach Repository — findCoachByUnionId / createCoach [P0]

- **RED**：编写测试用例，验证 `findCoachByUnionId` 返回 status=1/0/2 的 coach、status=3 时视为未命中、未命中返回 null；验证 `createCoach` 创建 status=-1 记录并写入 phone/avatar/nickname。
- **GREEN**：实现 `CoachRepository.findCoachByUnionId(unionId)`（`WHERE union_id=? AND status != 3`）与 `CoachRepository.createCoach({ openid, unionId, phone, avatarUrl, nickname })`。
- **REFACTOR**：提取 coach 创建时的默认值构造函数。
- **COMMIT**：`feat(coach): add CoachRepository for direct coach login`

### Task 2：扩展 WechatAuthService 以支持教练端 [P0]

- **RED**：编写测试用例，验证 `app_type=coach` 时：
  - 无 coach 记录 → 新建 coach（status=-1），返回 `is_new_coach=true`、`coach_status=-1`、`redirect_page=coach_onboarding`。
  - status=1 → 返回 `coach_home`。
  - status=0 → 返回 `coach_pending`。
  - status=2 → 返回 `coach_rejected` 并带 `rejection_reason`。
  - status=3 → 视为未命中，新建 coach。
  - 未勾选协议 → 抛 `TERMS_NOT_ACCEPTED`。
  - 非法 `app_type` → 抛 `VALIDATION_ERROR`。
- **GREEN**：扩展 `WechatAuthService.authenticate({ code, encryptedData, iv, termsAccepted, privacyAccepted, appType })` —— `appType='coach'` 时直接查/写 `coach` 表，不调用 user repo，计算 `redirect_page`。
- **REFACTOR**：提取 `resolveCoachRedirectPage(coachStatus)` 纯函数；提取协议校验函数。
- **COMMIT**：`feat(auth): extend WechatAuthService to support independent coach login`

### Task 3：改造 `POST /api/v1/auth/wechat-login` 端点 [P0]

- **RED**：编写测试用例，验证：
  - `app_type=coach` 未入驻返回 200 + `redirect_page=coach_onboarding`。
  - `app_type=coach` 已通过返回 200 + `coach_home`。
  - `app_type=coach` 缺失 `encryptedData`/`iv` 返回 400。
  - `app_type=coach` 未勾选协议返回 400 + `TERMS_NOT_ACCEPTED`。
  - `app_type=user` 保持 US-004 响应格式（不含 `coach_status`/`redirect_page`/`is_new_coach`）。
  - 非法 `app_type` 返回 400。
  - 幂等返回首次结果。
- **GREEN**：更新 controller 接收 `encryptedData`、`iv`、`terms_accepted`、`privacy_accepted`、`app_type`，透传 service，按 `app_type` 决定响应字段。
- **COMMIT**：`feat(api): extend POST /auth/wechat-login with coach login fields`

### Task 4：新增 `GET /api/v1/coach/me/status` 端点 [P0]

- **RED**：编写测试用例，验证登录态下：
  - status=1 → `coach_status=1`、`redirect_page=coach_home`。
  - 无 coach 记录 → `coach_status=-1`、`redirect_page=coach_onboarding`。
  - status=2 → `rejection_reason` 非空。
  - 未登录返回 401。
- **GREEN**：实现 `GET /api/v1/coach/me/status`，auth middleware 从 JWT 取 `coach_id`，查询 coach 表返回状态。
- **COMMIT**：`feat(api): add GET /api/v1/coach/me/status`

### Task 5：教练端登录页与路由守卫 [P1]

- **RED**：编写 E2E 测试，模拟：
  - 未勾选协议点击登录 → 前端拦截，不调用 `wx.login()`。
  - 拒绝授权 → 展示文案。
  - 首次登录 → 跳转入驻资料页。
  - 已通过 → 跳转教练首页。
  - 已登录且 token 有效时打开小程序 → 调用 `/coach/me/status` 并跳转。
  - token 过期 → 用 refresh_token 刷新。
  - refresh_token 过期 → 重新登录。
- **GREEN**：实现 `CoachLoginPage` — 勾选协议 → `Taro.login()` → `wx.getPhoneNumber()` → POST `/auth/wechat-login` with `app_type=coach` → 存 token → 按 `redirect_page` 跳转；实现路由守卫处理 token 刷新/重登。
- **COMMIT**：`feat(coach-miniapp): add login page and coach status route guard`

---

## 4. 测试用例映射

| 场景 | 测试方法 | 层级 |
|------|---------|------|
| 未入驻教练首次登录跳转入驻资料页 | `test_coach_login_redirect_to_onboarding` | 集成 |
| 已通过教练登录跳转教练首页 | `test_coach_login_redirect_to_home` | 集成 |
| 待审核教练登录跳转等待审核页 | `test_coach_login_redirect_to_pending` | 集成 |
| 已驳回教练登录跳转重新提交页 | `test_coach_login_redirect_to_rejected` | 集成 |
| 已离职教练重新登录新建账号 | `test_resigned_coach_login_creates_new_record` | 集成 |
| 未勾选协议前端拦截 | `test_coach_login_terms_not_accepted_frontend` | E2E |
| 未勾选协议后端返回 TERMS_NOT_ACCEPTED | `test_coach_login_terms_not_accepted_backend` | 集成 |
| 拒绝微信/手机号授权 | `test_coach_login_rejected_authorization` | E2E |
| 用户端已注册账号进入教练端仍新建 coach | `test_user_record_not_reused_for_coach` | 集成 |
| 登录后 token 刷新与重登 | `test_coach_token_refresh_and_relogin` | E2E |
| `GET /coach/me/status` 各状态返回 | `test_coach_me_status` | 集成 |

---

## 5. 关键测试断言

### 5.1 不依赖 `user` 表

- `app_type=coach` 时，禁止调用 `UserRepository.findByUnionId` / `UserRepository.create`。
- 断言数据库 `user` 表无新增记录。
- 响应中不含 `is_new_user`、`profile_completed`。

### 5.2 教练端状态默认值

- 新建 coach 记录：`status=-1`、`phone` 为解密手机号、`avatar_url` / `nickname` 来自微信授权。
- `is_new_coach=true`、`coach_status=-1`、`redirect_page=coach_onboarding`。

### 5.3 协议校验

- 前端未勾选时禁止发起 `wx.login()` 与后端请求。
- 后端 `terms_accepted=false` 或 `privacy_accepted=false` 时返回 HTTP 400 + `TERMS_NOT_ACCEPTED`。

---

## 6. 待补充

- 详细测试数据构造（微信 code2session mock、手机号解密 mock）。
- 各状态教练记录的 factory/fixture。
- 错误码断言完整表。
