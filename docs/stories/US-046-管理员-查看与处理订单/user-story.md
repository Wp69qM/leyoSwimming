# US-046 管理员查看与处理订单

> **状态**：[REVIEW]（评审中）
> **优先级**：[MVP]
> **估时**：2 人天
> **作者**：PM　|　**最后更新**：2026-08-12
> **配套文档**：Figma：[A-订单管理页](../../figma/page-spec/A-order-management-page.md) / [A-订单详情弹窗](../../figma/page-spec/A-order-detail-page.md)　·　技术设计：[./tech-design.md](./tech-design.md)　·　测试计划：[./test-plan.md](./test-plan.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-046 |
| **标题** | 管理员查看与处理订单 |
| **角色（Actor）** | 管理员（主）、系统（辅）|
| **业务价值（Why）** | 管理员可在后台查看全部财务订单（购买订单 + 退款订单），并对退款订单执行通过/驳回，完成交易与退款闭环 |
| **优先级** | [MVP] |
| **估时** | 2 人天 |
| **配套文档** | Figma / 技术设计 / 测试计划（链接见上）|

---

## 2. 触发条件

- **触发方**：管理员
- **触发动作**：登录 Web 管理后台，进入「订单管理」
- **触发时机**：需要查看订单流水、处理退款订单时

---

## 3. 前置条件

- [x] 管理员已登录且具有「订单管理」权限
- [x] `order` 表已存在购买订单或退款订单（依赖 US-025 / US-027 / 管理员从套餐管理页发起退款）

---

## 4. 业务流程

### 4.1 主路径

1. 管理员进入「订单管理」页面
2. 系统展示订单列表，默认按创建时间倒序，每页 20 条
3. 列表同时展示「购买订单」与「退款订单」，通过 `order.type` 区分
4. 管理员点击某订单查看详情
5. 系统展示订单信息、金额信息、支付/退款信息、关联套餐
6. 若为退款订单且状态为「退款审批中」，管理员可执行：
   - **通过退款**：填写/确认退款金额（可退金额仅作为参考，管理员可基于业务场景调整，需 ≥ 0） → 订单状态变为「退款处理中」→ 调用 Mock 退款渠道 → 回调成功后订单变为「已退款」，关联 package 变为 `refunded`
   - **驳回退款**：填写驳回原因 → 订单状态变为「退款被拒」→ 关联 package 自动恢复为 `active`

### 4.2 退款订单的产生来源

- 用户端提交退款申请（US-027）
- 管理员在「套餐管理」页面对 active 套餐发起退款（本 US 不实现发起界面，但处理其产生的退款订单）
- 系统因教练离职、平台强制等原因自动发起退款

### 4.3 异常分支

- **分支 1**：订单状态非「退款审批中」却执行审批 → 返回 `ORDER_STATUS_INVALID`
- **分支 2**：退款金额 < 0 → 返回 `REFUND_AMOUNT_INVALID`
- **分支 3**：驳回原因为空 → 返回 `REJECT_REASON_REQUIRED`
- **分支 4**：管理员无权限 → 返回 HTTP 403

---

## 5. 业务规则引用

| # | 规则 | 章节 |
|---|------|------|
| 1 | 订单状态机 | [§6.2](../../prd/prd.md) |
| 2 | 退款规则与原路退回 | [§6.4](../../prd/prd.md) |
| 3 | package 状态机 | [§4.2](../../prd/prd.md) |
| 4 | 管理员查看订单与处理退款 | [§5.5.3](../../prd/prd.md) |

---

## 6. 验收标准（业务级 Gherkin）

### 6.1 场景 1：管理员查看订单列表

```gherkin
Given 系统中存在 2 笔已支付购买订单和 1 笔退款审批中订单
When  管理员进入订单管理页
Then  列表展示 3 笔订单，每行展示订单号、用户、教练、关联套餐、金额、类型、状态、创建时间
And   退款订单可操作「通过 / 驳回」
And   购买订单仅可操作「查看」
And   返回 HTTP 200
```

### 6.2 场景 2：管理员查看订单详情

```gherkin
Given 系统中存在一笔 order.type='purchase'，status='已支付' 的订单 O-001
When  管理员点击订单 O-001 查看详情
Then  详情页展示订单信息、金额信息、支付信息、关联套餐
And   不展示通过/驳回按钮
And   返回 HTTP 200
```

### 6.3 场景 3：管理员通过退款订单

```gherkin
Given 存在一笔 order.type='refund'，status='退款审批中' 的订单 R-001
And   对应 package.status='frozen'，frozen_reason='refund_pending'
And   可退金额为 1440 元
When  管理员确认退款金额 1440 元并点击「通过」
Then  系统返回 HTTP 202 Accepted
And   order.status 更新为「退款处理中」
And   调用 Mock 退款渠道成功
When  Mock 渠道异步回调成功
Then  order.status 更新为「已退款」
And   package.status 更新为「refunded」
And   学员收到退款到账通知
```

### 6.4 场景 4：管理员修改退款金额后通过

```gherkin
Given 存在一笔 order.type='refund'，status='退款审批中' 的订单 R-002
And   可退金额为 1440 元
When  管理员将退款金额修改为 1200 元并点击「通过」
Then  系统返回 HTTP 202 Accepted
And   order.paid_amount = 1200 元
And   order.status 更新为「退款处理中」
And   Mock 渠道回调成功后 order.status 更新为「已退款」
```

### 6.5 场景 5：管理员驳回退款订单

```gherkin
Given 存在一笔 order.type='refund'，status='退款审批中' 的订单 R-003
And   对应 package.status='frozen'，frozen_reason='refund_pending'
When  管理员填写驳回原因"资料不足"并点击「驳回」
Then  系统返回 HTTP 200
And   order.status 更新为「退款被拒」
And   order.rejected_reason = "资料不足"
And   package.status 自动恢复为「active」
And   package.frozen_reason 清空
And   学员收到驳回通知
```

### 6.6 场景 6：退款金额为负数

```gherkin
Given 存在一笔 order.type='refund'，status='退款审批中' 的订单 R-004
When  管理员将退款金额修改为 -100 元并点击「通过」
Then  系统返回 HTTP 400
And   返回错误码 REFUND_AMOUNT_INVALID
And   订单与套餐状态不变
```

### 6.7 场景 7：对非退款审批中订单执行审批

```gherkin
Given 存在一笔 order.type='refund'，status='已退款' 的订单 R-005
When  管理员尝试再次点击「通过」
Then  系统返回 HTTP 400
And   返回错误码 ORDER_STATUS_INVALID
And   订单状态不变
```

### 6.8 场景 8：驳回原因必填

```gherkin
Given 存在一笔 order.type='refund'，status='退款审批中' 的订单 R-006
When  管理员未填写驳回原因直接点击「驳回」
Then  系统返回 HTTP 400
And   返回错误码 REJECT_REASON_REQUIRED
And   订单状态不变
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

| # | 表名 | 操作 | 说明 |
|---|------|------|------|
| 1 | `order` | 读取/修改 | 查询订单、更新 status、paid_amount、rejected_reason、approved_by、approved_at |
| 2 | `package` | 读取/修改 | 退款通过后 status → refunded；驳回后 status → active |
| 3 | `refund_record` | 读取 | 展示退款申请原因与证明材料 |
| 4 | `audit_log` | 新增 | 记录管理员审批操作 |
| 5 | `notification` | 新增 | 向学员发送审批结果通知 |

### 7.2 API 影响

| # | API | 方法 | 操作 | 说明 |
|---|-----|------|------|------|
| 1 | `/api/admin/orders/list` | POST | 新增 | 订单列表查询（分页、筛选、排序） |
| 2 | `/api/admin/orders/detail` | POST | 新增 | 订单详情 |
| 3 | `/api/admin/orders/approve-refund` | POST | 新增 | 通过退款订单 |
| 4 | `/api/admin/orders/reject-refund` | POST | 新增 | 驳回退款订单 |

> 按项目 API 规范，统一使用 POST，参数通过 JSON body 传递。

### 7.3 状态机影响

| # | 实体 | 转换 | 触发条件 | 说明 |
|---|------|------|---------|------|
| 1 | `order`（退款订单） | 退款审批中 → 退款处理中 | 管理员通过 | 中间态，等待渠道/Mock 回调 |
| 2 | `order`（退款订单） | 退款处理中 → 已退款 | Mock 退款回调成功 | 终态 |
| 3 | `order`（退款订单） | 退款审批中 → 退款被拒 | 管理员驳回 | 终态；package 自动恢复 active |
| 4 | `package` | frozen(refund_pending) → refunded | 退款回调成功 | 终态 |
| 5 | `package` | frozen(refund_pending) → active | 退款被驳回 | 自动恢复，可继续约课 |

---

## 8. 边界场景

### 8.1 边界场景 1：同一退款订单重复提交

- **触发条件**：管理员连续点击「通过」
- **预期行为**：幂等处理，第二次返回当前状态，不重复生成退款流水
- **用户可见反馈**：提示"该退款已处理"

### 8.2 边界场景 2：Mock 渠道退款回调失败

- **触发条件**：Mock 退款渠道返回失败（模拟异常分支）
- **预期行为**：order.status 从「退款处理中」回滚为「退款审批中」，package.status 保持 frozen，进入重试队列
- **用户可见反馈**：后台提示"退款渠道处理失败，已加入重试队列"

### 8.3 边界场景 3：退款处理中期间学员尝试约课

- **触发条件**：order.status = 退款处理中，package.status = frozen
- **预期行为**：系统阻止新预约，提示"套餐正在退款中，暂不可预约"

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）

- [x] US-025（学员支付套餐订单）—— 产生购买订单
- [x] US-027（学员申请退款）—— 产生退款订单
- [x] US-028（管理员处理退款并原路退回）—— 提供两阶段退款与 Mock 渠道服务
- [x] US-053（管理员账号密码登录）—— 后台管理登录入口

### 9.2 后续 US（依赖本故事）

- [ ] 无

---

## 10. INVEST 自检

- [x] **I**ndependent（独立）- 依赖订单数据已存在，审批 UI 可独立交付
- [x] **N**egotiable（可协商）- 列表字段、审批弹窗细节可协商
- [x] **V**aluable（有价值）- 完成交易与退款闭环
- [x] **E**stimable（可估算）- 2 人天明确
- [x] **S**mall（足够小）- 单个 Sprint 内可完成
- [x] **T**estable（可测试）- 8 个 GWT 场景可验证

---

## 11. 完整性检查

### 11.1 字段完整性

- [x] 15 个章节全部填写
- [x] 无"待定"/"TBD"占位符（除 Figma 链接待设计填写）
- [x] 错误码明确（ORDER_STATUS_INVALID / REFUND_AMOUNT_MISMATCH / REJECT_REASON_REQUIRED / FORBIDDEN）

### 11.2 业务规则

- [x] 引用 §6.2 / §6.4 / §4.2 / §5.5.3
- [x] 与订单/套餐状态机一致
- [x] 退款金额校验规则明确

### 11.3 验收标准

- [x] 5 正常 + 3 异常 GWT
- [x] 每个 Then 含具体数值/状态码/错误码/DB 字段值
- [x] 业务规则可被验证

### 11.4 配套文档

- [x] Figma 链接/状态明确
- [x] 技术设计文档链接/状态明确
- [x] 测试计划链接/状态明确

---

## 12. 备注

- **幂等键**：`{admin_id}:{order_id}:approve-refund` / `{admin_id}:{order_id}:reject-refund`
- **事务边界**：order 状态更新 + package 状态恢复在同一事务；Mock 渠道调用在事务外，失败走补偿
- **MVP Hack**：购买与退款均通过 `MockPaymentProvider` 模拟，不调用真实微信/支付宝接口；购买即时成功，退款管理员通过后即时回调成功（或可控失败用于测试）
- **金额调整**：可退金额仅作为参考，管理员可基于业务场景调整实际退款金额（需 ≥ 0），修改后需记录 audit_log

---

## 13. Figma 链接

> Figma **设计系统规范**见 [docs/figma/README.md](../../figma/README.md)。

| # | 内容 | 链接 / node-id | 状态 |
|---|------|---------------|------|
| 1 | 订单管理页 page-spec | [A-order-management-page.md](../../figma/page-spec/A-order-management-page.md) | ✅ |
| 2 | 订单详情弹窗 page-spec | [A-order-detail-page.md](../../figma/page-spec/A-order-detail-page.md) | ✅ |
| 3 | 订单管理页 Figma file URL | 🔲 待设计填写 | 🔲 |
| 4 | 订单详情弹窗 Figma file URL | 🔲 待设计填写 | 🔲 |
| 5 | 退款审批弹窗 frame node-id | 🔲 待设计填写 | 🔲 |

### 13.1 状态截图清单

> **四态要求**：每个页面必须设计空/加载/错误/成功四种状态。具体样式由设计师根据 [Figma 设计规范](../../figma/README.md) §0.5 主题与 §4 四态模板决定，不在本节指定。

| 页面 | 空状态 | 加载状态 | 错误状态 | 成功状态 | 备注 |
|------|--------|---------|---------|---------|------|
| **订单管理页** | 🔲 | 🔲 | 🔲 | 🔲 | 空状态提示"暂无订单" |
| **订单详情弹窗** | — | 🔲 | 🔲 | 🔲 | 购买订单仅查看；退款订单展示审批按钮 |

---

## 14. 页面级设计决策

### 14.1 订单列表默认按创建时间倒序

- 支持按订单类型（purchase/refund）、订单状态、支付方式、时间范围筛选
- 每页默认 20 条

### 14.2 退款审批入口合并至订单管理

- 不再保留独立的「退款审批」菜单
- 退款订单在订单管理列表中通过「通过 / 驳回」操作处理
- 列表中退款订单使用 Tag 区分，并可按状态筛选

### 14.3 退款金额允许管理员调整

- 通过弹窗展示系统计算的可退金额作为参考
- 管理员可基于业务场景调整实际退款金额（需 ≥ 0）
- 调整后需填写调整原因并记录 audit_log

### 14.4 页面级交互说明

| 交互 | 触发 | 反馈 | 备注 |
|------|------|------|------|
| 点击订单行 | 行点击 | 跳转详情页 | — |
| 通过退款 | 按钮 | 二次确认弹窗 → 状态更新 | 触发 Mock 退款渠道 |
| 驳回退款 | 按钮 | 弹窗要求填写原因 | package 自动恢复 active |
| 查看用户/教练 | 昵称/姓名 | 跳转用户/教练详情 | — |

---

## 15. 设计评审记录

| 日期 | 评审人 | 反馈 | 处置 |
|------|--------|------|------|
| 2026-08-12 | PM/开发 | 明确订单与套餐边界，退款审批合并至订单管理，驳回后 package 自动恢复 active，退款金额可调整 | 已更新 US-046 |

---

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | PM | 初版 |
| v2.0 | 2026-08-12 | PM/开发 | 重构：明确 order 与 package 为独立实体；退款审批合并进订单管理；新增购买/退款订单类型；退款金额允许管理员修改；驳回后 package 自动恢复 active；更新 API 路径为 POST 风格；更新 Gherkin 场景与状态机 |
| v2.1 | 2026-08-13 | PM | 放宽退款金额限制：可退金额仅作参考，管理员可调整实际退款金额（需 ≥ 0），移除 `[0, 可退金额]` 约束；§4.1/§4.3/§6.6/§12/§14.3 同步更新；错误码 `REFUND_AMOUNT_MISMATCH` 改为 `REFUND_AMOUNT_INVALID` |

---

<p align="right">
<sub>本文档遵循 <a href="../../spec/user-story/SPECIFICATION.md">US 拆分规范</a>　·　复制自 <a href="../../spec/user-story/TEMPLATE.md">TEMPLATE.md</a>　·　填写示例见 <a href="../../spec/user-story/EXAMPLE.md">EXAMPLE.md</a></sub>
</p>
