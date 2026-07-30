# Design: US-031 教练代约/改约正价课程

> 本文档对应 `docs/stories/US-031-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-031 完成教练在学员授权后代为预约或改约正价课程。核心流程：校验教练/学员身份与绑定关系 → 校验时段可用性 → 代约时 FIFO 预占课时创建 booking；改约时事务内取消原预约并创建新预约。

## Data Model

### 读写的表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `package` | 读写 | `id`, `user_id`, `coach_id`, `status`, `available_count`, `reserved_count`, `created_at` |
| `booking` | 写/修改 | `id`, `user_id`, `coach_id`, `package_id`, `schedule_slot_id`, `status`, `operator`, `start_time`, `end_time`, `created_at` |
| `schedule_slot` | 读 | `id`, `coach_id`, `start_time`, `end_time`, `status` |
| `user` | 读 | `id`, `status` |
| `coach` | 读 | `id`, `status` |

### 索引

```sql
CREATE UNIQUE INDEX idx_booking_slot_user ON booking(schedule_slot_id, user_id) WHERE status != '已取消';
CREATE INDEX idx_booking_user_status ON booking(user_id, status);
CREATE INDEX idx_booking_coach_status ON booking(coach_id, status);
CREATE INDEX idx_package_user_status ON package(user_id, status);
CREATE INDEX idx_schedule_slot_coach_time ON schedule_slot(coach_id, start_time);
```

## API Design

### POST /api/coach/bookings

- 鉴权：教练登录且为 booking.coach_id
- Request:
  ```json
  {
    "student_id": 100,
    "schedule_slot_id": 200,
    "idempotency_key": "uuid"
  }
  ```
- Response 201: `{ booking_id, status, start_time, end_time }`
- Response 400: `SLOT_TAKEN | NO_QUOTA | NO_BOUND_COACH | SLOT_NOT_AVAILABLE | PACKAGE_NOT_BOOKABLE`
- Response 403: `BOOKING_ACCESS_DENIED`

### POST /api/coach/bookings/{booking_id}/reschedule

- 鉴权：教练登录且为原 booking.coach_id
- Request:
  ```json
  {
    "new_schedule_slot_id": 300,
    "idempotency_key": "uuid"
  }
  ```
- Response 200: `{ old_booking_id, new_booking_id, status, start_time, end_time }`
- Response 400: `SLOT_TAKEN | NO_QUOTA | SLOT_NOT_AVAILABLE | BOOKING_NOT_RESCHEDULABLE`
- Response 403: `BOOKING_ACCESS_DENIED`

### GET /api/coaches/{coach_id}/slots

- 鉴权：教练登录
- Query: `start_date`, `end_date`
- Response 200: `{ items: Slot[] }`

### GET /api/coach/students/{student_id}/packages

- 鉴权：教练登录且与该学员存在绑定关系
- Response 200: `{ items: Package[] }`

## State Machine

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| `booking` | 无 → 已预约 | 教练代约成功 |
| `booking` | 已预约 → 已取消 | 教练改约释放原预约 |
| `package` | available → reserved | 代约成功或改约创建新预约 |
| `package` | reserved → available | 改约释放原预约 |

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| 可约时段 | `slots:{coach_id}:{date}` | 60s | 预约成功后删除 |
| 套餐 | `package:{package_id}` | 300s | 预占/释放后删除 |
| 学员预约列表 | `bookings:list:{user_id}` | 300s | 变更后删除 |
| 分布式锁 | `booking:lock:{schedule_slot_id}` | 5s | 预约完成后释放 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 代约提交接口 P99 | < 300ms |
| 改约接口 P99 | < 400ms |
| 时段列表查询 P99 | < 200ms |
| 并发预约冲突处理 | 1000 QPS P99 < 500ms |

## Security

- 严格校验教练身份与 coach_id
- 严格校验 package 归属与教练绑定关系
- 禁止教练预约/改约非绑定学员的课程
- 幂等键防重放
- 改约事务内完成，避免课时丢失

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-012 | 依赖 | 教练资质与主页 |
| US-014 | 依赖 | 教练可约时段 |
| US-020 | 依赖 | 学员购买正价套餐 |
| US-029 | 依赖 | 复用正价预约规则 |
| US-032 | 被依赖 | 学员签到/签退课程 |
| US-033 | 被依赖 | 教练确认上课记录 |
