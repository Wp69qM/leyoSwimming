# US-043 管理员手动冻结/解冻套餐

> **状态**：[REVIEW]（评审中）
> **优先级**：[MVP]
> **估时**：1 人天
> **作者**：PM　|　**最后更新**：2026-07-30
> **配套文档**：Figma：[待设计填写]　·　技术设计：[./tech-design.md](./tech-design.md)　·　测试计划：[./test-plan.md](./test-plan.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-043 |
| **标题** | 管理员手动冻结/解冻套餐 |
| **角色（Actor）** | 管理员（主）、系统（辅） |
| **业务价值（Why）** | 让管理员在投诉处理、异常订单等场景下临时冻结套餐，并在问题解决后解冻，保障平台与学员权益 |
| **优先级** | [MVP] |
| **估时** | 1 人天 |
| **配套文档** | Figma / 技术设计 / 测试计划（链接见上方） |

---

## 2. 触发条件

- **触发方**：管理员
- **触发动作**：在管理后台「用户管理 → 套餐管理」选择套餐并点击「冻结」或「解冻」
- **触发时机**：管理员主动操作

---

## 3. 前置条件

- [x] 管理员已登录且具有 `MANAGE_PACKAGE` 权限
- [x] 目标 package 存在于系统中
- [x] 冻结时 package.status 必须为 active；解冻时 package.status 必须为 frozen

---

## 4. 业务流程

### 4.1 主路径（冻结）

1. 管理员进入「套餐管理」并搜索目标 package
2. 系统展示 package 基本信息与当前状态
3. 管理员点击「冻结」
4. 系统弹出原因选择：投诉处理中 / 异常订单 / 司法冻结（均归入 admin_frozen 细分场景，明细记入 audit_log.remark）
5. 管理员选择原因并确认
6. 系统将 package.status 从 active 更新为 frozen，并写入 frozen_reason = `admin_frozen`（统一枚举，细分原因记录在 audit_log.remark）
7. 系统取消该 package 下所有**未上课**的 booking（status ∈ {已预约, 待上课}），cancel_reason = 6（套餐冻结）；并释放该 package 的 reserved 课时（reserved→available）。已完成 / 已取消 / 旷课的 booking 不受影响
8. 系统记录审计日志
9. 返回冻结成功提示

### 4.2 主路径（解冻）

1. 管理员在「套餐管理」找到 frozen 状态的 package
2. 管理员点击「解冻」
3. 系统二次确认
4. 系统将 package.status 从 frozen 更新为 active
5. 系统清除 frozen_reason
6. 系统记录审计日志
7. 返回解冻成功提示

### 4.3 异常分支

- **分支 1**：管理员无权限 → 返回 `ADMIN_PERMISSION_DENIED`
- **分支 2**：冻结时 package.status ≠ active → 返回 `PACKAGE_NOT_ACTIVE`
- **分支 3**：解冻时 package.status ≠ frozen → 返回 `PACKAGE_NOT_FROZEN`

---

## 5. 业务规则引用

| # | 规则 | 章节 |
|---|------|------|
| 1 | 管理员可手动冻结 package，需选择原因 | [§5.5.1.2](../../prd/prd.md) |
| 2 | frozen 状态统一行为：禁止约课、允许换教练、允许按 frozen 规则申请退款 | [§5.5.1.2](../../prd/prd.md) |
| 3 | 解冻后 package.status 恢复 active，frozen_reason 清除 | [§5.5.1.2](../../prd/prd.md) |
| 4 | 冻结触发场景包括投诉处理中、异常订单、司法冻结 | [§5.5.1.2](../../prd/prd.md) |

> **booking.cancel_reason 字段类型统一说明**（v3 评审 P0 修复）：`cancel_reason` 字段为 TINYINT 整型，全项目枚举值：`1=学员取消 / 2=教练离职 / 3=学员旷课 / 4=场馆闭馆 / 5=教练请假 / 6=套餐冻结`。本 US 冻结时取消已预约课程使用 `6=套餐冻结`。

> **package.frozen_reason 字段类型统一说明**（v3 评审 P0 修复，对齐 PRD §5.5.1.2）：`frozen_reason` 字段为 VARCHAR(32)，全项目统一 3 值枚举：`coach_resigned`（教练离职，系统自动）/ `refund_pending`（退款处理中，US-027/US-028 触发）/ `admin_frozen`（管理员手动冻结，细分原因如投诉处理中/异常订单/司法冻结记录在 audit_log.remark）。本 US 使用 `admin_frozen`。

---

## 6. 验收标准（业务级 Gherkin）

> 本 US 为 L2（1 人天），包含 2 个正常场景 + 3 个异常场景 = 5 个场景。

### 6.1 场景 1：管理员手动冻结套餐

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

### 6.2 场景 2：管理员手动解冻套餐

```gherkin
Given 管理员 M 已登录且具有套餐管理权限
And   学员 S 的 package P1 当前 status = frozen，frozen_reason = "admin_frozen"
When  管理员 M 对 P1 点击「解冻」并确认
Then  package P1 的 status 更新为 active
And   frozen_reason 清空
And   audit_log 新增 1 条 action='ADMIN_UNFREEZE_PACKAGE' 记录
And   返回 HTTP 200 与提示"套餐已解冻"
```

### 6.3 场景 3：无权限管理员操作失败

```gherkin
Given 管理员 M2 已登录但无套餐管理权限
When  管理员 M2 调用冻结/解冻接口
Then  返回错误码 ADMIN_PERMISSION_DENIED
And   HTTP 状态码 403
And   package 状态不发生变更
```

### 6.4 场景 4：冻结非 active 套餐失败

```gherkin
Given 管理员 M 已登录且具有套餐管理权限
And   package P1 当前 status = frozen
When  管理员 M 对 P1 调用冻结接口
Then  返回错误码 PACKAGE_NOT_ACTIVE
And   HTTP 状态码 409
And   package 状态保持 frozen 不变
```

### 6.5 场景 5：解冻非 frozen 套餐失败

```gherkin
Given 管理员 M 已登录且具有套餐管理权限
And   package P1 当前 status = active
When  管理员 M 对 P1 调用解冻接口
Then  返回错误码 PACKAGE_NOT_FROZEN
And   HTTP 状态码 409
And   package 状态保持 active 不变
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

| # | 表名 | 操作 | 说明 |
|---|------|------|------|
| 1 | `package` | 修改 | status、frozen_reason、reserved_count、available_count |
| 2 | `booking` | 修改 | 冻结时取消该 package 下已预约但未上课的课程 |
| 3 | `audit_log` | 新增 | 记录冻结/解冻操作 |

### 7.2 API 影响

| # | API | 方法 | 操作 | 说明 |
|---|-----|------|------|------|
| 1 | `/api/admin/v1/packages/{id}/freeze` | POST | 新增 | 手动冻结套餐 |
| 2 | `/api/admin/v1/packages/{id}/unfreeze` | POST | 新增 | 手动解冻套餐 |

### 7.3 状态机影响

| # | 实体 | 转换 | 触发条件 | 说明 |
|---|------|------|---------|------|
| 1 | package | active → frozen | 管理员冻结 | 需释放 reserved |
| 2 | package | frozen → active | 管理员解冻 | 清除 frozen_reason |
| 3 | booking | 已预约 → 已取消 | 冻结时 | cancel_reason = 6（套餐冻结） |

---

## 8. 边界场景

### 8.1 边界场景 1：并发冻结/解冻同一套餐

- **触发条件**：两个管理员同时对同一 package 执行冻结或解冻
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

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）

- [x] US-020（学员购买正价套餐）—— 产生 package 记录
- [x] US-021（学员查看我的套餐）—— 学员端展示状态
- [x] US-053（管理员账号密码登录）—— 后台管理系统登录认证入口

### 9.2 后续 US（依赖本故事）

- [ ] US-027 / US-028（学员申请退款 / 管理员处理退款）—— frozen package 可触发退款

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
| 1 | 套餐管理列表页 Figma file URL | 🔲 待设计填写 | 🔲 待设计填写 |
| 2 | 冻结/解冻确认弹窗 frame node-id | 🔲 待设计填写 | 🔲 待设计填写 |

### 13.1 状态截图清单

> **四态要求**：每个页面必须设计空/加载/错误/成功四种状态。具体样式由设计师根据 [Figma 设计规范](../../figma/README.md) §0.5 主题与 §4 四态模板决定，不在本节指定。

| 页面 | 空状态 | 加载状态 | 错误状态 | 成功状态 | 备注 |
|------|--------|---------|---------|---------|------|
| **套餐管理列表** | 🔲 | 🔲 | 🔲 | 🔲 | 空状态为无匹配套餐 |

---

## 14. 页面级设计决策

### 14.1 冻结原因选择

- **背景**：不同冻结场景对应不同学员端文案
- **选项**：A. 管理员手动输入；B. 下拉选择固定原因
- **结论**：选择 B，保证文案规范与 frozen_reason 枚举一致
- **影响范围**：冻结确认弹窗

### 14.2 解冻是否需要原因

- **背景**：解冻是恢复行为，是否需要记录
- **选项**：A. 不需要；B. 选填原因
- **结论**：选择 A（MVP），仅记录 audit_log
- **影响范围**：解冻确认弹窗

### 14.3 页面级交互说明

| 交互 | 触发 | 反馈 | 备注 |
|------|------|------|------|
| 点击冻结 | 列表操作列 | 弹出原因选择 + 确认 | active 状态才可用 |
| 点击解冻 | 列表操作列 | 二次确认弹窗 | frozen 状态才可用 |
| 套餐状态变更 | 管理员操作 | 列表行状态徽标实时更新 | |

---

## 15. 设计评审记录

| 日期 | 评审人 | 反馈 | 处置 |
|------|--------|------|------|
| 2026-07-30 | PM | 冻结时是否自动取消已预约课程 | 已在 §4.1 明确释放 reserved 并取消未上课 booking |

---

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | PM | 初版 |
| v1.1 | 2026-07-31 | PM | v3 评审 P0 修复：booking.cancel_reason 统一为整型（6=套餐冻结）；§5 新增 cancel_reason 枚举说明 |
| v1.2 | 2026-07-31 | PM | v3 评审 P0-1 修复：frozen_reason 对齐 PRD §5.5.1.2 统一枚举（coach_resigned/refund_pending/admin_frozen）；§6.1/§6.2 改为 `admin_frozen`；§5 新增 frozen_reason 枚举说明；§4.1 步骤 6 明确 frozen_reason 取值 |
| v1.3 | 2026-07-31 | 开发 | P1-11 修复：§4.1 步骤 7 明确仅取消**未上课**的 booking（status ∈ {已预约, 待上课}），cancel_reason = 6（套餐冻结）；已完成 / 已取消 / 旷课的 booking 不受影响；同步 openspec spec.md REQ-001 |
| v1.4 | 2026-07-31 | PM | §13 Figma 链接清理：预设占位 URL 改为 🔲 待设计填写，待设计师在 Figma Drafts 创建文件后回填真实链接 |

---

<p align="right">
<sub>本文档遵循 <a href="../../spec/user-story/SPECIFICATION.md">US 拆分规范</a>　·　复制自 <a href="../../spec/user-story/TEMPLATE.md">TEMPLATE.md</a></sub>
</p>
