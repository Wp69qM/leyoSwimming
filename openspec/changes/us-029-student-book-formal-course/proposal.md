## Why

US-020 完成了学员购买正价套餐，但学员仍需将课时转化为实际上课预约。本 US 让已购正价套餐的学员能够预约已绑定教练的可用时段，是核心上课流程的起点。

## What Changes

- 新增 `GET /api/coaches/{coach_id}/slots` 获取已绑定教练未来 7 天可约时段（复用 US-014）
- 新增 `GET /api/packages/active` 获取当前学员 active 正价套餐列表
- 新增 `POST /api/bookings` 提交正价课程预约
- 新增微信小程序「预约」页面，展示已绑定教练可约时段并支持提交预约
- 预约成功后系统按 FIFO 选择最早 active 套餐，预占 1 课时：available_count -1，reserved_count +1
- 创建 booking 记录，status = 已预约，并发送提醒通知
- 支持幂等键去重，防止网络抖动导致重复预约

## Capabilities

### New Capabilities

- `student-book-formal-course`: 学员预约正价课程，含时段查询、套餐校验、课时预占、预约创建

### Modified Capabilities

- `coach-manage-schedule-slots` (US-014): 预约成功后需失效相关时段缓存
- `student-buy-formal-package` (US-020): 提供 active 套餐数据供预约时校验与预占

## Impact

- **数据表**：读取 `schedule_slot`、`user`；读写 `package`、`booking`
- **API**：新增 3 个端点（1 写 2 读）
- **缓存**：新增 Redis key `slots:{coach_id}:{date}`、`package:{package_id}`、`booking:lock:{schedule_slot_id}`
- **前端**：新增小程序预约页面
- **依赖**：依赖 US-014、US-020；被 US-030、US-031、US-032、US-033 依赖
