# Tech Design: US-032 学员签到课程

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `booking` | 修改 | 记录 checked_in_at |
| `course_record` | 读/修改 | 读取教练记录，保存学员总结（由 US-033 创建） |
| `student_summary_draft` | 新增 | 临时学员总结草稿，course_record 尚未创建时使用 |

#### course_record（由 US-033 创建，本 US 仅读写 student_summary_json）

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `booking_id` | FK，1:1 |
| `coach_id` | FK |
| `user_id` | FK |
| `content` | 课程内容 |
| `focus_tags_json` | 今日重点标签数组 |
| `mastery_level` | 掌握程度：0=待评估 1=生疏 2=一般 3=熟练 |
| `homework` | 课后作业 |
| `media_json` | 照片/视频 URL 数组 |
| `student_summary_json` | 学员总结：身体感受、学习效果、问题反馈 |
| `created_at` | 创建时间 |
| `updated_at` | 更新时间 |

#### student_summary_draft（本 US 新增）

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `booking_id` | FK，1:1（唯一索引） |
| `user_id` | FK |
| `body_feeling` | 身体感受 |
| `learning_effect` | 学习效果 |
| `feedback` | 问题反馈 |
| `created_at` | 创建时间 |
| `updated_at` | 更新时间 |

> **course_record 时序说明**：course_record 由 US-033 教练确认上课时创建。若学员在教练确认前提交总结，系统先写入 `student_summary_draft`；US-033 创建 course_record 时，将 draft 合并到 `course_record.student_summary_json` 并删除 draft 记录。

### 1.2 索引

```sql
CREATE UNIQUE INDEX idx_course_record_booking ON course_record(booking_id);
CREATE UNIQUE INDEX idx_student_summary_draft_booking ON student_summary_draft(booking_id);
CREATE INDEX idx_booking_user_status ON booking(user_id, status);
CREATE INDEX idx_booking_checkin ON booking(user_id, checked_in_at);
```

## 2. API 设计

### 2.1 POST /api/bookings/{booking_id}/check-in

- **鉴权**：必须登录且为 booking 所有者
- **可用状态**：仅允许 booking.status ∈ {待上课, 上课中}；status = 已预约 时返回 CHECKIN_WINDOW_NOT_OPEN
- **签到窗口**：当前时间 ∈ [start_time - 10min, end_time]（即 status = 待上课 或 上课中）
- **Response 200**: `{ booking_id, checked_in_at }`
- **Response 400**: `CHECKIN_WINDOW_NOT_OPEN | BOOKING_NOT_CHECKINABLE`
- **Response 403**: `BOOKING_ACCESS_DENIED`

### 2.2 GET /api/bookings/{booking_id}/record

- **鉴权**：必须登录且为 booking 所有者
- **Response 200**: `{ booking_id, course_record: { content, focus_tags, mastery_level, homework, media, student_summary } }`
- **Response 404**: `COURSE_RECORD_NOT_FOUND`（仅记录不存在，booking 仍存在）

### 2.3 POST /api/bookings/{booking_id}/summary

- **鉴权**：必须登录且为 booking 所有者
- **Request**: `{ body_feeling: string, learning_effect: string, feedback: string }`
- **Response 200**: `{ course_record_id, student_summary_json }`
- **Response 400**: `SUMMARY_INVALID`

## 3. 业务规则

- 签到仅允许 status ∈ {待上课, 上课中}，status = 已预约 时不可签到
- 待上课状态定义为开课时间前 10 分钟至课程结束时间；该状态切换由系统状态机或定时任务负责（不在本 US 实现）
- 重复签到幂等返回，不重复发送通知

## 4. 状态机

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| `booking` | 无状态转换 | 签到仅更新 checked_in_at |
| `course_record` | 修改 | 提交课后总结 |

## 5. 缓存

- booking 缓存：`booking:{booking_id}`，TTL 300s，签到后删除
- 我的预约列表：`bookings:list:{user_id}`，签到后删除
- 上课记录缓存：`record:{booking_id}`，TTL 600s，总结提交后删除

## 6. 性能

| 指标 | 目标 |
|------|------|
| 签到接口 P99 | < 200ms |
| 上课记录查询 P99 | < 200ms |
| 课后总结提交 P99 | < 200ms |

## 7. 安全

- 严格校验 booking 归属
- 签到窗口服务端校验，防止客户端绕过
- 课后总结字段长度限制
- 禁止修改不属于自己的记录

## 8. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-029 | 依赖 | 已存在 booking |
| US-031 | 依赖 | 教练代约/改约产生 booking |
| US-033 | 被依赖 | 教练确认上课记录 |

## 9. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 正常签到 | `test_checkin_success` |
| 提交课后总结 | `test_submit_summary_success` |
| 签到窗口未开放 | `test_checkin_window_not_open` |
| 重复签到 | `test_checkin_idempotent` |
| 查看空上课记录 | `test_record_empty_state` |

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | 开发 | 初版 |
| v1.1 | 2026-07-31 | 开发 | 同步 user-story v1.5：§2.1 签到窗口 `start_time - 15min` 改为 `start_time - 10min`；§3 业务规则"开课时间前 15 分钟"改为"开课时间前 10 分钟"（对齐 PRD §6.1.2） |
