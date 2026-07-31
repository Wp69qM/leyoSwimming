## Why

原 US-033 同时承载「教练确认上课并扣除课时」与「管理员返还课时」两件职责不同的事，违反单 US 单一职责原则。前者由教练在课程结束后正向触发扣课时，后者由管理员在特殊情况下（教练误操作、客诉、争议）反向回滚课时，两者参与角色、触发时机、审计要求均不同。P0 评审决定将「管理员返还课时」拆分为独立 US-035（沿用原未占用的编号），使其可独立交付、独立审计、独立回滚，同时让 US-033 聚焦教练确认流程。

## What Changes

- 新增 `POST /api/admin/bookings/{booking_id}/return-hour`：管理员返还已扣课时
- 新增 `hour_return` 表记录每次返还（含原因分类、备注、操作管理员）
- 修改 `package` 表：`consumed_count -1`、`available_count +1`；并在返还后若 `consumed < total_hours` 则套餐从 `exhausted` 复活为 `active`；若原状态为 `expired` 且返还后 `available > 0`，则同步将 `status` 恢复为 `active` 并按原有效期时长更新 `expire_at`
- 系统 MUST 校验该 booking 累计 `returned_hours` < `consumed_hours`，否则返回 `RETURN_QUOTA_EXCEEDED`，防止同一 booking 被多次返还
- 写 `audit_log` 记录返还操作，写 `notification` 通知学员
- 管理后台 booking 详情页新增「返还课时」入口与弹窗（原因必填）
- 从 US-033 spec 中移除「管理员返还课时」职责（US-033 仅保留教练确认）

## Capabilities

### New Capabilities

- `admin-return-hour`: 管理员在特殊情况下对已扣课时的 booking 返还课时，事务内回滚 package 消耗、记录返还与审计、通知学员

### Modified Capabilities

- `coach-confirm-course-record` (US-033): 移除其中的管理员返还课时职责，US-033 仅保留教练确认上课并扣除课时

## Impact

- **数据表**：新增 `hour_return`；修改 `package`（含条件性 `expire_at` 更新）；写 `audit_log`、`notification`；读 `booking`
- **API**：新增 1 个管理员端点
- **状态机**：`package` 新增 `consumed → available`、`exhausted → active`（复活）与 `expired → active`（复活并延长 `expire_at`）转换
- **前端**：管理后台 booking 详情页新增返还入口与弹窗
- **安全**：管理员鉴权 + `MANAGE_BOOKING` 权限 + 事务 + 乐观锁防并发
- **依赖**：依赖 US-033（产生已扣课时 booking）；被 US-034 依赖（管理员查看上课记录可见返还记录）
