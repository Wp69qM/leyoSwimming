# US-040 教练重新入驻

> **状态**：[REVIEW]（评审中）
> **优先级**：[MVP]
> **估时**：1 人天
> **作者**：PM　|　**最后更新**：2026-07-30
> **配套文档**：Figma：[待设计填写]　·　技术设计：[./tech-design.md](./tech-design.md)　·　测试计划：[./test-plan.md](./test-plan.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-040 |
| **标题** | 教练重新入驻 |
| **角色（Actor）** | 教练（主）、系统（辅） |
| **业务价值（Why）** | 让已离职教练可重新申请入驻并恢复教学资格；历史数据保留在原有 coach 记录下，不隔离 |
| **优先级** | [MVP] |
| **估时** | 1 人天 |
| **配套文档** | Figma / 技术设计 / 测试计划（链接见上方） |

---

## 2. 触发条件

- **触发方**：教练
- **触发动作**：已离职教练登录教练端（US-051 / US-054）时，系统按 `coach_status = 3` 自动跳转 US-010 的 C-入驻资料填写页
- **触发时机**：用户主动登录，且当前 `coach.status = 3`

---

## 3. 前置条件

- [x] 教练当前 `coach.status = 3`（已离职，依赖 US-039 / US-041）
- [x] 教练账号未被注销或封禁
- [x] 系统允许已离职教练发起重新入驻（MVP 无冷却期限制）

---

## 4. 业务流程

### 4.1 主路径

1. 已离职教练登录教练端（US-051 / US-054）
2. 系统按 `coach.status = 3` 直接跳转 C-入驻资料填写页（US-010），页面顶部展示重新入驻说明条
3. C-入驻资料填写页自动回显 coach 表历史内容或最新 rejected/draft coach_application 快照，教练可编辑确认
4. 教练点击「提交审核」，由 US-010 处理资料校验与提交，创建新的 coach_application pending 快照（previous_coach_status=3），coach.status 从 3 更新为 0（待审核）
5. 管理员在后台审核重新入驻申请（复用 US-011 审核能力，审核视图中通过 previous_coach_status=3 标记为"重新入驻"）
6. 管理员审核通过后，coach_application 快照覆盖写入 coach 表，`coach.status` 从 0 更新为 1（已通过）

### 4.2 异常分支

- **分支 1**：教练状态非 3 → US-051 / US-054 登录后不按重新入驻流程跳转；若教练以其他方式进入 US-010 的 C-入驻资料填写页，US-010 校验状态后返回错误码 `COACH_STATUS_NOT_ALLOWED`
- **分支 2**：教练已处于 status=0（待审核） → 由 US-010 的重复提交校验处理，提示「入驻申请审核中，请勿重复提交」
- **分支 3**：重新入驻审核被拒绝 → `coach_application.status = rejected` 并写入 `rejection_reason`；`coach.status` 恢复为 3；coach 表生效资料保持不变，教练再次登录时重新进入 US-010 填写页提交

---

## 5. 业务规则引用

| # | 规则 | 章节 |
|---|------|------|
| 1 | 状态 3（已离职）的教练可重新申请入驻 | [§5.4.8](../../prd/prd.md) |
| 2 | 重新申请时创建新的 coach_application pending 快照（previous_coach_status=3），`coach.status: 3 → 0`；实际资料填写由 US-010 统一处理 | 本 US 设计决策 |
| 3 | 重新入驻审核驳回后 `coach.status` 恢复为 3，coach 表生效资料保持不变 | 本 US 设计决策 |
| 4 | 重新入驻时复用原 coach 记录，历史数据不回滚、不隔离 | 本 US 设计决策 |
| 5 | 历史评分/评价保留，新老学员均可见 | [§5.4.8](../../prd/prd.md) |
| 6 | 已 frozen 的老学员 package 仍 frozen，等待老学员主动换回原教练或退款 | [§5.4.8](../../prd/prd.md) |

---

## 6. 验收标准（业务级 Gherkin）

> 本 US 为 L2（1 人天），包含 2 个正常场景 + 3 个异常场景 = 5 个场景。

### 6.1 场景 1：已离职教练进入 US-010 重新入驻

```gherkin
Given 教练 C 当前 coach.status = 3（已离职）
And   教练 C 的账号状态正常
And   教练 C 之前已提交姓名"李教练"、任教年限 5 年、参考单价 300.00 元
When  教练 C 登录教练端（US-051）
Then  US-051 按 coach_status=3 直接跳转 C-入驻资料填写页（US-010）
And   页面顶部展示重新入驻说明条"你的账号已离职，请重新提交入驻资料，审核通过后即可恢复接单。"
And   表单自动回显姓名"李教练"、任教年限 5 年、参考单价 300.00 元
When  教练 C 确认资料并点击「提交审核」
Then  US-010 校验并创建 coach_application pending 快照（previous_coach_status=3）
And   coach.status 从 3 更新为 0（待审核）
And   coach.submitted_at 更新为当前时间
And   系统返回"提交成功，等待审核"
```

### 6.2 场景 2：管理员通过重新入驻审核

```gherkin
Given 教练 C 的 coach.status = 0（待审核）
And   存在 coach_application.status = pending 且 previous_coach_status=3 的重新入驻申请
When  管理员在后台点击「通过审核」
Then  coach_application.status 更新为 approved
And   coach_application 快照字段覆盖写入 coach 表
And   coach.status 更新为 1（已通过）
And   教练恢复教学资格，可继续使用 US-012 / US-013 / US-014 等功能
And   老学员可在历史记录中查看该教练过往评分
And   新学员查看教练主页时也展示该教练过往评分
```

### 6.3 场景 3：非已离职教练登录后不按重新入驻分流

```gherkin
Given 教练 C 的 coach.status = 1（已通过）
When  教练 C 登录教练端（US-051 / US-054）
Then  系统返回 coach_status = 1
And   前端按 US-051 正常映射跳转（例如教练首页）
And   不进入 US-010 的 C-入驻资料填写页
And   若教练以其他方式调用 US-010 重新入驻提交流程，则返回错误码 COACH_STATUS_NOT_ALLOWED
```

### 6.4 场景 4：重复发起重新入驻

```gherkin
Given 教练 C 的 coach.status = 0（待审核）
And   存在 coach_application.status = pending 且 previous_coach_status=3 的申请
When  教练 C 再次登录教练端（US-051 / US-054）
Then  系统按 coach_status = 0 跳转「等待审核页」
And   不进入 US-010 的 C-入驻资料填写页
And   coach.status 保持 0，不产生新的待审核记录
```

### 6.5 场景 5：重新入驻审核被拒绝

```gherkin
Given 教练 C 的 coach.status = 0（待审核）
And   存在 coach_application.status = pending 且 previous_coach_status=3 的重新入驻申请
When  管理员点击「拒绝审核」并填写原因="资料不完整"
Then  coach_application.status 更新为 rejected
And   coach_application.rejection_reason = "资料不完整"
And   coach.status 恢复为 3（已离职）
And   coach 表生效资料保持不变
And   教练端再次进入 US-010 填写页时顶部展示红色驳回原因条
And   表单回显 coach_application 快照内容，教练可再次编辑提交
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

| # | 表名 | 操作 | 说明 |
|---|------|------|------|
| 1 | `coach` | 修改 | `status` 3 → 0 → 1 或 3 → 0 → 3；审核通过时生效资料由 coach_application 快照覆盖；复用原 coach 记录，不隔离历史数据 |
| 2 | `coach_application` | 新增 | 重新入驻提交时创建 pending 快照（previous_coach_status=3）；驳回时标记 rejected |
| 3 | `coach_certificate_application` | 新增 | 重新入驻申请快照关联证书 |
| 4 | `coach_rating` / `review` | 只读 | 历史评分/评价保留，新老学员均可见 |
| 5 | `coach_audit_log` | 新增 | 记录状态变更与审核操作 |

### 7.2 API 影响

| # | API | 方法 | 操作 | 说明 |
|---|-----|------|------|------|
| 1 | （无新增教练端入口 API） | — | — | 重新入驻入口由 US-051 / US-054 登录状态分流提供；实际资料填写与提交复用 US-010 的 `POST /api/coach/application` 与 `PUT /api/coach/application/draft`；审核复用 US-011 的 `POST /api/admin/coach/applications/{application_id}/approve` / `/reject` |
| 2 | `/api/coach/v1/reapply/status` | GET | 可选 | 查询重新入驻审核状态；如复用 US-011 审核接口，可删除 |
| 3 | `/api/admin/coach/applications/{application_id}/approve` | POST | 复用 US-011 | 管理员通过重新入驻；coach_application 快照覆盖 coach 表，coach.status: 0 → 1 |
| 4 | `/api/admin/coach/applications/{application_id}/reject` | POST | 复用 US-011 | 管理员拒绝重新入驻；coach.status: 0 → 3，coach_application 标记 rejected |

### 7.3 状态机影响

| # | 实体 | 转换 | 触发条件 | 说明 |
|---|------|------|---------|------|
| 1 | coach.status | 3（已离职）→ 0（待审核） | 教练在 US-010 提交重新入驻资料 | 创建 coach_application pending 快照（previous_coach_status=3） |
| 2 | coach.status | 0（待审核）→ 1（已通过） | 管理员审核通过 | coach_application 快照覆盖 coach 生效资料，恢复教学资格 |
| 3 | coach.status | 0（待审核）→ 3（已离职） | 管理员审核拒绝 | coach.status 恢复为 previous_coach_status=3，coach 生效资料不变 |
| 4 | coach_application.status | pending → approved | 管理员通过 | 快照状态机 |
| 5 | coach_application.status | pending → rejected | 管理员拒绝 | 快照状态机 |

---

## 8. 边界场景

### 8.1 边界场景 1：并发发起重新入驻

- **触发条件**：教练登录后快速多次触发 US-010 资料提交
- **预期行为**：US-010 的提交接口幂等，仅产生一条 pending 的 coach_application
- **用户可见反馈**：仅一次成功提示

### 8.2 边界场景 2：已 frozen 的老学员套餐不变

- **触发条件**：教练重新入驻成功后，老学员查看之前因教练离职而被 frozen 的套餐
- **预期行为**：package.status 保持 frozen，不自动恢复为 active
- **用户可见反馈**：学员端显示「套餐已冻结，可换回原教练或申请退款」

### 8.3 边界场景 3：历史评价对新学员可见

- **触发条件**：新学员（未与该教练有过 package/booking 关联）浏览重新入驻教练的主页
- **预期行为**：系统返回该教练历史评分与评价
- **用户可见反馈**：主页评分区域展示完整历史评分

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）

- [x] US-041（管理员处理教练离职）—— 审批通过后 `coach.status` 变为 3（已离职），本 US 方可触发重新入驻
- [x] US-010（教练提交入驻资料）—— 实际资料填写、校验、提交逻辑由 US-010 处理
- [x] US-011（管理员审核教练入驻资质）—— 复用审核流程与表
- [x] US-039（教练申请离职）—— 通过 US-041 审批后间接产生 status=3

### 9.2 后续 US（依赖本故事）

- [ ] 无直接后续 US；重新入驻审核通过后教练恢复教学资格，可继续使用 US-012 / US-013 / US-014 等功能

---

## 10. INVEST 自检

- [x] **I**ndependent（独立）- 可在 US-039 / US-041 完成后独立交付
- [x] **N**egotiable（可协商）- 重新入驻字段与历史数据可见性规则可在设计中调整
- [x] **V**aluable（有价值）- 支持教练回流，降低平台教练流失成本
- [x] **E**stimable（可估算）- 1 人天，复用 US-010/011 流程
- [x] **S**mall（足够小）- 一个 Sprint 内可完成
- [x] **T**estable（可测试）- 5 个 GWT 场景可客观验证

---

## 11. 完整性检查

### 11.1 字段完整性

- [x] 15 个章节全部填写
- [x] 无"待定"/"TBD"占位符（除 Figma 链接状态待设计填写）
- [x] 错误码明确（US-010 返回 COACH_APPLICATION_PENDING；US-010 状态校验返回 COACH_STATUS_NOT_ALLOWED）

### 11.2 业务规则

- [x] 引用 §5.4.8
- [x] 与历史评分可见性规则一致
- [x] 与 frozen package 不自动恢复规则一致

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

- **事务边界**： coach.status 的修改由 US-010 提交接口负责；US-040 本身不新增入口校验接口
- **性能要求**：登录状态分流由 US-051 / US-054 返回 coach_status 决定，不产生额外接口调用；审核状态查询可复用 US-011
- **历史数据**：coach_rating / review 历史评分/评价保留，新老学员均可见；coach 表历史数据不隔离、不回滚

---

## 13. Figma 链接

> Figma **设计系统规范**见 [docs/figma/README.md](../../figma/README.md)。

| # | 内容 | 链接 / node-id | 状态 |
|---|------|---------------|------|
| 1 | C-入驻资料填写页（status=3 重新入驻说明条状态）Figma file URL | 🔲 待设计填写 | 🔲 待设计填写 |
| 2 | 管理员后台重新入驻审核列表/详情页 Figma file URL | 🔲 待设计填写 | 🔲 待设计填写 |

### 13.1 状态截图清单

> **四态要求**：每个页面必须设计空/加载/错误/成功四种状态。具体样式由设计师根据 [Figma 设计规范](../../figma/README.md) §0.5 主题与 §4 四态模板决定，不在本节指定。

| 页面 | 空状态 | 加载状态 | 错误状态 | 成功状态 | 备注 |
|------|--------|---------|---------|---------|------|
| **C-入驻资料填写页（status=3）** | — | 🔲 | 🔲 | 🔲 | 顶部含重新入驻说明条；入口由 US-051 / US-054 登录分流自动进入；实际设计由 US-010 负责 |
| **管理员后台重新入驻审核详情** | — | 🔲 | 🔲 | 🔲 | 需标记为"重新入驻"申请 |

---

## 14. 页面级设计决策

### 14.1 重新入驻入口位置

- **背景**：只有已离职教练需要重新入驻，且教练端无游客状态；已离职教练登录后不应进入主界面
- **选项**：A. 在教练端「我的」页面放置「重新入驻」入口；B. 由 US-051 / US-054 登录状态分流自动跳转 US-010 C-入驻资料填写页
- **结论**：选择 B，已离职教练登录后直接跳转 US-010，无需也无法进入「我的」页面
- **影响范围**：US-051 / US-054 登录回调、US-010 C-入驻资料填写页

### 14.2 重新入驻资料填写页复用 US-010

- **背景**：重新入驻需要填写的字段与首次入驻/驳回后重新提交完全一致
- **选项**：A. 独立重新入驻资料页；B. 复用 US-010 的 C-入驻资料填写页
- **结论**：选择 B，减少重复页面和维护成本；status=3 时直接进入 US-010 填写页，顶部展示前端硬编码的重新入驻说明条
- **影响范围**：US-040 流程、US-051 登录分流、US-010 填写页状态提示区

### 14.3 历史数据可见性

- **背景**：重新入驻后历史评分/评价是教练真实服务记录，应完整保留
- **选项**：A. 完全删除历史评分；B. 保留在 coach 记录下并对新老学员均可见
- **结论**：选择 B，历史数据不回滚、不隔离、不隐藏
- **影响范围**：教练主页、列表评分展示

### 14.4 页面级交互说明

| 交互 | 触发 | 反馈 | 备注 |
|------|------|------|------|
| 登录后自动跳转 | US-051 / US-054 状态分流 | 直接进入 US-010 C-入驻资料填写页 | status=3 时顶部展示重新入驻说明条 |
| 提交资料 | US-010 资料页按钮 | 跳转审核状态页 | status 由 US-010 从 3 变为 0 |
| 审核通过 | 管理员操作 | 教练端收到通知 | status 变为 1 |
| 审核拒绝 | 管理员操作 | 教练端显示原因 | status 恢复 3，教练再次登录后重新进入 US-010 提交 |

---

## 15. 设计评审记录

| 日期 | 评审人 | 反馈 | 处置 |
|------|--------|------|------|
| 2026-07-30 | PM | 重新入驻是否允许新学员看到历史差评 | 历史评分/评价保留，新老学员均可见 |

---

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | PM | 初版 |
| v1.2 | 2026-08-05 | PM | 重新入驻流程重构：实际资料填写由 US-010 统一处理；status=3 直接进入 US-010 C-入驻资料填写页；历史数据不复用隔离；GWT、API、状态机、设计决策同步更新 |
| v1.1 | 2026-07-31 | PM | §13 Figma 链接清理：预设占位 URL 改为 🔲 待设计填写，待设计师在 Figma Drafts 创建文件后回填真实链接 |

---

<p align="right">
<sub>本文档遵循 <a href="../../spec/user-story/SPECIFICATION.md">US 拆分规范</a>　·　复制自 <a href="../../spec/user-story/TEMPLATE.md">TEMPLATE.md</a></sub>
</p>
