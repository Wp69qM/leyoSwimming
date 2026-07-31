# Design: US-019 学员浏览正价套餐

## Overview

正价套餐浏览流程：用户进入教练详情页点击「查看套餐」→ 后端校验教练状态可见性 → 读取标准套餐 + 参考单价 → 返回标准套餐列表与自定义课时入口开关 → 前端展示并跳转 US-020 下单。

## Data Model

### 表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `coach` | 读 | `id`, `status`, `reference_price_per_hour` |
| `standard_package` / `package_template` | 读 | `id`, `hours`, `price`, `validity_days`, `status` |

### standard_package 字段

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK | — |
| `hours` | INT | NOT NULL | 课时数（1/6/8/10） |
| `price` | INT | NOT NULL | 价格（分） |
| `validity_days` | INT | NOT NULL | 有效期天数 |
| `status` | TINYINT | 默认 1 | 1=启用 0=禁用 |
| `created_at` / `updated_at` | DATETIME | — | 时间戳 |

### 索引

```sql
CREATE INDEX idx_standard_package_status ON standard_package(status, hours);
```

## API Design

### GET /api/coaches/{id}/packages

- **鉴权**：否（游客可访问）
- **Path**：`id` = 教练 ID
- **Response 200**:
  ```json
  {
    "coach_id": 1,
    "coach_status": 1,
    "reference_price": 20000,
    "standard_packages": [
      { "id": 1, "hours": 1, "price": 20000, "validity_days": 30 },
      { "id": 2, "hours": 6, "price": 108000, "validity_days": 90 },
      { "id": 3, "hours": 8, "price": 144000, "validity_days": 120 },
      { "id": 4, "hours": 10, "price": 180000, "validity_days": 150 }
    ],
    "custom_package_enabled": true,
    "custom_hours_min": 1,
    "custom_hours_max": 50
  }
  ```
- **Response 404**: `{ code: COACH_NOT_FOUND }`（教练不存在或 `status ≠ 1` 或不可约）

### 业务规则

- 仅返回 `coach.status = 1`（已通过且可约）的教练
- `reference_price` 为空时 `custom_package_enabled = false`
- 标准套餐仅返回 `package_template.status = active`（启用）的记录
- 教练不可约（离职/冻结）时统一返回 404 `COACH_NOT_FOUND`

## Caching

| 缓存 | 键 | TTL | 失效策略 |
|------|----|-----|---------|
| 教练套餐列表 | `coach:{id}:packages` | 5 分钟 | 管理员配置变更（US-045）或教练更新参考单价（US-012）时主动失效 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 套餐列表查询 P99 | < 200ms |

## Security

- 无需登录鉴权（游客可访问）
- 防刷：同一 IP 1 分钟内请求 > 200 次，触发限流 HTTP 429
- 教练 ID 遍历防护：`status ∉ {1, 4}` 统一返回 404，不泄露教练存在性

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-011 | 依赖 | 教练审核通过（status=1） |
| US-012 | 依赖 | 教练参考单价 |
| US-045 | 依赖 | 标准套餐配置 |
| US-020 | 被依赖 | 购买正价套餐（从本 US 跳转下单） |
