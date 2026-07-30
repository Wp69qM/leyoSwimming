## Why

US-029 让学员成功预约了正价课程，但实际运营中学员常因临时变动需要取消或改约。本 US 让学员在不同时间窗内按规则取消或改约正价课程并释放/预占课时，减少客诉与纠纷。

## What Changes

- 新增 `POST /api/bookings/{booking_id}/cancel`：学员提交取消，开课前 ≥24h 直接取消并释放课时，<24h 创建待审批取消申请
- 新增 `POST /api/coach/cancellation-requests/{id}/approve`：教练同意取消申请，释放课时
- 新增 `POST /api/coach/cancellation-requests/{id}/reject`：教练拒绝取消申请
- 新增 `booking_cancellation` 表记录取消申请与审批结果
- 新增定时任务：每分钟扫描超过 24h 未审批的取消申请，标记为已拒绝（超时）
- 新增小程序取消/改约流程页面

## Capabilities

### New Capabilities

- `student-cancel-reschedule-formal-course`: 学员取消/改约正价课程，含 24h 时间窗判断、教练审批、超时拒绝

### Modified Capabilities

- `student-book-formal-course` (US-029): 取消成功后回滚 booking 与 package 状态
- `coach-confirm-course-record` (US-033): 取消审批与确认上课存在状态互斥

## Impact

- **数据表**：新增 `booking_cancellation`；修改 `booking`、`package`
- **API**：新增 3 个端点
- **定时任务**：新增取消申请超时扫描任务
- **前端**：新增小程序取消预约页
- **依赖**：依赖 US-029；被 US-031、US-033 依赖
