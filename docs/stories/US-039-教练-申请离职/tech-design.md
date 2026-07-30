# US-039 教练申请离职技术设计

---

## 1. 上下文

本 US 实现教练主动离职的申请端。提交后教练状态变为 4（申请中），系统生成离职工单供教练登记学员套餐处理结果，随后进入管理员审批队列（US-041）。

---

## 2. 目标 / 非目标

**目标：**
- 仅 status=1 的教练可提交离职申请
- 提交后生成离职工单并变更教练状态
- 教练可对每份 active 套餐登记处理结果
- 登记完成后提交至管理员审批

**非目标：**
- 不实现管理员审批逻辑（US-041）
- 不实现状态 4 期间的样式标签（按 PRD 要求不展示）
- 不实现真正的退款/转教练执行（仅登记意图）

---

## 3. 数据模型

### 3.1 新增/修改表

#### `coach`（修改）

- `status` 新增允许值 4（申请中）

#### `coach_resignation_ticket`

| 字段 | 类型 | 索引 | 备注 |
|------|------|------|------|
| ticket_id | BIGINT PK | | |
| coach_id | BIGINT FK | IDX | |
| reason | VARCHAR(500) | | 离职原因 |
| status | TINYINT | IDX | 0=processing, 1=pending_audit, 2=approved, 3=rejected, 4=cancelled |
| total_packages | INT | | 待处理 active 套餐数 |
| handled_packages | INT | | 已登记数 |
| created_at | DATETIME | | |
| submitted_at | DATETIME | | 教练提交至管理员时间 |

#### `coach_resignation_action`

| 字段 | 类型 | 索引 | 备注 |
|------|------|------|------|
| action_id | BIGINT PK | | |
| ticket_id | BIGINT FK | IDX | |
| package_id | BIGINT FK | IDX | |
| action | TINYINT | | 0=transfer, 1=refund, 2=continue |
| target_coach_id | BIGINT FK | nullable | 转新教练时填写 |
| status | TINYINT | | 0=registered, 1=approved, 2=rejected |
| created_at | DATETIME | | |
| updated_at | DATETIME | | |

### 3.2 读取表

- `package`：读取 coach 名下 active 套餐生成工单清单
- `user`：读取学员姓名用于展示
- `audit_log`：记录操作

---

## 4. API 设计

### 4.1 `POST /api/coach/v1/resignation/apply`

- **鉴权**：教练 JWT，`coach.status = 1`
- **请求体**：`{ "reason": "个人发展", "idempotency_key": "..." }`
- **响应 200**：工单概要
- **错误码**：`COACH_STATUS_NOT_ALLOWED`（403）、`RESIGNATION_ALREADY_PENDING`（409）

### 4.2 `GET /api/coach/v1/resignation/ticket`

- **鉴权**：教练 JWT
- **功能**：返回当前教练的活跃工单详情与学员套餐清单
- **响应 200**：ticket + packages + actions

### 4.3 `PUT /api/coach/v1/resignation/tickets/{ticket_id}/packages/{package_id}/action`

- **鉴权**：教练 JWT，且 package 属于当前 coach
- **请求体**：`{ "action": "transfer", "target_coach_id": 200 }`
- **响应 200**：更新后的 action
- **错误码**：`NOT_OWN_PACKAGE`（403）、`TICKET_NOT_PROCESSING`（409）

### 4.4 `POST /api/coach/v1/resignation/tickets/{ticket_id}/submit`

- **鉴权**：教练 JWT
- **功能**：将工单从 processing 推进到 pending_audit
- **响应 200**：提交成功
- **错误码**：`TICKET_NOT_PROCESSING`（409）

### 4.5 `POST /api/coach/v1/resignation/tickets/{ticket_id}/cancel`

- **鉴权**：教练 JWT
- **功能**：撤销申请，恢复 coach.status=1，工单状态 cancelled
- **响应 200**
- **错误码**：`TICKET_NOT_CANCELLABLE`（409）

---

## 5. 状态机

### 5.1 教练状态

- `1` 已通过 → `4` 申请中（提交申请）
- `4` 申请中 → `1` 已通过（撤销申请）

### 5.2 离职工单状态

- `processing` → `pending_audit`（教练提交）
- `processing` → `cancelled`（教练撤销）
- `pending_audit` → `approved` / `rejected`（管理员审批，US-041）

---

## 6. 缓存策略

- 工单详情不缓存，保证管理员/教练看到最新进度
- coach.status 缓存需在更新时失效

---

## 7. 性能指标

- `POST /api/coach/v1/resignation/apply` P99 < 300ms
- `GET /api/coach/v1/resignation/ticket` P99 < 200ms
- `PUT action` P99 < 200ms

---

## 8. 安全

- 所有接口校验 JWT 中的 coach_id 与资源一致
- 仅允许修改 status=processing 的工单
- action 仅允许 transfer/refund/continue 枚举值
- 操作记录审计日志

---

## 9. 跨 US 依赖

- 依赖 US-012（教练主页管理）保证 coach.status=1 入口
- 依赖 US-020（购买套餐）产生 active package
- 输出到 US-040（重新入驻）和 US-041（管理员处理教练离职）

---

## 10. 接口测试映射

| 场景 | 测试方法 | 层级 |
|------|---------|------|
| 成功提交离职申请 | `test_apply_resignation_success` | 集成 |
| 登记套餐处理结果 | `test_register_package_action_success` | 集成 |
| 非已通过教练禁止申请 | `test_not_approved_cannot_apply` | 集成 |
| 重复提交 | `test_apply_resignation_duplicate` | 集成 |
| 登记非自己套餐 | `test_register_action_not_own_package` | 集成 |
| 撤销申请 | `test_cancel_resignation_success` | 集成 |
