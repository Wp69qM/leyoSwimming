## Why

US-029~US-032 完成了预约、取消/改约、签到与课后总结，但课时正式消耗需要教练在课程结束后确认并记录上课内容。本 US 让教练在课程结束后填写上课记录并扣除课时，同时支持管理员在特殊情况下返还课时，保证课时消耗准确可审计。

## What Changes

- 新增 `POST /api/coach/bookings/{booking_id}/confirm`：教练确认上课并扣除课时
- 新增 `POST /api/admin/bookings/{booking_id}/return-hour`：管理员返还课时
- 新增 `course_record` 表记录教练上课记录
- 新增 `hour_return` 表记录管理员返还课时
- 新增教练端课程确认页与上课记录填写弹窗
- 课程结束后 24 小时内教练可确认，超时由系统定时任务处理

## Capabilities

### New Capabilities

- `coach-confirm-course-record`: 教练确认上课记录并扣除课时，含管理员返还课时

### Modified Capabilities

- `student-checkin-checkout` (US-032): 学员签到后教练端可查看并确认
- `admin-view-course-records` (US-034): 管理员查看已确认的上课记录

## Impact

- **数据表**：新增 `course_record`、`hour_return`；修改 `booking`、`package`；写 `audit_log`
- **API**：新增 2 个端点
- **定时任务**：超时未确认自动扣课时
- **前端**：新增教练端课程确认页、管理端返还课时入口
- **依赖**：依赖 US-029、US-032；被 US-034 依赖
