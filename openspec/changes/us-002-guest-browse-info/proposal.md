## Why

游客通过查看套餐价格、场馆公告与场馆信息，形成对场馆的完整认知，是注册与购买转化的前置信息铺垫。当前系统已有管理端配置能力（US-045 配置套餐模板、US-047 配置场馆/公告、US-048 配置通知栏），但游客端缺少公开的套餐、公告、场馆信息展示页面，无法将管理端产出的数据转化为获客入口。

## What Changes

- 新增 `GET /api/v1/packages` 公开套餐列表接口（无需登录，返回上架的体验/正式套餐模板，按 sort_order + package_type 排序）
- 新增 `GET /api/v1/announcements` 公开公告列表接口（无需登录，返回 visible_scope=all 且时间窗内的公告，按 priority 降序）
- 新增 `GET /api/v1/venue` 公开场馆信息接口（无需登录，返回场馆名称/地址/经纬度/开业年限/泳池状态 + 当前闭馆换水提示）
- 新增微信小程序首页通知栏（最多 5 条公告垂直轮播）+ 套餐列表页 + 场馆信息页
- 新增 Redis 缓存层（套餐列表 TTL 300s / 公告 TTL 60s / 场馆信息 TTL 3600s）
- 仅展示 `package_template.status = 1`（上架）且 `package_type ∈ {0 体验, 1 正式}` 的模板；自定义套餐（type=2）不在列表返回
- 仅展示 `notice.visible_scope = 'all'`（游客可见）且 `start_at <= NOW() < end_at` 的公告
- 场馆未配置时返回 404 + `VENUE_NOT_CONFIGURED`，前端展示占位文案

## Capabilities

### New Capabilities

- `guest-info-browsing`: 游客查看套餐、公告与场馆信息，包含公开套餐列表查询、生效中公告查询、场馆信息查询（含闭馆换水提示）、空状态处理

### Modified Capabilities

（无——US-002 是独立的新能力，不修改已有 spec）

## Impact

- **数据表**：读取 `notice`（公告，PRD §9.2.13）、`venue`（场馆）、`venue_closure`（闭馆换水）、`package_template`（套餐模板，字段待 US-045 落地确认）；不修改任何表
- **API**：新增 3 个公开只读端点
- **缓存**：新增 Redis key `packages:list:type:*`、`announcements:list:limit:*`、`venue:info`
- **前端**：新增小程序 3 个页面（首页通知栏 + 套餐列表页 + 场馆信息页）
- **依赖**：依赖 US-045（套餐模板配置）/ US-047（场馆公告配置）/ US-048（通知栏配置）产出的数据；被 US-017（购买体验课）/ US-019（学员浏览正价套餐）/ US-003（预约释放倒计时）依赖
