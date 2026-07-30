## Context

本 US 实现管理员对 package 的手动冻结与解冻。冻结会将 active package 转为 frozen 并释放 reserved 课时；解冻则恢复为 active。需要保证状态转换的原子性与幂等性。

## Goals / Non-Goals

**Goals:**

- 管理员可冻结 active package 并选择原因
- 管理员可解冻 frozen package
- 冻结时自动释放 reserved 课时并取消未上课 booking
- 记录审计日志

**Non-Goals:**

- 不实现自动冻结策略（如投诉自动冻结）
- 不实现退款执行（由 US-028 处理）
- 不修改 package 其他字段

## Decisions

1. **`frozen_reason` 使用统一枚举（v3 评审 P0-1 修复）**
   - 理由：原使用 TINYINT 整型 `0=pending_review 1=admin_manual 2=court_order` 与 PRD §3.7 `coach_resigned`、§5.5.1.2 字符串枚举不一致，现统一为 VARCHAR(32) 三值枚举
   - 枚举：`coach_resigned`（教练离职，系统自动）/ `refund_pending`（退款处理中，US-027/US-028 触发）/ `admin_frozen`（管理员手动冻结，细分原因记录在 audit_log.remark）

2. **冻结时同步取消未上课 booking**
   - 理由：frozen 状态禁止约课，已预约未上课的课程必须释放，否则学员/教练端状态不一致
   - 范围：`start_time > NOW()` 且状态为已预约/待上课
   - cancel_reason：`6`（套餐冻结，v3 评审 P0-1 修复，TINYINT 整型）

3. **`package` 表增加 `version` 乐观锁**
   - 理由：防止并发冻结/解冻导致状态覆盖
   - 替代方案：数据库行锁 —— rejected，乐观锁更适合低频写操作

4. **解冻不恢复 reserved 课时**
   - 理由：冻结时已释放 reserved→available，解冻后学员需重新预约，不应自动预占课时

5. **请求体使用 `reason_detail` 而非 `reason`（v3 评审 P0-1 修复）**
   - 理由：frozen_reason 固定写入 `admin_frozen`，细分原因由管理员填写并写入 audit_log.remark
   - 请求字段：`reason_detail`（描述性字符串，非枚举）

## Risks / Trade-offs

- **[Risk]** 冻结导致学员即将开始的课程被取消，引发投诉 → **Mitigation**: 对 30 分钟内开始的课程增加二次确认，并通知学员/教练
- **[Risk]** 频繁冻结/解冻导致缓存穿透 → **Mitigation**: 操作后立即失效相关缓存，并设置合理的缓存 TTL
- **[Risk]** 司法冻结场景在 MVP 仅占位 → **Mitigation**: P3 功能，MVP 接口保留枚举值但不主动使用

## Migration Plan

1. 确认 `package` 表已存在 `status`、`frozen_reason`、`reserved_count`、`available_count`、`version` 字段
2. 部署后端接口与管理端页面
3. 回滚：回退代码，无需删除字段

## Open Questions

- 解冻后是否需要管理员手动恢复因冻结而取消的课程？建议 MVP 不恢复，由学员重新预约。
