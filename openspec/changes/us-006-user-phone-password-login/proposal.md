## Why

用户手机号/账号密码登录提供除微信授权外的独立登录方式，满足用户多场景登录需求（换设备、未安装微信、偏好账号密码等）。本 US 复用 US-005 补充的注册资料，让用户通过手机号+验证码或账号+密码完成登录，生成统一登录态。

PRD [§5.2.1](../../../docs/prd/prd.md) 要求支持手机号验证码登录、账号密码登录；[§11.1](../../../docs/prd/prd.md) 要求用户手机号、密码加密存储。

## What Changes

- 新增 `POST /api/auth/sms/code` 接口（发送登录验证码）
- 新增 `POST /api/auth/login/phone` 接口（手机号验证码登录）
- 新增 `POST /api/auth/login/password` 接口（账号密码登录）
- 新增 `sms_code` 表（存储验证码、场景、过期时间、使用状态）
- 新增 `user_login_log` 表（记录登录时间、IP、设备）
- 修改 `user` 表：更新 `last_login_at`、`login_ip`、`failed_login_count`、`locked_until`
- 新增 Redis 缓存：`login:fail:{account}`、`sms:limit:{phone}`、`session:{token}`
- 新增小程序登录页 Tab（手机号登录 / 密码登录）
- 边界处理：验证码 60 秒限流、连续 5 次密码错误锁定 30 分钟、异地登录提醒

## Capabilities

### New Capabilities

- `user-phone-password-login`: 用户通过手机号验证码或账号密码登录系统，包含验证码发送与校验、密码 bcrypt 校验、登录失败锁定、登录态生成与设备日志记录

### Modified Capabilities

- `user-complete-profile`: 为账号密码登录提供手机号/密码来源
- `wechat-auth`: 登录态与 JWT 逻辑共享

## Impact

- **数据表**：新增 `sms_code`；新增 `user_login_log`；修改 `user`（登录相关字段）
- **API**：新增 3 个端点 `POST /api/auth/sms/code`、`POST /api/auth/login/phone`、`POST /api/auth/login/password`
- **缓存**：新增 Redis key `login:fail:{account}`（TTL 1800s）、`sms:limit:{phone}`（TTL 60s）、`session:{token}`（TTL 30 天）
- **状态机**：无身份状态变化，仅会话状态转换（未登录 → 已登录）
- **前端**：新增/修改小程序登录页（支持两种登录方式 Tab）
- **依赖**：依赖 US-005 补充的手机号/密码；被 US-008 依赖
- **安全**：bcrypt 校验密码防时序攻击；连续失败锁定；验证码 TTL 5 分钟；短信限流
