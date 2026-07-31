# US-046 实施任务清单（Tasks）

> **US 关联**：[user-story.md](./user-story.md)（US-046 管理员查看与处理订单）
> **文档角色**：TDD 任务清单 + 测试计划
> **作者**：开发　|　**最后更新**：2026-07-30

---

## 0. 双重角色说明

本文档承担 TDD 任务清单与测试计划双重角色，覆盖 user-story.md 所有 GWT 场景。

---

## 1. Task 概览

| Task | 标题 | 优先级 | 对应 GWT 场景 |
|------|------|--------|--------------|
| 1 | Order Repository 查询与退款状态校验 | P0 | §6.1, §6.2, §6.6 |
| 2 | Refund Service 批准/拒绝退款 | P0 | §6.3, §6.5, §6.7 |
| 3 | GET /api/admin/orders 列表 API | P0 | §6.1 |
| 4 | GET /api/admin/orders/:id 详情 API | P0 | §6.2 |
| 5 | POST /api/admin/orders/:id/approve-refund | P0 | §6.3, §6.6, §6.7 |
| 6 | POST /api/admin/orders/:id/reject-refund | P0 | §6.5 |
| 7 | POST /api/admin/orders/:id/mark-dispute | P0 | §6.8 |
| 8 | 订单缓存与幂等 | P1 | §6.1, §6.3 |

---

## 2. 实施任务

### Task 1: Order Repository 查询与退款状态校验 [P0]

**Files:**
- Create: `backend/src/repositories/adminOrder.ts`
- Test: `backend/tests/repositories/adminOrder.test.ts`

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1管理员查看订单列表)、[§6.2 场景 2](./user-story.md#62-场景-2管理员查看订单详情)、[§6.6 场景 6](./user-story.md#66-场景-6管理员对非退款审批中订单执行退款)

- [ ] **Step 1: RED**

```typescript
// backend/tests/repositories/adminOrder.test.ts
import { AdminOrderRepository } from '../../src/repositories/adminOrder';

describe('AdminOrderRepository', () => {
  it('lists orders with pagination and status filter', async () => {
    const repo = new AdminOrderRepository();
    const result = await repo.list({ page: 1, size: 20, status: 2 }); // 2=已支付
    expect(result.items.length).toBeLessThanOrEqual(20);
  });

  it('throws ORDER_STATUS_INVALID when approving non-refunding order', async () => {
    const repo = new AdminOrderRepository();
    await expect(repo.approveRefund(1, 2000)).rejects.toThrow('ORDER_STATUS_INVALID');
  });
});
```

- [ ] **Step 2-6**: 实现、跑测试、重构、commit：`feat(admin-order): add repository and status validation`

---

### Task 2: Refund Service 批准/拒绝退款 [P0]

**Files:**
- Create: `backend/src/services/refund.ts`
- Test: `backend/tests/services/refund.test.ts`

**对应 GWT**：[§6.3 场景 3](./user-story.md#63-场景-3管理员批准退款受理成功)、[§6.5 场景 5](./user-story.md#65-场景-5管理员拒绝退款)、[§6.7 场景 7](./user-story.md#67-场景-7退款金额超过已支付金额)

- [ ] **Step 1-6**: 实现退款金额校验、package 状态变更、refund_record 写入；commit：`feat(refund): add admin approve/reject refund service`

---

### Task 3: GET /api/admin/orders 列表 API [P0]

**Files:**
- Create: `backend/src/controllers/admin/order.ts`
- Create: `backend/src/routes/admin/order.ts`
- Test: `backend/tests/controllers/admin/order.test.ts`

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1管理员查看订单列表与详情)

- [ ] **Step 1-6**: 实现列表查询；commit：`feat(admin): add GET /orders`

---

### Task 4: GET /api/admin/orders/:id 详情 API [P0]

**Files:**
- Modify: `backend/src/controllers/admin/order.ts`
- Modify: `backend/src/routes/admin/order.ts`
- Modify: `backend/tests/controllers/admin/order.test.ts`

**对应 GWT**：[§6.2 场景 2](./user-story.md#62-场景-2管理员查看订单详情)

- [ ] **Step 1-6**: 实现详情查询；commit：`feat(admin): add GET /orders/:id`

---

### Task 5: POST /api/admin/orders/:id/approve-refund [P0]

**Files:**
- Modify: `backend/src/controllers/admin/order.ts`
- Modify: `backend/src/routes/admin/order.ts`
- Modify: `backend/tests/controllers/admin/order.test.ts`

**对应 GWT**：[§6.3 场景 3](./user-story.md#63-场景-3管理员批准退款受理成功)、[§6.6 场景 6](./user-story.md#66-场景-6管理员对非退款审批中订单执行退款)、[§6.7 场景 7](./user-story.md#67-场景-7退款金额超过已支付金额)

- [ ] **Step 1-6**: 实现批准退款接口；commit：`feat(admin): add approve-refund endpoint`

---

### Task 6: POST /api/admin/orders/:id/reject-refund [P0]

**Files:**
- Modify: `backend/src/controllers/admin/order.ts`
- Modify: `backend/src/routes/admin/order.ts`
- Modify: `backend/tests/controllers/admin/order.test.ts`

**对应 GWT**：[§6.5 场景 5](./user-story.md#65-场景-5管理员拒绝退款)

- [ ] **Step 1-6**: 实现拒绝退款接口；commit：`feat(admin): add reject-refund endpoint`

---

### Task 7: POST /api/admin/orders/:id/mark-dispute [P0]

**Files:**
- Modify: `backend/src/controllers/admin/order.ts`
- Modify: `backend/src/routes/admin/order.ts`
- Modify: `backend/tests/controllers/admin/order.test.ts`

**对应 GWT**：[§6.8 场景 8](./user-story.md#68-场景-8管理员标记订单为争议退款并生成客服工单)

- [ ] **Step 1-6**: 实现标记争议退款接口，校验订单状态、写入 `order.dispute_flag` / `dispute_reason`、生成 `support_ticket`、通知学员；commit：`feat(admin): add mark-dispute endpoint`

---

### Task 8: 订单缓存与幂等 [P1]

**Files:**
- Modify: `backend/src/repositories/adminOrder.ts`
- Modify: `backend/src/services/cache.ts`
- Test: `backend/tests/repositories/adminOrder.test.ts`

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1管理员查看订单列表)、[§6.3 场景 3](./user-story.md#63-场景-3管理员批准退款受理成功)

- [ ] **Step 1-6**: 加 Redis 缓存与幂等键；commit：`feat(cache): add admin order cache and idempotency`

---

## 3. 任务执行纪律

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
- P0 必做，P1 视进度

---

## 4. 上下游引用

- 上游需求：[./user-story.md](./user-story.md)
- 设计输入：[./tech-design.md](./tech-design.md)
- Figma：[./user-story.md](./user-story.md#13-figma-链接) §13-15

---

## 5. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版：7 个 task 覆盖 5 个 GWT 场景 |
