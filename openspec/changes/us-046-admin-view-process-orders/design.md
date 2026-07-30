# Design: US-046 管理员查看与处理订单

> 本文档对应 `docs/stories/US-046-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-046 是管理员后台订单管理 US，核心提供订单查询与退款处理能力，涉及 order/package/refund_record 三张表的状态变更。

## Data Model

### 读取/修改表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `order` | 查询与更新状态 | order_id, status, paid_amount |
| `package` | 退款完成后更新 | package_id, status |
| `refund_record` | 新增退款记录 | refund_id, order_id, amount, status |
| `audit_log` | 记录操作 | admin_id, action, snapshot |

### 索引

```sql
CREATE INDEX idx_order_status_created ON order(status, created_at DESC);
CREATE INDEX idx_refund_record_order ON refund_record(order_id);
```

## API Design

### GET /api/admin/orders

- 鉴权：管理员登录 + `order:read`
- Query: `page`, `size`, `status`, `coach_id`, `user_id`, `start_date`, `end_date`
- Response 200 / 403

### GET /api/admin/orders/:id

- 鉴权：管理员登录 + `order:read`
- Response 200 / 404

### POST /api/admin/orders/:id/approve-refund

- 鉴权：管理员登录 + `order:write`
- Body: `{ amount, remark }`
- Response 200 / 400 / 403
- 错误码：`ORDER_STATUS_INVALID`, `REFUND_AMOUNT_MISMATCH`（v3 评审 P1 修复：原 `REFUND_AMOUNT_EXCEEDED` 全局统一为 `REFUND_AMOUNT_MISMATCH`）

### POST /api/admin/orders/:id/reject-refund

- 鉴权：管理员登录 + `order:write`
- Body: `{ reason }`
- Response 200 / 400 / 403

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis | `admin:orders:list:{hash}` | 60s | 订单状态变更时失效 |
| Redis | `admin:order:{id}` | 120s | 订单变更时失效 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 订单列表 P99 | < 300ms |
| 详情页 P99 | < 200ms |
| 退款处理 P99 | < 300ms |

## Security

- 登录 + RBAC
- 退款金额 ≤ paid_amount
- 操作日志不可修改

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-025 / US-027 | 被依赖 | 产生订单与退款申请 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-046-.../tech-design.md` §1 |
| API Design | `docs/stories/US-046-.../tech-design.md` §2 |
| Caching | `docs/stories/US-046-.../tech-design.md` §4 |
| Performance | `docs/stories/US-046-.../tech-design.md` §5 |
| Security | `docs/stories/US-046-.../tech-design.md` §6 |
