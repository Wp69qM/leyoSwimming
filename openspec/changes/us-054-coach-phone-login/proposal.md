## Why

教练端在 MVP 中已通过微信授权登录（US-051）建立入驻状态分流机制，但部分教练可能未绑定微信手机号、需要在多设备登录或切换账号。为覆盖这些场景，需要为教练端补充手机号验证码登录能力。该能力复用教练端独立的 `coach` 表和 `coach_session` 会话体系，登录成功后按 `coach.status` 自动分流到入驻资料页 / 等待审核页 / 教练首页 / 重新入驻页 / 离职处理中页，与 US-051 保持一致的教练端入口体验。

## What Changes

- 在教练端登录页增加「手机号登录」入口，点击后进入独立手机号登录页。
- 新增教练端短信验证码发送接口 `POST /api/common/sms/send`。
- 新增教练端手机号验证码登录接口 `POST /api/coach/auth/phone-login`。
- 登录成功后后端按手机号查询/创建 `coach` 记录，签发 JWT，返回 `coach_status`。
- 未注册手机号首次登录自动创建 `coach` 记录，`coach.status = -1`（未提交入驻资料）。
- 已离职手机号（`status = 3`）登录时复用原 coach 记录，登录后按 `coach_status=3` 跳转 US-010 重新入驻资料填写页，原账号历史数据保留。
- 前端按 `coach_status` 跳转，分流规则与 US-051 完全一致。
- 登录页与登录接口均需校验《用户须知》和《隐私协议》勾选。

## Capabilities

### New Capabilities
- `coach-phone-login`: 教练端手机号验证码登录，包括短信发送、验证码校验、自动注册、按 coach.status 分流。

### Modified Capabilities
- 无（本 US 不修改现有 capability 的需求行为，仅新增独立 capability）。

## Impact

- 前端：教练端登录页增加「手机号登录」入口；新增教练端手机号登录页。
- 后端：新增 2 个 API；复用 `coach` 表、`coach_session` 表与 JWT 签发逻辑。
- 数据库：新增/读取 `coach`、`coach_session`、`sms_code`、`coach_login_log` 记录。
- 短信服务：需接入短信服务商发送验证码。
- 依赖：US-009（隐私协议校验）、US-051（教练端账号体系与状态分流）。
