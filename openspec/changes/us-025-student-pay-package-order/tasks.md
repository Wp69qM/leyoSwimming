# Tasks: US-025 学员支付套餐订单

> 本文档对应 `docs/stories/US-025-.../test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → REFACTOR → COMMIT 循环。

## Task 1: Payment Service — 预支付创建与回调处理 [P0]

**Files:**
- Create: `backend/src/services/payment.ts`
- Test: `backend/tests/services/payment.test.ts`

**Spec coverage:** REQ-025-1 / REQ-025-2 全部场景

- [ ] **RED:** Write failing tests — create prepay params for WeChat/Alipay; handle success callback by updating order.status and creating package from order snapshot fields; reject expired orders; idempotent duplicate callback; ignore package_template changes after order creation
- [ ] **GREEN:** Implement `PaymentService.createPrepay(orderId, channel)` and `PaymentService.handleCallback(channel, payload)` with transaction; package creation must read snapshot fields from order and must not query package_template
- [ ] **REFACTOR:** Extract channel-specific signature verification into adapters
- [ ] **COMMIT:** `feat(payment): add prepay creation and callback handler`

## Task 2: POST /api/orders/{order_id}/pay [P0]

**Files:**
- Create: `backend/src/controllers/payment.ts`, `backend/src/routes/payment.ts`
- Test: `backend/tests/controllers/payment.test.ts`

- [ ] **RED:** Write failing tests — 200 with prepay params; 400 ORDER_EXPIRED; 404 ORDER_NOT_FOUND; 401 guest
- [ ] **GREEN:** Implement controller + route
- [ ] **REFACTOR:** Share order ownership validation
- [ ] **COMMIT:** `feat(api): add POST /orders/{id}/pay`

## Task 3: 支付回调接口 [P0]

**Files:**
- Modify: controller/route
- Test: `backend/tests/controllers/payment.test.ts`

- [ ] **RED:** Write failing tests — 200 SUCCESS on valid WeChat callback; 200 "success" on valid Alipay callback; duplicate callback returns same response
- [ ] **GREEN:** Implement callback endpoints with signature verification
- [ ] **REFACTOR:** Extract callback parsing to channel adapters
- [ ] **COMMIT:** `feat(api): add payment callbacks for wechat and alipay`

## Task 4: 订单过期定时任务 [P1]

**Files:**
- Create: `backend/src/jobs/expire-unpaid-orders.ts`
- Test: `backend/tests/jobs/expire-unpaid-orders.test.ts`

- [ ] **RED:** Write failing tests — orders older than 24h are cancelled; orders within 24h remain pending; concurrent waitlist promotion competes for `inventory:{coach_id}` lock
- [ ] **GREEN:** Implement cron job with inventory lock
- [ ] **REFACTOR:** Extract schedule configuration
- [ ] **COMMIT:** `feat(job): expire unpaid orders after 24h`

## Task 5: 微信小程序支付页 [P1]

**Files:**
- Create: `miniapp-user/src/pages/order-pay/index.tsx`
- Test: `miniapp-user/src/pages/order-pay/index.test.tsx`

- [ ] **RED:** Write failing tests — renders payment methods; calls pay API; polls order status
- [ ] **GREEN:** Implement page with Taro.requestPayment
- [ ] **REFACTOR:** Extract `<PaymentMethodSelector />`
- [ ] **COMMIT:** `feat(miniapp): add order payment page`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-3），P1 选做（Task 4-5）
