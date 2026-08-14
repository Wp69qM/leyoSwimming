# US-043 管理员-查看与管理用户套餐

> **状态**：[REVIEW]（评审中）
> **优先级**：[MVP]
> **估时**：2 人天
> **作者**：PM　|　**最后更新**：2026-08-13
> **配套文档**：Figma：[A-套餐管理页](../../figma/page-spec/A-package-management-page.md) / [A-套餐详情页](../../figma/page-spec/A-package-detail-page.md)　·　技术设计：[./tech-design.md](./tech-design.md)　·　测试计划：[./test-plan.md](./test-plan.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-043 |
| **标题** | 管理员-查看与管理用户套餐 |
| **角色（Actor）** | 管理员（主）、系统（辅） |
| **业务价值（Why）** | 让管理员集中查看用户购买后的套餐实例，进行冻结、解冻、延期、发起退款等操作，保障平台与学员权益 |
| **优先级** | [MVP] |
| **估时** | 2 人天 |
| **配套文档** | Figma / 技术设计 / 测试计划（链接见上方） |

---

## 2. 触发条件

- **触发方**：管理员
- **触发动作**：在管理后台「套餐订单 → 套餐管理」查看套餐列表/详情，并执行冻结、解冻、延期、发起退款等操作
- **触发时机**：管理员主动操作

---

## 3. 前置条件

- [x] 管理员已登录且具有 `MANAGE_PACKAGE` 权限
- [x] 目标 package 存在于系统中
- [x] 冻结时 package.status 必须为 active；解冻时 package.status 必须为 frozen
- [x] 延期时 package.status 必须为 active 或 expired，且 available_count + reserved_count > 0
- [x] 发起退款时 package.status 必须为 active，package.refund_enabled = true，且未超过 refund_valid_days

---

## 4. 业务流程

### 4.1 主路径（查看套餐列表）

1. 管理员进入「套餐订单 → 套餐管理」
2. 系统展示用户购买后的 package 实例列表，字段均为购买时快照
3. 管理员可通过套餐状态、课程类型、到期时间、关键词筛选
4. 管理员点击「查看」进入套餐详情

### 4.2 主路径（查看套餐详情）

1. 管理员在列表或关联页面点击套餐编号/查看
2. 系统展示 package 基本信息、购买时快照、购买时间、到期时间、课时消耗及消耗使用记录、冻结原因、关联订单等
3. frozen_reason 可能为系统设置的 `coach_resigned`（教练离职）或 `refund_pending`（退款审批中），也可能为管理员手动冻结的 `admin_frozen`；详情页按实际原因展示对应说明
4. 详情页按 package.status 展示可用的操作按钮

### 4.3 主路径（冻结）

1. 管理员在「套餐管理」找到 active 状态的 package
2. 管理员点击「冻结」
3. 系统弹出管理员手动冻结原因选择：投诉处理中 / 异常订单 / 司法冻结（均归入 `frozen_reason = admin_frozen` 细分场景，明细记入 audit_log.remark）
   - 注意：`coach_resigned`（教练离职）和 `refund_pending`（退款审批中）由系统自动设置，不在管理员手动冻结弹窗中选择
4. 管理员选择原因并确认
5. 系统将 package.status 从 active 更新为 frozen，并写入 frozen_reason = `admin_frozen`（统一枚举，细分原因记录在 audit_log.remark）
6. 系统取消该 package 下所有**未上课**的 booking（status ∈ {已预约, 待上课}），cancel_reason = 6（套餐冻结）；并释放该 package 的 reserved 课时（reserved→available）。已完成 / 已取消 / 旷课的 booking 不受影响
7. 系统记录审计日志
8. 返回冻结成功提示

### 4.4 主路径（解冻）

1. 管理员在「套餐管理」找到 frozen 状态的 package
2. 管理员点击「解冻」
3. 系统二次确认
4. 系统将 package.status 从 frozen 更新为 active
5. 系统清除 frozen_reason
6. 系统记录审计日志
7. 返回解冻成功提示

### 4.5 主路径（延期）

1. 管理员在「套餐管理」找到 active 或 expired 状态的 package
2. 管理员点击「延期」
3. 系统校验 package.status ∈ {active, expired} 且 available_count + reserved_count > 0
4. 管理员选择新的 expire_at（必须晚于当前时间）并填写延期原因（必填，最多 200 字）
5. 系统将 package.status 从 expired 更新为 active（若为 expired），更新 expire_at，并记录 extend_reason
6. 系统记录审计日志
7. 返回延期成功提示

### 4.6 主路径（发起退款）

1. 管理员在「套餐管理」找到 active 状态的 package
2. 管理员点击「退款」
3. 系统校验 package.refund_enabled = true 且未超过 refund_valid_days
4. 系统展示退款信息弹窗：套餐名称、总课时、已消耗课时、剩余课时、到期时间、退款比例、系统计算的可退金额
5. 管理员可修改退款金额（不得超过系统计算金额且不能小于 0），修改时需填写调整原因
6. 管理员填写退款原因并确认
7. 系统创建退款订单 order.type='refund'，status='refund_pending'，purchase_order_id 指向原购买订单，paid_amount 为填写的退款金额
8. 系统创建 refund_record，status = 待审批，amount = 填写的退款金额，关联退款订单
9. 系统将 package.status 更新为 frozen，frozen_reason = 'refund_pending'
10. 系统返回创建成功提示，并提示管理员前往「订单管理」审批

### 4.7 异常分支

- **分支 1**：管理员无权限 → 返回 `ADMIN_PERMISSION_DENIED`
- **分支 2**：冻结时 package.status ≠ active → 返回 `PACKAGE_NOT_ACTIVE`
- **分支 3**：解冻时 package.status ≠ frozen → 返回 `PACKAGE_NOT_FROZEN`
- **分支 4**：延期时 package.status ∉ {active, expired} 或 available_count + reserved_count = 0 → 返回 `PACKAGE_NOT_EXTENDABLE`
- **分支 5**：发起退款时 package.status ≠ active 或 refund_enabled = false 或超出 refund_valid_days → 返回 `PACKAGE_NOT_REFUNDABLE`
- **分支 6**：已存在待处理退款订单时再次发起退款 → 返回 `REFUND_PENDING_EXISTS`

---

## 5. 业务规则引用

| # | 规则 | 章节 |
|---|------|------|
| 1 | 管理员可查看所有用户购买后的 package 实例 | [§5.5.1.2](../../prd/prd.md) |
| 2 | package 实例字段为购买时快照，不受模板后续变更影响 | [US-045](../../stories/US-045-管理员-配置标准与自定义套餐/user-story.md) |
| 3 | 管理员可手动冻结 package，需选择原因 | [§5.5.1.2](../../prd/prd.md) |
| 4 | frozen 状态统一行为：禁止约课、允许换教练、允许按 frozen 规则申请退款 | [§5.5.1.2](../../prd/prd.md) |
| 5 | 解冻后 package.status 恢复 active，frozen_reason 清除 | [§5.5.1.2](../../prd/prd.md) |
| 6 | 仅 active 或 expired 且剩余课时 > 0 的 package 可手动延期 | [§4.6](../../prd/prd.md) |
| 7 | 退款由套餐管理发起，生成退款订单后在订单管理审批 | [US-028](../../stories/US-028-管理员-处理退款并原路退回/user-story.md) / [US-046](../../stories/US-046-管理员-查看与处理订单/user-story.md) |
| 8 | 冻结触发场景包括投诉处理中、异常订单、司法冻结 | [§5.5.1.2](../../prd/prd.md) |

> **booking.cancel_reason 字段类型统一说明**（v3 评审 P0 修复）：`cancel_reason` 字段为 TINYINT 整型，全项目枚举值：`1=学员取消 / 2=教练离职 / 3=学员旷课 / 4=场馆闭馆 / 5=教练请假 / 6=套餐冻结`。本 US 冻结时取消已预约课程使用 `6=套餐冻结`。

> **package.frozen_reason 字段类型统一说明**（v3 评审 P0 修复，对齐 PRD §5.5.1.2）：`frozen_reason` 字段为 VARCHAR(32)，全项目统一 3 值枚举：`coach_resigned`（教练离职，系统自动）/ `refund_pending`（退款处理中，US-027/US-028 触发）/ `admin_frozen`（管理员手动冻结，细分原因如投诉处理中/异常订单/司法冻结记录在 audit_log.remark）。本 US 使用 `admin_frozen`。

---

## 6. 验收标准（业务级 Gherkin）

> 本 US 包含 6 个正常场景 + 6 个异常场景 = 12 个场景。

### 6.1 场景 1：管理员查看套餐列表

```gherkin
Given 管理员 M 已登录且具有套餐管理权限
And   系统中存在多个用户购买的 package 实例
When  管理员 M 进入「套餐管理」
Then  系统展示 package 列表
And   列表字段包含：套餐编号、用户、教练、套餐模式、类型、状态、剩余课时、到期时间
And   列表数据按创建时间倒序排列
And   返回 HTTP 200
```

### 6.2 场景 2：管理员查看套餐详情

```gherkin
Given 管理员 M 已登录且具有套餐管理权限
And   学员 S 的 package P1 存在，created_at = "2026-08-01 10:00:00"，expire_at = "2026-09-01 23:59:59"，consumed_count = 2
And   P1 的 2 节消耗课程均有对应的上课/签到记录
When  管理员 M 点击 P1 的「查看」
Then  系统展示 P1 的详情
And   详情包含购买时快照、购买时间、到期时间、课时消耗及消耗使用记录、冻结原因、关联订单等
And   操作按钮按 P1.status 正确显隐
And   返回 HTTP 200
```

### 6.3 场景 3：管理员手动冻结套餐

```gherkin
Given 管理员 M 已登录且具有套餐管理权限
And   学员 S 的 package P1 当前 status = active，reserved_count = 2，available_count = 3
When  管理员 M 对 P1 点击「冻结」并选择原因="投诉处理中"
Then  package P1 的 status 更新为 frozen
And   frozen_reason = "admin_frozen"（细分原因"投诉处理中"记录在 audit_log.remark）
And   reserved_count = 0，available_count = 5
And   audit_log 新增 1 条 action='ADMIN_FREEZE_PACKAGE' 记录
And   返回 HTTP 200 与提示"套餐已冻结"
```

### 6.4 场景 4：管理员手动解冻套餐

```gherkin
Given 管理员 M 已登录且具有套餐管理权限
And   学员 S 的 package P1 当前 status = frozen，frozen_reason = "admin_frozen"
When  管理员 M 对 P1 点击「解冻」并确认
Then  package P1 的 status 更新为 active
And   frozen_reason 清空
And   audit_log 新增 1 条 action='ADMIN_UNFREEZE_PACKAGE' 记录
And   返回 HTTP 200 与提示"套餐已解冻"
```

### 6.5 场景 5：管理员手动延期套餐

```gherkin
Given 管理员 M 已登录且具有套餐管理权限
And   学员 S 的 package P1 当前 status = expired，available_count = 3，reserved_count = 0，expire_at = "2026-08-01"
When  管理员 M 对 P1 点击「延期」，选择新的 expire_at = "2026-09-01"，并填写延期原因 "学员出差一个月"
Then  package P1 的 status 更新为 active
And   expire_at 更新为 "2026-09-01"
And   extend_reason 更新为 "学员出差一个月"
And   audit_log 新增 1 条 action='ADMIN_EXTEND_PACKAGE' 记录
And   返回 HTTP 200 与提示"套餐已延期"
```

### 6.6 场景 6：管理员从套餐管理发起退款

```gherkin
Given 管理员 M 已登录且具有套餐管理权限
And   学员 S 的 package P1 当前 status = active，refund_enabled = true，refund_valid_days = 30，total_hours = 10，consumed_count = 2，paid_amount = 1800，refund_ratio = 1.0
And   对应购买订单 O1 的 order_no = "P202608010001"
And   不存在待处理退款订单
When  管理员 M 对 P1 点击「退款」
And   系统展示可退金额 = 1800 × (10-2)/10 × 1.0 = 1440 元
And   管理员 M 将退款金额修改为 1200 元并填写调整原因="协商一致"
And   填写退款原因="协商退款"并确认
Then  系统创建退款订单 O2，type = 'refund'，status = 'refund_pending'，purchase_order_id 指向 O1，paid_amount = 1200
And   refund_record 创建，status = 待审批，amount = 1200，关联 O2
And   package P1 的 status 更新为 frozen，frozen_reason = 'refund_pending'
And   返回 HTTP 200 与提示"退款订单已生成，请前往订单管理审批"
```

### 6.7 场景 7：无权限管理员操作失败

```gherkin
Given 管理员 M2 已登录但无套餐管理权限
When  管理员 M2 调用套餐管理相关接口
Then  返回错误码 ADMIN_PERMISSION_DENIED
And   HTTP 状态码 403
And   package 状态不发生变更
```

### 6.8 场景 8：冻结非 active 套餐失败

```gherkin
Given 管理员 M 已登录且具有套餐管理权限
And   package P1 当前 status = frozen
When  管理员 M 对 P1 调用冻结接口
Then  返回错误码 PACKAGE_NOT_ACTIVE
And   HTTP 状态码 409
And   package 状态保持 frozen 不变
```

### 6.9 场景 9：解冻非 frozen 套餐失败

```gherkin
Given 管理员 M 已登录且具有套餐管理权限
And   package P1 当前 status = active
When  管理员 M 对 P1 调用解冻接口
Then  返回错误码 PACKAGE_NOT_FROZEN
And   HTTP 状态码 409
And   package 状态保持 active 不变
```

### 6.10 场景 10：延期不符合条件套餐失败

```gherkin
Given 管理员 M 已登录且具有套餐管理权限
And   package P1 当前 status = exhausted
When  管理员 M 对 P1 调用延期接口
Then  返回错误码 PACKAGE_NOT_EXTENDABLE
And   HTTP 状态码 409
And   package 状态保持 exhausted 不变
```

### 6.11 场景 11：发起退款时 package 不满足退款条件失败

```gherkin
Given 管理员 M 已登录且具有套餐管理权限
And   package P1 当前 status = active，refund_enabled = false
When  管理员 M 对 P1 调用退款接口
Then  返回错误码 PACKAGE_NOT_REFUNDABLE
And   HTTP 状态码 409
And   package 状态保持 active 不变
```

### 6.12 场景 12：重复发起退款失败

```gherkin
Given 管理员 M 已登录且具有套餐管理权限
And   package P1 当前 status = frozen，frozen_reason = 'refund_pending'
When  管理员 M 对 P1 调用退款接口
Then  返回错误码 REFUND_PENDING_EXISTS
And   HTTP 状态码 409
And   package 状态保持 frozen 不变
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

| # | 表名 | 操作 | 说明 |
|---|------|------|------|
| 1 | `package` | 读取/修改 | 查询列表/详情、更新 status、frozen_reason、reserved_count、available_count、expire_at |
| 2 | `order` | 读取/新增 | 发起退款时创建退款订单；列表/详情展示关联订单 |
| 3 | `refund_record` | 新增 | 发起退款时创建 |
| 4 | `booking` | 修改 | 冻结时取消该 package 下已预约但未上课的课程 |
| 5 | `audit_log` | 新增 | 记录冻结/解冻/延期/发起退款操作 |

### 7.2 API 影响

| # | API | 方法 | 操作 | 说明 |
|---|-----|------|------|------|
| 1 | `/api/admin/package/list` | POST | 新增 | 查询套餐实例列表 |
| 2 | `/api/admin/package/detail` | POST | 新增 | 查询套餐实例详情 |
| 3 | `/api/admin/package/freeze` | POST | 新增 | 手动冻结套餐 |
| 4 | `/api/admin/package/unfreeze` | POST | 新增 | 手动解冻套餐 |
| 5 | `/api/admin/package/extend` | POST | 新增 | 手动延期套餐；请求体新增 `reason` 字段，必填，最多 200 字 |
| 6 | `/api/admin/package/refund` | POST | 新增 | 发起退款，创建退款订单；支持管理员调整退款金额 |

### 7.3 状态机影响

| # | 实体 | 转换 | 触发条件 | 说明 |
|---|------|------|---------|------|
| 1 | package | active → frozen | 管理员冻结 | 需释放 reserved |
| 2 | package | frozen → active | 管理员解冻 | 清除 frozen_reason |
| 3 | package | expired → active | 管理员延期 | 更新 expire_at |
| 4 | package | active → frozen | 管理员发起退款 | frozen_reason = 'refund_pending' |
| 5 | booking | 已预约 → 已取消 | 冻结时 | cancel_reason = 6（套餐冻结） |
| 6 | order | 无 → refund_pending | 管理员发起退款 | 生成退款订单 |

---

## 8. 边界场景

### 8.1 边界场景 1：并发冻结/解冻/延期同一套餐

- **触发条件**：两个管理员同时对同一 package 执行状态变更操作
- **预期行为**：使用乐观锁，仅第一次成功，第二次返回 `PACKAGE_CONCURRENTLY_UPDATED`
- **用户可见反馈**：第二次提示「套餐状态已被他人更新，请刷新后重试」

### 8.2 边界场景 2：冻结套餐存在即将开始的课程

- **触发条件**：package 下存在 start_time 在 30 分钟内的 booking
- **预期行为**：系统提示「存在即将开始的课程，冻结将自动取消该课程」，管理员确认后继续
- **用户可见反馈**：取消后通知相关学员

### 8.3 边界场景 3：学员端实时展示 frozen 状态

- **触发条件**：管理员冻结/解冻后学员打开「我的套餐」
- **预期行为**：学员端立即看到最新状态与对应文案
- **用户可见反馈**：frozen 时显示"套餐处理中，请等待"或"套餐已冻结，请联系客服"

### 8.4 边界场景 4：延期后套餐状态流转

- **触发条件**：管理员对 expired package 执行延期
- **预期行为**：status 更新为 active，但 reserved_count 不自动恢复（历史上已释放的 reserved 不再补回）
- **用户可见反馈**：详情页显示新的到期时间，状态变为 active

### 8.5 边界场景 5：延期原因缺失或超长

- **触发条件**：管理员提交延期时未填写原因，或原因超过 200 字
- **预期行为**：前端拦截并提示；后端二次校验，返回 `INVALID_EXTENSION_REASON`（400）
- **用户可见反馈**：提示"请输入延期原因，最多 200 字"

### 8.6 边界场景 6：退款订单被驳回后套餐恢复

- **触发条件**：管理员在「订单管理」驳回由本 US 发起的退款订单
- **预期行为**：package.status 从 frozen 自动恢复为 active，frozen_reason 清空
- **用户可见反馈**：套餐详情页状态恢复，可继续预约
- **规则来源**：[US-046](../../stories/US-046-管理员-查看与处理订单/user-story.md)

## 12. 备注

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）

- [x] US-020（学员购买正价套餐）—— 产生 package 记录
- [x] US-021（学员查看我的套餐）—— 学员端展示状态
- [x] US-045（管理员配置标准与自定义套餐）—— package 快照字段来源
- [x] US-053（管理员账号密码登录）—— 后台管理系统登录认证入口

### 9.2 后续 US（依赖本故事）

- [x] US-027 / US-028（学员申请退款 / 管理员处理退款）—— frozen package 可触发退款
- [x] US-046（管理员查看与处理订单）—— 本 US 发起的退款订单需在订单管理页审批

---

## 10. INVEST 自检

- [x] **I**ndependent（独立）- 可独立交付
- [x] **N**egotiable（可协商）- 冻结原因与文案可在设计中调整
- [x] **V**aluable（有价值）- 支撑投诉/异常订单处理
- [x] **E**stimable（可估算）- 1 人天，范围明确
- [x] **S**mall（足够小）- 一个 Sprint 内可完成
- [x] **T**estable（可测试）- 5 个 GWT 场景可客观验证

---

## 11. 完整性检查

### 11.1 字段完整性

- [x] 15 个章节全部填写
- [x] 无"待定"/"TBD"占位符（除 Figma 链接状态待设计填写）
- [x] 错误码明确（ADMIN_PERMISSION_DENIED / PACKAGE_NOT_ACTIVE / PACKAGE_NOT_FROZEN）

### 11.2 业务规则

- [x] 引用 §5.5.1.2
- [x] 与 frozen 状态行为规则一致
- [x] 与审计日志规则一致

### 11.3 验收标准

- [x] 2 正常 + 3 异常 = 5 个 GWT 场景
- [x] 每个 Then 含具体状态码 / DB 字段值
- [x] 业务规则可被验证

### 11.4 配套文档

- [x] Figma 链接/状态已列出
- [x] 技术设计文档已创建
- [x] 测试计划已创建

---

## 12. 备注

- **幂等键**：冻结/解冻接口使用 `admin:package:{package_id}:version:{package.updated_at}` 乐观锁
- **事务边界**：package 状态更新 + reserved 释放 + booking 取消 + 审计日志写入在同一事务
- **性能要求**：冻结/解冻接口 P99 < 300ms
- **通知**：冻结导致课程取消时，需通知学员与教练（调用通知服务，非本 US 核心）

---

## 13. Figma 链接

> Figma **设计系统规范**见 [docs/figma/README.md](../../figma/README.md)。

| # | 内容 | 链接 / node-id | 状态 |
|---|------|---------------|------|
| 1 | 套餐管理列表页 page-spec | [A-package-management-page.md](../../figma/page-spec/A-package-management-page.md) | ✅ |
| 2 | 套餐详情页 page-spec | [A-package-detail-page.md](../../figma/page-spec/A-package-detail-page.md) | ✅ |
| 3 | 套餐管理列表页 Figma file URL | 🔲 待设计填写 | 🔲 |
| 4 | 冻结/解冻确认弹窗 frame node-id | 🔲 待设计填写 | 🔲 |

### 13.1 状态截图清单

> **四态要求**：每个页面必须设计空/加载/错误/成功四种状态。具体样式由设计师根据 [Figma 设计规范](../../figma/README.md) §0.5 主题与 §4 四态模板决定，不在本节指定。

| 页面 | 空状态 | 加载状态 | 错误状态 | 成功状态 | 备注 |
|------|--------|---------|---------|---------|------|
| **套餐管理列表** | 🔲 | 🔲 | 🔲 | 🔲 | 空状态为无匹配套餐 |

---

## 14. 页面级设计决策

### 14.1 套餐管理与订单管理的职责边界

- **背景**：退款涉及 package 和 order 两个实体，页面职责需清晰
- **结论**：套餐管理页负责「发起退款」（创建退款订单并冻结套餐），管理员可在发起时查看并调整退款金额；订单管理页负责「审批/驳回退款」及原路退回；若发起时已调整金额，订单管理页仍可在审批时再次调整
- **影响范围**：两个页面的操作按钮与跳转逻辑

### 14.2 冻结原因选择

- **背景**：不同冻结场景对应不同学员端文案
- **选项**：A. 管理员手动输入；B. 下拉选择固定原因
- **结论**：选择 B，保证文案规范与 frozen_reason 枚举一致
- **影响范围**：冻结确认弹窗

### 14.3 解冻是否需要原因

- **背景**：解冻是恢复行为，是否需要记录
- **选项**：A. 不需要；B. 选填原因
- **结论**：选择 A（MVP），仅记录 audit_log
- **影响范围**：解冻确认弹窗

### 14.4 退款入口位置

- **背景**：用户可能从套餐列表或套餐详情发起退款
- **选项**：A. 仅在列表；B. 列表和详情均显示；C. 仅在详情
- **结论**：选择 B，列表提供快捷操作，详情提供完整上下文
- **影响范围**：A-套餐管理页、A-套餐详情页

### 14.5 页面级交互说明

| 交互 | 触发 | 反馈 | 备注 |
|------|------|------|------|
| 进入列表 | 菜单/面包屑 | 展示筛选区 + 表格 | 默认按创建时间倒序 |
| 点击查看 | 列表操作列 | 弹出 A-套餐详情弹窗 | 所有状态 |
| 点击冻结 | 列表/详情操作区 | 弹出原因选择 + 确认 | active 状态才可用 |
| 点击解冻 | 列表/详情操作区 | 二次确认弹窗 | frozen 状态才可用 |
| 点击延期 | 列表/详情操作区 | 弹出日期选择 | active/expired 状态才可用 |
| 点击退款 | 列表/详情操作区 | 弹出退款信息填写弹窗，展示课时/金额信息，可编辑退款金额 | active 状态且满足退款条件 |
| 套餐状态变更 | 管理员操作 | 列表/详情状态徽标实时更新 | 操作成功后刷新 |

---

## 15. 设计评审记录

| 日期 | 评审人 | 反馈 | 处置 |
|------|--------|------|------|
| 2026-07-30 | PM | 冻结时是否自动取消已预约课程 | 已在 §4.3 明确释放 reserved 并取消未上课 booking |
| 2026-08-13 | PM | 原 US-043 仅覆盖冻结/解冻，需扩展为完整套餐管理 | 已扩展为「查看与管理用户套餐」，新增列表/详情/延期/发起退款 |

---

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | PM | 初版：管理员手动冻结/解冻套餐 |
| v1.1 | 2026-07-31 | PM | v3 评审 P0 修复：booking.cancel_reason 统一为整型（6=套餐冻结）；§5 新增 cancel_reason 枚举说明 |
| v1.2 | 2026-07-31 | PM | v3 评审 P0-1 修复：frozen_reason 对齐 PRD §5.5.1.2 统一枚举（coach_resigned/refund_pending/admin_frozen）；§6.1/§6.2 改为 `admin_frozen`；§5 新增 frozen_reason 枚举说明；§4.1 步骤 6 明确 frozen_reason 取值 |
| v1.3 | 2026-07-31 | 开发 | P1-11 修复：§4.1 步骤 7 明确仅取消**未上课**的 booking（status ∈ {已预约, 待上课}），cancel_reason = 6（套餐冻结）；已完成 / 已取消 / 旷课的 booking 不受影响；同步 openspec spec.md REQ-001 |
| v1.4 | 2026-07-31 | PM | §13 Figma 链接清理：预设占位 URL 改为 🔲 待设计填写，待设计师在 Figma Drafts 创建文件后回填真实链接 |
| v2.0 | 2026-08-13 | PM | 扩展 US 范围：从「手动冻结/解冻套餐」扩展为「管理员-查看与管理用户套餐」；新增套餐列表/详情/延期/发起退款流程；明确套餐管理与订单管理职责边界；更新 API、验收标准、状态机、依赖关系 |
| v2.1 | 2026-08-13 | PM | 延期流程新增 `extend_reason`：管理员必须填写延期原因，最多 200 字；同步 page-spec A-package-detail-page.md §3.8 |

---

<p align="right">
<sub>本文档遵循 <a href="../../spec/user-story/SPECIFICATION.md">US 拆分规范</a>　·　复制自 <a href="../../spec/user-story/TEMPLATE.md">TEMPLATE.md</a></sub>
</p>
