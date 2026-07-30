# Tasks: US-018 体验课学员预约与取消

## Task 1: Trial Booking Service [P0]

**Files:**
- Create: `backend/src/services/trial-booking.ts`
- Test: `backend/tests/services/trial-booking.test.ts`

**Spec coverage:** REQ-001 all scenarios

- [ ] **RED:** Valid succeeds; no package rejected; taken slot rejected
- [ ] **GREEN:** Implement with slot lock and pre-occupation
- [ ] **REFACTOR:** Extract eligibility checker
- [ ] **COMMIT:** `feat(booking): add trial booking service`

## Task 2: POST /api/bookings/trial [P0]

**Files:**
- Create: `backend/src/controllers/booking.ts`, `backend/src/routes/booking.ts`
- Test: `backend/tests/controllers/booking.test.ts`

- [ ] **RED:** 201 valid; 400 no package; 409 taken
- [ ] **GREEN:** Implement endpoint
- [ ] **REFACTOR:** Share slot status check
- [ ] **COMMIT:** `feat(api): add POST /api/bookings/trial`

## Task 3: Cancel Service [P0]

**Files:**
- Modify: `backend/src/services/trial-booking.ts`
- Test: `backend/tests/services/trial-booking.test.ts`

**Spec coverage:** REQ-002 all scenarios

- [ ] **RED:** Free cancel releases; within 24h creates request
- [ ] **GREEN:** Implement cancel logic
- [ ] **REFACTOR:** Extract time window calculator
- [ ] **COMMIT:** `feat(booking): add trial cancel service`

## Task 4: POST /api/bookings/{id}/cancel [P0]

**Files:**
- Modify: controller/route
- Test: `backend/tests/controllers/booking.test.ts`

- [ ] **RED:** 200 free; 202 pending
- [ ] **GREEN:** Implement endpoint
- [ ] **REFACTOR:** Reuse ownership check
- [ ] **COMMIT:** `feat(api): add cancel booking endpoint`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
