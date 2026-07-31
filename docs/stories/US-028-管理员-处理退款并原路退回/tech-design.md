# Tech Design: US-028 管理员处理退款并原路退回

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `refund` | 读写 | 退款申请记录与状态 |
| `order` | 读写 | 更新退款状态与时间 |
| `refund_transaction` | 写 | 渠道退款流水 |
| `package` | 读/写 | 审批期间保持 active，渠道成功时更新为 refunded，驳回时 booking_frozen = false |
| `payment` | 读 | 获取原支付渠道与流水号 |
| `user` | 读/触发 | 身份重算 |

#### refund_transaction

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `refund_id` | FK |
| `channel` | 0=微信 / 1=支付宝 |
| `channel_refund_no` | 渠道退款单号 |
| `amount` | 退款金额（分）|
| `status` | 处理中 / 成功 / 失败 |
| `failure_reason` | 失败原因 |
| `created_at` | 创建时间 |
| `updated_at` | 更新时间 |

### 1.2 索引

```sql
CREATE INDEX idx_refund_status ON refund(status);
CREATE INDEX idx_refund_order ON refund(order_id);
CREATE INDEX idx_refund_transaction_refund ON refund_transaction(refund_id);
CREATE INDEX idx_order_status ON order(status);
CREATE INDEX idx_package_refund ON package(order_id, status);
```

## 2. API 设计

### 2.1 GET /api/admin/refunds

- **鉴权**：管理员
- **Query**: `page`, `size`, `status`
- **Response 200**: `{ items: RefundListItem[], total, page, size }`

### 2.2 GET /api/admin/refunds/{refund_id}

- **鉴权**：管理员
- **Response 200**: `{ refund_id, order, package, payment, amount, reason, status, audit_log }`
- **Response 404**: `REFUND_NOT_FOUND`

### 2.3 POST /api/admin/refunds/{refund_id}/approve

- **鉴权**：管理员
- **Request**: `{ amount?: number, remark?: string }`
- **Response 200**: `{ refund_id, refund_transaction_id, status }`
- **Response 400**: `INVALID_REFUND_AMOUNT | REFUND_ALREADY_PROCESSED`
- **Response 403**: `FORBIDDEN`
- **Response 404**: `REFUND_NOT_FOUND`

### 2.4 POST /api/admin/refunds/{refund_id}/reject

- **鉴权**：管理员
- **Request**: `{ reason: string }`
- **Response 200**: `{ refund_id, status: "管理员驳回" }`
- **Response 400**: `REFUND_ALREADY_PROCESSED`
- **Response 403**: `FORBIDDEN`

## 3. 状态机

> **两阶段退款时序**（v3 评审 P0 修复，对齐 PRD §6.2.1/§6.2.2 与 user-story §7.3）：批准后不直接到「已退款」，先进入中间态「退款处理中」，待渠道回调成功后才到「已退款」；渠道失败则回滚到「退款审批中」。

### 3.1 order 状态机

| # | 实体 | 转换 | 触发条件 | 说明 |
|---|------|------|---------|------|
| 1 | `order` | 退款审批中（4）→ 退款处理中（8） | 管理员批准且渠道退款受理 | 中间态，等待渠道回调 |
| 2 | `order` | 退款处理中（8）→ 已退款（6） | 渠道退款成功回调 | 终态；同步 package active→refunded |
| 3 | `order` | 退款处理中（8）→ 退款审批中（4） | 渠道退款失败/超时 | 回滚中间态；package 保持 active，booking_frozen 保持 true，管理员可重试 |
| 4 | `order` | 退款审批中（4）→ 退款被拒（7） | 管理员驳回 | 终态；package 保持 active，booking_frozen = false |

### 3.2 refund 状态机

| # | 实体 | 转换 | 触发条件 |
|---|------|------|---------|
| 1 | `refund` | 待审批 → 管理员批准 | 管理员批准（order 进入退款处理中） |
| 2 | `refund` | 待审批 → 管理员驳回 | 管理员驳回（order 进入退款被拒） |
| 3 | `refund` | 管理员批准 → 退款成功 | 渠道退款成功回调 |
| 4 | `refund` | 管理员批准 → 退款失败 | 渠道退款失败/超时（可重试） |

### 3.3 package 状态机

| # | 实体 | 转换 | 触发条件 | 说明 |
|---|------|------|---------|------|
| 1 | `package` | active → active（保持） | US-027 学员提交退款申请 | 由 US-027 触发，退款审批期间 status 不变，booking_frozen = true |
| 2 | `package` | active → refunded | order 进入已退款（渠道成功回调后） | 终态 |
| 3 | `package` | active → active（保持） | order 进入退款被拒（管理员驳回）或渠道失败回滚 | booking_frozen 保持 true（失败回滚）或置 false（驳回） |

## 4. 缓存

- 订单详情缓存：`order:detail:{order_id}`，状态变更后删除
- package 缓存：`package:{package_id}`，状态变更后删除
- 退款列表缓存：`admin:refunds:{status}:{page}:{size}`，TTL 60s，审批后删除

## 5. 性能

| 指标 | 目标 |
|------|------|
| 退款列表查询 P99 | < 200ms |
| 退款详情查询 P99 | < 100ms |
| 审批接口 P99 | < 800ms（含渠道退款接口调用）|
| 渠道退款重试间隔 | 5min，最多 10 次 |

## 6. 安全

- 接口仅对管理员角色开放
- 退款金额不得超过 refund.amount / order.paid_amount
- 渠道密钥安全存储，不暴露给前端
- 审批操作记录审计日志
- 幂等键防止重复审批

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-027 | 依赖 | 退款申请 |
| US-046 | 被依赖 | 管理员订单管理可查看退款状态 |

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 批准标准退款 | `test_admin_refund_approve_success` |
| 驳回退款 | `test_admin_refund_reject` |
| 渠道退款失败 | `test_admin_refund_channel_fail` |
| 重复审批 | `test_admin_refund_approve_idempotent` |
| 非管理员越权 | `test_admin_refund_forbidden` |

## 9. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版 |
| v1.1 | 2026-07-31 | Dev | v3 评审 P0 修复：§3 状态机重构为两阶段时序（退款审批中→退款处理中→已退款/退款审批中），对齐 PRD §6.2.1/§6.2.2 与 user-story §7.3；新增 §3.1/§3.2/§3.3 三表分别覆盖 order/refund/package 状态机 |
| v1.2 | 2026-07-31 | Dev | P0 修复：退款审批期间 package.status 保持 active，通过 booking_frozen 冻结约课能力；§1/§3 同步调整 |
