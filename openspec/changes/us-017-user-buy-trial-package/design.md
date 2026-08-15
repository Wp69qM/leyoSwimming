# Design: US-017 游客购买体验课套餐

## Overview

体验课购买流程：下单 → 支付 → 回调 → 套餐激活 → 身份升级。

## Data Model

### 表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `order` | 写 | `user_id`, `coach_id`, `course_type=0`, `status`, `amount`, `expire_at` |
| `package` | 写 | `user_id`, `coach_id`, `package_type=0`, `total_hours=1`, `available=1`, `status`, `expire_at` |
| `payment` | 写 | `order_id`, `provider`, `out_trade_no`, `status` |
| `agreement_sign` | 写 | `user_id`, `agreement_type`, `version`, `signed_at` |
| `user` | 改 | `identity` |
| `coach` | 读 | `status` |

### 索引

```sql
CREATE INDEX idx_package_user_type_status ON package(user_id, package_type, status);
```

## API Design

### POST /api/order/trial

- **鉴权**：需登录
- **Request Body**: `{ coachId }`
- **Response 201**: 订单信息（不含 package，套餐在支付回调成功时创建）
- **Response 400**: `TRIAL_PACKAGE_EXISTS` / `COACH_UNAVAILABLE`

### POST /api/order/pay

- **鉴权**：需登录
- **Request Body**: `{ orderId, channel }`
- **Response 200**: 调起支付参数

### POST /api/payment/mock-callback

- **鉴权**：Mock 支付平台签名
- **Request Body**: `{ orderId, channelTradeNo, amount, success }`
- **Response 200**: 成功

### POST /api/agreement/status

- **鉴权**：需登录
- **Request Body**: `{}`
- **Response 200**: 用户各协议签署状态

## Caching

（以写为主，无特殊缓存）

## Performance Targets

| 指标 | 目标 |
|------|------|
| 下单 P99 | < 300ms |
| 回调 P99 | < 200ms |

## Security

- 回调签名验证
- 体验套餐唯一性校验
- 教练状态校验
- 协议签署版本校验

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-004 | 依赖 | 登录 |
| US-011 | 依赖 | 教练审核 |
| US-018 | 被依赖 | 体验课预约 |
