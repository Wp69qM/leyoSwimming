# Test Plan: US-035 管理员返还课时

## Task 1: Hour Return Service [P0]

**Files:**
- Create: `backend/src/services/hour-return.ts`
- Test: `backend/tests/services/hour-return.test.ts`

**Spec coverage:** 返还课时、状态校验、exhausted→active 复活

- [ ] **RED:** Return success: consumed-1, available+1, hour_return created, audit_log created; reject NO_CONSUMED_HOUR; reject BOOKING_NOT_RETURNABLE; revive exhausted→active when consumed < total
- [ ] **GREEN:** Implement return logic with transaction + optimistic lock
- [ ] **REFACTOR:** Extract package status recompute
- [ ] **COMMIT:** `feat(refund): add hour return service`

## Task 2: POST /api/admin/bookings/:id/return-hour [P0]

**Files:**
- Create: `backend/src/controllers/admin-hour-return.ts`, `backend/src/routes/admin-hour-return.ts`
- Test: `backend/tests/controllers/admin-hour-return.test.ts`

**Spec coverage:** API 端点 + 权限校验

- [ ] **RED:** 200 success; 400 for error codes; 401 guest; 403 non-admin or no permission
- [ ] **GREEN:** Implement endpoint with auth middleware
- [ ] **REFACTOR:** —
- [ ] **COMMIT:** `feat(api): add admin return-hour endpoint`

---

## Execution Discipline

- 严格顺序：Task 1 → 2
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
