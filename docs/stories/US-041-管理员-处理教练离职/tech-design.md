# US-041 管理员处理教练离职技术设计

---

## 1. 上下文

本 US 实现管理后台对教练离职申请的审批。审批通过会触发大量批量变更：coach 状态、booking 取消、package frozen、schedule_slot hidden，需要保证原子性与幂等性。

---

## 2. 目标 / 非目标

**目标：**
- 管理员可查看 pending_audit 离职审批队列
- 管理员可通过或拒绝离职申请
- 通过时按 checklist 校验并执行批量同步变更
- 拒绝时恢复 coach.status=1，不回滚已登记处理结果

**非目标：**
- 不实现教练费实际结算（仅记录待结算）
- 不实现学员退款执行（由 US-028 处理）
- 不进行中的课程自动取消

---

## 3. 数据模型

复用 US-039 定义的表：

- `coach_resignation_ticket`
- `coach_resignation_action`
- `coach`
- `booking`
- `package`
- `schedule_slot`
- `audit_log`

### 3.1 新增字段

#### `coach_resignation_ticket`

| 字段 | 类型 | 备注 |
|------|------|------|
| `settlement_status` | TINYINT | 0=待结算 1=已结算（教练费） |
| `schedule_cleared` | BOOLEAN | 未来排班是否已清空 |

---

## 4. API 设计

### 4.1 `GET /api/admin/v1/resignation-tickets`

- **鉴权**：管理员 JWT，`MANAGE_COACH_RESIGNATION`
- **查询参数**：`status=pending_audit`（默认）、`page`、`page_size`
- **响应 200**：工单列表摘要

### 4.2 `GET /api/admin/v1/resignation-tickets/{ticket_id}`

- **鉴权**：管理员 JWT
- **响应 200**：工单详情 + coach 信息 + 套餐清单 + actions + checklist 状态

### 4.3 `POST /api/admin/v1/resignation-tickets/{ticket_id}/approve`

- **鉴权**：管理员 JWT
- **功能**：通过审批，触发批量变更
- **响应 200**：`{ "message": "审批通过" }`
- **错误码**：
  - `CHECKLIST_NOT_PASSED`（400）
  - `SCHEDULE_NOT_CLEARED`（400）
  - `TICKET_NOT_PENDING_AUDIT`（409）
  - `TICKET_ALREADY_PROCESSED`（409）

### 4.4 `POST /api/admin/v1/resignation-tickets/{ticket_id}/reject`

- **鉴权**：管理员 JWT
- **请求体**：`{ "reason": "..." }`
- **功能**：拒绝审批，`coach.status: 4 → 1`
- **响应 200**：`{ "message": "已拒绝" }`

---

## 5. 状态机

### 5.1 教练状态

- `4` 申请中 → `3` 已离职（通过）
- `4` 申请中 → `1` 已通过（拒绝）

### 5.2 离职工单状态

- `pending_audit` → `approved`
- `pending_audit` → `rejected`

### 5.3 package 状态

- `active` → `frozen`（frozen_reason = coach_resigned）

### 5.4 booking 状态

- `已预约` / `待支付` / `待上课` → `已取消`（cancel_reason = 教练离职）

---

## 6. 缓存策略

- 审批队列列表缓存 30 秒
- 工单详情不缓存
- 批量变更后失效相关 coach、package、booking 缓存

---

## 7. 性能指标

- `GET /api/admin/v1/resignation-tickets` P99 < 200ms
- `GET /api/admin/v1/resignation-tickets/{id}` P99 < 200ms
- `POST approve` P99 < 1s（100 份 package 以内）

---

## 8. 安全

- 管理员接口校验 `MANAGE_COACH_RESIGNATION` 权限
- approve/reject 使用数据库行锁或乐观锁防止并发处理
- 批量更新使用事务包裹
- 操作记录审计日志

---

## 9. 跨 US 依赖

- 依赖 US-039 生成 pending_audit 工单
- 依赖 US-014 的 schedule_slot 数据
- 输出 coach.status=3 给 US-040
- 输出 frozen package 给 US-027/US-028 退款流程

---

## 10. 接口测试映射

| 场景 | 测试方法 | 层级 |
|------|---------|------|
| 通过审批成功 | `test_approve_resignation_success` | 集成 |
| 拒绝审批成功 | `test_reject_resignation_success` | 集成 |
| 处理结果未登记 | `test_approve_checklist_not_passed` | 集成 |
| 排班未清空 | `test_approve_schedule_not_cleared` | 集成 |
| 非 pending_audit 工单 | `test_approve_ticket_not_pending` | 集成 |
| 并发审批 | `test_concurrent_approve_idempotent` | 集成 |
| 大量 package 批量处理 | `test_approve_bulk_packages` | 集成/性能 |
