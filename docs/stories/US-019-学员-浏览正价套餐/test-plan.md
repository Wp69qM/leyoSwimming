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

## Task 2: GET /api/coaches/:id/packages [P0]

**Files:**
- Create: `backend/src/controllers/coach-package.ts`, `backend/src/routes/coach-package.ts`
- Test: `backend/tests/controllers/coach-package.test.ts`

**Spec coverage:** API 端点 + 错误码

- [ ] **RED:** 200 with packages; 404 for invalid coach; empty standard list returns custom entry only
- [ ] **GREEN:** Implement endpoint
- [ ] **REFACTOR:** Share DTO with service
- [ ] **COMMIT:** `feat(api): add GET /api/coaches/:id/packages`

---

## Execution Discipline

- 严格顺序：Task 1 → 2
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
