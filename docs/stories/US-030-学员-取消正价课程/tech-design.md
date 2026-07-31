# Tech Design: US-030 学员取消/改约正价课程

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `booking` | 读写 | 更新为已取消；写入 operator 字段（student/coach/system）标识取消发起方（v3 评审 P0 修复：与 US-029/US-031 的 operator 字段统一） |
| `package` | 读写 | 释放预占课时 |
| `booking_cancellation` | 新增/修改 | 记录取消申请与审批 |

#### booking_cancellation

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `booking_id` | FK |
| `requester_id` | FK |
| `reason` | 取消原因 |
| `status` | 待审批 / 已通过 / 已拒绝 |
| `requested_at` | 申请时间 |
| `resolved_at` | 审批时间 |
| `resolved_by` | 审批教练 ID |

### 1.2 索引

```sql
CREATE UNIQUE INDEX idx_cancel_booking_active ON booking_cancellation(booking_id) WHERE status = '待审批';
CREATE INDEX idx_booking_user_status ON booking(user_id, status);
CREATE INDEX idx_cancel_status ON booking_cancellation(status);
```

## 2. API 设计

### 2.1 POST /api/bookings/{booking_id}/cancel

- **鉴权**：必须登录且为 booking 所有者
- **Request**: `{ reason: string }`
- **Response 200/201**: `{ booking_id, status, cancellation_id? }`
- **Response 400**: `BOOKING_NOT_CANCELLABLE | CANCEL_REQUEST_IN_PROGRESS | BOOKING_STARTED`
- **Response 403**: `BOOKING_ACCESS_DENIED`

### 2.2 POST /api/coach/cancellation-requests/{id}/approve

- **鉴权**：教练且为 booking.coach_id
- **Response 200**: `{ cancellation_id, status: "已通过" }`

### 2.3 POST /api/coach/cancellation-requests/{id}/reject

- **鉴权**：教练且为 booking.coach_id
- **Request**: `{ reason: string }`
- **Response 200**: `{ cancellation_id, status: "已拒绝" }`

## 3. 状态机

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| `booking` | 已预约 → 已取消 | ≥24h 直接取消 或 教练同意 |
| `package` | reserved → available | 取消成功 |
| `booking_cancellation` | 无 → 待审批 → 已通过/已拒绝 | 学员提交 / 教练审批 |

## 4. 缓存

- booking 缓存：`booking:{booking_id}`，TTL 300s，取消后删除
- package 缓存：`package:{package_id}`，TTL 300s，释放后删除
- 我的预约列表：`bookings:list:{user_id}`，取消后删除

## 5. 性能

| 指标 | 目标 |
|------|------|
| 取消接口 P99 | < 200ms |
| 教练审批接口 P99 | < 200ms |
| 超时扫描任务 | 每分钟一次，单次 < 1s |

## 6. 安全

- 严格校验 booking 归属
- 教练只能审批自己课程的取消申请
- 幂等键防止重复提交
- 防止取消已开始/已结束课程

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-029 | 依赖 | 已存在 booking |
| US-031 | 被依赖 | 教练代约/改约正价课程 |
| US-033 | 被依赖 | 教练确认上课记录 |

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 24h 外直接取消 | `test_cancel_outside_24h` |
| 24h 内提交申请 | `test_cancel_inside_24h_request` |
| 教练同意 | `test_cancel_coach_approve` |
| 申请超时 | `test_cancel_request_timeout` |
| 重复取消 | `test_cancel_already_cancelled` |

## 9. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版 |
| v1.1 | 2026-07-31 | Dev | v3 评审 P0 修复：§1.1 booking 表补 operator 字段说明（student/coach/system），与 US-029/US-031 统一 |
