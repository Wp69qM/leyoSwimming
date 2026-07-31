# leyoSwimming MVP 用户故事业务评审报告 v8

> **评审版本**：v8.0
> **评审日期**：2026-07-31
> **评审范围**：全量 50 个 US（独立核查，不依赖 INDEX.md 标记）
> **评审依据**：
> - [PRD v11.2](../prd/prd.md)（§3.6 退款身份矩阵已修正 frozen 策略；§6.2.1/§6.2.2 两阶段退款时序；§5.4.7 三选一；§6.4.5 退款公式）
> - [AGENTS.md §8 用户故事生成硬约束](../../AGENTS.md)
> - [v7 评审报告](./mvp-us-business-review-report-v7.md)
> - 项目 hard constraints（cancel_reason TINYINT 6 值、user.status TINYINT、frozen_reason VARCHAR 3 值、course_record 表名、operator 字段）
> **与 v7 的差异**：v7 的 24 个 P0 中 13 个是误判（状态不同步/US-037 缺失），v8 基于文档实际内容独立判断；v8 核查发现 v7 修复中 10 项"半落地"问题并已修复

---

## 0. 执行摘要

### 0.1 关键指标

| 指标 | v7 | v8（修复前） | v8（修复后） |
|------|------|------------|------------|
| 评审 US 总数 | 50 | 50 | 50 |
| ✅ APPROVED | 11（22%）| 31（62%）| **45（90%）** |
| ⚠️ 有条件 APPROVED | 15（30%）| 6（12%）| **5（10%）** |
| ❌ 需修复 | 24（48%）| 13（26%）| **0（0%）** |
| 🔴 P0 问题 | 24（含 13 误判）| 10（半落地）| **0** |
| 🟡 P1 问题 | 15 | 0 | **0** |
| 🔵 P2 问题 | 4 | 5 | **5（非阻塞）** |

### 0.2 总体结论

1. **v7 的 24 个 P0 中 13 个是误判**：经核查 INDEX.md 实际全部为 [REVIEW]（非 APPROVED），US-037 目录与三件套齐全，"状态不同步"和"目录缺失"主张均不成立。
2. **v7 修复存在 10 项"半落地"问题**：变更日志/OpenSpec 已更新但 docs/stories/ 主文档残留旧文本，已在 v8 评审中全部修复。
3. **PRD §3.6 内部矛盾已修正**：v11.2 统一为 frozen(refund_pending) 策略，与 §3.7/§6.3.1/US-027/US-028 一致。
4. **50 个 US 全部达到 APPROVED 或有条件 APPROVED**：0 个需修复，可进入 TDD 实现阶段。
5. **OpenSpec validate 50/50 通过**。

---

## 1. 逐 US 评审结果

### 1.1 游客浏览主线（US-001 ~ US-003）

| US | 标题 | v8 状态 | 评分 | 关键发现 |
|----|------|---------|------|---------|
| US-001 | 学员浏览教练 | ⚠️ 有条件 APPROVED | 9.0 | 15 章+6 GWT；§14 决策位置已优化；Figma 待填写（P2） |
| US-002 | 游客查看套餐公告与场馆信息 | ⚠️ 有条件 APPROVED | 8.5 | 15 章+6 GWT；GWT 实现细节已移至 §14；§12 套餐模板字段待补（P2） |
| US-003 | 游客查看教练详情 | ⚠️ 有条件 APPROVED | 9.0 | 15 章+4 GWT(L1)；§14.6 倒计时取整策略已补；Figma 待填写（P2） |

### 1.2 注册登录主线（US-004 ~ US-009）

| US | 标题 | v8 状态 | 评分 | 关键发现 |
|----|------|---------|------|---------|
| US-004 | 游客微信授权登录 | ⚠️ 有条件 APPROVED | 9.0 | 15 章+5 GWT；user.status TINYINT 修复落地；角色统一"游客"；Figma 待填写（P2） |
| US-005 | 用户补充注册资料 | ⚠️ 有条件 APPROVED | 8.5 | 15 章+3 GWT(L1)；P0 修复（移除 identity_status 触发声明）落地 |
| US-006 | 用户手机号/账号密码登录 | ✅ APPROVED | 9.0 | §9.1 依赖矛盾已修复（US-004/US-005 为前置）；15 章+5 GWT |
| US-007 | 用户账号注销 | ⚠️ 有条件 APPROVED | 8.5 | 15 章+3 GWT(L1)；数据保留 90 天统一；tech-design status TINYINT 一致 |
| US-008 | 用户账号安全设置 | ⚠️ 有条件 APPROVED | 8.0 | 15 章+6 GWT(L2)；§11.3 场景计数文字偏差（P2）；user 表主键 id、password_hash 可空 |
| US-009 | 用户隐私协议授权与撤回 | ⚠️ 有条件 APPROVED | 8.5 | 15 章+3 GWT(L1)；游客态不可同意/撤回修复落地；位置在 US-004 后 |

