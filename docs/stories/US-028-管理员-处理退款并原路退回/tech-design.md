# Tech Design: US-028 管理员处理退款并原路退回

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `refund` | 读写 | 退款申请记录与状态 |
| `order` | 读写 | 更新退款状态与时间 |
| `refund_transaction` | 写 | 渠道退款流水 |
| `package` | 读写 | 状态更新为 refunded 或恢复 active |
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

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| `order` | 退款审批中 → 已退款 | 管理员批准且渠道退款受理 |
| `order` | 退款审批中 → 已支付 | 管理员驳回 |
| `refund` | 待审批 → 管理员批准 / 管理员驳回 | 管理员操作 |
| `package` | active/frozen → refunded | 管理员批准 |
| `package` | active（冻结）→ active | 管理员驳回 |

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
