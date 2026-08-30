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

> 退款订单复用 US-046 的订单管理接口查看列表与详情；本 US 仅新增审批相关接口。

### POST /api/admin/order/list

- 鉴权：管理员登录 + `order:read`
- Request：
  ```json
  {
    "page": 1,
    "pageSize": 20,
    "status": "refund_pending",
    "coachId": 1,
    "userId": 10001,
    "startDate": "2026-08-01",
    "endDate": "2026-08-31"
  }
  ```
- Response 200: `{ items: OrderListItem[], total, page, pageSize }`

### POST /api/admin/order/detail

- 鉴权：管理员登录 + `order:read`
- Request：
  ```json
  {
    "orderId": 1
  }
  ```
- Response 200: `{ orderId, type, status, amount, package, payment, refundRecords, auditLog }`
- Response 404: `ORDER_NOT_FOUND`

### POST /api/admin/order/approve-refund

- 鉴权：管理员登录 + `order:write`
- Request：
  ```json
  {
    "orderId": 1,
    "amount": 144000,
    "remark": "同意退款"
  }
  ```
- Response 200: `{ orderId, refundTransactionId, status }`
- Response 400: `INVALID_REFUND_AMOUNT | REFUND_ALREADY_PROCESSED`
- Response 403: `FORBIDDEN`
- Response 404: `ORDER_NOT_FOUND`

### POST /api/admin/order/reject-refund

- 鉴权：管理员登录 + `order:write`
- Request：
  ```json
  {
    "orderId": 1,
    "reason": "不符合退款条件"
  }
  ```
- Response 200: `{ orderId, status: "refund_rejected" }`
- Response 400: `REFUND_ALREADY_PROCESSED`
- Response 403: `FORBIDDEN`

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| 退款列表 | `admin:refunds:{status}:{page}:{pageSize}` | 60s | 审批/驳回后删除 |
| 订单详情 | `order:detail:{order_id}` | 300s | order 状态变更后删除 |
| package | `package:{package_id}` | 300s | package 状态变更后删除 |

## State Machine

### order 状态机（两阶段退款，v3 评审 P0 修复）

| 转换 | 触发条件 | 说明 |
|------|---------|------|
| 退款审批中（4）→ 退款处理中（8） | 管理员批准且渠道退款受理 | 中间态，等待渠道回调 |
| 退款处理中（8）→ 已退款（6） | 渠道退款成功回调 | 终态；同步 package frozen(refund_pending) → refunded |
| 退款处理中（8）→ 退款审批中（4） | 渠道退款失败/超时 | 回滚中间态；package 保持 frozen（refund_pending），booking_frozen 保持 true，可重试 |
| 退款审批中（4）→ 退款被拒（7） | 管理员驳回 | 终态；package → active（解冻，frozen_reason 清空），booking_frozen = false |

### refund 状态机（v3 评审 P0 修复）

| 转换 | 触发条件 |
|------|---------|
| 待审批 → 管理员批准 | 管理员批准（order 进入退款处理中） |
| 待审批 → 管理员驳回 | 管理员驳回（order 进入退款被拒） |
| 管理员批准 → 退款成功 | 渠道退款成功回调 |
| 管理员批准 → 退款失败 | 渠道退款失败/超时（可重试） |

### package 状态机（v3 评审 P0 修复，v5 半落地修复）

| 转换 | 触发条件 | 说明 |
|------|---------|------|
| frozen（refund_pending）→ frozen（refund_pending，保持） | 管理员批准（阶段 1 受理）或 渠道失败回滚 | 由 US-027 触发 frozen(refund_pending)；退款处理期间 status 不变，booking_frozen = true |
| frozen（refund_pending）→ refunded | 渠道退款成功（阶段 2 成功） | 终态；关联赠送 package 同步作废 |
| frozen（refund_pending）→ active | 管理员驳回 | 解冻，frozen_reason 清空；booking_frozen = false |

> **package 退款状态说明**（v3 评审 P0 修复，v4 P0 再修复，v5 半落地修复）：PRD §3.6 / §5.5.1.2 明确退款审批期间 package.status = frozen（refund_pending）（由 US-027 学员提交退款时触发）。约课冻结通过 `package.booking_frozen = true` 实现；渠道成功时 package.status → refunded；驳回时 package.status → active（解冻，frozen_reason 清空），booking_frozen = false；失败回滚时 package.status 保持 frozen（refund_pending）。

## Performance Targets

| 指标 | 目标 |
|------|------|
| 退款列表查询 P99 | < 200ms |
| 退款详情查询 P99 | < 100ms |
| 审批接口 P99 | < 800ms（含渠道调用）|

## Security

- 接口仅对管理员角色开放
- 金额校验：审批金额 ≥ 0（可退金额仅作为参考，管理员可基于业务场景调整）
- 渠道密钥不暴露前端
- 审批操作记录审计日志
- 幂等键 + 分布式锁防重复审批

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-027 | 依赖 | 退款申请 |
| US-046 | 被依赖 | 管理员订单管理 |
