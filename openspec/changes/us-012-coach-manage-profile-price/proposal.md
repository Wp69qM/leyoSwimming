## Why

教练管理个人主页与参考单价是教练通过入驻审核（US-011）后维护自身展示信息与定价基准的核心功能。个人主页信息（任教年限、证书、简介、微信二维码、手机号等）直接影响学员端教练详情展示与信任转化；参考单价作为系统套餐定价和自定义套餐金额计算的基准，影响学员购课决策与订单金额。本 US 让已审核教练能够自主维护这些信息，支撑后续 US-017/US-020 套餐购买与 US-038 主页分享。

PRD [§5.4.1](../../../docs/prd/prd.md) 要求教练个人主页展示任教年限、证书、总课时、带过的学生总量、评分、评价、微信二维码、手机号，并允许教练设置一节课的参考单价；[§5.5.3](../../../docs/prd/prd.md) 明确实际订单金额可在审批时协商调整。

## What Changes

- 新增 `GET /api/coach/profile` 接口（获取教练个人主页信息）
- 新增 `PUT /api/coach/profile` 接口（更新个人主页资料）
- 新增 `PUT /api/coach/reference-price` 接口（更新参考单价）
- 复用 `POST /api/upload/image` 接口（上传证书/二维码图片）
- 修改 `coach` 表：更新 `bio`、`teaching_years`、`phone`、`qr_code_url`、`reference_price`、`price_changed_at`、`price_change_count_today`
- 新增/修改 `coach_certificate` 表：证书图片增删改
- 新增 `coach_update_log` 表：记录主页与单价变更历史
- 新增 Redis 缓存失效策略：教练详情缓存、教练列表缓存更新后立即失效
- 新增教练端「我的 → 个人主页」编辑页与「我的 → 参考单价」设置页
- 边界处理：参考单价 50-2000 元范围校验、每日最多 3 次改价限制、个人简介敏感词过滤、证书图片 5MB 限制

## Capabilities

### New Capabilities

- `coach-manage-profile-price`: 已审核教练维护个人主页信息（简介、任教年限、证书、微信二维码、手机号）与参考单价，包含范围校验、改价频率限制、敏感词过滤、图片大小限制与变更审计

### Modified Capabilities

- `coach-submit-application`: 教练入驻申请提交的个人资料字段与教练端主页编辑字段保持一致
- `admin-review-coach-application`: 管理员审核通过（US-011）后 coach.status = 1，才允许调用本 US 接口

## Impact

- **数据表**：修改 `coach`（新增/更新资料字段）；新增/修改 `coach_certificate`；新增 `coach_update_log`
- **API**：新增 3 个端点 `GET /api/coach/profile`、`PUT /api/coach/profile`、`PUT /api/coach/reference-price`（均需教练登录鉴权且 `coach.status = 1`）
- **缓存**：教练详情 `coach:{coach_id}` 与教练列表 `coach:list:*` 更新后立即失效
- **状态机**：不改变 `coach.status`，仅更新资料与价格字段
- **前端**：新增教练端 2 个页面（个人主页编辑页、参考单价设置页）
- **依赖**：依赖 US-011 管理员审核教练入驻资质；被 US-017/US-020 套餐定价、US-038 主页分享依赖
- **安全**：参考单价后端强校验防止绕过；敏感词过滤个人简介；操作记录审计日志；图片大小与格式校验
