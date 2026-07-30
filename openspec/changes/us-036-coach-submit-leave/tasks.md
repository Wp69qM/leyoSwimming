# Tasks: US-036 教练提交请假申请

> 本文档对应 `docs/stories/US-036-.../test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → REFACTOR → COMMIT 循环。

## Task 1: CoachLeave Repository — 创建请假记录 + 时间冲突校验 [P0]

**Files:**
- Create: `backend/src/repositories/coachLeave.ts`
- Test: `backend/tests/repositories/coachLeave.test.ts`

**Spec coverage:** REQ-036-1 Scenarios "正常提交请假申请", "请假时间与已批准请假冲突"

- [ ] **RED:** `createLeave` returns leave_id with status=0; throws `LEAVE_TIME_CONFLICT` when overlapping an approved leave
- [ ] **GREEN:** Implement `CoachLeaveRepository.create` with overlap check against `status=1` records
- [ ] **REFACTOR:** Extract overlap helper and time-range validator
- [ ] **COMMIT:** `feat(coach-leave): add create leave request with overlap validation`

## Task 2: POST /coach/leaves API — 提交 + 参数校验 [P0]

**Files:**
- Create: `backend/src/controllers/coachLeave.ts`
- Create: `backend/src/routes/coachLeave.ts`
- Test: `backend/tests/controllers/coachLeave.test.ts`

**Spec coverage:** REQ-036-1 Scenarios "正常提交请假申请", "请假开始时间已过", "结束时间早于开始时间", "教练状态非已通过"

- [ ] **RED:** 201 on valid submission; 400 on past start time; 400 on invalid time range; 403 on non-approved coach; 409 on conflict
- [ ] **GREEN:** Implement controller + route with validation pipeline
- [ ] **REFACTOR:** Standardize time range validation and error response format
- [ ] **COMMIT:** `feat(api): add POST /coach/leaves endpoint`

## Task 3: GET /coach/leaves API — 请假列表查询 [P1]

**Files:**
- Modify: `backend/src/controllers/coachLeave.ts`
- Modify: `backend/src/routes/coachLeave.ts`
- Modify: `backend/tests/controllers/coachLeave.test.ts`

**Spec coverage:** REQ-036-2 Scenarios "正常查询请假列表", "越权查询其他教练请假列表"

- [ ] **RED:** 200 with paginated list; rejects cross-coach access
- [ ] **GREEN:** Implement list method with coach-scoped query
- [ ] **REFACTOR:** Extract pagination helper
- [ ] **COMMIT:** `feat(api): add GET /coach/leaves endpoint`

## Task 4: 幂等性与边界场景测试 [P1]

**Files:**
- Modify: `backend/tests/controllers/coachLeave.test.ts`

**Spec coverage:** REQ-036-1 Scenario "重复提交幂等"

- [ ] **RED:** Duplicate submission within 3s returns existing leave; >30d leave allowed but flagged
- [ ] **GREEN:** Implement idempotency key handling and long-leave flag
- [ ] **REFACTOR:** Share idempotency test utilities
- [ ] **COMMIT:** `test(coach-leave): add idempotency and boundary tests`

---

## Execution Discipline

- 严格顺序：Task 1 → Task 2 → Task 3 → Task 4
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做，P1 选做
