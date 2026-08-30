# Test Plan: US-044 管理员管理教练排班与请假

## Task 1: Schedule Slot Service [P0]

**Files:**
- Create: `backend/src/services/schedule-slot.ts`
- Test: `backend/tests/services/schedule-slot.test.ts`

**Spec coverage:** 批量添加排班、修改排班、删除排班、时间冲突校验

- [ ] **RED:** Batch add valid slots; reject time conflict; update slot; delete slot; delete slot with booking requires confirmation
- [ ] **GREEN:** Implement CRUD + conflict detection
- [ ] **REFACTOR:** Extract conflict checker
- [ ] **COMMIT:** `feat(schedule): add schedule slot service`

## Task 2: Leave Request Approval Service [P0]

**Files:**
- Create: `backend/src/services/leave-approval.ts`
- Test: `backend/tests/services/leave-approval.test.ts`

**Spec coverage:** 通过请假（批量取消课程+释放课时+通知）、拒绝请假、重复审批幂等

- [ ] **RED:** Approve leave cancels overlapping bookings; releases reserved hours; sends notifications; reject keeps schedule; re-approve returns LEAVE_ALREADY_PROCESSED
- [ ] **GREEN:** Implement approval flow with transaction (leave_request + booking + package + notification)
- [ ] **REFACTOR:** Extract booking cancellation batch logic
- [ ] **COMMIT:** `feat(leave): add leave approval service`

## Task 3: Schedule & Leave Admin APIs [P0]

**Files:**
- Create: `backend/src/controllers/admin-schedule.ts`, `backend/src/controllers/admin-leave.ts`
- Create: `backend/src/routes/admin-schedule.ts`, `backend/src/routes/admin-leave.ts`
- Test: `backend/tests/controllers/admin-schedule.test.ts`, `backend/tests/controllers/admin-leave.test.ts`

**Spec coverage:** 7 个 API 端点（CRUD 排班 + 请假队列/通过/拒绝）

- [ ] **RED:** GET slots returns list; POST batch add returns 201; PUT update returns 200; DELETE returns 200; GET leave-requests returns queue; POST approve returns 200 with cancelled_bookings; POST reject returns 200; 403 without permission; 409 for conflicts
- [ ] **GREEN:** Implement endpoints with auth middleware
- [ ] **REFACTOR:** Share DTO schemas
- [ ] **COMMIT:** `feat(api): add admin schedule and leave endpoints`

## Task 4: Edge Cases & Idempotency [P1]

**Files:**
- Modify: `backend/src/services/leave-approval.ts`
- Test: `backend/tests/services/leave-approval-edge.test.ts`

**Spec coverage:** 请假时段与已上课重叠跳过、删除有预约排班二次确认、跨天批量添加、并发审批

- [ ] **RED:** Leave overlap with completed booking skips completed; batch add 30 days slots; concurrent approve uses optimistic lock
- [ ] **GREEN:** Implement skip-logic + optimistic lock
- [ ] **REFACTOR:** —
- [ ] **COMMIT:** `feat(leave): handle edge cases and idempotency`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
- 每 Task 完成后跑 `openspec validate us-044-admin-manage-coach-schedule-leave --json` 确认无回归
