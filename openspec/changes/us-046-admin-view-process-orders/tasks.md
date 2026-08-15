# Tasks: US-046 管理员查看与处理订单

> 本文档对应 `docs/stories/US-046-.../test-plan.md` 的 OpenSpec 映射版本。

## Task 1: AdminOrder Repository [P0]

**Spec coverage:** REQ-001 / REQ-002

- [ ] **RED:** Write failing tests for list/detail
- [ ] **GREEN:** Implement repository
- [ ] **REFACTOR:** Extract filter builder
- [ ] **COMMIT:** `feat(admin-order): add repository`

## Task 2: Refund Service [P0]

**Spec coverage:** REQ-003

- [ ] **RED:** Write failing tests for approve/reject/validation
- [ ] **GREEN:** Implement refund service
- [ ] **REFACTOR:** Reuse transaction wrapper
- [ ] **COMMIT:** `feat(refund): add admin refund service`

## Task 3: POST /api/admin/order/list API [P0]

**Spec coverage:** REQ-001 Scenario "管理员查看订单列表"

- [ ] **RED / GREEN / REFACTOR / COMMIT**

## Task 4: POST /api/admin/order/detail API [P0]

**Spec coverage:** REQ-002 Scenarios "管理员查看订单详情", "查看不存在的订单"

- [ ] **RED / GREEN / REFACTOR / COMMIT**

## Task 5: POST /api/admin/order/approve-refund API [P0]

**Spec coverage:** REQ-003 Scenarios "管理员批准退款受理成功", "对非退款审批中订单执行退款", "退款金额超过已支付金额"

- [ ] **RED / GREEN / REFACTOR / COMMIT**

## Task 6: POST /api/admin/order/reject-refund API [P0]

**Spec coverage:** REQ-003 Scenario "管理员拒绝退款"

- [ ] **RED / GREEN / REFACTOR / COMMIT**

## Task 7: 权限中间件 [P0]

**Spec coverage:** REQ-004 Scenario "无权限管理员访问订单接口"

- [ ] **RED / GREEN / REFACTOR / COMMIT**

## Task 8: Verification

- [ ] 8.1 Run integration tests for list/detail/approve/reject/permission scenarios
- [ ] 8.2 Verify error codes `ORDER_STATUS_INVALID`, `REFUND_AMOUNT_MISMATCH`, `ORDER_NOT_FOUND`, `FORBIDDEN`
- [ ] 8.3 Run `openspec validate us-046-admin-view-process-orders --json` and fix issues

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
