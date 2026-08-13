# Test Plan: US-025 学员支付套餐订单

## Task 1: Payment Service — 预支付创建与回调处理 [P0]

**Files:**
- Create: `backend/src/services/payment.ts`
- Test: `backend/tests/services/payment.test.ts`

**Spec coverage:** Mock 微信支付成功、Mock 支付宝支付成功、用户取消支付、订单超时、重复回调

- [ ] **RED:** Write failing tests — create mock payment for WeChat/Alipay; handle success callback by updating order.status and creating package; reject expired orders; idempotent duplicate callback
- [ ] **GREEN:** Implement `PaymentService.createMockPayment(orderId, channel)` and `PaymentService.handleMockCallback(payload)` with transaction
- [ ] **REFACTOR:** Extract `MockPaymentProvider` to encapsulate channel-specific mock transaction generation
- [ ] **COMMIT:** `feat(payment): add mock payment creation and callback handler`

## Task 2: POST /api/orders/{order_id}/pay [P0]

**Files:**
- Create: `backend/src/controllers/payment.ts`, `backend/src/routes/payment.ts`
- Test: `backend/tests/controllers/payment.test.ts`

**Spec coverage:** Mock 支付结果返回、订单过期校验、渠道校验

- [ ] **RED:** Write failing tests — 200 with mock payment result (`payment_id`, `channel_trade_no`, `status`); 400 ORDER_EXPIRED; 404 ORDER_NOT_FOUND; 401 guest
- [ ] **GREEN:** Implement controller + route; call `MockPaymentProvider.pay(...)` and trigger mock callback
- [ ] **REFACTOR:** Share order ownership validation
- [ ] **COMMIT:** `feat(api): add POST /orders/{id}/pay`

## Task 3: 支付回调接口 [P0]

**Files:**
- Modify: `backend/src/controllers/payment.ts`, `backend/src/routes/payment.ts`
- Test: `backend/tests/controllers/payment.test.ts`

**Spec coverage:** Mock 支付回调、幂等、金额校验

- [ ] **RED:** Write failing tests — 200 `{ code: "SUCCESS" }` on valid mock callback; duplicate callback returns same response; amount mismatch returns error
- [ ] **GREEN:** Implement `POST /api/payments/mock/callback` with idempotency check and transaction
- [ ] **REFACTOR:** Extract callback payload validation and order/package update into service methods
- [ ] **COMMIT:** `feat(api): add mock payment callback`

## Task 4: 订单过期定时任务 [P1]

**Files:**
- Create: `backend/src/jobs/expire-unpaid-orders.ts`
- Test: `backend/tests/jobs/expire-unpaid-orders.test.ts`

**Spec coverage:** 订单超时关闭

- [ ] **RED:** Write failing tests — orders older than 24h are cancelled; orders within 24h remain pending
- [ ] **GREEN:** Implement cron job
- [ ] **REFACTOR:** Extract schedule configuration
- [ ] **COMMIT:** `feat(job): expire unpaid orders after 24h`

## Task 5: 微信小程序支付页 [P1]

**Files:**
- Create: `miniapp-user/src/pages/order-pay/index.tsx`
- Test: `miniapp-user/src/pages/order-pay/index.test.tsx`

**Spec coverage:** 支付方式选择、调用 Mock 支付 API、支付结果轮询

- [ ] **RED:** Write failing tests — renders payment methods; calls `POST /api/orders/{order_id}/pay`; polls order status
- [ ] **GREEN:** Implement page; on confirm, call mock pay API and poll order status until paid/cancelled
- [ ] **REFACTOR:** Extract `<PaymentMethodSelector />`
- [ ] **COMMIT:** `feat(miniapp): add order payment page`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
