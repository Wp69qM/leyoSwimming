# Design: US-013 教练管理实时状态

> 本文档对应 `docs/stories/US-013-教练-管理实时状态/tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-013 让通过入驻审核的教练管理实时状态，支持手动切换与按课程安排的自动切换，并通过实时通道广播给学员端。核心是 2 个读写 API + 1 个管理员查询 API + 定时任务 + WebSocket/SSE 广播 + 手动覆盖优先级处理。

## Data Model

### 新增/修改的表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `coach` | UPDATE | `realtime_status`、`status_override_flag`、`override_until` |
| `coach_status_log` | INSERT | `log_id`、`coach_id`、`from_status`、`to_status`、`source`、`created_at` |
| `booking` | SELECT | 判断课程时间与状态 |

### 索引

```sql
-- 状态日志查询索引
CREATE INDEX idx_coach_status_log_coach_id ON coach_status_log(coach_id);

-- 定时任务扫描索引
CREATE INDEX idx_booking_coach_start_time ON booking(coach_id, start_time);
```

## API Design

### GET /api/coach/realtime-status

- 鉴权：是（需教练登录态且 `coach.status = 1`）
- Request: 无
- Response 200: `{ coach_id, realtime_status, status_override_flag, override_until }`
- Response 403: `COACH_STATUS_NOT_ALLOWED`

### PUT /api/coach/realtime-status

- 鉴权：是（需教练登录态且 `coach.status = 1`）
- Request: `{ realtime_status: number }`
- Response 200: `{ coach_id, realtime_status, status_override_flag }`
- Response 400: `INVALID_STATUS`（状态值非法）
- Response 400: `LEAVE_REQUIRED`（无请假申请时尝试设为请假中）
- Response 403: `STATUS_LOCKED_BY_LEAVE`（请假中状态不可手动修改）

### GET /api/admin/coach/realtime-status

- 鉴权：是（需管理员登录态）
- Request query: `coach_id`
- Response 200: `{ coach_id, realtime_status, status_override_flag, override_until }`

## State Machine

### 教练实时状态机

```
任意实时状态 ──[手动切换/自动切换]──→ 任意实时状态
```

- 手动切换后 `status_override_flag = 1`；该标记仅在非请假时段生效
- 请假中（5）由请假审批同步，优先级最高，覆盖手动/自动状态并禁止手动修改
- 自动切换仅当 `status_override_flag = 0` 或手动覆盖已过期时执行；请假时段内跳过自动切换

### 自动切换规则

| 触发条件 | 动作 | 目标状态 |
|---------|------|---------|
| 课程开始前 15 分钟 | 自动切换 | 2（上课中） |
| 课程结束后 15 分钟 | 自动恢复 | 1（空闲中）或 4（已下班） |

## Caching

| 层 | Key | TTL | 用途 | 失效策略 |
|----|-----|-----|------|---------|
| Redis | `coach:realtime:{coach_id}` | 5 分钟 | 学员端展示教练实时状态 | 状态变更后立即失效 |
| WebSocket/SSE | `coach:status:{coach_id}` | 实时 | 状态变更广播 | 状态变更时推送 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 手动状态更新接口 P99 | < 200ms |
| 自动切换任务单次执行 | < 30 秒 |
| 状态广播 P99 延迟 | < 1 秒 |

## Security

- 所有教练端接口必须教练登录鉴权且 `coach.status = 1`
- 请假中状态由 US-036 请假审批系统同步，本接口禁止手动修改
- 管理员查询接口需管理员权限
- 记录状态变更日志，来源区分手动/自动

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-011 | 依赖 | 管理员审核通过后 coach.status = 1 |
| US-036 | 关联 | 请假审批通过后自动设置 realtime_status = 5 |
| US-029 | 被依赖 | 学员端展示教练实时状态 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-013-.../tech-design.md` §1 |
| API Design | `docs/stories/US-013-.../tech-design.md` §2 |
| State Machine | `docs/stories/US-013-.../tech-design.md` §3 |
| Caching | `docs/stories/US-013-.../tech-design.md` §4 |
| Performance / Security | `docs/stories/US-013-.../tech-design.md` §5 |
