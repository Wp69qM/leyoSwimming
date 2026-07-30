# Spec Delta: trial-student-book-cancel

> 本 spec 为 US-018 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 体验课学员预约

系统 MUST 允许持有 active 体验套餐的学员预约体验课时段。系统 MUST 校验套餐可用且时段未被占用；预约成功时 MUST 预占 1 节课时并创建 booking。

#### Scenario: 正常预约体验课

```gherkin
Given 学员持有 active 体验套餐且 available = 1
And   教练 A 明日 9:00 时段未被预约
When  学员预约该时段
Then  booking.status = 已预约，course_type = 0
And   package.reserved = 1，package.available = 0
And   接口返回 HTTP 201
```

#### Scenario: 无可用体验套餐

```gherkin
Given 学员名下无 active 体验套餐
When  学员尝试预约体验课
Then  系统返回 HTTP 400，错误码 NO_TRIAL_PACKAGE
And   不创建 booking
```

#### Scenario: 时段已被预约

```gherkin
Given 教练 A 明日 9:00 已被其他学员预约
When  学员尝试预约该时段
Then  系统返回 HTTP 409，错误码 SLOT_TAKEN
And   package 课时不变
```

### Requirement: REQ-002 体验课学员取消预约

系统 MUST 允许学员取消已预约的体验课。距开课 ≥ 24h 时 MUST 直接释放课时；距开课 < 24h 时 MUST 提交教练审批。

#### Scenario: 开课前 24 小时外取消

```gherkin
Given 学员已预约后天 9:00 体验课
And   当前距开课 48 小时
When  学员取消预约
Then  booking.status = 已取消
And   package.reserved = 0，package.available = 1
And   接口返回 HTTP 200
```

#### Scenario: 开课前 24 小时内取消需审批

```gherkin
Given 学员已预约明日 9:00 体验课
And   当前距开课 12 小时
When  学员申请取消
Then  系统创建 cancel_request
And   booking.status 保持已预约
And   提示"已提交教练审批"
```
