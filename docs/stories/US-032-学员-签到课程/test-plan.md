# Test Plan: US-032 学员签到/签退课程

## Task 1: Check-in Service — 签到窗口与幂等 [P0]

**Files:**
- Create: `backend/src/services/checkin.ts`
- Test: `backend/tests/services/checkin.test.ts`

**Spec coverage:** 全部 5 个 GWT 场景

- [ ] **RED:** Write failing tests — check-in within window records timestamp; outside window rejected; idempotent repeat; non-owner rejected
- [ ] **GREEN:** Implement `CheckinService.checkIn(userId, bookingId)` with window and ownership validation
- [ ] **REFACTOR:** Extract window calculator
- [ ] **COMMIT:** `feat(booking): add student check-in service`

## Task 2: Class Record & Summary Service [P0]

**Files:**
- Create: `backend/src/services/class-record.ts`
- Test: `backend/tests/services/class-record.test.ts`

- [ ] **RED:** Write failing tests — read record returns coach content; submit summary updates student_summary_json; empty record handled
- [ ] **GREEN:** Implement `ClassRecordService.getRecord(bookingId)` and `submitSummary(userId, bookingId, dto)`
- [ ] **REFACTOR:** Extract summary validator
- [ ] **COMMIT:** `feat(booking): add class record and summary service`

## Task 3: Check-in & Record API [P0]

**Files:**
- Create/Modify: controller/route
- Test: `backend/tests/controllers/checkin.test.ts`, `backend/tests/controllers/class-record.test.ts`

- [ ] **RED:** Write failing tests — 200 check-in; 200 get record; 200 submit summary; 400 errors; 403 forbidden
- [ ] **GREEN:** Implement endpoints
- [ ] **REFACTOR:** Share booking ownership validation
- [ ] **COMMIT:** `feat(api): add check-in and class record endpoints`

## Task 4: 小程序签到与记录页 [P1]

**Files:**
- Modify: `miniapp-user/src/pages/booking-detail/index.tsx`
- Create: `miniapp-user/src/pages/class-record/index.tsx`
- Test: 对应测试文件

- [ ] **RED:** Write failing tests — shows check-in button in window; hides outside window; renders record and summary form
- [ ] **GREEN:** Implement UI
- [ ] **REFACTOR:** Extract `<CheckInButton />` and `<SummaryForm />`
- [ ] **COMMIT:** `feat(miniapp): add check-in and class record pages`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
