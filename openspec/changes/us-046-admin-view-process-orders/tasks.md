# Tasks: US-046 管理员查看与处理订单

> 本文档对应 `docs/stories/US-046-.../test-plan.md` 的 OpenSpec 映射版本。

## Task 1: AdminOrder Repository [P0]

**Spec coverage:** REQ-001 Scenarios "管理员查看订单列表与详情", "查看不存在的订单"

- [ ] **RED:** Write failing tests for list/detail
- [ ] **GREEN:** Implement repository
- [ ] **REFACTOR:** Extract filter builder
- [ ] **COMMIT:** `feat(admin-order): add repository`

## Task 2: Refund Service [P0]

**Spec coverage:** REQ-002 Scenarios "管理员批准退款", "管理员拒绝退款", "对非退款审批中订单执行退款", "退款金额超过已支付金额"

- [ ] **RED:** Write failing tests for approve/reject/validation
- [ ] **GREEN:** Implement refund service
- [ ] **REFACTOR:** Reuse transaction wrapper
- [ ] **COMMIT:** `feat(refund): add admin refund service`

## Task 3: GET /api/admin/v1/orders API [P0]

**Spec coverage:** REQ-001 Scenario "管理员查看订单列表与详情"

- [ ] **RED / GREEN / REFACTOR / COMMIT**

## Task 4: GET /api/admin/v1/orders/:id API [P0]

**Spec coverage:** REQ-001 Scenarios "管理员查看订单列表与详情", "查看不存在的订单"

- [ ] **RED / GREEN / REFACTOR / COMMIT**

## Task 5: POST /api/admin/v1/orders/:id/approve-refund API [P0]

**Spec coverage:** REQ-003 Scenarios "管理员批准退款受理成功", "对非退款审批中订单执行退款", "退款金额超过已支付金额"

- [ ] **RED / GREEN / REFACTOR / COMMIT**

## Task 6: POST /api/admin/v1/orders/:id/reject-refund API [P0]

**Spec coverage:** REQ-003 Scenario "管理员拒绝退款"

- [ ] **RED / GREEN / REFACTOR / COMMIT**

## Task 7: POST /api/admin/v1/orders/:id/mark-dispute API [P0]

**Spec coverage:** REQ-004 Scenario "管理员标记订单为争议退款并生成客服工单（不改变订单状态）"

- [ ] **RED:** Write failing tests for setting `dispute_flag` / `dispute_reason` and creating `support_ticket`；断言 `order.status` 不变（区别于用户侧申诉）
- [ ] **GREEN:** Implement mark-dispute endpoint
- [ ] **REFACTOR:** Extract ticket creation helper
- [ ] **COMMIT:** `feat(admin-order): add mark-dispute endpoint`

> 与 US-049 边界：本 Task 负责生成 `support_ticket`；US-049 负责工单的后续分配、回复与关闭。
> 区分说明：本 Task 仅处理「管理员侧标记异常」，不触发状态机转换；用户侧「争议退款处理中」状态机转换由 Task 9 处理。

## Task 8: 权限中间件 [P0]

**Spec coverage:** REQ-005 Scenario "无权限管理员访问订单接口"

- [ ] **RED / GREEN / REFACTOR / COMMIT**

## Task 9: 争议退款处理中状态机 + 管理员批准/拒绝申诉 API [P0]

**Spec coverage:** REQ-006 Scenarios "用户提交特殊原因申诉进入争议退款处理中", "管理员批准争议退款申诉", "管理员拒绝争议退款申诉"

- [ ] **RED:** Write failing tests:
  - 用户提交特殊原因申诉 → order.status 转「争议退款处理中」+ package 冻结约课（PRD §6.10.1）
  - 管理员批准申诉 → order.status 转「已退款」+ package.status → refunded + 释放课时 + 触发退款（PRD §6.10.3）
  - 管理员拒绝申诉并填写原因 → order.status 回退「已支付」+ package.status 恢复 active（PRD §6.10.3）
- [ ] **GREEN:** Implement state transitions + approve-dispute / reject-dispute endpoints
- [ ] **REFACTOR:** Reuse refund transaction wrapper（与 Task 5 退款流程对齐）
- [ ] **COMMIT:** `feat(admin-order): add dispute-resolution state machine`

> 与 Task 7 区分：Task 7 仅生成工单不改状态机；Task 9 处理「争议退款处理中」状态机的所有转换（已支付 ↔ 争议退款处理中 → 已退款）。

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
