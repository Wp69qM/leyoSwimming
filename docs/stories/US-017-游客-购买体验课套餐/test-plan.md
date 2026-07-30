# Test Plan: US-017 游客购买体验课套餐

## Task 1: Trial Package Domain Service — 资格校验 [P0]

**Files:**
- Create: `backend/src/services/trial-purchase.ts`
- Test: `backend/tests/services/trial-purchase.test.ts`

**Spec coverage:** 正常购买、已有套餐、教练不可约

- [ ] **RED:** Valid purchase allowed; duplicate rejected; unavailable coach rejected
- [ ] **GREEN:** Implement eligibility check and package/order creation
- [ ] **REFACTOR:** Extract eligibility rules
- [ ] **COMMIT:** `feat(trial): add trial purchase eligibility and order creation`

## Task 2: POST /api/orders/trial [P0]

**Files:**
- Create: `backend/src/controllers/order.ts`
- Create: `backend/src/routes/order.ts`
- Test: `backend/tests/controllers/order.test.ts`

- [ ] **RED:** 201 for valid; 400 for duplicate/unavailable coach; 401 for guest
- [ ] **GREEN:** Implement endpoint with auth and validation
- [ ] **REFACTOR:** Share order DTO schema
- [ ] **COMMIT:** `feat(api): add POST /api/orders/trial`

## Task 3: Payment Callback [P0]

**Files:**
- Create: `backend/src/services/payment-callback.ts`
- Test: `backend/tests/services/payment-callback.test.ts`

- [ ] **RED:** Callback updates order to paid, activates package, promotes user to student; idempotent
- [ ] **GREEN:** Implement callback handler with signature verification
- [ ] **REFACTOR:** Extract payment provider adapter
- [ ] **COMMIT:** `feat(payment): add trial payment callback`

## Task 4: Timeout Cancel Job [P1]

**Files:**
- Create: `backend/src/jobs/cancel-unpaid-orders.ts`
- Test: `backend/tests/jobs/cancel-unpaid-orders.test.ts`

- [ ] **RED:** 24h unpaid orders canceled with package rollback
- [ ] **GREEN:** Implement hourly scanner
- [ ] **REFACTOR:** Reuse cancel service
- [ ] **COMMIT:** `feat(order): add unpaid order cancellation job`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
