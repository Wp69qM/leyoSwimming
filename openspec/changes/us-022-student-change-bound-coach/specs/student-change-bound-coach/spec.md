# Spec Delta: student-change-bound-coach

> 本 spec 为 US-022 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 学员更换绑定教练

系统 MUST 允许学员分两步完成更换绑定教练。系统 MUST 提供第一步接口 `/api/users/me/coach/unsubscribe`：校验用户持有 active 套餐且无 reserved 预约，将旧套餐标记为 frozen（frozen_reason=refund_pending）并记录 original_status，创建 refund_record（status=pending_change，记录退款金额与 original_package_status），释放 reserved 课时，并将用户身份回退为注册用户。系统 MUST 提供第二步接口 `/api/users/me/coach/purchase`：校验新教练 status=1、与当前教练不同、用户同意协议、第二步在 24 小时有效期内、存在 status=pending_change 的换教练退款记录；校验通过后将旧套餐 status 更新为 refunded、refund_record.status 更新为 approved，并创建新教练的待支付订单。若第二步距第一步超过 24 小时未发起，系统 MUST 自动回滚第一步：旧套餐恢复为 original_status，取消/删除 pending_change 的 refund_record，身份按实际套餐状态重算，并返回错误码 COACH_CHANGE_EXPIRED。前端 MUST 强引导提示用户 24 小时有效期。

#### Scenario: 正常更换绑定教练

```gherkin
Given 学员已绑定教练 A 且持有 1 个 active 套餐（10 节，已用 2 节）
And   学员已取消所有预约（reserved=0）
And   教练 B 状态为已通过（status=1）
When  学员提交第一步 /api/users/me/coach/unsubscribe
Then  旧套餐 status = frozen，frozen_reason = refund_pending，reserved_count = 0，记录原始状态 active
And   创建 refund_record，status = pending_change，refund_amount = 实付金额 × (10-2)/10，original_package_status = active
And   用户身份变为注册用户
And   接口返回 HTTP 200 与 24 小时有效期
When  学员在 24 小时内提交第二步 /api/users/me/coach/purchase，选择教练 B 的 8 节标准套餐
Then  旧套餐 status = refunded，refund_record.status = approved
And   系统创建教练 B 的待支付订单 order.status = 待支付
And   接口返回 HTTP 201 与新订单信息
And   前端跳转支付页（US-025）
```

#### Scenario: 自定义课时更换教练

```gherkin
Given 学员已绑定教练 A 且持有 active 套餐
And   教练 B 已设置参考单价
When  学员提交第一步 /api/users/me/coach/unsubscribe
Then  旧套餐 status = frozen，frozen_reason = refund_pending
And   创建 refund_record，status = pending_change
When  学员在 24 小时内提交第二步 /api/users/me/coach/purchase，选择教练 B 的自定义 12 课时
Then  旧套餐 status = refunded，refund_record.status = approved
And   新订单金额 = 参考单价 × 12，order.status = 待支付
And   接口返回 HTTP 201
```

#### Scenario: 新教练不可用

```gherkin
Given 学员已绑定教练 A
And   教练 C 状态为待审核（status=0）
When  学员尝试更换为教练 C
Then  系统返回 HTTP 400，错误码 COACH_UNAVAILABLE
And   不修改旧套餐，不创建新订单
```

#### Scenario: 新教练与当前教练相同

```gherkin
Given 学员已绑定教练 A
When  学员选择教练 A 作为新教练
Then  系统返回 HTTP 400，错误码 SAME_COACH
And   不修改旧套餐，不创建新订单
```

#### Scenario: 旧套餐存在未取消预约

```gherkin
Given 学员已绑定教练 A 且旧套餐 reserved=1
When  学员提交更换教练
Then  系统返回 HTTP 400，错误码 PENDING_BOOKINGS
And   文案提示"您有未上课的预约，请先取消后再更换教练"
And   不修改旧套餐，不创建新订单
```

#### Scenario: 两步操作超过 24 小时未发起第二步

```gherkin
Given 学员已完成第一步冻结（旧套餐 status=frozen，frozen_reason=refund_pending，原始状态 active）
And   存在 refund_record.status = pending_change
And   距第一步完成已超过 24 小时
When  学员尝试进入第二步购买新教练套餐
Then  系统自动回滚第一步：旧套餐恢复 original_status（active），refund_record 被取消/删除
And   用户身份恢复学员
And   系统返回 HTTP 400，错误码 COACH_CHANGE_EXPIRED
And   文案提示"更换流程已超时，请重新发起退订"
And   不创建新教练订单
```
