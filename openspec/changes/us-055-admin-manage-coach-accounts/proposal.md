## Why

管理后台需要统一的教练账号管理能力，以支持管理员查看、维护教练资料，处理教练入职（新建）与离职（取消入驻）。缺少该能力会导致教练信息维护依赖教练端自助提交，无法及时响应运营需求。

## What Changes

- 管理后台新增「用户管理 → 教练管理」模块
- 管理员可按教练状态、实时状态、关键词筛选并查看教练列表
- 教练列表展示：教练 ID、姓名、性别、年龄、教学年限、擅长泳姿、创建时间、在职状态、实时状态
- 教练详情页展示完整生效资料：基础信息、实名与资质、教学履历、服务设置、证书列表及历史申请/操作记录
- 教练编辑弹窗支持管理员编辑全部入驻字段（包括实名与资质），无需重新审核
- 管理员可「新建教练」，填写与教练端入驻一致的字段，提交后直接 status=1（已通过）
- 管理员可「取消入驻」，填写原因后 coach.status 更新为 3（已离职），并触发 US-041 离职后续处理
- 所有敏感操作写入 `coach_audit_log`
- RBAC 权限细化：`COACH:READ` / `COACH:WRITE` / `COACH:CANCEL_ENTRY`

## Capabilities

### New Capabilities

- `admin-manage-coach-accounts`: 管理员查询、新建、编辑、查看教练账号及取消入驻

### Modified Capabilities

- 无

## Impact

- 后端：新增 `POST /api/admin/coach/list` / `detail` / `add` / `update` / `cancelEntry` 系列接口，参数通过 JSON body 传递，遵循 [api-convention.md](../../../docs/tech/api-convention.md)
- 管理端 Web：新增/复用教练管理列表页、教练详情页、教练编辑弹窗；字段与 US-010 教练入驻字段对齐
- 依赖：US-010（入驻字段）、US-011（审核详情字段展示）、US-053（管理员登录）、US-041（取消入驻后离职处理）
- 影响：为所有需要查询/维护教练资料的管理后台 US 提供统一入口
