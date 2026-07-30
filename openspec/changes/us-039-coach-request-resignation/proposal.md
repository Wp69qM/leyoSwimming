## Why

教练需要一条合规的主动离职通道，将离职意向转化为系统可追踪的工单，并为后续管理员审批、学员套餐转移或退款提供数据入口。缺少该能力会导致教练离职流程游离于系统之外，学员课时与教练结算难以闭环。

## What Changes

- 教练端「我的」新增「申请离职」入口，仅 `coach.status = 1` 可见
- 教练提交离职申请后，`coach.status` 从 `1` 变为 `4`（申请中）
- 系统自动生成「离职工单」，列出该教练名下所有 active 套餐及学员
- 教练可在工单中对每份 active 套餐登记处理结果：转新教练 / 全额退款 / 继续上完
- 教练处理完毕后将工单提交至管理员审批队列
- 教练在管理员审批前可撤销申请，恢复 `coach.status = 1`

## Capabilities

### New Capabilities

- `coach-request-resignation`: 教练主动申请离职、登记学员套餐处理结果并提交管理员审批

### Modified Capabilities

- 无

## Impact

- 后端：新增 `coach_resignation_ticket`、`coach_resignation_action` 表，新增 `/api/coach/v1/resignation/*` 系列接口、幂等键去重与审计日志
- 教练端小程序：新增「申请离职」入口、离职工单处理页
- 管理端：US-041 可读取本 US 生成的 pending_audit 工单
- 依赖：US-012（教练主页管理）、US-020（学员购买正价套餐）
