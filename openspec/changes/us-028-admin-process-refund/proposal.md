## Why

US-027 完成了学员退款申请，但退款仍处于「审批中」状态。必须由管理员在后台审批并触发原路退回，才能完成退款闭环。该流程涉及资金安全、订单/套餐状态转换、赠送课时作废以及用户身份重算，是支付链路的关键组成部分。

## What Changes

- 新增管理端退款列表与详情查询接口
- 新增 `POST /api/admin/refunds/{refund_id}/approve`：批准退款并调用微信/支付宝退款接口
- 新增 `POST /api/admin/refunds/{refund_id}/reject`：驳回退款并恢复订单/套餐状态
- 新增 `refund_transaction` 表记录渠道退款流水
- 管理员批准后：order.status → 已退款，package.status → refunded，关联赠送 package 同步作废
- 管理员驳回后：order.status → 已支付，package.status 保持 active，package.booking_frozen = false，已释放课时不再自动恢复
- 退款审批期间 package.status 保持 active，约课能力通过 package.booking_frozen 控制
- 退款完成后异步触发用户身份重算
- 渠道退款失败时创建失败流水，package.status 保持 active、booking_frozen 保持 true，进入重试队列

## Capabilities

### New Capabilities

- `admin-process-refund`: 管理员审批退款并触发原路退回，含批准、驳回、失败重试

### Modified Capabilities

- `student-apply-refund` (US-027): 退款申请提交后由本 US 完成审批与资金退回

## Impact

- **数据表**：新增 `refund_transaction`；修改 `order` / `refund` / `package`（status 保持 active，booking_frozen 按阶段调整）
- **API**：新增 4 个管理端端点
- **定时任务**：新增退款失败重试任务
- **前端**：新增 web-admin 退款处理页面
- **依赖**：依赖 US-027；被 US-046 依赖
