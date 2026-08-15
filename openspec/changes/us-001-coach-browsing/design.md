# Design: US-001 游客浏览教练列表与详情

> 本文档对应 `docs/stories/US-001-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-001 是只读 US，不修改任何实体状态。核心是 2 个公开 API + 2 个小程序页面 + Redis 缓存层。

## Data Model

### 读取的表（不修改）

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `coach` | 列表 + 详情主表 | 列表：`id`, `name`, `status`, `rating`, `years_of_teaching`, `teaching_strokes`, `real_time_status`；详情额外读取 `gender`, `age`, `avatar_url`, `total_students`, `total_hours`, `bio`, `reference_price`, `phone`, `wechat_qr_url` |
| `coach_certificate` | 详情页证书 | `coach_id`, `name`, `level` |
| `coach_review` | 详情页评价 | `coach_id`, `rating`, `content`, `created_at` |
| `coach_availability` | 详情页可约时间 | `coach_id`, `date`, `start_time`, `end_time` |

### 索引

```sql
CREATE INDEX idx_coach_status_rating ON coach(status, rating DESC);
```

### status 可见性规则

| status | 含义 | 公开可见 | UI 标签 |
|--------|------|---------|--------|
| 0 | 待审核 | ❌ | — |
| 1 | 已通过 | ✅ | 展示实时状态徽标 |
| 2 | 驳回 | ❌ | — |
| 3 | 已离职 | ❌ | — |
| 4 | 申请离职中 | ✅ | 不展示任何状态标签 |

## API Design

### POST /api/coach/list（列表）

- 鉴权：否（游客可访问）
- 幂等：是
- Request Body:
  - `page`（int，默认 1）
  - `pageSize`（int，默认 10，最大 50）
- 排序：`rating DESC`
- 过滤：`status IN (1, 4)`（status=0/2/3 不返回；status=4 不展示任何状态标签）
- Response 200: `{ items: CoachListItem[], total, page, pageSize }`
- CoachListItem 字段：`id`, `name`, `status`, `avatar`, `rating`, `yearsOfTeaching`, `teachingStrokes`, `realTimeStatus`
- 空列表也返回 200 + `items: []`

### POST /api/coach/detail（详情）

- 鉴权：否
- 幂等：是
- Request Body:
  - `coachId`（int，必填）
- `coachId` 不存在 / status=0（待审核）/ status=2（驳回）/ status=3（已离职） → 404 `COACH_NOT_FOUND`
- status=1（已通过）或 status=4（申请离职中） → 200；status=4 不展示任何状态标签
- Response 200: 含 `id`, `name`, `gender`, `age`, `status`, `avatar`, `rating`, `yearsOfTeaching`, `totalStudents`, `totalHours`, `teachingStrokes`, `bio`, `referencePrice`, `contact`, `certificates`, `packages`, `reviews`, `availableTimes`, `realTimeStatus`
  - `packages` 默认最多 3 项：用户具备体验课购买资格时第 1 项为体验课套餐，其余为标准套餐；不具备资格时全部为标准套餐
  - `availableTimes` 展示本周（周一至周日）每天的可约时段

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis（列表）| `coaches:list:page:{page}:pageSize:{pageSize}` | 60s | coach.status / real_time_status 变更时 MQ 广播失效 |
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
| US-018 | 依赖本 US | 从详情页跳转购买标准套餐 |
| US-029 | 依赖本 US | "立即预约"按钮跳转预约页 |

## Open Questions

- status=4 的教练在详情页是否允许展示"立即预约"按钮？当前由产品/设计在页面级设计决策中另行规定，本 US 实现仅保证状态标签不展示。

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-001-.../tech-design.md` §1 |
| API Design | `docs/stories/US-001-.../tech-design.md` §2 |
| Caching | `docs/stories/US-001-.../tech-design.md` §4 |
| Performance | `docs/stories/US-001-.../tech-design.md` §5 |
| Security | `docs/stories/US-001-.../tech-design.md` §6 |
