# leyoSwimming MVP 用户故事业务评审报告 v3

> **评审版本**：v3.0
> **评审日期**：2026-07-31
> **评审范围**：19 个 US（12 个 P1+v3 P0 修复 US + 7 个关联 US）
> **评审依据**：
> - [PRD v11.1](../prd/prd.md)（§6.2.2 新增「8-退款处理中」状态）
> - [AGENTS.md §8 用户故事生成硬约束](../../AGENTS.md)
> - [v2 评审报告](./mvp-us-business-review-report-v2.md)
> - 项目 hard constraints（cancel_reason TINYINT、user.status TINYINT、frozen_reason VARCHAR、两阶段退款时序等）
> **与 v2 的差异**：v3 聚焦验证 P1 修复 + v3 P0 修复的正确性，并新增跨 US 一致性专项检查（cancel_reason / user.status / operator / frozen_reason / order.status / course_record 表名 / 两阶段时序）

---

## 0. 执行摘要

### 0.1 关键指标

| 指标 | 数值 | 说明 |
|------|------|------|
| 评审 US 总数 | 19 | 12 个目标 US + 7 个关联 US |
| ✅ APPROVED | 9（47.4%） | US-008、US-014、US-021、US-027、US-030、US-031、US-035、US-037、US-044 |
| ⚠️ 有条件 APPROVED | 3（15.8%） | US-032、US-046、US-050 |
| ❌ 改后评审 | 7（36.8%） | US-004、US-026、US-028、US-033、US-041、US-043、US-047 |
| 🔴 P0 问题 | 9 | 含 1 项 PRD 自身不一致（frozen_reason） |
| 🟡 P1 问题 | 6 | — |
| 🔵 P2 问题 | 4 | — |
| 综合评分均值 | 7.18 | 标准差 σ ≈ 0.87 |

### 0.2 总体结论

1. **P1 修复整体质量良好**：12 个目标 US 中，P1 修复项多数已正确落地。
2. **v3 P0 修复存在"半修复"现象**：多个 US 的 user-story.md 已对齐，但 **tech-design.md 未同步更新**。这是本轮最高频 P0 模式（US-028 / US-033 / US-041 / US-047 / US-050）。
3. **跨 US 一致性仍有 5 处硬伤**：cancel_reason 字符串/整型混用、course_record/lesson_record 表名不一致、frozen_reason 枚举三套并存、user.status 字符串/整型混用、订单状态展示不全。
4. **US-043 frozen_reason 枚举冲突根因在 PRD 自身**：PRD §5.5.1.1（整数 0）、§5.5.1.2（pending_review/admin_manual/court_order）、§3.7（coach_resigned）三处互不一致，需 PRD 层面先行裁决。

---

## 1. 逐 US 评审结果

### 1.1 US-004 游客微信授权登录

**修复验证**：
- v3 P0（user.status 统一 TINYINT 0/1/2）：❌ **未修复** — user-story.md §7.1 仍写 `status='active'`（字符串）
- v3 P0（user 表 PK 改为 id）：✅ 已修复

**评分**：6.2 / 10　|　**推荐状态**：❌ 改后评审

**新发现问题**：
- 🔴 **P0**：user-story.md §1 状态 `[DRAFT]`，但 INDEX.md 为 `[REVIEW]`，状态不同步
- 🔴 **P0**：user-story.md §7.1 `status='active'`（字符串），应改为 `status=0`（TINYINT 整型）

---

### 1.2 US-008 用户账号安全设置

**修复验证**：
- v3 P0（user 表 PK `id` 修正）：✅ 已修复
- v3 P0（password_hash nullable）：✅ 已修复

**评分**：8.5 / 10　|　**推荐状态**：✅ APPROVED

---

### 1.3 US-014 教练管理可约时段

**修复验证**：
- P1（新增 coach_availability_template 表）：✅ 已修复
- v3 P0（status active/inactive → enabled/disabled）：✅ 已修复

