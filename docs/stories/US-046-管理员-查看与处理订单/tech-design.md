# US-046 技术设计：管理员查看与处理订单

> 本文档对应 `docs/stories/US-046-.../user-story.md` 的技术实现方案。
> 角色：开发 | 最后更新：2026-07-30

---

## 0. 文档定位

本文档为设计层，定义管理员订单查看与处理 refund 的数据模型、API、状态机及安全策略。执行步骤见 [./test-plan.md](./test-plan.md)。

---

## 1. 数据模型影响

### 1.1 读取/修改的表

| 表名 | 操作 | 说明 |
|------|------|------|
| `order` | 读取/修改 | 订单主表，字段：order_id, user_id, coach_id, amount, status, paid_amount |
| `package` | 读取/修改 | 退款批准后 status → refunded |
| `refund_record` | 新增 | 退款记录：refund_id, order_id, amount, reason, status, admin_id |
| `audit_log` | 新增 | 操作日志 |
| `user` / `coach` | 读取 | 列表/详情展示 |

### 1.2 索引

```sql
CREATE INDEX idx_order_status_created ON order(status, created_at DESC);
CREATE INDEX idx_refund_record_order ON refund_record(order_id);
```

---

## 2. API 设计

### 2.1 GET /api/admin/orders

- 鉴权：管理员登录 + `order:read`
- Query：`page`, `size`, `status`, `coach_id`, `user_id`, `start_date`, `end_date`
- Response 200：`{ items: OrderListItem[], total, page, size }`

### 2.2 GET /api/admin/orders/:id

- 鉴权：管理员登录 + `order:read`
- Response 200：订单详情含 refund_records
- Response 404：`{ error: 'ORDER_NOT_FOUND' }`

### 2.3 POST /api/admin/orders/:id/approve-refund

- 鉴权：管理员登录 + `order:write`
- Body：`{ amount, remark }`
- Response 200 / 400 / 409
- 错误码：`ORDER_STATUS_INVALID`, `REFUND_AMOUNT_EXCEEDED`

### 2.4 POST /api/admin/orders/:id/reject-refund

- 鉴权：管理员登录 + `order:write`
- Body：`{ reason }`
- Response 200 / 400

---

## 3. 状态机影响

```
order.status:
  退款审批中 ──[批准退款]──→ 已退款
  退款审批中 ──[拒绝退款]──→ 退款被拒

package.status:
  active ──[退款完成]──→ refunded
```

---

## 4. 缓存策略

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis | `admin:orders:list:{hash}` | 60s | 订单状态变更时失效 |
| Redis | `admin:order:{id}` | 120s | 订单变更时失效 |

---

## 5. 性能指标

| 指标 | 目标 |
|------|------|
| 订单列表 P50 | < 200ms |
| 订单列表 P99 | < 300ms |
| 详情页 P99 | < 200ms |
| 退款处理 P99 | < 300ms |

---

## 6. 安全 / 鉴权

- 所有接口登录 + RBAC
- 退款金额 ≤ paid_amount
- 操作日志不可修改
- 敏感字段（支付流水）脱敏展示

---

## 7. 跨 US 依赖

| US | 依赖方向 | 说明 |
|----|---------|------|
| US-025 / US-027 | 被依赖 | 产生订单与退款申请 |
| US-028 | 并行/被依赖 | 管理员处理退款原路退回可拆分或合并 |

---

## 8. 异常与边界

| 场景 | 处理 |
|------|------|
| 订单不存在 | 404 ORDER_NOT_FOUND |
| 状态非法 | 400 ORDER_STATUS_INVALID |
| 金额超额 | 400 REFUND_AMOUNT_EXCEEDED |
| 重复提交 | 幂等返回已有结果 |
| 无权限 | 403 FORBIDDEN |

---

## 9. 实现顺序（与 test-plan 的映射）

| tech-design 章节 | 对应 test-plan Task |
|------------------|---------------------|
| §1 数据模型 | Task 1, 2 |
| §2 API 设计 | Task 3, 4, 5 |
| §4 缓存策略 | Task 6 |

---

## 10. 上下游引用

- 上游需求：[./user-story.md](./user-story.md)
- 执行计划：[./test-plan.md](./test-plan.md)
- 全局规范：[docs/spec/tech-design/README.md](../../spec/tech-design/README.md)

---

## 11. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版 |
