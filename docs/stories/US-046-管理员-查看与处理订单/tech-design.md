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
| `refund_record` | 读取 | 读取退款申请记录：refund_id, order_id, amount, reason, status, admin_id |
| `audit_log` | 新增 | 操作日志 |
| `user` / `coach` | 读取 | 列表/详情展示 |

### 1.2 索引

```sql
CREATE INDEX idx_order_status_created ON order(status, created_at DESC);
CREATE INDEX idx_refund_record_order ON refund_record(order_id);
```

---

## 2. API 设计

> 统一使用 POST，URL 按 `/api/{module}/{resource}/{action}`，参数通过 JSON body 传递，字段使用小驼峰。

### 2.1 POST /api/admin/order/list

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
- Response 200：`{ items: OrderListItem[], total, page, pageSize }`

### 2.2 POST /api/admin/order/detail

- 鉴权：管理员登录 + `order:read`
- Request：
  ```json
  {
    "orderId": 1
  }
  ```
- Response 200：订单详情含 refundRecords
- Response 404：`{ error: 'ORDER_NOT_FOUND' }`

### 2.3 POST /api/admin/order/approve-refund

- 鉴权：管理员登录 + `order:write`
- Request：
  ```json
  {
    "orderId": 1,
    "amount": 144000,
    "remark": "同意退款"
  }
  ```
- Response 202 / 400 / 409
- 错误码：`ORDER_STATUS_INVALID`, `REFUND_AMOUNT_INVALID`, `REFUND_AMOUNT_MISMATCH`, `REFUND_ALREADY_PROCESSED`

### 2.4 POST /api/admin/order/reject-refund

- 鉴权：管理员登录 + `order:write`
- Request：
  ```json
  {
    "orderId": 1,
    "reason": "不符合退款条件"
  }
  ```
- Response 200 / 400

---

## 3. 状态机影响

```
order.status:
  退款审批中 ──[管理员批准（阶段1受理）]──→ 退款处理中
  退款处理中 ──[渠道退款成功回调（阶段2成功）]──→ 已退款（终态）
  退款处理中 ──[渠道退款失败（阶段2失败）]──→ 退款审批中（回滚，重试队列）
  退款审批中 ──[拒绝退款]──→ 退款被拒

package.status:
  frozen ──[渠道退款成功（阶段2成功）]──→ refunded
  frozen ──[拒绝退款 或 渠道失败回滚]──→ active
```

> **对齐说明**（P1 修复）：本 US 状态机已对齐 US-028 P1 修复的两阶段退款时序与 PRD §6.2.2 v11.1 新增的「8 - 退款处理中」状态。原"退款审批中 → 已退款"的单步转换已废弃。
>
> **争议退款处理中状态说明**（v1.2 半落地修复）：状态机图补全 PRD §6.2.2 状态 5「争议退款处理中」与 PRD §6.10 转换路径——用户提交特殊原因申诉时由「已支付」进入「争议退款处理中」，管理员批准则 → 已退款，管理员拒绝则 → 已支付。本 US 不提供 mark-dispute 接口；争议退款处理流程由 US-049 客服工单承接。

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
- 退款金额需 ≥ 0；是否允许超过原支付金额 paid_amount 待产品确认
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
| 退款金额 < 0 | 400 REFUND_AMOUNT_INVALID |
| 退款金额超过原支付金额 | 400 REFUND_AMOUNT_MISMATCH |
| 重复提交 | 409 REFUND_ALREADY_PROCESSED / 幂等返回已有结果 |
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
| v1.1 | 2026-07-31 | Dev | v7 评审 P0 修复：§3 状态机补全 [争议退款处理中] 状态（PRD §6.2.2 状态 5 + §6.10）；新增「已支付 → 争议退款处理中（用户申诉）」「争议退款处理中 → 已退款/已支付（管理员批准/拒绝）」转换；明确管理员标记异常不触发状态机转换 |
| v1.2 | 2026-07-31 | Dev | 半落地修复：v1.1 变更日志声称 §3 状态机已补全「争议退款处理中」状态，但实际状态机图未画出该状态及「已支付 → 争议退款处理中」转换。本次将状态机图实际补全三条转换：已支付 → 争议退款处理中（用户申诉，PRD §6.10）、争议退款处理中 → 已退款（管理员批准）、争议退款处理中 → 已支付（管理员拒绝）；并补充与 §2.5 mark-dispute 接口的边界说明 |