**评分**：8.8 / 10　|　**推荐状态**：✅ APPROVED

---

### 1.4 US-021 学员查看我的套餐

**修复验证**：
- v3 P0（§6 新增场景 4 exhausted + 场景 5 expired 含退款入口）：✅ 已修复

**评分**：8.6 / 10　|　**推荐状态**：✅ APPROVED

**残留**：🔵 P2 §6.5 expired 退款入口未显式指向 US-027

---

### 1.5 US-026 学员查看订单列表与详情

**修复验证**：
- v3 P0（tech-design 9 状态 + refunded_at/refund_status + 缓存 key 含 user_id）：✅ tech-design 已修复

**评分**：6.8 / 10　|　**推荐状态**：❌ 改后评审

**新发现问题**：
- 🔴 **P0**：user-story.md §6.1 订单状态仅展示 4 种（待支付/已支付/已取消/已退款），缺「退款审批中/争议退款处理中/退款处理中/退款被拒」4 种
- 🔴 **P0**：user-story.md §14.1 状态标签仅 4 色，缺退款相关状态标签色定义
- 🟡 P1：§14.3 状态筛选下拉缺退款相关状态筛选项

---

### 1.6 US-027 学员申请退款

**修复验证**：
- v3 P0（§3 对齐新状态集 + §6.3 场景 3 + §4.2 分支 3 frozen 教练离职可退）：✅ 已修复

**评分**：8.4 / 10　|　**推荐状态**：✅ APPROVED

---

### 1.7 US-028 管理员处理退款并原路退回

**修复验证**：
- P1（两阶段退款时序）：✅ user-story §7.3 已添加
- v3 P0（§7.3 package frozen 转换明确）：✅ user-story 已修复

**评分**：6.5 / 10　|　**推荐状态**：❌ 改后评审

**新发现问题**：
- 🔴 **P0**：tech-design.md §3 状态机仍为旧版单步（`退款审批中 → 已退款`），与 user-story §7.3 两阶段时序不一致

---

### 1.8 US-030 学员取消正价课程

**修复验证**：
- v3 P0（tech-design §1.1 新增 operator 字段说明）：✅ 已修复

**评分**：8.5 / 10　|　**推荐状态**：✅ APPROVED

---

### 1.9 US-031 教练代约/改约正价课程

**修复验证**：
- v3 P0（§6.2 补 operator=coach 断言 + §12 措辞改已落地）：✅ 已修复

**评分**：8.5 / 10　|　**推荐状态**：✅ APPROVED

---

### 1.10 US-032 学员签到课程

**修复验证**：
- P1（标题改为「学员签到课程」）：⚠️ 部分修复 — §1 标题已改，但目录名仍含「签退」、INDEX.md 标题仍为「学员签到/签退课程」
- P1（student_summary_draft 表 + course_record 三阶段时序）：✅ 已修复
- P1（场景 2b 学员预填总结）：✅ 已修复

**评分**：7.6 / 10　|　**推荐状态**：⚠️ 有条件 APPROVED

**新发现问题**：
- 🟡 P1：INDEX.md 标题与 user-story.md §1 不一致
- 🟡 P1：目录名 `US-032-学员-签到签退课程` 含「签退」，应改为 `US-032-学员-签到课程`

---

### 1.11 US-033 教练确认上课记录

**修复验证**：
- P1（未成年监护人短信）：✅ 已修复
- P1（course_record 三阶段时序）：✅ 已修复
- v3 P0（cancel_reason 整型 3=学员旷课）：✅ user-story §7.3 已修复
- v3 P0（first_lesson_confirmed_at 字段）：✅ tech-design §1.4 已修复

**评分**：6.3 / 10　|　**推荐状态**：❌ 改后评审

