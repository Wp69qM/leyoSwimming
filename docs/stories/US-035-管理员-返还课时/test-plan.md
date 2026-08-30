# Test Plan: US-035 管理员返还课时

## Task 1: Hour Return Service [P0]

**Files:**
- Create: `backend/src/services/hour-return.ts`
- Test: `backend/tests/services/hour-return.test.ts`

**Spec coverage:** 返还课时、状态校验、exhausted→active 复活、expired→active 复活并延长 expire_at

- [ ] **RED:** Return success: consumed-1, available+1, hour_return created, audit_log created; reject NO_CONSUMED_HOUR; reject BOOKING_NOT_RETURNABLE; revive exhausted→active when consumed < total; revive expired→active and extend expire_at by original validity period when original status is expired and available > 0 after return
- [ ] **GREEN:** Implement return logic with transaction + optimistic lock + conditional expire_at extension for expired revival
- [ ] **REFACTOR:** Extract package status recompute and expire_at extension helper
- [ ] **COMMIT:** `feat(refund): add hour return service`

## Task 2: Admin Hour Return Service — 防重复返还与并发 [P0]

**Files:**
- Create: `backend/src/services/admin-hour-return.ts`
- Test: `backend/tests/services/admin-hour-return.test.ts`

**Spec coverage:** 重复返还校验、累计返还课时上限

- [ ] **RED:** Write failing tests — second return rejected with `RETURN_QUOTA_EXCEEDED` when returned_hours_sum >= consumed_count; concurrent coach confirm handled by optimistic lock
- [ ] **GREEN:** Implement cumulative returned_hours check and optimistic lock in `AdminHourReturnService.returnHour(adminId, bookingId, reasonType, reasonDetail)`
- [ ] **REFACTOR:** Extract returned-hours calculator and return-quota validator
- [ ] **COMMIT:** `feat(refund): add admin hour return service with quota guard`

## Task 3: POST /api/admin/bookings/:id/return-hour [P0]

**Files:**
- Create: `backend/src/controllers/admin-hour-return.ts`, `backend/src/routes/admin-hour-return.ts`
- Test: `backend/tests/controllers/admin-hour-return.test.ts`

**Spec coverage:** API 端点 + 权限校验

- [ ] **RED:** 200 success; 400 `NO_CONSUMED_HOUR` / `BOOKING_NOT_RETURNABLE` / `RETURN_QUOTA_EXCEEDED`; 401 guest; 403 non-admin or no permission
- [ ] **GREEN:** Implement endpoint with auth middleware
- [ ] **REFACTOR:** —
- [ ] **COMMIT:** `feat(api): add admin return-hour endpoint`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
