## Why

正价套餐浏览是用户购买决策的前置入口。用户（学员/游客）需在购买前查看教练的标准套餐选项（1/6/8/10 节）与自定义课时入口，才能跳转 US-020 完成下单。本 US 为 US-020 的前置依赖。

## What Changes

- 新增 `GET /api/coaches/{id}/packages` 查询教练正价套餐选项
- 新增标准套餐列表读取（管理员配置）
- 新增教练参考单价读取与自定义课时入口开关
- 新增教练状态可见性校验（仅 status IN (1, 4) 可浏览）
- 新增 5 分钟缓存与限流防护

## Capabilities

### New Capabilities

- `student-browse-formal-packages`: 学员/游客浏览教练的正价套餐选项

### Modified Capabilities

（无）

## Impact

- **数据表**：读 `coach`、`standard_package`
- **API**：新增 1 个端点（游客可访问，无需登录鉴权）
- **校验**：教练状态可见性、参考单价是否设置、标准套餐启用状态
- **缓存**：新增 `coach:{id}:packages` TTL 5 分钟
- **依赖**：依赖 US-011 / US-012 / US-045；被 US-020 依赖
