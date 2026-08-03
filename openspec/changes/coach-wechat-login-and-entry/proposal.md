## Why

当前 MVP 规格中，教练端小程序没有独立的登录/注册入口 US。US-010「教练提交入驻资料」直接假设「教练已完成微信授权登录」，但 US-004「游客微信授权登录」仅描述用户端场景，未覆盖教练端首次打开小程序后的授权、入驻状态判定与页面分流。这导致教练端页面（PRD §13.3 编号 22-35）缺少明确的入口流程，无法进入 Figma 设计与前端开发。

本 US 补全这一缺口：教练在教练端完成微信授权登录后，系统根据其 `coach.status` 自动分流到入驻资料页、等待审核页、教练首页、重新提交页或离职处理中页，成为教练端所有功能的前提。

PRD [§5.2.1](../../../docs/prd/prd.md) 要求支持微信授权登录；[§3.1 身份类型](../../../docs/prd/prd.md) 明确教练是独立角色，不与用户身份重叠；[§6.2 教练状态机](../../../docs/prd/prd.md) 定义了教练的 5 种状态（0=待审核、1=已通过、2=已驳回、3=已离职、4=离职中）。PRD [§11.2](../../../docs/prd/prd.md) 要求首次注册需同意隐私协议，因此教练端首次登录后、进入目标页面前需先完成 US-009 隐私协议授权。

## What Changes

- 改造 `POST /api/v1/auth/wechat-login` 接口：新增请求参数 `app_type=coach`，响应新增 `coach_status`（null/0/1/2/3/4）与 `redirect_page` 枚举字段
- 新增教练端入驻状态查询接口 `GET /api/v1/coach/me/status`（登录态兜底）
- 新增教练端微信授权登录页（与用户端视觉一致，底部固定「微信一键登录」按钮）
- 新增登录后按 `coach.status` 跳转的路由守卫/状态分流逻辑
- 登录成功后增加隐私协议校验：未同意当前生效隐私协议时，先跳转 US-009 隐私协议页；已同意则按 `redirect_page` 跳转
- 复用 `user` 表基础账号：同一 `union_id` 在用户端和教练端共享 `user` 记录，教练资质状态独立保存在 `coach` 表
- 本 US 不修改 `coach.status`，仅读取并返回用于分流；`coach.status` 的状态转换仍由 US-010 / US-011 / US-039 / US-040 / US-041 负责
- 边界处理：已注销账号（`user.status=1`）按 PRD §5.2.1 第 4 条新建账号，不绑定原数据

## Capabilities

### New Capabilities

- `coach-wechat-login`: 教练在教练端小程序通过微信授权登录，系统按 `coach.status` 返回 `redirect_page`，实现入驻状态分流

### Modified Capabilities

- `wechat-auth` (US-004): 扩展 `POST /api/v1/auth/wechat-login` 接口，新增 `app_type` 参数与 `coach_status`/`redirect_page` 响应字段，使其同时服务于用户端和教练端

## Impact

- **数据表**：`user` 表读取/新增（同 US-004），`coach` 表读取（按 `union_id` 查询 `status`），`user_session` 表新增
- **API**：改造 1 个端点 `POST /api/v1/auth/wechat-login`（新增可选参数与响应字段）；新增 1 个端点 `GET /api/v1/coach/me/status`（需登录鉴权）
- **缓存**：复用 US-004 的 Redis 缓存策略（`session_key`、`auth:idempotent:wechat-login:{code}`）
- **状态机**：触发用户身份状态机 `游客 → 注册用户`（首次登录且 user 表无记录时）；不触发教练状态机转换
> **隐私协议前置**：教练端首次登录成功后、进入目标页面前需先完成 US-009 隐私协议授权；用户端已同意的隐私协议版本对教练端同样有效。

- **前端**：新增教练端小程序 1 个页面（登录页）+ 登录态路由守卫 + 隐私协议前置校验
- **依赖**：前置 US-009（用户/教练隐私协议授权与撤回）；被 US-010 / US-011 / US-012 / US-039 / US-040 / US-041 依赖
- **安全**：`session_key` 不返回前端；`app_type` 白名单校验仅允许 `user`/`coach`
