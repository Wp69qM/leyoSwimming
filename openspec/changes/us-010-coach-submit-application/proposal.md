## Why

教练提交入驻资料是教练端可用前提，也是教练服务上线的起点。通过收集证书、任教年限、总学员数、总课时数、个人简介与参考单价，平台可对教练资质进行审核，为学员提供可信赖的教练资源。

PRD [§5.4.1](../../../docs/prd/prd.md) 要求首次注册需上传证书、任教年限、总学员数、总课时数、个人简介，教练可设置一节课的参考单价；[§5.5.1](../../../docs/prd/prd.md) 要求入驻资质需管理员审核。

## What Changes

- 新增 `POST /api/coach/application` 接口（提交入驻资料）
- 新增 `PUT /api/coach/application/draft` 接口（保存草稿，保存后 `coach.status` 同样为 0，不新增独立草稿态）
- 新增 `GET /api/coach/application` 接口（查询当前申请状态）
- 新增 `POST /api/upload/image` 接口（证书图片上传）
- 新增 `coach` 表（教练基本信息、资质、参考单价、状态；含 `submitted_at` 区分草稿与已提交）
- 新增 `coach_certificate` 表（证书图片存储）
- 新增 `coach_audit_log` 表（记录入驻申请提交）
- 新增 Redis 缓存：`coach:application:{user_id}`、`upload:temp:{file_key}`
- 新增教练小程序"入驻资料填写页"与"提交成功页"
- 触发教练状态机：**无 → 待审核(0)** 与 **驳回(2) → 待审核(0)**（提交审核与保存草稿均进入待审核态；已驳回教练可修改后重新提交）
- 边界处理：必填项缺失、图片过大、参考单价超出范围、重复提交、已驳回重新提交

## Capabilities

### New Capabilities

- `coach-submit-application`: 教练提交入驻资料，包含证书上传、字段校验、参考单价范围校验、草稿保存、重复提交拦截与教练状态机初始转换

### Modified Capabilities

- `upload-image`: 新增证书图片上传场景

## Impact

- **数据表**：新增 `coach`；新增 `coach_certificate`；新增 `coach_audit_log`
- **API**：新增 4 个端点（提交、草稿、状态查询、图片上传）
- **缓存**：新增 Redis key `coach:application:{user_id}`（TTL 5min）、`upload:temp:{file_key}`（TTL 1h）
- **状态机**：触发教练状态机 `无 → 待审核(0)` 与 `驳回(2) → 待审核(0)`
- **前端**：新增教练小程序"入驻资料填写页"、"提交成功页"
- **依赖**：无前置 US；被 US-011、US-012、US-013、US-014 依赖
- **安全**：图片上传校验格式与大小；防重复提交；敏感字段后端校验
