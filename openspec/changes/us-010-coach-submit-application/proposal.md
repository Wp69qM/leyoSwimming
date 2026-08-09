## Why

教练提交入驻资料是教练端可用前提，也是教练服务上线的起点。通过收集微信基础信息、实名资质（身份证、身份证正反面、教练资格证、健康证、个人形象照）、教学履历（任教年限、总学员数、总课时数、擅长泳姿、个人简介）与服务设置（参考单价），平台可对教练资质进行审核，为学员提供可信赖的教练资源。

PRD [§5.4.1](../../../docs/prd/prd.md) 要求首次注册需上传证书、任教年限、总学员数、总课时数、个人简介，教练可设置一节课的参考单价；[§5.5.1](../../../docs/prd/prd.md) 要求入驻资质需管理员审核。字段设计进一步明确了身份证、各类资质照片、参考单价范围与 `coach.status = -1` 的默认未提交态。教练进入本 US 之前需先完成 US-051 教练端微信授权登录或 US-054 手机号验证码登录，以及 US-009 隐私协议授权。

## What Changes

- 新增 `POST /api/coach/application/submit` 接口（提交入驻资料，请求体含全部字段及 `certificates: [{cert_type, image_url}]`）
- 新增 `POST /api/coach/application/save-draft` 接口（保存草稿，数据写入 `coach_application` 快照，`status = draft`，`coach.status` 保持 `-1/2/3` 不变）
- 新增 `POST /api/coach/application/detail` 接口（查询当前最新 `coach_application` 快照完整字段 + 证书列表，返回 `entry_type` 与 `prompt_message`）
- 新增 `POST /api/common/file/upload` 接口（通用图片上传，JPG/PNG，≤5MB）
- 新增 `coach_application` 快照表（与 `coach` 资料字段同构，含 `status` draft/pending/approved/rejected、`previous_coach_status`）
- 新增 `coach_certificate_application` 快照证书记录表
- 新增 `coach_audit_log` 表记录（submit/approve/reject/draft_save）
- 明确 `coach` 表完整字段作为**生效资料**主表；pending 期间生效资料保持不变
- 新增 Redis 缓存：`coach:application:{openid}`、`upload:temp:{file_key}`
- 新增教练小程序三个页面：C-教练端入驻资料填写页、C-教练端入驻提交成功页、C-教练端等待审核页
- 触发教练状态机：`-1 → 0`、`2 → 0` 与 `3 → 0`（提交审核时）；保存草稿不触发 `coach.status` 转换
- 明确错误码：`COACH_APPLICATION_PENDING`、`INVALID_REFERENCE_PRICE`、`MISSING_REQUIRED_FIELDS`、`INVALID_ID_CARD`、`IMAGE_TOO_LARGE`、`INVALID_IMAGE_FORMAT`
- 边界处理：必填项缺失、身份证号非法、必填资质缺失、图片过大/格式错误、参考单价超出范围、重复提交、已驳回重新提交、已离职重新入驻、等待审核页查看资料

## Capabilities

### New Capabilities

- `coach-submit-application`: 教练提交入驻资料，包含基础信息（头像/姓名/手机号/性别/年龄/邮箱/微信二维码）、实名与资质、教学履历、服务设置、证书上传、字段校验、参考单价范围校验、草稿保存、重复提交拦截、已驳回/已离职重新提交与教练状态机初始转换

### Modified Capabilities

- `upload-image`: 新增证书/身份证/形象照等图片上传场景

## Impact

- **数据表**：新增 `coach_application` 快照表；新增 `coach_certificate_application` 快照证书记录表；明确 `coach`（生效资料 + 生命周期）、`coach_certificate`（生效证书）、`coach_audit_log`
- **API**：新增 4 个端点（提交、草稿、状态查询、图片上传）
- **缓存**：新增 Redis key `coach:application:{openid}`（TTL 5min）、`upload:temp:{file_key}`（TTL 1h）
- **状态机**：触发教练状态机 `-1 → 0`、`2 → 0` 与 `3 → 0`；`coach_application` 快照状态机 `draft → pending → approved/rejected`
- **前端**：新增教练小程序「入驻资料填写页」「入驻提交成功页」「等待审核页」；C-入驻资料填写页需支持 status=2 驳回原因条、status=3 重新入驻说明条与历史数据回显
- **依赖**：前置 US-051（教练端微信授权登录）、US-054（教练手机号验证码登录）与 US-009（隐私协议授权）；被 US-011、US-012、US-013、US-014、US-040 依赖
- **安全**：身份证 AES 加密；图片上传校验格式与大小；防重复提交；敏感字段后端校验；参考单价后端校验
