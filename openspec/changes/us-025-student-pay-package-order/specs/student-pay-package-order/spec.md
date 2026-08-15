# Spec Delta: student-pay-package-order

> 本 spec 为 US-025 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-025-1 创建 Mock 支付

系统 MUST 提供订单 Mock 支付接口，允许学员选择微信或支付宝渠道发起支付。系统 MUST 校验订单为待支付状态且未超过 24 小时有效期，并为每笔支付生成唯一幂等键。

#### Scenario: 微信支付 Mock 成功

```gherkin
Given 用户已登录且存在 order.status = 待支付，amount = 1800 元的订单
And   订单创建时间 < 24h
When  用户选择微信支付并请求 Mock 支付
Then  系统创建 payment.status = 待支付，channel = 0
And   调用 MockPaymentProvider 生成 channelTradeNo = "MOCK-WX-..."
And   返回 { paymentId, channelTradeNo, status: "success" }
And   HTTP 状态码 = 200
```

#### Scenario: 支付宝支付 Mock 成功

```gherkin
Given 用户已登录且存在 order.status = 待支付，amount = 2400 元的订单
And   订单创建时间 < 24h
When  用户选择支付宝支付并请求 Mock 支付
Then  系统创建 payment.status = 待支付，channel = 1
And   调用 MockPaymentProvider 生成 channelTradeNo = "MOCK-ALI-..."
And   返回 { paymentId, channelTradeNo, status: "success" }
And   HTTP 状态码 = 200
```

#### Scenario: 订单已超时

```gherkin
Given 待支付订单已创建 24 小时 01 分
When  用户请求该订单的 Mock 支付
Then  系统返回 HTTP 400，错误码 ORDER_EXPIRED
And   order.status = 已取消
```

---

### Requirement: REQ-025-2 处理 Mock 支付回调

系统 MUST 正确处理 Mock 渠道异步支付回调，幂等更新订单与课时包状态。系统 MUST 验证回调金额一致性。系统 MUST 在处理订单超时取消（库存释放）与候补转正（US-023）时竞争同一分布式锁 `inventory:{coachId}`，防止并发导致库存重复分配或超卖。

#### Scenario: Mock 微信支付回调成功

```gherkin
Given 存在待支付订单 order.amount = 1800 元
And   订单中已快照模板字段：packageName = "蛙泳基础 10 节", packageMode = "standard", coachId = "C001", coachName = "王教练", teachingType = "1v1", strokeIds = ["breaststroke"], totalHours = 10, durationMinutes = 60, validDays = 90, originalPrice = 2000, paidAmount = 1800, refundEnabled = true, refundRatio = 0.8, refundValidDays = 30
And   已生成对应 Mock 微信支付流水
When  Mock 渠道发送支付成功回调，金额为 1800 元
Then  系统校验 payment 存在且金额一致
And   order.status = 已支付，paidAt 非空
And   package.status = active，available = 10
And   package 字段与订单快照一致：packageMode = "standard", coachId = "C001", teachingType = "1v1", totalHours = 10, durationMinutes = 60, validDays = 90, paidAmount = 1800
And   返回 { code: "SUCCESS" }
```

#### Scenario: 重复支付回调

```gherkin
Given 订单已支付成功且 package.status = active
And   package 字段与订单快照一致
When  Mock 渠道再次发送同一 channelTradeNo 的支付成功回调
Then  系统返回 HTTP 200
And   不重复创建 package
And   order.status 仍 = 已支付
And   package 字段仍与订单快照一致
```

#### Scenario: 支付期间模板被修改/下架

```gherkin
Given 存在待支付订单 order.amount = 1800 元
And   订单中已快照模板字段：packageMode = "standard", totalHours = 10, paidAmount = 1800
And   用户下单后、支付回调前，对应 packageTemplate 被管理员修改（如下架、totalHours 改为 12）
When  Mock 渠道发送支付成功回调，金额为 1800 元
Then  系统使用订单快照字段创建 package
And   package.status = active
And   package.packageMode = "standard"
And   package.totalHours = 10
And   package.paidAmount = 1800
And   package 字段不受 packageTemplate 后续变更影响
```

#### Scenario: 支付超时取消与候补转正并发竞争

```gherkin
Given 订单 O1 已创建 24 小时 01 分，status = 待支付，coachId = A
And   教练 A 存在候补学员 W1 等待转正
When  订单超时取消任务释放 O1 库存的同时，W1 触发候补转正
Then  两个操作竞争同一分布式锁 `inventory:A`
And   仅有一个操作成功修改库存
And   另一个操作基于最新库存重试或等待
And   教练 A 的总可售名额不出现超卖或负库存
```
