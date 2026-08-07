## Why

当前 MVP 规格中，教练端小程序没有独立的登录/注册入口 US。US-010「教练提交入驻资料」直接假设「教练已完成微信授权登录」，但 US-004「游客微信授权登录」仅描述用户端场景，未覆盖教练端首次打开小程序后的授权、入驻状态判定与页面分流。这导致教练端页面（PRD §13.3 编号 22-35）缺少明确的入口流程，无法进入 Figma 设计与前端开发。

本 US 补全这一缺口：教练在教练端完成微信授权登录后，系统直接查询 `coach` 表并根据 `coach.status` 自动分流到入驻资料页、等待审核页、教练首页、重新提交页或离职处理中页，成为教练端所有功能的前提。

PRD [§5.2.1](../../../docs/prd/prd.md) 要求支持微信授权登录；[§3.1 身份类型](../../../docs/prd/prd.md) 明确教练是独立角色，不与用户身份重叠；[§6.2 教练状态机](../../../docs/prd/prd.md) 定义了教练的入驻状态。PRD [§11.2](../../../docs/prd/prd.md) 要求首次注册需同意隐私协议与用户须知，因此教练端登录时必须校验协议勾选。

## What Changes

- 改造 `POST /api/v1/auth/wechat-login` 接口：新增请求参数 `app_type=coach`、`encryptedData`、`iv`、`terms_accepted`、`privacy_accepted`；响应新增 `is_new_coach`、`coach_status`（-1/0/1/2/3/4），由前端根据状态映射跳转页面。
- `app_type=coach` 时后端直接查询/写入 `coach` 表与 `coach_session` 表，不读、不写、不关联 `user` 表或 `user_session` 表。
- 首次登录且 `coach` 表无记录时，新建 `coach` 记录，`status=-1`（未提交入驻资料），写入手机号、微信昵称（可选）。
- 登录时必须校验 `terms_accepted` 与 `privacy_accepted` 均为 `true`，否则返回 `TERMS_NOT_ACCEPTED`。
- 新增教练端入驻状态查询接口 `GET /api/v1/coach/me/status`（登录态兜底）。
- 新增教练端微信授权登录页（与用户端视觉一致，底部固定「微信一键登录」按钮 + 协议勾选区）。
- 新增登录后按 `coach.status` 跳转的路由守卫/状态分流逻辑。
- 登录成功后前端在 `onShow` 检查 token：`access_token` 过期用 `refresh_token` 刷新，`refresh_token` 过期重新登录。
- 本 US 不修改 `coach.status`，仅读取并返回用于分流；`coach.status` 的状态转换仍由 US-010 / US-011 / US-039 / US-040 / US-041 负责。
- 边界处理：已离职教练（`coach.status=3`）按未命中处理，重新注册生成新教练账号，status=-1，不绑定旧数据。

## Capabilities

### New Capabilities

- `coach-wechat-login`: 教练在教练端小程序通过微信授权登录，系统直接查 `coach` 表并返回 `coach.status`，由前端映射跳转目标页，实现入驻状态分流。

### Modified Capabilities

- `wechat-auth` (US-004): 扩展 `POST /api/v1/auth/wechat-login` 接口，新增 `app_type` 参数与教练端专属响应字段 `is_new_coach`/`coach_status`，使其同时服务于用户端和教练端，但两端的账号数据独立。

## Impact

- **数据表**：`coach` 表读取/新增，`coach_session` 表新增；教练端不再使用 `user` 表或 `user_session` 表。
- **API**：改造 1 个端点 `POST /api/v1/auth/wechat-login`（新增可选参数与响应字段）；新增 1 个端点 `GET /api/v1/coach/me/status`（需登录鉴权）。
- **缓存**：复用 US-004 的 Redis 幂等缓存策略（`auth:idempotent:wechat-login:{code}`）；`coach:session_key:{coach_id}` 可选缓存 TTL 7200s。
- **状态机**：不触发用户身份状态机；本 US 仅初始化 `coach.status = -1`（首次登录无记录时），不触发其他教练状态机转换。
- **前端**：新增教练端小程序 1 个页面（登录页）+ 登录态路由守卫 + token 刷新/重登逻辑。
- **依赖**：前置 US-009（用户/教练隐私协议与用户须知授权）；被 US-010 / US-011 / US-012 / US-039 / US-040 / US-041 依赖。
- **安全**：`session_key` 不返回前端；`app_type` 白名单校验仅允许 `user`/`coach`；协议勾选后端二次校验。
