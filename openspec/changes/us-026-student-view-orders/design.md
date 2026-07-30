# Design: US-026 学员查看订单列表与详情

> 本文档对应 `docs/stories/US-026-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-026 是只读 US，为学员提供订单列表与详情查询能力，含分页、状态筛选、权限校验与缓存。

## Data Model

### 读取的表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `order` | 列表 + 详情主表 | `id`, `user_id`, `amount`, `status`, `created_at`, `paid_at` |
| `package` | 详情套餐信息 | `id`, `total_hours`, `available_count`, `status` |
| `coach` | 详情教练信息 | `id`, `name` |
| `payment` | 详情支付时间 | `order_id`, `paid_at`, `status` |
| `refund` | 详情退款信息 | `order_id`, `amount`, `status`, `completed_at` |

### 索引

```sql
CREATE INDEX idx_order_user_created ON order(user_id, created_at DESC);
```

## API Design

### GET /api/orders

- 鉴权：必须登录
- Query: `page`, `size`, `status`
- Response 200: `{ items: OrderListItem[], total, page, size }`

### GET /api/orders/{order_id}

- 鉴权：必须登录且为订单所有者
- Response 200: `{ order_id, status, amount, package, coach, payment, refund }`
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
