# Tasks: US-021 学员查看我的套餐

## Task 1: My Packages Service [P0]

**Files:**
- Create: `backend/src/services/my-packages.ts`
- Test: `backend/tests/services/my-packages.test.ts`

**Spec coverage:** REQ-001 正常查看、空状态、冻结套餐

- [ ] **RED:** Returns grouped packages with summary; empty when none; frozen visible
- [ ] **GREEN:** Implement `MyPackagesService.findGroupedByUser`
- [ ] **REFACTOR:** Extract status group mapper
- [ ] **COMMIT:** `feat(package): add my packages query service`

## Task 2: GET /api/users/me/packages [P0]

**Files:**
- Create: `backend/src/controllers/my-packages.ts`, `backend/src/routes/my-packages.ts`
- Test: `backend/tests/controllers/my-packages.test.ts`

- [ ] **RED:** 200 grouped list; 200 empty; 401 guest
- [ ] **GREEN:** Implement endpoint
- [ ] **REFACTOR:** Share auth middleware
- [ ] **COMMIT:** `feat(api): add GET /api/users/me/packages`

---

## Execution Discipline

- 严格顺序：Task 1 → 2
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
