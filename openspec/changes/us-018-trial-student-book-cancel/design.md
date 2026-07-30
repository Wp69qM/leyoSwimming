# Design: US-018 体验课学员预约与取消

## Overview

体验课预约与取消，核心为正价预约流程的体验课版本，使用体验套餐课时。

## Data Model

### 表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `booking` | 写 | `user_id`, `coach_id`, `slot_id`, `package_id`, `course_type=0`, `status` |
| `package` | 改 | `reserved`, `available` |
| `schedule_slot` | 读/改 | `status` |
| `cancel_request` | 写 | `booking_id`, `reason`, `status` |

### 索引

```sql
CREATE UNIQUE INDEX idx_booking_slot_user ON booking(slot_id, user_id) WHERE status != '已取消';
CREATE INDEX idx_booking_user_status ON booking(user_id, status);
```

## API Design

- `POST /api/bookings/trial`：创建体验课预约
- `POST /api/bookings/{id}/cancel`：取消预约

## Caching

| 层 | Key | TTL |
|----|-----|-----|
| Redis | `slot:{id}:status` | 30s |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 预约 P99 | < 300ms |
| 取消 P99 | < 200ms |

## Security

- 只能操作自己的 booking
- 唯一索引防止并发重复预约

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-014/US-016 | 依赖 | 可约时段 |
| US-017 | 依赖 | 体验套餐 |
| US-032/US-033 | 被依赖 | 签到/扣课时 |
