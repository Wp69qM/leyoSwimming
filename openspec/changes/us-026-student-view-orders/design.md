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
| `refund` | 详情退款信息 | `order_id`, `amount`, `status`, `completed_at` |
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

### GET /api/orders

- 鉴权：必须登录
- Query: `page`, `size`, `status`
- Response 200: `{ items: OrderListItem[], total, page, size }`，其中 OrderListItem 包含 `package_mode` 字段，前端映射为"正价"/"体验课"标签

### GET /api/orders/{order_id}

- 鉴权：必须登录且为订单所有者
- Response 200: `{ order_id, status, amount, package_snapshot, package, coach, payment, refund }`，其中 `package_snapshot` 为购买时模板快照字段，不受 package_template 后续变更影响
- Response 403: `ORDER_ACCESS_DENIED`
- Response 404: `ORDER_NOT_FOUND`

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis 列表 | `orders:list:{user_id}:{page}:{size}:{status}` | 60s | order 状态变更时删除 |
| Redis 详情 | `order:detail:{order_id}` | 300s | order/payment/refund 变更时删除 |

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
| US-027 | 被依赖 | 退款入口 |
| US-046 | 被依赖 | 管理端订单查看 |
