# Tasks: US-032 学员签到/签退课程

> 本文档对应 `docs/stories/US-032-.../test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → REFACTOR → COMMIT 循环。

## Task 1: Check-in Service — 签到窗口与幂等 [P0]

**Files:**
- Create: `backend/src/services/checkin.ts`
- Test: `backend/tests/services/checkin.test.ts`

**Spec coverage:** REQ-032-1

- [ ] **RED:** Write failing tests — check-in within window records timestamp; outside window rejected; idempotent repeat; non-owner rejected
- [ ] **GREEN:** Implement `CheckinService.checkIn(userId, bookingId)` with window and ownership validation
- [ ] **REFACTOR:** Extract window calculator
- [ ] **COMMIT:** `feat(booking): add student check-in service`

## Task 2: Class Record & Summary Service [P0]

**Files:**
- Create: `backend/src/services/class-record.ts`
- Test: `backend/tests/services/class-record.test.ts`

**Spec coverage:** REQ-032-2

- [ ] **RED:** Write failing tests — read record returns coach content; submit summary updates student_summary_json; empty record handled
- [ ] **GREEN:** Implement `ClassRecordService.getRecord(bookingId)` and `submitSummary(userId, bookingId, dto)`
- [ ] **REFACTOR:** Extract summary validator
- [ ] **COMMIT:** `feat(booking): add class record and summary service`

## Task 3: Check-in & Record API [P0]

**Files:**
- Create/Modify: controller/route
- Test: `backend/tests/controllers/checkin.test.ts`, `backend/tests/controllers/class-record.test.ts`

**Spec coverage:** REQ-032-1 / REQ-032-2

- [ ] **RED:** Write failing tests — 200 check-in; 200 get record; 200 submit summary; 400 errors; 403 forbidden
- [ ] **GREEN:** Implement endpoints
- [ ] **REFACTOR:** Share booking ownership validation
- [ ] **COMMIT:** `feat(api): add check-in and class record endpoints`

## Task 4: 小程序签到与记录页 [P1]

**Files:**
- Modify: `miniapp-user/src/pages/booking-detail/index.tsx`
- Create: `miniapp-user/src/pages/class-record/index.tsx`
- Test: 对应测试文件

**Spec coverage:** REQ-032-1 / REQ-032-2

- [ ] **RED:** Write failing tests — shows check-in button in window; hides outside window; renders record and summary form
- [ ] **GREEN:** Implement UI
- [ ] **REFACTOR:** Extract `<CheckInButton />` and `<SummaryForm />`
- [ ] **COMMIT:** `feat(miniapp): add check-in and class record pages`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-3），P1 选做（Task 4）
