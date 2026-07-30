## Why

用户购买体验课后需要完成实际预约才能上课。本 US 让体验课学员选择教练可约时段并提交预约，同时支持规则内的取消。

## What Changes

- 新增体验课预约接口 `POST /api/bookings/trial`
- 新增取消预约接口 `POST /api/bookings/{id}/cancel`
- 新增体验课预约的课时预占/释放逻辑
- 新增 24h 内取消的教练审批流程（创建 cancel_request）
- 预约页区分体验课与正价课标签

## Capabilities

### New Capabilities

- `trial-student-book-cancel`: 体验课学员预约与取消

### Modified Capabilities

（无）

## Impact

- **数据表**：新增/修改 `booking`、`package`、`cancel_request`
- **API**：新增 2 个端点
- **依赖**：依赖 US-014 / US-016 / US-017；被 US-032 / US-033 依赖
