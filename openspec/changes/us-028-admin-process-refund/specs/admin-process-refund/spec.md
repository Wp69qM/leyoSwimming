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

系统 MUST 允许管理员批准退款申请，可退金额仅作为参考展示，管理员可基于业务场景调整实际退款金额（需 ≥ 0），调用原支付渠道退款接口，并更新订单与课时包状态。系统 MUST 对同一退款申请的重复批准进行幂等处理。

#### Scenario: 批准标准退款

```gherkin
Given 管理员已登录
And   存在 refund.status = 待审批，amount = 1440 分的退款申请
And   对应 order.status = 退款审批中，package.status = frozen（refund_pending），package.booking_frozen = true
And   原支付渠道为微信支付
When  管理员批准该退款并将金额调整为 1500 分（大于可退金额）
Then  系统记录 audit_log 含调整原因
And   refund_record.amount 与退款订单 paid_amount 更新为 1500 分
And   系统创建 refund_transaction.status = 处理中
And   order.status = 退款处理中
And   package.status = frozen（refund_pending，保持）
And   package.booking_frozen = true
When  渠道异步回调成功
Then  order.status = 已退款
And   package.status = refunded
And   refund_transaction.status = 成功
And   HTTP 状态码 = 200
```

#### Scenario: 重复批准退款

```gherkin
Given 订单已退款成功，order.status = 已退款，package.status = refunded
When  管理员再次批准同一退款
Then  系统返回 HTTP 200
And   不重复创建 refund_transaction
And   order.status 仍为已退款
And   package.status 仍为 refunded
```

#### Scenario: 渠道退款失败回滚

```gherkin
Given 管理员已登录
And   存在 refund.status = 待审批，amount = 1440 分的退款申请
And   对应 order.status = 退款审批中，package.status = frozen（refund_pending），package.booking_frozen = true
And   微信退款接口返回失败
When  管理员批准该退款
Then  系统创建 refund_transaction.status = 失败
And   order.status = 退款审批中
And   package.status = frozen（refund_pending，保持）
And   package.booking_frozen = true
And   refund 进入重试队列
And   HTTP 状态码 = 200
```

### Requirement: REQ-028-3 管理员驳回退款

系统 MUST 允许管理员驳回退款申请。驳回后订单状态 MUST 更新为 退款被拒（7），package.status MUST 由 frozen（refund_pending）转回 active（解冻，frozen_reason 清空），package.booking_frozen MUST 置为 false，系统 MUST 通知学员并记录驳回原因。只有用户主动撤销退款申请时，订单状态才回到已支付。

#### Scenario: 驳回退款

```gherkin
Given 管理员已登录
And   存在 refund.status = 待审批
And   order.status = 退款审批中
And   package.status = frozen（refund_pending）
When  管理员驳回该退款并填写原因"资料不足"
Then  order.status = 退款被拒（7）
And   package.status = active（解冻，frozen_reason 清空）
And   package.booking_frozen = false
And   refund.status = 管理员驳回
And   学员收到驳回通知
And   HTTP 状态码 = 200
```
