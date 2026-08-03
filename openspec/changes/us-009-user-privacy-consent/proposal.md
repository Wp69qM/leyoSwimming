## Why

用户/教练隐私协议授权与撤回满足隐私合规要求（GDPR、CCPA、国内《个人信息保护法》），让用户和教练都能查看、同意并撤回隐私授权。这是用户权利保障的核心 US，也是首次注册流程与教练端首次登录流程的必经环节。

PRD [§11.2](../../../docs/prd/prd.md) 要求首次注册需用户同意隐私协议，明确告知数据用途，支持撤回授权；[§5.2.2](../../../docs/prd/prd.md) 要求账号安全中支持隐私协议授权与撤回。US-051 教练端微信授权登录完成后，教练需先完成隐私协议授权方可进入教练端目标页面。

## What Changes

- 新增 `GET /api/privacy-policy/current` 接口（获取当前生效协议，对游客可见）
- 新增 `GET /api/user/privacy/status` 接口（查询用户/教练授权状态，必须登录）
- 新增 `POST /api/user/privacy/consent` 接口（提交同意/撤回，必须登录）
- 新增 `privacy_policy` 表（存储协议版本与内容）
- 新增 `user_privacy_consent` 表（记录用户/教练同意/撤回状态）
- 新增 `audit_log` 表记录同意/撤回审计日志
- 新增 Redis 缓存：`privacy_policy:current`、`user:privacy:{user_id}`
- 新增小程序"隐私协议页"与"隐私设置页"（用户端与教练端可复用同一页面）
- 边界处理：不同意协议阻止进入首页/教练端目标页、协议版本号变化时触发重新授权、重复同意幂等
- 游客态限制：`GET /api/privacy-policy/current` 对游客可见；`GET /api/user/privacy/status` 与 `POST /api/user/privacy/consent` 必须登录，游客调用返回 401

## Capabilities

### New Capabilities

- `user-privacy-consent`: 用户/教练查看当前隐私协议、同意、撤回授权，系统记录版本号与时间，并限制非必要数据收集

### Modified Capabilities

- `user-complete-profile`: 资料补充后触发隐私协议同意流程
- `coach-wechat-login` (US-051): 教练端首次登录后需先完成隐私协议授权，再进入目标页面

## Impact

- **数据表**：新增 `privacy_policy`；新增 `user_privacy_consent`；新增 `audit_log`
- **API**：新增 3 个端点（当前协议、授权状态、同意/撤回）
- **缓存**：新增 Redis key `privacy_policy:current`（TTL 1h）、`user:privacy:{user_id}`（TTL 30min）
- **状态机**：触发隐私授权状态机 `未同意 → 已同意 → 已撤回`
- **前端**：新增小程序"隐私协议页"、"隐私设置页"（用户端与教练端复用）
- **依赖**：依赖 US-004 / US-005 / US-051 用户/教练身份
- **被依赖**：US-051 教练端登录后需先完成本 US 授权
- **安全**：协议内容不可篡改；同意/撤回记录不可删除，仅可追加；撤回后限制非必要数据收集；游客态禁止提交授权变更
