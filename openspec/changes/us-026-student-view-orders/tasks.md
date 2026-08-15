# Tasks: US-026 学员查看订单列表与详情

> 本文档对应 `docs/stories/US-026-.../test-plan.md` 的 OpenSpec 映射版本。

## Task 1: Order Query Service [P0]

**Files:**
- Create: `backend/src/services/order-query.ts`
- Test: `backend/tests/services/order-query.test.ts`

**Spec coverage:** REQ-026-1 / REQ-026-2 全部场景

- [ ] **RED:** Write failing tests — list orders by user sorted by created_at desc with packageMode tag and status-specific fields (remainingSeconds/actions for pending, cancel_refund action for refunding, refundReason for refunded); detail returns order snapshot (package) + coach + payment + refund + refundRejectReason; reject other user's order; detail unaffected by packageTemplate changes after purchase
- [ ] **GREEN:** Implement `OrderQueryService.list(userId, pagination, status)` and `OrderQueryService.detail(userId, orderId)`; list returns packageMode, remainingSeconds, actions, refundReason per status; detail returns package snapshot from order and does not read packageTemplate
- [ ] **REFACTOR:** Extract reusable order DTO mapper
- [ ] **COMMIT:** `feat(order): add order query service`

## Task 2: POST /api/order/list & POST /api/order/detail [P0]

**Files:**
- Create: `backend/src/controllers/order.ts`, `backend/src/routes/order.ts`
- Test: `backend/tests/controllers/order.test.ts`

- [ ] **RED:** Write failing tests — 200 list with pagination; 200 detail; 403 forbidden; 404 not found; 401 guest
- [ ] **GREEN:** Implement controllers + routes
- [ ] **REFACTOR:** Share pagination parsing
- [ ] **COMMIT:** `feat(api): add order list and detail endpoints`

## Task 3: 微信小程序订单列表/详情页 [P1]

**Files:**
- Create: `miniapp-user/src/pages/orders/index.tsx`, `miniapp-user/src/pages/order-detail/index.tsx`
- Test: 对应测试文件

- [ ] **RED:** Write failing tests — renders order cards with status-specific buttons (cancel_pay/go_pay/cancel_refund) and remaining time; empty state; detail page shows refund entry, refund reason, or reject reason based on status
- [ ] **GREEN:** Implement pages
- [ ] **REFACTOR:** Extract `<OrderCard />`
- [ ] **COMMIT:** `feat(miniapp): add order list and detail pages`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-2），P1 选做（Task 3）
