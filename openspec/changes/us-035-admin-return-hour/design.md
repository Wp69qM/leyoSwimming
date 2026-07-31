# Design: US-035 管理员返还课时

> 本文档对应 `docs/stories/US-035-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-035 让管理员在特殊情况下（教练误操作、客诉、争议）对已扣课时的 booking 手动返还课时。核心流程：校验管理员权限与 booking 可返还状态 → 校验 package 有可扣课时 → 事务内回滚 package 消耗、创建返还记录与审计日志、通知学员 → 若套餐原为 exhausted 且返还后 consumed < total_hours 则复活为 active；若套餐原为 expired 且返还后 available > 0，则同步恢复为 active 并按原有效期时长延长 expire_at。

## Data Model

### 读写的表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `package` | 读/写 | `id`, `consumed_count`, `available_count`, `total_hours`, `status`, `expire_at`, `purchased_at` |
| `booking` | 读 | `id`, `status` |
| `hour_return` | 写 | `id`, `booking_id`, `package_id`, `admin_id`, `reason_type`, `reason_detail`, `returned_hours`, `created_at` |
| `audit_log` | 写 | `id`, `action`, `operator_id`, `target_id`, `details` |
| `notification` | 写 | `id`, `user_id`, `type`, `payload` |

### hour_return 字段

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK | — |
| `booking_id` | BIGINT | FK, IDX | 关联 booking |
| `package_id` | BIGINT | FK | 关联 package |
| `admin_id` | BIGINT | FK | 操作管理员 |
| `reason_type` | TINYINT | NOT NULL | 1=教练误操作 2=客诉处理 3=系统故障 4=其他 |
| `reason_detail` | VARCHAR(500) | NULL | 详细说明 |
| `returned_hours` | INT | 默认 1 | 返还课时数（MVP 固定 1） |
| `created_at` | DATETIME | — | 时间戳 |

### 索引

```sql
CREATE INDEX idx_hour_return_booking ON hour_return(booking_id);
CREATE INDEX idx_hour_return_package ON hour_return(package_id);
CREATE INDEX idx_hour_return_admin ON hour_return(admin_id);
```

## API Design

### POST /api/admin/bookings/{booking_id}/return-hour

- **鉴权**：管理员 + `MANAGE_BOOKING` 权限
- **Request**:
  ```json
  {
    "reason_type": 1,
    "reason_detail": "教练误操作标记旷课，实际学员已到场"
  }
  ```
- **Response 200**: `{ package_id, consumed_count, available_count, status, expire_at }`
- **Response 400**: `{ code: "NO_CONSUMED_HOUR" | "BOOKING_NOT_RETURNABLE" | "INVALID_REASON_TYPE" | "PACKAGE_NOT_RETURNABLE" | "RETURN_QUOTA_EXCEEDED" }`
- **Response 401**: 未登录
- **Response 403**: 非管理员或无 `MANAGE_BOOKING` 权限

### 幂等键

`{admin_id}:{booking_id}:return-hour:{timestamp}` —— 同一 booking 允许多次返还（每次独立审计），但每次提交生成独立幂等键防重复提交。

### 前置条件（v3 评审 P0 修复）

- 管理员已登录且具有 `MANAGE_BOOKING` 权限
- 存在 booking.status ∈ {已完成, 旷课}
- 对应 package.consumed_count > 0
- 套餐状态 package.status ∈ {active, exhausted, expired}（已退款/refunded 不可返还，PRD §4.6 续期规则同理）

## State Machine

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| `package` | consumed → available | 管理员返还（consumed-1, available+1） |
| `package` | exhausted → active | 返还后若 consumed < total_hours，套餐复活 |
| `package` | expired → active | 返还后 available > 0 且管理员确认恢复（与 PRD §4.6 延期规则一致） |

复活判定逻辑：事务内更新 package 后：
- 若 `package.status = exhausted` 且 `consumed_count < total_hours`，则将 `status` 置为 `active`
- 若 `package.status = expired` 且返还后 `available_count > 0`，则将 `status` 从 `expired` 恢复为 `active`，并按原有效期时长重新计算 `expire_at`：新 `expire_at` = 返还操作时间 +（返还前 `expire_at` - `purchased_at`），保证 `status = active` 时必有 `now() < expire_at`（与 PRD §4.6 管理员手动延期规则一致）

> **PACKAGE_NOT_RETURNABLE 错误码**（v3 评审 P0 修复）：当 `package.status = refunded` 时返回此错误码，refunded 状态套餐不可返还（PRD §4.6 续期规则同理）。

## Caching

无特殊缓存（写操作为主）。返还成功后删除 `package:{package_id}` 与 `booking:{booking_id}` 缓存以保持一致。

## Performance Targets

| 指标 | 目标 |
|------|------|
| 返还接口 P99 | < 300ms |
| 并发返还 | 单 package 串行（乐观锁） |

## Security

- 管理员鉴权 + `MANAGE_BOOKING` 权限校验
- 必填 `reason_type`，审计留痕
- 事务包裹：`package` 更新 + `hour_return` 创建 + `audit_log` 写入 + `notification` 创建同一事务
- `package` 乐观锁防返还与教练确认并发：UPDATE 语句带 `WHERE consumed_count = ?` 条件，影响行数为 0 时回滚并返回错误
- 防重复返还：事务内校验该 booking 累计 `returned_hours_sum < consumed_count`，否则返回 `RETURN_QUOTA_EXCEEDED`
- 防重提交：基于幂等键的短期去重

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-033 | 依赖 | 产生已扣课时 booking（status ∈ {已完成, 旷课}） |
| US-034 | 被依赖 | 管理员查看上课记录可见返还记录 |
