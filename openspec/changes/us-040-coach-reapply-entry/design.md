## Context

本变更让已离职教练（`coach.status = 3`）可重新发起入驻申请，状态回到 0（待审核），复用 US-010/011 的审核流程。核心约束是历史评分/评价保留但仅对老学员可见，已 frozen 的老学员套餐不自动恢复。

## Goals / Non-Goals

**Goals:**

- 仅 `status = 3` 的教练可发起重新入驻
- 重新入驻时 `coach.status` 从 3 变为 0，并生成新的入驻申请记录
- 管理员可通过/拒绝重新入驻申请
- 历史评分对老学员保留可见，对新学员隐藏

**Non-Goals:**

- 不修改 US-010/011 新教练入驻的核心审核逻辑
- 不自动解冻老学员的 frozen package
- 不删除或修改历史评价内容

## Decisions

1. **复用 `coach_application` 表并增加 `is_reapply` 字段**
   - 理由：避免重复建设审核流程，统一管理员审核入口
   - 替代方案：新建 `coach_reapply_application` 表 —— rejected，增加维护成本且审核逻辑重复

2. **扩展 `coach_rating` / `review` 增加 `is_visible_to_new` 字段**
   - 理由：精确控制重新入驻后历史评价对新学员的可见性，老学员不受影响
   - 替代方案：按时间过滤历史评价 —— rejected，老学员也可能在重新入驻后查看，时间过滤不准确

3. **不自动解冻 frozen package**
   - 理由：PRD 明确要求已 frozen 的老学员 package 仍 frozen，等待学员主动换回原教练或退款
   - 替代方案：重新入驻成功后批量恢复 active —— rejected，违反 PRD §5.4.8

4. **幂等键去重**
   - 理由：防止教练快速重复点击提交产生重复申请
   - 键格式：`Idempotency-Key: coach:{coach_id}:reapply`

## Risks / Trade-offs

- **[Risk]** 重新入驻教练历史差评对新学员隐藏，但老学员仍可见，可能引发老学员公平性质疑 → **Mitigation**: 产品侧在《用户须知》中说明评价可见性规则
- **[Risk]** 管理员在入驻审核队列中难以区分新入驻与重新入驻 → **Mitigation**: 列表增加 `is_reapply` 标签与筛选条件
- **[Risk]** 重新入驻成功后，原 frozen package 学员未及时感知教练回归 → **Mitigation**: 学员主动进入「我的套餐」可看到「换回原教练」入口（US-022 延伸）

## Migration Plan

1. 执行 Knex migration 为 `coach_application` 增加 `is_reapply` 字段
2. 执行 Knex migration 为 `coach_rating` / `review` 增加 `is_visible_to_new` 字段
3. 部署后端接口与教练端/管理端页面
4. 回滚：删除新增字段并回退代码

## Open Questions

- 重新入驻是否需要重新上传证书？建议 MVP 沿用原 coach 资料，教练可修改后提交。
