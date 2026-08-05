# Tasks: US-004 用户微信授权登录

> 本文档对应 `docs/stories/US-004-.../test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: User Repository — findByUnionId + create [P0]

**Files:**
- Create: `backend/src/repositories/user.ts`
- Test: `backend/tests/repositories/user.test.ts`

**Spec coverage:** REQ-002 Scenarios "首次登录触发游客→注册用户状态转换", "老用户登录不修改 identity_status", "user 表唯一约束"

- [ ] **RED:** Write 4 failing tests — `findByUnionId` returns active user (老用户复用); returns null for deleted user (软删除不复用); returns null for not found (新用户); `create` inserts with `identity_status='注册用户'` (状态机转换)
- [ ] **GREEN:** Implement `UserRepository.findByUnionId(unionId)` — `WHERE union_id=? AND status=0`; `create(input)` — INSERT with `identity_status='注册用户'`, `profile_completed=false`, `status=0`
- [ ] **COMMIT:** `feat(user): add UserRepository with findByUnionId and create (identity_status transition)`

## Task 2: 微信 OAuth Service — code2session + 用户创建/查询 [P0]

**Files:**
- Create: `backend/src/services/wechat-auth.ts`
- Create: `backend/src/clients/wechat.ts`
- Test: `backend/tests/services/wechat-auth.test.ts`

**Spec coverage:** REQ-001 Scenarios "首次微信授权登录成功", "已注册用户微信授权登录成功", "登录凭证已失效", "微信接口调用失败"; REQ-002 Scenario "首次登录触发游客→注册用户状态转换"

- [ ] **RED:** Write 4 failing tests — 首次登录创建新用户 (`identity_status='注册用户'`, `isNewUser=true`, session_key 缓存到 Redis); 老用户登录复用账号 (`isNewUser=false`); code 失效抛 `WECHAT_CODE_INVALID`; 微信超时抛 `WECHAT_API_TIMEOUT`
- [ ] **GREEN:** Implement `WechatAuthService.authenticate(code)` — call `code2session` (3s timeout), map errcode, `findByUnionId` or `create` (with state transition), cache `session_key` to Redis (TTL 7200s)
- [ ] **COMMIT:** `feat(auth): add WechatAuthService with code2session and user creation (identity_status transition)`

## Task 3: JWT 签发与验证 [P0]

**Files:**
- Create: `backend/src/services/jwt.ts`
- Create: `backend/src/services/session.ts`
- Test: `backend/tests/services/jwt.test.ts`

**Spec coverage:** REQ-001 Scenarios "首次微信授权登录成功", "已注册用户微信授权登录成功" (token 签发)

- [ ] **RED:** Write 3 failing tests — `signAccessToken` includes `sub`/`identity_status`/`profile_completed`; `verifyAccessToken` rejects expired token with `TOKEN_EXPIRED`; `SessionService.create` writes `user_session` with `refresh_token_hash` (not plaintext)
- [ ] **GREEN:** Implement `JwtService` (HS256, 2h expiry) + `SessionService` (generate `refresh_token` via `crypto.randomBytes(32)`, store SHA-256 hash, encrypt `session_key` with AES-256-CBC)
- [ ] **COMMIT:** `feat(auth): add JwtService and SessionService with access_token + refresh_token`

## Task 4: POST /auth/wechat-login API 端点 [P0]

**Files:**
- Create: `backend/src/controllers/auth.ts`
- Create: `backend/src/routes/auth.ts`
- Test: `backend/tests/controllers/auth.test.ts`

**Spec coverage:** REQ-001 All 6 Scenarios (首次登录、老用户登录、未勾选协议、code 失效、微信接口失败、幂等)

- [ ] **RED:** Write 8 failing tests — 200 + `isNewUser=true` (首次); 200 + `isNewUser=false` (老用户); 400 `TERMS_NOT_ACCEPTED` (未勾选协议); 401 `WECHAT_CODE_INVALID`; 502 `WECHAT_API_ERROR`; 504 `WECHAT_API_TIMEOUT`; 幂等返回首次结果; 400 缺少 code
- [ ] **GREEN:** Implement `wechatLogin` controller — validate `termsAccepted`/`privacyAccepted`, validate `code`, check Redis idempotency (5min), call `WechatAuthService.authenticate`, call `SessionService.create`, cache idempotent result, map error codes to HTTP status
- [ ] **COMMIT:** `feat(api): add POST /auth/wechat-login endpoint with terms/privacy validation, idempotency and error mapping`

## Task 5: 小程序登录页与登录态管理 [P1]

**Files:**
- Create: `miniapp-user/src/pages/login/index.tsx`
- Create: `miniapp-user/src/utils/auth.ts`
- Test: `miniapp-user/src/pages/login/index.test.tsx`
- Test: `miniapp-user/src/utils/auth.test.tsx`

**Spec coverage:** REQ-001 Scenarios "首次微信授权登录成功", "已注册用户微信授权登录成功", "未勾选《用户须知》或《隐私协议》", "微信接口调用失败"; 拒绝授权处理（user-story §6.4）; REQ-003 Scenario "登录成功后存储 token 并用其维持登录态"

- [ ] **RED:** Write 8 failing tests — 未勾选协议点击登录提示文案且不调用微信授权; 勾选协议后首次登录跳转补充资料页; 老用户登录跳转首页; 拒绝授权展示提示文案; 微信接口错误展示文案; 防重复点击（按钮 disabled）; 登录成功后 token 写入本地存储; 请求拦截器自动携带 Bearer token; token 过期触发刷新或重新登录
- [ ] **GREEN:** Implement `LoginPage` — show terms/privacy checkbox; block login if not checked; call `Taro.login()` → POST `/auth/wechat-login` → save tokens to storage → redirect by `profileCompleted` flag; handle auth deny error; disable button during loading. Implement `auth.ts` — token storage helper, request interceptor attaching `Authorization: Bearer {access_token}`, token refresh on 401, redirect to login when refresh fails
- [ ] **COMMIT:** `feat(miniapp): add wechat login page, token storage, request interceptor and session handling`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-4），P1 选做（Task 5）
- **状态机验证**：Task 1 和 Task 2 必须显式断言 `identity_status='注册用户'`（游客→注册用户转换）

## GWT Coverage Matrix

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 | Task 5 |
|---------|--------|--------|--------|--------|--------|
| §6.1 首次登录（正常） | ✅ | ✅ | ✅ | ✅ | ✅ |
| §6.2 老用户登录（正常） | ✅ | ✅ | ✅ | ✅ | ✅ |
| §6.3 未勾选协议（异常） | — | — | — | ✅ | ✅ |
| §6.4 拒绝授权（异常） | — | — | — | — | ✅ |
| §6.5 微信接口失败（异常） | — | ✅ | — | ✅ | ✅ |
| §6.6 code 失效（异常） | — | ✅ | — | ✅ | — |
| §6.7 token 存储与登录态维持（正常） | — | — | ✅ | — | ✅ |
