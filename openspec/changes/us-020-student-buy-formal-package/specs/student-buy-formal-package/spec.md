# Spec Delta: student-buy-formal-package

> 本 spec 为 US-020 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 正价套餐订单创建

系统 MUST 允许已登录用户提交正价套餐订单。系统 MUST 校验用户已同意最新版协议、教练状态为已通过、用户未持有其他教练的 active 套餐；如用户为未成年人，MUST 校验已填写监护人手机号并通过短信验证码校验。校验通过后 MUST 创建 status=待支付的订单、记录协议签署，并在用户为未成年人时向其监护人手机号发送订单创建短信通知。

#### Scenario: 正常提交标准套餐

```gherkin
Given 注册用户已登录且同意最新版协议
And   教练 A 状态为已通过（status=1）
And   用户名下无其他教练的 active 套餐
And   系统已配置 10 节标准套餐
When  用户选择教练 A 的 10 节标准套餐并提交订单
Then  系统创建 order.status = 待支付，course_type = 1
And   系统记录 agreement_sign 版本号与签署时间
And   接口返回 HTTP 201 与订单 ID
And   前端跳转支付页（US-025）
```

#### Scenario: 正常提交自定义课时

```gherkin
Given 注册用户已登录且同意最新版协议
And   教练 A 已设置参考单价
And   用户选择自定义 12 课时
When  用户提交自定义课时订单
Then  系统按参考单价计算订单金额
And   order.status = 待支付，course_type = 1
And   接口返回 HTTP 201 与订单 ID
```

#### Scenario: 未同意协议

```gherkin
Given 注册用户已登录
And   用户未勾选《用户须知》
When  用户提交正价套餐订单
Then  系统返回 HTTP 400，错误码 AGREEMENT_REQUIRED
And   不创建订单
```

#### Scenario: 已持有其他教练 active 套餐

```gherkin
Given 用户已持有教练 B 的 active 套餐
When  用户尝试购买教练 A 的正价套餐
Then  系统返回 HTTP 400，错误码 COACH_CONFLICT
And   文案提示"您已持有其他教练的有效套餐，需先更换教练"
And   不创建订单
```

#### Scenario: 未成年人未校验监护人手机号

```gherkin
Given 注册用户已登录且同意最新版协议
And   用户年龄为 16 岁
And   教练 A 状态为已通过（status=1）
And   用户名下无其他教练的 active 套餐
When  用户提交正价套餐订单但未填写监护人手机号
Then  系统返回 HTTP 400，错误码 GUARDIAN_PHONE_REQUIRED
And   不创建订单
```

#### Scenario: 教练状态不可用

```gherkin
Given 教练 C 状态为待审核（status=0）
When  用户购买教练 C 的正价套餐
Then  系统返回 HTTP 400，错误码 COACH_UNAVAILABLE
And   不创建订单
```
