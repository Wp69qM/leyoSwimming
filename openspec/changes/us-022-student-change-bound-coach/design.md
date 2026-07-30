# Design: US-022 学员更换绑定教练

## Overview

学员换教练流程：选择新教练 → 校验 → 旧套餐退款 → 创建新订单 → 身份回退 → 跳转支付。

## Data Model

### 表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `package` | 修改 | `status`, `reserved_count`, `available_count`, `refunded_at` |
| `order` | 写 | `user_id`, `coach_id`, `course_type=1`, `amount`, `status` |
| `refund_record` | 写 | `package_id`, `user_id`, `amount`, `reason`, `status` |
| `user` | 修改 | `identity` |
| `coach` | 读 | `status`, `reference_price_per_hour` |
| `booking` | 读 | `status` |

### 索引

```sql
CREATE INDEX idx_package_user_status ON package(user_id, status);
CREATE INDEX idx_booking_user_status ON booking(user_id, status);
CREATE INDEX idx_coach_status ON coach(status);
```

## API Design

- `GET /api/coaches/available-for-change`：可更换教练列表
- `POST /api/users/me/coach/change`：提交更换教练

## Caching

（无）

## Performance Targets

| 指标 | 目标 |
|------|------|
| 更换接口 P99 | < 500ms |

## Security

- 登录鉴权
- 仅操作当前用户 package
- 事务内完成退款 + 新订单 + 身份更新

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-004/US-005 | 依赖 | 登录 |
| US-020 | 依赖 | 订单创建 |
| US-021 | 依赖 | 我的套餐入口 |
| US-030 | 依赖 | 取消预约 |
| US-023/US-029/US-050 | 被依赖 | 后续流程 |
