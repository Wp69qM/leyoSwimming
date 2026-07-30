## Why

管理员审核教练入驻资质是保证教练资质合规、控制教练质量的关键关卡，也是教练上线服务的前置条件。通过审核机制，平台可确保持证教练符合服务标准，维护学员权益。

PRD [§5.4.1](../../../docs/prd/prd.md) 要求入驻资质需管理员审核；[§5.5.1](../../../docs/prd/prd.md) 要求管理员审核教练入驻资质，教练状态 0=待审核 1=已通过 2=驳回。

## What Changes

- 新增 `GET /api/admin/coach/applications` 接口（待审核列表）
- 新增 `POST /api/admin/coach/applications/{id}/approve` 接口（审核通过）
- 新增 `POST /api/admin/coach/applications/{id}/reject` 接口（审核驳回）
- 修改 `coach` 表：`status`、`approved_at`、`rejection_reason`、`auditor_id`
- 新增 `coach_audit_log` 表记录审核结果
- 新增 `notification` 表发送审核通知给教练
- 新增 Redis 缓存：`admin:coach:applications`、`coach:{coach_id}`
- 新增 Web 管理后台"教练审核列表页"与"审核详情页"
- 触发教练状态机：**待审核(0) → 已通过(1) / 驳回(2)**
- 边界处理：无权限审核、重复审核、审核过程中教练撤回

## Capabilities

### New Capabilities

- `admin-review-coach-application`: 管理员查看待审核教练列表、查看详情、通过/驳回审核，系统更新教练状态、发送通知、记录审计日志

### Modified Capabilities

- `coach-submit-application`: 审核后改变 coach.status

## Impact

- **数据表**：修改 `coach`；新增 `coach_audit_log`；新增 `notification`
- **API**：新增 3 个端点（待审核列表、通过、驳回）
- **缓存**：新增 Redis key `admin:coach:applications`（TTL 1min）；审核后清除 `coach:{coach_id}`
- **状态机**：触发教练状态机 `待审核(0) → 已通过(1)` 或 `待审核(0) → 驳回(2)`
- **前端**：新增 Web 管理后台"教练审核列表页"、"审核详情页"
- **依赖**：依赖 US-010 产生待审核数据；被 US-012、US-013、US-014 依赖
- **安全**：RBAC 权限校验（coach:audit）；状态机校验；记录审计日志；异步发送通知
