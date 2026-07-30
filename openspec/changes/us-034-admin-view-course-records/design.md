# Design: US-034 管理员查看上课记录

> 本文档对应 `docs/stories/US-034-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-034 为管理员提供上课记录查询能力。核心流程：鉴权与权限校验 → 按条件筛选 booking/course_record → 分页返回列表；详情则关联 coach/student/package/course_record/audit_log 返回完整信息。

## Data Model

### 读写的表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `booking` | 读 | `id`, `coach_id`, `user_id`, `package_id`, `status`, `start_time`, `end_time` |
| `course_record` | 读 | `id`, `booking_id`, `content`, `student_summary_json` |
| `package` | 读 | `id`, `consumed_count`, `available_count` |
| `user` | 读 | `id`, `name` |
| `audit_log` | 读 | `id`, `action`, `target_id`, `created_at` |

### 索引

```sql
CREATE INDEX idx_booking_status_start_time ON booking(status, start_time DESC);
CREATE INDEX idx_booking_coach_status ON booking(coach_id, status);
CREATE INDEX idx_booking_user_status ON booking(user_id, status);
CREATE INDEX idx_course_record_booking ON course_record(booking_id);
```

## API Design

### GET /api/admin/course-records

- 鉴权：管理员且拥有 `course_record:read` 权限
- Query:
  - `coach_id?: int`
  - `student_id?: int`
  - `status?: string`（可逗号分隔多个状态）
  - `start_date?: date`（YYYY-MM-DD）
  - `end_date?: date`（YYYY-MM-DD，最大跨度 1 年）
  - `page?: int = 1`
  - `size?: int = 20`（最大 100）
- Response 200: `{ items: CourseRecordListItem[], total, page, size }`
- Response 400: `DATE_RANGE_TOO_LARGE | INVALID_PAGE | INVALID_SIZE`
- Response 403: `ADMIN_PERMISSION_DENIED`

### GET /api/admin/course-records/{booking_id}

- 鉴权：管理员且拥有 `course_record:read` 权限
- Response 200: `{ booking_id, status, start_time, end_time, coach, student, package, course_record, audit_logs }`
- Response 404: `COURSE_RECORD_NOT_FOUND`
- Response 403: `ADMIN_PERMISSION_DENIED`

### GET /api/admin/course-records/export

- 鉴权：管理员且拥有 `course_record:read` 权限
- Query: 同列表查询
- Response 200: CSV/Excel 文件流
- Response 400: `DATE_RANGE_TOO_LARGE | EXPORT_LIMIT_EXCEEDED`

## State Machine

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| 无 | — | 本 US 为只读查询，不触发状态转换 |

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| 上课记录列表 | `admin:course-records:{hash}` | 60s | 状态变更后删除 |
| 单条详情 | `admin:course-record:{booking_id}` | 300s | 关联数据变更后删除 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 列表查询 P99 | < 300ms |
| 详情查询 P99 | < 200ms |
| 导出 10000 条 | < 5s |
| 单页最大 100 条 | 响应 < 200ms |

## Security

- 严格校验管理员权限
- 防止越权访问其他场馆/数据（后续多租户扩展）
- 导出功能增加审计日志
- 不返回敏感字段（如完整手机号、身份证号）

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-029 | 依赖 | 已存在 booking |
| US-033 | 依赖 | 已存在 course_record |
