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

## Task 2: POST /api/order/trial [P0]

**Files:**
- Create: `backend/src/controllers/order.ts`
- Create: `backend/src/routes/order.ts`
- Test: `backend/tests/controllers/order.test.ts`

- [ ] **RED:** 201 for valid; 400 for duplicate/unavailable coach; 401 for guest
- [ ] **GREEN:** Implement endpoint with auth and validation
- [ ] **REFACTOR:** Share order DTO schema
- [ ] **COMMIT:** `feat(api): add POST /api/order/trial`

## Task 3: POST /api/order/pay [P0]

**Files:**
- Modify: `backend/src/controllers/order.ts`, `backend/src/routes/order.ts`
- Test: `backend/tests/controllers/order.test.ts`

**Spec coverage:** 发起 Mock 支付，返回支付参数

- [ ] **RED:** 200 with mock payment params for valid order; 400 ORDER_EXPIRED; 404 ORDER_NOT_FOUND; 401 guest
- [ ] **GREEN:** Implement endpoint calling `MockPaymentProvider.pay(orderId, channel)`
- [ ] **REFACTOR:** Share payment creation logic with formal purchase flow
- [ ] **COMMIT:** `feat(api): add POST /api/order/pay`

## Task 4: POST /api/payment/mock-callback [P0]

**Files:**
- Create: `backend/src/services/payment-callback.ts`
- Create/Modify: `backend/src/controllers/order.ts`, `backend/src/routes/order.ts`
- Test: `backend/tests/services/payment-callback.test.ts`, `backend/tests/controllers/order.test.ts`

**Spec coverage:** Mock 渠道回调入口 + 事务内更新订单/创建套餐/更新身份

- [ ] **RED:** Controller returns 200 `{ code: "SUCCESS" }` on valid callback; service updates order to paid, activates package, promotes user to student; idempotent duplicate callback returns same response
- [ ] **GREEN:** Implement `POST /api/payment/mock-callback` controller and callback handler with signature verification
- [ ] **REFACTOR:** Extract payment provider adapter
- [ ] **COMMIT:** `feat(api): add POST /api/payment/mock-callback`

## Task 5: POST /api/agreement/status [P0]

**Files:**
- Modify: `backend/src/controllers/order.ts`, `backend/src/routes/order.ts`
- Test: `backend/tests/controllers/order.test.ts`

**Spec coverage:** 查询协议签署状态

- [ ] **RED:** 200 with signed status for required agreements; 200 with unsigned flag when any agreement missing
- [ ] **GREEN:** Implement endpoint to query 用户须知 / 健康承诺书 / 免责协议 sign status
- [ ] **REFACTOR:** Share agreement status helper with trial purchase service
- [ ] **COMMIT:** `feat(api): add POST /api/agreement/status`

## Task 5: Timeout Cancel Job [P1]

**Files:**
- Create: `backend/src/jobs/cancel-unpaid-orders.ts`
- Test: `backend/tests/jobs/cancel-unpaid-orders.test.ts`

- [ ] **RED:** 24h unpaid orders canceled with package rollback
- [ ] **GREEN:** Implement hourly scanner
- [ ] **REFACTOR:** Reuse cancel service
- [ ] **COMMIT:** `feat(order): add unpaid order cancellation job`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
