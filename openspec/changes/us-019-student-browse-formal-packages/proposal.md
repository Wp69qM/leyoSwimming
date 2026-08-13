## Why

正价套餐浏览是用户购买决策的前置入口。用户（学员/游客）需在购买前查看教练的标准套餐选项（1/6/8/10 节）与自定义课时入口，才能跳转 US-020 完成下单。本 US 为 US-020 的前置依赖。

## What Changes

- 新增 `POST /api/packages/list` 查询全局已上架套餐列表（首页/全部套餐入口）
- 新增 `POST /api/coach/packages/list` 查询指定教练的套餐选项（教练详情页入口）
- 新增 `POST /api/packages/detail` 查询单个套餐模板详情，返回适配教练列表（全局入口）或当前教练信息（教练详情页入口）
- 新增标准套餐/体验课/自定义套餐读取（管理员配置）
- 新增教练参考单价读取与自定义课时入口开关
- 新增教练状态可见性校验（仅 `coach.status = 1` 可浏览）
- 新增标准套餐模板过滤（`package_template.status = active`）
- 新增当前教练可约性校验（未离职、未冻结）
- 新增 5 分钟缓存与限流防护

## Capabilities

### New Capabilities

- `student-browse-formal-packages`: 学员/游客浏览正价套餐选项，支持全局套餐列表与教练详情页两个入口

### Modified Capabilities

（无）

## Impact

- **数据表**：读 `coach`、`package_template`、`coach_package_template`（或等效关联表）
- **API**：新增 3 个端点（游客可访问，无需登录鉴权）
- **校验**：教练状态可见性、参考单价是否设置、标准套餐启用状态、套餐模式过滤
- **缓存**：新增教练套餐列表缓存 TTL 5 分钟
- **依赖**：依赖 US-011 / US-012 / US-045；被 US-020 依赖
