## Why

游客通过浏览教练信息了解游泳馆师资，是首次接触产品、形成购买决策的关键路径。当前系统已有教练入驻流程（US-010~US-013），但游客端缺少公开的教练列表与详情展示页面，无法将教练资源转化为获客入口。

## What Changes

- 新增 `GET /api/v1/coaches` 公开列表接口（无需登录，按综合评分降序 + 分页）
- 新增 `GET /api/v1/coaches/:id` 公开详情接口（含证书、评价、可约时间、实时状态）
- 新增微信小程序「找教练」列表页（支持空状态、下拉刷新、无限滚动）
- 新增微信小程序教练详情页（含实时状态徽标、"立即预约"按钮状态显隐）
- 新增 Redis 缓存层（列表页 TTL 60s / 详情页 TTL 300s）
- 公开可见的教练状态为 `status=1`（已通过）和 `status=4`（申请离职中）；`status=0`（待审核）、`status=2`（驳回）、`status=3`（已离职）均不公开
- `status=4` 的教练在列表/详情中不得展示"申请离职中"等任何状态标签

## Capabilities

### New Capabilities

- `coach-browsing`: 游客浏览教练列表与详情，包含公开列表查询、详情查询、实时状态展示、空状态处理

### Modified Capabilities

（无——这是首个 US，不修改已有 spec）

## Impact

- **数据表**：读取 `coach`（主表）、`coach_certificate`、`coach_review`、`coach_availability`；不修改任何表
- **API**：新增 2 个公开只读端点
- **缓存**：新增 Redis key `coaches:list:page:*` 和 `coach:detail:*`
- **前端**：新增小程序 2 个页面（`coaches/index` + `coach-detail/index`）
- **依赖**：依赖 US-010~US-013（教练入驻流程）产出的数据；被 US-017/US-018/US-029 依赖
