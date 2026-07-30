# Design: US-024 学员候补自动转正

## Overview

当 schedule_slot 因取消释放名额时，系统按候补顺序自动为首位符合条件的学员创建 booking，并通知转正学员与关注用户。

## Data Model

### 表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `waitlist` | 写 | `user_id`, `schedule_slot_id`, `status`, `created_at`, `converted_at` |
| `booking` | 写 | `user_id`, `coach_id`, `schedule_slot_id`, `package_id`, `status`, `source` |
| `package` | 写 | `available`, `reserved` |
| `schedule_slot` | 读/写 | `capacity`, `booked_count`, `status` |
| `slot_follow` | 读 | `user_id`, `schedule_slot_id` |
| `notification` | 写 | `user_id`, `type`, `payload`, `sent_at` |
| `user` | 读 | `identity` |

### 索引

```sql
CREATE INDEX idx_waitlist_slot_status_created ON waitlist(schedule_slot_id, status, created_at);
CREATE INDEX idx_waitlist_user_status ON waitlist(user_id, status);
CREATE INDEX idx_slot_follow_slot ON slot_follow(schedule_slot_id);
CREATE INDEX idx_booking_slot_status ON booking(schedule_slot_id, status);
```

## API Design

- **内部事件**：`BookingCancelledEvent` → `WaitlistPromotionService.promote(schedule_slot_id)`
- 无新增用户端 API

## Caching

（无）

## Performance Targets

| 指标 | 目标 |
|------|------|
| 候补转正处理 P99 | < 300ms |
| 单 slot 关注提醒发送 P99 | < 100ms |

## Security

- 仅内部事件消费触发，不暴露外部端点
- 事务控制保证名额不超卖
- 同一 `waitlist.id` 仅允许一次转正
- 转正时仅操作本人 package

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-023 | 依赖 | 候补/关注记录 |
| US-030 | 依赖 | 取消事件触发释放 |
| US-029 | 被依赖 | 预约上课流程 |
