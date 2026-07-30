# Spec Delta: coach-confirm-course-record

> 本 spec 为 US-033 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-033-1 教练确认上课并扣除课时

系统 MUST 允许对应 booking 的教练在课程开始之后确认上课。教练确认时系统 MUST 填写上课记录，将 booking.status 更新为 已完成，并将 package.reserved_count 转换为 consumed_count。系统 MUST 拒绝课程未开始、booking 已终态或非本课程教练的操作。

#### Scenario: 教练确认上课并扣除课时

```gherkin
Given 教练已登录
And   存在 booking.status = 上课中，开课时间为今日 10:00，结束时间为 11:00
And   当前时间为今日 11:05
And   对应 package.reserved_count = 1，consumed_count = 2，available_count = 7
When  教练填写课程内容"自由泳打腿"并点击「确认上课并扣除课时」
Then  booking.status = 已完成
And   package.reserved_count = 0，consumed_count = 3，available_count = 7
And   course_record 创建成功，content = "自由泳打腿"
And   学员收到课时扣除通知
And   HTTP 状态码 = 200
```

#### Scenario: 课程尚未开始

```gherkin
Given 教练已登录
And   存在 booking.status = 已预约，开课时间为今日 14:00
And   当前时间为今日 12:00
When  教练点击「确认上课并扣除课时」
Then  系统返回 HTTP 400，错误码 CLASS_NOT_STARTED
And   package 课时不变
And   不创建 course_record
```

#### Scenario: booking 已取消

```gherkin
Given 教练已登录
And   存在 booking.status = 已取消
When  教练点击「确认上课并扣除课时」
Then  系统返回 HTTP 400，错误码 BOOKING_NOT_CONFIRMABLE
And   package 课时不变
```

#### Scenario: 非本课程教练操作

```gherkin
Given 教练 B 已登录
And   存在 booking.coach_id = 100
And   教练 B 的 coach_id = 200
When  教练 B 对该 booking 点击「确认上课并扣除课时」
Then  系统返回 HTTP 403
And   booking 与 package 均无变化
```

### Requirement: REQ-033-2 管理员返还课时

系统 MUST 允许具有权限的管理员在特殊情况下对已完成/已扣课时的 booking 返还课时。系统 MUST 减少 package.consumed_count 并增加 available_count，创建 hour_return 记录并记录审计日志。

#### Scenario: 管理员返还课时

```gherkin
Given 管理员已登录
And   存在 booking.status = 已完成
And   对应 package.consumed_count = 3，available_count = 7
When  管理员点击「返还课时」并填写原因"教练误操作"
Then  package.consumed_count = 2，available_count = 8
And   hour_return 记录创建，reason = "教练误操作"
And   audit_log 记录管理员返还操作
And   学员收到课时返还通知
And   HTTP 状态码 = 200
```
