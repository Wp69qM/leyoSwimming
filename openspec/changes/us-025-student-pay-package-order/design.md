# Design: US-025 学员支付套餐订单

> 本文档对应 `docs/stories/US-025-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-025 完成正价套餐订单的 Mock 支付闭环，核心是 Mock 支付流水创建 + Mock 渠道回调处理 + 订单/package 状态事务更新 + 身份异步重算。MVP 不调用真实微信/支付宝 SDK。

## Data Model

### 读写的表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `order` | 更新支付状态；保存模板快照；增加 type 字段与退款订单区分 | `id`, `type`, `user_id`, `amount`, `status`, `paid_at`, `expire_at`, `package_name`, `package_mode`, `coach_id`, `coach_name`, `teaching_type`, `stroke_ids`, `total_hours`, `duration_minutes`, `valid_days`, `original_price`, `paid_amount`, `refund_enabled`, `refund_ratio`, `refund_valid_days` |
| `payment` | 支付流水 | `id`, `order_id`, `idempotency_key`, `channel`, `channel_trade_no`, `amount`, `status` |
| `package` | 支付成功后创建，字段取自 order 快照 | `id`, `user_id`, `coach_id`, `order_id`, `package_mode`, `teaching_type`, `stroke_ids`, `total_hours`, `duration_minutes`, `valid_days`, `original_price`, `paid_amount`, `available_count`, `status` |
| `user` | 读取身份 | `id`, `status` |
| `agreement_sign` | 校验协议 | `user_id`, `agreement_type`, `version` |
| `package_template` | 下单时校验状态 | `id`, `status` |

### 索引

```sql
CREATE INDEX idx_order_user_status ON order(user_id, status);
CREATE INDEX idx_payment_order_idempotency ON payment(order_id, idempotency_key);
CREATE UNIQUE INDEX idx_payment_channel_trade ON payment(channel, channel_trade_no);
CREATE INDEX idx_package_order ON package(order_id);
```

## API Design

### POST /api/orders/{order_id}/pay

- 鉴权：必须登录且为订单所有者
- Request: `{ channel: 0|1 }`（0=微信 Mock / 1=支付宝 Mock）
- Response 200: `{ payment_id, channel_trade_no, status: "success" }`
- Response 400: `ORDER_EXPIRED | ORDER_NOT_PAYABLE | INVALID_CHANNEL`
- Response 404: `ORDER_NOT_FOUND`
- 实现说明：创建 payment 流水后调用 `MockPaymentProvider.pay(...)` 生成 `channel_trade_no` 并即时返回成功；由 MockProvider 异步触发回调

### POST /api/payments/mock/callback

- 鉴权：内部接口；开发/测试环境使用，生产环境禁用或替换为真实渠道回调
- Request: `{ channel: 0|1, order_id, channel_trade_no, amount, success: true }`
- Response 200: `{ code: "SUCCESS" }`
- 业务逻辑：校验 payment 存在且金额一致；同一 `channel_trade_no` 仅处理一次；事务内更新 payment/order/package；异步触发身份重算

### POST /api/orders/{order_id}

- 鉴权：必须登录且为订单所有者
- Request: `{ order_id }`（参数通过 JSON body 传递）
- Response 200: `{ order_id, type, status, amount, paid_at, package_id }`

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis 分布式锁 | `payment:lock:{channel_trade_no}` | 30s | 回调处理完立即释放 |
| Redis 订单状态 | `order:status:{order_id}` | 60s | order/payment 状态变更时删除 |
| Redis 库存锁 | `inventory:{coach_id}` | 30s | 库存释放或候补转正操作完成后立即释放 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 预支付参数获取 P99 | < 200ms |
| 支付回调处理 P99 | < 500ms |
| 订单状态查询 P99 | < 100ms |
| 并发支付回调 1000 QPS P99 | < 1s |

## Security

- Mock 回调接口需配置内部 token / IP 白名单，生产环境应禁用或替换为真实渠道回调
- 订单归属校验
- 回调金额与 order.amount 一致性校验
- 幂等键与 channel_trade_no 去重
- 订单超时取消与候补转正竞争同一库存锁 `inventory:{coach_id}`，防止并发超卖
- HTTPS 全链路加密
- 真实支付渠道接入时，需补充对应签名验证与证书管理

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-020 | 依赖 | 待支付订单 |
| US-021 | 被依赖 | 我的套餐 |
| US-026 | 被依赖 | 订单列表 |
| US-027 | 被依赖 | 退款 |
| US-029 | 被依赖 | 预约 |