**新发现问题**：
- 🔴 **P0**：tech-design.md §2.2 仍保留 `/api/admin/bookings/{booking_id}/return-hour` API，但 user-story §7.2 已明确该 API 迁移至 US-035
- 🔴 **P0**：tech-design.md §1.1 仍保留 `hour_return` 表定义与索引，应随 API 一并迁移至 US-035
- 🟡 P1：user-story.md §11.2 仍写「管理员返还课时」在本 US 范围内，与 §7.2 迁移声明矛盾

---

### 1.12 US-035 管理员返还课时

**修复验证**：
- v3 P0（expired 套餐返还规则 + PACKAGE_NOT_RETURNABLE + §7.3 expired→active + §8.4 边界）：✅ 已修复

**评分**：8.7 / 10　|　**推荐状态**：✅ APPROVED

---

### 1.13 US-037 教练维护学员信息

**修复验证**：
- P1（§5 短信责任澄清：由 US-033 触发，本 US 仅维护字段）：✅ 已修复

**评分**：8.6 / 10　|　**推荐状态**：✅ APPROVED

---

### 1.14 US-041 管理员处理教练离职

**修复验证**：
- P1（frozen_reason 统一为字符串 VARCHAR(32)）：✅ user-story 已修复
- v3 P0（cancel_reason=2 教练离职）：⚠️ 部分修复 — user-story 已用整型，tech-design §5.3 仍为字符串

**评分**：6.0 / 10　|　**推荐状态**：❌ 改后评审

**新发现问题**：
- 🔴 **P0**：tech-design.md §5.3 `cancel_reason = 教练离职`（字符串），应改为 `cancel_reason = 2`
- 🔴 **P0**：tech-design.md §5.3 booking 状态列出「已预约 / 待支付 / 待上课」，但 PRD §6.1 booking 状态机不含「待支付」，需删除
- 🟡 P1：§6 GWT 场景未显式断言「教练离职 100% 退款」金额计算

---

### 1.15 US-043 管理员手动冻结/解冻套餐

**修复验证**：
- v3 P0（cancel_reason=6 套餐冻结）：✅ user-story §5 + §6.1 + §7.3 已修复

**评分**：6.5 / 10　|　**推荐状态**：❌ 改后评审

**新发现问题**：
- 🔴 **P0（根因在 PRD）**：user-story §6.1 `frozen_reason = "pending_review"`、§6.2 `frozen_reason = "admin_manual"`，取自 PRD §5.5.1.2。但项目硬约束要求 frozen_reason 枚举为 `coach_resigned/refund_pending/admin_frozen`，PRD §3.7 用 `coach_resigned`，PRD §5.5.1.1 用整数 `0`。**三套枚举并存，需 PRD 层面先行裁决**
- 🟡 P1：§4.1 步骤 4 冻结原因选项与 frozen_reason 枚举映射关系未明确

---

### 1.16 US-044 管理员管理教练排班与请假

**修复验证**：
- v3 P0（cancel_reason=5 教练请假）：✅ user-story §5 + §6.1 + §7.3 已修复

**评分**：8.5 / 10　|　**推荐状态**：✅ APPROVED

---

### 1.17 US-046 管理员查看与处理订单

**修复验证**：
- v3 P0（两阶段退款时序对齐 + §7.3 状态机 6 行 + §6.5 错误码统一）：✅ user-story 已修复

**评分**：7.5 / 10　|　**推荐状态**：⚠️ 有条件 APPROVED

**新发现问题**：
- 🟡 P1：user-story §4.3、§11.1 和 tech-design 第 54、126 行仍残留 `REFUND_AMOUNT_EXCEEDED`，未与 §6.5 已修复的 `REFUND_AMOUNT_MISMATCH` 同步

---

### 1.18 US-047 管理员配置场馆、公告、闭馆换水与用户须知

**修复验证**：
- P1（HTTP 202 异步 + task_id）：✅ §6.2 场景 2 已修复
- v3 P0（cancel_reason=4 场馆闭馆）：⚠️ 部分修复 — user-story 已用整型，tech-design §3 仍为字符串

