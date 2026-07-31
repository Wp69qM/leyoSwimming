## Why

用户账号注销满足用户删除账号的合规诉求（《个人信息保护法》用户权利），同时确保历史订单、交易记录等数据落档保留，避免法律与财务风险。本 US 将账号标记为注销状态并清除登录态。

PRD [§5.2.1](../../../docs/prd/prd.md) 要求账号注销后进入软删除状态，无法继续登录；[§3.7](../../../docs/prd/prd.md) 要求历史数据保留 90 天后匿名化；[§11.2](../../../docs/prd/prd.md) 要求支持隐私协议撤回。

## What Changes

- 新增 `GET /api/user/account/cancel/check` 接口（查询是否满足注销条件）
- 新增 `POST /api/user/account/cancel` 接口（提交注销申请）
- 修改 `user` 表：`status = 2`（已注销）、`deleted_at` 赋值
- 新增 `audit_log` 表记录注销审计日志
- 删除 `user_session` 中该用户所有登录态
- 新增 Redis 缓存失效：`cancel:check:{user_id}`、`session:{token}`
- 新增小程序"注销确认页"
- 边界处理：存在 active 套餐/未完成订单拒绝注销、二次验证、重复提交幂等

## Capabilities

### New Capabilities

- `user-account-deletion`: 用户提交账号注销申请，系统校验无 active 套餐/未完成订单、完成二次验证后软删除账号、清除会话、记录审计日志

### Modified Capabilities

- `user-privacy-consent`: 注销时同步撤回隐私授权

## Impact

- **数据表**：修改 `user`（注销字段）；新增 `audit_log`；删除 `user_session` 记录
- **API**：新增 2 个端点 `GET /api/user/account/cancel/check`、`POST /api/user/account/cancel`
- **缓存**：新增 Redis key `cancel:check:{user_id}`（TTL 60s）；注销后清除所有 `session:{token}`
- **状态机**：触发用户账号状态机 `正常(1) → 已注销(2)`
- **前端**：新增小程序"注销确认页"
- **依赖**：依赖 US-004 登录态；与 US-020/US-025 等订单/套餐状态联动
- **安全**：必须二次验证（密码或验证码）；记录审计日志；按数据保留策略落档保留（课程记录 2 年、套餐/订单 5 年、个人身份信息脱敏）
