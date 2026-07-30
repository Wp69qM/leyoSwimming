# Spec Delta: admin-process-refund

> 本 spec 为 US-028 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-028-1 管理员查看退款申请列表与详情

系统 MUST 提供管理端退款申请列表与详情接口，仅允许具有退款管理权限的管理员访问。列表 MUST 支持按状态筛选与分页，详情 MUST 展示订单、套餐消耗、支付渠道及可退金额计算过程。

#### Scenario: 正常查看退款列表

```gherkin
Given 管理员已登录
And   存在 2 条待审批退款申请
When  管理员请求退款列表
Then  系统返回 2 条记录
And   每条记录包含 refund_id、order_id、amount、status
And   HTTP 状态码 = 200
```

#### Scenario: 非管理员访问退款列表

```gherkin
Given 普通学员已登录
When  学员请求管理端退款列表
Then  系统返回 HTTP 403，错误码 FORBIDDEN
```

### Requirement: REQ-028-2 管理员批准退款并原路退回

系统 MUST 允许管理员批准退款申请，校验退款金额不超过可退金额，调用原支付渠道退款接口，并更新订单与课时包状态。系统 MUST 对同一退款申请的重复批准进行幂等处理。

#### Scenario: 批准标准退款

```gherkin
Given 管理员已登录
And   存在 refund.status = 待审批，amount = 1440 分的退款申请
And   对应 order.status = 退款审批中
And   原支付渠道为微信支付
When  管理员批准该退款
Then  系统创建 refund_transaction.status = 处理中
And   order.status = 已退款
And   package.status = refunded
And   HTTP 状态码 = 200
```

#### Scenario: 重复批准退款

```gherkin
Given 订单已退款成功，order.status = 已退款
When  管理员再次批准同一退款
Then  系统返回 HTTP 200
And   不重复创建 refund_transaction
And   order.status 仍为已退款
```

### Requirement: REQ-028-3 管理员驳回退款

系统 MUST 允许管理员驳回退款申请。驳回后订单状态 MUST 恢复为已支付，套餐状态 MUST 恢复为 active，系统 MUST 通知学员并记录驳回原因。

#### Scenario: 驳回退款

```gherkin
Given 管理员已登录
And   存在 refund.status = 待审批
And   order.status = 退款审批中
When  管理员驳回该退款并填写原因"资料不足"
Then  order.status = 已支付
And   package.status = active
And   refund.status = 管理员驳回
And   学员收到驳回通知
And   HTTP 状态码 = 200
```
