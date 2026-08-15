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

## Task 2: POST /api/order/formal [P0]

**Files:**
- Create: `backend/src/controllers/order.ts`, `backend/src/routes/order.ts`
- Test: `backend/tests/controllers/order.test.ts`

- [ ] **RED:** 201 with orderId; 400 for each error code; 401 guest
- [ ] **GREEN:** Implement endpoint
- [ ] **REFACTOR:** Share DTO schema
- [ ] **COMMIT:** `feat(api): add POST /api/order/formal`

## Task 3: Agreement Status Endpoint [P0]

**Files:**
- Modify: controller/route
- Test: `backend/tests/controllers/order.test.ts`

**Spec coverage:** 协议版本校验

- [ ] **RED:** Returns required vs signed versions
- [ ] **GREEN:** Implement POST /api/agreement/status
- [ ] **REFACTOR:** Combine with agreement service
- [ ] **COMMIT:** `feat(api): add POST /api/agreement/status`

## Task 4: POST /api/guardian/verify [P0]

**Files:**
- Create/Modify: `backend/src/controllers/order.ts`, `backend/src/routes/order.ts`
- Test: `backend/tests/controllers/order.test.ts`

**Spec coverage:** 监护人手机号校验占位（MVP 阶段仅校验已填写，不真正发短信）

- [ ] **RED:** 200 when `guardianPhone` present; 400 `GUARDIAN_PHONE_REQUIRED` when missing
- [ ] **GREEN:** Implement endpoint with MVP-level validation
- [ ] **REFACTOR:** Extract guardian validation helper
- [ ] **COMMIT:** `feat(api): add POST /api/guardian/verify placeholder`

## Task 5: Idempotency & Boundaries [P1]

**Files:**
- Create: `backend/src/services/order-idempotency.ts`
- Test: `backend/tests/services/order-idempotency.test.ts`

- [ ] **RED:** Duplicate submit returns same unpaid order; invalid hours rejected
- [ ] **GREEN:** Implement idempotency key and hours validation
- [ ] **REFACTOR:** Reuse in other order creation flows
- [ ] **COMMIT:** `feat(order): add formal order idempotency and validation`

---

## 依赖接口说明

以下接口由其他 US 实现，本 US test-plan 不重复覆盖：
- `POST /api/package/detail`：由 US-019 测试计划覆盖，本 US 在确认订单页复用。
- `POST /api/order/pay`：由 US-025 测试计划覆盖，本 US 创建订单后跳转至 US-025 支付页。

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
