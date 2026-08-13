# Design: US-019 学员浏览正价套餐

## Overview

正价套餐浏览支持两个入口：

- **入口 A（全局套餐列表）**：用户从首页/全部套餐进入，查看所有已上架套餐，按体验课/标准正价课/自定义分组。点击套餐卡片进入套餐详情页，详情页返回适配教练列表供 US-020 选择教练。
- **入口 B（教练详情页）**：用户从教练详情页点击「查看套餐」，查看当前教练支持的标准套餐、体验课、自定义课时入口。点击套餐卡片进入套餐详情页，详情页返回当前教练信息。

两个入口最终都跳转 US-020 完成下单。

## Data Model

### 表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `coach` | 读 | `id`, `status`, `reference_price_per_hour` |
| `package_template` | 读 | `id`, `package_mode`, `hours`, `price`, `validity_days`, `status` |
| `coach_package_template`（或等效关联表） | 读 | `coach_id`, `package_template_id` |

### package_template 字段

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK | — |
| `package_mode` | VARCHAR | NOT NULL | `experience` / `standard` / `custom` |
| `hours` | INT | — | 课时数（1/6/8/10），自定义可为空 |
| `price` | INT | — | 价格（分），自定义可为空 |
| `validity_days` | INT | — | 有效期天数 |
| `status` | TINYINT | 默认 1 | 1=启用 0=禁用 |
| `created_at` / `updated_at` | DATETIME | — | 时间戳 |

### 索引

```sql
CREATE INDEX idx_package_template_status_mode ON package_template(status, package_mode);
CREATE INDEX idx_coach_package_template_coach ON coach_package_template(coach_id, package_template_id);
```

## API Design

### POST /api/packages/list

- **鉴权**：否（游客可访问）
- **Body**：`{}` 或分页参数
- **Response 200**:
  ```json
  {
    "packages": [
      { "id": 1, "package_mode": "experience", "name": "体验课", "hours": 1, "price": 9900, "validity_days": 30 },
      { "id": 2, "package_mode": "standard", "name": "标准 6 节", "hours": 6, "price": 108000, "validity_days": 90 },
      { "id": 3, "package_mode": "custom", "name": "自定义课时", "hours": null, "price": null, "validity_days": null }
    ]
  }
  ```

### POST /api/coach/packages/list

- **鉴权**：否（游客可访问）
- **Body**：`{ "coach_id": 1 }`
- **Response 200**:
  ```json
  {
    "coach_id": 1,
    "coach_status": 1,
    "reference_price": 20000,
    "packages": [
      { "id": 1, "package_mode": "experience", "name": "体验课", "hours": 1, "price": 9900, "validity_days": 30 },
      { "id": 2, "package_mode": "standard", "name": "标准 6 节", "hours": 6, "price": 108000, "validity_days": 90 }
    ],
    "custom_package_enabled": true,
    "custom_hours_min": 1,
    "custom_hours_max": 50
  }
  ```
- **Response 404**: `{ code: COACH_NOT_FOUND }`

### POST /api/packages/detail

- **鉴权**：否（游客可访问）
- **Body**：`{ "package_id": 2 }` 或 `{ "package_id": 2, "coach_id": 1 }`
- **Response 200（全局入口，未传 coach_id）**:
  ```json
  {
    "package": { "id": 2, "package_mode": "standard", "name": "标准 6 节", "hours": 6, "price": 108000, "validity_days": 90 },
    "coaches": [
      { "id": 1, "avatar_url": "...", "name": "教练 A", "rating": 4.8, "strokes": ["自由泳", "蛙泳"], "teaching_years": 5, "total_students": 120 },
      { "id": 2, "avatar_url": "...", "name": "教练 B", "rating": 4.5, "strokes": ["蝶泳"], "teaching_years": 3, "total_students": 80 }
    ]
  }
  ```
- **Response 200（教练详情页入口，传入 coach_id）**:
  ```json
  {
    "package": { "id": 2, "package_mode": "standard", "name": "标准 6 节", "hours": 6, "price": 108000, "validity_days": 90 },
    "coach": { "id": 1, "avatar_url": "...", "name": "教练 A", "rating": 4.8, "strokes": ["自由泳", "蛙泳"], "teaching_years": 5, "total_students": 120 }
  }
  ```
- **Response 404**: `{ code: PACKAGE_NOT_FOUND }`

### 业务规则

- 仅返回 `coach.status = 1`（已通过且可约）的教练
- `reference_price` 为空时 `custom_package_enabled = false`
- 标准套餐仅返回 `package_template.status = active`（启用）的记录
- 教练不可约（离职/冻结）时统一返回 404 `COACH_NOT_FOUND`
- 全局列表不过滤教练，返回所有已上架套餐
- 套餐详情页根据 `coach_id` 参数返回当前教练信息或适配教练列表

## Caching

| 缓存 | 键 | TTL | 失效策略 |
|------|----|-----|---------|
| 教练套餐列表 | `coach:{id}:packages` | 5 分钟 | 管理员配置变更（US-045）或教练更新参考单价（US-012）时主动失效 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 套餐列表查询 P99 | < 200ms |
| 套餐详情查询 P99 | < 200ms |

## Security

- 无需登录鉴权（游客可访问）
- 防刷：同一 IP 1 分钟内请求 > 200 次，触发限流 HTTP 429
- 教练 ID 遍历防护：`status ∉ {1, 4}` 统一返回 404，不泄露教练存在性

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-011 | 依赖 | 教练审核通过（status=1） |
| US-012 | 依赖 | 教练参考单价 |
| US-045 | 依赖 | 标准/自定义套餐配置 |
| US-020 | 被依赖 | 购买正价套餐（从本 US 跳转下单） |
