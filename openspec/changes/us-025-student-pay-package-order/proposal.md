## Why

US-020 完成了正价套餐下单，但订单仍处于待支付状态。MVP 阶段使用 Mock 支付方式完成微信/支付宝支付闭环，将待支付订单转化为有效课时包，使用户获得学员身份，才能继续约课等后续流程。Mock 支付需具备幂等、对账、超时处理等能力，并与 US-046 管理员订单管理连通。

## What Changes

- 新增 `POST /api/order/pay`：创建 Mock 支付流水并返回 `paymentId` + `channelTradeNo`
- 新增 `POST /api/payment/mock-callback`：Mock 渠道异步回调（开发/测试环境），幂等更新 order/package
- 新增 `POST /api/order/detail`：查询订单支付状态
- 新增 `payment` 表记录支付流水与幂等键
- `order` 表增加 `type = purchase`，与 US-046 退款订单区分，管理员可在订单管理中查看 mock-paid 订单
- 支付成功事务内更新 order.status → 已支付；使用订单中已快照的模板字段创建 package.status → active，不实时查询 package_template
- 异步触发身份重算：注册用户 → 学员
- 新增定时任务关闭 24h 未支付订单
- 新增 Redis 分布式锁与订单状态缓存
- 新增库存释放与候补转正共享分布式锁，防止并发竞争

## Capabilities

### New Capabilities

- `student-pay-package-order`: 学员使用 Mock 微信/支付宝支付正价套餐订单，含 Mock 支付、回调、幂等、超时关闭

### Modified Capabilities

- `student-buy-formal-package` (US-020): 订单创建后由本 US 完成支付与 package 激活

## Impact

- **数据表**：新增/修改 `order`（增加 `type = purchase`，回填 paid_at，保存模板快照字段） / `payment` / `package`（字段取自 order 快照）；读取 `user` / `agreement_sign` / `package_template`（下单时校验 active，支付回调阶段不依赖）
- **API**：新增 3 个端点（1 个 Mock 支付 + 1 个 Mock 回调 + 1 个订单查询）
- **缓存**：新增 Redis 分布式锁与订单状态缓存
- **定时任务**：新增订单过期关闭任务
- **前端**：新增小程序支付页（调用 Mock 支付 API，不调起真实 SDK）
- **依赖**：依赖 US-020 / US-004 / US-005；被 US-021 / US-026 / US-027 / US-029 / US-046 依赖
