# Spec Delta: admin-review-coach-application

> 本 spec 为 US-011 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 审核通过

系统 MUST 提供 `POST /api/admin/coach/applications/{id}/approve` 接口，供具有 `coach:audit` 权限的管理员将待审核教练资质设为已通过。系统 MUST 校验当前 coach.status=0。系统 MUST 更新 `coach.status=1`、`approved_at` 为当前时间、`auditor_id` 为当前管理员 ID。系统 MUST 记录 `coach_audit_log`。系统 MUST 异步发送审核通过通知给教练。系统 MUST 解锁教练端功能。

#### Scenario: 审核通过

```gherkin
Given 管理员已登录且有教练审核权限
And   存在 coach.status = 0 的入驻申请
When  管理员查看资料后点击「通过」
Then  coach.status 更新为 1（已通过）
And   coach.approved_at 记录当前时间
And   教练收到审核通过通知
And   教练端功能解锁
```

#### Scenario: 无权限审核

```gherkin
Given 管理员已登录但无教练审核权限
When  管理员尝试调用审核接口
Then  返回 HTTP 403 + 错误码 FORBIDDEN
And   coach.status 保持不变
And   前端提示"您没有操作权限"
```

### Requirement: REQ-002 审核驳回

系统 MUST 提供 `POST /api/admin/coach/applications/{id}/reject` 接口，供管理员驳回待审核教练资质。系统 MUST 要求填写驳回原因。系统 MUST 更新 `coach.status=2`、`rejection_reason` 为填写原因。系统 MUST 发送驳回通知及原因给教练。系统 MUST 允许教练在 US-040 中修改资料后重新提交（2 → 0）。

#### Scenario: 审核驳回

```gherkin
Given 管理员已登录且有教练审核权限
And   存在 coach.status = 0 的入驻申请
When  管理员点击「驳回」并填写原因"证书不清晰"
Then  coach.status 更新为 2（驳回）
And   coach.rejection_reason = "证书不清晰"
And   教练收到驳回通知及原因
And   教练可重新修改资料后提交
```

#### Scenario: 重复审核

```gherkin
Given 管理员已登录且有教练审核权限
And   存在 coach.status = 1 的已通过记录
When  管理员再次点击「通过」
Then  返回 HTTP 409 + 错误码 ALREADY_REVIEWED
And   coach.status 保持不变
And   前端提示"该申请已审核，无需重复操作"
```

### Requirement: REQ-003 待审核列表查询

系统 MUST 提供 `GET /api/admin/coach/applications` 接口，供管理员分页查询待审核教练列表。系统 MUST 支持按提交时间排序、按姓名/手机号搜索。系统 MUST 仅返回 `status=0` 且 `submitted_at IS NOT NULL` 的记录，避免草稿进入审核队列。

#### Scenario: 查询待审核列表

```gherkin
Given 管理员已登录且有教练审核权限
And   系统存在 3 条 status=0 的入驻申请
When  管理员查看教练审核列表
Then  返回 HTTP 200
And   列表包含 3 条待审核记录
And   每条记录展示教练姓名、提交时间、参考单价、证书缩略图
```