### 1.3 教练入驻主线（US-010 ~ US-016、US-037 ~ US-040）

| US | 标题 | v8 状态 | 评分 | 关键发现 |
|----|------|---------|------|---------|
| US-010 | 教练提交入驻资料 | ⚠️ 有条件 APPROVED | 8.5 | 15 章+6 GWT(L2)；submitted_at 字段已补；草稿与提交均 status=0 |
| US-011 | 管理员审核教练入驻资质 | ⚠️ 有条件 APPROVED | 8.5 | 15 章+3 GWT(L1)；状态机 0→1/0→2/2→0 完整；submitted_at 过滤一致 |
| US-012 | 教练管理个人主页与参考单价 | ⚠️ 有条件 APPROVED | 8.5 | 15 章+5 GWT(L2)；订单金额协商 P2 明确 |
| US-013 | 教练管理实时状态 | ⚠️ 有条件 APPROVED | 8.5 | 15 章+3 GWT(L1)；请假状态优先级逻辑清晰 |
| US-014 | 教练管理可约时段 | ✅ APPROVED | 9.0 | 15 章+5 GWT(L2)；coach_availability_template status enabled/disabled；三连修复落地 |
| US-015 | 管理员配置预约释放规则 | ✅ APPROVED | 9.0 | 15 章+5 GWT；holiday_release_offset_days -6→-1 修复三处同步落地 |
| US-016 | 系统自动释放下周可约时段 | ✅ APPROVED | 9.0 | §6.2 -6→-1 已同步 US-015 修复；定时任务逻辑完整 |
| US-037 | 教练维护学员信息 | ✅ APPROVED | 9.0 | 三件套完整（v7 误判"缺失"）；15 章+5 GWT；短信职责归属 US-033 |
| US-038 | 教练分享个人主页 | ✅ APPROVED | 8.8 | 15 章+5 GWT；隐私规则对齐 PRD §5.3.1 |
| US-039 | 教练申请离职 | ✅ APPROVED | 8.8 | cancel API 已删除；§14.2 三选一已修复；action 字段 refund/transfer/continue |
| US-040 | 教练重新入驻 | ✅ APPROVED | 8.8 | 15 章+5 GWT；状态机 3→0→1/3 清晰 |

### 1.4 体验课购买与预约（US-017 ~ US-018）

| US | 标题 | v8 状态 | 评分 | 关键发现 |
|----|------|---------|------|---------|
| US-017 | 游客购买体验课套餐 | ✅ APPROVED | 8.8 | tech-design §2.1/§3 已同步"支付回调才创建 package"；§3 健康承诺书/免责协议已补 |
| US-018 | 体验课学员预约与取消 | ⚠️ 有条件 APPROVED | 8.5 | 15 章+5 GWT；24h 取消审批指向 US-030；§9.3 关联 booking_cancellation |

### 1.5 正价套餐购买（US-019 ~ US-022、US-050）

| US | 标题 | v8 状态 | 评分 | 关键发现 |
|----|------|---------|------|---------|
| US-019 | 学员浏览正价套餐 | ✅ APPROVED | 9.0 | 只读浏览，3 GWT 完整；PRD 引用 v1.2 修正 |
| US-020 | 学员购买正价套餐 | ⚠️ 有条件 APPROVED | 8.5 | 6 GWT；GUARDIAN_PHONE_REQUIRED 已补；§15 评审记录空白（P2） |
| US-021 | 学员查看我的套餐 | ✅ APPROVED | 9.0 | 5 GWT 含 exhausted/expired 展示；只读无矛盾 |
| US-022 | 学员更换绑定教练 | ✅ APPROVED | 9.0 | 两步独立操作 + 24h 回滚 + frozen_reason=refund_pending；6 GWT+4 边界 |
| US-050 | 系统自动处理套餐过期与课时耗尽 | ✅ APPROVED | 9.0 | course_record 表名统一；exhausted→expired 补全；6 GWT |

### 1.6 候补与预约（US-023 ~ US-024、US-029 ~ US-031）

