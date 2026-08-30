# Test Plan: US-019 学员浏览正价套餐

## Task 1: Coach Package Service [P0]

**Files:**
- Create: `backend/src/services/coach-package.ts`
- Test: `backend/tests/services/coach-package.test.ts`

**Spec coverage:** 标准套餐列表、参考单价、自定义课时入口、教练状态校验

- [ ] **RED:** Return standard packages + reference price for valid coach; return custom_package_enabled=false when no reference price; return COACH_NOT_FOUND for status ∉ {1,4}
- [ ] **GREEN:** Implement query with coach status filter + package join
- [ ] **REFACTOR:** Extract coach status visibility rule
- [ ] **COMMIT:** `feat(package): add coach package browsing service`

## Task 2: POST /api/package/list [P0]

**Files:**
- Create: `backend/src/controllers/package.ts`, `backend/src/routes/package.ts`
- Test: `backend/tests/controllers/package.test.ts`

**Spec coverage:** API 端点 + 错误码

- [ ] **RED:** 200 with all active packages grouped by packageMode; 200 empty when no active templates
- [ ] **GREEN:** Implement endpoint; filter by `status = 'active'` and group by `packageMode`
- [ ] **REFACTOR:** Share list query with coach package service
- [ ] **COMMIT:** `feat(api): add POST /api/package/list`

## Task 3: POST /api/coach/package/list [P0]

**Files:**
- Create: `backend/src/controllers/coach-package.ts`, `backend/src/routes/coach-package.ts`
- Test: `backend/tests/controllers/coach-package.test.ts`

**Spec coverage:** API 端点 + 错误码

- [ ] **RED:** 200 with packages; 404 for invalid coach; empty standard list returns custom entry only (coachId passed in JSON body)
- [ ] **GREEN:** Implement endpoint; read coachId from JSON body
- [ ] **REFACTOR:** Share DTO with service
- [ ] **COMMIT:** `feat(api): add POST /api/coach/package/list`

## Task 4: POST /api/package/detail [P0]

**Files:**
- Modify: `backend/src/controllers/package.ts`, `backend/src/routes/package.ts`
- Test: `backend/tests/controllers/package.test.ts`

**Spec coverage:** API 端点 + 下架校验

- [ ] **RED:** 200 with template detail and coach list when packageId valid; 404 PACKAGE_NOT_FOUND when inactive or missing
- [ ] **GREEN:** Implement endpoint; accept optional `coachId` in JSON body; return current coach info when provided
- [ ] **REFACTOR:** Share detail DTO with package list
- [ ] **COMMIT:** `feat(api): add POST /api/package/detail`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
