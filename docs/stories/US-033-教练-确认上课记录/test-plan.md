# Test Plan: US-033 教练确认上课记录

## Task 1: Coach Class Confirmation Service — 扣课时与记录 [P0]

**Files:**
- Create: `backend/src/services/class-confirmation.ts`
- Test: `backend/tests/services/class-confirmation.test.ts`

**Spec coverage:** 场景 1、3、4、5（教练确认相关）

- [ ] **RED:** Write failing tests — confirm updates booking to 已完成 and consumes hour; reject non-confirmable states; reject before class ends; reject wrong coach
- [ ] **GREEN:** Implement `ClassConfirmationService.confirm(coachId, bookingId, recordDto)` with transaction
- [ ] **REFACTOR:** Extract confirmation validator and course record builder
- [ ] **COMMIT:** `feat(booking): add coach class confirmation service`

## Task 1b: Coach Mark Absent Service — 旷课标记与扣课时 [P0]

**Files:**
- Create: `backend/src/services/mark-absent.ts`
- Test: `backend/tests/services/mark-absent.test.ts`

**Spec coverage:** 场景 2、边界场景 4

- [ ] **RED:** Write failing tests — mark absent updates booking to 旷课 and consumes hour; reject before class ends; reject non-confirmable states; reject wrong coach
- [ ] **GREEN:** Implement `MarkAbsentService.markAbsent(coachId, bookingId, remark?)` with transaction
- [ ] **REFACTOR:** Extract mark-absent validator
- [ ] **COMMIT:** `feat(booking): add coach mark absent service`

## Task 2: Confirmation API [P0]

**Files:**
- Create/Modify: controller/route
- Test: `backend/tests/controllers/class-confirmation.test.ts`

**Spec coverage:** 场景 1、3、4、5（教练确认相关）

- [ ] **RED:** Write failing tests — 200 confirm; 400 CLASS_NOT_ENDED / BOOKING_NOT_CONFIRMABLE; 403 forbidden
- [ ] **GREEN:** Implement endpoints
- [ ] **REFACTOR:** Share booking/coach validation
- [ ] **COMMIT:** `feat(api): add class confirmation endpoints`

## Task 2b: Mark Absent API [P0]

**Files:**
- Create/Modify: controller/route
- Test: `backend/tests/controllers/mark-absent.test.ts`

**Spec coverage:** 场景 2、边界场景 4

- [ ] **RED:** Write failing tests — 200 mark-absent; 400 CLASS_NOT_ENDED / BOOKING_NOT_CONFIRMABLE; 403 forbidden
- [ ] **GREEN:** Implement endpoint
- [ ] **REFACTOR:** Share booking/coach validation
- [ ] **COMMIT:** `feat(api): add coach mark absent endpoint`

## Task 3: 教练端课程确认页 [P1]

**Files:**
- Create: `miniapp-coach/src/pages/class-confirm/index.tsx`
- Test: 对应测试文件

**Spec coverage:** 场景 1、场景 2

- [ ] **RED:** Write failing tests — renders record form; submits confirmation; submits mark-absent; shows success
- [ ] **GREEN:** Implement UI
- [ ] **REFACTOR:** Extract `<ClassRecordForm />` and `<MarkAbsentButton />`
- [ ] **COMMIT:** `feat(miniapp-coach): add class confirmation page`

---

## Execution Discipline

- 严格顺序：Task 1 → 1b → 2 → 2b → 3
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder

---

## 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版：Task 1-3（仅覆盖教练确认） |
| v1.1 | 2026-07-31 | Dev | P0 修复：新增 Task 1b/2b 覆盖旷课标记服务与 API；Task 1/2 spec coverage 限定为教练确认场景；Task 3 RED 增加旷课提交；execution discipline 增加 1b/2b |
