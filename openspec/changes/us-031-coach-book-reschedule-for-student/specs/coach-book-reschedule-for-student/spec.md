# Spec Delta: coach-book-reschedule-for-student

> 本 spec 为 US-031 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-031-1 教练为学员代约正价课程

系统 MUST 允许已登录且资质通过的教练为已绑定学员代约正价课程。系统 MUST 校验学员存在 active 套餐且已绑定当前教练、目标时段有效且空闲，并按 FIFO 预占 1 课时创建 booking，记录操作来源为 coach。

#### Scenario: 教练成功代约正价课程

```gherkin
Given 教练已登录且 coach_id = 100
And   学员 A 已绑定教练 100，存在 active 套餐 available_count = 5
And   教练 100 在明日 9:00 有可约时段
When  教练为学员 A 提交明日 9:00 代约
Then  booking 创建成功，status = 已预约，operator = coach
And   package.reserved_count = 1，available_count = 4
And   学员 A 收到代约成功通知
And   HTTP 状态码 = 201
```

#### Scenario: 时段已被他人预约

```gherkin
Given 教练已登录
And   明日 9:00 时段已被其他学员预约
When  教练尝试为学员 A 代约明日 9:00
Then  系统返回 HTTP 400，错误码 SLOT_TAKEN
And   package 课时不变
```

#### Scenario: 学员未绑定当前教练

```gherkin
Given 教练已登录且 coach_id = 100
And   学员 B 的套餐绑定 coach_id = 200
When  教练 100 尝试为学员 B 代约
Then  系统返回 HTTP 400，错误码 NO_BOUND_COACH
And   不创建 booking
```

### Requirement: REQ-031-2 教练为学员改约正价课程

系统 MUST 允许教练为学员已有的 status ∈ {已预约, 待上课} 的 booking 改约到其他可约时段。系统 MUST 先取消原预约释放课时，再创建新预约，并保证整个改约操作的原子性。

#### Scenario: 教练成功改约正价课程

```gherkin
Given 教练已登录
And   学员 A 已存在 booking.status = 已预约，开课时间为明日 9:00
And   明日 14:00 有可约时段
When  教练为学员 A 提交改约到明日 14:00
Then  原 booking.status = 已取消，package 原课时释放
And   新 booking 创建成功，status = 已预约
And   package.reserved_count 保持 1，available_count = 4
And   学员 A 收到改约通知
And   HTTP 状态码 = 200
```

#### Scenario: 改约时 booking 已取消

```gherkin
Given 教练已登录
And   学员 A 的 booking.status = 已取消
When  教练尝试为该 booking 改约
Then  系统返回 HTTP 400，错误码 BOOKING_NOT_RESCHEDULABLE
And   不创建新 booking
```
