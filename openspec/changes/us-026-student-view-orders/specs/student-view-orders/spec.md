# Spec Delta: student-view-orders

> 本 spec 为 US-026 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-026-1 订单列表查询

系统 MUST 提供当前登录学员的订单列表查询接口，按创建时间倒序排列，支持分页与状态筛选。系统 MUST 仅返回当前用户的订单。

#### Scenario: 正常查看订单列表

```gherkin
Given 学员已登录且名下有 3 笔订单
When  学员请求「我的订单」列表
Then  系统返回 HTTP 200
And   items 按 created_at 倒序包含 3 笔订单
And   每笔订单包含 order_id、amount、status
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

系统 MUST 提供订单详情查询接口，展示订单金额、套餐信息、教练信息、支付状态、支付时间与退款入口。系统 MUST 校验订单归属。

#### Scenario: 查看已支付订单详情

```gherkin
Given 学员已登录且存在 order.status = 已支付的订单
When  学员请求该订单详情
Then  系统返回 HTTP 200
And   详情包含订单号、套餐名称、教练姓名、金额、支付时间
And   展示「申请退款」入口
```
