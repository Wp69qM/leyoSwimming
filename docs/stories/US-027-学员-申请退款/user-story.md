# US-027 学员申请退款

> **状态**：[REVIEW]（评审中）
> **优先级**：[MVP]
> **估时**：1 人天
> **作者**：PM　|　**最后更新**：2026-07-30
> **配套文档**：Figma：[U-我的套餐详情页](../../figma/page-spec/U-my-package-detail-page.md) / [U-退款申请页](../../figma/page-spec/U-refund-apply-page.md)　·　技术设计：[./tech-design.md](./tech-design.md)　·　测试计划：[./test-plan.md](./test-plan.md)

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
- **触发动作**：在「我的套餐」详情页点击「申请退款」并填写退款原因
- **触发时机**：存在已支付的购买订单，且对应 package.status ∈ {active, expired}；或 package.status = frozen 且 frozen_reason = coach_resigned（教练离职 100% 退款场景，PRD §3.7 / §6.4.5）
- **退款资格**：package 快照字段 `refund_enabled = true`，且未超过快照字段 `refund_valid_days` 限制；若 `refund_enabled = false`，前端不展示退款入口或提示「该套餐不支持退款」

---

## 3. 前置条件

- [x] 学员已登录（依赖 US-004 / US-005）
- [x] 存在一笔 order.type = 'purchase' 且 order.status = 已支付 的购买订单（依赖 US-025 / US-026）
- [x] 对应 package.status ∈ {active, expired, frozen}（frozen 仅允许教练离职场景 100% 退，PRD §6.4.5；已退款/refunded 不可再申请；已耗尽/exhausted 不可退款）
- [x] package 快照字段 `refund_enabled = true`；若为 false，直接拦截并提示「该套餐不支持退款」
- [x] 未超过 package 快照字段 `refund_valid_days` 限制（自购买日起算），超过则提示「已超出退款有效期」
- [x] 对应 package 无正在处理中的退款订单（不存在 order.type = 'refund' 且 status ∈ {退款审批中, 争议退款处理中, 退款处理中} 的订单）

---

## 4. 业务流程

### 4.1 主路径

1. 学员在「我的套餐」详情页点击「申请退款」
2. 系统展示退款规则与可退金额（按 package 快照字段计算）：可退金额 = `paid_amount × (total_hours - consumed_count) / total_hours × refund_ratio`，并展示快照字段 `refund_valid_days` 剩余有效期
3. 学员选择退款原因：
   - 教练原因（教练离职、教练服务质量）
   - 个人原因（时间冲突、不再学习）
   - 平台原因（系统故障、场馆问题）
4. 学员填写退款说明（可选）并提交
5. 系统事务内执行：
   - 创建退款订单 order（type = 'refund'，status = 退款审批中，关联原 purchase_order_id 与 package_id）
   - 原购买订单 order.type = 'purchase' 的 status 保持「已支付」不变（购买记录为实际发生金额，不随退款流程改变）
   - 创建 refund_record（status = 待审批，关联退款订单与 package）
   - package.status → frozen（frozen_reason='refund_pending'，PRD §3.6 / §5.5.1.2）
   - 释放全部 reserved_count → 0，自动取消已预约课程（booking.status → 已取消，cancel_reason=1 学员取消，PRD §6.3.1）
   - 触发 US-024 候补转正
6. 系统通知管理员有新退款订单待处理（US-028 / US-046）
7. 系统通知学员「退款申请已提交，等待管理员审批」

### 4.2 异常分支

