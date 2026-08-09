## Why

用户在使用小程序过程中需要能够主动结束当前登录态，清除本地及服务端会话，以保障账号使用安全与隐私。原 US-008「用户账号安全设置」因 MVP 阶段不涉及密码修改、安全设置、登录设备管理等功能，已被取消，由本 US 替代其在注册登录能力中的位置。

PRD [§5.2.1](../../../docs/prd/prd.md) 要求用户可通过「我的」页面退出当前登录态；退出登录后需清除会话/token，重新登录需重新授权。

## What Changes

- 新增 `POST /api/user/auth/logout` 接口，接收当前 `access_token` 与 `refresh_token`，使服务端当前会话失效
- 修改 `user_session` 表：退出登录时将当前会话标记为失效（写入 `revoked_at`）或删除记录
- 在小程序「我的」TabBar 页面底部新增「退出登录」按钮
- 新增退出登录二次确认弹窗，含「确定」和「取消」两个按钮
- 前端清除本地 `access_token`、`refresh_token`、`expires_in`、`token_expire_at`
- 退出成功后停留在「我的」页面，刷新页面为未登录态（展示游客可见内容）
- 触发登录态状态机转换：已登录 → 未登录（不修改 user.status/identity_status）
- 边界处理：网络异常时前端仍清除本地 token 并刷新为未登录态；本地 token 已不存在时直接清除残留并刷新为未登录态

## Capabilities

### New Capabilities

- `user-logout`: 用户在「我的」页面主动退出登录，包含二次确认、服务端 session 失效、前端 token 清除、刷新页面为未登录态

### Modified Capabilities

- （无——本 US 仅新增退出能力，不修改已有登录能力）

## Impact

- **数据表**：修改 `user_session`（标记当前 session 失效）
- **API**：新增 1 个写入端点 `POST /api/user/auth/logout`（需登录鉴权）
- **状态机**：触发登录态状态机 `已登录 → 未登录`（仅会话层，不修改账号生命周期字段）
- **前端**：小程序「我的」页面新增退出登录按钮与确认弹窗
- **依赖**：前置 US-004（微信授权登录）、US-006（手机号验证码登录）；无后续 US
- **安全**：退出后 refresh_token 必须失效；前端 storage 必须完全清除
