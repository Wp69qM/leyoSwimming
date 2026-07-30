# US-010 教练提交入驻资料 — 技术设计

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## 1. 数据模型

### 1.1 涉及的表

| 表名 | 操作 | 说明 |
|------|------|------|
| `coach` | 新增 | 教练主表 |
| `coach_certificate` | 新增 | 证书图片 |
| `coach_audit_log` | 新增 | 审核日志 |

### 1.2 字段定义

**coach 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `coach_id` | BIGINT | PK | 教练 ID |
| `user_id` | BIGINT | FK | 关联用户 |
| `name` | VARCHAR(32) | 非空 | 姓名 |
| `teaching_years` | INT | 非空 | 任教年限 |
| `total_students` | INT | 默认 0 | 总学员数 |
| `total_hours` | INT | 默认 0 | 总课时数 |
| `bio` | TEXT | 可空 | 个人简介 |
| `reference_price` | DECIMAL(10,2) | 非空 | 参考单价（元/节）|
| `status` | TINYINT | 默认 0 | 0=待审核, 1=已通过, 2=驳回, 3=已离职, 4=申请离职中 |
| `created_at` | DATETIME | 默认 CURRENT_TIMESTAMP | 创建时间 |

**coach_certificate 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `cert_id` | BIGINT | PK | 证书 ID |
| `coach_id` | BIGINT | FK | 教练 ID |
| `image_url` | VARCHAR(512) | 非空 | 图片 URL |
| `sort_order` | INT | 默认 0 | 排序 |

---

## 2. API 设计

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/coach/application` | POST | 提交入驻资料 |
| `/api/coach/application/draft` | PUT | 保存草稿 |
| `/api/coach/application` | GET | 查询申请状态 |
| `/api/upload/image` | POST | 上传证书图片 |

### 2.1 POST /api/coach/application

- **请求体**：
  ```json
  {
    "name": "张教练",
    "teaching_years": 5,
    "total_students": 100,
    "total_hours": 500,
    "bio": "专业游泳教练",
    "reference_price": 300,
    "certificates": ["url1", "url2"]
  }
  ```
- **响应体**：
  ```json
  {
    "code": 0,
    "data": {
      "coach_id": 20001,
      "status": 0
    }
  }
  ```
- **错误码**：`COACH_APPLICATION_PENDING` (400401), `INVALID_PRICE` (400402)

---

## 3. 状态机

```
无 ──[提交资料]──→ 待审核(0)
```

- 本 US 触发初始转换
- 后续 US-011 触发：0 → 1（通过）或 0 → 2（驳回）

---

## 4. 缓存策略

| 缓存 | 键 | TTL | 说明 |
|------|----|-----|------|
| 教练申请状态 | `coach:application:{user_id}` | 5 分钟 | 减少重复查询 |
| 上传图片临时链接 | `upload:temp:{file_key}` | 1 小时 | 图片上传后暂存 |

---

## 5. 性能与安全

### 5.1 性能

- 提交接口 P99 < 500ms（含图片处理）
- 状态查询 P99 < 100ms

### 5.2 安全

- 图片上传校验格式与大小
- 防重复提交（数据库唯一索引 + 幂等键）
- 敏感字段后端校验

---

## 6. 跨 US 依赖

- 为 US-011 提供待审核数据
- 支撑 US-012/US-013/US-014 教练端功能