- **分支 1**：订单未支付 → 返回 `ORDER_NOT_PAID`
- **分支 2**：package.status = refunded → 返回 `PACKAGE_ALREADY_REFUNDED`
- **分支 3**：package.status = exhausted → 返回 `PACKAGE_EXHAUSTED_NOT_REFUNDABLE`，前端不展示退款入口或提示「课时已用完，不支持退款」
- **分支 4**：package.status = frozen 且 frozen_reason ≠ coach_resigned → 返回 `PACKAGE_FROZEN`（非教练离职原因导致的 frozen 不允许申请退款）
- **分支 5**：对应 package 已存在待处理退款订单（order.type = 'refund' 且 status ∈ {退款审批中, 争议退款处理中, 退款处理中}）→ 返回 `REFUND_IN_PROGRESS`
- **分支 6**：package 快照字段 `refund_enabled = false` → 返回 `REFUND_NOT_SUPPORTED`，前端不展示退款入口或提示「该套餐不支持退款」
- **分支 7**：超出 package 快照字段 `refund_valid_days` 有效期 → 返回 `REFUND_EXPIRED`，提示「已超出退款有效期」

---

## 5. 业务规则引用

| # | 规则 | 章节 |
|---|------|------|
| 1 | 退款触发场景：已支付且 package.status ∈ {active, expired}；教练离职 frozen 可 100% 退；exhausted 不可退款 | [§6.4.1](../../prd/prd.md)、[§3.7](../../prd/prd.md) |
| 2 | 退款金额 = package 快照 `paid_amount × (total_hours - consumed_count) / total_hours × refund_ratio`；所有计算字段均取自 package 购买时快照，不受后续 package_template 变更影响 | [§6.4.2](../../prd/prd.md) |
| 3 | 退款审批期间 package.status → frozen（refund_pending），释放 reserved 并自动取消已预约课程 | [§3.6](../../prd/prd.md)、[§5.5.1.2](../../prd/prd.md)、[§6.3.1](../../prd/prd.md) |
| 4 | 退款审批期间身份保持学员 | [§3.6](../../prd/prd.md) |
| 5 | 退款申请创建新的退款订单（order.type = 'refund'），原购买订单 status 保持「已支付」不变 | [§6.2](../../prd/prd.md) |
| 6 | 退款资格与有效期受 package 快照 `refund_enabled`、`refund_valid_days` 控制 | [§6.4.1](../../prd/prd.md) |
| 7 | 体验课套餐（`package_mode = 'experience'`）按业务规则可能有特殊退款策略，需单独校验 | [§6.4.x](../../prd/prd.md) |

---

## 6. 验收标准（业务级 Gherkin）

### 6.1 场景 1：正常申请退款

```gherkin
Given 学员已登录
And   存在一笔 order.type = 'purchase'，order.status = 已支付，对应 package.status = active
And   package 快照 package_mode = 'standard'，total_hours = 10，consumed_count = 2，paid_amount = 1800 元，refund_enabled = true，refund_ratio = 1.0，refund_valid_days = 30
And   无待处理退款订单
When  学员选择退款原因"个人原因-时间冲突"并提交
Then  创建退款订单 order_R001（type = 'refund'，status = 退款审批中，purchase_order_id 指向原购买订单）
And   refund_record 创建，status = 待审批，关联 order_R001
And   refund_amount = 1800 × (10-2)/10 × 1.0 = 1440 元
And   refund_record 关联的 package 快照包含 refund_enabled、refund_ratio、refund_valid_days
And   原购买订单 order.type = 'purchase' 的 status 保持「已支付」不变
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
And   存在已退款的退款订单 order.type = 'refund'，status = 已退款，对应 package.status = refunded
When  学员尝试申请退款
Then  系统返回 HTTP 400，错误码 PACKAGE_ALREADY_REFUNDED
And   不创建 refund_record 与新的退款订单
```

### 6.3 场景 3：存在待处理退款订单重复提交

```gherkin
Given 学员已登录
And   存在对应 package 的一笔退款订单 order.type = 'refund' 且 status ∈ {退款审批中, 争议退款处理中, 退款处理中}
When  学员再次提交退款申请
Then  系统返回 HTTP 400，错误码 REFUND_IN_PROGRESS
And   不创建新的 refund_record 与退款订单
```

