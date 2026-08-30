# Spec Delta: student-cancel-formal-course

> 本 spec 为 US-030 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-030-1 学员取消正价课程预约

系统 MUST 允许学员在课程开始前取消 status ∈ {已预约, 待上课} 的 booking。系统 MUST 区分 24 小时时间窗：≥24h 直接取消并释放课时，<24h 创建 booking_cancellation 待教练审批。系统 MUST 对重复取消和非可取消状态返回明确错误。因特殊原因（场馆闭馆、教练离职等）被管理员或系统取消的 booking，学员 MUST 可在 US-049 客服工单中申诉。

#### Scenario: 开课前 24 小时外直接取消

```gherkin
Given 学员已登录
And   存在 booking.status = 已预约，开课时间为明日 10:00
And   当前时间为今日 9:00（距开课 25 小时）
And   package.reserved_count = 1，available_count = 4
When  学员点击「取消预约」
Then  booking.status = 已取消
And   package.reserved_count = 0，available_count = 5
And   系统通知教练
And   返回 HTTP 200
```

#### Scenario: 开课前 24 小时内提交取消申请

```gherkin
Given 学员已登录
And   存在 booking.status = 已预约，开课时间为今日 22:00
And   当前时间为今日 10:00（距开课 12 小时）
When  学员点击「取消预约」并选择原因"临时有事"
Then  系统创建 booking_cancellation.status = 待审批
And   booking.status 保持已预约
And   package 课时暂不变
And   系统通知教练审批
And   返回 HTTP 201
```

#### Scenario: <2h 特殊原因取消申诉

```gherkin
Given 学员已登录
And   存在 booking.status = 已预约，开课时间为今日 12:00
And   当前时间为今日 10:30（距开课 1.5 小时，< 2h 阈值）
And   学员此前已提交 24h 内取消申请，但被教练拒绝
And   package.reserved_count = 1，available_count = 4
When  学员在预约详情页点击「特殊原因申诉」并提交原因"突发疾病" + 上传医院证明
Then  系统创建 dispute_refund.status = 争议退款处理中
And   booking.status 保持已预约（不立即释放课时）
And   package 课时暂不变
And   系统通知管理员在 3 工作日内审核
And   返回 HTTP 201
```

#### Scenario: 重复取消

```gherkin
Given booking.status = 已取消
When  学员再次点击「取消预约」
Then  系统返回 HTTP 400，错误码 BOOKING_NOT_CANCELLABLE
And   不创建新的取消申请
```

#### Scenario: 因特殊原因被取消后申诉

```gherkin
Given 学员已登录
And   存在 booking.status = 已取消，cancel_reason = 4（场馆闭馆）
When  学员进入该 booking 详情页
Then  页面展示取消原因为「场馆闭馆」
And   提供「申诉」入口跳转至 US-049 客服工单
```

### Requirement: REQ-030-2 教练审批 24 小时内取消申请

系统 MUST 允许对应 booking 的教练审批待处理的取消申请。教练同意后 booking 变为已取消并释放课时；拒绝后 booking 保持已预约，学员收到通知。系统 MUST 对超时未审批的申请自动标记为已拒绝（超时）。

#### Scenario: 教练同意取消申请

```gherkin
Given 学员已提交待审批的取消申请
And   booking.status = 已预约
And   package.reserved_count = 1，available_count = 4
When  教练点击「同意取消」
Then  booking.status = 已取消
And   package.reserved_count = 0，available_count = 5
And   booking_cancellation.status = 已通过
And   返回 HTTP 200
```

#### Scenario: 取消申请超时未处理

```gherkin
Given 学员提交的取消申请已等待 24 小时
And   教练未审批
When  系统定时任务执行
Then  booking_cancellation.status = 已拒绝（超时）
And   booking.status 保持已预约
And   package 课时不变
And   学员收到"取消申请已超时"通知
```
