## Why

用户账号安全设置让用户能够管理手机号、密码、邮箱及登录设备，保障账号安全。这是注册登录模块的收尾 US，支撑用户长期使用中的安全诉求（换手机号、改密码、设备管理）。

PRD [§5.2.2](../../../docs/prd/prd.md) 要求支持手机号换绑、密码修改、邮箱绑定、登录设备管理；[§11.1](../../../docs/prd/prd.md) 要求手机号、密码加密存储；[§11.4](../../../docs/prd/prd.md) 要求敏感操作记录审计日志。

## What Changes

- 新增 `PUT /api/user/security/phone` 接口（换绑手机号）
- 新增 `PUT /api/user/security/password` 接口（修改密码）
- 新增 `PUT /api/user/security/email` 接口（绑定/换绑邮箱）
- 新增 `GET /api/user/security/devices` 接口（获取登录设备列表）
- 新增 `DELETE /api/user/security/devices/{id}` 接口（下线指定设备）
- 修改 `user` 表：`phone`、`password_hash`、`email`、`phone_changed_at`
- 修改/删除 `user_session` 表：下线设备时删除对应 token
- 新增 `audit_log` 表记录换绑/改密/设备下线
- 新增 Redis 缓存：`user:{user_id}`、`session:{token}`、`phone_change:limit:{user_id}`
- 新增小程序"账号安全页"与"设备管理页"
- 边界处理：24 小时内只能换绑 1 次手机号、下线设备后立即失效 token

## Capabilities

### New Capabilities

- `user-account-security`: 用户管理手机号、密码、邮箱与登录设备，包含二次验证、频率限制、设备下线、审计日志

### Modified Capabilities

- `user-phone-password-login`: 换绑手机号/修改密码后登录凭据变化
- `user-complete-profile`: 补充时设置初始手机号/密码/邮箱

## Impact

- **数据表**：修改 `user`；修改/删除 `user_session`；新增 `audit_log`
- **API**：新增 5 个端点（换绑手机、修改密码、邮箱、设备列表、下线设备）
- **缓存**：新增 Redis key `phone_change:limit:{user_id}`（TTL 24h）；变更后清除 `user:{user_id}` 与 `session:{token}`
- **状态机**：无身份状态变化，仅会话状态转换（有效 → 失效）
- **前端**：新增小程序"账号安全页"、"设备管理页"
- **依赖**：依赖 US-005、US-006
- **安全**：敏感操作必须二次验证；记录审计日志；换绑手机号限流 1 次/24 小时；下线设备立即失效 token
