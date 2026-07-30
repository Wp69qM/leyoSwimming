# Design: US-029 学员预约正价课程

> 本文档对应 `docs/stories/US-029-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-029 完成学员选择已绑定教练可约时段并提交正价课程预约。核心流程：校验学员身份与套餐状态 → 校验时段可用性 → FIFO 选择套餐并预占课时 → 创建 booking → 返回详情并通知。

## Data Model

### 读写的表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `package` | 读写 | `id`, `user_id`, `coach_id`, `status`, `available_count`, `reserved_count`, `created_at` |
| `booking` | 写 | `id`, `user_id`, `coach_id`, `package_id`, `schedule_slot_id`, `status`, `start_time`, `end_time`, `created_at` |
| `schedule_slot` | 读 | `id`, `coach_id`, `start_time`, `end_time`, `status` |
| `user` | 读 | `id`, `status` |

### 索引

```sql
CREATE UNIQUE INDEX idx_booking_slot_user ON booking(schedule_slot_id, user_id) WHERE status != '已取消';
CREATE INDEX idx_booking_user_status ON booking(user_id, status);
CREATE INDEX idx_package_user_status ON package(user_id, status);
CREATE INDEX idx_schedule_slot_coach_time ON schedule_slot(coach_id, start_time);
```

## API Design

### GET /api/coaches/{coach_id}/slots

- 鉴权：必须登录
- Query: `start_date`, `end_date`
- Response 200: `{ items: Slot[] }`

### GET /api/packages/active

- 鉴权：必须登录
- Response 200: `{ items: Package[] }`

### POST /api/bookings

- 鉴权：必须登录
- Request:
  ```json
  {
    "coach_id": 100,
    "schedule_slot_id": 200,
    "idempotency_key": "uuid"
  }
  ```
- Response 201: `{ booking_id, status, start_time, end_time }`
- Response 400: `SLOT_TAKEN | NO_QUOTA | NO_BOUND_COACH | SLOT_NOT_AVAILABLE | PACKAGE_NOT_BOOKABLE`
- Response 409: `DUPLICATE_BOOKING`

## State Machine

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| `booking` | 无 → 已预约 | 预约校验通过 |
| `package` | available → reserved | 预约成功，FIFO 预占 1 课时 |

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| 可约时段 | `slots:{coach_id}:{date}` | 60s | 预约成功后删除 |
| 套餐 | `package:{package_id}` | 300s | 预占后删除 |
| 分布式锁 | `booking:lock:{schedule_slot_id}` | 5s | 预约完成后释放 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 时段列表查询 P99 | < 200ms |
| 预约提交接口 P99 | < 300ms |
| 并发预约冲突处理 | 1000 QPS P99 < 500ms |

## Security

- 严格校验订单归属与套餐归属
- 禁止预约他人套餐
- 幂等键防重放
- 时段释放校验防止预约未释放时段

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-014 | 依赖 | 教练可约时段 |
| US-020 | 依赖 | active 正价套餐 |
| US-030 | 被依赖 | 取消/改约正价课程 |
| US-031 | 被依赖 | 教练代约/改约正价课程 |
| US-032 | 被依赖 | 学员签到/签退课程 |
| US-033 | 被依赖 | 教练确认上课记录 |
