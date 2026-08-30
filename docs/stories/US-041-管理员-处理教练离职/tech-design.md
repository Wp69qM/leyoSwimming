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
- 通过时按工单登记的 action 类型分流处理：transfer（换教练）、refund（生成 100% 待退款记录并冻结套餐）、continue（保持 active 继续上完）
- 拒绝时恢复 coach.status=1，不回滚已确认的学员处理结果

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

#### `refund_record`

| 字段 | 类型 | 备注 |
|------|------|------|
| `refund_id` | BIGINT PK | |
| `package_id` | BIGINT FK | |
| `ticket_id` | BIGINT FK | 关联离职工单 |
| `refund_amount` | DECIMAL(10,2) | `price_per_hour × (reserved_count + available_count)`，已消耗课时不退（PRD §6.4.5） |
| `status` | TINYINT | 0=pending, 1=approved, 2=rejected, 3=completed |
| `created_at` | DATETIME | |
| `updated_at` | DATETIME | |

---

## 4. API 设计

### 4.1 `POST /api/admin/coach/resignation-ticket/list`

- **鉴权**：管理员 JWT，`MANAGE_COACH_RESIGNATION`
- **请求体**：`{ "status": "pending_audit", "page": 1, "pageSize": 20 }`
- **响应 200**：工单列表摘要

### 4.2 `POST /api/admin/coach/resignation-ticket/detail`

- **鉴权**：管理员 JWT
- **请求体**：`{ "ticketId": 10001 }`
- **响应 200**：工单详情 + coach 信息 + 套餐清单 + actions + checklist 状态

### 4.3 `POST /api/admin/coach/resignation-ticket/approve`

- **鉴权**：管理员 JWT
- **请求体**：`{ "ticketId": 10001 }`
- **功能**：通过审批，触发批量变更
- **执行顺序**：
  1. 按 `coach_resignation_action` 中登记的 action 类型，逐份处理 active package：
     - **transfer**：`package.coach_id` 更新为新教练，不生成退款记录，不冻结；
     - **refund**：生成 `refund_record`：`refund_amount = 单价 × 剩余课时`（已消耗不退），`package.status → frozen`（`frozen_reason='coach_resigned'`）；
     - **continue**：package 保持 active，不生成退款记录，不冻结；
  2. 取消该教练所有未来 booking（`start_time > NOW()` 且 status ∈ 已预约/待上课），`cancel_reason = 2`；
  3. 将未来 `schedule_slot`（`start_time > NOW()`）更新为 hidden；
  4. `coach.status 4 → 3`。
- **响应 200**：`{ "message": "审批通过" }`
- **错误码**：
  - `CHECKLIST_NOT_PASSED`（400）
  - `SCHEDULE_NOT_CLEARED`（400）
  - `TICKET_NOT_PENDING_AUDIT`（409）
  - `TICKET_ALREADY_PROCESSED`（409）

### 4.4 `POST /api/admin/coach/resignation-ticket/reject`

- **鉴权**：管理员 JWT
- **请求体**：`{ "ticketId": 10001, "reason": "..." }`
- **功能**：拒绝审批，`coach.status: 4 → 1`
- **响应 200**：`{ "message": "已拒绝，教练可继续教学" }`
- **业务逻辑**：
  1. 校验 ticket 存在且 `status = pending_audit`
  2. 更新 `coach_resignation_ticket.status = rejected`、`reason = reason`（如提供）
  3. 恢复 `coach.status = 1`

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

### 5.4 refund_record 状态

- `pending` → `approved` / `rejected` / `completed`（US-028 处理退款）
- 教练主动离职时，所有 active package 未消耗剩余课时强制 100% 退款

### 5.5 booking 状态

- `已预约` / `待上课` → `已取消`（cancel_reason = 2，教练离职）

> **cancel_reason 字段类型**（v3 评审 P0 修复）：TINYINT 整型，全项目统一枚举 `1=学员取消 / 2=教练离职 / 3=学员旷课 / 4=场馆闭馆 / 5=教练请假 / 6=套餐冻结`。本 US 使用 `2=教练离职`。

> **注**：booking 状态机不含「待支付」。「待支付」是 order 实体的状态（PRD §6.2.1/§6.2.2），不应出现在 booking 状态转换中。原 §5.4 错误列入「待支付」已修正。

---

## 6. 缓存策略

- 审批队列列表缓存 30 秒
- 工单详情不缓存
- 批量变更后失效相关 coach、package、booking 缓存

---

## 7. 性能指标

- `POST /api/admin/coach/resignation-ticket/list` P99 < 200ms
- `POST /api/admin/coach/resignation-ticket/detail` P99 < 200ms
- `POST /api/admin/coach/resignation-ticket/approve` P99 < 1s（100 份 package 以内）

---

## 8. 安全

- 管理员接口校验 `MANAGE_COACH_RESIGNATION` 权限
- approve/reject 使用数据库行锁或乐观锁防止并发处理
- 批量更新使用事务包裹
- 自动生成 `refund_record` 时金额按「单价 × 剩余课时」计算，已消耗课时不退
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

## 11. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版 |
| v1.1 | 2026-07-31 | Dev | v3 评审 P0 修复：§5.4 booking 状态转换 cancel_reason 从字符串「教练离职」改为整型 `2`；删除「待支付」状态（待支付是 order 实体状态，非 booking 状态） |
| v1.2 | 2026-07-31 | Dev | v7 评审 P0 修复：对齐 PRD §5.4.7 三选一（refund / transfer / continue）；退款公式对齐 PRD §6.4.5：`refund_amount = price_per_hour × (reserved_count + available_count)`；§2/§3.1/§4.3/§5.3/§5.4/§8 同步调整 |
