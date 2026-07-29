## Why

用户微信授权登录是用户进入小程序交易流程的入口。当前系统无任何用户身份识别能力，用户无法使用受登录态保护的功能（购买套餐、预约教练、上课记录）。本 US 是注册登录模块的第 1 个故事，是用户身份状态机的起点（游客→注册用户），也是后续所有业务的前置条件。

PRD [§5.2.1](../../../docs/prd/prd.md) 要求支持微信授权登录，首次登录后补充手机号、用户名、密码、邮箱；[§4](../../../docs/prd/prd.md) 定义了用户身份状态机：游客 → 注册用户（通过微信授权登录）→ 学员（购买正价套餐后）。

## What Changes

- 新增 `POST /api/v1/auth/wechat-login` 接口（接收微信 `code`，调用 `code2session`，查询/创建用户，签发 JWT）
- 新增 `user` 表写入：首次登录时创建用户记录（`openid`、`union_id`、`identity_status='注册用户'`、`profile_completed=false`、`status='active'`）
- 新增 `user_session` 表写入：会话管理（`session_key` 加密存储、`refresh_token_hash`、`expires_at`）
- 新增 JWT 签发逻辑：`access_token`（2h）+ `refresh_token`（7d）
- 新增 Redis 缓存：`session_key`（TTL 7200s）、登录幂等键（`code` 5 分钟内有效）
- 新增微信小程序登录页（含拒绝授权处理、错误文案、按 `is_new_user`/`profile_completed` 跳转）
- 触发用户身份状态机转换：**游客 → 注册用户**（首次登录新建用户记录时）
- 边界处理：`union_id` 命中已注销账号（`status='deleted'`）时新建账号，不绑定原数据（PRD §5.2.1 第 4 条）

## Capabilities

### New Capabilities

- `wechat-auth`: 用户通过微信授权登录小程序，包含 code2session 调用、用户查询/创建、JWT 签发、状态机转换（游客→注册用户）、幂等处理、错误码映射

### Modified Capabilities

（无——这是注册登录模块的首个 US，不修改已有 spec）

## Impact

- **数据表**：新增写入 `user`（首次登录时 INSERT）、新增写入 `user_session`（每次登录 INSERT）
- **API**：新增 1 个写入端点 `POST /api/v1/auth/wechat-login`（无需登录鉴权）
- **缓存**：新增 Redis key `wechat:session_key:{user_id}`（TTL 7200s）、`auth:idempotent:wechat-login:{code}`（TTL 300s）
- **状态机**：触发用户身份状态机 `游客 → 注册用户`（本 US 是该状态机的起点）
- **前端**：新增小程序 1 个页面（`login/index`）
- **依赖**：无前置 US（本 US 是用户身份流程起点）；被 US-005~US-009、US-017、US-020 依赖
- **安全**：`session_key` 不返回前端；`refresh_token` 存储 SHA-256 hash；JWT HS256 签名