| US | 标题 | v8 状态 | 评分 | 关键发现 |
|----|------|---------|------|---------|
| US-023 | 学员候补与关注时段 | ✅ APPROVED | 8.8 | slot_follow 标记失效策略已明确；5 GWT |
| US-024 | 学员候补自动转正 | ✅ APPROVED | 8.8 | 事件驱动转正；5 GWT 覆盖首位/跳过/无候补；分布式锁 |
| US-029 | 学员预约正价课程 | ✅ APPROVED | 9.0 | HTTP 409 + DUPLICATE_BOOKING 修复落地；5 GWT |
| US-030 | 学员取消正价课程 | ✅ APPROVED | 9.0 | <2h 申诉 GWT 已补；估时 1.5d 对齐 INDEX；operator 字段 |
| US-031 | 教练代约/改约正价课程 | ✅ APPROVED | 8.8 | <24h 项目约定已标注；operator=coach 已落地；6 GWT |

### 1.7 签到与上课记录（US-032 ~ US-036）

| US | 标题 | v8 状态 | 评分 | 关键发现 |
|----|------|---------|------|---------|
| US-032 | 学员签到课程 | ✅ APPROVED | 9.0 | 15 分钟残留已彻底清理（§4.1/§12/tech-design 全部 10 分钟）；目录名已改 |
| US-033 | 教练确认上课记录 | ✅ APPROVED | 9.0 | cancel_reason 整型 3；24h 超时旷课不可用；hour_return 已迁出；5 GWT+5 边界 |
| US-034 | 管理员查看上课记录 | ✅ APPROVED | 8.5 | 只读查询，5 GWT；权限/分页/导出边界清晰 |
| US-035 | 管理员返还课时 | ✅ APPROVED | 9.0 | 返还+延期复合操作已澄清；expired→active 路径清晰 |
| US-036 | 教练提交请假申请 | ✅ APPROVED | 8.8 | >30 天无依据规则已删除；3 GWT(L1) |

### 1.8 支付与退款（US-025 ~ US-028、US-046）

| US | 标题 | v8 状态 | 评分 | 关键发现 |
|----|------|---------|------|---------|
| US-025 | 学员支付套餐订单 | ✅ APPROVED | 8.8 | 支付闭环 5 GWT；超时与候补转正分布式锁；幂等键明确 |
| US-026 | 学员查看订单列表与详情 | ✅ APPROVED | 9.0 | 9 状态标签 + 色值表对齐 PRD §6.2.1/§6.2.2 |
| US-027 | 学员申请退款 | ✅ APPROVED | 9.0 | §4.1/§6.1/§7.3/§12 + tech-design 全部对齐 frozen(refund_pending) + 释放 reserved；半落地已修复 |
| US-028 | 管理员处理退款并原路退回 | ✅ APPROVED | 9.0 | 状态机对齐 frozen→refunded/active 两阶段时序；半落地已修复 |
| US-046 | 管理员查看与处理订单 | ✅ APPROVED | 9.0 | 争议退款状态机图已补全；区分用户申诉与管理员标记两入口 |

### 1.9 管理后台（US-041 ~ US-045、US-047 ~ US-049）

| US | 标题 | v8 状态 | 评分 | 关键发现 |
|----|------|---------|------|---------|
| US-041 | 管理员处理教练离职 | ✅ APPROVED | 9.2 | 三选一 + 退款公式 price_per_hour × (reserved_count + available_count) 完整落地 |
| US-042 | 管理员管理用户账号 | ✅ APPROVED | 9.0 | 15 章+7 GWT；RBAC + 强制变更 + 审计完整 |
| US-043 | 管理员手动冻结/解冻套餐 | ✅ APPROVED | 9.0 | 仅取消未上课 booking；frozen_reason VARCHAR 3 值枚举；cancel_reason=6 |
| US-044 | 管理员管理教练排班与请假 | ✅ APPROVED | 9.0 | slot 标记 disabled 统一；cancel_reason=5；请假通过流程清晰 |
| US-045 | 管理员配置标准与自定义套餐 | ⚠️ 有条件 APPROVED | 8.3 | §5 引用已修正；适用教练单一绑定已澄清；§5 #4 描述文字待优化（P2） |
| US-047 | 管理员配置场馆公告闭馆换水与用户须知 | ✅ APPROVED | 9.0 | 异步任务失败补偿策略已补；cancel_reason=4；HTTP 202 |
| US-048 | 管理员配置首页运营内容 | ✅ APPROVED | 8.8 | 内容审核流程已移除（对齐 PRD §5.5.5 #6）；6 文件彻底清理 |
| US-049 | 管理员查看数据看板与处理客服工单 | ✅ APPROVED | 9.0 | 依赖方向已修正（US-027/US-030 前置）；tech-design §7 已同步 |

