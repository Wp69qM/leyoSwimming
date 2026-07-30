# Design: US-033 教练确认上课记录

> 本文档对应 `docs/stories/US-033-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-033 完成教练在课程结束后确认上课并扣除课时。核心流程：校验课程已开始且 booking 可确认 → 填写上课记录 → 事务内更新 booking/package/course_record → 通知学员。管理员返还课时职责已迁移至 US-035。

> **管理员返还课时已迁移至 US-035**（v3 评审 P0 修复）：原 `hour_return` 表与 `/api/admin/bookings/{booking_id}/return-hour` API 已移至 US-035 tech-design §1.2/§2.1 定义。本 US 仅负责教练侧确认/旷课标记，不再包含管理员返还职责。

## Data Model

### 读写的表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `booking` | 修改 | `id`, `coach_id`, `status`, `start_time` |
| `package` | 修改 | `id`, `reserved_count`, `consumed_count`, `available_count`, `total_hours`, `status`, `first_lesson_confirmed_at` |
| `course_record` | 新增/修改 | `id`, `booking_id`, `content`, `focus_tags_json`, `mastery_level`, `homework`, `media_json` |
| `student_summary_draft` | 读取/删除 | 学员在 US-032 预填的总结草稿，本 US 创建 course_record 时合并并删除 |
| `audit_log` | 写 | `id`, `action`, `operator_id`, `target_id`, `details` |

### package 表新增字段（v3 评审 P0 修复）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `first_lesson_confirmed_at` | DATETIME | 可空，默认 NULL | 本套餐首次上课确认时间；教练确认上课时，若该字段为 NULL 则写入当前时间，并触发未成年人监护人短信通知（US-037 协同）；非首次确认时该字段已有值，不重复触发短信 |

**写入逻辑**：
- 教练点击「确认上课并扣除课时」时：
  - 若 `package.first_lesson_confirmed_at IS NULL`：写入当前时间，并检查 `coach_student_profile.is_minor = true`，若为真则异步触发监护人短信
  - 若 `package.first_lesson_confirmed_at IS NOT NULL`：不写入，不触发短信
- 该字段在套餐购买时（US-020）初始化为 NULL，在套餐退款/作废时不清空（保留历史记录）

### 索引

```sql
CREATE UNIQUE INDEX idx_course_record_booking ON course_record(booking_id);
CREATE UNIQUE INDEX idx_student_summary_draft_booking ON student_summary_draft(booking_id);
CREATE INDEX idx_booking_coach_status ON booking(coach_id, status);
CREATE INDEX idx_package_user_status ON package(user_id, status);
CREATE INDEX idx_package_first_lesson ON package(package_id, first_lesson_confirmed_at);
```

## API Design

### POST /api/coach/bookings/{booking_id}/confirm

- 鉴权：教练且为 booking.coach_id
- Request: `{ content: string, focus_tags: string[], mastery_level: int, homework: string, media: string[] }`
- Response 200: `{ booking_id, status: "已完成", package: { reserved_count, consumed_count, available_count } }`
- Response 400: `CLASS_NOT_STARTED | BOOKING_NOT_CONFIRMABLE`
- Response 403: `BOOKING_ACCESS_DENIED`

### POST /api/coach/bookings/{booking_id}/mark-absent

- 鉴权：教练且为 booking.coach_id
- Request: `{ remark?: string }`
- Response 200: `{ booking_id, status: "旷课", cancel_reason: 3, package: { reserved_count, consumed_count, available_count } }`
- Response 400: `CLASS_NOT_STARTED | BOOKING_NOT_CONFIRMABLE`
- Response 403: `BOOKING_ACCESS_DENIED`

> 注：管理员返还课时 API 已迁移至 US-035 tech-design §2.1。

## State Machine

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| `booking` | 待上课/上课中 → 已完成 | 教练确认 |
| `booking` | 待上课/上课中 → 旷课（cancel_reason=3） | 教练标记学员未到课 |
| `package` | reserved → consumed | 教练确认 或 旷课标记 |
| `package` | active → exhausted | 确认最后一课时 |

> **首次上课判定**（v3 评审 P0 修复）：通过 package.first_lesson_confirmed_at 字段判定。该字段为 NULL 表示未首次确认，写入当前时间后表示已首次确认。监护人短信仅在 first_lesson_confirmed_at 从 NULL → 有值 时触发。

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| booking | `booking:{booking_id}` | 300s | 确认/旷课标记后删除 |
| package | `package:{package_id}` | 300s | 变更后删除 |
| 我的预约列表 | `bookings:list:{user_id}` | 300s | 确认后删除 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 教练确认接口 P99 | < 300ms |
| 旷课标记接口 P99 | < 300ms |
| 并发确认 | 单 booking 串行（分布式锁/乐观锁）|

## Security

- 教练只能确认/标记旷课自己的课程
- 幂等键防止重复确认/重复标记旷课
- 防止课程未开始或已终态的确认

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-029 | 依赖 | 已存在 booking |
| US-032 | 依赖 | 学员签到/记录页已存在 |
| US-035 | 被依赖 | 管理员返还课时（独立 US） |
| US-034 | 被依赖 | 管理员查看上课记录 |
