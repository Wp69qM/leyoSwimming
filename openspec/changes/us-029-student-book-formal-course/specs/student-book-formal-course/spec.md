# Spec Delta: student-book-formal-course

> 本 spec 为 US-029 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-029-1 学员查看可约时段与 active 套餐

系统 MUST 为已登录学员提供已绑定教练的未来 7 天可约时段查询，以及当前 active 正价套餐列表查询。系统 MUST 在校验学员身份、套餐状态及绑定关系后返回数据。

#### Scenario: 正常进入预约页

```gherkin
Given 学员已登录
And   存在 active 套餐，available_count = 5，coach_id = 100
And   已绑定教练 100 在未来 7 天内有可约时段
When  学员进入「预约」页面
Then  系统返回教练 100 的可约时段列表
And   系统返回当前学员的 active 套餐列表
And   HTTP 状态码 = 200
```

#### Scenario: 未绑定教练时进入预约页

```gherkin
Given 学员已登录
And   名下无 active 正价套餐或套餐 coach_id 为空
When  学员进入「预约」页面
Then  系统返回 HTTP 400，错误码 NO_BOUND_COACH
And   提示"请先购买并绑定教练"
```

### Requirement: REQ-029-2 学员提交正价课程预约

系统 MUST 允许学员选择可约时段并提交预约。系统 MUST 按 FIFO 选择最早 active 套餐预占 1 课时，创建 status = 已预约 的 booking 记录，并对同一请求进行幂等处理。

#### Scenario: 正常预约已绑定教练时段

```gherkin
Given 学员已登录
And   存在 active 套餐，available_count = 5，coach_id = 100
And   已绑定教练 100 在明日 9:00 有可约时段
When  学员选择明日 9:00 并提交预约
Then  booking 创建成功，status = 已预约
And   package.reserved_count = 1，available_count = 4
And   返回预约详情
And   HTTP 状态码 = 201
```

#### Scenario: 时段已被他人预约

```gherkin
Given 学员已登录
And   明日 9:00 时段已被其他学员预约
When  学员尝试预约明日 9:00
Then  系统返回 HTTP 400，错误码 SLOT_TAKEN
And   package 课时不变
```

#### Scenario: 套餐可用课时不足

```gherkin
Given 学员已登录
And   active 套餐 available_count = 0
When  学员尝试预约
Then  系统返回 HTTP 400，错误码 NO_QUOTA
And   不创建 booking
```

#### Scenario: 重复提交预约

```gherkin
Given 学员已登录
And   已成功预约明日 9:00
When  学员再次提交同一时段预约
Then  系统返回 HTTP 200
And   不重复创建 booking
And   package 课时不变
```
