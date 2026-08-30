# Tasks: US-022 学员更换绑定教练

## Task 1: Coach Unsubscribe Service — 第一步 [P0]

**Files:**
- Create: `backend/src/services/coach-change/unsubscribe.ts`
- Test: `backend/tests/services/coach-change/unsubscribe.test.ts`

**Spec coverage:** REQ-001 正常更换绑定教练 / 旧套餐存在未取消预约 / 多套餐同时冻结 / 超时回滚

- [ ] **RED:** Write failing tests — active package frozen with `frozen_reason=refund_pending`; `refund_record` created with `status=pending_change`; reserved bookings rejected; multiple packages handled; `original_status` preserved
- [ ] **GREEN:** Implement `CoachChangeUnsubscribeService.unsubscribe(userId, packageId)`
- [ ] **REFACTOR:** Extract refund calculator and eligibility validator
- [ ] **COMMIT:** `feat(coach-change): add unsubscribe step for coach change`

---

## Task 2: Coach Purchase Service — 第二步 [P0]

**Files:**
- Create: `backend/src/services/coach-change/purchase.ts`
- Test: `backend/tests/services/coach-change/purchase.test.ts`

**Spec coverage:** REQ-001 正常更换绑定教练 / 自定义课时更换教练 / 新教练不可用 / 新教练与当前教练相同 / 未退订直接购新 / 两步操作超过 24 小时未发起第二步

- [ ] **RED:** Write failing tests — valid purchase refunds old package and creates new order; rejects unavailable/same/no-pending-change/expired; preserves frozen state on rejection
- [ ] **GREEN:** Implement `CoachChangePurchaseService.purchase(userId, dto)`
- [ ] **REFACTOR:** Extract new order creation reuse from US-020
- [ ] **COMMIT:** `feat(coach-change): add purchase step for coach change`

---

## Task 3: Expired Rollback Job [P0]

**Files:**
- Create: `backend/src/jobs/rollback-expired-coach-changes.ts`
- Test: `backend/tests/jobs/rollback-expired-coach-changes.test.ts`

**Spec coverage:** REQ-001 两步操作超过 24 小时未发起第二步

- [ ] **RED:** Write failing tests — `pending_change` records older than 24h restore original package status; `refund_record` cancelled; identity recalculated
- [ ] **GREEN:** Implement cron job scanning `refund_record.status = pending_change` with `created_at < NOW() - INTERVAL 24 HOUR`
- [ ] **REFACTOR:** Extract rollback transaction helper
- [ ] **COMMIT:** `feat(job): rollback expired coach change requests after 24h`

---

## Task 4: GET /api/coaches/available-for-change [P0]

**Files:**
- Modify: `backend/src/controllers/coach.ts`, `backend/src/routes/coach.ts`
- Test: `backend/tests/controllers/coach.test.ts`

**Spec coverage:** REQ-001 正常更换绑定教练

- [ ] **RED:** Returns `status=1` coaches excluding current bound coach; 401 guest
- [ ] **GREEN:** Implement endpoint
- [ ] **REFACTOR:** Reuse coach list filter
- [ ] **COMMIT:** `feat(api): add GET /api/coaches/available-for-change`

---

## Task 5: POST /api/users/me/coach/unsubscribe [P0]

**Files:**
- Create: `backend/src/controllers/coach-change/unsubscribe.ts`, `backend/src/routes/coach-change.ts`
- Test: `backend/tests/controllers/coach-change.test.ts`

**Spec coverage:** REQ-001 正常更换绑定教练 / 旧套餐存在未取消预约

- [ ] **RED:** 200 with refund records and `valid_before`; 400 for `NO_ACTIVE_PACKAGE` / `PENDING_BOOKINGS`
- [ ] **GREEN:** Implement endpoint
- [ ] **REFACTOR:** Share DTO schema with service
- [ ] **COMMIT:** `feat(api): add POST /api/users/me/coach/unsubscribe`

---

## Task 6: POST /api/users/me/coach/purchase [P0]

**Files:**
- Create: `backend/src/controllers/coach-change/purchase.ts`
- Modify: `backend/src/routes/coach-change.ts`
- Test: `backend/tests/controllers/coach-change.test.ts`

**Spec coverage:** REQ-001 正常更换绑定教练 / 自定义课时更换教练 / 新教练不可用 / 新教练与当前教练相同 / 未退订直接购新 / 两步操作超过 24 小时未发起第二步

- [ ] **RED:** 201 with refund records and new order; 400 for `COACH_UNAVAILABLE` / `SAME_COACH` / `ACTIVE_PACKAGE_EXISTS` / `COACH_CHANGE_EXPIRED` / `AGREEMENT_REQUIRED`
- [ ] **GREEN:** Implement endpoint
- [ ] **REFACTOR:** Share DTO schema with order controller
- [ ] **COMMIT:** `feat(api): add POST /api/users/me/coach/purchase`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
