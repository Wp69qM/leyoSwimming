# Design: US-016 系统自动释放下周可约时段

## Overview

定时任务驱动，按规则将教练模板转换为可约时段。仅对 `coach.status IN (1, 4)` 的教练生成 slot，`status=3`（已离职）教练排除。

## Data Model

### 表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `schedule_slot` | 写 | `coach_id`, `start_time`, `end_time`, `status`, `course_type` |
| `release_log` | 写 | `release_date`, `release_scope`, `planned_at`, `executed_at`, `generated_slots`, `status` |
| `coach_availability_template` | 读 | 模板来源 |
| `release_rule` | 读 | 规则来源 |
| `venue_closure` | 读 | 闭馆日 |

### 索引

```sql
CREATE INDEX idx_release_log_date ON release_log(release_date, status);
CREATE UNIQUE INDEX idx_slot_coach_time ON schedule_slot(coach_id, start_time);
```

## API Design

（无新增用户接口）

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis | `next_release_time` | 60s | 释放成功后更新 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 100 教练总完成 | P99 < 30s |
| 单教练生成 | < 50ms |

## Security

- 任务仅内部触发
- 唯一索引防止重复 slot

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-014 | 依赖 | 教练模板 |
| US-015 | 依赖 | 释放规则 |
| US-003 | 被依赖 | 倒计时 |
| US-018/US-029 | 被依赖 | 预约 |
