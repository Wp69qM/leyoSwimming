## Why

学员可能因教学风格、时间等原因需要更换绑定教练。系统必须支持按规则退旧购新，并严格遵守单教练约束与注册用户中转。

## What Changes

- 新增 `GET /api/coaches/available-for-change` 可更换教练列表
- 新增 `POST /api/users/me/coach/unsubscribe` 第一步：冻结旧套餐并生成 pending_change 退款记录
- 新增 `POST /api/users/me/coach/purchase` 第二步：校验有效期后正式退款旧套餐并创建新教练待支付订单
- 新增旧套餐冻结状态（active → frozen，frozen_reason=refund_pending，复用 PRD §5.5.1.2 枚举值）与待处理退款记录 `refund_record`（status=pending_change）
- 新增第二步成功后旧套餐正式退款（frozen → refunded）
- 新增超时未发起第二步时的回滚机制（frozen → original_status，取消 pending_change 退款记录）
- 新增新教练待支付订单创建
- 用户身份由学员回退为注册用户，第二步成功或超时回滚后重算
- 两步操作增加 24 小时有效期

## Capabilities

### New Capabilities

- `student-change-bound-coach`: 学员更换绑定教练

### Modified Capabilities

（无）

## Impact

- **数据表**：修改 `package`、`user`；新增 `order`、`refund_record`
- **API**：新增 3 个端点（可更换教练列表 + 两步操作端点）
- **状态机**：package active→frozen（frozen_reason=refund_pending）→refunded / original_status；refund_record 无→pending_change→approved / 取消；user 学员→注册用户
- **依赖**：依赖 US-004/US-005 / US-020 / US-021 / US-030；被 US-023 / US-029 / US-050 依赖
