## Why

学员在购买教练课时包后，可能因教练离职、个人时间冲突或平台故障等合理场景需要主动发起退款。US-025/US-026 完成了订单支付与查看，但缺少退款申请入口，导致学员资金权益无法保障，US-028（管理员处理退款）也缺乏上游退款申请数据来源。本变更补齐学员侧退款申请能力，启动退款审批流程并冻结相关课时包，是退款闭环（申请 → 审批 → 退回）的入口环节。

## What Changes

- 新增 `GET /api/orders/{order_id}/refund/check`：查询学员退款资格与可退金额计算过程
- 新增 `POST /api/orders/{order_id}/refund`：学员提交退款申请
- 新增 `refund_record` 表记录退款申请（金额、原因类型、说明、状态）
- 提交退款时事务内执行：创建 refund_record(status=待审批) + order.status → 退款审批中 + package.status → frozen
- 释放 package 的 reserved 课时，禁止审批期间预约
- 退款金额按 PRD §6.4.2 公式计算：`paid_amount × (total_hours - consumed_count) / total_hours`
- 提交后立即通知管理员（US-028 入口）与学员
- 拒绝重复申请：已存在待审批退款记录时返回 REFUND_IN_PROGRESS
- 体验套餐退款金额为 0 时前端置灰，过期套餐有剩余课时允许申请

## Capabilities

### New Capabilities

- `student-request-refund`: 学员在订单详情发起退款申请，含资格检查、金额计算、状态冻结与通知

### Modified Capabilities

- 无（本 US 为退款闭环入口，被 US-028 依赖）

## Impact

- **数据表**：新增 `refund_record`；修改 `order`（status → 退款审批中）、`package`（status → frozen）、`notification`（新增通知记录）
- **API**：新增 2 个学员侧端点（资格检查 + 退款提交）
- **状态机**：order 已支付 → 退款审批中；package active/exhausted/expired → frozen
- **缓存**：新增 `refund:check:{order_id}` TTL 30s
- **性能**：退款申请 P99 < 300ms，资格检查 P99 < 200ms
- **依赖**：依赖 US-004/005（登录）、US-025/026（已支付订单）；被 US-028（管理员审批退款）依赖
