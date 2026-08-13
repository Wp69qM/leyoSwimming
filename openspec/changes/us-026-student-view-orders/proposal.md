## Why

支付完成后，用户需要查看自己的订单记录与支付状态，确认交易成功，并在需要时发起退款。订单列表与详情是支付闭环后的基础查询能力。

## What Changes

- 新增 `GET /api/orders`：当前用户订单列表（分页 + 状态筛选），列表卡片展示套餐模式标签（正价/体验课）
- 新增 `GET /api/orders/{order_id}`：订单详情（含 package / coach / payment / refund），套餐信息区展示购买时快照字段，不依赖 package_template 当前状态
- 新增 Redis 缓存层：列表 60s / 详情 300s
- 新增小程序「我的订单」列表页与订单详情页
- 订单详情展示退款/驳回原因（US-027/028）

## Capabilities

### New Capabilities

- `student-view-orders`: 学员查看自己的订单列表与详情

### Modified Capabilities

- 无

## Impact

- **数据表**：读取 `order`（列表与详情中的套餐信息均取自 order 快照字段） / `package`（仅用于展示当前课时包状态） / `coach`（兜底读取） / `payment` / `refund`；不读取 `package_template`
- **API**：新增 2 个端点
- **缓存**：新增 Redis 订单列表与详情缓存
- **前端**：新增小程序 2 个页面
- **依赖**：依赖 US-020 / US-025；被 US-027 / US-046 依赖
