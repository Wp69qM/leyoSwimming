# US-041 管理员处理教练离职

> **状态**：[REVIEW]（评审中）
> **优先级**：[MVP]
> **估时**：1.5 人天
> **作者**：PM　|　**最后更新**：2026-07-30
> **配套文档**：Figma：[待设计填写]　·　技术设计：[./tech-design.md](./tech-design.md)　·　测试计划：[./test-plan.md](./test-plan.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-041 |
| **标题** | 管理员处理教练离职 |
| **角色（Actor）** | 管理员（主）、系统（辅） |
| **业务价值（Why）** | 让管理员按 checklist 审批教练离职，确保学员套餐、未来预约、排班得到妥善处理 |
| **优先级** | [MVP] |
| **估时** | 1.5 人天 |
| **配套文档** | Figma / 技术设计 / 测试计划（链接见上方） |

---

## 2. 触发条件

- **触发方**：管理员
- **触发动作**：在管理后台「用户管理 → 教练离职审批队列」打开工单并审批
- **触发时机**：教练已提交离职工单（ticket.status = pending_audit，依赖 US-039）

---

## 3. 前置条件

- [x] 管理员已登录且具有 `MANAGE_COACH_RESIGNATION` 权限
- [x] 教练已提交离职申请，`coach.status = 4`（申请中）
- [x] 离职工单状态为 `pending_audit`

---

## 4. 业务流程

### 4.1 主路径

1. 管理员进入「教练离职审批队列」
2. 系统列出所有 `pending_audit` 的离职工单
3. 管理员点击目标工单进入详情
4. 系统展示：教练基本信息、工单进度、每份 active 套餐的处理结果
5. 管理员逐项检查 checklist（后端强制校验，缺一不可）：
   - ① active 学员数 = 0；
   - ② 若 active 学员数 > 0，所有 active 套餐已确认全额退款；
   - ③ 教练费已结算（含未消耗课时）；
   - ④ 未来排班已清空。
6. 管理员点击「通过审批」
7. 系统按以下顺序执行（建议在同一事务或 Saga 中按序执行，避免重复释放课时）：
   1. 对所有 active package（含未确认退款的套餐），系统自动生成 100% 待退款记录 `refund_amount = 单价 × 剩余课时`（已消耗不退），并通知学员选择退款或换教练；
   2. 取消该教练所有未来 booking（start_time > NOW() 且 status ∈ 已预约/待上课），释放对应 package 的 reserved 课时；
   3. 将该教练所有 active package 的 reserved_count 归 0，available_count 相应增加（兜底，确保 booking 取消后残留 reserved 被清零）；
   4. 将上述 active package 状态更新为 frozen，frozen_reason = coach_resigned；
   5. 将未来 schedule_slot（start_time > NOW()）更新为 hidden；
   6. 将 `coach.status` 从 4 更新为 3（已离职）。
8. 系统返回审批成功提示

### 4.2 异常分支

- **分支 1**：checklist 未全部通过 → 系统提示「请先完成所有检查项」，不允许通过
- **分支 2**：管理员点击「拒绝审批」 → `coach.status: 4 → 1`，工单 rejected，教练可继续教学
- **分支 3**：工单非 pending_audit 状态 → 返回错误 `TICKET_NOT_PENDING_AUDIT`

---

## 5. 业务规则引用

| # | 规则 | 章节 |
|---|------|------|
| 1 | 管理员检查清单全部通过才可批准离职 | [§5.5.1.1](../../prd/prd.md) |
| 2 | 审批通过后执行批量同步事务：取消未来 booking、释放预占、active→frozen、隐藏未来排班 | [§5.5.1.1](../../prd/prd.md) |
| 3 | 审批拒绝后 `coach.status: 4 → 1`，不回滚已登记的学员处理结果 | [§5.5.1.1](../../prd/prd.md) |
| 4 | 教练离职时清算已消耗课时，学员退款成功后教练费同步扣回 | [§5.5.1.1](../../prd/prd.md) |
| 5 | `package.frozen_reason` 字段类型为 VARCHAR，取值 `'coach_resigned'`（与 PRD §3.7 line 230 数据模型定义一致） | [§3.7](../../prd/prd.md) |

> **frozen_reason 类型统一说明**（P1 修复）：PRD §5.5.1.1 line 605 的示例 SQL 中出现 `frozen_reason = 0`（整型），与 PRD §3.7 line 230 的数据模型定义 `frozen_reason='coach_resigned'`（字符串）矛盾。本 US 统一采用**字符串类型**（与 §3.7 数据模型定义一致，权威性更高）。已忽略 §5.5.1.1 line 605 SQL 示例的整型写法，视为笔误。后续实现时 `package.frozen_reason` 字段为 VARCHAR(32)，取值枚举：`'coach_resigned'` / `'refund_pending'` / `'admin_frozen'`。

---

## 6. 验收标准（业务级 Gherkin）

> 本 US 为 L2（1.5 人天），包含 2 个正常场景 + 3 个异常场景 = 5 个场景。

### 6.1 场景 1：管理员通过离职审批

```gherkin
Given 管理员 M 已登录且具有离职审批权限
And   教练 C 的 coach.status = 4，工单 status = pending_audit
And   教练 C 名下 3 份 active 套餐均已确认全额退款
And   active 学员数 > 0
And   未来排班已清空
When  管理员 M 点击「通过审批」
Then  coach.status 更新为 3（已离职）
And   全额退款套餐自动生成 refund_record，refund_amount = 单价 × 剩余课时（已消耗不退）
And   所有未来 booking 状态更新为「已取消」且 cancel_reason = 2（教练离职）
And   package.reserved_count = 0，available_count 增加对应数值
And   3 份 active package 更新为 frozen，frozen_reason = "coach_resigned"
And   未来 schedule_slot 更新为 hidden
And   返回 HTTP 200 与提示"审批通过"
```

### 6.2 场景 2：管理员拒绝离职审批

```gherkin
Given 管理员 M 已登录且具有离职审批权限
And   教练 C 的 coach.status = 4，工单 status = pending_audit
When  管理员 M 点击「拒绝审批」并填写原因="资料待补充"
Then  coach.status 更新为 1（已通过）
And   ticket.status 更新为 rejected
And   已登记的学员处理结果保持不变
And   返回 HTTP 200 与提示"已拒绝，教练可继续教学"
```

### 6.3 场景 3：active 套餐存在且未全部确认退款时禁止通过

```gherkin
Given 管理员 M 已登录且具有离职审批权限
And   教练 C 的工单中有 2 份 active 套餐未确认全额退款
And   active 学员数 > 0
When  管理员 M 点击「通过审批」
Then  返回错误码 CHECKLIST_NOT_PASSED
And   HTTP 状态码 400
And   coach.status 保持 4，不执行任何批量变更
```

### 6.4 场景 4：未来排班未清空时禁止通过

```gherkin
Given 管理员 M 已登录且具有离职审批权限
And   教练 C 的工单中所有套餐已登记处理结果
And   教练 C 未来 7 天仍有未 hidden 的 schedule_slot
When  管理员 M 点击「通过审批」
Then  返回错误码 SCHEDULE_NOT_CLEARED
And   HTTP 状态码 400
And   coach.status 保持 4
```

### 6.5 场景 5：非 pending_audit 工单不可审批

```gherkin
Given 管理员 M 已登录且具有离职审批权限
And   教练 C 的工单 status = processing（教练尚未提交）
When  管理员 M 调用审批接口
Then  返回错误码 TICKET_NOT_PENDING_AUDIT
And   HTTP 状态码 409
And   coach.status 保持 4
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

| # | 表名 | 操作 | 说明 |
|---|------|------|------|
| 1 | `coach` | 修改 | `status` 4 → 3 或 4 → 1 |
| 2 | `coach_resignation_ticket` | 修改 | status 更新为 approved / rejected |
| 3 | `refund_record` | 新增 | 未消耗剩余课时 100% 退款记录：`refund_amount = 单价 × 剩余课时` |
| 4 | `booking` | 批量修改 | 未来课程取消 |
| 5 | `package` | 批量修改 | reserved→available，active→frozen |
| 6 | `schedule_slot` | 批量修改 | 未来时段 hidden |
| 7 | `audit_log` | 新增 | 记录审批操作与批量变更 |

### 7.2 API 影响

| # | API | 方法 | 操作 | 说明 |
|---|-----|------|------|------|
| 1 | `/api/admin/v1/resignation-tickets` | GET | 新增 | 查询 pending_audit 离职审批队列 |
| 2 | `/api/admin/v1/resignation-tickets/{id}` | GET | 新增 | 查看工单详情 |
| 3 | `/api/admin/v1/resignation-tickets/{id}/approve` | POST | 新增 | 通过审批 |
| 4 | `/api/admin/v1/resignation-tickets/{id}/reject` | POST | 新增 | 拒绝审批 |

### 7.3 状态机影响

| # | 实体 | 转换 | 触发条件 | 说明 |
|---|------|------|---------|------|
| 1 | coach | 4（申请中）→ 3（已离职） | 管理员通过审批 | 批量处理学员套餐与排班 |
| 2 | coach | 4（申请中）→ 1（已通过） | 管理员拒绝审批 | 教练恢复教学 |
| 3 | coach_resignation_ticket | pending_audit → approved | 通过审批 | — |
| 4 | coach_resignation_ticket | pending_audit → rejected | 拒绝审批 | — |
| 5 | package | active → frozen | 通过审批 | frozen_reason = coach_resigned |
| 6 | booking | 已预约 → 已取消 | 通过审批 | cancel_reason = 2（教练离职） |
| 7 | refund_record | 无 → pending | 通过审批 | 所有 active package 未消耗剩余课时 100% 退款：`refund_amount = 单价 × 剩余课时` |

---

## 8. 边界场景

### 8.1 边界场景 1：并发审批同一工单

- **触发条件**：两个管理员同时点击通过审批
- **预期行为**：使用数据库乐观锁或唯一约束，仅第一次成功，第二次返回 `TICKET_ALREADY_PROCESSED`
- **用户可见反馈**：第二次提示「该工单已被处理」

### 8.2 边界场景 2：教练名下有大量 active 套餐

- **触发条件**：教练名下有 100+ 份 active package
- **预期行为**：批量更新分批执行（每批 100 条），整体在一个分布式事务或 Saga 中
- **用户可见反馈**：审批成功后学员端逐步看到 package frozen

### 8.3 边界场景 3：审批时存在进行中的课程

- **触发条件**：审批时刻有 booking 状态为「上课中」
- **预期行为**：仅取消未来（start_time > NOW()）的 booking，进行中的课程不处理，由教练/学员线下收尾
- **用户可见反馈**：系统提示「X 节课正在进行中，未自动取消」

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）

- [x] US-039（教练申请离职）—— 产生 status=4 与 pending_audit 工单
- [x] US-014（教练管理可约时段）—— 提供 schedule_slot 数据

### 9.2 后续 US（依赖本故事）

- [ ] US-040（教练重新入驻）—— 产生 status=3 后教练可重新入驻
- [ ] US-027 / US-028（学员申请退款 / 管理员处理退款）—— frozen package 可触发退款

---

## 10. INVEST 自检

- [x] **I**ndependent（独立）- 可在 US-039 完成后独立交付
- [x] **N**egotiable（可协商）- checklist 细项与批量策略可在设计中调整
- [x] **V**aluable（有价值）- 是教练离职闭环的关键审批节点
- [x] **E**stimable（可估算）- 1.5 人天，范围明确
- [x] **S**mall（足够小）- 一个 Sprint 内可完成
- [x] **T**estable（可测试）- 5 个 GWT 场景可客观验证

---

## 11. 完整性检查

### 11.1 字段完整性

- [x] 15 个章节全部填写
- [x] 无"待定"/"TBD"占位符（除 Figma 链接状态待设计填写）
- [x] 错误码明确（CHECKLIST_NOT_PASSED / SCHEDULE_NOT_CLEARED / TICKET_NOT_PENDING_AUDIT）

### 11.2 业务规则

- [x] 引用 §5.5.1.1
- [x] 与批量变更 SQL 一致
- [x] 与拒绝后状态恢复规则一致

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

- **幂等键**：审批接口使用 `resignation:ticket:{ticket_id}:version:{ticket.updated_at}` 乐观锁
- **事务边界**：coach.status + ticket + refund_record + booking + package + schedule_slot 批量更新应在同一数据库事务
- **退款硬约束**：教练主动离职时，未消耗剩余课时须 100% 退还；审批通过时所有 active package 均生成待退款记录
- **性能要求**：审批接口 P99 < 1s（含 100 份以内 package 批量处理）
- **教练费结算**：审批通过时记录待结算教练费，实际结算由财务模块异步处理

---

## 13. Figma 链接

> Figma **设计系统规范**见 [docs/spec/figma/README.md](../../spec/figma/README.md)。

| # | 内容 | 链接 / node-id | 状态 |
|---|------|---------------|------|
| 1 | 离职审批队列页 Figma file URL | `https://www.figma.com/file/leyoSwimming/admin-resignation-queue` | 🔲 待设计填写 |
| 2 | 离职工单详情页 Figma file URL | `https://www.figma.com/file/leyoSwimming/admin-resignation-detail` | 🔲 待设计填写 |
| 3 | 审批通过/拒绝确认弹窗 frame node-id | `admin-resignation-detail:approve-modal` | 🔲 待设计填写 |

### 13.1 状态截图清单

| 页面 | 空状态 | 加载状态 | 错误状态 | 成功状态 | 备注 |
|------|--------|---------|---------|---------|------|
| **离职审批队列** | 🔲 | 🔲 | 🔲 | 🔲 | 空状态为无待审批工单 |
| **工单详情** | — | 🔲 | 🔲 | 🔲 | 错误状态含 checklist 未通过提示 |

---

## 14. 页面级设计决策

### 14.1 审批 checklist 展示形式

- **背景**：需要确保管理员不遗漏关键检查项
- **选项**：A. 纯文本提示；B. 可勾选的 checklist，未全勾选时禁用通过按钮
- **结论**：选择 B，强制管理员确认每项
- **影响范围**：离职工单详情页

### 14.2 批量变更结果反馈

- **背景**：审批通过会触发大量数据变更
- **选项**：A. 同步等待全部完成；B. 部分异步，后台显示进度
- **结论**：选择 A（MVP），package 数量在百级以内可在 1s 内完成
- **影响范围**：审批通过接口、前端 loading 态

### 14.3 页面级交互说明

| 交互 | 触发 | 反馈 | 备注 |
|------|------|------|------|
| 进入审批队列 | 菜单点击 | 加载 pending_audit 工单列表 | |
| 打开工单详情 | 点击行 | 展示教练信息、套餐处理结果、checklist | |
| 勾选 checklist | 点击复选框 | 全部勾选后「通过」按钮高亮 | |
| 点击通过 | 通过按钮 | 二次确认弹窗 → 执行批量变更 | 失败时显示具体未通过项 |
| 点击拒绝 | 拒绝按钮 | 填写原因 → 恢复 coach.status=1 | |

---

## 15. 设计评审记录

| 日期 | 评审人 | 反馈 | 处置 |
|------|--------|------|------|
| 2026-07-30 | PM | 审批拒绝后是否回滚已登记的学员处理 | 已在 §5.3 明确不回滚，维持现状 |

---

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | PM | 初版 |
| v1.1 | 2026-07-31 | PM | P1 修复：§5 统一 `frozen_reason` 为字符串类型（与 PRD §3.7 数据模型定义一致），明确 PRD §5.5.1.1 line 605 整型写法为笔误；补充字段枚举值 |
| v1.2 | 2026-07-31 | PM | v3 评审 P0 修复：booking.cancel_reason 统一为整型（2=教练离职） |

---

<p align="right">
<sub>本文档遵循 <a href="../../spec/user-story/SPECIFICATION.md">US 拆分规范</a>　·　复制自 <a href="../../spec/user-story/TEMPLATE.md">TEMPLATE.md</a></sub>
</p>
