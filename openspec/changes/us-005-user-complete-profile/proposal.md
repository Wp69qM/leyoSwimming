## Why

用户补充注册资料是微信授权登录（US-004）后完善用户档案的关键步骤。当前用户首次微信登录后已由 US-004 创建用户记录并置 `identity_status='注册用户'`、`profile_completed=false`，但缺少手机号、用户名、密码等核心资料。本 US 在身份已为「注册用户」的基础上，完成 `profile_completed: false → true` 的资料完整化转换，使用户可使用需要完整资料的业务功能（账号安全、购课约课）。

PRD [§5.2.1](../../../docs/prd/prd.md) 要求微信授权登录后首次登录需补充手机号、用户名、密码、邮箱；[§11.2](../../../docs/prd/prd.md) 要求首次注册需用户同意隐私协议，明确告知数据用途。

## What Changes

- 新增 `PUT /api/user/profile` 接口（补充/更新用户资料，设置 `profile_completed = true`）
- 新增 `GET /api/user/phone/exists` 接口（校验手机号是否已注册）
- 修改 `user` 表：补充 `phone`、`username`、`password_hash`、`email`、`profile_completed = true`、`status`
- 新增 Redis 缓存：`phone:exists:{phone_hash}`、`username:exists:{username}`、`user:{user_id}`
- 新增小程序"资料补充页"（默认带入微信头像与昵称，手机号必填，含隐私协议同意 checkbox）
- 触发用户资料完成状态转换：**`profile_completed`: false → true**（`identity_status` 已在 US-004 置为「注册用户」，本 US 不再修改）
- 边界处理：手机号已注册、用户名已占用、密码强度不足、隐私协议未同意、重复提交幂等

## Capabilities

### New Capabilities

- `user-complete-profile`: 用户在微信授权登录后补充手机号、用户名、密码、邮箱等注册资料，完成 `profile_completed: false → true` 的资料完整化转换，包含唯一性校验、密码强度校验、幂等处理与错误码映射

### Modified Capabilities

- `wechat-auth`: 登录成功后新增 `profile_completed` 判断，未补充资料时跳转资料补充页

## Impact

- **数据表**：修改 `user`（补充资料字段、更新 `profile_completed = true`）
- **API**：新增 2 个端点 `PUT /api/user/profile`（需登录鉴权）、`GET /api/user/phone/exists`（需登录鉴权）
- **缓存**：新增 Redis key `phone:exists:{phone_hash}`（TTL 300s）、`username:exists:{username}`（TTL 300s）、`user:{user_id}`（TTL 1800s）
- **状态机**：触发用户资料完成状态转换 `profile_completed: false → true`（`identity_status` 保持「注册用户」）
- **前端**：新增小程序 1 个页面（`complete-profile/index`）
- **依赖**：依赖 US-004 微信授权登录、US-009 隐私协议授权；被 US-008 依赖
- **安全**：手机号 AES-256 加密存储；密码 bcrypt 哈希（cost=12）；接口限流防撞库
