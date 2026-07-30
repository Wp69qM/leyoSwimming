# Design: US-015 管理员配置预约释放规则

## Overview

本 US 为全局配置型 US，新增单表单行配置 `release_rule`，供下游定时任务与首页倒计时读取。

## Data Model

### 表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `release_rule` | 读/写 | `id`(固定 1)、`release_weekday`、`release_time`、`release_scope`、`waitlist_enabled`、`waitlist_ttl_hours`、`watch_reminder_minutes`、`holiday_release_enabled`、`holiday_release_offset_days` |
| `admin_operation_log` | 写 | `target_type`、`target_id`、`before_snapshot`、`after_snapshot`、`operator_id` |

### 索引

（单表单行，无需额外索引）

## API Design

### GET /api/admin/release-rule

- 鉴权：需 `SCHEDULE_RELEASE_CONFIG` 权限
- Response 200: 完整配置对象

### PUT /api/admin/release-rule

- 鉴权：需 `SCHEDULE_RELEASE_CONFIG` 权限
- Request Body: 完整或部分配置字段
- Response 200: 更新后完整配置
- Response 400: `INVALID_RELEASE_TIME` / `REMINDER_TOO_LONG`
- Response 403: `FORBIDDEN`

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis | `release_rule:current` | 60s | PUT 成功后立即 DEL |

## Performance Targets

| 指标 | 目标 |
|------|------|
| GET P99 | < 50ms |
| PUT P99 | < 100ms |

## Security

- 接口仅限管理员角色
- 所有字段服务端校验
- 操作日志记录变更前后快照

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-016 | 被依赖 | 释放任务读取规则 |
| US-003 | 被依赖 | 首页倒计时读取规则 |
| US-023 | 被依赖 | 候补/关注读取规则 |
