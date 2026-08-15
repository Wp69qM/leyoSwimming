# Design: US-026 学员查看订单列表与详情

> 本文档对应 `docs/stories/US-026-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-026 是只读 US，为学员提供订单列表与详情查询能力，含分页、状态筛选、权限校验与缓存。

## Data Model

### 读取的表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `order` | 列表 + 详情主表；套餐信息取自快照字段 | `id`, `user_id`, `amount`, `status`, `created_at`, `paid_at`, `refunded_at`, `refund_status`, `package_name`, `package_mode`, `coach_name`, `teaching_type`, `total_hours`, `duration_minutes`, `valid_days`, `original_price`, `paid_amount`, `refund_enabled`, `refund_ratio`, `refund_valid_days` |
| `package` | 详情当前课时包状态 | `id`, `total_hours`, `available_count`, `status` |
| `coach` | 兜底读取教练信息（优先展示 order.coach_name 快照） | `id`, `name` |
| `payment` | 详情支付时间 | `order_id`, `paid_at`, `status` |
| `refund` | 详情/列表退款信息 | `order_id`, `amount`, `status`, `reason`, `reject_reason`, `completed_at` |
| `package_template` | 不读取 | — |

### order.status 枚举（v3 评审 P0 修复，对齐 PRD §6.2.2 v11.1）

| 值 | 业务含义 |
|----|---------|
| `0` | 无 |
| `1` | 待支付 |
| `2` | 已支付 |
| `3` | 已取消 |
| `4` | 退款审批中 |
| `5` | 争议退款处理中 |
| `6` | 已退款 |
| `7` | 退款被拒 |
| `8` | 退款处理中 |

> `refund_status` 字段：退款子状态（无 / 处理中 / 成功 / 失败），用于细化 order.status=8 时的渠道状态。`refunded_at` 由 US-028 渠道成功回调时回填。

### 索引

```sql
CREATE INDEX idx_order_user_created ON order(user_id, created_at DESC);
```

## API Design

### POST /api/order/list

- **鉴权**：必须登录
- **Request**:
  ```json
  {
    "page": 1,
    "pageSize": 20,
    "status": "pending"
  }
  ```
- **Response 200**: `{ items: OrderListItem[], total, page, pageSize }`
  - `OrderListItem` 包含 `packageMode` 字段，前端映射为"正价"/"体验课"标签
  - 待支付订单额外返回 `remainingSeconds` 与 `actions: ["cancel_pay", "go_pay"]`
  - 退款中订单额外返回 `actions: ["cancel_refund"]`
  - 已退款订单额外返回 `refundReason`

### POST /api/order/detail

- **鉴权**：必须登录且为订单所有者
- **Request**:
  ```json
  {
    "orderId": 1
  }
  ```
- **Response 200**: `{ orderId, status, amount, package, coach, payment, refund, refundRejectReason, remainingSeconds, actions }`
  - `package` 为购买时模板快照字段，不受 packageTemplate 后续变更影响
  - 待支付订单返回 `remainingSeconds` 与 `actions: ["cancel_pay", "go_pay"]`
  - 退款中订单返回 `actions: ["cancel_refund"]`
  - 退款被拒订单返回 `refundRejectReason`
- **Response 403**: `ORDER_ACCESS_DENIED`
- **Response 404**: `ORDER_NOT_FOUND`

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis 列表 | `orders:list:{user_id}:{page}:{pageSize}:{status}` | 60s | order 状态变更时删除 |
| Redis 详情 | `order:detail:{user_id}:{order_id}` | 300s | order/payment/refund 变更时删除 |

> **安全约束**：订单详情缓存 Key 必须包含 `user_id`，防止跨用户越权读取缓存。读缓存前先校验 `order.user_id = current_user.id`。

## Performance Targets

| 指标 | 目标 |
|------|------|
| 列表查询 P99 | < 200ms |
| 详情查询 P99 | < 100ms |
| 分页最大 size | 50 |

## Security

- 严格订单归属校验
- 禁止跨用户查询
- 只返回当前用户订单

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-020 | 依赖 | 订单数据 |
| US-025 | 依赖 | 支付状态与 package |
| US-027 | 被依赖 | 订单详情展示退款原因/状态 |
| US-046 | 被依赖 | 管理端订单查看 |
