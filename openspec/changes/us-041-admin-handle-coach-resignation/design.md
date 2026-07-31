## Context

本变更实现管理后台对教练离职申请的审批。审批通过会触发大量批量变更：coach 状态、booking 取消、package frozen、schedule_slot hidden，需要保证原子性与幂等性。

## Goals / Non-Goals

**Goals:**

- 管理员可查看 pending_audit 离职审批队列
- 管理员可通过或拒绝离职申请
- 通过时按 checklist 校验并执行批量同步变更
- 通过时根据 action 类型分流处理（PRD §5.4.7 三选一）：refund 生成退款记录、transfer 换教练、continue 保持 active
- 拒绝时恢复 coach.status=1，不回滚已登记的处理结果

**Non-Goals:**

- 不实现教练费实际结算（仅记录待结算）
- 不实现学员退款执行（由 US-028 处理）
- 不进行中的课程自动取消

## Decisions

1. **同一数据库事务包裹批量变更**
   - 理由：审批通过涉及 coach、ticket、booking、package、schedule_slot 多张表，必须全部成功或全部回滚
   - 替代方案：Saga 异步拆分 —— rejected，MVP 数据量在百级以内，单事务可在 1s 内完成

2. **使用数据库乐观锁防止并发审批**
   - 理由：两个管理员同时审批同一工单时，仅第一次应成功
   - 实现：通过 `ticket.version` 或 `updated_at` 行锁校验

3. **未来 booking 定义为 `start_time > NOW()`**
   - 理由：避免误取消正在进行的课程
   - 边界：进行中的课程由教练/学员线下收尾

4. **checklist 后端强制校验**
   - 理由：前端勾选可辅助但不可信，关键检查项必须由后端执行
   - 独立检查项：① active 学员数 = 0；② 若 active 学员数 > 0，所有 active 套餐已确认全额退款；③ 教练费已结算；④ 未来排班已清空。禁止用「或」削弱第①项

## State Machine（v3 评审 P0 修复同步）

### 教练状态

- `4` 申请中 → `3` 已离职（通过）
- `4` 申请中 → `1` 已通过（拒绝）

### 离职工单状态

- `pending_audit` → `approved`
- `pending_audit` → `rejected`

### package 状态

- `active` → `frozen`（frozen_reason = `coach_resigned`）
- 审批过程中：`reserved_count` 先随 booking 取消释放，再兜底归 0，`available_count` 相应增加

### refund_record 状态

- `pending` → `approved` / `rejected` / `completed`（US-028 处理退款）
- 教练主动离职时，所有 active package 未消耗剩余课时强制 100% 退款

### booking 状态

- `已预约` / `待上课` → `已取消`（cancel_reason = 2，教练离职）

> **cancel_reason 字段类型**（v3 评审 P0 修复）：TINYINT 整型，全项目统一枚举 `1=学员取消 / 2=教练离职 / 3=学员旷课 / 4=场馆闭馆 / 5=教练请假 / 6=套餐冻结`。本 US 使用 `2=教练离职`。

> **注**：booking 状态机不含「待支付」。「待支付」是 order 实体的状态（PRD §6.2.1/§6.2.2），不应出现在 booking 状态转换中。

> **审批执行顺序**（v3 评审 P0 修复）：为避免 `reserved` 课时重复释放，通过审批时必须按以下顺序执行：
> 1. 对所有 active package（含未确认退款的套餐），生成 `refund_record`：`refund_amount = 单价 × 剩余课时`（已消耗不退），并通知学员选择退款或换教练；
> 2. 取消未来 booking（释放对应 package.reserved）；
> 3. 兜底清零 active package.reserved_count，available_count 相应增加；
> 4. active package → frozen（frozen_reason = coach_resigned）；
> 5. 未来 schedule_slot → hidden；
> 6. coach.status 4 → 3。

## Risks / Trade-offs

- **[Risk]** 批量 package 数量大导致事务超时 → **Mitigation**: 100 份以内单事务；超过 100 份分批并在 Saga 中补偿（P2）
- **[Risk]** 审批通过后缓存未失效导致学员仍能看到旧教练可约时段 → **Mitigation**: 批量变更后统一失效 coach/package/booking/slot 缓存
- **[Risk]** 拒绝审批后教练已登记的部分套餐处理结果处于半完成状态 → **Mitigation**: 产品规则明确不回滚，教练可继续教学并在线下收尾

## Migration Plan

1. 执行 Knex migration 为 `coach_resignation_ticket` 增加 `settlement_status` 与 `schedule_cleared` 字段
2. 执行 Knex migration 创建 `refund_record` 表及索引
3. 部署后端接口与管理端页面
4. 回滚：删除新增字段/表并回退代码

## Open Questions

- 教练费结算状态是否作为 MVP 强校验项？建议作为 checklist 展示但允许标记为待结算后通过，结算由财务模块异步处理。
