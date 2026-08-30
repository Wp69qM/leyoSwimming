# Design: US-030 学员取消正价课程

> 本文档对应 `docs/stories/US-030-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-030 完成学员按规则取消正价课程预约。核心流程：判断时间窗 → ≥24h 直接取消释放课时 / <24h 创建取消申请 → 教练审批 → 超时自动拒绝。

## Data Model

### 读写的表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `booking` | 读写 | `id`, `user_id`, `coach_id`, `status`, `start_time` |
| `package` | 读写 | `id`, `reserved_count`, `available_count` |
| `booking_cancellation` | 新增/修改 | `id`, `booking_id`, `requester_id`, `reason`, `status`, `requested_at`, `resolved_at`, `resolved_by` |

### 索引

```sql
CREATE UNIQUE INDEX idx_cancel_booking_active ON booking_cancellation(booking_id) WHERE status = '待审批';
CREATE INDEX idx_booking_user_status ON booking(user_id, status);
CREATE INDEX idx_cancel_status ON booking_cancellation(status);
```

## API Design

### POST /api/bookings/{booking_id}/cancel

- 鉴权：必须登录且为 booking 所有者
- Request: `{ reason: string }`
- Response 200/201: `{ booking_id, status, cancellation_id? }`
- Response 400: `BOOKING_NOT_CANCELLABLE | CANCEL_REQUEST_IN_PROGRESS | BOOKING_STARTED`
- Response 403: `BOOKING_ACCESS_DENIED`

### POST /api/coach/cancellation-requests/{id}/approve

- 鉴权：教练且为 booking.coach_id
- Response 200: `{ cancellation_id, status: "已通过" }`

### POST /api/coach/cancellation-requests/{id}/reject

- 鉴权：教练且为 booking.coach_id
- Request: `{ reason: string }`
- Response 200: `{ cancellation_id, status: "已拒绝" }`

## State Machine

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| `booking` | 已预约 → 已取消 | ≥24h 直接取消 或 教练同意 |
| `package` | reserved → available | 取消成功 |
| `booking_cancellation` | 无 → 待审批 → 已通过/已拒绝 | 学员提交 / 教练审批 / 超时 |

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| booking | `booking:{booking_id}` | 300s | 取消后删除 |
| package | `package:{package_id}` | 300s | 释放后删除 |
| 我的预约列表 | `bookings:list:{user_id}` | 300s | 取消后删除 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 取消接口 P99 | < 200ms |
| 教练审批接口 P99 | < 200ms |
| 超时扫描任务 | 每分钟一次，单次 < 1s |

## Security

- 严格校验 booking 归属
- 教练只能审批自己课程的取消申请
- 幂等键防止重复提交
- 防止取消已开始/已结束课程
- 因特殊原因（场馆闭馆、教练离职等）被管理员/系统取消的 booking，学员端展示取消原因并提供 US-049 客服工单申诉入口

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-029 | 依赖 | 已存在 booking |
| US-031 | 被依赖 | 教练代约/改约正价课程 |
| US-033 | 被依赖 | 教练确认上课记录 |
