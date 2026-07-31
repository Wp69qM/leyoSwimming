## Why

US-029 完成了预约创建，但学员到馆后需要通知教练已到场，课后也需要回顾教练记录并填写自我总结，才能形成完整的学习闭环。本 US 让学员在指定时间窗口内自助签到，并在课后查看/填写上课记录与总结。

## What Changes

- 新增 `POST /api/bookings/{booking_id}/check-in`：学员一键签到，记录 checked_in_at
- 新增 `GET /api/bookings/{booking_id}/record`：查看教练上课记录
- 新增 `POST /api/bookings/{booking_id}/summary`：提交学员课后总结
- 新增微信小程序签到入口、上课记录页、课后总结弹窗
- 签到可用状态：仅允许 booking.status ∈ {待上课, 上课中}；status = 已预约 时不可签到
- 签到窗口：开课时间前 15 分钟至课程结束时间（即 待上课 状态持续期间）
- 签到不扣除课时，仅用于通知教练
- 课后总结保存到 `course_record.student_summary_json`

## Capabilities

### New Capabilities

- `student-checkin-checkout`: 学员签到/签退课程，含时间窗校验、上课记录查看、课后总结提交

### Modified Capabilities

- `coach-confirm-course-record` (US-033): 教练填写上课记录后学员端可查看

## Impact

- **数据表**：修改 `booking`；读写 `course_record`
- **API**：新增 3 个端点
- **缓存**：booking 与上课记录缓存
- **前端**：新增小程序签到与记录页面
- **依赖**：依赖 US-029、US-031；被 US-033 依赖
