# Design: US-036 教练提交请假申请

> 本文档对应 `docs/stories/US-036-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-036 负责教练提交请假申请，审批逻辑由 US-044 处理。核心是 1 个写 API + 1 个读 API + `coach_leave` 表。

## Data Model

### 新增表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `coach_leave` | 请假记录主表 | `leave_id`, `coach_id`, `start_time`, `end_time`, `reason`, `status`, `created_at`, `updated_at` |
| `audit_log` | 操作审计 | 记录提交动作 |

### 索引

```sql
CREATE INDEX idx_coach_leave_coach_time ON coach_leave(coach_id, start_time, end_time);
CREATE INDEX idx_coach_leave_status ON coach_leave(status);
CREATE INDEX idx_coach_leave_coach_status ON coach_leave(coach_id, status);
```

### 状态定义

| status | 含义 |
|--------|------|
| 0 | 待审批 |
| 1 | 已通过 |
| 2 | 已驳回 |

## API Design

### POST /api/v1/coach/leaves

- 鉴权：教练 JWT，且 `coach.status = 1`
- Body: `{ start_time, end_time, reason? }`
- 成功：201 `{ leave_id, status: 0, message }`
- 错误：`COACH_STATUS_INVALID` (403) / `LEAVE_START_PAST` (400) / `INVALID_TIME_RANGE` (400) / `LEAVE_TIME_CONFLICT` (409)

### GET /api/v1/coach/leaves

- 鉴权：教练 JWT
- Query: `page`, `size`
- 成功：200 `{ items, total, page, size }`
- 仅返回当前教练自己的请假记录

## State Machine

| 实体 | 转换 | 触发条件 |
|------|------|----------|
| `coach_leave` | 无 → 0（待审批） | 教练提交请假申请 |

> 0→1 / 0→2 由 US-044 处理。

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis | `coach:{coach_id}:leaves:pending` | 60s | 请假状态变更时删除 |
| Redis | `coach:{coach_id}:leaves:list:{page}:{size}` | 60s | 请假记录变更时删除 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 提交接口 P99 | < 300ms |
| 列表接口 P99 | < 200ms |
| 重叠查询 DB | < 50ms |

## Security

- 写接口校验教练 JWT 与 `coach.status = 1`
- 列表查询按 `coach_id` 严格隔离，禁止越权
- 已开始课程不可请假：`start_time >= NOW()`
- 输入参数做 SQL 注入 / XSS 过滤
- 敏感操作记录 `audit_log`
- 限流：同一教练 1 分钟 > 60 次提交 → 429

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-011 | 被依赖 | 教练需先通过审核 |
| US-012 | 被依赖 | 教练个人主页数据 |
| US-044 | 依赖本 US | 管理员审批请假并处理受影响预约 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-036-.../tech-design.md` §1 |
| API Design | `docs/stories/US-036-.../tech-design.md` §2 |
| State Machine | `docs/stories/US-036-.../tech-design.md` §3 |
| Caching | `docs/stories/US-036-.../tech-design.md` §4 |
| Performance | `docs/stories/US-036-.../tech-design.md` §5 |
| Security | `docs/stories/US-036-.../tech-design.md` §6 |
