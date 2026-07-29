## Why

游客需要在首页直观感知下周可约时段的释放时间，以便在释放时刻抢约心仪教练。当前系统已有管理员配置预约释放规则的能力（US-015），但游客端缺少倒计时展示，用户不知道何时可以预约，导致错过释放时刻或频繁刷新页面。

PRD §5.3.2 第 4 条明确要求：首页显示释放倒计时，释放前 24 小时显示「距离下周预约开放还有 XX 小时」，释放当天显示「今日 10:00 开放下周预约」。

## What Changes

- 新增 `GET /api/v1/release-countdown` 公开只读接口（无需登录，返回倒计时状态 + 文案 + 服务器时间）
- 新增倒计时计算服务 `ReleaseCountdownService`（根据 `release_rule` + 当前服务器时间计算下次释放时刻、是否显示、文案类型）
- 新增微信小程序首页倒计时组件（每秒本地递减 + 每 10s 拉 API 校准，避免客户端时间漂移）
- 新增 Redis 缓存层（Key `release:countdown`，TTL 10s，频繁更新但避免 DB 空查）
- 仅读取 `release_rule` 表（由 US-015 创建维护），不修改任何数据
- 倒计时窗口：释放前 24 小时至释放时刻；窗口外组件隐藏

## Capabilities

### New Capabilities

- `release-countdown`: 游客在首页查看预约释放倒计时，包含释放规则读取、倒计时计算、API 端点、前端组件、缓存策略

### Modified Capabilities

（无——这是新能力，不修改已有 spec）

## Impact

- **数据表**：读取 `release_rule`（由 US-015 创建）；不修改任何表
- **API**：新增 1 个公开只读端点 `GET /api/v1/release-countdown`
- **缓存**：新增 Redis key `release:countdown`（TTL 10s）
- **前端**：新增小程序组件 `components/release-countdown`
- **依赖**：依赖 US-015（管理员配置释放规则）产出的 `release_rule` 数据；被 US-016（系统自动释放）协作（本 US 仅展示倒计时，不触发释放）；US-023（候补与关注）的订阅消息提醒依赖释放时刻
- **PRD 引用**：§5.1 第 5 条、§5.3.2 第 3-4 条、§5.5.2 第 5 条、§7.2 第 3 条
