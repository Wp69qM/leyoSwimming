# US-011 管理员审核教练入驻资质

> **状态**：[APPROVAL]（已确认）
> **优先级**：[MVP]
> **估时**：0.5 人天
> **作者**：PM　|　**最后更新**：2026-07-30
> **配套文档**：Figma：[A-coach-audit-queue-page.md](../../figma/page-spec/A-coach-audit-queue-page.md) / [A-coach-audit-detail-page.md](../../figma/page-spec/A-coach-audit-detail-page.md)　·　技术设计：[tech-design.md](./tech-design.md)　·　测试计划：[test-plan.md](./test-plan.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-011 |
| **标题** | 管理员审核教练入驻资质 |
| **角色（Actor）** | 管理员（主）、系统（辅）|
| **业务价值（Why）** | 保证教练资质合规，控制教练质量，是教练上线服务的前置关卡 |
| **优先级** | [MVP] |
| **估时** | 0.5 人天 |
| **配套文档** | Figma / 技术设计 / 测试计划（链接见上方）|

---

## 2. 触发条件

- **触发方**：管理员
- **触发动作**：在 Web 管理后台查看待审核教练列表并进行审核
- **触发时机**：教练提交入驻资料后，管理员主动处理

---

## 3. 前置条件

- [x] 管理员已登录 Web 后台
- [x] 存在 `coach_application.status = pending` 的教练入驻/重新入驻申请（依赖 US-010；草稿不进入审核列表）
- [x] 管理员具有「教练审核」权限

---

## 4. 业务流程

### 4.1 主路径

1. 管理员进入「用户管理 → 教练审核」列表。
2. 系统默认展示 `coach_application.status = pending` 的教练列表，按 `coach_id` 维度聚合（每个教练只展示一行）。列表表格字段包含：教练 ID、姓名、性别、年龄、教学年限、擅长泳姿、最新提交时间、流程状态（待审核/已通过/已驳回）、previous_coach_status（首次入驻/已驳回重新入驻/已离职重新入驻）、latest_application_id。管理员可通过状态筛选切换为全部 / 待审核 / 已通过 / 已驳回。
3. 管理员可执行以下任一操作：
   - **列表页直接通过**：在待审核列表中点击「通过」，系统弹出二次确认，确认后直接执行通过逻辑。
   - **详情页查看后通过**：点击「详情」进入审核详情页，系统展示该教练当前最新申请（`latest_application_id`）的 coach_application 快照完整资料，并在「申请历史」区域展示该教练的所有历史申请记录（含申请 ID、状态、时间、审核人、驳回原因）。管理员确认无误后点击「通过」。
4. 系统校验权限与 coach_application.status = pending
5. 系统将 coach_application 快照字段覆盖写入 `coach` 表生效资料
6. 系统更新 `coach.status = 1`（已通过）、`coach.approved_at = 当前时间`
7. 系统更新 `coach_application.status = approved`、`approved_at`、`approved_by`
8. 系统将快照证书写入 `coach_certificate` 表（覆盖旧证书）
9. 系统写入 `coach_audit_log`：`action='approve'`、`from_status=0`、`to_status=1`
10. 系统发送审核通过通知给教练
11. 教练端功能解锁

### 4.2 异常分支

- **分支 1**：资料不全 → 管理员点击「驳回」并填写原因；系统标记 `coach_application.status = rejected` 并写入 `rejection_reason`；`coach.status` 恢复为 `previous_coach_status`（-1→2，2→2，3→3）。
  - 列表页驳回：点击「驳回」后弹出轻量输入框（Popconfirm 或小型 Dialog），管理员填写驳回原因后确认。
  - 详情页驳回：点击「驳回」后底部/侧边弹出原因输入区，驳回原因必填，支持常用原因快捷选择。
- **分支 2**：管理员无权限 → 返回 403

---

## 5. 业务规则引用

| # | 规则 | 章节 |
|---|------|------|
| 1 | 入驻资质需管理员审核 | [§5.4.1](../../prd/prd.md) |
| 2 | 管理员审核教练入驻资质 | [§5.5.1](../../prd/prd.md) |
| 3 | 教练状态 0=待审核 1=已通过 2=驳回 | [§5.5.1](../../prd/prd.md) |

---

## 6. 验收标准（业务级 Gherkin）

### 6.1 场景 1：审核列表直接通过

```gherkin
Given 管理员已登录且有教练审核权限
And   存在 coach_application.status = pending 的入驻申请（application_id=10001，previous_coach_status=-1）
And   该申请快照中姓名为"张教练"、参考单价为 300.00 元
When  管理员在教练审核列表点击「通过」
And   系统弹出二次确认弹窗
And   管理员点击「确认通过」
Then  coach_application.status 更新为 approved
And   coach_application.approved_at 记录当前时间
And   coach 表生效资料被覆盖为 application 快照内容（姓名"张教练"、参考单价 300.00 元）
And   coach.status 更新为 1（已通过）
And   coach.approved_at 记录当前时间
And   coach_certificate 表被该 application 快照证书覆盖
And   系统写入 coach_audit_log：action='approve'、from_status=0、to_status=1、application_id=10001
And   列表刷新，该行状态变为「已通过」
And   教练收到审核通过通知
And   教练端功能解锁
```

### 6.2 场景 2：审核详情页通过

```gherkin
Given 管理员已登录且有教练审核权限
And   存在 coach_application.status = pending 的入驻申请（application_id=10001，previous_coach_status=-1）
And   该申请快照中姓名为"张教练"、参考单价为 300.00 元
When  管理员点击「详情」查看 coach_application 快照资料
And   点击「通过」
And   系统弹出二次确认弹窗
And   管理员点击「确认通过」
Then  coach_application.status 更新为 approved
And   coach 表生效资料被覆盖为 application 快照内容（姓名"张教练"、参考单价 300.00 元）
And   coach.status 更新为 1（已通过）
And   coach_certificate 表被该 application 快照证书覆盖
And   系统写入 coach_audit_log：action='approve'、from_status=0、to_status=1、application_id=10001
And   页面提示"操作成功"并返回列表
And   教练收到审核通过通知
And   教练端功能解锁
```

### 6.3 场景 3：审核驳回

```gherkin
Given 管理员已登录且有教练审核权限
And   存在 coach_application.status = pending 的入驻申请（application_id=10001，previous_coach_status=-1）
When  管理员点击「驳回」并填写原因"证书不清晰"
Then  coach_application.status 更新为 rejected
And   coach_application.rejection_reason = "证书不清晰"
And   coach.status 更新为 2（驳回）
And   系统写入 coach_audit_log：action='reject'、from_status=0、to_status=2、reason="证书不清晰"、application_id=10001
And   教练收到驳回通知及原因
And   教练可重新修改资料后提交
```

### 6.4 场景 4：无权限审核

```gherkin
Given 管理员已登录但无教练审核权限
And   存在 coach_application.status = pending 的申请
When  管理员尝试调用审核接口
Then  返回 HTTP 403
And   coach_application.status 保持 pending
And   coach.status 保持不变
And   前端提示"您没有操作权限"
```

### 6.5 场景 5：已离职教练重新入驻申请被驳回

```gherkin
Given 管理员已登录且有教练审核权限
And   存在 coach_application.status = pending 的重新入驻申请（application_id=10002，previous_coach_status=3）
And   教练当前 coach.status = 0
When  管理员点击「驳回」并填写原因"资料不完整"
Then  coach_application.status 更新为 rejected
And   coach_application.rejection_reason = "资料不完整"
And   coach.status 恢复为 3（已离职）
And   系统写入 coach_audit_log：action='reject'、from_status=0、to_status=3、reason="资料不完整"、application_id=10002
And   教练收到驳回通知及原因
And   coach 表生效资料保持离职前状态不变
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

| # | 表名 | 操作 | 说明 |
|---|------|------|------|
| 1 | coach | 修改 | 审核通过时 coach_application 快照字段覆盖写入：`name`、`phone`、`gender`、`age`、`email`、`wechat_qr_url`、`id_card_no`、`teaching_years`、`total_students`、`total_hours`、`teaching_strokes`、`bio`、`reference_price`；更新 `status=1`、`approved_at` |
| 2 | coach_application | 修改 | status 由 pending 更新为 approved/rejected；`previous_coach_status`、`submitted_at`、`approved_at`、`approved_by`、`rejection_reason` |
| 3 | coach_certificate | 修改 | 审核通过时由 coach_certificate_application 快照覆盖写入：`cert_type`（ID_CARD_FRONT/ID_CARD_BACK/COACH_CERT/HEALTH_CERT/PORTRAIT/OTHER）、`image_url`、`sort_order` |
| 4 | coach_audit_log | 新增 | 记录审核人、时间、结果、原因；`coach_id`、`application_id`、`admin_id`、`action`（approve/reject）、`from_status` / `to_status` 记录 coach.status 变更 |
| 5 | notification | 新增 | 向教练发送审核结果通知 |

### 7.2 API 影响

> 本 US 接口遵循 [API 接口规范](../../tech/api-convention.md)：统一使用 `POST`，URL 按 `/list`、`/detail`、`/approve`、`/reject` 动作命名，参数通过 JSON body 传递。

| # | API | 方法 | 操作 | 说明 |
|---|-----|------|------|------|
| 1 | /api/admin/coach/application/list | POST | 新增 | 教练审核列表；请求体 `{ page, pageSize, keyword, status }`；按 coach_id 聚合，每个教练一行；默认 status=pending，支持筛选全部 / pending / approved / rejected；返回 coach_id、姓名、性别、年龄、教学年限、擅长泳姿、最新提交时间、流程状态、previous_coach_status、latest_application_id |
| 2 | /api/admin/coach/application/detail | POST | 新增 | 审核详情；请求体 `{ applicationId }`；返回 coach_application 快照完整资料（基础信息/实名与资质/教学履历/服务设置/证书列表）、该教练的申请历史列表及 coach_audit_log 历史 |
| 3 | /api/admin/coach/application/approve | POST | 新增 | 审核通过；请求体 `{ applicationId, remark? }`；将快照覆盖写入 coach 表及 coach_certificate 表 |
| 4 | /api/admin/coach/application/reject | POST | 新增 | 审核驳回；请求体 `{ applicationId, reason }`；coach.status 恢复为 previous_coach_status |

### 7.3 状态机影响

| # | 实体 | 转换 | 触发条件 | 说明 |
|---|------|------|---------|------|
| 1 | coach.status | 0 → 1 | 管理员通过 | 生命周期状态机 |
| 2 | coach.status | 0 → previous_coach_status | 管理员驳回 | previous_coach_status=-1 时目标为 2；=2 时目标为 2；=3 时目标为 3 |
| 3 | coach_application.status | pending → approved | 管理员通过 | 快照状态机 |
| 4 | coach_application.status | pending → rejected | 管理员驳回 | 快照状态机 |
| 5 | coach.status | 2 → 0 | 已驳回教练重新提交（US-010） | 创建新 pending application |
| 6 | coach.status | 3 → 0 | 已离职教练重新入驻提交（US-040） | 创建新 pending application |

---

## 8. 边界场景

### 8.1 边界场景 1：重复审核

- **触发条件**：管理员对 coach_application.status 已为 approved/rejected 的记录再次点击通过/驳回
- **预期行为**：幂等或拒绝，coach.status 与 coach_application.status 均不变
- **用户可见反馈**："该申请已审核，无需重复操作"

### 8.2 边界场景 2：已驳回教练重新提交后再次审核

- **触发条件**：coach.status 从 2 被 US-010 重新置为 0，并创建新的 pending coach_application
- **预期行为**：管理员按正常待审核流程处理，可再次通过或驳回；coach 表生效资料在通过前保持旧值
- **用户可见反馈**：审核列表中展示该记录为新的待审核申请

### 8.3 边界场景 3：批量审核

- **触发条件**：管理员勾选多条 coach_application.status = pending 记录批量通过
- **预期行为**：逐条校验并更新，失败项单独提示；每条通过的 application 快照独立覆盖对应 coach 表
- **用户可见反馈**："成功通过 N 条，失败 M 条"

### 8.4 边界场景 4：已离职教练重新入驻审核驳回

- **触发条件**：coach_application.previous_coach_status = 3 的重新入驻申请被驳回
- **预期行为**：coach_application.status = rejected，coach.status 恢复为 3；coach 表生效资料不变
- **用户可见反馈**：教练端展示"重新入驻申请被驳回"及原因，仍显示已离职状态

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）

- [x] US-010（教练提交入驻资料）

### 9.2 后续 US（依赖本故事）

- [ ] US-012（教练管理个人主页与参考单价）
- [ ] US-013（教练管理实时状态）
- [ ] US-014（教练管理可约时段）

---

## 10. INVEST 自检

- [x] **I**ndependent（独立）- 在 US-010 完成后可独立交付
- [x] **N**egotiable（可协商）- 审核字段、原因模板可协商
- [x] **V**aluable（有价值）- 控制教练质量
- [x] **E**stimable（可估算）- 0.5 人天明确
- [x] **S**mall（足够小）- 单一审核操作
- [x] **T**estable（可测试）- 5 个 GWT 场景可验证

---

## 11. 完整性检查

### 11.1 字段完整性

- [x] 15 个章节全部填写
- [x] 无"待定"/"TBD"占位符（除配套文档占位）
- [x] 业务规则引用明确

### 11.2 业务规则

- [x] 引用 §5.4.1 / §5.5.1
- [x] 与教练状态机一致（0 → 1 / 0 → 2）
- [x] 与数据模型一致

### 11.3 验收标准

- [x] 场景数量符合 L1（3 正常 + 2 异常 = 5 个）
- [x] 业务级 Gherkin，不绑死实现
- [x] 用户可观察的结果可被验证

### 11.4 配套文档

- [x] Figma 链接/状态明确
- [x] 技术设计文档链接/状态明确
- [x] 测试计划链接/状态明确

---

## 12. 备注

- **权限码**：`coach:audit`
- **通知渠道**：小程序订阅消息 + 短信
- **审计要求**：审核操作必须记录 audit_log
- **教练状态机**：0=待审核 1=已通过 2=驳回 3=已离职 4=申请离职中；驳回后可在 US-010 中重新提交（2 → 0）

---

## 13. Figma 链接

> Figma **设计系统规范**（token / 组件 / 状态徽标 / 4 态模板 / 文案）见 [docs/figma/README.md](../../figma/README.md)。
> 页面规格详见：
> - [A-教练入驻审核队列页](../../figma/page-spec/A-coach-audit-queue-page.md)
> - [A-教练入驻审核详情页](../../figma/page-spec/A-coach-audit-detail-page.md)

| # | 内容 | 链接 | 状态 |
|---|------|------|------|
| 1 | 教练入驻审核队列页 | [A-coach-audit-queue-page.md](../../figma/page-spec/A-coach-audit-queue-page.md) | ✅ |
| 2 | 教练入驻审核详情页 | [A-coach-audit-detail-page.md](../../figma/page-spec/A-coach-audit-detail-page.md) | ✅ |

### 13.1 状态截图清单

> **四态要求**：每个页面必须设计空/加载/错误/成功四种状态。具体样式由设计师根据 [Figma 设计规范](../../figma/README.md) §0.5 主题与 §4 四态模板决定，不在本节指定。

| 页面 | 空状态 | 加载状态 | 错误状态 | 成功状态 | 备注 |
|------|--------|---------|---------|---------|------|
| **教练审核列表** | 🔲 | 🔲 | 🔲 | 🔲 | 空状态提示暂无待审核 |
| **教练审核详情** | 🔲 | 🔲 | 🔲 | 🔲 | 含证书大图预览 |

---

## 14. 页面级设计决策

### 14.1 审核详情布局

- **背景**：需要同时查看教练资料与操作按钮
- **结论**：左侧/上方展示资料，右侧/下方展示通过/驳回按钮
- **影响范围**：教练审核详情页

### 14.2 驳回原因输入

- **背景**：教练需要知道驳回原因以便修改
- **结论**：驳回时强制填写原因，支持常用原因快捷选择
- **影响范围**：驳回弹窗

### 14.3 页面级交互说明

| 交互 | 触发 | 反馈 | 备注 |
|------|------|------|------|
| 列表页点击通过 | 点击按钮 | 弹出二次确认弹窗 | 确认后直接通过并刷新列表，不跳转详情页；通过原因非必填 |
| 列表页点击驳回 | 点击按钮 | 弹出轻量输入框 / Popconfirm | 必填驳回原因；确认后立即执行驳回 |
| 详情页点击通过 | 点击按钮 | 二次确认后更新状态 | 通过原因非必填 |
| 详情页点击驳回 | 点击按钮 | 底部/侧边展开原因输入区 | 驳回原因必填，支持常用原因快捷选择 |
| 查看证书大图 | 点击图片 | 弹窗放大预览 | 支持左右切换 |
| 展开申请历史 | 点击「申请历史」 | 展开时间轴/表格 | 展示该教练所有申请记录及驳回原因 |

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
| v1.1 | 2026-08-08 | PM | 教练审核列表「通过」按钮改为二次确认后直接通过，不再跳转详情页；新增详情页通过场景；场景数更新为 5 |

---

<p align="right">
<sub>本文档遵循 <a href="../../spec/user-story/SPECIFICATION.md">US 拆分规范</a>　·　复制自 <a href="../../spec/user-story/TEMPLATE.md">TEMPLATE.md</a>　·　填写示例见 <a href="../../spec/user-story/EXAMPLE.md">EXAMPLE.md</a></sub>
</p>
