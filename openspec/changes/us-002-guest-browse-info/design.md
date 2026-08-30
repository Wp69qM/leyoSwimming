# Design: US-002 游客查看套餐、公告与场馆信息

> 本文档对应 `docs/stories/US-002-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-002 是只读 US，不修改任何实体状态。核心是 3 个公开只读 API + 微信小程序 3 个页面 + Redis 缓存层。

## Data Model

### 读取的表（不修改）

| 表 | 用途 | 关键字段 | PRD 来源 |
|----|------|---------|---------|
| `notice` | 公告 / 通知栏 | `notice_id`, `type`, `priority`, `title`, `content`, `visible_scope`, `start_at`, `end_at` | §9.2.13（已定义字段）|
| `venue` | 场馆主表 | `venue_id`, `name`, `address`, `longitude`, `latitude`, `open_year`, `pool_status`, `image_url` | §5.5.4（字段推导，待 US-047 落地确认）|
| `venue_closure` | 闭馆换水 | `venue_id`, `closure_type`, `start_at`, `end_at`, `reason` | §9.1 ERD 1:N 关系 |
| `package_template` | 套餐模板 | `template_id`, `package_type`, `total_hours`, `price`, `valid_days`, `name`, `status`, `sort_order` | §5.5.3（字段推导，待 US-045 落地确认）|

### 索引

```sql
CREATE INDEX idx_notice_time_priority ON notice(start_at, end_at, priority DESC);
CREATE INDEX idx_venue_closure_time ON venue_closure(venue_id, start_at, end_at);
CREATE INDEX idx_package_template_status ON package_template(status, sort_order);
```

### notice.type 与 visible_scope 语义

| 字段 | 取值 | 业务含义 | 游客可见规则 |
|------|------|---------|-------------|
| `type` | 0=开放 / 1=换水 / 2=释放 / 3=紧急 | 公告类别 | 全部可见 |
| `visible_scope` | all / student / coach | 可见范围 | 仅 `all` 可见 |
| `priority` | 0=普通 / 1=提醒 / 2=紧急 | 排序权重 | 降序 |

### 状态过滤规则

- `package_template`：仅 `status=1`（上架）且 `package_type ∈ {0, 1}`（体验/正式）且 `price > 0` 返回
- `notice`：仅 `visible_scope='all'` 且 `start_at <= NOW() < end_at` 返回
- `venue_closure`：命中当前时间窗才填充 `closureNotice` 字段

## API Design

### GET /api/v1/packages（套餐列表）

- 鉴权：否（游客可访问）
- Query: `type`（可选，`experience` / `standard` / `custom`）
- 排序：`sort_order ASC, package_type ASC`（体验在前）
- 过滤：`status=1` 且 `package_type ∈ {0, 1}`
- Response 200: `{ items: PackageTemplateItem[] }`
- 空列表也返回 200 + `items: []`

### GET /api/v1/announcements（公告列表）

- 鉴权：否
- Query: `limit`（默认 5，最大 10；Repository 将 `limit` 钳制为 `Math.min(limit, 10)`）
- 过滤：`visible_scope='all'` 且 `start_at <= NOW() < end_at`
- 排序：`priority DESC, start_at DESC`
- Response 200: `{ items: AnnouncementItem[] }`（`limit > 10` 仍返回 200，最多 10 条）
- 空列表返回 200 + `items: []`（前端隐藏通知栏区域）

### GET /api/v1/venue（场馆信息）

- 鉴权：否
- `venue` 表无记录 → 404 `VENUE_NOT_CONFIGURED` + message "场馆信息配置中"
- Response 200: 含 `venueId`, `name`, `address`, `longitude`, `latitude`, `openYear`, `poolStatus`, `imageUrl`, `closureNotice`（命中闭馆换水时填充，否则为 null）

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis（套餐）| `packages:list:type:{type}` | 300s | US-045 上下架套餐模板时主动 DEL |
| Redis（公告）| `announcements:list:limit:{limit}` | 60s | US-047/US-048 增删改公告时主动 DEL |
| Redis（场馆）| `venue:info` | 3600s | US-047 修改场馆信息/闭馆换水时主动 DEL |
| 小程序本地 | `Taro.setStorageSync` | stale-while-revalidate | 首次展示缓存 → 后台静默刷新 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 公告列表首屏 P50 | < 100ms |
| 公告列表首屏 P99 | < 300ms |
| 套餐列表首屏 P50 | < 150ms |
| 套餐列表首屏 P99 | < 400ms |
| 场馆信息首屏 P50 | < 100ms |
| 场馆信息首屏 P99 | < 300ms |
| 公告 DB 查询 | < 30ms |
| 并发 1000 QPS P99 | < 800ms |

## Security

- 三个接口均无需登录
- 防刷：同一 IP 1 分钟 > 200 次 → 429 限流
- 防注入：query 参数类型校验（`limit` 必须为 ≥1 整数，大于 10 时由 Repository 钳制为 10；`type` 必须为枚举值）
- 不暴露管理端字段（`created_by` / `updated_at` 等不返回游客）

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-045 | 被依赖 | 管理员配置套餐模板 |
| US-047 | 被依赖 | 管理员配置场馆/公告/闭馆换水 |
| US-048 | 被依赖 | 管理员配置首页通知栏 |
| US-017 | 依赖本 US | 游客从套餐列表购买体验课 |
| US-019 | 依赖本 US | 学员浏览正价套餐（复用套餐查询）|
| US-003 | 依赖本 US | 预约释放倒计时复用公告 type=2 |

## Open Questions

- `package_template` 表完整字段定义：PRD §9.2 未给出，本文档基于 §5.5.3 业务描述推导，最终字段以 US-045 落地为准
- `venue` / `venue_closure` 表完整字段定义：PRD §9.2 未给出，本文档基于 §5.5.4 业务描述推导，最终字段以 US-047 落地为准

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-002-.../tech-design.md` §1 |
| API Design | `docs/stories/US-002-.../tech-design.md` §2 |
| Caching | `docs/stories/US-002-.../tech-design.md` §4 |
| Performance | `docs/stories/US-002-.../tech-design.md` §5 |
| Security | `docs/stories/US-002-.../tech-design.md` §6 |
