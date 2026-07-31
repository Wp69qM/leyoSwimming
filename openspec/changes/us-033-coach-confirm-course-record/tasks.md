# Tasks: US-033 教练确认上课记录

> 本文档对应 `docs/stories/US-033-.../test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → REFACTOR → COMMIT 循环。

## Task 1: Coach Class Confirmation Service — 扣课时与记录 [P0]

**Files:**
- Create: `backend/src/services/class-confirmation.ts`
- Test: `backend/tests/services/class-confirmation.test.ts`

**Spec coverage:** REQ-033-1

- [ ] **RED:** Write failing tests — confirm updates booking to 已完成 and consumes hour; reject non-confirmable states; reject before class ends; reject wrong coach
- [ ] **GREEN:** Implement `ClassConfirmationService.confirm(coachId, bookingId, recordDto)` with transaction
- [ ] **REFACTOR:** Extract confirmation validator and course record builder
- [ ] **COMMIT:** `feat(booking): add coach class confirmation service`

## Task 1b: Coach Mark Absent Service — 旷课标记与扣课时 [P0]

**Files:**
- Create: `backend/src/services/mark-absent.ts`
- Test: `backend/tests/services/mark-absent.test.ts`

**Spec coverage:** REQ-033-2

- [ ] **RED:** Write failing tests — mark absent updates booking to 旷课 and consumes hour; reject before class ends; reject non-confirmable states; reject wrong coach
- [ ] **GREEN:** Implement `MarkAbsentService.markAbsent(coachId, bookingId, remark?)` with transaction
- [ ] **REFACTOR:** Extract mark-absent validator
- [ ] **COMMIT:** `feat(booking): add coach mark absent service`

## Task 2: Confirmation API [P0]

**Files:**
- Create/Modify: controller/route
- Test: `backend/tests/controllers/class-confirmation.test.ts`

**Spec coverage:** REQ-033-1

- [ ] **RED:** Write failing tests — 200 confirm; 400 CLASS_NOT_ENDED / BOOKING_NOT_CONFIRMABLE; 403 forbidden
- [ ] **GREEN:** Implement endpoints
- [ ] **REFACTOR:** Share booking/coach validation
- [ ] **COMMIT:** `feat(api): add class confirmation endpoints`

## Task 2b: Mark Absent API [P0]

**Files:**
- Create/Modify: controller/route
- Test: `backend/tests/controllers/mark-absent.test.ts`

**Spec coverage:** REQ-033-2

- [ ] **RED:** Write failing tests — 200 mark-absent; 400 CLASS_NOT_ENDED / BOOKING_NOT_CONFIRMABLE; 403 forbidden
- [ ] **GREEN:** Implement endpoint
- [ ] **REFACTOR:** Share booking/coach validation
- [ ] **COMMIT:** `feat(api): add coach mark absent endpoint`

## Task 3: 教练端课程确认页 [P1]

**Files:**
- Create: `miniapp-coach/src/pages/class-confirm/index.tsx`
- Test: 对应测试文件

**Spec coverage:** REQ-033-1 / REQ-033-2

- [ ] **RED:** Write failing tests — renders record form; submits confirmation; submits mark-absent; shows success
- [ ] **GREEN:** Implement UI
- [ ] **REFACTOR:** Extract `<ClassRecordForm />` and `<MarkAbsentButton />`
- [ ] **COMMIT:** `feat(miniapp-coach): add class confirmation page`

---

## Execution Discipline

- 严格顺序：Task 1 → 1b → 2 → 2b → 3
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-2 / 1b-2b），P1 选做（Task 3）
