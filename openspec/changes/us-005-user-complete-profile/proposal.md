## Why

用户补充注册资料是微信授权登录（US-004）后完成身份转换的关键步骤。当前用户首次微信登录后仅创建基础记录，缺少手机号、用户名、密码等核心资料，无法被视为完整注册用户，也无法使用需要完整身份的业务功能（账号安全、隐私授权、购课约课）。本 US 完成从"游客/半注册用户"到"注册用户"的身份状态机转换。

PRD [§5.2.1](../../../docs/prd/prd.md) 要求微信授权登录后首次登录需补充手机号、用户名、密码、邮箱；[§11.2](../../../docs/prd/prd.md) 要求首次注册需用户同意隐私协议，明确告知数据用途。

## What Changes

- 新增 `PUT /api/user/profile` 接口（补充/更新用户资料，触发身份状态转换）
- 新增 `GET /api/user/phone/exists` 接口（校验手机号是否已注册）
- 修改 `user` 表：补充 `phone`、`username`、`password_hash`、`email`、`identity`、`status`
- 新增 `user_identity_log` 表记录：游客 → 注册用户
- 新增 Redis 缓存：`phone:exists:{phone_hash}`、`username:exists:{username}`、`user:{user_id}`
- 新增小程序"资料补充页"（默认带入微信头像与昵称，手机号必填）
- 触发用户身份状态机转换：**游客 → 注册用户**
- 边界处理：手机号已注册、用户名已占用、密码强度不足、重复提交幂等

## Capabilities

### New Capabilities

- `user-complete-profile`: 用户在微信授权登录后补充手机号、用户名、密码、邮箱等注册资料，完成游客到注册用户的身份转换，包含唯一性校验、密码强度校验、幂等处理与错误码映射

### Modified Capabilities

- `wechat-auth`: 登录成功后新增 `profile_completed` 判断，未补充资料时跳转资料补充页

## Impact

- **数据表**：修改 `user`（补充资料字段、更新 identity/status）；新增 `user_identity_log`
- **API**：新增 2 个端点 `PUT /api/user/profile`（需登录鉴权）、`GET /api/user/phone/exists`（需登录鉴权）
- **缓存**：新增 Redis key `phone:exists:{phone_hash}`（TTL 300s）、`username:exists:{username}`（TTL 300s）、`user:{user_id}`（TTL 1800s）
- **状态机**：触发用户身份状态机 `游客 → 注册用户`
- **前端**：新增小程序 1 个页面（`complete-profile/index`）
- **依赖**：依赖 US-004 微信授权登录；被 US-008、US-009 依赖
- **安全**：手机号 AES-256 加密存储；密码 bcrypt 哈希（cost=12）；接口限流防撞库
