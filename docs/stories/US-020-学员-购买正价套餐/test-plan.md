# Test Plan: US-020 学员购买正价套餐

## Task 1: Formal Purchase Service [P0]

**Files:**
- Create: `backend/src/services/formal-purchase.ts`
- Test: `backend/tests/services/formal-purchase.test.ts`

**Spec coverage:** 标准套餐下单、自定义课时、协议校验、教练冲突、教练不可用

- [ ] **RED:** Valid standard order created; valid custom order amount correct; reject missing agreement / coach conflict / unavailable coach
- [ ] **GREEN:** Implement eligibility, amount calculation, order creation
- [ ] **REFACTOR:** Extract eligibility rules and amount calculator
- [ ] **COMMIT:** `feat(formal): add formal package purchase service`

## Task 2: POST /api/orders/formal [P0]

**Files:**
- Create: `backend/src/controllers/order.ts`, `backend/src/routes/order.ts`
- Test: `backend/tests/controllers/order.test.ts`

- [ ] **RED:** 201 with order_id; 400 for each error code; 401 guest
- [ ] **GREEN:** Implement endpoint
- [ ] **REFACTOR:** Share DTO schema
- [ ] **COMMIT:** `feat(api): add POST /api/orders/formal`

## Task 3: Agreement Status Endpoint [P0]

**Files:**
- Modify: controller/route
- Test: `backend/tests/controllers/order.test.ts`

**Spec coverage:** 协议版本校验

- [ ] **RED:** Returns required vs signed versions
- [ ] **GREEN:** Implement GET /api/agreements/status
- [ ] **REFACTOR:** Combine with agreement service
- [ ] **COMMIT:** `feat(api): add GET /api/agreements/status`

## Task 4: Idempotency & Boundaries [P1]

**Files:**
- Create: `backend/src/services/order-idempotency.ts`
- Test: `backend/tests/services/order-idempotency.test.ts`

- [ ] **RED:** Duplicate submit returns same unpaid order; invalid hours rejected
- [ ] **GREEN:** Implement idempotency key and hours validation
- [ ] **REFACTOR:** Reuse in other order creation flows
- [ ] **COMMIT:** `feat(order): add formal order idempotency and validation`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
