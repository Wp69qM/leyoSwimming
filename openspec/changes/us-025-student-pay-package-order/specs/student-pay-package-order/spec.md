# Spec Delta: student-pay-package-order

> 本 spec 为 US-025 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-025-1 创建预支付参数

系统 MUST 提供订单支付接口，允许学员选择微信或支付宝渠道发起支付。系统 MUST 校验订单为待支付状态且未超过 24 小时有效期，并为每笔支付生成唯一幂等键。

#### Scenario: 微信支付预支付成功

```gherkin
Given 用户已登录且存在 order.status = 待支付，amount = 1800 元的订单
And   订单创建时间 < 24h
When  用户选择微信支付并请求预支付参数
Then  系统创建 payment.status = 待支付，channel = 0
And   返回 prepay_params 包含 timeStamp、nonceStr、package、signType、paySign
And   HTTP 状态码 = 200
```

#### Scenario: 支付宝支付预支付成功

```gherkin
Given 用户已登录且存在 order.status = 待支付，amount = 2400 元的订单
And   订单创建时间 < 24h
When  用户选择支付宝支付并请求预支付参数
Then  系统创建 payment.status = 待支付，channel = 1
And   返回 prepay_params 包含 orderStr
And   HTTP 状态码 = 200
```

#### Scenario: 订单已超时

```gherkin
Given 待支付订单已创建 24 小时 01 分
When  用户请求该订单的预支付参数
Then  系统返回 HTTP 400，错误码 ORDER_EXPIRED
And   order.status = 已取消
```

---

### Requirement: REQ-025-2 处理支付回调

系统 MUST 正确处理微信/支付宝异步支付回调，幂等更新订单与课时包状态。系统 MUST 验证回调签名与金额一致性。

#### Scenario: 微信支付回调成功

```gherkin
Given 存在待支付订单 order.amount = 1800 元
And   已生成对应微信支付流水
When  微信发送支付成功回调，金额为 1800 元
Then  系统验证签名通过
And   order.status = 已支付，paid_at 非空
And   package.status = active，available = 10
And   返回 { code: "SUCCESS" }
```

#### Scenario: 重复支付回调

```gherkin
Given 订单已支付成功且 package.status = active
When  微信再次发送同一 channel_trade_no 的支付成功回调
Then  系统返回 HTTP 200
And   不重复创建 package
And   order.status 仍 = 已支付
```
