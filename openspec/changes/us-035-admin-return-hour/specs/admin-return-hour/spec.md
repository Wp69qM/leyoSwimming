# Spec Delta: admin-return-hour

> 本 spec 为 US-035 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-035-001 管理员返还课时

系统 MUST 允许具有 `MANAGE_BOOKING` 权限的管理员在特殊情况下对 `booking.status ∈ {已完成, 旷课}` 且 `package.consumed_count > 0` 的预约返还课时。系统 MUST 在单一事务内执行：`package.consumed_count -1`、`package.available_count +1`、创建 `hour_return` 记录、写 `audit_log`、发送学员通知。系统 MUST 要求管理员填写返还原因分类（`reason_type`）作为必填字段。系统 SHALL 在返还后 `package.consumed_count < package.total_hours` 且原状态为 `exhausted` 时将 `package.status` 复活为 `active`。系统 MUST 拒绝无可扣课时（`consumed_count = 0`）、不可返还的 booking 状态、以及无权限的操作。

#### Scenario: 管理员正常返还课时

```gherkin
Given 管理员已登录且具有 MANAGE_BOOKING 权限
And   存在 booking.status = 已完成
And   对应 package.consumed_count = 3，available_count = 7，status = active
When  管理员点击「返还课时」并提交 reason_type = 1（教练误操作），reason_detail = "教练误操作标记旷课"
Then  package.consumed_count = 2，available_count = 8
And   hour_return 记录创建，booking_id 匹配，admin_id = 当前管理员，reason_type = 1，returned_hours = 1
And   audit_log 记录管理员返还操作
And   学员收到课时返还通知
And   HTTP 状态码 = 200
```

#### Scenario: 无可扣课时可返还

```gherkin
Given 管理员已登录且具有 MANAGE_BOOKING 权限
And   存在 booking.status = 已完成
And   对应 package.consumed_count = 0，available_count = 10
When  管理员尝试返还课时
Then  系统返回 HTTP 400，错误码 NO_CONSUMED_HOUR
And   package.consumed_count 与 available_count 不变
And   不创建 hour_return 记录
And   不写 audit_log
```

#### Scenario: 非可返还 booking 状态

```gherkin
Given 管理员已登录且具有 MANAGE_BOOKING 权限
And   存在 booking.status = 已取消
When  管理员尝试返还课时
Then  系统返回 HTTP 400，错误码 BOOKING_NOT_RETURNABLE
And   package 课时不变
And   不创建 hour_return 记录
```

#### Scenario: 返还后套餐从 exhausted 复活为 active

```gherkin
Given 管理员已登录且具有 MANAGE_BOOKING 权限
And   存在 booking.status = 已完成
And   对应 package.status = exhausted，consumed_count = 10，total_hours = 10，available_count = 0
When  管理员返还 1 课时
Then  package.consumed_count = 9，available_count = 1
And   package.status = active
And   hour_return 记录创建
And   HTTP 状态码 = 200
```

#### Scenario: 无权限用户访问

```gherkin
Given 用户已登录但角色为学员
And   存在 booking.status = 已完成
When  该用户调用 POST /api/admin/bookings/{booking_id}/return-hour
Then  系统返回 HTTP 403
And   package 课时不变
And   不创建 hour_return 记录
```
