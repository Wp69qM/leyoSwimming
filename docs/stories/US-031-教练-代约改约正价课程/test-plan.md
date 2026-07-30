# Test Plan: US-031 教练代约/改约正价课程

## Task 1: Coach Booking Service — 教练代约 [P0]

**Files:**
- Create: `backend/src/services/coach-booking.ts`
- Test: `backend/tests/services/coach-booking.test.ts`

**Spec coverage:** 场景 1（成功代约）、场景 3（时段已被预约）、场景 4（学员未绑定当前教练）

- [ ] **RED:** Write failing tests — coach books for student with FIFO package selection; reject taken slot / no quota / no bound coach / unavailable slot
- [ ] **GREEN:** Implement `CoachBookingService.bookForStudent(coachId, studentId, slotId, idempotencyKey)`
- [ ] **REFACTOR:** Extract slot validator, package selector, and ownership checker
- [ ] **COMMIT:** `feat(booking): add coach book-for-student service`

## Task 2: Coach Reschedule Service — 教练改约 [P0]

**Files:**
- Modify: `backend/src/services/coach-booking.ts`
- Test: `backend/tests/services/coach-booking.test.ts`

**Spec coverage:** 场景 2（成功改约）、场景 5（改约时 booking 已取消）

- [ ] **RED:** Write failing tests — reschedule cancels old booking and creates new one; reject non-reschedulable states; reject taken new slot
- [ ] **GREEN:** Implement `CoachBookingService.reschedule(coachId, bookingId, newSlotId, idempotencyKey)` within transaction
- [ ] **REFACTOR:** Extract reschedule validator and reuse booking creation logic
- [ ] **COMMIT:** `feat(booking): add coach reschedule-for-student service`

## Task 3: Coach Booking API [P0]

**Files:**
- Create: `backend/src/controllers/coach-booking.ts`, `backend/src/routes/coach-booking.ts`
- Test: `backend/tests/controllers/coach-booking.test.ts`

**Spec coverage:** 全部 5 个 GWT 场景

- [ ] **RED:** Write failing tests — 201 book; 200 reschedule; 400 errors; 403 forbidden
- [ ] **GREEN:** Implement controller + route
- [ ] **REFACTOR:** Share ownership validation with student booking controller
- [ ] **COMMIT:** `feat(api): add coach booking and reschedule endpoints`

## Task 4: 教练端代约/改约页 [P1]

**Files:**
- Create: `miniapp-coach/src/pages/student-booking/index.tsx`
- Modify: `miniapp-coach/src/pages/class-schedule/index.tsx`
- Test: 对应测试文件

**Spec coverage:** 场景 1、场景 2

- [ ] **RED:** Write failing tests — renders student list; submits book/reschedule; handles errors
- [ ] **GREEN:** Implement pages
- [ ] **REFACTOR:** Extract `<StudentSlotGrid />` and `<RescheduleModal />`
- [ ] **COMMIT:** `feat(miniapp-coach): add book/reschedule for student pages`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
