# Spec Delta: coach-reapply-entry

> 本 spec 为 US-040 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 已离职教练可发起重新入驻

系统 MUST 允许 `coach.status = 3`（已离职）的教练发起重新入驻。已离职教练登录教练端时，US-051 / US-054 MUST 按 `coach_status = 3` 直接跳转 US-010 的 C-入驻资料填写页。教练在「我的」页面点击「重新入驻」时，系统 MUST 校验 `coach.status = 3` 并允许进入 US-010 的 C-入驻资料填写页；校验不通过时返回对应错误码，且 `coach.status` 不发生变化。实际资料填写、校验与提交 MUST 由 US-010 处理；US-010 MUST 为重新入驻创建 `coach_application` pending 快照，`previous_coach_status = 3`，并将 `coach.status` 从 3 更新为 0。系统 MUST 在历史数据不隔离、不回滚的前提下复用原 `coach` 记录。

#### Scenario: 已离职教练登录后自动进入 US-010 重新入驻

```gherkin
Given 教练 C 当前 coach.status = 3（已离职）
And   教练 C 的账号状态正常
And   coach 表中姓名为"李教练"、任教年限 5 年、参考单价 300.00 元
When  教练 C 登录教练端（US-051 / US-054）
Then  登录响应返回 coach_status = 3
And   前端按 coach_status=3 直接跳转 C-入驻资料填写页（US-010）
And   页面顶部展示重新入驻说明条"你的账号已离职，请重新提交入驻资料，审核通过后即可恢复接单。"
And   表单自动回显姓名"李教练"、任教年限 5 年、参考单价 300.00 元
When  教练 C 确认资料并点击「提交审核」
Then  US-010 创建 coach_application pending 快照（previous_coach_status=3）
And   coach.status 从 3 更新为 0（待审核）
And   coach.submitted_at 更新为当前时间
And   系统返回"提交成功，等待审核"
```

#### Scenario: 已离职教练从「我的」页面进入重新入驻

```gherkin
Given 教练 C 当前 coach.status = 3（已离职）
When  教练 C 在「我的」页面点击「重新入驻」
Then  系统调用 POST /api/coach/v1/reapply/entry 校验 status=3
And   返回 entry_allowed=true 与 redirect_to=coach_onboarding_page
And   页面跳转至 US-010 的 C-入驻资料填写页
And   顶部展示重新入驻说明条
And   表单自动回显 coach 表历史内容
```

#### Scenario: 非已离职教练无法重新入驻

```gherkin
Given 教练 C 的 coach.status = 1（已通过）
When  教练 C 访问「我的」页面「重新入驻」入口
Then  入口隐藏或按钮置灰
And   点击时提示"当前状态不可重新入驻"
And   调用 POST /api/coach/v1/reapply/entry 返回 HTTP 403 + 错误码 COACH_STATUS_NOT_ALLOWED
And   coach.status 保持 1 不变
```

#### Scenario: 重复发起重新入驻

```gherkin
Given 教练 C 的 coach.status = 0（待审核）
And   存在 coach_application.status = pending 且 previous_coach_status=3 的申请
When  教练 C 再次点击「重新入驻」
Then  系统进入 US-010 的 C-入驻资料填写页
And   US-010 重复提交校验返回错误码 COACH_APPLICATION_PENDING
And   coach.status 保持 0，不产生新的待审核记录
And   调用 POST /api/coach/v1/reapply/entry 返回 HTTP 409 + 错误码 REAPPLY_ALREADY_PENDING
```

### Requirement: REQ-002 管理员可通过重新入驻审核

系统 MUST 允许管理员通过重新入驻申请。审核通过后，`coach_application` 快照字段 MUST 覆盖写入 `coach` 表生效资料，`coach.status` MUST 从 0 更新为 1，教练恢复教学资格。历史评分/评价 MUST 保留，且仅对老学员可见，对新学员隐藏。

#### Scenario: 管理员通过重新入驻

```gherkin
Given 教练 C 的 coach.status = 0（待审核）
And   存在 coach_application.status = pending 且 previous_coach_status=3 的重新入驻申请
And   该快照中姓名为"李教练（新）"、参考单价为 350.00 元
When  管理员在后台调用 POST /api/admin/coach/applications/{application_id}/approve
Then  coach_application.status 更新为 approved
And   coach 表生效资料被覆盖为快照内容（姓名"李教练（新）"、参考单价 350.00 元）
And   coach.status 更新为 1（已通过）
And   coach_certificate 表被快照证书覆盖
And   教练恢复教学资格，可继续使用 US-012 / US-013 / US-014 等功能
And   老学员仍可在历史记录中查看该教练过往评分
And   新学员查看教练主页时不展示历史评分
And   系统返回"审核通过"
```

### Requirement: REQ-003 系统应限制重新入驻仅对已离职教练开放

系统 MUST 阻止 `coach.status ≠ 3` 的教练访问或提交重新入驻入口。US-051 / US-054 登录响应中的 `coach_status` 不为 3 时，前端 MUST 不进入 US-010 C-入驻资料填写页。

#### Scenario: 已通过教练看不到重新入驻入口

```gherkin
Given 教练 C 的 coach.status = 1（已通过）
When  教练 C 打开「我的」页面
Then  「重新入驻」入口不展示
And   调用 POST /api/coach/v1/reapply/entry 返回 HTTP 403
And   coach.status 保持 1 不变
```

### Requirement: REQ-004 系统应防止重复发起重新入驻

系统 MUST 确保教练在已存在 pending 状态的 `coach_application` 时无法再次提交重新入驻申请。`POST /api/coach/v1/reapply/entry` MUST 在 `coach.status = 0` 且存在 pending 申请时返回 `REAPPLY_ALREADY_PENDING`。

#### Scenario: 待审核期间再次发起重新入驻被拒绝

```gherkin
Given 教练 C 的 coach.status = 0（待审核）
And   存在 coach_application.status = pending 的申请
When  教练 C 调用 POST /api/coach/v1/reapply/entry
Then  API 返回 HTTP 409 + 错误码 REAPPLY_ALREADY_PENDING
And   coach.status 保持 0
And   不创建新的申请记录
```

### Requirement: REQ-005 管理员可拒绝重新入驻审核

系统 MUST 允许管理员拒绝重新入驻申请。拒绝后，`coach_application.status` MUST 更新为 rejected 并写入 `rejection_reason`；`coach.status` MUST 从 0 恢复为 3。`coach` 表生效资料 MUST 保持拒绝前状态不变，教练可再次进入 US-010 填写页重新提交。

#### Scenario: 重新入驻审核被拒绝

```gherkin
Given 教练 C 的 coach.status = 0（待审核）
And   存在 coach_application.status = pending 且 previous_coach_status=3 的重新入驻申请
And   coach 表当前生效姓名为"李教练"、参考单价为 300.00 元
When  管理员调用 POST /api/admin/coach/applications/{application_id}/reject 并填写原因="资料不完整"
Then  coach_application.status 更新为 rejected
And   coach_application.rejection_reason = "资料不完整"
And   coach.status 更新为 3（已离职）
And   coach 表生效资料保持不变（姓名"李教练"、参考单价 300.00 元）
And   系统写入 coach_audit_log：action='reject'、from_status=0、to_status=3、reason="资料不完整"
And   教练端再次进入 US-010 填写页时顶部展示红色驳回原因条
And   表单回显 coach_application 快照内容，教练可再次编辑提交
And   系统返回"审核已拒绝"
```
