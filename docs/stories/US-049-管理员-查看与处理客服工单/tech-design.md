# US-049 技术设计：管理员查看数据看板与处理客服工单

> 本文档对应 `docs/stories/US-049-.../user-story.md` 的技术实现方案。
> 角色：开发 | 最后更新：2026-07-30

---

## 0. 文档定位

设计层，定义数据看板与客服工单的数据模型、API、状态机、缓存与权限方案。

---

## 1. 数据模型影响

### 1.1 新增/修改表

| 表名 | 操作 | 说明 |
|------|------|------|
| `support_ticket` | 新增 | 客服工单 |
| `ticket_reply` | 新增 | 工单回复 |
| `booking` / `package` / `coach` / `order` / `user` | 读取 | 看板数据源 |
| `audit_log` | 新增 | 工单处理审计 |

### 1.2 `support_ticket` 字段

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT PK | AUTO_INCREMENT | 工单 ID |
| `ticket_no` | VARCHAR(32) | UK | 工单编号 |
| `type` | TINYINT | NOT NULL | 0=投诉 1=建议 2=换教练 3=退款申诉 |
| `user_id` | BIGINT FK | IDX | 提交者 |
| `coach_id` | BIGINT FK | | 关联教练 |
| `order_id` | BIGINT FK | | 关联订单 |
| `title` | VARCHAR(128) | NOT NULL | 标题 |
| `content` | TEXT | NOT NULL | 内容 |
| `status` | TINYINT | 0=pending 1=processing 2=awaiting_feedback 3=closed | 状态 |
| `handler_id` | BIGINT FK | | 处理人 |
| `created_at` | DATETIME | | |
| `updated_at` | DATETIME | | |

### 1.3 `ticket_reply` 字段

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT PK | | |
| `ticket_id` | BIGINT FK | IDX | 工单 ID |
| `replier_type` | TINYINT | 0=user 1=admin | 回复者类型 |
| `replier_id` | BIGINT | | 回复者 ID |
| `content` | TEXT | NOT NULL | 回复内容 |
| `created_at` | DATETIME | | |

### 1.4 索引

```sql
CREATE INDEX idx_support_ticket_status ON support_ticket(status);
CREATE INDEX idx_support_ticket_user ON support_ticket(user_id, status);
CREATE INDEX idx_ticket_reply_ticket ON ticket_reply(ticket_id);
```

---

## 2. API 设计

### 2.1 GET /api/admin/dashboard/peak-hours

- **鉴权**：管理员登录 + `dashboard:read`
- **Query**：`course_type`（0=体验 1=正价），`start_date`，`end_date`
- **Response 200**：`{ hours: [{ hour, count } x 24] }`
- **Response 400**：`{ error: 'INVALID_DATE_RANGE' }`
- **Response 403**：`{ error: 'FORBIDDEN' }`

### 2.2 GET /api/admin/dashboard/monthly-hours

- **鉴权**：管理员登录 + `dashboard:read`
- **Query**：`start_month`，`end_month`
- **Response 200**：`{ months: [{ month, coach_id, name, total_hours }] }`
- **Response 400**：`{ error: 'INVALID_DATE_RANGE' }`

### 2.3 GET /api/admin/dashboard/coach-ratings

- **鉴权**：管理员登录 + `dashboard:read`
- **Query**：`limit`（默认 50）
- **Response 200**：`{ items: [{ coach_id, name, rate, total_students, total_hours }] }`

### 2.4 GET /api/admin/dashboard/student-portrait

- **鉴权**：管理员登录 + `dashboard:read`
- **Response 200**：`{ student_count, conversion_funnel, package_distribution, top_coaches, package_type_ratio }`

### 2.5 GET /api/admin/tickets

- **鉴权**：管理员登录 + `ticket:read`
- **Query**：`status`，`type`，`page`，`size`
- **Response 200**：`{ items: Ticket[], total, page, size }`

### 2.6 GET /api/admin/tickets/:id

- **鉴权**：管理员登录 + `ticket:read`
- **Response 200**：工单详情 + 回复列表
- **Response 404**：`{ error: 'TICKET_NOT_FOUND' }`

### 2.7 PUT /api/admin/tickets/:id/assign

- **鉴权**：管理员登录 + `ticket:write`
- **Body**：`{ handler_id }`
- **Response 200**：更新后的工单

### 2.8 POST /api/admin/tickets/:id/reply

- **鉴权**：管理员登录 + `ticket:write`
- **幂等性**：`Idempotency-Key`
- **Body**：`{ content, status? }`
- **Response 201**：回复记录
- **Response 400**：`{ error: 'TICKET_CLOSED' }`

### 2.9 PUT /api/admin/tickets/:id/status

- **鉴权**：管理员登录 + `ticket:write`
- **Body**：`{ status }`
- **Response 200**：更新后的工单

---

## 3. 状态机影响

```
support_ticket.status:
  pending ──[assign]──→ processing
  processing ──[reply]──→ awaiting_feedback
  awaiting_feedback ──[user confirm / admin close]──→ closed
  closed（终态）
```

- closed 后禁止回复与分配

---

## 4. 缓存策略

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis | `dashboard:peak-hours:{type}:{range}` | 300s | 新预约/取消时失效 |
| Redis | `dashboard:monthly-hours:{range}` | 600s | 上课记录变更时失效 |
| Redis | `dashboard:coach-ratings` | 600s | 教练评分变更时失效 |
| Redis | `dashboard:student-portrait` | 600s | 用户身份变更时失效 |
| DB | 物化视图 mv_user_status（P2）| — | — |

### 4.1 降级策略

- Redis 不可用时直接查 DB，记录 warning 日志

---

## 5. 性能指标

| 指标 | 目标 |
|------|------|
| 看板接口 P50 | < 200ms |
| 看板接口 P99 | < 500ms |
| 工单列表 P99 | < 200ms |
| 工单详情 P99 | < 150ms |
| 并发 100 QPS | 无 5xx |

---

## 6. 安全 / 鉴权

- 所有管理接口登录 + RBAC
- 看板数据脱敏（不返回用户手机号等敏感字段）
- 工单回复内容 XSS 过滤
- 操作日志记录管理员 ID、IP、变更前后快照

---

## 7. 跨 US 依赖

| US | 依赖方向 | 说明 |
|----|---------|------|
| US-004 / US-025 / US-032 | 被依赖 | 用户、订单、上课记录数据 |
| US-027 / US-030 | 依赖本 US | 退款/取消纠纷工单处理 |

---

## 8. 异常与边界

| 场景 | 处理 |
|------|------|
| 时间范围不合法 | 400 INVALID_DATE_RANGE |
| 工单不存在 | 404 TICKET_NOT_FOUND |
| 无权限 | 403 FORBIDDEN |
| 工单已关闭 | 400 TICKET_CLOSED |
| 并发状态冲突 | 乐观锁 version 字段，冲突返回 409 CONCURRENT_MODIFY |
| 看板数据为空 | 返回空数组，前端展示空状态 |

---

## 9. 实现顺序（与 test-plan 的映射）

| tech-design 章节 | 对应 test-plan Task |
|------------------|---------------------|
| §1 数据模型 | Task 1 |
| §2 API 设计 | Task 2-8 |
| §4 缓存策略 | Task 9 |

---

## 10. 上下游引用

- 上游需求：[./user-story.md](./user-story.md)
- 执行计划：[./test-plan.md](./test-plan.md)
- 全局规范：[docs/spec/tech-design/README.md](../../spec/tech-design/README.md)

---

## 11. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版 |
