# Test Plan: US-021 学员查看我的套餐

## Task 1: Package Query Service [P0]

**Files:**
- Create: `backend/src/services/my-packages.ts`
- Test: `backend/tests/services/my-packages.test.ts`

**Spec coverage:** 正常查看、空状态、冻结套餐

- [ ] **RED:** Returns packages grouped by status with summary; empty when none; frozen visible
- [ ] **GREEN:** Implement `MyPackagesService.findGroupedByUser`
- [ ] **REFACTOR:** Extract status group mapper
- [ ] **COMMIT:** `feat(package): add my packages query service`

## Task 2: POST /api/user/package/list [P0]

**Files:**
- Create: `backend/src/controllers/my-packages.ts`, `backend/src/routes/my-packages.ts`
- Test: `backend/tests/controllers/my-packages.test.ts`

- [ ] **RED:** 200 with grouped list; 200 empty; 401 guest
- [ ] **GREEN:** Implement endpoint
- [ ] **REFACTOR:** Share auth middleware
- [ ] **COMMIT:** `feat(api): add POST /api/user/package/list`

## Task 3: POST /api/coach/package/detail [P0]

**Files:**
- Modify: `backend/src/controllers/my-packages.ts`, `backend/src/routes/my-packages.ts` (or create `backend/src/controllers/coach-package-detail.ts`, `backend/src/routes/coach-package-detail.ts`)
- Test: `backend/tests/controllers/coach-package-detail.test.ts`

**Spec coverage:** 教练视角套餐使用详情页数据源

- [ ] **RED:** 200 with package snapshot, booking usage list, and student mini-card for associated coach; 403 NOT_ASSOCIATED_STUDENT for unauthorized coach; 404 when package not found
- [ ] **GREEN:** Implement endpoint; validate `packageId` belongs to current coach; return snapshot + booking records
- [ ] **REFACTOR:** Share package snapshot DTO with user package list
- [ ] **COMMIT:** `feat(api): add POST /api/coach/package/detail`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
