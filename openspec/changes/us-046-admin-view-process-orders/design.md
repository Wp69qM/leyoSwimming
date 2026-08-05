# Design: US-046 管理员查看与处理订单

> 本文档对应 `docs/stories/US-046-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-046 是管理员后台订单管理 US，核心提供订单查询与退款处理能力，涉及 order/package/refund_record 三张表的状态变更。本 US 同时区分两个争议入口：用户侧申诉（PRD §6.10，进入「争议退款处理中」状态）与管理员侧标记异常（仅生成 support_ticket，不改变订单状态）。

## Data Model

### 读取/修改表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `order` | 查询与更新状态 | order_id, status, paid_amount, dispute_flag, dispute_reason |
| `package` | 退款完成后更新 | package_id, status |
| `refund_record` | 新增退款记录 | refund_id, order_id, amount, status |
| `support_ticket` | 标记争议时新增 | id, type=3, order_id, status |
| `audit_log` | 记录操作 | admin_id, action, snapshot |

### 索引

```sql
CREATE INDEX idx_order_status_created ON order(status, created_at DESC);
CREATE INDEX idx_refund_record_order ON refund_record(order_id);
```

## API Design

### GET /api/admin/v1/orders

- 鉴权：管理员登录 + `order:read`
- Query: `page`, `size`, `status`, `coach_id`, `user_id`, `start_date`, `end_date`
- Response 200 / 403

### GET /api/admin/v1/orders/:id

- 鉴权：管理员登录 + `order:read`
- Response 200 / 404

### POST /api/admin/v1/orders/:id/approve-refund

- 鉴权：管理员登录 + `order:write`
- Body: `{ amount, remark }`
- Response 200 / 400 / 403
- 错误码：`ORDER_STATUS_INVALID`, `REFUND_AMOUNT_MISMATCH`（v3 评审 P1 修复：原 `REFUND_AMOUNT_EXCEEDED` 全局统一为 `REFUND_AMOUNT_MISMATCH`）

### POST /api/admin/v1/orders/:id/reject-refund

- 鉴权：管理员登录 + `order:write`
- Body: `{ reason }`
- Response 200 / 400 / 403

### POST /api/admin/v1/orders/:id/mark-dispute

- 鉴权：管理员登录 + `order:write`
- Body: `{ reason }`
- Response 200 / 400 / 403 / 404
- 业务逻辑：
  - 校验订单存在且状态允许标记（非已退款/已取消终态）
  - 设置 `dispute_flag = true` 与 `dispute_reason = reason`，不改变订单状态
  - 自动生成 `support_ticket`：type=3（退款申诉），order_id 关联当前订单，status=0（pending）
  - 通知学员，写入 `audit_log`
- 与 US-049 边界：本 US 负责生成工单；US-049 负责工单的后续分配、回复与关闭

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis | `admin:orders:list:{hash}` | 60s | 订单状态变更时失效 |
| Redis | `admin:order:{id}` | 120s | 订单变更时失效 |

## State Machine

```
order.status:
  已支付 ──[用户提交特殊原因申诉，PRD §6.10]──→ 争议退款处理中
  争议退款处理中 ──[管理员批准退款]──→ 已退款（终态）
  争议退款处理中 ──[管理员拒绝申诉]──→ 已支付
  退款审批中 ──[管理员批准（阶段1受理）]──→ 退款处理中
  退款处理中 ──[渠道退款成功回调（阶段2成功）]──→ 已退款（终态）
  退款处理中 ──[渠道退款失败（阶段2失败）]──→ 退款审批中（回滚，重试队列）
  退款审批中 ──[拒绝退款]──→ 退款被拒

package.status:
  frozen ──[渠道退款成功（阶段2成功）]──→ refunded
  frozen ──[拒绝退款 或 渠道失败回滚]──→ active
```

> **争议退款处理中状态说明**：状态机图覆盖 PRD §6.2.2 状态 5「争议退款处理中」与 PRD §6.10 转换路径——用户提交特殊原因申诉时由「已支付」进入「争议退款处理中」，管理员批准则 → 已退款，管理员拒绝则 → 已支付。注意：管理员通过 `mark-dispute` 接口对退款审批中订单打 dispute_flag 不触发状态机转换，仅在 support_ticket 中记录申诉，订单仍保持退款审批中。

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
| US-053 | 本 US 依赖 | 后台管理系统登录认证入口 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-046-.../tech-design.md` §1 |
| API Design | `docs/stories/US-046-.../tech-design.md` §2 |
| Caching | `docs/stories/US-046-.../tech-design.md` §4 |
| Performance | `docs/stories/US-046-.../tech-design.md` §5 |
| Security | `docs/stories/US-046-.../tech-design.md` §6 |
