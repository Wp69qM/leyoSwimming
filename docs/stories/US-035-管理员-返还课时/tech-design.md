# Tech Design: US-035 管理员返还课时

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `package` | 读/写 | 校验 consumed_count，更新 consumed-1 available+1 |
| `booking` | 读 | 校验 booking 状态 ∈ {已完成, 旷课} |
| `hour_return` | 写 | 新增返还记录 |
| `audit_log` | 写 | 审计日志 |

### 1.2 hour_return 字段

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK | — |
| `booking_id` | BIGINT | FK, IDX | 关联 booking |
| `package_id` | BIGINT | FK | 关联 package |
| `admin_id` | BIGINT | FK | 操作管理员 |
| `reason_type` | TINYINT | NOT NULL | 1=教练误操作 2=客诉处理 3=系统故障 4=其他 |
| `reason_detail` | VARCHAR(500) | NULL | 详细说明 |
| `returned_hours` | INT | 默认 1 | 返还课时数（MVP 固定 1） |
| `created_at` | DATETIME | — | 时间戳 |

### 1.3 索引

```sql
CREATE INDEX idx_hour_return_booking ON hour_return(booking_id);
CREATE INDEX idx_hour_return_package ON hour_return(package_id);
```

---

## 2. API 设计

### 2.1 POST /api/admin/bookings/{booking_id}/return-hour

- **鉴权**：管理员 + `MANAGE_BOOKING` 权限
- **Request**:
  ```json
  {
    "reason_type": 1,
    "reason_detail": "教练误操作标记旷课，实际学员已到场"
  }
  ```
- **Response 200**: `{ package_id, consumed_count, available_count, status }`
- **Response 400**: `{ code: NO_CONSUMED_HOUR | BOOKING_NOT_RETURNABLE }`
- **Response 403**: 无权限

---

## 3. 状态机

```
package: consumed → available（consumed-1, available+1）
package: exhausted → active（若返还后 consumed < total_hours）
```

---

## 4. 缓存策略

无特殊缓存（写操作）。

---

## 5. 性能

| 指标 | 目标 |
|------|------|
| 返还接口 P99 | < 300ms |

---

## 6. 安全

- 管理员鉴权 + `MANAGE_BOOKING` 权限
- 必须填写返还原因（审计留痕）
- 事务包裹：package 更新 + hour_return + audit_log 同一事务
- package 乐观锁防并发

---

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-033 | 依赖 | 产生已扣课时 booking |
| US-034 | 被依赖 | 管理员查看上课记录可见返还记录 |

---

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 正常返还 | `test_return_hour_success` |
| 无可扣课时 | `test_return_hour_no_consumed` |
| 不可返还状态 | `test_return_hour_not_returnable` |
| 返还后 exhausted→active | `test_return_hour_revive_exhausted` |
| 重复返还 | `test_return_hour_duplicate` |
