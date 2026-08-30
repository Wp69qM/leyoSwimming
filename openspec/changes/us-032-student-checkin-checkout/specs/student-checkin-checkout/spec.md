# Spec Delta: student-checkin-checkout

> 本 spec 为 US-032 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-032-1 学员在指定时间窗口内签到

系统 MUST 允许学员对待上课或上课中的 booking 进行签到，签到窗口为开课时间前 10 分钟至课程结束时间（PRD §6.1.2）。系统 MUST 拒绝 已预约 状态的签到请求。系统 MUST 记录签到时间戳并通知教练，对重复签到进行幂等处理，对课程已结束或不可签到的 booking 返回明确错误。

#### Scenario: 正常签到

```gherkin
Given 学员已登录
And   存在 booking.status = 待上课，开课时间为今日 10:00，结束时间为 11:00
And   当前时间为今日 9:50（开课 10 分钟前，已进入待上课状态）
When  学员点击「一键签到」
Then  booking.checked_in_at 写入当前时间戳
And   booking.status 保持 待上课
And   教练收到「学员已到场」通知
And   返回 HTTP 200
```

#### Scenario: 不在签到窗口内

```gherkin
Given 学员已登录
And   存在 booking.status = 已预约，开课时间为今日 10:00
And   当前时间为今日 9:30（课前 30 分钟，状态仍为 已预约，未进入待上课）
When  学员尝试签到
Then  系统返回 HTTP 400，错误码 CHECKIN_WINDOW_NOT_OPEN
And   booking.status 保持 已预约
And   不更新 booking.checked_in_at
```

#### Scenario: 重复签到

```gherkin
Given 学员已登录
And   存在 booking.checked_in_at 已设置
When  学员再次点击「一键签到」
Then  系统返回 HTTP 200
And   booking.checked_in_at 保持不变
And   不重复发送通知
```

### Requirement: REQ-032-2 学员查看上课记录并提交课后总结

系统 MUST 允许学员在课程开始后查看教练填写的上课记录，并提交自我总结。系统 MUST 将总结保存到 `course_record.student_summary_json`，并在教练尚未填写记录时展示空状态占位。

#### Scenario: 正常提交课后总结

```gherkin
Given 学员已登录
And   存在 booking.status = 已完成
And   对应 course_record 已创建
When  学员填写课后总结：身体感受"稍有疲劳"、学习效果"蛙泳划手有进步"、问题反馈"换气节奏不稳"
Then  course_record.student_summary_json 保存上述内容
And   返回 HTTP 200
```

#### Scenario: 查看教练尚未填写的上课记录

```gherkin
Given 学员已登录
And   存在 booking.status = 已完成
And   教练尚未创建 course_record
When  学员进入「上课记录」页
Then  系统返回空记录占位
And   提示"教练还未填写本节课记录"
And   仍允许填写课后总结
```
