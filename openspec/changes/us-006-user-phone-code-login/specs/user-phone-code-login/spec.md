> **OpenSpec Spec | 映射自 `docs/stories/US-006-用户-手机号验证码登录/user-story.md` §6**

## Capability

用户手机号验证码登录

## ADDED Requirements

### Requirement: REQ-001 手机号验证码登录

系统 MUST 提供 `POST /api/user/auth/phone-login` 接口，供用户使用手机号和短信验证码登录。系统 MUST 校验手机号格式、`terms_accepted=true` 且 `privacy_accepted=true`、验证码是否存在且未过期、验证码未被使用过。系统 MUST 在登录成功后签发 JWT `accessToken` + `refreshToken`，返回 `expiresIn`、`isNewUser`、`profileCompleted`、`userId`；前端 MUST 将 token 存储到本地并在后续请求中通过 `Authorization: Bearer {accessToken}` 携带；后端 MUST 校验 token 有效后方可访问受保护接口。系统 MUST 更新 `user.last_login_at`、记录 `user_login_log`。系统 MUST 在未勾选协议时返回 `TERMS_NOT_ACCEPTED`。系统 MUST 在验证码错误或过期时返回 `INVALID_SMS_CODE`。系统 MUST 在手机号未注册或手机号存在但 `status=1`（已注销）且验证码正确时，按 PRD §5.2.1 第 4 条重新创建用户记录，`identity_status='注册用户'`，`profile_completed=false`，且不绑定原账号数据。

#### Scenario: 已注册手机号验证码登录成功
- **GIVEN** 用户已注册且手机号 13800138000 状态正常
- **AND** 用户已勾选《用户须知》和《隐私协议》
- **AND** 系统已为该手机号生成未过期验证码 123456
- **WHEN** 用户输入手机号 13800138000 和验证码 123456
- **AND** 用户点击「登录」
- **THEN** 系统返回登录态 token
- **AND** 若 `profile_completed = true`，页面跳转至首页
- **AND** 若 `profile_completed = false`，页面跳转至「完善个人资料页」（US-005）
- **AND** `user.last_login_at` 更新为当前时间

#### Scenario: 未注册手机号首次验证码登录并自动注册
- **GIVEN** user 表中不存在手机号 13800138000
- **AND** 用户已同意隐私协议 v2.0
- **AND** 用户已勾选《用户须知》和《隐私协议》
- **AND** 系统已为该手机号生成未过期验证码 123456
- **WHEN** 用户输入手机号 13800138000 和验证码 123456
- **AND** 用户点击「登录」
- **THEN** 系统自动创建新用户记录
- **AND** `identity_status = "注册用户"`
- **AND** `profile_completed = false`
- **AND** `phone = 13800138000`
- **AND** 系统返回登录态 token
- **AND** 页面跳转至「完善个人资料页」（US-005）

#### Scenario: 未勾选《用户须知》或《隐私协议》
- **GIVEN** 用户已输入手机号 13800138000 和正确验证码 123456
- **WHEN** 用户未勾选《用户须知》或《隐私协议》
- **AND** 用户点击「登录」
- **THEN** 前端拦截登录请求并提示"请阅读并同意《用户须知》和《隐私协议》"
- **AND** 不发起后端登录请求
- **AND** 若用户绕过前端直接调用后端接口，后端返回错误码 `TERMS_NOT_ACCEPTED`
- **AND** 不签发任何 token
- **AND** 不创建或修改 user 记录

#### Scenario: 验证码错误
- **GIVEN** 用户已勾选《用户须知》和《隐私协议》
- **AND** 用户输入手机号 13800138000
- **WHEN** 用户输入错误验证码 000000
- **AND** 用户点击「登录」
- **THEN** 返回错误码 `INVALID_SMS_CODE`
- **AND** 前端提示"验证码错误或已过期"
- **AND** 不签发任何 token

#### Scenario: 已注销手机号重新注册登录
- **GIVEN** user 表中存在手机号 13800138000 且 status = 1（已注销）
- **AND** 用户已勾选《用户须知》和《隐私协议》
- **AND** 系统已为该手机号生成未过期验证码 123456
- **WHEN** 用户输入手机号 13800138000 和验证码 123456
- **AND** 用户点击「登录」
- **THEN** 系统按 PRD §5.2.1 第 4 条重新创建新用户记录
- **AND** 新记录 `identity_status = "注册用户"`
- **AND** 新记录 `profile_completed = false`
- **AND** 新记录 `phone = 13800138000`
- **AND** 新记录 `user_id` 与原注销账号不同
- **AND** 系统返回登录态 token
- **AND** 页面跳转至「完善个人资料页」（US-005）
- **AND** 原注销账号数据不被新账号访问或绑定

#### Scenario: 登录成功后存储 token 并用其维持登录态

- **GIVEN** 用户已完成手机号验证码登录
- **AND** 后端返回 `accessToken`、`refreshToken` 和 `expiresIn = 7200`
- **WHEN** 前端收到登录响应
- **THEN** 前端将 `accessToken` 和 `refreshToken` 写入本地存储
- **AND** 前端记录 `accessToken` 过期时间
- **WHEN** 用户访问受登录态保护的接口（如「我的」页面）
- **THEN** 前端在 Authorization Header 中携带 `Bearer {accessToken}`
- **AND** 后端校验 token 有效后返回用户数据
- **WHEN** `accessToken` 过期但 `refreshToken` 未过期
- **THEN** 前端调用刷新接口换取新的 `accessToken`
- **AND** 后续请求使用新的 `accessToken`
- **WHEN** 本地 token 不存在或 `refreshToken` 已过期
- **THEN** 前端引导用户重新进入登录页

### Requirement: REQ-002 登录验证码发送

系统 MUST 提供 `POST /api/common/sms/send` 接口发送登录验证码。系统 MUST 校验手机号格式。系统 MUST 将 6 位数字验证码、TTL 5 分钟写入 `sms_code` 表（手机号以 `phone_hash` 形式存储）。系统 MUST 对同一手机号 60 秒内只能发送 1 条验证码。

#### Scenario: 成功发送登录验证码
- **GIVEN** 用户手机号 13800138000
- **WHEN** 用户调用 `POST /api/common/sms/send`
- **THEN** 返回 HTTP 200
- **AND** `sms_code` 表新增一条记录，code 为 6 位数字
- **AND** `expires_at` 为当前时间 + 5 分钟

#### Scenario: 60 秒内重复获取验证码
- **GIVEN** 用户已在 60 秒内获取过验证码
- **WHEN** 用户再次点击「获取验证码」
- **THEN** 后端拒绝发送
- **AND** 前端提示"请 60 秒后再试"

## Delta Header

```yaml
delta:
  change: us-006-user-phone-code-login
  capability: user-phone-code-login
  type: revise
  rationale: 产品设计修正：移除账号密码登录，改为手机号验证码登录兼注册
  scope: docs/stories/US-006, openspec/changes/us-006-user-phone-code-login
```
