## Why

首页运营是 leyoSwimming 触达用户、引导转化的关键入口。MVP 要求管理员能在后台灵活配置小程序首页的通知栏、Banner 与运营卡片，并按用户身份差异化展示。当前缺少后台运营配置能力，导致首页内容无法运营，公告与活动无法及时触达。

## What Changes

- 复用 `notice` 表管理首页通知栏
- 新增 `homepage_banner` 表，存储 Banner 名称、图片、跳转、可见范围、有效期与状态
- 新增 `homepage_card` 表，存储运营卡片标题、图标、跳转、可见范围、有效期与状态
- 新增 5 个接口：通知栏 CRUD、Banner CRUD、卡片 CRUD、首页预览、公开首页配置
- 字段校验通过后直接写入业务表并即时生效（PRD §5.5.5 #6 / D47：预览机制无需审核直接生效）
- 新增 Redis 缓存层，配置变更时按身份主动失效
- 前端 web-admin 新增「首页运营」页面，小程序首页按身份渲染

## Capabilities

### New Capabilities

- `admin-config-homepage-ops`: 管理员配置首页通知栏、Banner 与运营卡片，并支持实时预览

### Modified Capabilities

（无——本 US 只新增首页运营配置能力）

## Impact

- **数据表**：复用 `notice`；新增 `homepage_banner`、`homepage_card`；新增 `audit_log` 记录
- **API**：新增 5 个接口
- **缓存**：新增 Redis key `homepage:config:{identity}`
- **前端**：新增 web-admin「首页运营」页面；小程序首页渲染逻辑扩展
- **依赖**：依赖 US-004/US-042 产出的身份与权限基础；被 US-002/US-019 依赖
