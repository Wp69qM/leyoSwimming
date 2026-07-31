## Why

已离职教练是平台的潜在优质资源，允许其重新入驻可降低教练流失成本。同时需要避免历史差评或旧数据误导新学员，因此重新入驻必须走完整审核流程，并隔离历史评价对新学员的可见性。

## What Changes

- 教练端为 `coach.status = 3` 的教练展示「重新入驻」入口
- 教练发起重新入驻后，`coach.status` 从 `3` 变为 `0`（待审核）
- 系统在 `coach_application` 中生成标记为 `is_reapply = true` 的审核记录
- 管理员可在后台通过或拒绝重新入驻申请
- 审核通过后 `coach.status` 变为 `1`，历史评分仅对老学员可见
- 审核拒绝后 `coach.status` 恢复 `3`，教练可再次申请

## Capabilities

### New Capabilities

- `coach-reapply-entry`: 已离职教练重新发起入驻申请并通过管理员审核恢复教学资格

### Modified Capabilities

- `admin-review-coach-application`（US-011）：新增处理 `is_reapply = true` 的重新入驻申请分支

## Impact

- 后端：扩展 `coach_application` 表（`is_reapply`）、扩展 `coach_rating`/`review` 表（`is_visible_to_new`），新增 `/api/coach/v1/reapply` 与 `/api/admin/v1/coaches/{id}/reapply/*` 接口
- 教练端小程序：新增「重新入驻」入口、资料确认页、审核状态页
- 管理端：在入驻审核队列中区分新入驻与重新入驻
- 依赖：US-041（管理员处理教练离职）审批通过后产生 `coach.status = 3` 方可重新入驻；US-011（管理员审核教练入驻资质）提供入驻审核流程；US-039 通过 US-041 间接产生 status=3
