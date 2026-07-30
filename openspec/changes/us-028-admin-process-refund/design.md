# Design: US-028 管理员处理退款并原路退回

> 本文档对应 `docs/stories/US-028-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-028 完成退款申请的审批与原路退回。核心流程：管理员批准/驳回 → 校验金额与状态 → 调用渠道退款（批准时）→ 事务更新 order/refund/package → 异步身份重算。

## Data Model

### 读写的表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `refund` | 读写 | `id`, `order_id`, `amount`, `status`, `reason` |
| `order` | 读写 | `id`, `status`, `paid_amount`, `refunded_at` |
| `refund_transaction` | 写 | `id`, `refund_id`, `channel`, `amount`, `status`, `failure_reason` |
| `package` | 读写 | `id`, `order_id`, `status`, `reserved_count`, `available_count`, `consumed_count` |
| `payment` | 读 | `order_id`, `channel`, `channel_trade_no` |
| `user` | 读/触发 | `id`, `status` |

### 索引

```sql
CREATE INDEX idx_refund_status ON refund(status);
CREATE INDEX idx_refund_order ON refund(order_id);
CREATE INDEX idx_refund_transaction_refund ON refund_transaction(refund_id);
CREATE INDEX idx_order_status ON order(status);
CREATE INDEX idx_package_refund ON package(order_id, status);
```

## API Design

### GET /api/admin/refunds

- 鉴权：管理员
- Query: `page`, `size`, `status`
- Response 200: `{ items, total, page, size }`

### GET /api/admin/refunds/{refund_id}

- 鉴权：管理员
- Response 200: `{ refund_id, order, package, payment, amount, reason, status, audit_log }`
- Response 404: `REFUND_NOT_FOUND`

### POST /api/admin/refunds/{refund_id}/approve

- 鉴权：管理员
- Request: `{ amount?: number, remark?: string }`
- Response 200: `{ refund_id, refund_transaction_id, status }`
- Response 400: `INVALID_REFUND_AMOUNT | REFUND_ALREADY_PROCESSED`
- Response 403: `FORBIDDEN`
- Response 404: `REFUND_NOT_FOUND`

### POST /api/admin/refunds/{refund_id}/reject

- 鉴权：管理员
- Request: `{ reason: string }`
- Response 200: `{ refund_id, status: "管理员驳回" }`
- Response 400: `REFUND_ALREADY_PROCESSED`
- Response 403: `FORBIDDEN`

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| 退款列表 | `admin:refunds:{status}:{page}:{size}` | 60s | 审批/驳回后删除 |
| 订单详情 | `order:detail:{order_id}` | 300s | order 状态变更后删除 |
| package | `package:{package_id}` | 300s | package 状态变更后删除 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 退款列表查询 P99 | < 200ms |
| 退款详情查询 P99 | < 100ms |
| 审批接口 P99 | < 800ms（含渠道调用）|

## Security

- 接口仅对管理员角色开放
- 金额校验：审批金额 ≤ refund.amount
- 渠道密钥不暴露前端
- 审批操作记录审计日志
- 幂等键 + 分布式锁防重复审批

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-027 | 依赖 | 退款申请 |
| US-046 | 被依赖 | 管理员订单管理 |