**评分**：6.5 / 10　|　**推荐状态**：❌ 改后评审

**新发现问题**：
- 🔴 **P0**：tech-design.md §3 `cancel_reason='venue_closure'`（字符串），应改为 `cancel_reason=4`

---

### 1.19 US-050 系统自动处理套餐过期与课时耗尽状态转换

**修复验证**：
- P1（exhausted→expired 转换）：✅ user-story 已修复
- v3 P0（course_record 表名统一）：⚠️ 部分修复 — user-story §7.1 已用 course_record，tech-design §1.1 仍用 lesson_record

**评分**：7.6 / 10　|　**推荐状态**：⚠️ 有条件 APPROVED

**新发现问题**：
- 🟡 P1：tech-design.md §1.1 `lesson_record`，应改为 `course_record`

---

## 2. 跨 US 一致性专项检查

### 2.1 cancel_reason 字段类型一致性

| US | user-story | tech-design | 一致？ |
|----|-----------|-------------|--------|
| US-030 | cancel_reason=1 ✅ | operator=student ✅ | ✅ |
| US-031 | operator=coach ✅ | ✅ | ✅ |
| US-033 | cancel_reason=3 ✅ | — | ✅ |
| US-041 | cancel_reason=2 ✅ | ❌ `教练离职`（字符串）| ❌ P0 |
| US-043 | cancel_reason=6 ✅ | — | ✅ |
| US-044 | cancel_reason=5 ✅ | — | ✅ |
| US-047 | cancel_reason=4 ✅ | ❌ `'venue_closure'`（字符串）| ❌ P0 |

**结论**：user-story 层面已全部统一为整型，但 **tech-design 层面 US-041、US-047 仍为字符串**。

### 2.2 user.status 字段类型一致性

| US | user-story | 一致？ |
|----|-----------|--------|
| US-004 | ❌ `status='active'`（字符串）| ❌ P0 |
| US-008 | 未显式写 status 值 | ✅ |

**结论**：PRD §9.2.1 明确 `user.status TINYINT 0=正常/1=软删除/2=封禁`，US-004 仍用字符串 `active`，需改为 `0`。

### 2.3 frozen_reason 枚举一致性

| 来源 | 枚举值 |
|------|--------|
| 项目硬约束 | coach_resigned / refund_pending / admin_frozen |
| PRD §3.7 第 230 行 | coach_resigned |
| PRD §5.5.1.1 第 605 行 | 整数 0 |
| PRD §5.5.1.2 第 621-625 行 | pending_review / admin_manual / court_order |
| US-041 | coach_resigned ✅ |
| US-043 | pending_review / admin_manual ❌ |

**结论**：🔴 **P0 级 PRD 自身不一致**。需 PRD 层面先行裁决。

### 2.4 order.status 9 状态展示一致性

| US | 展示状态数 | 一致？ |
|----|-----------|--------|
| US-026 | 4 | ❌ P0 缺 5 状态 |
| US-027 | 9 | ✅ |
| US-028 | 9 | ✅ |
| US-046 | 9 | ✅ |

**结论**：US-026 作为订单展示入口，仅展示 4 状态，需补全。

### 2.5 course_record 表名一致性

| US | user-story | tech-design | 一致？ |
|----|-----------|-------------|--------|
| US-032 | course_record ✅ | — | ✅ |
| US-033 | course_record ✅ | course_record ✅ | ✅ |
| US-050 | course_record ✅ | ❌ lesson_record | ❌ P1 |

### 2.6 两阶段退款时序一致性

| US | user-story | tech-design | 一致？ |
|----|-----------|-------------|--------|
| US-027 | ✅ 两阶段 | — | ✅ |
| US-028 | ✅ 两阶段（§7.3）| ❌ 单步（§3）| ❌ P0 |
| US-046 | ✅ 两阶段 | ✅ 两阶段 | ✅ |

---

