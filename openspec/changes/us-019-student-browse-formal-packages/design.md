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

### POST /api/package/list

- **鉴权**：否（游客可访问）
- **Request**:
  ```json
  {
    "page": 1,
    "pageSize": 20
  }
  ```
- **Response 200**:
  ```json
  {
    "items": [
      { "id": 1, "name": "体验课", "packageMode": "experience", "hours": 1, "price": 20000, "originalPrice": 30000, "teachingType": "one_on_one", "durationMinutes": 60, "validityDays": 30 },
      { "id": 2, "name": "标准 6 节", "packageMode": "standard", "hours": 6, "price": 108000, "originalPrice": 120000, "teachingType": "one_on_one", "durationMinutes": 60, "validityDays": 90 }
    ],
    "total": 10,
    "page": 1,
    "pageSize": 20
  }
  ```

### POST /api/coach/package/list

- **鉴权**：否（游客可访问）
- **Request**:
  ```json
  {
    "coachId": 1
  }
  ```
- **Response 200**:
  ```json
  {
    "coachId": 1,
    "coachStatus": 1,
    "referencePrice": 20000,
    "standardPackages": [
      { "id": 1, "hours": 1, "price": 20000, "validityDays": 30 },
      { "id": 2, "hours": 6, "price": 108000, "validityDays": 90 },
      { "id": 3, "hours": 8, "price": 144000, "validityDays": 120 },
      { "id": 4, "hours": 10, "price": 180000, "validityDays": 150 }
    ],
    "customPackageEnabled": true,
    "customHoursMin": 1,
    "customHoursMax": 50
  }
  ```
- **Response 404**: `{ code: COACH_NOT_FOUND }`

### POST /api/package/detail

- **鉴权**：否（游客可访问）
- **Request**:
  ```json
  {
    "packageId": 1,
    "coachId": 1
  }
  ```
- **Response 200**:
  ```json
  {
    "id": 1,
    "name": "标准 6 节",
    "packageMode": "standard",
    "hours": 6,
    "price": 108000,
    "originalPrice": 120000,
    "teachingType": "one_on_one",
    "durationMinutes": 60,
    "validityDays": 90,
    "refundPolicySummary": "未消费全额退",
    "applicableCoaches": [
      { "coachId": 1, "name": "教练 A", "avatarUrl": "..." }
    ]
  }
  ```
- **Response 404**: `{ code: PACKAGE_NOT_FOUND }`

### 业务规则

- 仅返回 `coach.status IN (1, 4)` 的教练
- `referencePrice` 为空时 `customPackageEnabled = false`
- 标准套餐仅返回 `status = 1`（启用）的记录
- `applicableCoaches` 在未传 `coachId` 时返回全部适配教练；传入 `coachId` 时仅返回当前教练信息

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
