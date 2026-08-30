> 本 spec 为 US-011 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 审核通过

系统 MUST 提供 `POST /api/admin/coach/application/approve` 接口，供具有 `coach:audit` 权限的管理员将待审核教练资质设为已通过。请求体 MUST 包含 `{ applicationId, remark? }`。系统 MUST 校验当前 `coach_application.status = pending`。系统 MUST 将 `coach_application` 快照字段覆盖写入 `coach` 表生效资料。系统 MUST 将 `coach_certificate_application` 快照证书覆盖写入 `coach_certificate` 表。系统 MUST 更新 `coach.status = 1`、`coach.approved_at` 为当前时间。系统 MUST 更新 `coach_application.status = approved`、`approved_at`、`approved_by`。系统 MUST 写入 `coach_audit_log`：`action='approve'`、`from_status=0`、`to_status=1`、`application_id` 为当前申请 ID。系统 MUST 异步发送审核通过通知给教练。系统 MUST 解锁教练端功能。

#### Scenario: 审核通过

```gherkin
Given 管理员已登录且有教练审核权限
And   存在 coach_application.status = pending 的入驻申请（application_id=10001，previous_coach_status=-1）
And   该申请快照中姓名为"张教练"、参考单价为 300.00 元
When  管理员查看 coach_application 快照资料后点击「通过」
Then  coach_application.status 更新为 approved
And   coach_application.approved_at 记录当前时间
And   coach 表生效资料被覆盖为 application 快照内容（姓名"张教练"、参考单价 300.00 元）
And   coach.status 更新为 1（已通过）
And   coach.approved_at 记录当前时间
And   coach_certificate 表被该 application 快照证书覆盖
And   系统写入 coach_audit_log：action='approve'、from_status=0、to_status=1、application_id=10001
And   教练收到审核通过通知
And   教练端功能解锁
```

#### Scenario: 无权限审核

```gherkin
Given 管理员已登录但无教练审核权限
And   存在 coach_application.status = pending 的申请
When  管理员尝试调用审核接口
Then  返回 HTTP 403 + 错误码 FORBIDDEN
And   coach_application.status 保持 pending
And   coach.status 保持不变
And   前端提示"您没有操作权限"
```

### Requirement: REQ-002 审核驳回

系统 MUST 提供 `POST /api/admin/coach/application/reject` 接口，供管理员驳回待审核教练资质。请求体 MUST 包含 `{ applicationId, reason }`，且 `reason` 必填。系统 MUST 要求填写驳回原因。系统 MUST 校验当前 `coach_application.status = pending`。系统 MUST 更新 `coach_application.status = rejected` 并写入 `rejection_reason`。系统 MUST 根据 `coach_application.previous_coach_status` 恢复 `coach.status`：previous_coach_status=-1 时目标为 2；=2 时目标为 2；=3 时目标为 3。系统 MUST 写入 `coach_audit_log`：`action='reject'`、`from_status=0`、`to_status=恢复后的status`、`reason=填写原因`、`application_id` 为当前申请 ID。系统 MUST 发送驳回通知及原因给教练。系统 MUST 允许教练在 US-010 中修改资料后重新提交。

#### Scenario: 审核驳回

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

#### Scenario: 已离职教练重新入驻申请被驳回

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

#### Scenario: 重复审核

```gherkin
Given 管理员已登录且有教练审核权限
And   存在 coach_application.status = approved 的已通过记录
When  管理员再次点击「通过」
Then  返回 HTTP 409 + 错误码 ALREADY_REVIEWED
And   coach_application.status 保持不变
And   coach.status 保持不变
And   前端提示"该申请已审核，无需重复操作"
```

### Requirement: REQ-003 审核申请列表查询

系统 MUST 提供 `POST /api/admin/coach/application/list` 接口，供管理员分页查询教练入驻/重新入驻申请列表。请求体 MUST 包含 `{ page, pageSize, keyword, status }`。系统 MUST 按 `coach_id` 聚合，每个教练只展示一行，行内展示该教练在筛选条件下的最新申请信息。系统默认 MUST 仅返回存在 `coach_application.status = pending` 的教练，避免草稿进入审核队列；同时 MUST 支持按 `status` 筛选为全部 / `pending` / `approved` / `rejected`。系统 MUST 支持按提交时间排序、按姓名/手机号搜索。系统 MUST 在列表中展示教练 ID、姓名、性别、年龄、教学年限、擅长泳姿、最新提交时间、流程状态、最新申请 ID 与 `previous_coach_status` 标记（用于区分重新入驻申请）。系统 MUST NOT 在列表中展示头像。

#### Scenario: 查询待审核列表（默认）

```gherkin
Given 管理员已登录且有教练审核权限
And   系统存在 3 条 coach_application.status = pending 的入驻申请
And   系统存在 1 条 coach_application.status = approved 的入驻申请
When  管理员查看教练审核列表
Then  返回 HTTP 200
And   列表默认包含 3 条待审核记录
And   approved 记录未出现在默认列表中
```

#### Scenario: 按状态筛选全部记录

```gherkin
Given 管理员已登录且有教练审核权限
And   系统存在 pending、approved、rejected 状态的申请各 1 条
When  管理员选择状态筛选"全部"
Then  返回 HTTP 200
And   列表包含 3 条记录
And   每条记录展示教练姓名、手机号、参考单价、证书缩略图
And   previous_coach_status=3 的记录标记为"重新入驻"
```

### Requirement: REQ-004 审核详情查询

系统 MUST 提供 `POST /api/admin/coach/application/detail` 接口，供具有 `coach:audit` 权限的管理员查看 coach_application 快照完整资料及该教练的申请历史。请求体 MUST 包含 `{ applicationId }`。系统 MUST 返回基础信息（姓名/昵称、手机号、性别、年龄、邮箱、微信二维码）、实名与资质（身份证号、身份证正面照、身份证反面照、教练资格证、健康证、个人形象照）、教学履历（任教年限、总学员数、总课时数、擅长泳姿、个人简介）、服务设置（参考单价）及 `previous_coach_status`。系统 MUST 在 `application_history` 中返回该教练的所有历史申请记录（申请 ID、状态、提交时间、审核时间、审核人、驳回原因）。系统 MUST 在管理员审核场景中完整展示手机号与身份证号，不进行脱敏处理，以便管理员核对资质与联系教练。

#### Scenario: 查看审核详情

```gherkin
Given 管理员已登录且有教练审核权限
And   存在 coach_application.status = pending 的入驻申请（application_id=10001）
And   该申请快照中手机号="13800138000"、身份证号="110101199001011234"
When  管理员打开该申请审核详情页
Then  返回 HTTP 200
And   页面展示完整手机号"13800138000"
And   页面展示完整身份证号"110101199001011234"
And   页面展示身份证正反面照、教练资格证、健康证、个人形象照
And   页面展示基础信息、教学履历、服务设置全部字段
```
