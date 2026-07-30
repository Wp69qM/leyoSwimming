# Design: US-033 教练确认上课记录

> 本文档对应 `docs/stories/US-033-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-033 完成教练在课程结束后确认上课并扣除课时，以及管理员特殊情况下返还课时。核心流程：校验课程已开始且 booking 可确认 → 填写上课记录 → 事务内更新 booking/package/course_record → 通知学员；管理员返还时校验已消耗课时并回滚。

## Data Model

### 读写的表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `booking` | 修改 | `id`, `coach_id`, `status`, `start_time` |
| `package` | 修改 | `id`, `reserved_count`, `consumed_count`, `available_count`, `total_hours`, `status` |
| `course_record` | 新增/修改 | `id`, `booking_id`, `content`, `focus_tags_json`, `mastery_level`, `homework`, `media_json` |
| `hour_return` | 新增 | `id`, `booking_id`, `package_id`, `admin_id`, `reason`, `hours`, `created_at` |
| `audit_log` | 写 | `id`, `action`, `operator_id`, `target_id`, `details` |

### 索引

```sql
CREATE INDEX idx_hour_return_booking ON hour_return(booking_id);
CREATE INDEX idx_hour_return_package ON hour_return(package_id);
CREATE INDEX idx_course_record_booking ON course_record(booking_id);
CREATE INDEX idx_booking_coach_status ON booking(coach_id, status);
CREATE INDEX idx_package_user_status ON package(user_id, status);
```

## API Design

### POST /api/coach/bookings/{booking_id}/confirm

- 鉴权：教练且为 booking.coach_id
- Request: `{ content: string, focus_tags: string[], mastery_level: int, homework: string, media: string[] }`
- Response 200: `{ booking_id, status: "已完成", package: { reserved_count, consumed_count, available_count } }`
- Response 400: `CLASS_NOT_STARTED | BOOKING_NOT_CONFIRMABLE`
- Response 403: `BOOKING_ACCESS_DENIED`

### POST /api/admin/bookings/{booking_id}/return-hour

- 鉴权：管理员
- Request: `{ reason: string, hours: int = 1 }`
- Response 200: `{ hour_return_id, package: { consumed_count, available_count } }`
- Response 400: `NO_CONSUMED_HOUR | INVALID_RETURN_HOURS`

## State Machine

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| `booking` | 待上课/上课中 → 已完成 | 教练确认 |
| `package` | reserved → consumed | 教练确认 |
| `package` | consumed → available | 管理员返还 |
| `package` | active → exhausted | 确认最后一课时 |

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| booking | `booking:{booking_id}` | 300s | 确认/返还后删除 |
| package | `package:{package_id}` | 300s | 变更后删除 |
| 我的预约列表 | `bookings:list:{user_id}` | 300s | 确认后删除 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 教练确认接口 P99 | < 300ms |
| 管理员返还接口 P99 | < 300ms |
| 并发确认 | 单 booking 串行（分布式锁/乐观锁）|

## Security

- 教练只能确认自己的课程
- 管理员返还需记录原因与审计日志
- 幂等键防止重复确认
- 防止课程未开始或已终态的确认

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-029 | 依赖 | 已存在 booking |
| US-032 | 依赖 | 学员签到/记录页已存在 |
| US-034 | 被依赖 | 管理员查看上课记录 |
