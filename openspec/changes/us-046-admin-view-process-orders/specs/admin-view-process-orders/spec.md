# Spec Delta: admin-view-process-orders

> 本 spec 为 US-046 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 管理员查看订单列表与详情

系统 MUST 提供管理员订单列表与详情查询接口。系统 MUST 支持按状态、教练、学员、时间范围筛选，并返回订单支付信息、关联套餐及退款记录。

#### Scenario: 管理员查看订单列表与详情

```gherkin
Given 系统中存在 3 笔已支付订单
When  管理员进入订单管理页并选择"已支付"筛选
Then  列表展示 3 笔订单，每行展示订单号、学员、教练、金额、状态
And   管理员点击订单 O-001
Then  详情页展示订单 O-001 的支付流水、套餐信息、退款记录
```

#### Scenario: 查看不存在的订单

```gherkin
Given 系统中不存在 id=99999 的订单
When  管理员查看该订单详情
Then  系统返回 HTTP 404
And   返回错误码 ORDER_NOT_FOUND
```

---

### Requirement: REQ-002 管理员处理退款

系统 MUST 提供管理员批准退款与拒绝退款接口。系统 MUST 校验订单状态必须为"退款审批中"；批准退款后 MUST 更新 order.status 为已退款、package.status 为 refunded，并生成退款记录；拒绝退款时 MUST 要求填写原因。

#### Scenario: 管理员批准退款

```gherkin
Given 存在一笔状态为"退款审批中"的订单 R-001，paid_amount=2000.00
When  管理员批准该退款
Then  系统返回 HTTP 200
And   order.status 更新为"已退款"
And   package.status 更新为"refunded"
And   生成退款记录 refund.amount=2000.00
And   向学员发送退款到账通知
```

#### Scenario: 管理员拒绝退款

```gherkin
Given 存在一笔状态为"退款审批中"的订单 R-002
When  管理员拒绝退款并填写原因"未提供有效凭证"
Then  系统返回 HTTP 200
And   order.status 更新为"退款被拒"
And   package.status 保持"active"
And   学员端显示"退款未通过，原因为：未提供有效凭证"
```

#### Scenario: 对非退款审批中订单执行退款

```gherkin
Given 存在一笔状态为"已取消"的订单 C-001
When  管理员尝试批准该订单退款
Then  系统返回 HTTP 400
And   返回错误码 ORDER_STATUS_INVALID
And   订单状态不变
```

#### Scenario: 退款金额超过已支付金额

```gherkin
Given 存在一笔状态为"退款审批中"的订单 R-003，paid_amount=1000.00
When  管理员提交退款金额 1200.00
Then  系统返回 HTTP 400
And   返回错误码 REFUND_AMOUNT_MISMATCH
And   提示"退款金额不能超过已支付金额"
```

---

### Requirement: REQ-003 管理员订单权限控制

系统 MUST 对订单接口进行权限控制。无 `order:read` / `order:write` 权限的管理员 MUST 无法调用对应接口。

#### Scenario: 无权限管理员访问订单接口

```gherkin
Given 管理员已登录但无"订单管理"权限
When  管理员调用 GET /api/admin/orders
Then  系统返回 HTTP 403
And   返回错误码 FORBIDDEN
```
