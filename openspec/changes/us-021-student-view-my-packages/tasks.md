# Tasks: US-021 学员查看我的套餐

## Task 1: My Packages Service [P0]

**Files:**
- Create: `backend/src/services/my-packages.ts`
- Test: `backend/tests/services/my-packages.test.ts`

**Spec coverage:** REQ-001 正常查看、空状态、冻结套餐、退款入口

- [ ] **RED:** Returns grouped packages with summary; empty when none; frozen visible; refund entry visible when eligible
- [ ] **GREEN:** Implement `MyPackagesService.findGroupedByUser`
- [ ] **REFACTOR:** Extract status group mapper
- [ ] **COMMIT:** `feat(package): add my packages query service`

## Task 2: POST /api/user/package/list [P0]

**Files:**
- Create: `backend/src/controllers/my-packages.ts`, `backend/src/routes/my-packages.ts`
- Test: `backend/tests/controllers/my-packages.test.ts`

- [ ] **RED:** 200 grouped list; 200 empty; 401 guest
- [ ] **GREEN:** Implement endpoint
- [ ] **REFACTOR:** Share auth middleware
- [ ] **COMMIT:** `feat(api): add POST /api/user/package/list`

## Task 3: Coach-view package usage detail [P1]

**Files:**
- Create: `backend/src/services/coach-package-detail.ts`, `backend/src/controllers/coach-package-detail.ts`
- Create: coach miniapp page `C-套餐使用详情页`
- Test: `backend/tests/services/coach-package-detail.test.ts`, `backend/tests/controllers/coach-package-detail.test.ts`

**Spec coverage:** REQ-004 教练视角套餐使用详情页

- [ ] **RED:** Returns package snapshot, summary, student mini-card, and booking history for coach-owned package; 403 for non-associated package
- [ ] **GREEN:** Implement `CoachPackageDetailService.findByIdForCoach` and `POST /api/coach/package/detail`
- [ ] **REFACTOR:** Share package snapshot mapper with user view
- [ ] **COMMIT:** `feat(package): add coach-view package usage detail`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
