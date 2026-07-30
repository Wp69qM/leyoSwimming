# Test Plan: US-018 体验课学员预约与取消

## Task 1: Trial Booking Service [P0]

**Files:**
- Create: `backend/src/services/trial-booking.ts`
- Test: `backend/tests/services/trial-booking.test.ts`

**Spec coverage:** 正常预约、无套餐、时段被占

- [ ] **RED:** Valid booking succeeds; no package rejected; taken slot rejected
- [ ] **GREEN:** Implement booking with slot lock and package pre-occupation
- [ ] **REFACTOR:** Extract eligibility checker
- [ ] **COMMIT:** `feat(booking): add trial booking service`

## Task 2: POST /api/bookings/trial [P0]

**Files:**
- Create: `backend/src/controllers/booking.ts`, `backend/src/routes/booking.ts`
- Test: `backend/tests/controllers/booking.test.ts`

- [ ] **RED:** 201 valid; 400 no package; 409 taken slot
- [ ] **GREEN:** Implement endpoint
- [ ] **REFACTOR:** Share slot status check
- [ ] **COMMIT:** `feat(api): add POST /api/bookings/trial`

## Task 3: Cancel Booking Service [P0]

**Files:**
- Modify: `backend/src/services/trial-booking.ts`
- Test: `backend/tests/services/trial-booking.test.ts`

**Spec coverage:** 24h 外取消、24h 内取消

- [ ] **RED:** Free cancel releases package; within 24h creates cancel_request
- [ ] **GREEN:** Implement cancel logic
- [ ] **REFACTOR:** Extract time window calculator
- [ ] **COMMIT:** `feat(booking): add trial cancel service`

## Task 4: POST /api/bookings/{id}/cancel [P0]

**Files:**
- Modify: controller/route
- Test: `backend/tests/controllers/booking.test.ts`

- [ ] **RED:** 200 free cancel; 202 pending approval
- [ ] **GREEN:** Implement endpoint
- [ ] **REFACTOR:** Reuse ownership check
- [ ] **COMMIT:** `feat(api): add cancel booking endpoint`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
