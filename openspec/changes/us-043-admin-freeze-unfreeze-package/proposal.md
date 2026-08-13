## Why

管理员需要集中查看用户购买后的 package 实例，并在投诉处理、异常订单、学员协商退款等场景下进行干预，包括冻结、解冻、延期、发起退款等操作。缺少该能力会导致平台无法及时管控风险订单，也无法在资产侧发起退款流程。

## What Changes

- 管理后台「套餐订单 → 套餐管理」新增用户已购买 package 实例列表与详情
- 列表支持按套餐状态、套餐模式、到期时间、关键词筛选与分页
- 套餐详情展示购买时快照、购买时间、到期时间、课时消耗及消耗使用记录、冻结原因、关联订单、上课记录、操作日志
- 管理员可对 active package 执行「冻结」，选择原因（投诉处理中 / 异常订单 / 司法冻结），冻结时释放 reserved 课时并取消未上课 booking
- 管理员可对 frozen package 执行「解冻」，恢复 active 并清除 frozen_reason
- 管理员可对 active 或 expired 且剩余课时 > 0 的 package 执行「延期」
- 管理员可对 active package 执行「退款」，填写原因后生成退款订单（order.type='refund'），package 标记为 frozen/refund_pending，后续在订单管理页审批
- 所有操作记录审计日志

## Capabilities

### New Capabilities

- `admin-manage-user-packages`: 管理员查看与管理用户套餐实例（列表/详情/冻结/解冻/延期/发起退款）

### Modified Capabilities

- 无

## Impact

- 后端：新增 `POST /api/admin/package/list|detail|freeze|unfreeze|extend|refund` 接口，package 状态转换服务，booking 取消与审计日志
- 管理端 Web：套餐管理列表页、详情页、冻结/解冻/延期/退款确认弹窗
- 依赖：US-020（产生 package）、US-021（学员端展示）、US-045（快照字段）、US-053（管理员登录）
- 输出：退款订单进入 US-046 审批流程；frozen package 可进入 US-027/US-028 退款流程
