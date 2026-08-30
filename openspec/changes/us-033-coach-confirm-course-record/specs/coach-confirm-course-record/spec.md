# Spec Delta: coach-confirm-course-record

> 本 spec 为 US-033 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-033-1 教练确认上课并扣除课时

系统 MUST 允许对应 booking 的教练在课程结束后确认上课。教练确认时系统 MUST 填写上课记录，将 booking.status 更新为 已完成，并将 package.reserved_count 转换为 consumed_count。系统 MUST 拒绝课程未结束、booking 已终态或非本课程教练的操作。

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

#### Scenario: 课程尚未结束

```gherkin
Given 教练已登录
And   存在 booking.status = 已预约，开课时间为今日 14:00，结束时间为 15:00
And   当前时间为今日 12:00
When  教练点击「确认上课并扣除课时」
Then  系统返回 HTTP 400，错误码 CLASS_NOT_ENDED
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

### Requirement: REQ-033-2 教练标记学员旷课并自动扣课时

系统 MUST 允许对应 booking 的教练在课程结束后、24h 确认窗口内标记学员未到课。系统 MUST 将 booking.status 更新为旷课（cancel_reason = 3），将 package.reserved_count 转换为 consumed_count，并记录 audit_log。系统 MUST 拒绝课程未结束、booking 已终态（含 24h 超时系统自动确认后）、或非本课程教练的操作。

#### Scenario: 教练标记学员旷课并自动扣课时

```gherkin
Given 教练已登录
And   存在 booking.status = 上课中，开课时间为今日 10:00，结束时间为 11:00
And   当前时间为今日 11:05（课程已结束，学员未到场）
And   对应 package.reserved_count = 1，consumed_count = 2，available_count = 7
When  教练点击「标记学员未到课」并填写备注"学员未到场，电话未接"
Then  booking.status = 旷课
And   package.reserved_count = 0，consumed_count = 3，available_count = 7
And   audit_log 记录标记人 = 教练，cancel_reason = 3（学员旷课）
And   学员收到"已被标记旷课，课时已扣除"通知
And   HTTP 状态码 = 200
```

#### Scenario: 课程尚未结束

```gherkin
Given 教练已登录
And   存在 booking.status = 上课中，开课时间为今日 10:00，结束时间为 11:00
And   当前时间为今日 10:30（课程尚未结束）
When  教练点击「标记学员未到课」
Then  系统返回 HTTP 400，错误码 CLASS_NOT_ENDED
And   package 课时不变
And   booking.status 保持 上课中
```

#### Scenario: booking 已取消

```gherkin
Given 教练已登录
And   存在 booking.status = 已取消
When  教练点击「标记学员未到课」
Then  系统返回 HTTP 400，错误码 BOOKING_NOT_CONFIRMABLE
And   package 课时不变
```

#### Scenario: 非本课程教练操作

```gherkin
Given 教练 B 已登录
And   存在 booking.coach_id = 100
And   教练 B 的 coach_id = 200
When  教练 B 对该 booking 点击「标记学员未到课」
Then  系统返回 HTTP 403
And   booking 与 package 均无变化
```


