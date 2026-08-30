## Why

管理员需要统一控制每周可约时段的释放时间、范围以及候补/关注策略，这是首页倒计时、自动释放任务、候补转正等下游功能的前置配置能力。当前系统缺少全局预约释放规则配置入口。

## What Changes

- 新增 `release_rule` 全局配置表（单行记录）
- 新增 `GET /api/admin/release-rule` 查询接口
- 新增 `PUT /api/admin/release-rule` 更新接口（需 `SCHEDULE_RELEASE_CONFIG` 权限）
- 新增 web-admin「预约释放规则配置」页面
- 新增 Redis 缓存 `release_rule:current`（TTL 60s），更新后立即失效
- 支持配置：释放星期、释放时间、释放范围、候补开关、候补有效期、关注提醒提前时间、节假日提前释放

## Capabilities

### New Capabilities

- `admin-config-release-rule`: 管理员查询与更新预约释放全局规则

### Modified Capabilities

（无）

## Impact

- **数据表**：新增 `release_rule`；写入 `admin_operation_log`
- **API**：新增 2 个管理端端点
- **缓存**：新增 Redis key `release_rule:current`
- **前端**：新增 web-admin 1 个页面
- **依赖**：被 US-016 / US-003 / US-023 依赖
