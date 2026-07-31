# Spec Delta: user-buy-trial-package

> 本 spec 为 US-017 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 游客购买体验课套餐

系统 MUST 允许已登录用户购买体验课套餐。系统 MUST 校验用户已阅读并同意最新版《用户须知》、用户名下无 active/exhausted 体验套餐且教练状态为已通过；支付成功后 MUST 创建 active 体验套餐、记录协议签署并将用户身份升级为学员。

#### Scenario: 正常购买体验课

```gherkin
Given 游客已登录且名下无体验套餐
And   用户已阅读并同意最新版《用户须知》
And   教练 A 状态为已通过（status=1）
When  游客购买教练 A 的体验课并完成支付
Then  订单状态 = 已支付
And   package.status = active，package_type = 0，available = 1，total_hours = 1
And   系统记录 agreement_sign 版本号与签署时间
And   用户身份变为学员
And   接口返回 HTTP 200
```

#### Scenario: 游客未勾选《用户须知》

```gherkin
Given 游客已登录且名下无体验套餐
And   教练 A 状态为已通过（status=1）
And   用户未勾选《用户须知》
When  游客提交体验课订单
Then  系统返回 HTTP 400，错误码 AGREEMENT_REQUIRED
And   不创建订单
```

#### Scenario: 游客未登录触发登录

```gherkin
Given 游客未登录
When  游客点击"购买体验课"
Then  系统引导游客先完成微信授权登录
And   登录后回到购买确认页
```

#### Scenario: 已有体验套餐再次购买

```gherkin
Given 用户名下已有 status=active 的体验套餐
When  用户再次购买体验课
Then  系统返回 HTTP 400，错误码 TRIAL_PACKAGE_EXISTS
And   不创建新订单和套餐
```

#### Scenario: 教练不可约

```gherkin
Given 教练 B 状态为待审核（status=0）
When  游客购买教练 B 的体验课
Then  系统返回 HTTP 400，错误码 COACH_UNAVAILABLE
And   不创建订单
```

#### Scenario: 支付超时取消

```gherkin
Given 游客已提交体验课订单但未支付
And   订单创建时间为 24 小时前
When  系统定时任务扫描超时订单
Then  订单状态变为已取消
And   对应 package 回滚
And   用户身份回退（如无其他 active 套餐）
```
