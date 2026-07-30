# Tech Design: US-032 学员签到/签退课程

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `booking` | 修改 | 记录 checked_in_at |
| `course_record` | 读/修改 | 读取教练记录，保存学员总结 |

#### course_record

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

### 1.2 索引

```sql
CREATE UNIQUE INDEX idx_course_record_booking ON course_record(booking_id);
CREATE INDEX idx_booking_user_status ON booking(user_id, status);
CREATE INDEX idx_booking_checkin ON booking(user_id, checked_in_at);
```

## 2. API 设计

### 2.1 POST /api/bookings/{booking_id}/check-in

- **鉴权**：必须登录且为 booking 所有者
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

## 3. 状态机

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| `booking` | 无状态转换 | 签到仅更新 checked_in_at |
| `course_record` | 修改 | 提交课后总结 |

## 4. 缓存

- booking 缓存：`booking:{booking_id}`，TTL 300s，签到后删除
- 我的预约列表：`bookings:list:{user_id}`，签到后删除
- 上课记录缓存：`record:{booking_id}`，TTL 600s，总结提交后删除

## 5. 性能

| 指标 | 目标 |
|------|------|
| 签到接口 P99 | < 200ms |
| 上课记录查询 P99 | < 200ms |
| 课后总结提交 P99 | < 200ms |

## 6. 安全

- 严格校验 booking 归属
- 签到窗口服务端校验，防止客户端绕过
- 课后总结字段长度限制
- 禁止修改不属于自己的记录

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-029 | 依赖 | 已存在 booking |
| US-033 | 被依赖 | 教练确认上课记录 |

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 正常签到 | `test_checkin_success` |
| 提交课后总结 | `test_submit_summary_success` |
| 签到窗口未开放 | `test_checkin_window_not_open` |
| 重复签到 | `test_checkin_idempotent` |
| 查看空上课记录 | `test_record_empty_state` |
