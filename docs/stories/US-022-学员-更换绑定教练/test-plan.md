# Test Plan: US-022 学员更换绑定教练

## Task 1: Coach Change Service [P0]

**Files:**
- Create: `backend/src/services/coach-change.ts`
- Test: `backend/tests/services/coach-change.test.ts`

**Spec coverage:** 正常更换、自定义课时、教练不可用、同教练、存在预约

- [ ] **RED:** Valid change refunds old packages and creates new order; rejects unavailable/same/no-active/pending bookings
- [ ] **GREEN:** Implement eligibility, refund calculation, order creation
- [ ] **REFACTOR:** Extract refund calculator and eligibility rules
- [ ] **COMMIT:** `feat(coach): add coach change service`

## Task 2: GET /api/coaches/available-for-change [P0]

**Files:**
- Modify: `backend/src/controllers/coach.ts`, `backend/src/routes/coach.ts`
- Test: `backend/tests/controllers/coach.test.ts`

- [ ] **RED:** Returns status=1 coaches excluding current bound coach; 401 guest
- [ ] **GREEN:** Implement endpoint
- [ ] **REFACTOR:** Reuse coach list filter
- [ ] **COMMIT:** `feat(api): add GET /api/coaches/available-for-change`

## Task 3: POST /api/users/me/coach/change [P0]

**Files:**
- Create: `backend/src/controllers/coach-change.ts`, `backend/src/routes/coach-change.ts`
- Test: `backend/tests/controllers/coach-change.test.ts`

- [ ] **RED:** 200 with refund records and new order; 400 for each error code
- [ ] **GREEN:** Implement endpoint
- [ ] **REFACTOR:** Share DTO schema with order controller
- [ ] **COMMIT:** `feat(api): add POST /api/users/me/coach/change`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
