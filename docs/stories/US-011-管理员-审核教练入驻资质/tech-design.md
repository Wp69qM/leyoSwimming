# US-011 管理员审核教练入驻资质 — 技术设计

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## 1. 数据模型

### 1.1 涉及的表

| 表名 | 操作 | 说明 |
|------|------|------|
| `coach_application` | 修改 | 审核目标；status pending → approved/rejected |
| `coach` | 修改 | 通过时由 application 快照覆盖生效资料；status、approved_at 更新 |
| `coach_certificate` | 修改 | 通过时由 coach_certificate_application 快照覆盖 |
| `coach_audit_log` | 新增 | 审核事件日志；from_status / to_status 记录 coach.status 变更 |
| `notification` | 新增 | 发送给教练的通知 |

### 1.2 字段定义

**coach_application 表（审核目标）**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `application_id` | BIGINT | PK | 申请快照 ID |
| `coach_id` | BIGINT | FK | 教练 ID |
| `status` | ENUM | 非空 | draft / pending / approved / rejected |
| `previous_coach_status` | TINYINT | 非空 | 提交前 coach.status（-1/2/3） |
| 资料字段（name/avatar_url/gender/age/email/wechat_qr_url/id_card_no/teaching_years/total_students/total_hours/teaching_strokes/bio/reference_price） | — | 可空 | 申请快照字段，与 coach 表同构 |
| `submitted_at` | DATETIME | 可空 | 提交时间 |
| `approved_at` | DATETIME | 可空 | 通过时间 |
| `approved_by` | BIGINT | 可空 | 审核管理员 ID |
| `rejection_reason` | VARCHAR(512) | 可空 | 驳回原因 |
| `created_at` | DATETIME | 默认 CURRENT_TIMESTAMP | 创建时间 |

**coach_audit_log 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `log_id` | BIGINT | PK | 日志 ID |
| `coach_id` | BIGINT | FK | 教练 ID |
| `application_id` | BIGINT | FK | 关联申请快照 ID |
| `admin_id` | BIGINT | FK | 管理员 ID |
| `action` | VARCHAR(16) | 非空 | approve / reject |
| `from_status` | TINYINT | 非空 | 变更前 coach.status |
| `to_status` | TINYINT | 非空 | 变更后 coach.status |
| `reason` | VARCHAR(512) | 可空 | 原因 |
| `created_at` | DATETIME | 默认 CURRENT_TIMESTAMP | 时间 |

---

## 2. API 设计

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/coach/applications` | GET | 待审核列表；查询 coach_application.status = pending |
| `/api/admin/coach/applications/{application_id}/approve` | POST | 通过；将快照覆盖写入 coach 及 coach_certificate |
| `/api/admin/coach/applications/{application_id}/reject` | POST | 驳回；coach.status 恢复为 previous_coach_status |

### 2.1 GET /api/admin/coach/applications

- **查询参数**：`status=pending`、`page`、`page_size`
- **说明**：待审核列表直接查询 `coach_application.status = pending`，不再依赖 coach.status/submitted_at 推断

### 2.2 POST /api/admin/coach/applications/{application_id}/approve

- **功能**：
  1. 校验 `coach_application.status = pending`
  2. 将 coach_application 快照字段覆盖写入 coach 表
  3. 将 coach_certificate_application 快照覆盖写入 coach_certificate 表
  4. 更新 `coach.status = 1`、`coach.approved_at = now`
  5. 更新 `coach_application.status = approved`、`approved_at`、`approved_by`
  6. 写入 `coach_audit_log`：`action='approve'`、`from_status=0`、`to_status=1`
  7. 异步发送通知
- **响应体**：
  ```json
  {
    "code": 0,
    "data": {
      "coach_id": 20001,
      "application_id": 10001,
      "status": 1,
      "approved_at": "2026-07-30T12:00:00Z"
    }
  }
  ```
- **错误码**：`NOT_PENDING` (400501), `FORBIDDEN` (403001)

### 2.3 POST /api/admin/coach/applications/{application_id}/reject

- **请求体**：`{ "reason": "证书不清晰" }`
- **功能**：
  1. 校验 `coach_application.status = pending`
  2. 更新 `coach_application.status = rejected`、`rejection_reason = reason`
  3. 根据 `coach_application.previous_coach_status` 恢复 `coach.status`：
     - previous_coach_status=-1 → coach.status=2
     - previous_coach_status=2 → coach.status=2
     - previous_coach_status=3 → coach.status=3
  4. 写入 `coach_audit_log`：`action='reject'`、`from_status=0`、`to_status=恢复后的status`、`reason`
  5. 异步发送通知
- **响应体**：
  ```json
  {
    "code": 0,
    "data": {
      "coach_id": 20001,
      "application_id": 10001,
      "status": 2,
      "rejection_reason": "证书不清晰"
    }
  }
  ```
- **错误码**：`NOT_PENDING` (400501), `FORBIDDEN` (403001)

---

## 3. 状态机

### 3.1 coach.status（生命周期状态）

```
待审核(0) ──[通过]──→ 已通过(1)
待审核(0) ──[驳回]──→ previous_coach_status
            ├── previous=-1 → 驳回(2)
            ├── previous=2  → 驳回(2)
            └── previous=3  → 已离职(3)
驳回(2) ──[重新提交]──→ 待审核(0)
已离职(3) ──[重新入驻提交]──→ 待审核(0)
```

### 3.2 coach_application.status（快照状态）

```
draft 草稿 ──[提交审核]──→ pending 待审核
pending 待审核 ──[通过]──→ approved 已通过
pending 待审核 ──[驳回]──→ rejected 已驳回
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
