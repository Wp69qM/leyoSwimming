## Why

订单与退款是 leyoSwimming 的交易闭环。MVP 要求管理员能在后台查看全部订单、处理退款申请与争议退款，确保资金安全和用户体验。

## What Changes

- 新增管理员订单列表与详情查询能力，API 统一为 `/api/admin/v1/orders/*`
- 新增管理员批准退款、拒绝退款接口
- 新增管理员「标记争议」操作（管理员侧标记异常入口）：记录 `order.dispute_flag` 与 `dispute_reason`，自动生成关联客服工单（`support_ticket`，类型=退款申诉，见 US-049），**不改变订单状态**，并通知学员
- 新增「争议退款处理中」状态机支持（PRD §6.2.2 状态 5 + §6.10）：区分两个争议入口——管理员侧标记异常（仅生成工单，不改状态机）与用户侧特殊原因申诉（自动转入「争议退款处理中」状态，由本 US 提供管理员批准/拒绝处理 API）
- 管理员批准争议退款申诉：order.status → 已退款，package.status → refunded，释放课时 + 触发退款
- 管理员拒绝争议退款申诉：order.status 回退至已支付，package.status 恢复 active
- 退款批准后自动将关联 package.status 更新为 refunded，并生成 refund_record
- 新增 audit_log 记录管理员操作
- 前端 web-admin 新增「订单管理」页面

## Capabilities

### New Capabilities

- `admin-view-process-orders`: 管理员查看订单与处理退款

### Modified Capabilities

（无）

## Impact

- **数据表**：读取/修改 `order`、`package`；新增 `refund_record`、`audit_log`；标记争议时新增 `support_ticket`
- **API**：新增 5 个管理员接口（订单列表/详情/批准退款/拒绝退款/标记争议）
- **缓存**：新增 Redis 订单列表缓存
- **前端**：新增 web-admin「订单管理」页面
- **依赖**：依赖 US-025/US-027 产生的订单与退款数据
