# US-040 教练重新入驻技术设计

---

## 1. 上下文

本 US 让已离职教练（`coach.status = 3`）可重新发起入驻申请，状态回到 0（待审核），复用 US-010/011 的审核流程。核心约束是历史评分/评价保留但仅对老学员可见，已 frozen 的老学员套餐不自动恢复。

---

## 2. 目标 / 非目标

**目标：**
- 仅 `status = 3` 的教练可发起重新入驻
- 重新入驻时 `coach.status` 从 3 变为 0，并生成新的入驻申请记录
- 管理员可通过/拒绝重新入驻申请
- 历史评分对老学员保留可见，对新学员隐藏

**非目标：**
- 不修改 US-010/011 新教练入驻的核心审核逻辑
- 不自动解冻老学员的 frozen package
- 不删除或修改历史评价内容

---

## 3. 数据模型

### 3.1 修改表

#### `coach_application`（新增字段）

| 字段 | 类型 | 索引 | 备注 |
|------|------|------|------|
| `is_reapply` | BOOLEAN | IDX | true=重新入驻申请 |
| `original_coach_id` | BIGINT FK | | 重新入驻时与 coach_id 相同，用于追溯 |

#### `coach_rating` / `review`（新增字段）

| 字段 | 类型 | 索引 | 备注 |
|------|------|------|------|
| `is_visible_to_new` | BOOLEAN | IDX | false=重新入驻后对新学员隐藏 |

### 3.2 读取表

- `coach`：读取当前状态与基础资料
- `coach_application`：写入并查询重新入驻申请
- `audit_log`：记录状态变更

---

## 4. API 设计

### 4.1 `POST /api/coach/v1/reapply`

- **鉴权**：教练 JWT，`coach.status = 3`
- **请求体**：`{ "idempotency_key": "..." }`
- **响应 200**：`{ "application_id": 1001, "status": 0, "message": "入驻申请已提交" }`
- **错误码**：`COACH_STATUS_NOT_ALLOWED`（403）、`REAPPLY_ALREADY_PENDING`（409）

### 4.2 `GET /api/coach/v1/reapply/status`

- **鉴权**：教练 JWT
- **功能**：返回当前教练最新的重新入驻申请状态
- **响应 200**：`{ "application_id": 1001, "status": "pending", "submitted_at": "..." }`

### 4.3 `POST /api/admin/v1/coaches/{coach_id}/reapply/approve`

- **鉴权**：管理员 JWT
- **功能**：通过重新入驻，`coach.status: 0 → 1`
- **响应 200**：`{ "message": "审核通过" }`
- **错误码**：`APPLICATION_NOT_PENDING`（409）

### 4.4 `POST /api/admin/v1/coaches/{coach_id}/reapply/reject`

- **鉴权**：管理员 JWT
- **请求体**：`{ "reason": "资料不完整" }`
- **功能**：拒绝重新入驻，`coach.status: 0 → 3`
- **响应 200**：`{ "message": "审核已拒绝" }`

---

## 5. 状态机

### 5.1 教练状态

- `3` 已离职 → `0` 待审核（发起重新入驻）
- `0` 待审核 → `1` 已通过（管理员通过）
- `0` 待审核 → `3` 已离职（管理员拒绝）

### 5.2 入驻申请状态

复用 `coach_application` 状态：pending → approved / rejected

---

## 6. 缓存策略

- `coach:status:{coach_id}` 在状态变更时失效
- `coach:profile:{coach_id}` 在审核通过后失效
- 重新入驻申请详情不缓存

---

## 7. 性能指标

- `POST /api/coach/v1/reapply` P99 < 300ms
- `GET /api/coach/v1/reapply/status` P99 < 150ms
- 管理员审核接口 P99 < 200ms

---

## 8. 安全

- 所有接口校验 JWT 身份
- 教练端接口仅允许 `coach.status = 3` 调用
- 管理员接口校验 `MANAGE_COACH` 权限
- 操作记录审计日志

---

## 9. 跨 US 依赖

- 依赖 US-039（教练申请离职）或 US-041（管理员处理教练离职）产生 `coach.status = 3`
- 依赖 US-011（管理员审核教练入驻资质）的 `coach_application` 表与审核流程
- 历史评分可见性影响 US-001（游客浏览教练列表与详情）和 US-012（教练管理个人主页）

---

## 10. 接口测试映射

| 场景 | 测试方法 | 层级 |
|------|---------|------|
| 已离职教练发起重新入驻 | `test_reapply_success` | 集成 |
| 管理员通过重新入驻 | `test_approve_reapply_success` | 集成 |
| 非已离职教练禁止重新入驻 | `test_not_resigned_cannot_reapply` | 集成 |
| 重复发起重新入驻 | `test_reapply_duplicate` | 集成 |
| 审核拒绝 | `test_reject_reapply_success` | 集成 |
| 并发发起幂等 | `test_reapply_idempotent` | 集成 |
| 历史评分对新学员隐藏 | `test_rating_hidden_for_new_students` | 单元/集成 |
