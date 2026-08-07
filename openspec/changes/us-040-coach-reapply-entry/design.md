# Design: US-040 教练重新入驻

> 本文档对应 `docs/stories/US-040-教练-重新入驻/tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-040 让已离职教练（`coach.status = 3`）可重新发起入驻申请。核心约束：

- **不复用独立的重新入驻资料填写页**：已离职教练登录后由 US-051 / US-054 直接跳转 US-010 的 C-入驻资料填写页。
- **实际资料提交由 US-010 处理**：字段校验、图片上传、`coach.status` 从 3 更新为 0 等逻辑全部由 US-010 的 `POST /api/coach/application` 与 `PUT /api/coach/application/draft` 完成；US-010 创建 `previous_coach_status=3` 的 `coach_application` pending 快照。
- **历史数据不隔离**：复用原 `coach` 记录，不回滚历史数据；历史评分/评价保留并对新老学员均可见。
- **管理员重新入驻审核复用 US-011**：通过/拒绝重新入驻申请，触发 `coach.status` 0 → 1 或 0 → 3 的转换。

`coach.status = 3` 必须由 US-041 管理员审批教练离职通过后产生；US-039 教练申请离职需经 US-041 审批后才能进入本 US。

## Goals / Non-Goals

**Goals:**

- 仅 `status = 3` 的教练可发起重新入驻
- 已离职教练登录后由 US-051 / US-054 直接跳转 US-010 的 C-入驻资料填写页
- 管理员可通过/拒绝重新入驻申请（复用 US-011）
- 历史评分/评价保留并对新老学员均可见

**Non-Goals:**

- 不新建独立的重新入驻资料填写页
- 不将历史数据与新申请隔离
- 不自动解冻老学员的 frozen package
- 不删除或修改历史评价内容
- 不修改 US-010 新教练入驻的核心字段校验逻辑
- 不新增 `coach_application.is_reapply` 字段（使用 `previous_coach_status=3` 标识重新入驻）

## Data Model

### 修改表

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
| `rejection_reason` | VARCHAR(512) | — | 驳回原因；**不冗余到 coach 表** |

> 说明：每次提交（含首次、驳回后重新提交、重新入驻）均新增一条记录，`coach_id` 不变，不隔离历史数据。`previous_coach_status=3` 表示重新入驻申请。

### 读取表

- `coach`：读取当前状态与基础资料；重新入驻时复用原记录
- `coach_application`：读取重新入驻申请记录与驳回原因
- `coach_audit_log`：记录状态变更

## API Design

> 教练端不新增重新入驻入口 API。已离职教练的入口由 US-051 / US-054 登录响应中的 `coach_status = 3` 自动分流提供；实际资料填写、校验与提交复用 US-010 的接口。

### POST /api/admin/coach/applications/{application_id}/approve

- **鉴权**：管理员 JWT + `coach:audit` 权限
- **功能**：复用 US-011；通过重新入驻，`coach_application` 快照覆盖 coach 表，`coach.status: 0 → 1`
- **响应 200**：`{ "coach_id": 20001, "application_id": 10002, "status": 1, "approved_at": "..." }`
- **错误码**：`NOT_PENDING`（409）

### POST /api/admin/coach/applications/{application_id}/reject

- **鉴权**：管理员 JWT + `coach:audit` 权限
- **请求体**：`{ "reason": "资料不完整" }`
- **功能**：复用 US-011；拒绝重新入驻，`coach_application.status = rejected`，`coach.status: 0 → 3`，coach 表生效资料保持不变
- **响应 200**：`{ "coach_id": 20001, "application_id": 10002, "status": 3, "rejection_reason": "资料不完整" }`

> 说明：通过/拒绝接口均复用 US-011 通用审核接口，US-040 不再新建独立管理员接口。

## State Machine

### 教练状态

```
3 已离职 ──[在 US-010 提交重新入驻资料]──→ 0 待审核
0 待审核 ──[管理员通过，US-011]──────────→ 1 已通过
0 待审核 ──[管理员拒绝，US-011]──────────→ 3 已离职
```

- `3 → 0` 的转换由 US-010 的提交接口触发，并创建 `previous_coach_status=3` 的 `coach_application` pending 快照。
- `0 → 1` 与 `0 → 3` 的转换由 US-011 的审核接口触发。
- US-040 仅通过 US-051 / US-054 的登录响应提供前端跳转提示，不直接修改 coach.status。

### 入驻申请状态（coach_application.status）

复用 US-010 状态机：`draft → pending → approved / rejected`；每次提交新增一条记录，`coach_id` 不变，`previous_coach_status=3` 表示重新入驻。

## Caching

- `coach:status:{coach_id}` 在状态变更时失效
- `coach:profile:{coach_id}` 在审核通过后失效
- `coach:application:{openid}` 在提交/审核后失效
- 重新入驻入口校验不缓存

## Performance Targets

| 指标 | 目标 |
|------|------|
| 登录状态分流由 US-051 / US-054 返回 coach_status，不产生额外接口调用 |
| 复用的 US-011 审核接口 P99 | < 200ms |

## Security

- 所有接口校验 JWT 身份
- 管理员接口校验 `coach:audit` 权限
- 操作记录审计日志
- US-010 的提交接口需校验 `coach.status = 3` 时才允许创建 `previous_coach_status=3` 的重新入驻快照

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-041 | 依赖 | 审批通过后产生 `coach.status = 3`；US-039 通过 US-041 间接产生 status=3 |
| US-010 | 依赖 | 处理重新入驻的资料填写、校验与提交；触发 `3 → 0` 转换 |
| US-011 | 依赖 | 提供/复用管理员审核流程；触发 `0 → 1` / `0 → 3` 转换 |
| US-051 / US-054 | 依赖 | 提供教练端登录态与 `coach_status` 分流 |
| US-001 / US-012 | 被依赖 | 历史评分可见性影响教练列表/详情与教练主页 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Context / Goals | `docs/stories/US-040-.../user-story.md` §1-§5 |
| Data Model | `docs/stories/US-040-.../tech-design.md` §3 |
| API Design | `docs/stories/US-040-.../tech-design.md` §4 |
| State Machine | `docs/stories/US-040-.../tech-design.md` §5 |
| Caching / Performance / Security | `docs/stories/US-040-.../tech-design.md` §6-§8 |