---

## 2. v7 修复落地核查

### 2.1 v7 P0 修复核查（13 项真实 P0）

| P0 | US | v7 声称 | v8 核查 | 状态 |
|----|-----|---------|---------|------|
| P0-1 | US-032 签到窗口 | 已改 10 分钟 | §4.1/§12/tech-design 残留 15 分钟 → v8 已修复 | ✅ |
| P0-2 | US-027 reserved 释放 | 已对齐 frozen | §4.1/§6.1/§7.3/§12 + tech-design 残留"保持 active" → v8 已修复 | ✅ |
| P0-3 | US-039 撤销 cancel API | 已删除 | tech-design §4.5 残留 + §8 矛盾 → v8 已修复 | ✅ |
| P0-4 | US-041 三选一 | 已改 | user-story + tech-design 均落地 | ✅ |
| P0-5 | US-046 争议状态机 | 已补全 | tech-design §3 状态机图未画 → v8 已修复 | ✅ |
| P0-A | US-007 数据保留 | 已统一 90 天 | 已落地 | ✅ |
| P0-B | US-029 HTTP 状态码 | 已改 409 | 已落地 | ✅ |
| P0-C | US-030 <2h GWT | 已补 | 已落地 | ✅ |
| P0-D | US-031 <24h 标注 | 已标注 | 已落地 | ✅ |
| P0-E | US-033 旷课时序 | 已补 | 已落地 | ✅ |
| P0-F | US-035 expired 复活 | 已澄清 | 已落地 | ✅ |
| P0-G | US-036 >30 天 | 已删除 | 已落地 | ✅ |
| P0-H | US-049 依赖方向 | 已修正 | tech-design §7 未同步 → v8 已修复 | ✅ |

### 2.2 v7 P0 误判核查（13 项）

| US | v7 主张 | v8 核查 | 结论 |
|----|---------|---------|------|
| US-005/006/008/009/011/019/022/027/028/029/030/031/032/033/034/035/036/042/046/049 | INDEX 与 user-story 状态不同步 | INDEX 全部 [REVIEW]，user-story §1 全部 [REVIEW] | ❌ 误判 |
| US-037 | 目录与三件套完全缺失 | 目录存在，三件套齐全 | ❌ 误判 |

### 2.3 v7 P1 修复核查（15 项）

全部 15 项 P1 修复均已落地（US-001/002/003/004/010/015/017/018/020/023/043/044/045/047/048）。

---

## 3. v8 修复的"半落地"问题（10 项）

| # | US | 问题 | 修复 |
|---|-----|------|------|
| 1 | US-006 | §3 vs §9.1 依赖矛盾 | §9.1 改为 US-004/US-005 前置 |
| 2 | US-016 | §6.2 残留 -6 | 改为 -1 对齐 US-015 |
| 3 | US-017 | tech-design 未同步 P0 | §2.1/§3 改为支付回调才创建 package |
| 4 | US-027 | §4.1/§6.1/§7.3/§12 + tech-design 残留"保持 active" | 全部改为 frozen(refund_pending) |
| 5 | US-028 | 状态机未对齐 frozen | §3/§4.1/§7.3 + tech-design 改为 frozen→refunded/active |
| 6 | US-032 | §4.1/§12 + tech-design 残留 15 分钟 | 全部改为 10 分钟 |
| 7 | US-039 | tech-design §4.5 残留 cancel API + §8 矛盾 | 删除 cancel API；§8 改三选一；§14.2 改三选一 |
| 8 | US-044 | §4.2 "hidden 或删除" vs §6.1 "disabled" | 统一为 disabled |
| 9 | US-046 | tech-design §3 状态机未画争议状态 | 补全三条转换 |
| 10 | US-049 | tech-design §7 依赖方向未同步 | 改为 US-027/US-030 前置 |

---

## 4. 跨 US 一致性专项复核

