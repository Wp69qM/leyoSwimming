# US-040 教练重新入驻技术设计

---

## 1. 上下文

本 US 让已离职教练（`coach.status = 3`）可重新发起入驻申请。已离职教练登录教练端时，US-051 / US-054 按 `coach_status = 3` 直接跳转 US-010 的 C-入驻资料填写页；由 US-010 处理资料校验与提交，创建新的 `coach_application` pending 快照（`previous_coach_status = 3`），`coach.status` 从 3 变为 0（待审核）。审核通过时快照字段覆盖 coach 表生效资料；审核驳回时 `coach.status` 恢复为 3，coach 表生效资料保持不变。历史评分/评价保留并对新老学员均可见，已 frozen 的老学员套餐不自动恢复。

---

## 2. 目标 / 非目标

**目标：**
- 仅 `status = 3` 的教练可发起重新入驻
- 已离职教练登录后由 US-051 / US-054 直接跳转 US-010 的 C-入驻资料填写页；由 US-010 处理提交后创建 `coach_application` pending 快照，`coach.status` 从 3 变为 0
- 管理员可通过/拒绝重新入驻申请；通过时快照覆盖 coach 生效资料，拒绝时 coach.status 恢复为 3
- 历史数据保留在原有 coach 记录下，不回滚、不隔离
- 历史评分/评价保留，新老学员均可见

**非目标：**
- 不新建独立的重新入驻资料填写页
- 不将历史数据与新申请隔离
- 不自动解冻老学员的 frozen package
- 不删除或修改历史评价内容

---

## 3. 数据模型

### 3.1 修改表

#### `coach_application`（入驻/重新入驻/编辑申请快照表）

| 字段 | 类型 | 索引 | 备注 |
|------|------|------|------|
| `application_id` | BIGINT | PK | 申请快照 ID |
| `coach_id` | BIGINT | FK | 教练 ID |
| `status` | ENUM | IDX | draft / pending / approved / rejected |
| `previous_coach_status` | TINYINT | IDX | 提交前 coach.status：-1/2/3；重新入驻时为 3 |
| 资料字段 | — | — | 与 coach 表资料字段同构，作为快照 |
| `submitted_at` | DATETIME | — | 正式提交时间 |
| `approved_at` | DATETIME | — | 通过时间 |
| `approved_by` | BIGINT | — | 审核管理员 ID |
| `rejection_reason` | VARCHAR(512) | — | 驳回原因 |

> 说明：每次提交（含首次、驳回后重新提交、重新入驻）均新增一条记录，`coach_id` 不变，不隔离历史数据。`previous_coach_status=3` 表示重新入驻申请。

### 3.2 读取表

- `coach`：读取当前状态与基础资料；重新入驻时复用原记录
- `coach_application`：如保留，则写入并查询重新入驻申请记录
- `audit_log`：记录状态变更

---

## 4. API 设计

> 教练端不新增重新入驻入口 API。已离职教练的入口由 US-051 / US-054 登录响应中的 `coach_status = 3` 自动分流提供；实际资料填写、校验与提交复用 US-010 的接口。

### 4.1 `POST /api/admin/coach/application/approve`

- **鉴权**：管理员 JWT
- **请求体**：`{ "applicationId": 10002 }`
- **功能**：复用 US-011；通过重新入驻，`coach_application` 快照覆盖 coach 表，`coach.status: 0 → 1`
- **响应 200**：`{ "coach_id": 20001, "application_id": 10002, "status": 1, "approved_at": "..." }`
- **错误码**：`APPLICATION_NOT_PENDING`（409）

### 4.2 `POST /api/admin/coach/application/reject`

- **鉴权**：管理员 JWT
- **请求体**：`{ "applicationId": 10002, "reason": "资料不完整" }`
- **功能**：复用 US-011；拒绝重新入驻，`coach_application.status = rejected`，`coach.status: 0 → 3`
- **响应 200**：`{ "coach_id": 20001, "application_id": 10002, "status": 3, "rejection_reason": "资料不完整" }`

---

## 5. 状态机

### 5.1 教练状态（coach.status）

- `3` 已离职 → `0` 待审核（在 US-010 提交重新入驻资料，创建 previous_coach_status=3 的 pending application）
- `0` 待审核 → `1` 已通过（管理员通过；application 快照覆盖 coach 生效资料）
- `0` 待审核 → `3` 已离职（管理员拒绝；coach.status 恢复为 previous_coach_status=3）

### 5.2 入驻申请状态（coach_application.status）

- `draft` 草稿 → `pending` 待审核（提交审核）
- `pending` 待审核 → `approved` 已通过（管理员通过）
- `pending` 待审核 → `rejected` 已驳回（管理员拒绝）
- 每次提交新增一条记录，`coach_id` 不变，`previous_coach_status=3` 表示重新入驻。

---

## 6. 缓存策略

- `coach:status:{coach_id}` 在状态变更时失效
- `coach:profile:{coach_id}` 在审核通过后失效
- 重新入驻申请详情不缓存

---

## 7. 性能指标

- 登录状态分流由 US-051 / US-054 返回 coach_status，不产生额外接口调用
- 管理员审核接口 P99 < 200ms

---

## 8. 安全

- 所有接口校验 JWT 身份
- 管理员接口校验 `MANAGE_COACH` 权限
- 操作记录审计日志
- US-010 的提交接口需校验 `coach.status = 3` 时才允许创建 `previous_coach_status=3` 的重新入驻快照

---

## 9. 跨 US 依赖

- 依赖 US-041（管理员处理教练离职）审批通过后产生 `coach.status = 3`；US-039 通过 US-041 间接产生 status=3
- 依赖 US-010（教练提交入驻资料）处理重新入驻的资料填写、校验与提交
- 依赖 US-011（管理员审核教练入驻资质）的审核流程
- 历史评分可见性影响 US-001（游客浏览教练列表与详情）和 US-012（教练管理个人主页）

---

## 10. 接口测试映射

| 场景 | 测试方法 | 层级 |
|------|---------|------|
| 已离职教练登录后自动进入 US-010 重新入驻 | `test_reapply_login_redirect` | 集成 |
| 管理员通过重新入驻 | `test_approve_reapply_success` | 集成 |
| 非已离职教练登录后不按重新入驻分流 | `test_not_resigned_no_redirect` | 集成 |
| 重复发起重新入驻 | `test_reapply_duplicate` | 集成 |
| 审核拒绝 | `test_reject_reapply_success` | 集成 |
| 并发提交幂等 | `test_reapply_idempotent` | 集成 |
| 历史评分对新学员可见 | `test_rating_visible_for_new_students` | 单元/集成 |
