# US-022 学员更换绑定教练

> **状态**：[REVIEW]（评审中）
> **优先级**：[MVP]
> **估时**：1 人天
> **作者**：PM　|　**最后更新**：2026-07-31
> **配套文档**：Figma：[待设计填写]　·　技术设计：[./tech-design.md](./tech-design.md)　·　测试计划：[./test-plan.md](./test-plan.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-022 |
| **标题** | 学员更换绑定教练 |
| **角色（Actor）** | 学员（主）、系统（辅）|
| **业务价值（Why）** | 允许学员在必要时更换教练，按规则退旧购新，保持单教练约束 |
| **优先级** | [MVP] |
| **估时** | 1 人天 |
| **配套文档** | Figma / 技术设计 / 测试计划（链接见上方）|

---

## 2. 触发条件

- **触发方**：学员
- **触发动作**：在「我的套餐」页点击「更换教练」并选择新教练提交
- **触发时机**：用户主动发起换教练时

---

## 3. 前置条件

- [x] 用户已登录（依赖 US-004 / US-005）
- [x] 用户当前为学员且持有 active 套餐（依赖 US-020 / US-021）
- [x] 用户无已预约未上课的课程（或已取消全部预约）

---

## 4. 业务流程

> **硬约束**（PRD §6.7 line 1008-1010）：退订与购买**不并发**，必须拆分为两步独立操作。学员必须先完成「冻结旧教练套餐并生成待处理退款」（步骤 1-3），再独立发起「购买新教练套餐」（步骤 4-5），两步必须在 24 小时内完成；超时未发起第二步，第一步自动回滚，旧套餐恢复为第一步前的原始状态（active/exhausted/expired），待处理退款记录取消。

### 4.1 主路径

1. 学员进入「我的套餐」页，点击「更换教练」
2. 系统展示当前绑定教练及可更换教练列表，并**强引导提示**「更换流程为两步：先冻结当前教练套餐并生成待处理退款，再购买新教练套餐；两步必须在 24 小时内完成，否则第一步自动回滚」
3. **第一步 — 冻结旧套餐并生成待处理退款**：
   - 学员确认退订当前教练套餐
   - 系统按 §6.4 公式计算旧套餐退款金额
   - 系统将旧套餐标记为 frozen，frozen_reason = refund_pending（复用 PRD §5.5.1.2 的 `refund_pending` 枚举值，换教练视为待处理退款，禁止新增枚举），释放所有 reserved 课时，并记录变更前原始状态（active/exhausted/expired）
   - 系统创建 refund_record，status = pending_change，记录退款金额与 original_package_status
   - 用户身份回退为注册用户
   - 系统启动 24 小时有效期窗口
   - 系统返回退款详情，前端提示「退订完成，请在 24 小时内购买新教练套餐」
4. **第二步 — 购买新套餐**（独立入口，必须在第一步完成后 24 小时内发起）：
   - 学员选择新教练并确认套餐（标准套餐或自定义课时）
   - 系统校验：新教练 status=1、与当前教练不同、用户同意协议、第二步在 24 小时有效期内、存在 status = pending_change 的换教练退款记录
   - 校验通过后，系统正式处理旧套餐退款：旧套餐 status → refunded，refund_record.status 更新为 approved
   - 系统创建新教练的待支付订单（order.status=待支付，course_type=1）
   - 前端跳转支付页（US-025）
5. 支付成功后（US-025 回调），用户身份重算为学员

### 4.2 异常分支

- **分支 1**（第二步）：新教练 status ≠ 1 → 返回错误码 `COACH_UNAVAILABLE`
- **分支 2**（第二步）：新教练与当前教练相同 → 返回错误码 `SAME_COACH`
- **分支 3**（第一步）：用户无 active 套餐 → 返回错误码 `NO_ACTIVE_PACKAGE`
- **分支 4**（第一步）：旧套餐存在 reserved 课时 → 返回错误码 `PENDING_BOOKINGS`，引导先取消预约（US-030）
- **分支 5**（第二步）：用户尚未退订旧套餐（仍持有 active 套餐）→ 返回错误码 `ACTIVE_PACKAGE_EXISTS`，提示先完成退订
- **分支 6**（第二步）：第二步距第一步超过 24 小时 → 自动触发回滚：旧套餐恢复为第一步前的原始状态（active/exhausted/expired），取消/删除 status = pending_change 的 refund_record，身份按当前 active 套餐重算（若恢复后仍持有有效套餐则恢复学员，否则保持注册用户），返回错误码 `COACH_CHANGE_EXPIRED`，提示「更换流程已超时，请重新发起退订」

---

## 5. 业务规则引用

| # | 规则 | 章节 |
|---|------|------|
| 1 | **退订 + 购买不并发**（两步独立操作硬约束） | [§6.7 line 1008-1010](../../prd/prd.md) |
| 2 | 更换绑定教练规则 | [§6.7](../../prd/prd.md) |
| 3 | 换教练退款按 §6.4 计算 | [§6.4](../../prd/prd.md) |
| 4 | 单教练约束 | [§3.5](../../prd/prd.md) / [§4.4](../../prd/prd.md) |
| 5 | 用户身份状态机 | [§3.2](../../prd/prd.md) |

---

## 6. 验收标准（业务级 Gherkin）

### 6.1 场景 1：第一步 — 正常冻结旧教练套餐并生成待处理退款

```gherkin
Given 学员已绑定教练 A 且持有 1 个 active 套餐（10 节，已用 2 节）
And   学员已取消所有预约（reserved=0）
When  学员在「更换教练」流程中确认退订教练 A 套餐
Then  旧套餐 status = frozen，frozen_reason = refund_pending，reserved_count = 0，记录原始状态 active
And   创建 refund_record，status = pending_change，refund_amount = 实付金额 × (10-2)/10，original_package_status = active
And   用户身份变为注册用户
And   系统返回退款详情与 24 小时有效期
And   前端提示"退订完成，请前往购买新教练套餐"
And   接口返回 HTTP 200
```

### 6.2 场景 2：第二步 — 正常购买新教练套餐

```gherkin
Given 学员已完成第一步冻结（身份为注册用户，旧套餐 status = frozen，存在 status = pending_change 的 refund_record）
And   教练 B 状态为已通过（status=1）
And   第二步在 24 小时有效期内
When  学员选择教练 B 的 8 节标准套餐并提交购买
Then  旧套餐 status = refunded，refund_record.status = approved
And   系统创建教练 B 的待支付订单 order.status = 待支付
And   接口返回 HTTP 201 与新订单信息
And   前端跳转支付页（US-025）
```

### 6.3 场景 3：第二步未退订直接购新（违反两步硬约束）

```gherkin
Given 学员仍持有教练 A 的 active 套餐（尚未退订）
And   教练 B 状态为已通过（status=1）
When  学员尝试直接购买教练 B 的套餐
Then  系统返回 HTTP 400，错误码 ACTIVE_PACKAGE_EXISTS
And   文案提示"您尚有未退订的套餐，请先完成退订后再购买新套餐"
And   不创建新订单
```

### 6.4 场景 4：第二步新教练不可用

```gherkin
Given 学员已完成第一步冻结（旧套餐 status = frozen，存在 status = pending_change 的 refund_record）
And   教练 C 状态为待审核（status=0）
And   第二步在 24 小时有效期内
When  学员尝试购买教练 C 的套餐
Then  系统返回 HTTP 400，错误码 COACH_UNAVAILABLE
And   不创建新订单
And   旧套餐保持 frozen，refund_record 保持 pending_change
```

### 6.5 场景 5：第一步旧套餐存在未取消预约

```gherkin
Given 学员已绑定教练 A 且旧套餐 reserved=1
When  学员尝试退订教练 A 套餐
Then  系统返回 HTTP 400，错误码 PENDING_BOOKINGS
And   文案提示"您有未上课的预约，请先取消后再退订"
And   不修改旧套餐
```

### 6.6 场景 6：第二步超过 24 小时未发起触发回滚

```gherkin
Given 学员已完成第一步冻结（旧套餐 status = frozen，frozen_reason = refund_pending，原始状态 active）
And   存在 refund_record.status = pending_change
And   距第一步完成已超过 24 小时
When  学员尝试进入第二步购买新教练套餐
Then  系统自动回滚第一步：旧套餐恢复 active，refund_record 被取消/删除
And   用户身份恢复学员
And   系统返回 HTTP 400，错误码 COACH_CHANGE_EXPIRED
And   文案提示"更换流程已超时，请重新发起退订"
And   不创建新教练订单
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

| # | 表名 | 操作 | 说明 |
|---|------|------|------|
| 1 | `package` | 修改 | 第一步 status=frozen，frozen_reason=refund_pending，reserved→0，记录 original_status；第二步成功后 status=refunded；超时回滚恢复 original_status |
| 2 | `order` | 新增 | 新教练待支付订单 |
| 3 | `refund_record` | 新增 | status=pending_change → approved（第二步成功）/ 取消（超时回滚），记录退款金额与 original_package_status |
| 4 | `user` | 修改 | 第一步 identity 学员 → 注册用户；第二步支付成功或超时回滚后重算 identity |
| 5 | `coach` | 读取 | 校验新教练状态 |
| 6 | `booking` | 读取 | 校验无 reserved 预约 |

### 7.2 API 影响

| # | API | 方法 | 操作 | 说明 |
|---|-----|------|------|------|
| 1 | `/api/users/me/coach/unsubscribe` | POST | 新增 | **第一步**：冻结旧教练套餐（status=frozen，frozen_reason=refund_pending）+ 创建 pending_change 退款记录 + 释放 reserved + 身份回退 |
| 2 | `/api/users/me/coach/purchase` | POST | 新增 | **第二步**：校验 24 小时有效期与 pending_change 退款记录，正式退款旧套餐（status=refunded，refund_record=approved），创建新教练待支付订单 |
| 3 | `/api/coaches/available-for-change` | GET | 新增 | 可更换教练列表 |

> 注：原单一 `/api/users/me/coach/change` 已按 PRD §6.7 拆分为两个独立端点，退订与购买不并发。

### 7.3 状态机影响

| # | 实体 | 转换 | 触发条件 | 说明 |
|---|------|------|---------|------|
| 1 | `package` | active → frozen | 第一步冻结旧套餐 | frozen_reason=refund_pending，记录 original_status |
| 2 | `package` | frozen → refunded | 第二步购买校验通过 | 正式完成旧教练退款 |
| 3 | `package` | frozen → original_status（active/exhausted/expired） | 超时未发起第二步 | 回滚第一步 |
| 4 | `refund_record` | 无 → pending_change | 第一步 | 记录退款金额与 original_package_status |
| 5 | `refund_record` | pending_change → approved | 第二步购买校验通过 | 随 package 转为 refunded 同步更新 |
| 6 | `refund_record` | pending_change → 取消/删除 | 超时未发起第二步 | 回滚第一步 |
| 7 | `user` | 学员 → 注册用户 | 第一步完成（旧套餐全部 frozen） | 必经中转 |
| 8 | `order` | 无 → 待支付 | 第二步购买 | 新教练订单（与第一步独立） |

---

## 8. 边界场景

### 8.1 边界场景 1：退款金额计算含已消耗课时

- **触发条件**：旧套餐已消耗 3 节，剩余 7 节
- **预期行为**：退款 = 实付金额 × 7/10
- **用户可见反馈**：页面展示预计退款金额

### 8.2 边界场景 2：多个旧套餐同时冻结

- **触发条件**：用户持有同一教练的 2 个 active 套餐
- **预期行为**：两个套餐分别计算退款金额并标记 frozen，均记录各自的 original_package_status；第二步成功时分别转为 refunded，超时回滚时分别恢复原始状态
- **用户可见反馈**：展示每个套餐的退款明细

### 8.3 边界场景 3：更换过程中并发预约

- **触发条件**：用户提交更换瞬间又成功预约了旧教练课程
- **预期行为**：事务校验发现 reserved>0，拒绝更换
- **用户可见反馈**：提示"检测到新预约，请取消后重试"

### 8.4 边界场景 4：超时回滚恢复原始状态非默认 active

- **触发条件**：第一步冻结时旧套餐状态为 exhausted（如体验课已用完）或 expired，超时未发起第二步
- **预期行为**：回滚后 package.status 恢复为原状态（exhausted/expired），不会强制变为 active；身份按恢复后的实际套餐状态重算
- **用户可见反馈**：提示"更换流程已超时，请重新发起退订"

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）

- [x] US-004（用户微信授权登录）
- [x] US-005（用户补充注册资料）
- [x] US-020（学员购买正价套餐）
- [x] US-021（学员查看我的套餐）
- [x] US-030（学员取消/改约正价课程，用于释放 reserved）

### 9.2 后续 US（依赖本故事）

- [ ] US-023（学员候补与关注时段）
- [ ] US-029（学员预约正价课程）
- [ ] US-050（系统自动处理套餐过期与课时耗尽）

---

## 10. INVEST 自检

- [x] **I**ndependent（独立）- 依赖已明确的预约取消/购买，但自身可独立交付
- [x] **N**egotiable（可协商）- 退款展示方式、可换教练筛选可协商
- [x] **V**aluable（有价值）- 满足用户换教练需求
- [x] **E**stimable（可估算）- 1 人天明确
- [x] **S**mall（足够小）- 仅覆盖提交更换到生成新订单
- [x] **T**estable（可测试）- 5 个 GWT 场景可客观验证

---

## 11. 完整性检查

### 11.1 字段完整性

- [x] 15 个章节全部填写
- [x] 无"待定"/"TBD"占位符
- [x] 错误码明确

### 11.2 业务规则

- [x] 引用 §6.7 / §6.4 / §3.5 / §4.4 / §3.2
- [x] 与状态机一致

### 11.3 验收标准

- [x] 2 正常 + 4 异常 GWT
- [x] 每个 Then 含具体数值/状态码/DB 字段值
- [x] 业务规则可被验证

### 11.4 配套文档

- [x] 链接明确

---

## 12. 备注

- **幂等键**：第一步用 `{user_id}:{package_id}:unsubscribe`；第二步用 `{user_id}:{new_coach_id}:{order_id}` 作为幂等键
- **事务边界**：第一步（旧套餐冻结 + pending_change 退款记录创建 + 身份回退）在同一事务；第二步（旧套餐正式退款 + 新订单创建）独立事务。两步之间无事务关联。
- **性能要求**：退订接口 P99 < 500ms，购买接口 P99 < 500ms
- **状态快照**：第一步必须记录 package 变更前的原始状态（original_status / original_package_status），超时回滚时按原始状态恢复，不得默认恢复为 active
- **退款记录状态**：换教练专用退款记录 status = pending_change，与 US-027 的待审批退款在业务上隔离；第二步成功时转为 approved，超时回滚时取消/删除
- **frozen_reason 枚举约束**：第一步冻结旧套餐时，`frozen_reason` 必须且只能使用 PRD §5.5.1.2 规定的 `refund_pending`，禁止新增 `coach_change_pending` 等枚举值；回滚时清空 `frozen_reason`
- **两步间隔**：第一步完成后用户须在 24 小时内发起第二步；超时未发起则系统自动回滚第一步（旧套餐恢复 original_status、取消 pending_change 退款记录、身份按实际套餐状态重算），用户需重新发起更换流程
- **事务一致性**：第一步（旧套餐冻结 + pending_change 退款记录 + 身份回退）在同一事务；第二步（旧套餐正式退款 + 新订单创建）独立事务；两步之间的 24 小时窗口与超时回滚由定时任务 + 状态版本号保证

---

## 13. Figma 链接

> Figma **设计系统规范**见 [docs/figma/README.md](../../figma/README.md)。

| # | 内容 | 链接 / node-id | 状态 |
|---|------|---------------|------|
| 1 | 更换教练页 Figma file URL | 🔲 待设计填写 | 🔲 |
| 2 | 退款确认弹窗 Figma file URL | 🔲 待设计填写 | 🔲 |

### 13.1 状态截图清单

> **四态要求**：每个页面必须设计空/加载/错误/成功四种状态。具体样式由设计师根据 [Figma 设计规范](../../figma/README.md) §0.5 主题与 §4 四态模板决定，不在本节指定。

| 页面 | 空状态 | 加载状态 | 错误状态 | 成功状态 | 备注 |
|------|--------|---------|---------|---------|------|
| **更换教练页** | 🔲 | 🔲 | 🔲 | 🔲 | |
| **退款确认弹窗** | — | 🔲 | 🔲 | 🔲 | |

---

## 14. 页面级设计决策

### 14.1 可更换教练列表

- 仅展示 status=1 的教练
- 已绑定教练置灰并提示"当前教练"

### 14.2 退款明细展示

- 展示旧套餐实付金额、已用课时、预计退款金额
- 用户确认后再提交

---

## 15. 设计评审记录

| 日期 | 评审人 | 反馈 | 处置 |
|------|--------|------|------|
| 🔲 | 🔲 | 🔲 | 🔲 |

---

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | PM | 初版 |
| v1.1 | 2026-07-30 | PM | P0 修复：按 PRD §6.7 line 1008-1010 拆分退订+购新为两步独立操作；§4/§5/§6/§7/§12 全部更新；API 拆为 unsubscribe + purchase 两个端点 |
| v1.2 | 2026-07-31 | PM | P0 修复：明确两步操作 24 小时有效期与超时自动回滚机制；§4.1/§4.2/§12 更新 |
| v1.3 | 2026-07-31 | PM | P0 修复：第一步由立即 refunded 改为 frozen + pending_change 退款记录；超时回滚按原始状态恢复；同步更新 §4/§6/§7/§8/§11/§12 |
| v1.4 | 2026-07-31 | PM | P0 修复：frozen_reason 由自定义 `coach_change_pending` 改为复用 PRD §5.5.1.2 枚举 `refund_pending`；同步更新 §4.1/§6/§7/§12 与 OpenSpec 4 件套 |
| v1.5 | 2026-08-01 | PM | §13.1 四态标记统一为 🔲，删除样式描述，添加四态要求说明 |

---

<p align="right">
<sub>本文档遵循 <a href="../../spec/user-story/SPECIFICATION.md">US 拆分规范</a>　·　复制自 <a href="../../spec/user-story/TEMPLATE.md">TEMPLATE.md</a></sub>
</p>
