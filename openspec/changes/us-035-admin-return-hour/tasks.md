# Tasks: US-035 管理员返还课时

> 本文档对应 `docs/stories/US-035-.../test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → REFACTOR → COMMIT 循环。

## Task 1: Hour Return Service — 返还课时与状态复活 [P0]

**Files:**
- Create: `backend/src/services/hour-return.ts`
- Test: `backend/tests/services/hour-return.test.ts`

**Spec coverage:** REQ-035-001

- [ ] **RED:** Write failing tests — return success decreases consumed and increases available, creates hour_return + audit_log + notification; reject `NO_CONSUMED_HOUR` when consumed=0; reject `BOOKING_NOT_RETURNABLE` for status ∉ {已完成, 旷课}; revive `exhausted → active` when consumed < total after return; revive `expired → active` and extend `expire_at` by original validity period when original status is `expired` and available > 0 after return; reject duplicate submission via idempotency key; optimistic lock conflict when concurrent with coach confirm
- [ ] **GREEN:** Implement `HourReturnService.return(adminId, bookingId, reasonType, reasonDetail)` with DB transaction + optimistic lock on package.consumed_count + package status recompute + conditional `expire_at` extension for `expired → active` revival
- [ ] **REFACTOR:** Extract package status recompute helper and returnable validator
- [ ] **COMMIT:** `feat(refund): add hour return service`

## Task 2: Admin Hour Return Service — Quota Guard [P0]

**Files:**
- Create: `backend/src/services/admin-hour-return.ts`
- Test: `backend/tests/services/admin-hour-return.test.ts`

**Spec coverage:** REQ-035-001

- [ ] **RED:** Write failing tests — second return on same booking rejected with `RETURN_QUOTA_EXCEEDED` when `returned_hours_sum >= consumed_count`; concurrent coach confirm handled by optimistic lock
- [ ] **GREEN:** Implement cumulative returned-hours check and optimistic lock in `AdminHourReturnService.returnHour(adminId, bookingId, reasonType, reasonDetail)`
- [ ] **REFACTOR:** Extract returned-hours calculator and return-quota validator
- [ ] **COMMIT:** `feat(refund): add admin hour return service with quota guard`

## Task 3: POST /api/admin/bookings/:id/return-hour [P0]

**Files:**
- Create: `backend/src/controllers/admin-hour-return.ts`
- Create: `backend/src/routes/admin-hour-return.ts`
- Test: `backend/tests/controllers/admin-hour-return.test.ts`

**Spec coverage:** REQ-035-001

- [ ] **RED:** Write failing tests — 200 success returns updated package; 400 `NO_CONSUMED_HOUR`; 400 `BOOKING_NOT_RETURNABLE`; 400 `INVALID_REASON_TYPE` for missing reason_type; 400 `RETURN_QUOTA_EXCEEDED`; 401 for guest; 403 for non-admin or admin without `MANAGE_BOOKING`
- [ ] **GREEN:** Implement endpoint with admin auth middleware + `MANAGE_BOOKING` permission check + request body validation + delegation to `HourReturnService`
- [ ] **REFACTOR:** Share admin permission guard with other admin booking endpoints
- [ ] **COMMIT:** `feat(api): add admin return-hour endpoint`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-3）
- 测试覆盖率：核心业务逻辑 ≥ 80%，状态机覆盖 100%（consumed→available、exhausted→active 复活、expired→active 复活并更新 expire_at）
