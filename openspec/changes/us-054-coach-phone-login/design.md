# Design: coach-phone-login

## 架构

复用 US-051 建立的教练端账号体系：`coach` 表存储教练身份，`coach_session` 表/Redis 存储会话，`sms_code` 表存储短信验证码。新增独立的教练端手机号验证码登录 API，登录成功后返回 `coach_status`，前端按 US-051 的 `coach_status` 分流规则跳转。

## 数据

- `coach` 表：新增/读取教练记录；手机号加密存储；`status` 字段支持 -1/0/1/2/3/4。
- `coach_session` 表：记录 JWT refresh_token 与会话元数据。
- `sms_code` 表：记录验证码、过期时间、手机号、使用状态。
- `coach_login_log` 表：记录登录时间、IP、设备、登录方式（phone）。

## API

- `POST /api/v1/auth/coach/sms/code`
  - 请求：`{ phone, app_type: "coach" }`
  - 响应：`{ success: true }` 或错误码
  - 限流：同一手机号 60 秒 1 条，24 小时上限 10 条。

- `POST /api/v1/auth/coach/login/phone`
  - 请求：`{ phone, code, terms_accepted, privacy_accepted }`
  - 响应：`{ access_token, refresh_token, expires_in, is_new_coach, coach_status }`
  - 错误码：`TERMS_NOT_ACCEPTED`、`INVALID_SMS_CODE`、`PHONE_FORMAT_ERROR`、`SMS_SEND_FAILED`、`NETWORK_ERROR`。

## 状态机

- 本 US 仅读取 `coach.status`，不主动修改状态。
- 首次登录创建 `coach` 记录，`status = -1`。
- 已离职手机号登录创建新记录，`status = -1`，新旧记录隔离。

## 安全

- `coach.phone` AES 加密存储。
- `sms_code` 表验证码 hash 存储或明文限时存储。
- JWT `access_token` 2h，`refresh_token` 7d。
- 登录接口限流：同一手机号 5 分钟内最多 5 次登录尝试。
- 日志中手机号脱敏。

## 跨 US 依赖

- US-051：复用 `coach` 表结构、`coach_session` 设计、coach_status 分流规则。
- US-009：复用协议勾选校验逻辑。
