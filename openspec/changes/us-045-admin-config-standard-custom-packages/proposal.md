## Why

正价套餐是 leyoSwimming 核心收入来源。MVP 要求管理员能在后台预设标准套餐模板，并允许学员购买时按后台配置的规则自由选择课时。当前缺少后台套餐配置能力，导致套餐信息无法运营维护，学员端无套餐可浏览、可购买。

## What Changes

- 新增 `package_template` 表，存储标准套餐模板（名称、教练、课时、有效期、售价、上下架状态）
- 新增 `custom_package_config` 表，存储自定义套餐全局规则（可选课时范围、默认有效期）
- 新增 4 个管理员接口：列表、新增、编辑、上下架切换
- 新增 Redis 缓存层，模板变更主动失效
- 前端 web-admin 新增「套餐配置」页面
- 套餐模板状态 `active/inactive` 仅影响新购可见性，不影响已购 package

## Capabilities

### New Capabilities

- `admin-config-standard-custom-packages`: 管理员配置标准套餐模板与自定义套餐规则

### Modified Capabilities

（无——本 US 只新增后台配置能力）

## Impact

- **数据表**：新增 `package_template`、`custom_package_config`；读取 `coach`
- **API**：新增 4 个管理员接口
- **缓存**：新增 Redis key `package:templates:active`、`package:template:{id}`
- **前端**：新增 web-admin「套餐配置」页面
- **依赖**：依赖 US-010/US-011 产出的教练数据；被 US-019/US-020 依赖
