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

1. **`frozen_reason` 使用枚举值**
   - 理由：保证学员端文案与后台筛选一致性
   - 枚举：`pending_review` / `admin_manual` / `court_order`

2. **冻结时同步取消未上课 booking**
   - 理由：frozen 状态禁止约课，已预约未上课的课程必须释放，否则学员/教练端状态不一致
   - 范围：`start_time > NOW()` 且状态为已预约/待上课

3. **`package` 表增加 `version` 乐观锁**
   - 理由：防止并发冻结/解冻导致状态覆盖
   - 替代方案：数据库行锁 —— rejected，乐观锁更适合低频写操作

4. **解冻不恢复 reserved 课时**
   - 理由：冻结时已释放 reserved→available，解冻后学员需重新预约，不应自动预占课时

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
