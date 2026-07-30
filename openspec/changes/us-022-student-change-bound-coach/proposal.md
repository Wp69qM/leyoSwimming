## Why

学员可能因教学风格、时间等原因需要更换绑定教练。系统必须支持按规则退旧购新，并严格遵守单教练约束与注册用户中转。

## What Changes

- 新增 `GET /api/coaches/available-for-change` 可更换教练列表
- 新增 `POST /api/users/me/coach/change` 提交更换教练
- 新增旧套餐退款计算与状态变更（active → refunded）
- 新增退款记录 `refund_record`
- 新增新教练待支付订单创建
- 用户身份由学员回退为注册用户

## Capabilities

### New Capabilities

- `student-change-bound-coach`: 学员更换绑定教练

### Modified Capabilities

（无）

## Impact

- **数据表**：修改 `package`、`user`；新增 `order`、`refund_record`
- **API**：新增 2 个端点
- **状态机**：package active→refunded；user 学员→注册用户
- **依赖**：依赖 US-004/US-005 / US-020 / US-021 / US-030；被 US-023 / US-029 / US-050 依赖
