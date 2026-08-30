# US-035 管理员返还课时

> **状态**：[REVIEW]（评审中）
> **优先级**：[MVP]
> **估时**：0.5 人天
> **作者**：PM　|　**最后更新**：2026-07-30
> **配套文档**：Figma：[待设计填写]　·　技术设计：[./tech-design.md](./tech-design.md)　·　测试计划：[./test-plan.md](./test-plan.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-035 |
| **标题** | 管理员返还课时 |
| **角色（Actor）** | 管理员（主）、系统（辅） |
| **业务价值（Why）** | 让管理员在特殊情况下（教练误操作、客诉、争议）手动返还已扣课时，保证课时消耗可审计、可回滚 |
| **优先级** | [MVP] |
| **估时** | 0.5 人天 |
| **配套文档** | Figma / 技术设计 / 测试计划（链接见上方）|

> **拆分说明**（P0 修复）：原 US-033 同时包含「教练确认上课」与「管理员返还课时」两件事，违反单 US 单一职责。已将「管理员返还课时」拆分为本独立 US-035（沿用原未占用的编号）。教练侧职责保留在 US-033。

---

## 2. 触发条件

- **触发方**：管理员
- **触发动作**：在管理后台 booking 详情页点击「返还课时」并填写原因
- **触发时机**：任意已扣课时状态（booking.status ∈ {已完成, 旷课}）且 package.consumed_count > 0

---

## 3. 前置条件

- [x] 管理员已登录且具有 `MANAGE_BOOKING` 权限
- [x] 存在 booking.status ∈ {已完成, 旷课}（依赖 US-033）
- [x] 对应 package.consumed_count > 0
- [x] 套餐状态 package.status ∈ {active, exhausted, expired}（已退款/refunded 不可返还，PRD §4.6 续期规则同理）

---

## 4. 业务流程

### 4.1 主路径

1. 管理员在管理后台进入 booking 详情页
2. 系统展示 booking 信息、package 消耗情况、已扣课时数
3. 管理员点击「返还课时」
4. 系统弹窗要求填写返还原因（必填）与备注（可选）
5. 管理员确认提交
6. 系统事务内执行：
   - 查询该 booking 累计已返还课时 `returned_hours_sum`；若 `returned_hours_sum >= consumed_hours`，返回 `RETURN_QUOTA_EXCEEDED`
   - package.consumed_count -1
   - package.available_count +1
   - 创建 hour_return 记录（关联 booking、管理员、原因），`returned_hours = 1`
   - 记录 audit_log
   - 若 package.status = expired 且返还后 available_count > 0，同步将 package.status 从 expired 恢复为 active，并按套餐原有效期时长从返还操作时间重新计算 `expire_at`（新 expire_at = 返还时刻 + 原有效期时长；原有效期时长 = 原 expire_at - purchased_at），保证状态与有效期一致（与 PRD §4.6 管理员手动延期规则一致）
7. 系统通知学员「课时已返还」
8. 返回成功

### 4.2 异常分支

- **分支 1**：consumed_count = 0 → 返回 `NO_CONSUMED_HOUR`
- **分支 2**：booking 状态 ∉ {已完成, 旷课} → 返回 `BOOKING_NOT_RETURNABLE`
- **分支 3**：非管理员或无权限 → 返回 403
- **分支 4**：package.status = refunded → 返回 `PACKAGE_NOT_RETURNABLE`
- **分支 5**：该 booking 累计已返还课时 ≥ 已扣课时 → 返回 `RETURN_QUOTA_EXCEEDED`

---

## 5. 业务规则引用

| # | 规则 | 章节 |
|---|------|------|
| 1 | 管理员可在特殊情况下手动返还课时 | [§5.3.2](../../prd/prd.md) |
| 2 | 返还后 package.consumed_count -1, available_count +1 | [§6.3.1](../../prd/prd.md) |
| 3 | 返还操作必须记录审计日志 | [§5.5.3](../../prd/prd.md) |
| 4 | expired 套餐管理员手动延期规则：仅延长 expire_at，available 保持原样，不增加课时；exhausted/refunded 不允许延期 | [§4.6](../../prd/prd.md) |
| 5 | refunded 状态套餐不可返还（与 §4.6 不允许延期同理） | [§4.6](../../prd/prd.md) |

---

## 6. 验收标准（业务级 Gherkin）

### 6.1 场景 1：正常返还课时

```gherkin
Given 管理员已登录且具有 MANAGE_BOOKING 权限
And   存在 booking.status = 已完成
And   对应 package.consumed_count = 3，available_count = 7
When  管理员点击「返还课时」并填写原因"教练误操作"
Then  package.consumed_count = 2，available_count = 8
And   hour_return 记录创建，reason = "教练误操作"
And   audit_log 记录管理员返还操作
And   学员收到课时返还通知
And   HTTP 状态码 = 200
```

### 6.2 场景 2：无可扣课时可返

```gherkin
Given 管理员已登录
And   存在 booking.status = 已完成
And   对应 package.consumed_count = 0
When  管理员尝试返还课时
Then  系统返回 HTTP 400，错误码 NO_CONSUMED_HOUR
And   package 课时不变
```

### 6.3 场景 3：非可返还 booking 状态

```gherkin
Given 管理员已登录
And   存在 booking.status = 已取消
When  管理员尝试返还课时
Then  系统返回 HTTP 400，错误码 BOOKING_NOT_RETURNABLE
And   package 课时不变
```

### 6.4 场景 4：返还 expired 套餐课时并恢复有效期

```gherkin
Given 管理员已登录且具有 MANAGE_BOOKING 权限
And   存在 booking.status = 已完成
And   对应 package.status = expired，consumed_count = 3，available_count = 0，total_hours = 3
And   package.purchased_at = '2026-06-01T00:00:00'，原 expire_at = '2026-07-01T00:00:00'（原有效期 30 天）
When  管理员在 '2026-07-31T12:00:00' 点击「返还课时」并填写原因"教练误操作"
Then  package.consumed_count = 2，available_count = 1
And   package.status = active
And   package.expire_at = '2026-08-30T12:00:00'（按原 30 天有效期从返还时间重新计算）
And   hour_return 记录创建，reason = "教练误操作"
And   audit_log 记录管理员返还操作
And   学员收到课时返还通知
And   HTTP 状态码 = 200
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

| # | 表名 | 操作 | 说明 |
|---|------|------|------|
| 1 | `package` | 修改 | consumed-1, available+1；若从 expired 复活为 active，同步按原有效期时长更新 `expire_at` |
| 2 | `hour_return` | 新增 | 返还记录（booking_id, admin_id, reason） |
| 3 | `audit_log` | 新增 | 记录返还操作 |
| 4 | `notification` | 新增 | 通知学员 |

### 7.2 API 影响

| # | API | 方法 | 操作 | 说明 |
|---|-----|------|------|------|
| 1 | `/api/admin/bookings/{booking_id}/return-hour` | POST | 新增 | 管理员返还课时 |

### 7.3 状态机影响

| # | 实体 | 转换 | 触发条件 | 说明 |
|---|------|------|---------|------|
| 1 | `package` | consumed → available | 管理员返还 | consumed-1, available+1 |
| 2 | `package` | exhausted → active | 返还后若 consumed < total_hours | 套餐复活 |
| 3 | `package` | expired → active | 返还后 available > 0 且管理员确认恢复 | 状态恢复同时按原有效期时长延长 `expire_at`，与 PRD §4.6 延期规则一致 |

---

## 8. 边界场景

### 8.1 边界场景 1：返还后套餐从 exhausted 复活为 active

- **触发条件**：套餐已 exhausted（consumed = total），管理员返还 1 课时
- **预期行为**：package.status 从 exhausted → active
- **用户可见反馈**：学员端提示「套餐已恢复可用」

### 8.2 边界场景 2：重复返还同一 booking

- **触发条件**：管理员对同一 booking 再次点击「返还课时」
- **预期行为**：系统校验该 booking 累计 `returned_hours_sum < consumed_hours`；若已返还课时等于或超过已扣课时，则返回 `RETURN_QUOTA_EXCEEDED`，禁止再次返还
- **用户可见反馈**：提示「该 booking 可返还课时已用完，无法重复返还」

### 8.3 边界场景 3：返还与教练确认并发

- **触发条件**：管理员返还课时与教练确认上课同时发生
- **预期行为**：通过 package 乐观锁保证数据一致性
- **用户可见反馈**：以最终状态为准

### 8.4 边界场景 4：返还 expired 套餐课时（v3 评审 P0 修复）

- **触发条件**：package.status = expired，consumed_count > 0，管理员点击「返还课时」
- **预期行为**：允许返还；返还后若 available_count > 0，package.status 从 expired 恢复为 active，并按套餐原有效期时长从返还时间重新计算 `expire_at`（新 expire_at = 返还时刻 + 原有效期时长），避免状态为 active 但 expire_at 仍停留在过去；若返还后 available_count 仍为 0（如 consumed-1 后仍 ≥ total_hours，理论不可能），保持 expired
- **用户可见反馈**：学员端提示「套餐已恢复可用，有效期已延长」
- **特殊说明**：refunded 状态套餐不可返还（PRD §4.6 续期规则同理），系统返回 PACKAGE_NOT_RETURNABLE 错误码

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）

- [x] US-033（教练确认上课记录）—— 产生已扣课时 booking

### 9.2 后续 US（依赖本故事）

- [ ] 无

---

## 10. INVEST 自检

- [x] **I**ndependent（独立）- 依赖 US-033 的 booking 数据，但返还逻辑可独立交付
- [x] **N**egotiable（可协商）- 返还原因分类可协商
- [x] **V**aluable（有价值）- 客诉兜底机制
- [x] **E**stimable（可估算）- 0.5 人天明确
- [x] **S**mall（足够小）- 单一返还操作
- [x] **T**estable（可测试）- 4 个 GWT 场景可客观验证

---

## 11. 完整性检查

### 11.1 字段完整性

- [x] 15 个章节全部填写
- [x] 无"待定"/"TBD"占位符
- [x] 错误码明确

### 11.2 业务规则

- [x] 引用 §5.3.2 / §6.3.1 / §5.5.3
- [x] 与状态机一致

### 11.3 验收标准

- [x] 1 正常 + 3 异常 = 4 个 GWT 场景（L1 等级）
- [x] 每个 Then 含具体数值/状态码/DB 字段值

### 11.4 配套文档

- [x] 链接明确

---

## 12. 备注

- **幂等键**：`{admin_id}:{booking_id}:return-hour:{timestamp}`（同一 booking 可多次返还，但每次需独立审计）
- **事务边界**：package 扣减 + hour_return 创建 + audit_log 同一事务
- **性能要求**：返还接口 P99 < 300ms

---

## 13. Figma 链接

> Figma **设计系统规范**见 [docs/figma/README.md](../../figma/README.md)。

| # | 内容 | 链接 / node-id | 状态 |
|---|------|---------------|------|
| 1 | 管理端返还课时页 Figma file URL | 🔲 待设计填写 | 🔲 |

### 13.1 状态截图清单

> **四态要求**：每个页面必须设计空/加载/错误/成功四种状态。具体样式由设计师根据 [Figma 设计规范](../../figma/README.md) §0.5 主题与 §4 四态模板决定，不在本节指定。

| 页面 | 空状态 | 加载状态 | 错误状态 | 成功状态 | 备注 |
|------|--------|---------|---------|---------|------|
| **返还课时弹窗** | — | 🔲 | 🔲 | 🔲 | |

---

## 14. 页面级设计决策

### 14.1 返还原因分类

- **背景**：需要区分不同返还场景便于审计
- **结论**：预设 4 类：教练误操作 / 客诉处理 / 系统故障 / 其他
- **影响范围**：返还弹窗

### 14.2 页面级交互说明

| 交互 | 触发 | 反馈 | 备注 |
|------|------|------|------|
| 点击返还 | 管理员点击 | 弹窗（原因必填）→ 成功 Toast | 审计留痕 |

---

## 15. 设计评审记录

| 日期 | 评审人 | 反馈 | 处置 |
|------|--------|------|------|
| 2026-07-30 | P0 评审 | 从 US-033 拆分为独立 US | 已执行 |

---

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | PM | P0 修复：从 US-033 拆分出独立 US-035「管理员返还课时」（沿用原未占用编号） |
| v1.1 | 2026-07-31 | PM | v3 评审 P0 修复：明确 expired 套餐返还规则（允许返还，返还后恢复 active）；补充 refunded 不可返还错误码 PACKAGE_NOT_RETURNABLE；§7.3 新增 expired→active 转换；§8.4 新增边界场景 |
| v1.2 | 2026-07-31 | PM | v5 评审修复：§4.1 增加 booking 累计返还课时校验；§4.2/§8.2 修改重复返还行为，新增 `RETURN_QUOTA_EXCEEDED` |
| v1.3 | 2026-07-31 | PM | v6 评审 P0 修复：返还 expired 套餐复活为 active 时，同步按原有效期时长更新 `expire_at`，消除 status=active 但 expire_at 已过的状态机矛盾 |
| v1.4 | 2026-07-31 | PM | v7 P0-F 修复：澄清与 PRD §4.6 口径差异——§4.1 步骤 6、§5 业务规则引用、§7.3 状态机、§8.4 边界场景明确说明"返还 + 同步延期"是复合操作（返还使 available+1，与 §4.6 单纯延期不动 available 是不同操作）；§5 新增第 4/5 条引用 PRD §4.6；同步 OpenSpec spec/design |
| v1.5 | 2026-08-01 | PM | §13.1 四态标记统一为 🔲，删除样式描述，添加四态要求说明 |

---

<p align="right">
<sub>本文档遵循 <a href="../../spec/user-story/SPECIFICATION.md">US 拆分规范</a>　·　复制自 <a href="../../spec/user-story/TEMPLATE.md">TEMPLATE.md</a></sub>
</p>
