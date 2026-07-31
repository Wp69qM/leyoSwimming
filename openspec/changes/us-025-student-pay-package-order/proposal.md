## Why

US-020 完成了正价套餐下单，但订单仍处于待支付状态。必须实现微信/支付宝支付闭环，将待支付订单转化为有效课时包，使用户获得学员身份，才能继续约课等后续流程。支付是核心营收路径，必须具备幂等、对账、超时处理等能力。

## What Changes

- 新增 `POST /api/orders/{order_id}/pay`：创建支付流水并返回微信/支付宝预支付参数
- 新增 `POST /api/payments/callback/wechat` 与 `POST /api/payments/callback/alipay`：处理渠道异步回调
- 新增 `GET /api/orders/{order_id}`：查询订单支付状态
- 新增 `payment` 表记录支付流水与幂等键
- 支付成功事务内更新 order.status → 已支付，创建 package.status → active
- 异步触发身份重算：注册用户 → 学员
- 新增定时任务关闭 24h 未支付订单
- 新增 Redis 分布式锁与订单状态缓存
- 新增库存释放与候补转正共享分布式锁，防止并发竞争

## Capabilities

### New Capabilities

- `student-pay-package-order`: 学员使用微信/支付宝支付正价套餐订单，含预支付、回调、幂等、超时关闭

### Modified Capabilities

- `student-buy-formal-package` (US-020): 订单创建后由本 US 完成支付与 package 激活

## Impact

- **数据表**：新增/修改 `order` / `payment` / `package`；读取 `user` / `agreement_sign`
- **API**：新增 4 个端点
- **缓存**：新增 Redis 分布式锁与订单状态缓存
- **定时任务**：新增订单过期关闭任务
- **前端**：新增小程序支付页
- **依赖**：依赖 US-020；被 US-021 / US-026 / US-027 / US-029 依赖
