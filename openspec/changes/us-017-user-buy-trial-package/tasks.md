# Tasks: US-017 游客购买体验课套餐

## Task 1: Trial Purchase Service [P0]

**Files:**
- Create: `backend/src/services/trial-purchase.ts`
- Test: `backend/tests/services/trial-purchase.test.ts`

**Spec coverage:** REQ-001 正常/重复/教练不可约/未签署协议

- [ ] **RED:** Valid allowed; duplicate rejected; unavailable coach rejected; missing agreement rejected
- [ ] **GREEN:** Implement eligibility and order/package creation
- [ ] **REFACTOR:** Extract eligibility rules
- [ ] **COMMIT:** `feat(trial): add purchase eligibility and order creation`

## Task 2: POST /api/orders/trial [P0]

**Files:**
- Create: `backend/src/controllers/order.ts`, `backend/src/routes/order.ts`
- Test: `backend/tests/controllers/order.test.ts`

- [ ] **RED:** 201 valid; 400 duplicate/unavailable; 401 guest
- [ ] **GREEN:** Implement endpoint
- [ ] **REFACTOR:** Share DTO schema
- [ ] **COMMIT:** `feat(api): add POST /api/orders/trial`

## Task 3: Payment Callback [P0]

**Files:**
- Create: `backend/src/services/payment-callback.ts`
- Test: `backend/tests/services/payment-callback.test.ts`

- [ ] **RED:** Callback updates order, activates package, upgrades identity; idempotent
- [ ] **GREEN:** Implement with signature verification
- [ ] **REFACTOR:** Provider adapter
- [ ] **COMMIT:** `feat(payment): add trial payment callback`

## Task 4: Timeout Cancel Job [P1]

**Files:**
- Create: `backend/src/jobs/cancel-unpaid-orders.ts`
- Test: `backend/tests/jobs/cancel-unpaid-orders.test.ts`

- [ ] **RED:** 24h unpaid orders canceled with rollback
- [ ] **GREEN:** Implement hourly scanner
- [ ] **REFACTOR:** Reuse cancel service
- [ ] **COMMIT:** `feat(order): add unpaid order cancellation job`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
