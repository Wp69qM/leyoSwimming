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
| `user` | 改 | `identity` |
| `coach` | 读 | `status` |

### 索引

```sql
CREATE INDEX idx_package_user_type_status ON package(user_id, package_type, status);
```

## API Design

- `POST /api/orders/trial`：创建体验课订单
- `POST /api/orders/{id}/pay`：调起支付
- `POST /api/payments/callback`：支付回调

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

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-004 | 依赖 | 登录 |
| US-011 | 依赖 | 教练审核 |
| US-018 | 被依赖 | 体验课预约 |
