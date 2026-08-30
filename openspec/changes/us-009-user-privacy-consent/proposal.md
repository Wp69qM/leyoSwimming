> **OpenSpec Proposal | 映射自 `docs/stories/US-009-用户-隐私协议与用户须知授权/user-story.md` §1-§5**

## Why

满足隐私合规与产品告知要求。在用户端/教练端登录环节强制用户/教练同意《用户须知》和《隐私协议》，登录后支持在「我的 → 设置」中查看协议内容，但不支持撤回授权。

PRD [§11.2](../../../docs/prd/prd.md) 要求首次注册/登录需用户同意隐私协议与用户须知，明确告知数据用途；[§5.2.2](../../../docs/prd/prd.md) 要求账号安全中支持查看隐私协议与用户须知。

## What Changes

- 新增 `POST /api/common/terms/current` 接口（获取当前生效《用户须知》，对游客可见）
- 新增 `POST /api/common/privacy/current` 接口（获取当前生效《隐私协议》，对游客可见）
- 新增 `POST /api/user/terms/status` 接口（查询用户/教练《用户须知》同意状态，必须登录）
- 新增 `POST /api/user/privacy/status` 接口（查询用户/教练《隐私协议》同意状态，必须登录）
- 新增 `POST /api/user/terms/consent` 接口（记录《用户须知》同意，必须登录）
- 新增 `POST /api/user/privacy/consent` 接口（记录《隐私协议》同意，必须登录）
- `terms_policy` 表（存储《用户须知》版本与内容，由 US-047 管理写入）
- `privacy_policy` 表（存储《隐私协议》版本与内容，由 US-047 管理写入）
- `user_terms_consent` 表（记录用户/教练同意的《用户须知》版本号与时间）
- `user_privacy_consent` 表（记录用户/教练同意的《隐私协议》版本号与时间）
- 新增 `audit_log` 表记录同意审计日志
- 新增 Redis 缓存：`terms_policy:current`、`privacy_policy:current`、`user:terms:{user_id}`、`user:privacy:{user_id}`
- 新增登录页协议浮层弹窗（双 Tab：《用户须知》/《隐私协议》，底部「同意」「不同意」按钮）
- 新增「我的 → 设置 → 用户须知」与「我的 → 设置 → 隐私协议」查看页（无撤回按钮）
- 登录页协议勾选由 US-004 / US-006 / US-051 承载，本 US 提供协议内容、同意记录与查看能力

## Capabilities

### New Capabilities

- `user-privacy-consent`: 用户/教练查看当前《用户须知》和《隐私协议》、在登录页通过浮层同意、系统记录版本号与时间；登录后仅支持查看，不支持撤回

### Modified Capabilities

- `wechat-auth-login` (US-004): 登录页需展示协议勾选区与浮层弹窗，登录成功后调用本 US 接口记录同意版本
- `user-phone-code-login` (US-006): 登录页需展示协议勾选区与浮层弹窗，登录成功后调用本 US 接口记录同意版本
- `coach-wechat-login` (US-051): 登录页需展示协议勾选区与浮层弹窗，登录成功后调用本 US 接口记录同意版本
- `coach-phone-code-login` (US-054): 登录页需展示协议勾选区与浮层弹窗，登录成功后调用本 US 接口记录同意版本

## Impact

- **数据表**：新增 `terms_policy`、`privacy_policy`、`user_terms_consent`、`user_privacy_consent`；新增 `audit_log`
- **API**：新增 6 个端点（当前用户须知、当前隐私协议、两个状态查询、两个同意记录）
- **缓存**：新增 Redis key `terms_policy:current`、`privacy_policy:current`（TTL 1h）、`user:terms:{user_id}`、`user:privacy:{user_id}`（TTL 30min）
- **状态机**：仅存在「未同意 → 已同意」转换，无撤回状态
- **前端**：新增登录页协议浮层弹窗；新增「我的 → 设置」中的用户须知/隐私协议查看页
- **依赖**：依赖 US-004 / US-006 / US-051 / US-054 登录身份
- **被依赖**：US-051 / US-054 教练端登录后需校验本 US 同意状态
- **安全**：协议内容不可篡改；同意记录不可删除；游客态禁止提交授权变更
