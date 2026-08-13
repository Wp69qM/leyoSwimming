## Context

本 US 实现管理员对用户购买后的 package 实例进行管理，覆盖查看列表/详情、手动冻结、解冻、延期以及发起退款。冻结会将 active package 转为 frozen 并释放 reserved 课时；解冻则恢复为 active。发起退款会创建退款订单并冻结 package，后续审批/驳回在订单管理页完成。需要保证状态转换的原子性、幂等性与乐观锁并发控制。

> **v2.0 范围变更**：从仅覆盖冻结/解冻扩展为完整套餐管理。

## Goals / Non-Goals

**Goals:**

- 管理员可查询套餐实例列表与详情
- 管理员可冻结 active package 并选择原因
- 管理员可解冻 frozen package
- 管理员可延期 active 或 expired 且剩余课时 > 0 的 package
- 管理员可从套餐管理发起退款，生成退款订单与 refund_record
- 冻结时自动释放 reserved 课时并取消未上课 booking
- 所有状态变更写入 audit_log

**Non-Goals:**

- 不实现自动冻结策略（如投诉自动冻结）
- 不实现退款审批/驳回/金额调整/原路退回（由 US-046 处理）
- 不修改 package_template
- 不恢复因冻结而取消的 booking

## Decisions

1. **`frozen_reason` 使用统一枚举（v3 评审 P0-1 修复）**
   - 理由：原使用 TINYINT 整型与 PRD §5.5.1.2 字符串枚举不一致
   - 枚举：`coach_resigned`（教练离职，系统自动）/ `refund_pending`（退款处理中）/ `admin_frozen`（管理员手动冻结，细分原因记录在 audit_log.remark）

2. **冻结时同步取消未上课 booking**
   - 理由：frozen 状态禁止约课，已预约未上课的课程必须释放
   - 范围：`start_time > NOW()` 且状态为已预约/待上课
   - cancel_reason：`6`（套餐冻结）

3. **`package` 表使用 `version` 乐观锁**
   - 理由：防止并发状态变更导致覆盖

4. **解冻不恢复 reserved 课时**
   - 理由：冻结时已释放 reserved→available，解冻后学员需重新预约

5. **退款入口在套餐管理页，审批在订单管理页**
   - 理由：套餐管理负责资产状态，订单管理负责资金流水
   - 本 US 发起退款后 package 标记为 frozen/refund_pending，US-046 完成后续审批

6. **请求体使用 `reasonDetail` 而非 `reason`**
   - 理由：管理员手动冻结时 frozen_reason 固定写入 `admin_frozen`，细分原因写入 audit_log.remark

## Risks / Trade-offs

- **[Risk]** 冻结导致学员即将开始的课程被取消 → **Mitigation**: 对 30 分钟内开始的课程增加二次确认，并通知学员/教练
- **[Risk]** 频繁冻结/解冻导致缓存穿透 → **Mitigation**: 操作后立即失效相关缓存，并设置合理的缓存 TTL
- **[Risk]** 发起退款后若订单管理页未及时处理，package 长期处于 frozen → **Mitigation**: 在详情页显示待处理退款订单链接，并在 US-046 中提供待审批提醒

## Migration Plan

1. 确认 `package` 表已存在快照字段（package_mode, original_price, refund_enabled, refund_ratio, refund_valid_days）与 `version` 字段
2. 确认 `order` 与 `refund_record` 表已支持退款订单创建
3. 部署后端接口与管理端页面
4. 回滚：回退代码，无需删除字段

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-020 / US-021 | 本 US 依赖 | 产生 package 记录并在学员端展示状态 |
| US-045 | 本 US 依赖 | package 快照字段来源 |
| US-053 | 本 US 依赖 | 后台管理系统登录认证入口 |
| US-046 | 双向 | 本 US 发起的退款订单在 US-046 审批/驳回 |
| US-027 / US-028 | 双向 | 退款流程共享 order/refund_record 模型 |
