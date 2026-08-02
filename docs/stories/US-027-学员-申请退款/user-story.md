# US-027 学员申请退款

> **状态**：[REVIEW]（评审中）
> **优先级**：[MVP]
> **估时**：1 人天
> **作者**：PM　|　**最后更新**：2026-07-30
> **配套文档**：Figma：[待设计填写]　·　技术设计：[./tech-design.md](./tech-design.md)　·　测试计划：[./test-plan.md](./test-plan.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-027 |
| **标题** | 学员申请退款 |
| **角色（Actor）** | 学员（主）、系统（辅） |
| **业务价值（Why）** | 让学员在合理场景下主动发起退款申请，启动退款审批流程，保障用户资金权益 |
| **优先级** | [MVP] |
| **估时**：1 人天 |
| **配套文档** | Figma / 技术设计 / 测试计划（链接见上方）|

> **补建说明**（P0 修复）：本 US 原本在 INDEX.md 中注册但目录缺失，导致 US-028（管理员处理退款）依赖断裂。现补建三件套，作为 US-028 的前置退款申请入口。

---

## 2. 触发条件

- **触发方**：学员
- **触发动作**：在「我的订单」详情页点击「申请退款」并填写退款原因
- **触发时机**：订单已支付且 package.status ∈ {active, exhausted, expired}；或 package.status = frozen 且 frozen_reason = coach_resigned（教练离职 100% 退款场景，PRD §3.7 / §6.4.5）

---

## 3. 前置条件

- [x] 学员已登录（依赖 US-004 / US-005）
- [x] 存在 order.status = 已支付 的订单（依赖 US-025 / US-026）
- [x] 对应 package.status ∈ {active, exhausted, expired, frozen}（frozen 仅允许教练离职场景 100% 退，PRD §6.4.5；已退款/refunded 不可再申请）
- [x] 订单无正在处理中的退款申请（order.status ∉ {退款审批中, 争议退款处理中, 退款处理中}）

---

## 4. 业务流程

### 4.1 主路径

1. 学员在「我的订单」详情页点击「申请退款」
2. 系统展示退款规则与可退金额（按 §6.4.2 公式计算）
3. 学员选择退款原因：
   - 教练原因（教练离职、教练服务质量）
   - 个人原因（时间冲突、不再学习）
   - 平台原因（系统故障、场馆问题）
4. 学员填写退款说明（可选）并提交
5. 系统事务内执行：
   - 创建 refund_record（status = 待审批）
   - order.status → 退款审批中
   - package.status → frozen（frozen_reason='refund_pending'，PRD §3.6 / §5.5.1.2）
   - 释放全部 reserved_count → 0，自动取消已预约课程（booking.status → 已取消，cancel_reason=1 学员取消，PRD §6.3.1）
   - 触发 US-024 候补转正
6. 系统通知管理员有新退款申请待处理（US-028）
7. 系统通知学员「退款申请已提交，等待管理员审批」

### 4.2 异常分支

- **分支 1**：订单未支付 → 返回 `ORDER_NOT_PAID`
- **分支 2**：package.status = refunded → 返回 `PACKAGE_ALREADY_REFUNDED`
- **分支 3**：package.status = frozen 且 frozen_reason ≠ coach_resigned → 返回 `PACKAGE_FROZEN`（非教练离职原因导致的 frozen 不允许申请退款）
- **分支 4**：存在待处理退款申请 → 返回 `REFUND_IN_PROGRESS`

---

## 5. 业务规则引用

| # | 规则 | 章节 |
|---|------|------|
| 1 | 退款触发场景：已支付且 package.status ∈ {active, exhausted, expired}；教练离职 frozen 可 100% 退 | [§6.4.1](../../prd/prd.md)、[§3.7](../../prd/prd.md) |
| 2 | 退款金额 = 实付金额 × (total_hours - consumed_count) / total_hours | [§6.4.2](../../prd/prd.md) |
| 3 | 退款审批期间 package.status → frozen（refund_pending），释放 reserved 并自动取消已预约课程 | [§3.6](../../prd/prd.md)、[§5.5.1.2](../../prd/prd.md)、[§6.3.1](../../prd/prd.md) |
| 4 | 退款审批期间身份保持学员 | [§3.6](../../prd/prd.md) |
| 5 | 订单状态机：已支付 → 退款审批中 | [§6.2](../../prd/prd.md) |

---

## 6. 验收标准（业务级 Gherkin）

### 6.1 场景 1：正常申请退款

```gherkin
Given 学员已登录
And   存在 order.status = 已支付，package.status = active
And   package.total_hours = 10，consumed_count = 2，paid_amount = 1800 元
And   无待处理退款申请
When  学员选择退款原因"个人原因-时间冲突"并提交
Then  refund_record 创建，status = 待审批
And   refund_amount = 1800 × (10-2)/10 = 1440 元
And   order.status = 退款审批中
And   package.status = frozen（frozen_reason='refund_pending'，PRD §3.6 / §5.5.1.2）
And   package.reserved_count = 0（已释放，PRD §6.3.1）
And   已预约课程 booking.status → 已取消（cancel_reason=1 学员取消）
And   触发 US-024 候补转正
And   系统通知管理员与学员
And   返回 HTTP 201
```

### 6.2 场景 2：套餐已退款再次申请

```gherkin
Given 学员已登录
And   存在 order.status = 已退款，package.status = refunded
When  学员尝试申请退款
Then  系统返回 HTTP 400，错误码 PACKAGE_ALREADY_REFUNDED
And   不创建 refund_record
```

### 6.3 场景 3：存在待处理退款申请重复提交

```gherkin
Given 学员已登录
And   存在 order.status ∈ {退款审批中, 争议退款处理中, 退款处理中}
When  学员再次提交退款申请
Then  系统返回 HTTP 400，错误码 REFUND_IN_PROGRESS
And   不创建新的 refund_record
```

### 6.4 场景 4：教练离职 frozen 套餐允许 100% 退款

```gherkin
Given 学员已登录
And   存在 order.status = 已支付，package.status = frozen
And   package.frozen_reason = coach_resigned
And   package.total_hours = 10，consumed_count = 2，paid_amount = 1800 元
And   无待处理退款申请
When  学员提交退款申请
Then  refund_record 创建，status = 待审批
And   refund_amount = 1800 元（100% 全额退款）
And   order.status = 退款审批中
And   package.status = frozen（frozen_reason='refund_pending'，PRD §5.5.1.2）
And   package.frozen_reason 历史值保留为 coach_resigned（用于 100% 退款计算，PRD §6.4.5）
And   package.reserved_count = 0（已释放，PRD §6.3.1）
And   已预约课程 booking.status → 已取消（cancel_reason=1 学员取消）
And   触发 US-024 候补转正
And   系统通知管理员与学员
And   返回 HTTP 201
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

| # | 表名 | 操作 | 说明 |
|---|------|------|------|
| 1 | `refund_record` | 新增 | 记录退款申请（金额、原因、status=待审批） |
| 2 | `order` | 修改 | status → 退款审批中 |
| 3 | `package` | 修改 | status → frozen（frozen_reason='refund_pending'），reserved_count → 0（已释放，PRD §6.3.1），available_count 恢复对应课时数；教练离职场景保留 frozen_reason 历史值为 coach_resigned 用于 100% 退款计算 |
| 4 | `booking` | 修改 | 已预约 booking → 已取消（cancel_reason=1 学员取消） |
| 5 | `notification` | 新增 | 通知管理员与学员 |

### 7.2 API 影响

| # | API | 方法 | 操作 | 说明 |
|---|-----|------|------|------|
| 1 | `/api/orders/{order_id}/refund` | POST | 新增 | 学员提交退款申请 |
| 2 | `/api/orders/{order_id}/refund/check` | GET | 新增 | 查询可退金额与退款资格 |

### 7.3 状态机影响

| # | 实体 | 转换 | 触发条件 | 说明 |
|---|------|------|---------|------|
| 1 | `order` | 已支付 → 退款审批中 | 学员提交退款 | 启动退款流程 |
| 2 | `package` | active/exhausted/expired → frozen（refund_pending） | 学员提交退款 | status → frozen（PRD §3.6 / §5.5.1.2）；释放 reserved_count → 0；自动取消已预约课程 |
| 3 | `package` | frozen(coach_resigned) → frozen（refund_pending） | 学员提交退款 | 保留 frozen_reason 历史值为 coach_resigned 用于 100% 退款计算；释放 reserved_count → 0；自动取消已预约课程 |

---

## 8. 边界场景

### 8.1 边界场景 1：体验套餐退款金额为 0

- **触发条件**：体验套餐 consumed_count = 1（已用完），total_hours = 1
- **预期行为**：可退金额 = 0，前端提示「本套餐已用完，无可退金额」
- **用户可见反馈**：退款按钮置灰或提示

### 8.2 边界场景 2：套餐已过期但有剩余课时

- **触发条件**：package.status = expired，available > 0
- **预期行为**：允许申请退款（PRD §6.4.1）
- **用户可见反馈**：正常提交退款申请

### 8.3 边界场景 3：并发提交退款

- **触发条件**：学员快速点击两次「提交退款」
- **预期行为**：幂等处理，第二次返回已存在退款申请
- **用户可见反馈**：提示「退款申请已提交，请勿重复操作」

### 8.4 边界场景 4：退款申请立即释放 reserved 课时

- **触发条件**：学员提交退款申请时，package 仍有 reserved_count > 0
- **预期行为**：退款申请创建后立即释放 reserved_count → 0，自动取消已预约课程（booking.status → 已取消，cancel_reason=1 学员取消），触发 US-024 候补转正（PRD §3.6 / §6.3.1）
- **用户可见反馈**：学员端显示「退款申请已提交，等待管理员审批」；已预约课程被取消，约课按钮冻结

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）

- [x] US-004（用户微信授权登录）
- [x] US-005（用户补充注册资料）
- [x] US-025（学员支付套餐订单）
- [x] US-026（学员查看订单列表与详情）

### 9.2 后续 US（依赖本故事）

- [ ] US-028（管理员处理退款并原路退回）—— 处理本 US 提交的退款申请

---

## 10. INVEST 自检

- [x] **I**ndependent（独立）- 依赖 US-025/026，但退款申请逻辑可独立交付
- [x] **N**egotiable（可协商）- 退款原因选项、说明字段可协商
- [x] **V**aluable（有价值）- 保障学员资金权益
- [x] **E**stimable（可估算）- 1 人天明确
- [x] **S**mall（足够小）- 仅覆盖学员侧申请提交
- [x] **T**estable（可测试）- 3 个 GWT 场景可客观验证

---

## 11. 完整性检查

### 11.1 字段完整性

- [x] 15 个章节全部填写
- [x] 无"待定"/"TBD"占位符（除 Figma 链接状态待设计填写）
- [x] 错误码明确

### 11.2 业务规则

- [x] 引用 §6.4.1 / §6.4.2 / §3.4.4 / §3.6 / §6.2
- [x] 退款金额计算公式正确
- [x] 与状态机一致

### 11.3 验收标准

- [x] 1 正常 + 2 异常 = 3 个 GWT 场景（L1 等级）
- [x] 每个 Then 含具体数值/状态码/DB 字段值
- [x] 业务规则可被验证

### 11.4 配套文档

- [x] Figma 链接/状态已列出
- [x] 技术设计文档已创建
- [x] 测试计划已创建

---

## 12. 备注

- **幂等键**：`{user_id}:{order_id}:refund`
- **事务边界**：refund_record 创建 + order 状态更新 + package.status → frozen(refund_pending) + reserved 释放 + booking 取消在同一事务（PRD §3.6 / §6.3.1）
- **性能要求**：退款申请接口 P99 < 300ms
- **通知**：提交后立即通知管理员（微信订阅消息）与学员（短信）
- **教练离职 100% 退款**：`package.frozen_reason = coach_resigned` 时，退款金额 = paid_amount（全额），不按剩余课时比例计算；提交退款后 package.status → frozen（refund_pending），并保留 frozen_reason 历史值为 coach_resigned 用于 100% 退款计算（PRD §6.4.5）
- **reserved 释放时机**：reserved 在本 US 释放（PRD §3.6 / §6.3.1），提交退款申请时立即释放 reserved_count → 0，自动取消已预约课程并触发 US-024 候补转正

---

## 13. Figma 链接

> Figma **设计系统规范**见 [docs/figma/README.md](../../figma/README.md)。

| # | 内容 | 链接 / node-id | 状态 |
|---|------|---------------|------|
| 1 | 退款申请页 Figma file URL | 🔲 待设计填写 | 🔲 |
| 2 | 退款原因选择弹窗 frame node-id | 🔲 待设计填写 | 🔲 |

### 13.1 状态截图清单

> **四态要求**：每个页面必须设计空/加载/错误/成功四种状态。具体样式由设计师根据 [Figma 设计规范](../../figma/README.md) §0.5 主题与 §4 四态模板决定，不在本节指定。

| 页面 | 空状态 | 加载状态 | 错误状态 | 成功状态 | 备注 |
|------|--------|---------|---------|---------|------|
| **退款申请页** | — | 🔲 | 🔲 | 🔲 | 展示可退金额 |
| **退款原因弹窗** | — | 🔲 | 🔲 | 🔲 | |

---

## 14. 页面级设计决策

### 14.1 可退金额展示

- **背景**：用户需清楚知道能退多少钱
- **结论**：在提交前实时展示可退金额计算过程（实付金额 × 剩余课时比例）
- **影响范围**：退款申请页

### 14.2 页面级交互说明

| 交互 | 触发 | 反馈 | 备注 |
|------|------|------|------|
| 查看可退金额 | 进入页面 | 实时计算展示 | 调用 GET /refund/check |
| 提交退款 | 点击按钮 | 二次确认弹窗 → 成功 Toast | 跳转订单详情 |

---

## 15. 设计评审记录

| 日期 | 评审人 | 反馈 | 处置 |
|------|--------|------|------|
| 2026-07-30 | P0 评审 | 补建缺失的 US-027 三件套 | 已执行，作为 US-028 前置依赖 |

---

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | PM | P0 修复：补建缺失的 US-027 三件套 |
| v1.1 | 2026-07-31 | PM | v3 评审 P0 修复：§3 前置条件对齐新状态集（含退款处理中、frozen 教练离职场景）；§6.3 场景 3 对齐 order.status 新状态；§4.2 分支 3 明确教练离职 frozen 可退 |
| v1.2 | 2026-07-31 | PM | P1 修复：§6 增加教练离职 frozen 场景 100% 退款 GWT 场景；§7/§12 补充 frozen_reason 说明 |
| v1.3 | 2026-07-31 | PM | P0 修复：package.status 在退款审批期间保持 active，通过 booking_frozen 冻结约课能力；§2/§3/§4/§6/§7/§12 同步调整 |
| v1.4 | 2026-07-31 | PM | v7 评审 P0 修复：reserved 释放策略对齐 PRD §3.6 / §6.3.1，退款申请提交时立即释放 reserved 并取消已预约课程；package.status → frozen(refund_pending)（PRD §5.5.1.2） |
| v1.5 | 2026-07-31 | PM | 半落地修复：清理 §4.1 step5 / §5 表 / §6.1 场景 1 / §7.3 状态机 / §8.4 边界场景 / §12 备注中残留的"保持 active / reserved 不释放"旧文本，全部对齐 PRD §3.6 v11.2（frozen(refund_pending) + 立即释放 reserved） |
| v1.6 | 2026-08-01 | PM | §13.1 四态标记统一为 🔲，删除样式描述，添加四态要求说明 |

---

<p align="right">
<sub>本文档遵循 <a href="../../spec/user-story/SPECIFICATION.md">US 拆分规范</a>　·　复制自 <a href="../../spec/user-story/TEMPLATE.md">TEMPLATE.md</a></sub>
</p>
