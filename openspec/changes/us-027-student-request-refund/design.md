# Design: US-027 学员申请退款

> 本文档对应 `docs/stories/US-027-学员-申请退款/tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-027 是退款闭环的入口。核心流程：学员在订单详情发起退款 → 资格检查（金额计算） → 提交申请 → 事务内创建 refund_record + 冻结 order/package → 通知管理员（US-028 入口）与学员。

## Data Model

### 读写的表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `order` | 读/写 | `id`, `user_id`, `status`, `paid_amount` |
| `package` | 读/写 | `id`, `order_id`, `status`, `total_hours`, `consumed_count`, `reserved_count`, `available_count` |
| `refund_record` | 写 | `id`, `order_id`, `user_id`, `refund_amount`, `reason_type`, `reason_detail`, `status` |
| `notification` | 写 | `id`, `user_id`, `type`, `payload`, `created_at` |

### refund_record 字段定义

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 主键 |
| `order_id` | BIGINT | FK → order.id, NOT NULL, IDX | 关联订单 |
| `user_id` | BIGINT | FK → user.id, NOT NULL, IDX | 学员 |
| `refund_amount` | INT | NOT NULL | 退款金额（分） |
| `reason_type` | TINYINT | NOT NULL | 1=教练原因 2=个人原因 3=平台原因 |
| `reason_detail` | VARCHAR(500) | NULL | 退款说明 |
| `status` | TINYINT | NOT NULL DEFAULT 0 | 0=待审批 1=已批准 2=已驳回 3=已退款 |
| `created_at` | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

### 索引

```sql
CREATE INDEX idx_refund_order ON refund_record(order_id, status);
CREATE INDEX idx_refund_user_status ON refund_record(user_id, status);
CREATE UNIQUE INDEX uk_refund_idempotent ON refund_record(order_id, status) WHERE status = 0;
```

> `uk_refund_idempotent` 部分唯一索引保证同一订单最多一条待审批退款记录，从 DB 层兜底幂等。

## API Design

### GET /api/orders/{order_id}/refund/check

- **鉴权**：必须登录，且为订单所属学员
- **Response 200**:
  ```json
  {
    "eligible": true,
    "refund_amount": 144000,
    "calculation": {
      "paid_amount": 180000,
      "total_hours": 10,
      "consumed_count": 2,
      "formula": "180000 × (10-2)/10"
    },
    "reason_codes": [
      { "code": 1, "label": "教练原因" },
      { "code": 2, "label": "个人原因" },
      { "code": 3, "label": "平台原因" }
    ]
  }
  ```
- **Response 400**: `{ code: "ORDER_NOT_PAID" | "PACKAGE_ALREADY_REFUNDED" | "PACKAGE_FROZEN" | "REFUND_IN_PROGRESS" }`
- **Response 401**: 未登录
- **Response 403**: 非订单所属用户

### POST /api/orders/{order_id}/refund

- **鉴权**：必须登录，且为订单所属学员
- **Request**:
  ```json
  {
    "reason_type": 2,
    "reason_detail": "时间冲突，无法继续学习"
  }
  ```
- **Response 201**: `{ "refund_id": 12345, "status": "待审批" }`
- **Response 400**: `{ code: "REFUND_IN_PROGRESS" | "PACKAGE_ALREADY_REFUNDED" | "PACKAGE_FROZEN" | "ORDER_NOT_PAID" | "INVALID_REASON_TYPE" }`
- **Response 401**: 未登录
- **Response 403**: 非订单所属用户

### 业务规则

- 退款金额 = `paid_amount × (total_hours - consumed_count) / total_hours`（PRD §6.4.2），向下取整到分
- `package.status` 必须 ∈ {active, exhausted, expired}，否则按状态返回对应错误码
- 已存在 `status=0`（待审批）的 refund_record → 返回 `REFUND_IN_PROGRESS`
- `reason_type` 必须 ∈ {1, 2, 3}，否则返回 `INVALID_REASON_TYPE`

## State Machine

```
order: 已支付 ──[学员提交退款]──→ 退款审批中
package: active/exhausted/expired ──[学员提交退款]──→ frozen
refund_record: (无) ──[学员提交]──→ 待审批
```

转换在单一数据库事务内完成，保证三者原子性。

## Caching

| 缓存 | Key | TTL | 失效策略 |
|------|-----|-----|---------|
| 退款资格检查结果 | `refund:check:{order_id}` | 30s | 订单/套餐状态变更时主动删除；提交退款后立即删除 |

缓存仅缓存 eligible + refund_amount + calculation，不缓存 reason_codes（静态数据由前端字典维护）。

## Performance Targets

| 指标 | 目标 |
|------|------|
| 退款申请接口（POST）P99 | < 300ms |
| 退款资格检查接口（GET）P99 | < 200ms |
| 退款资格检查缓存命中率 | ≥ 70%（同一用户多次查看） |

## Security

- **鉴权**：所有接口必须校验登录态 + 订单归属（`order.user_id == current_user.id`）
- **幂等**：幂等键 `{user_id}:{order_id}:refund`，通过 Redis 分布式锁 + DB 部分唯一索引双重保障
- **事务**：refund_record 创建 + order 状态更新 + package 冻结 + reserved 释放必须在同一事务，任一失败回滚
- **金额校验**：服务端重新计算 refund_amount，不信任前端传入的金额
- **输入校验**：`reason_type` 枚举校验，`reason_detail` 长度 ≤ 500
- **限流**：单用户对同一订单的 refund/check 接口限流 10 次/分钟

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-004 / US-005 | 依赖 | 学员登录态 |
| US-025 / US-026 | 依赖 | 已支付订单与订单详情页入口 |
| US-028 | 被依赖 | 管理员处理退款申请，消费本 US 创建的 refund_record |
