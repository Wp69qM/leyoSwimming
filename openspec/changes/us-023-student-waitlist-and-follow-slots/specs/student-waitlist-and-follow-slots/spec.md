# Spec Delta: student-waitlist-and-follow-slots

> 本 spec 为 US-023 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 候补时段

系统 MUST 允许持有 active 套餐的学员对已满的 schedule_slot 加入候补。系统 MUST 校验时段已满、用户有 active 套餐、未重复候补。创建后 waitlist.status = waiting。

#### Scenario: 成功加入候补

```gherkin
Given 学员已登录且持有 active 套餐
And   教练 A 明日 9:00 时段已约满
When  学员点击「加入候补」
Then  系统创建 waitlist.status = waiting
And   学员在「我的候补」看到该记录
And   接口返回 HTTP 201
```

#### Scenario: 时段未满不能候补

```gherkin
Given 学员已登录且持有 active 套餐
And   教练 A 明日 11:00 时段仍有余位
When  学员查看该时段
Then  「加入候补」按钮置灰
And   文案提示"该时段可直接预约"
```

#### Scenario: 无 active 套餐不能候补

```gherkin
Given 注册用户已登录但无 active 套餐
And   教练 A 明日 9:00 时段已约满
When  学员点击「加入候补」
Then  系统返回 HTTP 400，错误码 NO_ACTIVE_PACKAGE
And   不创建 waitlist 记录
```

#### Scenario: 重复加入同一时段候补

```gherkin
Given 学员已登录且已加入教练 A 明日 9:00 的候补
When  学员再次点击「加入候补」
Then  系统返回 HTTP 400，错误码 ALREADY_IN_WAITLIST
And   不重复创建记录
```

### Requirement: REQ-002 关注时段

系统 MUST 允许已登录用户关注任意已释放的 schedule_slot。关注不自动预约，仅用于提醒。

#### Scenario: 成功关注时段

```gherkin
Given 用户已登录
And   教练 A 明日 10:00 时段已释放
When  用户点击「关注」
Then  系统创建 slot_follow 记录
And   用户在「我的关注」看到该记录
And   接口返回 HTTP 201
```
