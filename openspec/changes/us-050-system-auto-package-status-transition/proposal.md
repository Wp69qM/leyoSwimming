## Why

leyoSwimming 的套餐状态（active / exhausted / expired）是核心业务契约，直接影响学员能否继续预约、上课记录是否有效。MVP 要求系统在套餐过期或课时耗尽时自动转换状态，避免人工介入导致的状态不一致与客服纠纷。当前缺少定时巡检与事件驱动的状态自动转换能力，依赖人工或业务代码零散判断，容易出现 expired/exhausted 状态滞后。

## What Changes

- 新增 `package_status_log` 表，记录每次状态转换审计日志
- 新增 `cron_job_lock` 表，支持分布式定时任务锁
- 新增 `ExpirePackageCronJob` 每小时巡检过期套餐并原子更新为 `expired`
- 新增 `PackageStatusReconciler` 事件处理器，在教练确认上课、学员取消预约后重算状态
- 新增 Redis 缓存失效策略：状态变更时删除 `package:{id}` 与 `user:{user_id}:packages`
- 明确 `active → expired`、`active → exhausted` 两条状态转换及不变量校验

## Capabilities

### New Capabilities

- `system-auto-package-status-transition`: 系统自动处理套餐过期与课时耗尽状态转换

### Modified Capabilities

（无——本 US 只新增系统自动化能力）

## Impact

- **数据表**：新增 `package_status_log`、`cron_job_lock`；修改 `package`（状态与计数更新）
- **API**：无外部 API，仅新增内部定时任务与事件处理器
- **缓存**：新增 Redis key 失效策略 `package:{id}`、`user:{user_id}:packages`
- **前端**：无独立页面，状态变化体现在「我的套餐」列表（US-021）
- **依赖**：依赖 US-020/US-029/US-032/US-033 产生的 package 与上课事件；被 US-021/US-030 依赖
