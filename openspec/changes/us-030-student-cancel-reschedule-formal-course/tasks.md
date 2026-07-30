# Tasks: US-030 学员取消/改约正价课程

> 本文档对应 `docs/stories/US-030-.../test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → REFACTOR → COMMIT 循环。

## Task 1: Cancellation Service — 24h 判断与申请创建 [P0]

**Files:**
- Create: `backend/src/services/booking-cancellation.ts`
- Test: `backend/tests/services/booking-cancellation.test.ts`

**Spec coverage:** REQ-030-1 全部场景

- [ ] **RED:** Write failing tests — cancel outside 24h releases package; inside 24h creates pending request; reject non-cancellable booking; prevent duplicate request
- [ ] **GREEN:** Implement `BookingCancellationService.cancel(userId, bookingId, reason)`
- [ ] **REFACTOR:** Extract 24h window calculator and cancellation validator
- [ ] **COMMIT:** `feat(booking): add cancellation service`

## Task 2: Coach Cancellation Approval Service [P0]

**Files:**
- Modify: service
- Test: `backend/tests/services/booking-cancellation.test.ts`

**Spec coverage:** REQ-030-2

- [ ] **RED:** Write failing tests — coach approve releases package; reject keeps booking; timeout marks rejected
- [ ] **GREEN:** Implement `BookingCancellationService.approve(cancellationId, coachId)` and `reject()`
- [ ] **REFACTOR:** Extract approval validator
- [ ] **COMMIT:** `feat(booking): add coach cancellation approval`

## Task 3: Cancellation API [P0]

**Files:**
- Create/Modify: controller/route
- Test: `backend/tests/controllers/booking-cancellation.test.ts`

**Spec coverage:** REQ-030-1 / REQ-030-2

- [ ] **RED:** Write failing tests — 200/201 cancel; 200 approve/reject; 400 errors; 403 forbidden
- [ ] **GREEN:** Implement endpoints
- [ ] **REFACTOR:** Share booking ownership validation
- [ ] **COMMIT:** `feat(api): add booking cancellation endpoints`

## Task 4: Cancellation Timeout Cron Job [P1]

**Files:**
- Create: `backend/src/jobs/expire-pending-cancellations.ts`
- Test: `backend/tests/jobs/expire-pending-cancellations.test.ts`

**Spec coverage:** REQ-030-2

- [ ] **RED:** Write failing tests — pending requests older than 24h are rejected
- [ ] **GREEN:** Implement cron job
- [ ] **REFACTOR:** Extract schedule config
- [ ] **COMMIT:** `feat(job): expire pending cancellation requests after 24h`

## Task 5: 小程序取消预约页 [P1]

**Files:**
- Modify: `miniapp-user/src/pages/booking-detail/index.tsx`
- Test: 对应测试文件

**Spec coverage:** REQ-030-1

- [ ] **RED:** Write failing tests — shows cancel button; submits reason; displays approval status
- [ ] **GREEN:** Implement UI
- [ ] **REFACTOR:** Extract `<CancelBookingModal />`
- [ ] **COMMIT:** `feat(miniapp): add booking cancellation flow`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-3），P1 选做（Task 4-5）
