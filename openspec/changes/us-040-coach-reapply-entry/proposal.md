## Why

已离职教练是平台的潜在优质资源，允许其重新入驻可降低教练流失成本。本 US 让 `coach.status = 3`（已离职）的教练重新发起入驻申请并恢复教学资格；实际资料填写、校验与提交全部复用 US-010 的 C-入驻资料填写页与接口，历史数据保留在原有 coach 记录下，不隔离、不回滚。

PRD [§5.4.8](../../../docs/prd/prd.md) 要求状态 3（已离职）的教练可重新申请入驻；重新申请时 `coach.status: 3 → 0`，实际资料填写由 US-010 统一处理。本 US 在 US-041（管理员处理教练离职）产生 `status = 3` 后方可触发。

## What Changes

- 已离职教练登录教练端时，US-051 / US-054 按 `coach_status = 3` 直接跳转 US-010 的 C-入驻资料填写页（顶部展示重新入驻说明条）
- 实际资料填写、字段校验、图片上传、提交审核全部由 US-010 的 `POST /api/coach/application` 与 `PUT /api/coach/application/draft` 处理；US-010 创建 `previous_coach_status=3` 的 `coach_application` pending 快照
- 管理员重新入驻审核复用 US-011 接口：`POST /api/admin/coach/applications/{application_id}/approve` 与 `/reject`；通过时快照覆盖 coach 生效资料，`coach.status: 0 → 1`；拒绝时 `coach.status` 恢复为 3
- 历史评分/评价保留，并对新老学员均可见
- 已 frozen 的老学员套餐不自动恢复为 active，等待老学员主动换回原教练或退款
- 边界处理：非已离职教练禁止重新入驻、重复发起、已离职重新入驻审核驳回后状态恢复为 3

## Capabilities

### New Capabilities

- `coach-reapply-entry`: 已离职教练重新发起入驻申请并恢复教学资格，包含登录后自动分流、历史评分对全体学员可见

### Modified Capabilities

- `coach-submit-application`（US-010）：新增 `status = 3`（已离职）教练重新入驻提交资料的场景；C-入驻资料填写页新增重新入驻说明条与历史数据回显
- `admin-review-coach-application`（US-011）：新增处理 `previous_coach_status=3` 的重新入驻申请分支，并在审核视图中标记为"重新入驻"

## Impact

- **数据表**：`coach`（`status` 3 → 0 → 1 或 3 → 0 → 3）；`coach_application`（`previous_coach_status=3` 的 pending/rejected 快照）
- **API**：复用 US-010 提交/草稿接口；复用 US-011 审核接口
- **状态机**：触发 `coach.status` 3 → 0（由 US-010 提交接口触发）、0 → 1（US-011 通过）、0 → 3（US-011 拒绝）
- **前端**：US-051 / US-054 登录响应按 coach_status=3 自动跳转 US-010 C-入驻资料填写页；US-010 C-入驻资料填写页新增 status=3 重新入驻说明条
- **依赖**：依赖 US-041 产生 `coach.status = 3`；依赖 US-010 处理资料提交；依赖 US-011 提供审核流程；US-039 通过 US-041 间接产生 status=3
- **安全**：所有接口校验 JWT 身份；US-010 提交接口需校验 `coach.status = 3` 时才允许创建 `previous_coach_status=3` 的重新入驻快照；管理员接口校验 `coach:audit` 权限；操作记录审计日志
