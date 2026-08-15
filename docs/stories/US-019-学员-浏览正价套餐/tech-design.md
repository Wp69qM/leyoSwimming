# Tech Design: US-019 学员浏览正价套餐

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `coach` | 读 | 校验教练状态、读取参考单价 |
| `standard_package` | 读 | 读取管理员配置的标准套餐 |

### 1.2 字段

#### standard_package

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK | — |
| `hours` | INT | NOT NULL | 课时数（1/6/8/10） |
| `price` | INT | NOT NULL | 价格（分） |
| `validity_days` | INT | NOT NULL | 有效期天数 |
| `status` | TINYINT | 默认 1 | 1=启用 0=禁用 |
| `created_at` / `updated_at` | DATETIME | — | 时间戳 |

### 1.3 索引

```sql
CREATE INDEX idx_standard_package_status ON standard_package(status, hours);
```

---

## 2. API 设计

> 统一使用 POST，URL 按 `/api/{module}/{resource}/{action}`，参数通过 JSON body 传递，字段使用小驼峰。

### 2.1 POST /api/package/list

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

### 2.2 POST /api/coach/package/list

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
- **Response 404**: `{ code: COACH_NOT_FOUND }`（教练不存在或 status ∉ {1, 4}）

### 2.3 POST /api/package/detail

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
- **Response 404**: `{ code: PACKAGE_NOT_FOUND }`（模板不存在、已下架或 packageMode 不匹配）

### 2.4 业务规则

- 仅返回 `coach.status IN (1, 4)` 的教练
- `referencePrice` 为空时 `customPackageEnabled = false`
- 标准套餐仅返回 `status = 1`（启用）的记录
- `applicableCoaches` 在未传 `coachId` 时返回全部适配教练；传入 `coachId` 时仅返回当前教练信息

---

## 3. 状态机

US-019 是只读 US，不修改任何实体状态。

---

## 4. 缓存策略

| 缓存 | 键 | TTL | 失效策略 |
|------|----|-----|---------|
| 教练套餐列表 | `coach:{id}:packages` | 5 分钟 | 管理员配置变更（US-045）或教练更新参考单价（US-012）时主动失效 |

---

## 5. 性能

| 指标 | 目标 |
|------|------|
| 套餐列表查询 P99 | < 200ms |

---

## 6. 安全

- 无需登录鉴权（游客可访问）
- 防刷：同一 IP 1 分钟内请求 > 200 次，触发限流 429
- 教练 ID 遍历防护：status ∉ {1, 4} 统一返回 404

---

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-011 | 依赖 | 教练审核通过 |
| US-012 | 依赖 | 教练参考单价 |
| US-045 | 依赖 | 标准套餐配置 |
| US-020 | 被依赖 | 购买正价套餐（从本 US 跳转下单） |

---

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 正常浏览套餐 | `test_get_coach_packages_success` |
| 教练未设参考单价 | `test_get_coach_packages_no_reference_price` |
| 教练不可约 | `test_get_coach_packages_not_found` |
| 标准套餐为空 | `test_get_coach_packages_empty_standard` |
