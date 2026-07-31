## Why

US-029 让学员可自行预约正价课程，但部分学员（尤其未成年人或不熟悉操作的用户）需要教练协助完成预约或改约。本 US 让教练在获得学员授权后代为预约或改约正价课程，提升服务便利性并减少学员操作成本。

## What Changes

- 新增 `POST /api/coach/bookings`：教练为已绑定学员代约正价课程
- 新增 `POST /api/coach/bookings/{booking_id}/reschedule`：教练为学员改约到其他可约时段
- 新增教练端「学员管理」代约入口与「预约详情」改约入口
- 代约时系统按 FIFO 选择学员最早 active 套餐预占 1 课时，booking.operator = coach
- 代约/改约前学员须在系统内明确授权教练并写入 `coach_authorization` 记录，确保可审计
- 代约/改约前校验教练与学员的 `coach_student` 绑定关系，教练只能代约/改约自己绑定的学员
- 代约/改约适用 24h 时间窗规则：开课时间 <24h 的代约/改约需学员在系统内确认，未确认不生效
- 改约时系统先取消原预约释放课时，再按新时段创建新预约；<24h 改约须学员确认后才执行，未确认则原 booking 保持原状
- 整过程在事务内完成
- 代约/改约成功后通知学员

## Capabilities

### New Capabilities

- `coach-book-reschedule-for-student`: 教练代约/改约正价课程，含学员绑定校验、课时预占、改约事务

### Modified Capabilities

- `student-book-formal-course` (US-029): 复用时段查询与套餐预占逻辑
- `coach-manage-schedule-slots` (US-014): 预约成功后需失效相关时段缓存

## Impact

- **数据表**：读取 `schedule_slot`、`user`、`coach`；读写 `package`、`booking`
- **API**：新增 2 个教练端端点
- **缓存**：复用 US-029 缓存策略
- **前端**：新增教练端代约/改约页面
- **依赖**：依赖 US-012、US-014、US-020、US-029；被 US-032、US-033 依赖
