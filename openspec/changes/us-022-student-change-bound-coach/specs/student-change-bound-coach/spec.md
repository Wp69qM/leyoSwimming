# Spec Delta: student-change-bound-coach

> 本 spec 为 US-022 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 学员更换绑定教练

系统 MUST 允许学员提交更换绑定教练请求。系统 MUST 校验新教练 status=1、与当前教练不同、用户持有 active 套餐且无 reserved 预约。校验通过后 MUST 将旧套餐按 §6.4 公式 refunded、创建新教练的待支付订单，并将用户身份回退为注册用户。

#### Scenario: 正常更换绑定教练

```gherkin
Given 学员已绑定教练 A 且持有 1 个 active 套餐（10 节，已用 2 节）
And   学员已取消所有预约（reserved=0）
And   教练 B 状态为已通过（status=1）
When  学员选择教练 B 的 8 节标准套餐并提交更换
Then  旧套餐 status = refunded，退款金额 = 实付金额 × (10-2)/10
And   系统创建教练 B 的待支付订单 order.status = 待支付
And   用户身份变为注册用户
And   接口返回 HTTP 200 与退款/新订单信息
And   前端跳转支付页（US-025）
```

#### Scenario: 自定义课时更换教练

```gherkin
Given 学员已绑定教练 A 且持有 active 套餐
And   教练 B 已设置参考单价
When  学员选择教练 B 的自定义 12 课时并提交更换
Then  旧套餐 refunded，新订单金额 = 参考单价 × 12
And   接口返回 HTTP 200
```

#### Scenario: 新教练不可用

```gherkin
Given 学员已绑定教练 A
And   教练 C 状态为待审核（status=0）
When  学员尝试更换为教练 C
Then  系统返回 HTTP 400，错误码 COACH_UNAVAILABLE
And   不修改旧套餐，不创建新订单
```

#### Scenario: 新教练与当前教练相同

```gherkin
Given 学员已绑定教练 A
When  学员选择教练 A 作为新教练
Then  系统返回 HTTP 400，错误码 SAME_COACH
And   不修改旧套餐，不创建新订单
```

#### Scenario: 旧套餐存在未取消预约

```gherkin
Given 学员已绑定教练 A 且旧套餐 reserved=1
When  学员提交更换教练
Then  系统返回 HTTP 400，错误码 PENDING_BOOKINGS
And   文案提示"您有未上课的预约，请先取消后再更换教练"
And   不修改旧套餐，不创建新订单
```
