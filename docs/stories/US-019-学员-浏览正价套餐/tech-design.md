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

### 2.1 GET /api/coaches/{id}/packages

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
- **Response 404**: `{ code: COACH_NOT_FOUND }`（教练不存在或 status ∉ {1, 4}）

### 2.2 业务规则

- 仅返回 `coach.status IN (1, 4)` 的教练
- `reference_price` 为空时 `custom_package_enabled = false`
- 标准套餐仅返回 `status = 1`（启用）的记录

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
