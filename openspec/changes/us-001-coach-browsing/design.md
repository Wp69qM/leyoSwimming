# Design: US-001 游客浏览教练列表与详情

> 本文档对应 `docs/stories/US-001-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-001 是只读 US，不修改任何实体状态。核心是 2 个公开 API + 2 个小程序页面 + Redis 缓存层。

## Data Model

### 读取的表（不修改）

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `coach` | 列表 + 详情主表 | `id`, `name`, `status`, `rating`, `years_of_teaching`, `real_time_status` |
| `coach_certificate` | 详情页证书 | `coach_id`, `name`, `level` |
| `coach_review` | 详情页评价 | `coach_id`, `rating`, `content`, `created_at` |
| `coach_availability` | 详情页可约时间 | `coach_id`, `date`, `start_time`, `end_time` |

### 索引

```sql
CREATE INDEX idx_coach_status_rating ON coach(status, rating DESC);
```

### status 可见性规则

| status | 含义 | 公开可见 |
|--------|------|---------|
| 0 | 申请中 | ❌ |
| 1 | 在职 | ✅ |
| 2 | 休息 | ✅ |
| 3 | 离职审批中 | ❌（保守处理，避免法律风险）|
| 4 | 已离职 | ❌ |

## API Design

### GET /api/v1/coaches（列表）

- 鉴权：否（游客可访问）
- Query: `page`（默认 1）、`size`（默认 10，最大 50）
- 排序：`rating DESC`
- 过滤：`status = 1`
- Response 200: `{ items: CoachListItem[], total, page, size }`
- 空列表也返回 200 + `items: []`

### GET /api/v1/coaches/:id（详情）

- 鉴权：否
- `id` 不存在 / status=0 / status=4 → 404 `COACH_NOT_FOUND`
- Response 200: 含 certificates、reviews、availableTimes、realTimeStatus

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis（列表）| `coaches:list:page:{page}:size:{size}` | 60s | coach.status / real_time_status 变更时 MQ 广播失效 |
| Redis（详情）| `coach:detail:{id}` | 300s | coach / certificate / review / availability 变更时失效 |
| 小程序本地 | `Taro.setStorageSync('coaches_list')` | 首次进入先展示缓存 → 后台静默刷新 | — |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 列表页首屏 P50 | < 300ms |
| 列表页首屏 P99 | < 800ms |
| 详情页首屏 P50 | < 200ms |
| 详情页首屏 P99 | < 500ms |
| 列表页 DB 查询 | < 50ms |
| 并发 1000 QPS P99 | < 1s |

## Security

- 两个接口均无需登录
- 防刷：同一 IP 1 分钟 > 200 次 → 429 限流
- 防 ID 遍历：统一返回 404（不暴露是否存在）

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-010~US-013 | 被依赖 | 教练入驻流程产出数据 |
| US-017 | 依赖本 US | 从详情页跳转购买体验课 |
| US-029 | 依赖本 US | "立即预约"按钮跳转预约页 |

## Open Questions

- status=3（离职审批中）是否公开？当前按"不公开"处理，待 PM 确认。

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-001-.../tech-design.md` §1 |
| API Design | `docs/stories/US-001-.../tech-design.md` §2 |
| Caching | `docs/stories/US-001-.../tech-design.md` §4 |
| Performance | `docs/stories/US-001-.../tech-design.md` §5 |
| Security | `docs/stories/US-001-.../tech-design.md` §6 |
