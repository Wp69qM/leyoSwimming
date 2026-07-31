# Design: US-022 学员更换绑定教练

## Overview

学员换教练流程：选择新教练 → 第一步冻结旧套餐并生成 pending_change 退款记录 → 第二步校验有效期后正式退款旧套餐并创建新订单 → 身份回退/重算 → 跳转支付。

## Data Model

### 表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `package` | 修改 | `status`, `original_status`, `frozen_reason`, `reserved_count`, `available_count`, `refunded_at` |
| `order` | 写 | `user_id`, `coach_id`, `course_type=1`, `amount`, `status` |
| `refund_record` | 写 | `package_id`, `user_id`, `amount`, `reason`, `status`, `original_package_status` |
| `user` | 修改 | `identity` |
| `coach` | 读 | `status`, `reference_price_per_hour` |
| `booking` | 读 | `status` |

> **frozen_reason 枚举约束**：换教练第一步冻结时，`frozen_reason = refund_pending`，复用 PRD §5.5.1.2 统一枚举值，禁止新增 `coach_change_pending` 等自定义枚举。

### 索引

```sql
CREATE INDEX idx_package_user_status ON package(user_id, status);
CREATE INDEX idx_booking_user_status ON booking(user_id, status);
CREATE INDEX idx_coach_status ON coach(status);
CREATE INDEX idx_refund_record_user_status ON refund_record(user_id, status);
```

## API Design

- `GET /api/coaches/available-for-change`：可更换教练列表
- `POST /api/users/me/coach/unsubscribe`：第一步，冻结旧套餐、创建 pending_change 退款记录、身份回退
- `POST /api/users/me/coach/purchase`：第二步，校验有效期与 pending_change 退款记录，正式退款旧套餐并创建新教练待支付订单

## Caching

（无）

## Performance Targets

| 指标 | 目标 |
|------|------|
| 退订接口 P99 | < 500ms |
| 购买接口 P99 | < 500ms |

## Security

- 登录鉴权
- 仅操作当前用户 package
- 第一步事务内完成旧套餐冻结 + pending_change 退款记录创建 + 身份回退
- 第二步事务内完成旧套餐正式退款 + 新订单创建
- 两步操作 24 小时有效期：超时未购买新套餐自动回滚第一步（旧套餐恢复 original_status、取消 pending_change 退款记录、身份按实际套餐状态重算），由定时任务 + 状态版本号保证

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-004/US-005 | 依赖 | 登录 |
| US-020 | 依赖 | 订单创建 |
| US-021 | 依赖 | 我的套餐入口 |
| US-027/US-028 | 依赖 | 退款记录与状态机复用 |
| US-030 | 依赖 | 取消预约 |
| US-023/US-029/US-050 | 被依赖 | 后续流程 |
