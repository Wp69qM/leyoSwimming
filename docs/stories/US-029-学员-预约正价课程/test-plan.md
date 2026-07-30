# Test Plan: US-029 学员预约正价课程

## Task 1: Booking Service — 预约创建与课时预占 [P0]

**Files:**
- Create: `backend/src/services/booking-create.ts`
- Test: `backend/tests/services/booking-create.test.ts`

**Spec coverage:** 全部 5 个 GWT 场景

- [ ] **RED:** Write failing tests — create booking with FIFO package selection; reject taken slot / no quota / no bound coach / unavailable slot; idempotent duplicate request
- [ ] **GREEN:** Implement `BookingCreateService.create(userId, coachId, slotId, idempotencyKey)`
- [ ] **REFACTOR:** Extract slot validator and package selector
- [ ] **COMMIT:** `feat(booking): add formal class booking service`

## Task 2: GET /api/coaches/{id}/slots & /api/packages/active [P0]

**Files:**
- Modify: existing controller/route from US-014 / US-021
- Test: `backend/tests/controllers/booking.test.ts`

- [ ] **RED:** Write failing tests — 200 slots list; 200 active packages; 401 guest
- [ ] **GREEN:** Ensure endpoints exposed and auth enforced
- [ ] **REFACTOR:** Share coach slot query
- [ ] **COMMIT:** `feat(api): expose slots and active packages for booking`

## Task 3: POST /api/bookings [P0]

**Files:**
- Create: `backend/src/controllers/booking.ts`, `backend/src/routes/booking.ts`
- Test: `backend/tests/controllers/booking.test.ts`

- [ ] **RED:** Write failing tests — 201 success; 400 SLOT_TAKEN / NO_QUOTA / NO_BOUND_COACH; 409 duplicate; 401 guest
- [ ] **GREEN:** Implement controller + route
- [ ] **REFACTOR:** Share ownership and package validation
- [ ] **COMMIT:** `feat(api): add POST /bookings for formal class`

## Task 4: 小程序预约页 [P1]

**Files:**
- Create: `miniapp-user/src/pages/booking/index.tsx`
- Test: `miniapp-user/src/pages/booking/index.test.tsx`

- [ ] **RED:** Write failing tests — renders slots; submits booking; handles errors
- [ ] **GREEN:** Implement page
- [ ] **REFACTOR:** Extract `<SlotGrid />`
- [ ] **COMMIT:** `feat(miniapp): add formal class booking page`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
