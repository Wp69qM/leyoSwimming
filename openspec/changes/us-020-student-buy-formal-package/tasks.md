# Tasks: US-020 学员购买正价套餐

## Task 1: Formal Purchase Service [P0]

**Files:**
- Create: `backend/src/services/formal-purchase.ts`
- Test: `backend/tests/services/formal-purchase.test.ts`

**Spec coverage:** REQ-001 标准套餐、自定义课时、协议、教练冲突、教练不可用、未成年人监护人

- [ ] **RED:** Valid standard/custom orders; reject missing agreement / conflict / unavailable coach / missing guardian phone for minor
- [ ] **GREEN:** Implement eligibility, amount calculation, order creation
- [ ] **REFACTOR:** Extract eligibility rules and amount calculator
- [ ] **COMMIT:** `feat(formal): add formal package purchase service`

## Task 2: POST /api/orders/formal [P0]

**Files:**
- Create: `backend/src/controllers/order.ts`, `backend/src/routes/order.ts`
- Test: `backend/tests/controllers/order.test.ts`

- [ ] **RED:** 201 valid; 400 for each error code; 401 guest
- [ ] **GREEN:** Implement endpoint
- [ ] **REFACTOR:** Share DTO schema
- [ ] **COMMIT:** `feat(api): add POST /api/orders/formal`

## Task 3: GET /api/agreements/status [P0]

**Files:**
- Modify: controller/route
- Test: `backend/tests/controllers/order.test.ts`

- [ ] **RED:** Returns required vs signed versions
- [ ] **GREEN:** Implement endpoint
- [ ] **REFACTOR:** Combine with agreement service
- [ ] **COMMIT:** `feat(api): add GET /api/agreements/status`

## Task 4: Idempotency & Validation [P1]

**Files:**
- Create: `backend/src/services/order-idempotency.ts`
- Test: `backend/tests/services/order-idempotency.test.ts`

- [ ] **RED:** Duplicate submit returns same unpaid order; invalid hours rejected
- [ ] **GREEN:** Implement idempotency key and hours validation
- [ ] **REFACTOR:** Reuse in other order flows
- [ ] **COMMIT:** `feat(order): add formal order idempotency and validation`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
