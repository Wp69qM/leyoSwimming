## Why

在投诉处理、异常订单等场景下，管理员需要临时冻结学员套餐以阻止继续约课，避免问题扩大；问题解决后再解冻恢复使用。缺少该能力会导致平台无法及时干预风险订单。

## What Changes

- 管理后台「用户管理 → 套餐管理」新增「冻结」与「解冻」操作
- 管理员冻结 active package 时需选择原因：投诉处理中 / 异常订单 / 司法冻结（P3）
- 冻结时 `package.status` 从 active 变为 frozen，写入 `frozen_reason`，并释放 `reserved` 课时
- 冻结时自动取消该 package 下已预约但未上课的 booking
- 管理员解冻 frozen package 时，`package.status` 恢复 active，`frozen_reason` 清除
- 所有操作记录审计日志

## Capabilities

### New Capabilities

- `admin-freeze-unfreeze-package`: 管理员手动冻结与解冻学员套餐

### Modified Capabilities

- 无

## Impact

- 后端：新增 `/api/admin/v1/packages/{id}/freeze` 与 `/api/admin/v1/packages/{id}/unfreeze` 接口、package 状态转换服务、booking 取消与审计日志
- 管理端 Web：套餐管理列表页增加冻结/解冻操作列与确认弹窗
- 依赖：US-020（学员购买正价套餐）、US-021（学员查看我的套餐）
- 输出：frozen package 可进入 US-027/US-028 退款流程
