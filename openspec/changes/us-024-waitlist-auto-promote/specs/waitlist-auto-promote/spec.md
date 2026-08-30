# Spec Delta: waitlist-auto-promote

> 本 spec 为 US-024 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 候补自动转正

系统 MUST 在 `schedule_slot` 因取消释放名额时，按 `waitlist.created_at` 顺序为首位符合条件的学员自动创建 booking。系统 MUST 校验该学员仍持有 active 套餐且 `available > 0`、waitlist.status = waiting。校验通过后 MUST 创建 booking.status = 已预约，扣减 package.available 并增加 package.reserved，将 waitlist.status 更新为 converted，并取消该学员其他 waiting 状态的 waitlist。

#### Scenario: 取消后首位候补转正

```gherkin
Given 教练 A 明日 9:00 时段已有 1 条 booking
And   学员 X 排队候补该时段（waitlist.status=waiting）
And   学员 X 持有 active 套餐且 available=3
When  原预约学员取消该 booking
Then  系统自动为学员 X 创建 booking.status = 已预约
And   学员 X 套餐 available=2，reserved=1
And   waitlist.status = converted
And   系统向学员 X 发送预约成功通知
```

#### Scenario: 多人候补仅首位转正

```gherkin
Given 教练 A 明日 9:00 时段释放 1 个名额
And   waitlist 顺序为：学员 X（第1）、学员 Y（第2）
And   两人均持有 active 套餐且 available > 0
When  系统处理候补转正
Then  仅学员 X 被自动预约
And   学员 Y 保持 waiting 状态
```

#### Scenario: 首位候补无可用课时

```gherkin
Given 教练 A 明日 9:00 时段释放 1 个名额
And   首位候补学员 X 的 active 套餐 available=0
And   次位候补学员 Y 的 active 套餐 available=2
When  系统处理候补转正
Then  跳过学员 X，为学员 Y 创建 booking
And   学员 Y 的 waitlist.status = converted
And   学员 X 的 waitlist 保持 waiting
```

#### Scenario: 首位候补已取消

```gherkin
Given 教练 A 明日 9:00 时段释放 1 个名额
And   首位候补学员 X 的 waitlist.status = cancelled
And   次位候补学员 Y 的 waitlist.status = waiting
When  系统处理候补转正
Then  跳过学员 X，为学员 Y 创建 booking
And   学员 Y 的 waitlist.status = converted
```

### Requirement: REQ-002 释放时关注提醒

当 `schedule_slot` 释放名额且该 slot 无任何 waiting 的 waitlist 时，系统 MUST 向关注该 slot 的用户发送「时段可约」提醒。若该用户同时已候补并转正，则 MUST 不再重复发送关注提醒。

#### Scenario: 无候补仅触发关注提醒

```gherkin
Given 教练 A 明日 9:00 时段释放 1 个名额
And   该时段无任何 waitlist 记录
And   用户 Z 关注了该时段
When  系统处理取消事件
Then  不创建 booking
And   向用户 Z 发送「时段可约」提醒
```
