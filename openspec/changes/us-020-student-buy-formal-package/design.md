# Design: US-020 学员购买正价套餐

## Overview

正价套餐下单流程：选择套餐 → 协议确认 → 冲突/状态校验 → 创建待支付订单 → 跳转支付。

## Data Model

### 表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `order` | 写 | `user_id`, `coach_id`, `course_type=1`, `standard_package_id`, `custom_hours`, `amount`, `status`, `expire_at` |
| `agreement_sign` | 写 | `user_id`, `agreement_type`, `version`, `signed_at` |
| `user` | 读 | `identity` |
| `package` | 读 | `status`, `coach_id` |
| `coach` | 读 | `status`, `reference_price_per_hour` |
| `standard_package` | 读 | `hours`, `price` |

### 索引

```sql
CREATE INDEX idx_order_user_coach_status ON order(user_id, coach_id, status);
CREATE INDEX idx_package_user_status ON package(user_id, status);
CREATE INDEX idx_agreement_sign_user ON agreement_sign(user_id, agreement_type, version);
```

## API Design

- `POST /api/orders/formal`：创建正价套餐订单
- `GET /api/agreements/status`：查询协议签署状态

## Caching

（以写为主，无特殊缓存）

## Performance Targets

| 指标 | 目标 |
|------|------|
| 下单 P99 | < 300ms |

## Security

- 登录鉴权
- 同教练 active 套餐唯一性校验
- 协议版本后端校验
- 自定义课时范围校验

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-004/US-005 | 依赖 | 登录/注册 |
| US-011 | 依赖 | 教练审核 |
| US-019 | 依赖 | 套餐浏览 |
| US-045 | 依赖 | 套餐配置 |
| US-021 | 被依赖 | 我的套餐 |
| US-025 | 被依赖 | 支付 |
