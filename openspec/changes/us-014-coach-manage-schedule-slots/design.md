# Design: US-014 教练管理可约时段

> 本文档对应 `docs/stories/US-014-教练-管理可约时段/tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-014 让通过入驻审核的教练管理未来可约时段，支持添加、修改、删除与批量复制上周排班。核心是 5 个 REST API + `schedule_slot` 表 + 冲突/过去时间/已预约校验 + 缓存失效策略。

## Data Model

### 新增/修改的表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `schedule_slot` | INSERT/UPDATE/DELETE | `slot_id`、`coach_id`、`start_time`、`end_time`、`status`、`created_at`、`updated_at` |
| `coach_availability_template` | INSERT/UPDATE | `template_id`、`coach_id`、`day_of_week`、`start_time`、`end_time`、`status`、`created_at`、`updated_at` |
| `coach_schedule_log` | INSERT | `log_id`、`coach_id`、`slot_id`、`action`、`from_value`、`to_value`、`created_at` |
| `booking` | SELECT | 判断时段是否已被预约 |
| `venue_closure` | SELECT | 复制上周排班时跳过闭馆日期 |

### status 字段命名（v3 评审 P0 修复）

| 表 | status 取值 | 说明 |
|----|------------|------|
| `schedule_slot` | `available` / `booked` / `closed` / `hidden` | 时段实例状态 |
| `coach_availability_template` | `enabled` / `disabled` | 模板启用/禁用，**避免与 schedule_slot.status 的 `available` 语义冲突** |

> 模板表 status 使用 `enabled / disabled`，与 slot 表的 `available / booked / closed / hidden` 区分清晰。

### 索引

```sql
-- 时段唯一性索引
CREATE UNIQUE INDEX uk_coach_start_time ON schedule_slot(coach_id, start_time);

-- 教练时段查询索引
CREATE INDEX idx_schedule_slot_coach_time ON schedule_slot(coach_id, start_time, status);

-- 排班日志查询索引
CREATE INDEX idx_coach_schedule_log_coach_id ON coach_schedule_log(coach_id);
```

## API Design

### GET /api/coach/schedule-slots

- 鉴权：是（需教练登录态且 `coach.status = 1`）
- Request query: `start_date`、`end_date`
- Response 200: `{ slots: [{ slot_id, start_time, end_time, status }] }`

### POST /api/coach/schedule-slots

- 鉴权：是（需教练登录态且 `coach.status = 1`）
- Request: `{ slots: [{ start_time, end_time }] }`
- Response 201: `{ created: number, conflicts: [] }`
- Response 400: `PAST_TIME_NOT_ALLOWED`
- Response 409: `SLOT_TIME_CONFLICT`

### PUT /api/coach/schedule-slots/{id}

- 鉴权：是（需教练登录态且 `coach.status = 1`）
- Request: `{ start_time, end_time }`
- Response 200: `{ slot_id, start_time, end_time, status }`
- Response 404: `SLOT_NOT_FOUND`（非本教练时段）
- Response 400: `PAST_TIME_NOT_ALLOWED`
- Response 409: `SLOT_TIME_CONFLICT`

### DELETE /api/coach/schedule-slots/{id}

- 鉴权：是（需教练登录态且 `coach.status = 1`）
- Response 204
- Response 404: `SLOT_NOT_FOUND`
- Response 409: `SLOT_HAS_BOOKING`

### POST /api/coach/schedule-slots/copy-last-week

- 鉴权：是（需教练登录态且 `coach.status = 1`）
- Request: `{ target_week_start: 'YYYY-MM-DD' }`
- Response 200: `{ created: number, skipped: number, conflicts: [{ slot_id, start_time, reason }] }`

## State Machine

### 时段状态机

```
无 ──[教练发布]──→ available ──[学员预约成功]──→ booked
  │                    │
  │                    └──[教练关闭]──→ closed
  │                    │
  │                    └──[管理员隐藏]──→ hidden
```

本 US 触发：`无 → available`。

## Caching

| 层 | Key | TTL | 用途 | 失效策略 |
|----|-----|-----|------|---------|
| Redis | `coach:slots:{coach_id}:{date}` | 5 分钟 | 学员端展示教练某日可约时段 | 增删改/复制后立即失效 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 批量添加接口 P99（≤ 50 个时段） | < 500ms |
| 学员端可约时段查询 P99 | < 200ms |
| 复制上周排班接口 P99 | < 1000ms |

## Security

- 所有教练端排班接口必须教练登录鉴权且 `coach.status = 1`
- 删除/修改操作校验教练对 `slot_id` 的所有权
- 已预约时段禁止删除，避免学员权益受损
- 操作记录审计日志 `coach_schedule_log`
- 手动修改/删除由 US-016 自动释放的时段时，需同步调整自动释放队列，避免重复释放或释放已删除模板时段

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-011 | 依赖 | 管理员审核通过后 coach.status = 1 |
| US-016 | 被依赖 | 系统自动释放下周可约时段依赖 schedule_slot 表结构 |
| US-018/US-029 | 被依赖 | 学员预约课程依赖 available 时段 |
| US-023 | 被依赖 | 学员候补与关注时段依赖可约时段数据 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-014-.../tech-design.md` §1 |
| API Design | `docs/stories/US-014-.../tech-design.md` §2 |
| State Machine | `docs/stories/US-014-.../tech-design.md` §3 |
| Caching | `docs/stories/US-014-.../tech-design.md` §4 |
| Performance / Security | `docs/stories/US-014-.../tech-design.md` §5 |
