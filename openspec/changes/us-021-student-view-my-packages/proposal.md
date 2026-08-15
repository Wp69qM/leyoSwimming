## Why

学员需要随时查看自己的套餐状态、剩余课时和有效期，以便合理安排预约和续费。

## What Changes

- 新增 `POST /api/user/package/list` 查询当前用户套餐列表
- 按 active / exhausted / expired / refunded / frozen 分组返回
- 顶部汇总 active 套餐课时
- frozen 套餐展示原因文案
- 新增 `POST /api/coach/package/detail` 查询教练视角套餐使用详情
- 套餐详情页满足退款条件时展示「申请退款」入口，点击跳转 US-027

## Capabilities

### New Capabilities

- `student-view-my-packages`: 学员查看我的套餐

### Modified Capabilities

（无）

## Impact

- **数据表**：读取 `package`（含快照字段 `paid_amount`、`refund_enabled`、`refund_ratio`、`refund_valid_days`）、`coach`
- **API**：新增 2 个端点
- **前端**：新增「我的套餐」页面，入口位于底部导航「我的」→ 个人中心 →「我的套餐」；套餐详情页在满足退款条件时展示「申请退款」入口
- **依赖**：依赖 US-004/US-005 / US-020；被 US-027 / US-022 / US-029 依赖
