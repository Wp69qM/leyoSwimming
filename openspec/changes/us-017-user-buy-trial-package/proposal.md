## Why

体验课是游客转化为学员的关键入口。通过低价单节课降低决策门槛，让用户在购买正价套餐前先体验教练教学。

## What Changes

- 新增体验课下单接口 `POST /api/orders/trial`
- 新增支付调起与回调处理
- 新增体验套餐创建逻辑（1 节/30 天，同用户唯一）
- 新增订单 24h 超时取消任务
- 支付成功后触发用户身份升级为学员

## Capabilities

### New Capabilities

- `user-buy-trial-package`: 游客/注册用户购买体验课套餐

### Modified Capabilities

（无）

## Impact

- **数据表**：新增 `order`、`package`、`payment` 记录
- **API**：新增 3 个端点
- **任务**：新增超时取消任务
- **依赖**：依赖 US-004 / US-011；被 US-018 依赖
