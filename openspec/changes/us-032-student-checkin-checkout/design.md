# Design: US-032 学员签到/签退课程

> 本文档对应 `docs/stories/US-032-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-032 完成学员到馆签到与课后总结。核心流程：校验签到窗口 → 记录 checked_in_at → 通知教练；课程结束后展示教练记录 → 允许学员填写课后总结。

## Data Model

### 读写的表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `booking` | 修改 | `id`, `user_id`, `status`, `checked_in_at`, `start_time`, `end_time` |
| `course_record` | 读/修改 | `id`, `booking_id`, `coach_id`, `user_id`, `content`, `focus_tags_json`, `mastery_level`, `homework`, `media_json`, `student_summary_json` |

### 索引

```sql
CREATE UNIQUE INDEX idx_course_record_booking ON course_record(booking_id);
CREATE INDEX idx_booking_user_status ON booking(user_id, status);
CREATE INDEX idx_booking_checkin ON booking(user_id, checked_in_at);
```

## API Design

### POST /api/bookings/{booking_id}/check-in

- 鉴权：必须登录且为 booking 所有者
- Response 200: `{ booking_id, checked_in_at }`
- Response 400: `CHECKIN_WINDOW_NOT_OPEN | BOOKING_NOT_CHECKINABLE`
- Response 403: `BOOKING_ACCESS_DENIED`

### GET /api/bookings/{booking_id}/record

- 鉴权：必须登录且为 booking 所有者
- Response 200: `{ booking_id, course_record: { content, focus_tags, mastery_level, homework, media, student_summary } }`
- Response 404: `COURSE_RECORD_NOT_FOUND`（仅记录不存在，booking 仍存在）

### POST /api/bookings/{booking_id}/summary

- 鉴权：必须登录且为 booking 所有者
- Request: `{ body_feeling: string, learning_effect: string, feedback: string }`
- Response 200: `{ course_record_id, student_summary_json }`
- Response 400: `SUMMARY_INVALID`

## State Machine

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| `booking` | 无状态转换 | 签到仅更新 checked_in_at |
| `course_record` | 修改 | 提交课后总结 |

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| booking | `booking:{booking_id}` | 300s | 签到后删除 |
| 我的预约列表 | `bookings:list:{user_id}` | 300s | 签到后删除 |
| 上课记录 | `record:{booking_id}` | 600s | 总结提交后删除 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 签到接口 P99 | < 200ms |
| 上课记录查询 P99 | < 200ms |
| 课后总结提交 P99 | < 200ms |

## Security

- 严格校验 booking 归属
- 签到窗口服务端校验，防止客户端绕过
- 课后总结字段长度限制
- 禁止修改不属于自己的记录

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-029 | 依赖 | 已存在 booking |
| US-033 | 被依赖 | 教练确认上课记录 |
