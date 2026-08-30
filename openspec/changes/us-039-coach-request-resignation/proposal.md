## Why

教练需要一条合规的主动离职通道，将离职意向转化为系统可追踪的工单，并为后续管理员审批、学员套餐转移或退款提供数据入口。缺少该能力会导致教练离职流程游离于系统之外，学员课时与教练结算难以闭环。

## What Changes

- 教练端「我的」新增「申请离职」入口，仅 `coach.status = 1` 可见
- 教练提交离职申请后，`coach.status` 从 `1` 变为 `4`（申请中）
- 系统自动生成「离职工单」，列出该教练名下所有 active 套餐、学员及单价
- 教练在工单中对每份 active 套餐确认「全额退款」处理结果并落库
- 确认「全额退款」时，系统自动生成 100% 退款记录：`refund_amount = 单价 × 剩余课时`（已消耗课时不退）
- 工单提交至管理员时，未确认退款的 active 套餐默认按「全额退款」生成待退款记录
- MVP 不支持教练自行撤销离职申请（PRD §5.4.7 未定义 `4 → 1` 转换）；如需撤销由 US-041 管理员驳回触发

## Capabilities

### New Capabilities

- `coach-request-resignation`: 教练主动申请离职、确认学员套餐全额退款并提交管理员审批

### Modified Capabilities

- 无

## Impact

- 后端：新增 `coach_resignation_ticket`、`coach_resignation_action`、`refund_record` 表，新增 `/api/coach/resignation/*` 系列接口、幂等键去重与审计日志
- 教练端小程序：新增「申请离职」入口、离职工单处理页
- 管理端：US-041 可读取本 US 生成的 pending_audit 工单与待退款记录
- 依赖：US-012（教练主页管理）、US-020（学员购买正价套餐）
