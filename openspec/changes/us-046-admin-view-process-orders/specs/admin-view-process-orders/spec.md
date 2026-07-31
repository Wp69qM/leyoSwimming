# Spec Delta: admin-view-process-orders

> 本 spec 为 US-046 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 管理员查看订单列表

系统 MUST 提供管理员订单列表查询接口。系统 MUST 支持按状态、教练、学员、时间范围筛选，并返回订单支付信息、关联套餐及退款记录。

#### Scenario: 管理员查看订单列表

```gherkin
Given 系统中存在 3 笔已支付订单
When  管理员进入订单管理页并选择"已支付"筛选
Then  列表展示 3 笔订单，每行展示订单号、学员、教练、金额、状态
And   返回 HTTP 200
```

---

### Requirement: REQ-002 管理员查看订单详情

系统 MUST 提供管理员订单详情查询接口。系统 MUST 返回订单支付信息、关联套餐、退款记录及争议标记。

#### Scenario: 管理员查看订单详情

```gherkin
Given 系统中存在订单 O-001，状态为"已支付"
When  管理员点击订单 O-001 查看详情
Then  详情页展示订单 O-001 的支付流水、套餐信息、退款记录
And   返回 HTTP 200
```

#### Scenario: 查看不存在的订单

```gherkin
Given 系统中不存在 id=99999 的订单
When  管理员查看该订单详情
Then  系统返回 HTTP 404
And   返回错误码 ORDER_NOT_FOUND
```

---

### Requirement: REQ-003 管理员处理退款

系统 MUST 提供管理员批准退款与拒绝退款接口。系统 MUST 校验订单状态必须为"退款审批中"；批准退款阶段 1 受理后 MUST 更新 order.status 为"退款处理中"、生成退款记录并通知学员；渠道退款成功回调后 MUST 将 order.status 更新为"已退款"、package.status 更新为 refunded；拒绝退款时 MUST 要求填写原因。

#### Scenario: 管理员批准退款受理成功

```gherkin
Given 存在一笔状态为"退款审批中"的订单 R-001，paid_amount=2000.00
When  管理员批准该退款（阶段 1 受理）
Then  系统返回 HTTP 202 Accepted
And   order.status 更新为"退款处理中"
And   package.status 保持 frozen
And   生成退款记录 refund.amount=2000.00，refund_transaction.status="处理中"
And   向学员发送"退款处理中"受理通知
```

#### Scenario: 渠道退款回调成功更新终态

```gherkin
Given 订单 R-001 已处于"退款处理中"，refund_transaction.status="处理中"
When  渠道退款成功回调（阶段 2 成功）
Then  order.status 更新为"已退款"
And   package.status 更新为"refunded"
And   refund_transaction.status="成功"
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

### Requirement: REQ-004 管理员标记争议退款

系统 MUST 允许管理员将订单标记为「争议退款」。系统 MUST 记录 `order.dispute_flag=true` 与 `dispute_reason`；该操作 MUST 自动生成一条关联该订单的客服工单（`support_ticket`，类型=退款申诉），并通知学员；该操作 MUST 不改变订单当前状态。

#### Scenario: 管理员标记订单为争议退款并生成客服工单

```gherkin
Given 管理员 M 已登录且具有订单管理权限
And   存在一笔状态为"已支付"的订单 D-001，order_id=10001
And   学员 U 为订单 D-001 的购买者
When  管理员 M 将订单 D-001 标记为争议退款并填写原因"学员对扣课时有异议"
Then  系统返回 HTTP 200
And   order.dispute_flag=true
And   order.dispute_reason="学员对扣课时有异议"
And   order.status 保持"已支付"不变
And   support_ticket 表新增 1 条记录，type=3（退款申诉），order_id=10001，status=0（pending）
And   学员 U 收到争议标记通知
```

---

### Requirement: REQ-005 管理员订单权限控制

系统 MUST 对订单接口进行权限控制。无 `order:read` / `order:write` 权限的管理员 MUST 无法调用对应接口。

#### Scenario: 无权限管理员访问订单接口

```gherkin
Given 管理员已登录但无"订单管理"权限
When  管理员调用 GET /api/admin/v1/orders
Then  系统返回 HTTP 403
And   返回错误码 FORBIDDEN
```