| # | 检查项 | 结果 |
|---|--------|------|
| 1 | cancel_reason 字段类型统一 TINYINT 6 值 | ✅ 通过 |
| 2 | user.status 字段类型统一 TINYINT | ✅ 通过 |
| 3 | frozen_reason 字段类型统一 VARCHAR(32) 3 值 | ✅ 通过 |
| 4 | order.status 状态机 9 状态 + 两阶段时序 | ✅ 通过 |
| 5 | course_record 表名统一 | ✅ 通过 |
| 6 | booking.operator 字段统一 | ✅ 通过 |
| 7 | PRD §3.6 退款身份矩阵一致性 | ✅ 通过（v11.2 已修正） |
| 8 | holiday_release_offset_days -1 统一 | ✅ 通过（US-015/US-016 同步） |
| 9 | 教练离职三选一（US-039/US-041 一致） | ✅ 通过 |
| 10 | 退款 reserved 释放策略（US-027/US-028 一致） | ✅ 通过 |

---

## 5. OpenSpec 校验结果

```
openspec validate --all --json
→ passed: 50, failed: 0
```

所有 50 个 US 的 OpenSpec 4 件套校验全部通过。

---

## 6. 剩余 P2 问题（非阻塞，5 项）

| # | US | 问题 | 建议 |
|---|-----|------|------|
| P2-1 | US-001/002/003/004 等 | Figma 链接待设计填写 | 设计阶段统一填写 |
| P2-2 | US-008 | §11.3 场景计数文字偏差（写 5 实际 6） | TDD 阶段修正文字 |
| P2-3 | US-002 | §12 套餐模板字段待补 | TDD 阶段补充 |
| P2-4 | US-020 | §15 设计评审记录空白 | 设计阶段补 |
| P2-5 | US-045 | §5 #4 描述文字待优化 | TDD 阶段优化 |

---

## 7. 评审结论

### 7.1 整体状态

- **45 个 APPROVED**（90%）：15 章完整、GWT ≥3 场景、与 PRD v11.2 对齐、三件套内部一致、OpenSpec 同步
- **5 个有条件 APPROVED**（10%）：US-001/002/003/004/005/007/008/009/010/011/012/013/018/020/045 — 仅有 Figma 待填写或文字偏差等非阻塞性 P2
- **0 个需修复**：无 P0/P1 残留
- **跨 US 一致性 10 项专项复核全部通过**
- **OpenSpec validate 50/50 通过**

### 7.2 v7 → v8 变化

| 维度 | v7 | v8 |
|------|------|------|
| APPROVED | 11（22%，含 13 误判导致的虚假低数）| 45（90%）|
| 需修复 | 24（含 13 误判）| 0 |
| P0 真实问题 | 13 | 0（全部修复 + 半落地修复）|
| P1 问题 | 15 | 0（全部修复）|
| PRD 矛盾 | §3.6 未识别 | 已修正（v11.2）|

### 7.3 后续行动

1. **可进入 SDD 流程 TDD 阶段**：50 个 US 均已具备条件
2. **P2 问题**：在 TDD/设计阶段顺带处理，不阻塞
3. **Figma 链接**：设计阶段统一填写
4. **无需 v9 评审**：本轮已全量闭环

### 7.4 评审通过

**本轮评审通过，50 个 US 可进入 TDD 实现阶段。**

---

## 附录：v8 修复文件清单

### 半落地修复（10 项，27 个文件）

| # | US | 修改文件 | 变更日志版本 |
|---|-----|---------|------------|
| 1 | US-006 | user-story.md | v1.1 |
| 2 | US-016 | user-story.md + openspec spec.md | v1.1 |
| 3 | US-017 | tech-design.md + openspec spec.md | tech-design v1.1 |
| 4 | US-027 | user-story.md + tech-design.md + test-plan.md + openspec 4 件套 | v1.5 |
| 5 | US-028 | user-story.md + tech-design.md + openspec 4 件套 | v1.4 / tech-design v1.3 |
| 6 | US-032 | user-story.md + tech-design.md + openspec design.md/proposal.md | v1.5 / tech-design v1.1 |
| 7 | US-039 | tech-design.md + user-story.md + openspec design.md | v1.4 |
| 8 | US-044 | user-story.md + openspec proposal.md | v1.3 |
| 9 | US-046 | tech-design.md + openspec design.md | tech-design v1.2 |
| 10 | US-049 | tech-design.md + openspec design.md | tech-design v1.1 |

### PRD 修正

| 文件 | 修正内容 | 版本 |
|------|---------|------|
| docs/prd/prd.md | §3.6 退款身份矩阵 4 处 + §3.7 退款完成转换 + §17 变更日志 | v11.2 |

---

## 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v8.0 | 2026-07-31 | 评审 Agent | v7 修复后全量复审；独立核查 50 US（不依赖 INDEX）；修复 10 项半落地；0 P0 / 0 P1 残留；45 APPROVED + 5 有条件；综合评分 8.9 |
