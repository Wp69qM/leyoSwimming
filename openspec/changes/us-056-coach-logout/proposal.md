## Why

教练在使用教练端小程序过程中需要能够主动结束当前登录态，清除本地及服务端会话，以保障账号使用安全与隐私。教练端账号体系独立于用户端（操作 `coach` 表与 `coach_session` 表），因此需要独立的退出登录能力声明。

PRD [§5.2.1](../../../docs/prd/prd.md) 要求教练可通过「我的」页面退出当前登录态；退出登录后需清除会话/token，重新登录需重新授权。

## What Changes

- 复用/扩展 `POST /api/v1/auth/logout` 接口，接收当前教练 `access_token` 与 `refresh_token`，使服务端当前教练会话失效
- 修改 `coach_session` 表：退出登录时将当前会话标记为失效（写入 `revoked_at`）或删除记录
- 后端通过 JWT payload 中 `app_type='coach'` 校验，确保只操作教练会话
- 在教练端小程序「我的」TabBar 页面底部新增「退出登录」按钮
- 新增退出登录二次确认弹窗，含「确定」和「取消」两个按钮
- 前端清除本地 `access_token`、`refresh_token`、`expires_in`、`token_expire_at`
- 退出成功后跳转回教练登录页（pages/login/index）
- 触发教练登录态状态机转换：已登录 → 未登录（不修改 coach.status）
- 边界处理：网络异常时前端仍清除本地 token 并跳转回教练登录页；本地 token 已不存在时直接清除残留并跳转回教练登录页

## Capabilities

### New Capabilities

- `coach-logout`: 教练在教练端「我的」页面主动退出登录，包含二次确认、服务端 coach_session 失效、前端 token 清除、跳转回教练登录页

### Modified Capabilities

- （无——本 US 仅新增退出能力，不修改已有登录能力）

## Impact

- **数据表**：修改 `coach_session`（标记当前 session 失效）
- **API**：复用/扩展 1 个写入端点 `POST /api/v1/auth/logout`（需登录鉴权，按 `app_type` 区分会话表）
- **状态机**：触发教练登录态状态机 `已登录 → 未登录`（仅会话层，不修改账号生命周期字段）
- **前端**：教练端小程序「我的」页面新增退出登录按钮与确认弹窗
- **依赖**：前置 US-051（教练微信授权登录）、US-054（教练手机号验证码登录）；无后续 US
- **安全**：退出后 coach refresh_token 必须失效；前端 storage 必须完全清除
