# Test Plan: US-033 教练确认上课记录

## Task 1: Coach Class Confirmation Service — 扣课时与记录 [P0]

**Files:**
- Create: `backend/src/services/class-confirmation.ts`
- Test: `backend/tests/services/class-confirmation.test.ts`

**Spec coverage:** 全部 5 个 GWT 场景

- [ ] **RED:** Write failing tests — confirm updates booking to 已完成 and consumes hour; reject non-confirmable states; reject before start; reject wrong coach
- [ ] **GREEN:** Implement `ClassConfirmationService.confirm(coachId, bookingId, recordDto)` with transaction
- [ ] **REFACTOR:** Extract confirmation validator and course record builder
- [ ] **COMMIT:** `feat(booking): add coach class confirmation service`

## Task 2: Admin Hour Return Service [P0]

**Files:**
- Modify: service
- Test: `backend/tests/services/class-confirmation.test.ts`

- [ ] **RED:** Write failing tests — return hour increases available and decreases consumed; reject when no consumed hour
- [ ] **GREEN:** Implement `ClassConfirmationService.returnHour(adminId, bookingId, reason, hours)`
- [ ] **REFACTOR:** Extract return validator
- [ ] **COMMIT:** `feat(booking): add admin hour return service`

## Task 3: Confirmation & Return API [P0]

**Files:**
- Create/Modify: controller/route
- Test: `backend/tests/controllers/class-confirmation.test.ts`

- [ ] **RED:** Write failing tests — 200 confirm; 200 return hour; 400 errors; 403 forbidden
- [ ] **GREEN:** Implement endpoints
- [ ] **REFACTOR:** Share booking/coach validation
- [ ] **COMMIT:** `feat(api): add class confirmation and hour return endpoints`

## Task 4: 教练端课程确认页 [P1]

**Files:**
- Create: `miniapp-coach/src/pages/class-confirm/index.tsx`
- Test: 对应测试文件

- [ ] **RED:** Write failing tests — renders record form; submits confirmation; shows success
- [ ] **GREEN:** Implement UI
- [ ] **REFACTOR:** Extract `<ClassRecordForm />`
- [ ] **COMMIT:** `feat(miniapp-coach): add class confirmation page`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
