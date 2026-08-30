# Tech Design: US-019 学员浏览正价套餐

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `coach` | 读 | 校验教练状态、读取参考单价 |
| `package_template` | 读 | 读取管理员配置的套餐模板 |
| `package_template_coach` | 读 | 校验标准套餐是否适配当前教练 |

### 1.2 字段

#### package_template

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK | 模板 ID |
| `name` | VARCHAR(64) | NOT NULL | 套餐名称 |
| `package_mode` | VARCHAR(20) | NOT NULL | 套餐模式：standard / experience / custom |
| `teaching_type` | VARCHAR(20) | NOT NULL | 教学类型/班级规模：one_on_one / one_on_two / one_on_three |
| `total_hours` | INT | CHECK > 0 | 课时数 |
| `duration_minutes` | INT | CHECK > 0 | 每节课时长（分钟） |
| `valid_days` | INT | CHECK > 0 | 有效期天数 |
| `original_price` | DECIMAL(10,2) | CHECK >= 0 | 原价 |
| `price` | DECIMAL(10,2) | CHECK >= 0 | 售价 |
| `refund_enabled` | TINYINT(1) | NOT NULL | 是否支持退款 |
| `refund_ratio` | DECIMAL(3,2) | CHECK >= 0 | 退款比例（0~1） |
| `refund_valid_days` | INT | CHECK >= 0 | 退款有效期限制（天） |
| `status` | VARCHAR(16) | NOT NULL | 上下架：inactive / active |
| `created_at` / `updated_at` | DATETIME | — | 时间戳 |

#### package_template_coach

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK | 关联 ID |
| `package_template_id` | BIGINT | FK, IDX | 套餐模板 ID |
| `coach_id` | BIGINT | FK, IDX | 教练 ID |
| `reference_price_snapshot` | DECIMAL(10,2) | CHECK >= 0 | 保存时该教练的参考单价快照 |

### 1.3 索引

```sql
CREATE INDEX idx_package_template_status_mode ON package_template(status, package_mode);
CREATE INDEX idx_package_template_coach ON package_template_coach(coach_id, package_template_id);
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
- 套餐模板仅返回 `status = 'active'` 的记录
- 教练详情页入口的标准/体验套餐需通过 `package_template_coach` 校验是否适配当前教练
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

---

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | 开发 | 初版（基于旧 `standard_package` 模型） |
| v1.1 | 2026-08-15 | AI | 三件套一致性修复：§1 数据模型由 `standard_package` 更新为 `package_template` / `package_template_coach`，索引与业务规则同步适配 US-045 |
