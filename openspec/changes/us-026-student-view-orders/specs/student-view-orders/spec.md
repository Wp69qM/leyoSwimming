# Spec Delta: student-view-orders

> 本 spec 为 US-026 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-026-1 订单列表查询

系统 MUST 提供当前登录学员的订单列表查询接口，按创建时间倒序排列，支持分页与状态筛选。系统 MUST 仅返回当前用户的订单。

#### Scenario: 正常查看订单列表

```gherkin
Given 学员已登录且名下有多笔订单
And   订单 A 的 packageMode = "standard"，status = 已支付
And   订单 B 的 packageMode = "experience"，status = 已退款，refundReason = "个人原因"
And   订单 C 的 packageMode = "standard"，status = 待支付，剩余有效期 23 小时 30 分
And   订单 D 的 packageMode = "standard"，status = 退款中
When  学员请求「我的订单」列表
Then  系统返回 HTTP 200
And   items 按 created_at 倒序包含所有订单
And   每笔订单包含 orderId、amount、status
And   每笔订单包含 packageMode 字段，"standard" 映射为"正价"，"experience" 映射为"体验课"
And   已完成订单（已支付/已退款）包含 refundReason（仅已退款）
And   待支付订单包含 remainingSeconds 与 actions ["cancel_pay", "go_pay"]
And   退款中订单包含 actions ["cancel_refund"]
```

#### Scenario: 无订单空状态

```gherkin
Given 学员已登录但名下无任何订单
When  学员请求「我的订单」列表
Then  系统返回 HTTP 200
And   items = []
And   前端展示"暂无订单"
```

#### Scenario: 越权访问他人订单

```gherkin
Given 学员 A 已登录
And   存在学员 B 的订单
When  学员 A 请求学员 B 的订单详情
Then  系统返回 HTTP 403，错误码 ORDER_ACCESS_DENIED
```

---

### Requirement: REQ-026-2 订单详情查询

系统 MUST 提供订单详情查询接口，展示订单金额、套餐信息、教练信息、支付状态、支付时间、退款/驳回原因。系统 MUST 校验订单归属。

#### Scenario: 查看已支付订单详情

```gherkin
Given 学员已登录且存在 order.status = 已支付的订单
And   该订单快照字段：packageName = "蛙泳基础 10 节", packageMode = "standard", coachName = "王教练", teachingType = "1v1", totalHours = 10, durationMinutes = 60, validDays = 90, originalPrice = 2000, paidAmount = 1800, refundEnabled = true, refundRatio = 0.8, refundValidDays = 30
And   对应 packageTemplate 后续已被管理员修改为其他内容
When  学员请求该订单详情
Then  系统返回 HTTP 200
And   详情包含订单号、金额、支付时间、状态标签
And   套餐信息区包含购买时快照：packageName、packageMode（显示"正价"）、coachName、teachingType、totalHours、durationMinutes、validDays、originalPrice、paidAmount、退款规则
And   套餐信息取值不受 packageTemplate 后续变更影响
```

#### Scenario: 查看待支付订单详情

```gherkin
Given 学员已登录且存在 order.status = 待支付的订单
And   订单剩余有效期 = 23 小时 30 分
When  学员请求该订单详情
Then  系统返回 HTTP 200
And   详情包含订单号、金额、状态标签、剩余支付倒计时
And   展示「取消支付」「去支付」操作入口
```

#### Scenario: 查看退款中订单详情

```gherkin
Given 学员已登录且存在 order.status = 退款中的订单
And   该订单关联 package.status = active
When  学员请求该订单详情
Then  系统返回 HTTP 200
And   详情包含订单号、金额、状态标签、关联套餐信息
And   展示「取消退款」操作入口
```

#### Scenario: 查看退款被驳回订单详情

```gherkin
Given 学员已登录且存在 order.status = 退款被拒 的订单
And   管理员驳回原因 = "已超过退款有效期"
When  学员请求该订单详情
Then  系统返回 HTTP 200
And   详情包含订单号、金额、状态标签
And   展示驳回原因"已超过退款有效期"
And   仍展示购买时套餐快照与支付信息
```