### 6.4 场景 4：教练离职 frozen 套餐允许 100% 退款

```gherkin
Given 学员已登录
And   存在一笔 order.type = 'purchase'，order.status = 已支付，对应 package.status = frozen
And   package.frozen_reason = coach_resigned
And   package 快照 package_mode = 'standard'，total_hours = 10，consumed_count = 2，paid_amount = 1800 元，refund_enabled = true，refund_ratio = 1.0，refund_valid_days = 30
And   无待处理退款订单
When  学员提交退款申请
Then  创建退款订单 order_R002（type = 'refund'，status = 退款审批中，purchase_order_id 指向原购买订单）
And   refund_record 创建，status = 待审批，关联 order_R002
And   refund_amount = 1800 元（100% 全额退款，基于快照 paid_amount × refund_ratio）
And   refund_record 关联的 package 快照包含 refund_enabled、refund_ratio、refund_valid_days
And   原购买订单 order.type = 'purchase' 的 status 保持「已支付」不变
And   package.status = frozen（frozen_reason='refund_pending'，PRD §5.5.1.2）
And   package.frozen_reason 历史值保留为 coach_resigned（用于 100% 退款计算，PRD §6.4.5）
And   package.reserved_count = 0（已释放，PRD §6.3.1）
And   已预约课程 booking.status → 已取消（cancel_reason=1 学员取消）
And   触发 US-024 候补转正
And   系统通知管理员与学员
And   返回 HTTP 201
```

### 6.5 场景 5：已耗尽套餐拒绝退款

```gherkin
Given 学员已登录
And   存在 order.status = 已支付，package.status = exhausted
And   package 快照 total_hours = 10，consumed_count = 10，paid_amount = 1800 元，refund_enabled = true，未超过 refund_valid_days
When  学员尝试申请退款
Then  系统返回 HTTP 400，错误码 PACKAGE_EXHAUSTED_NOT_REFUNDABLE
And   前端不展示退款入口或提示「课时已用完，不支持退款」
And   不创建 refund_record
```

### 6.6 场景 6：套餐不支持退款

```gherkin
Given 学员已登录
And   存在 order.status = 已支付，package.status = active
And   package 快照 refund_enabled = false
When  学员尝试申请退款
Then  系统返回 HTTP 400，错误码 REFUND_NOT_SUPPORTED
And   前端不展示退款入口或提示「该套餐不支持退款」
And   不创建 refund_record
```

### 6.7 场景 7：超出退款有效期

