# US-011 管理员审核教练入驻资质 — 技术设计

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## 1. 数据模型

### 1.1 涉及的表

| 表名 | 操作 | 说明 |
|------|------|------|
| `coach` | 修改 | status、approved_at、rejection_reason |
| `coach_audit_log` | 新增 | 审核记录 |
| `notification` | 新增 | 发送给教练的通知 |

### 1.2 字段定义

**coach 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `coach_id` | BIGINT | PK | 教练 ID |
| `status` | TINYINT | 默认 0 | 0=待审核, 1=已通过, 2=驳回 |
| `approved_at` | DATETIME | 可空 | 通过时间 |
| `rejection_reason` | VARCHAR(512) | 可空 | 驳回原因 |
| `auditor_id` | BIGINT | 可空 | 审核管理员 ID |

**coach_audit_log 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `log_id` | BIGINT | PK | ID |
| `coach_id` | BIGINT | FK | 教练 ID |
| `admin_id` | BIGINT | FK | 管理员 ID |
| `action` | VARCHAR(16) | 非空 | approve/reject |
| `reason` | VARCHAR(512) | 可空 | 原因 |
| `created_at` | DATETIME | 默认 CURRENT_TIMESTAMP | 时间 |

---

## 2. API 设计

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/coach/applications` | GET | 待审核列表 |
| `/api/admin/coach/applications/{id}/approve` | POST | 通过 |
| `/api/admin/coach/applications/{id}/reject` | POST | 驳回 |

### 2.1 POST /api/admin/coach/applications/{id}/approve

- **响应体**：
  ```json
  {
    "code": 0,
    "data": {
      "coach_id": 20001,
      "status": 1,
      "approved_at": "2026-07-30T12:00:00Z"
    }
  }
  ```
- **错误码**：`NOT_PENDING` (400501), `FORBIDDEN` (403001)

---

## 3. 状态机

```
待审核(0) ──[通过]──→ 已通过(1)
待审核(0) ──[驳回]──→ 驳回(2)
```

---

## 4. 缓存策略

| 缓存 | 键 | TTL | 说明 |
|------|----|-----|------|
| 待审核列表 | `admin:coach:applications` | 1 分钟 | 列表缓存 |
| 教练状态 | `coach:{coach_id}` | 立即失效 | 审核后刷新 |

---

## 5. 性能与安全

### 5.1 性能

- 审核列表 P99 < 200ms
- 审核操作 P99 < 300ms

### 5.2 安全

- RBAC 权限校验（coach:audit）
- 状态机校验（仅 0 可转 1/2）
- 记录审计日志
- 异步发送通知

---

## 6. 跨 US 依赖

- 依赖 US-010 产生待审核数据
- 支撑 US-012/US-013/US-014 教练端功能解锁
