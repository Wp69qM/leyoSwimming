## Context

本变更实现教练主动离职的申请端。提交后教练状态变为 4（申请中），系统生成离职工单供教练登记学员套餐处理结果，随后进入管理员审批队列（US-041）。

## Goals / Non-Goals

**Goals:**

- 仅 `status = 1` 的教练可提交离职申请
- 提交后生成离职工单并变更教练状态
- 教练对每份 active 套餐登记处理结果（PRD §5.4.7 三选一：转新教练 / 全额退款 / 继续上完）
- 确认完成后提交至管理员审批

**Non-Goals:**

- 不实现管理员审批逻辑（US-041）
- 不实现状态 4 期间的样式标签（按 PRD 要求不展示）
- 不实现 transfer / continue 的真正执行（执行由 US-041 审批通过时分别触发转教练 / 维持 active；本 US 仅登记意图）
- 不实现教练自行撤销离职申请（PRD §5.4.7 未定义 `4 → 1` 转换）
- 不退款的真正打款执行（由 US-028 处理）
- 不通知学员（PRD 明确 status=4 期间不通知学员）

## Decisions

1. **新增 `coach_resignation_ticket`、`coach_resignation_action` 与 `refund_record` 表**
   - 理由：工单主表记录离职申请进度，action 子表记录每份套餐的处理决策，`refund_record` 记录 100% 退款金额，支持后续管理员审批与 US-028 退款执行
   - 替代方案：在 `package` 表增加 `resignation_action` 字段 —— rejected，无法支持历史记录与多维查询

2. **工单状态机独立设计**
   - 理由：离职流程涉及教练、管理员、学员多方，独立状态机便于追踪
   - 状态：processing → pending_audit → approved/rejected/cancelled

3. **幂等键去重**
   - 理由：防止教练快速重复点击提交产生重复工单
   - 键格式：`Idempotency-Key: coach:{coach_id}:resignation:apply`

4. **不缓存工单详情**
   - 理由：工单进度需要实时一致，缓存会引入脏读风险
   - coach.status 缓存需在更新时失效

## Risks / Trade-offs

- **[Risk]** 教练误提交离职申请后无法自行撤销 → **Mitigation**: PRD §5.4.7 未定义撤销流程，MVP 不支持教练自行撤销；如业务需要，应先在 PRD 中补充 `4 → 1` 状态转换与审批队列处理规则；当前由 US-041 管理员驳回触发 4 → 1
- **[Risk]** 教练登记 transfer/continue 后，US-041 审批通过前学员体验不一致 → **Mitigation**: 工单 status=processing/pending_audit 期间 package 维持 active，审批通过后才执行转教练或维持；学员在审批通过前可继续上课
- **[Risk]** 状态 4 期间教练仍继续上课，审批通过后突然取消未来课程 → **Mitigation**: US-041 在审批通过时批量取消未来 booking

## Migration Plan

1. 执行 Knex migration 创建 `coach_resignation_ticket`、`coach_resignation_action` 与 `refund_record` 表及索引
2. 部署后端接口与教练端页面
3. 为 `coach.status` 增加枚举值 4 的校验
4. 回滚：删除表并回退 coach.status 校验

## Open Questions

- 撤销申请后，已登记的处理结果是否保留？建议 MVP 保留 action 记录但标记为 cancelled，便于审计。
