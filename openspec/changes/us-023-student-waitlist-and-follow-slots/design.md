# Design: US-023 学员候补与关注时段

## Overview

新增候补与关注能力：已满时段可排队，任意时段可关注提醒。

## Data Model

### 表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `waitlist` | 写 | `user_id`, `schedule_slot_id`, `status`, `created_at`, `converted_at` |
| `slot_follow` | 写 | `user_id`, `schedule_slot_id`, `created_at` |
| `schedule_slot` | 读 | `capacity`, `booked_count`, `status` |
| `package` | 读 | `status` |
| `user` | 读 | `identity` |

### 索引

```sql
CREATE INDEX idx_waitlist_slot_status_created ON waitlist(schedule_slot_id, status, created_at);
CREATE INDEX idx_waitlist_user_status ON waitlist(user_id, status);
CREATE INDEX idx_slot_follow_user ON slot_follow(user_id);
CREATE INDEX idx_slot_follow_slot ON slot_follow(schedule_slot_id);
```

## API Design

- `POST /api/schedule-slots/{id}/waitlist`：加入候补
- `DELETE /api/waitlist/{id}`：取消候补
- `POST /api/schedule-slots/{id}/follow`：关注
- `DELETE /api/follows/{id}`：取消关注
- `GET /api/users/me/waitlist-and-follows`：我的列表

## Caching

（无）

## Performance Targets

| 指标 | 目标 |
|------|------|
| 加入候补/关注 P99 | < 200ms |
| 我的列表 P99 | < 100ms |

## Security

- 登录鉴权
- 只能操作本人记录
- 候补需 active 套餐

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-004/US-005 | 依赖 | 登录 |
| US-014/US-016 | 依赖 | 时段释放 |
| US-020/US-021 | 依赖 | active 套餐 |
| US-024 | 被依赖 | 候补转正 |