```gherkin
Given 学员已登录
And   存在 order.status = 已支付，package.status = active
And   package 快照 refund_enabled = true，refund_valid_days = 7，购买时间距今 8 天
When  学员尝试申请退款
Then  系统返回 HTTP 400，错误码 REFUND_EXPIRED
And   提示「已超出退款有效期」
And   不创建 refund_record
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

| # | 表名 | 操作 | 说明 |
|---|------|------|------|
| 1 | `package_template` | 修改 | 新增字段：`package_mode`（standard/experience）、`teaching_type`、`stroke_ids`、`total_hours`、`duration_minutes`、`valid_days`、`original_price`、`price`、`refund_enabled`、`refund_ratio`、`refund_valid_days`、`tags`、`description`、`images`、`status`（US-045） |
| 2 | `package` | 修改 | 购买时保存模板快照字段：`package_mode`、`total_hours`、`paid_amount`、`refund_enabled`、`refund_ratio`、`refund_valid_days` 等；退款计算与资格判定均基于快照，不受后续 `package_template` 变更影响 |
| 3 | `refund_record` | 新增 | 记录退款申请（金额、原因、status=待审批），并关联 package 快照字段 |
| 4 | `order` | 新增 | 创建退款订单（type = 'refund'，status = 退款审批中，关联原 purchase_order_id 与 package_id）；原购买订单 status 保持「已支付」不变 |
| 5 | `package` | 修改 | status → frozen（frozen_reason='refund_pending'），reserved_count → 0（已释放，PRD §6.3.1），available_count 恢复对应课时数；教练离职场景保留 frozen_reason 历史值为 coach_resigned 用于 100% 退款计算 |
| 6 | `booking` | 修改 | 已预约 booking → 已取消（cancel_reason=1 学员取消） |
| 7 | `notification` | 新增 | 通知管理员与学员 |

### 7.2 API 影响

| # | API | 方法 | 操作 | 说明 |
|---|-----|------|------|------|
| 1 | `/api/packages/{package_id}/refund` | POST | 新增 | 学员从套餐详情页提交退款申请；系统校验 package 归属与状态，创建退款订单（type='refund'，status=退款审批中，关联原 purchase_order_id 与 package_id），原购买订单 status 保持「已支付」不变 |
| 2 | `/api/packages/{package_id}/refund/check` | GET | 新增 | 查询可退金额与退款资格 |

### 7.3 状态机影响

| # | 实体 | 转换 | 触发条件 | 说明 |
|---|------|------|---------|------|
| 1 | `order` | 新建退款订单（type='refund'，status=退款审批中） | 学员提交退款 | 退款订单关联原 purchase_order_id 与 package_id；原购买订单 status 保持「已支付」不变 |
| 2 | `package` | active/expired → frozen（refund_pending） | 学员提交退款 | status → frozen（PRD §3.6 / §5.5.1.2）；释放 reserved_count → 0；自动取消已预约课程 |
| 3 | `package` | frozen(coach_resigned) → frozen（refund_pending） | 学员提交退款 | 保留 frozen_reason 历史值为 coach_resigned 用于 100% 退款计算；释放 reserved_count → 0；自动取消已预约课程 |

---

## 8. 边界场景

### 8.1 边界场景 1：已耗尽套餐不可退款

- **触发条件**：package.status = exhausted（consumed_count = total_hours）
- **预期行为**：系统拒绝退款申请，返回 `PACKAGE_EXHAUSTED_NOT_REFUNDABLE`；前端不展示「申请退款」入口或提示「课时已用完，不支持退款」
- **用户可见反馈**：退款入口置灰或隐藏，提示「课时已用完，不支持退款」

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

### 8.5 边界场景 5：套餐快照 refund_enabled = false

- **触发条件**：package 快照字段 `refund_enabled = false`
- **预期行为**：前端不展示「申请退款」入口；若直接调用接口则返回 `REFUND_NOT_SUPPORTED`
- **用户可见反馈**：提示「该套餐不支持退款」

### 8.6 边界场景 6：超出快照 refund_valid_days 有效期

- **触发条件**：当前时间超过 package 快照 `refund_valid_days` 限制（自购买日起算）
- **预期行为**：不允许提交退款申请，返回 `REFUND_EXPIRED`
- **用户可见反馈**：提示「已超出退款有效期」

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
- [x] **T**estable（可测试）- 7 个 GWT 场景可客观验证

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

- [x] 2 正常 + 5 异常 = 7 个 GWT 场景（L1 等级）
- [x] 每个 Then 含具体数值/状态码/DB 字段值
- [x] 业务规则可被验证

### 11.4 配套文档

- [x] Figma 链接/状态已列出
- [x] 技术设计文档已创建
- [x] 测试计划已创建

---

## 12. 备注

- **幂等键**：`{user_id}:{purchase_order_id}:refund`
- **快照字段**：退款资格（`refund_enabled`、`refund_valid_days`）与金额计算（`paid_amount`、`total_hours`、`refund_ratio`）全部基于 package 购买时快照；模板后续变更不影响已购 package 的退款计算
- **事务边界**：退款订单创建 + refund_record 创建 + package.status → frozen(refund_pending) + reserved 释放 + booking 取消在同一事务（PRD §3.6 / §6.3.1）；原购买订单 status 不随退款流程改变
- **性能要求**：退款申请接口 P99 < 300ms
- **通知**：提交后立即通知管理员（微信订阅消息）与学员（短信）
- **教练离职 100% 退款**：`package.frozen_reason = coach_resigned` 时，退款金额 = 快照 `paid_amount × refund_ratio`（通常 ratio = 1.0，即全额），不按剩余课时比例计算；提交退款后 package.status → frozen（refund_pending），并保留 frozen_reason 历史值为 coach_resigned 用于 100% 退款计算（PRD §6.4.5）
- **reserved 释放时机**：reserved 在本 US 释放（PRD §3.6 / §6.3.1），提交退款申请时立即释放 reserved_count → 0，自动取消已预约课程并触发 US-024 候补转正

---

## 13. Figma 链接

> Figma **设计系统规范**见 [docs/figma/README.md](../../figma/README.md)。

| # | 内容 | 链接 / node-id | 状态 |
|---|------|---------------|------|
| 1 | 我的套餐详情页（含申请退款入口）page-spec | [U-my-package-detail-page.md](../../figma/page-spec/U-my-package-detail-page.md) | ✅ |
| 2 | 退款申请页 page-spec | [U-refund-apply-page.md](../../figma/page-spec/U-refund-apply-page.md) | ✅ |
| 3 | 退款申请页 Figma file URL | 🔲 待设计填写 | 🔲 |

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
- **结论**：在提交前实时展示可退金额计算过程（实付金额 × 剩余课时比例 × 退款比例），所有数值取自 package 购买时快照
- **影响范围**：退款申请页

### 14.2 页面级交互说明

| 交互 | 触发 | 反馈 | 备注 |
|------|------|------|------|
| 查看可退金额 | 进入退款申请页 | 实时计算展示 | 调用 GET /refund/check |
| 提交退款 | 点击按钮 | 二次确认弹窗 → 成功 Toast | 跳转我的套餐详情或订单详情 |

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
| v1.7 | 2026-08-12 | PM | 适配 US-045：明确退款资格与金额计算基于 package 快照字段（refund_enabled、refund_ratio、refund_valid_days）；§2/§3 增加快照资格判定；§4.1/§5 更新退款公式；§4.2 增加 REFUND_NOT_SUPPORTED、REFUND_EXPIRED 分支；§6 补充快照字段断言与两个新场景；§7.1 更新数据表影响；§8 增加体验课与快照边界场景；§12 补充快照字段说明 |
| v1.8 | 2026-08-12 | PM | 适配 US-046 订单/套餐边界重构：退款申请创建独立的退款订单（order.type='refund'），原购买订单 status 保持「已支付」不变；§2/§3/§4.1/§4.2/§5/§6/§7/§12 同步更新 |
| v1.9 | 2026-08-13 | PM | 明确 exhausted 套餐不可退款：§2/§3/§4.2/§5/§7.3/§8.1 移除 exhausted 退款支持；新增异常分支 3 与场景 5 拒绝 exhausted 退款；§10/§11 更新场景数量；同步 OpenSpec 错误码 PACKAGE_EXHAUSTED_NOT_REFUNDABLE |
| v2.0 | 2026-08-13 | PM | 退款入口从「我的订单详情页」调整为「我的套餐详情页」；§2/§4.1/§7.2/§13/§14.2 同步更新；API 路径从 `/api/orders/{order_id}/refund` 改为 `/api/packages/{package_id}/refund`；tech-design.md 同步更新鉴权、缓存、幂等键与错误码；同步 OpenSpec 产物 |

---

<p align="right">
<sub>本文档遵循 <a href="../../spec/user-story/SPECIFICATION.md">US 拆分规范</a>　·　复制自 <a href="../../spec/user-story/TEMPLATE.md">TEMPLATE.md</a></sub>
</p>
