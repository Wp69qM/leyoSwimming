# Design: US-021 学员查看我的套餐

## Overview

只读 US，提供当前用户套餐列表与状态分组。套餐详情页在满足退款条件时展示「申请退款」入口，点击跳转 US-027 发起退款。

## Data Model

### 表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `package` | 读 | `user_id`, `coach_id`, `status`, `total_hours`, `consumed_count`, `available_count`, `reserved_count`, `expire_at`, `frozen_reason`, 快照字段 `paid_amount`, `refund_enabled`, `refund_ratio`, `refund_valid_days` |
| `coach` | 读 | `name`, `status` |
| `user` | 读 | `identity` |

### 索引

```sql
CREATE INDEX idx_package_user_status ON package(user_id, status);
CREATE INDEX idx_package_user_type ON package(user_id, package_type);
```

## API Design

### POST /api/user/package/list

- **鉴权**：必须登录
- **Request**:
  ```json
  {
    "status": "active"
  }
  ```
- **Response 200**:
  ```json
  {
    "active": { "items": [...], "summary": { "totalHours", "consumedHours", "availableHours" } },
    "exhausted": { "items": [...] },
    "expired": { "items": [...] },
    "refunded": { "items": [...] },
    "frozen": { "items": [...] }
  }
  ```

### POST /api/coach/package/detail

- **鉴权**：教练 JWT，`coach.status = 1`
- **Request**:
  ```json
  {
    "packageId": 1
  }
  ```
- **Response 200**:
  ```json
  {
    "packageId": 1,
    "userId": 10001,
    "userName": "张小明",
    "packageMode": "standard",
    "status": "active",
    "totalHours": 10,
    "availableHours": 8,
    "reservedHours": 2,
    "consumedHours": 2,
    "expireAt": "2026-10-01T00:00:00Z",
    "usageRecords": [
      { "id": 1, "lessonId": 101, "consumedHours": 1, "consumedAt": "2026-08-10T10:00:00Z" }
    ]
  }
  ```
- **Response 403**: `{ code: FORBIDDEN }`（package 不属于当前教练）
- **Response 404**: `{ code: PACKAGE_NOT_FOUND }`

## Caching

| 层 | Key | TTL |
|----|-----|-----|
| 小程序本地 | `my_packages` | 30s |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 列表 P99 | < 100ms |

## Security

- 登录鉴权
- 仅返回当前 user_id 数据

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-004/US-005 | 依赖 | 登录 |
| US-020 | 依赖 | 套餐购买 |
| US-027 | 被依赖 | 点击「申请退款」跳转 US-027 发起退款 |
| US-022 | 被依赖 | 更换教练 |
| US-029 | 被依赖 | 预约 |