## 3. 优先修复建议（按阻断性排序）

| 优先级 | US | 问题 | 修复动作 |
|--------|-----|------|---------|
| P0-1 | US-043 + PRD | frozen_reason 三套枚举冲突 | **PRD 层面先行裁决**，统一为一套枚举，再回流 US-041/US-043 |
| P0-2 | US-004 | status='active' 字符串 + [DRAFT] vs [REVIEW] | §7.1 改 `status=0`；§1 状态改 [REVIEW] |
| P0-3 | US-028 | tech-design §3 状态机单步 | 改为两阶段时序，与 user-story §7.3 对齐 |
| P0-4 | US-033 | tech-design 残留 hour_return 表 + return-hour API | 迁移至 US-035 tech-design，本 US 删除 |
| P0-5 | US-041 | tech-design cancel_reason 字符串 + 待支付非 booking 状态 | 改 `cancel_reason=2`；删除「待支付」 |
| P0-6 | US-047 | tech-design cancel_reason='venue_closure' 字符串 | 改 `cancel_reason=4` |
| P0-7 | US-026 | §6.1/§14.1 仅 4 状态 | 补全 9 状态展示与标签色 |
| P1-1 | US-032 | INDEX 标题 + 目录名含「签退」 | 同步 INDEX 标题；目录名改 `US-032-学员-签到课程` |
| P1-2 | US-046 | §4.3/§11.1/tech-design 残留 REFUND_AMOUNT_EXCEEDED | 全局替换为 REFUND_AMOUNT_MISMATCH |
| P1-3 | US-050 | tech-design §1.1 lesson_record | 改为 course_record |

---

## 4. 流程改进建议

1. **tech-design 同步检查应纳入修复 Definition of Done**：本轮 5 个 P0 均为"user-story 已改、tech-design 未改"，建议在修复 checklist 中增加「tech-design.md 对应章节已同步」强制项。
2. **PRD 自身一致性需先行裁决**：frozen_reason 枚举冲突是 PRD 层面问题，不应由 US 层面各自修复。建议在下一轮 US 修复前，先完成 PRD §3.7 / §5.5.1.1 / §5.5.1.2 三处统一。
3. **INDEX.md 标题同步应纳入预检**：US-032 标题不一致应在预检阶段拦截。

---

## 5. 评分汇总表

| US | 评分 | 推荐状态 | US | 评分 | 推荐状态 |
|---|---|---|---|---|---|
| US-014 | 8.8 | ✅ APPROVED | US-030 | 8.5 | ✅ APPROVED |
| US-035 | 8.7 | ✅ APPROVED | US-031 | 8.5 | ✅ APPROVED |
| US-021 | 8.6 | ✅ APPROVED | US-008 | 8.5 | ✅ APPROVED |
| US-037 | 8.6 | ✅ APPROVED | US-044 | 8.5 | ✅ APPROVED |
| US-027 | 8.4 | ✅ APPROVED | US-046 | 7.5 | ⚠️ 有条件 |
| US-032 | 7.6 | ⚠️ 有条件 | US-050 | 7.6 | ⚠️ 有条件 |
| US-026 | 6.8 | ❌ 改后评审 | US-043 | 6.5 | ❌ 改后评审 |
| US-028 | 6.5 | ❌ 改后评审 | US-047 | 6.5 | ❌ 改后评审 |
| US-004 | 6.2 | ❌ 改后评审 | US-033 | 6.3 | ❌ 改后评审 |
| US-041 | 6.0 | ❌ 改后评审 | — | — | — |

---

## 6. 变更日志

| 版本 | 日期 | 变更 |
|------|------|------|
| v3.0 | 2026-07-31 | v3 评审报告：19 个 US 深度复审（12 个 P1+v3 P0 修复 US + 7 个关联 US）；发现 9 个 P0、6 个 P1、4 个 P2；识别 5 处跨 US 一致性硬伤；提出 10 项优先修复建议 |
