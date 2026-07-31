## Why

教练提交离职申请后，需要管理员按统一 checklist 审批，确保学员套餐、未来预约、教练排班等得到妥善处理。审批通过意味着教练正式离职并触发大量数据变更，拒绝则让教练恢复教学，因此审批节点是离职闭环的关键控制点。

## What Changes

- 管理后台新增「用户管理 → 教练离职审批队列」
- 管理员可查看所有 `pending_audit` 离职工单及其详情
- 管理员审批前必须完成 checklist 校验：① active 学员数 = 0；② 若 active 学员数 > 0 则所有 active 套餐已确认全额退款；③ 教练费已结算；④ 未来排班已清空
- 审批通过时，在同一个事务中：
  - 对所有 active package（含未确认退款的套餐），自动生成 100% 待退款记录：`refund_amount = 单价 × 剩余课时`（已消耗不退），并通知学员选择退款或换教练
  - `coach.status: 4 → 3`
  - 取消该教练所有未来 booking
  - `package.reserved → available`
  - 所有 active package → frozen（frozen_reason = coach_resigned）
  - 未来 schedule_slot → hidden
- 审批拒绝时，`coach.status: 4 → 1`，工单 rejected，已确认退款记录不回滚

## Capabilities

### New Capabilities

- `admin-handle-coach-resignation`: 管理员审批教练离职申请并通过/拒绝触发后续状态变更

### Modified Capabilities

- 无

## Impact

- 后端：新增 `/api/admin/v1/resignation-tickets/*` 系列接口、checklist 校验服务、批量更新事务与审计日志
- 管理端 Web：新增离职审批队列页、工单详情页、审批确认弹窗
- 依赖：US-039（教练申请离职）、US-014（教练管理可约时段）
- 输出：US-040（教练重新入驻）、US-027/US-028（frozen package 退款）
