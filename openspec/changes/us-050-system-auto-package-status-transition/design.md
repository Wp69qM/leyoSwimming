# Design: US-050 系统自动处理套餐过期与课时耗尽状态转换

> 本文档对应 `docs/stories/US-050-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-050 是系统后台自动化 US，核心新增 `package_status_log` 与 `cron_job_lock` 两张表，提供每小时运行的过期巡检任务与事件驱动的状态重算器，确保套餐状态与课时/有效期强一致。

## Data Model

### 新增表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `package_status_log` | 状态转换审计日志 | package_id, from_status, to_status, reason, created_at |
| `cron_job_lock` | 定时任务分布式锁 | job_name, hour_bucket, locked_at, owner |

### 读取/修改表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `package` | 状态与计数更新 | status, available, reserved, consumed, expire_at |
| `course_record` | 教练确认上课触发计数变化（v3 评审 P1 修复：表名与 US-032/US-033 统一） | id, package_id, status |

### 索引

```sql
CREATE UNIQUE INDEX idx_package_status_log_unique
  ON package_status_log(package_id, from_status, to_status, reason, created_at);
CREATE INDEX idx_package_active_expire
  ON package(status, expire_at)
  WHERE status = 'active';
```

## Cron Job Design

### ExpirePackageCronJob

- 调度周期：`0 * * * *`
- 扫描条件：`status = 'active' AND expire_at <= NOW()`
- 批量策略：按 `id` 游标分页，每批 500 条
- 原子更新：`UPDATE package SET status='expired' WHERE id=:id AND status='active'`
- 分布式锁：`cron_job_lock(job_name='expire_package_cron', hour_bucket)`

## Event Handler Design

### PackageStatusReconciler

- 订阅事件：`lesson.confirmed`、`lesson.cancelled`
- 事务内更新计数后重算状态
- 仅当 `status='active' AND available=0 AND reserved=0` 时转换为 `exhausted`

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis | `package:{id}` | 300s | 状态变更时主动失效 |
| Redis | `user:{user_id}:packages` | 60s | 状态变更时主动失效 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 单次过期巡检 P99 | < 5 分钟 |
| 事件触发状态重算 P99 | < 100ms |
| 批量更新单批 500 条耗时 | < 1s |

## Security

- 定时任务与事件处理器均为内部服务，不暴露外部 API
- 状态转换日志不可删除，保留 180 天
- 分布式锁超时 10 分钟，防止任务僵死

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-020 / US-029 / US-032 / US-033 | 被依赖 | package 与上课事件来源 |
| US-021 / US-030 | 依赖本 US | 状态读取与取消事件 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-050-.../tech-design.md` §1 |
| Cron Job / Event Handler | `docs/stories/US-050-.../tech-design.md` §2 |
| Caching | `docs/stories/US-050-.../tech-design.md` §4 |
| Performance | `docs/stories/US-050-.../tech-design.md` §5 |
| Security | `docs/stories/US-050-.../tech-design.md` §6 |
