# Spec Delta: student-request-refund

> 本 spec 为 US-027 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-027-1 学员退款资格检查

系统 MUST 提供 `GET /api/orders/{order_id}/refund/check` 接口，仅允许订单所属学员查询退款资格与可退金额。系统 MUST 校验订单状态为已支付、套餐状态 ∈ {active, exhausted, expired} 或（套餐状态 = frozen 且 frozen_reason = coach_resigned），且不存在 status=待审批 的 refund_record。可退金额 MUST 按 PRD §6.4.2 公式计算：`paid_amount × (total_hours - consumed_count) / total_hours`，单位为分；当 frozen_reason = coach_resigned 时，可退金额 MUST 为 paid_amount 的 100%。系统 MUST 返回计算明细（实付金额、总课时、已消耗课时、计算公式）及退款原因码列表。

#### Scenario: 正常查询可退金额

```gherkin
Given 学员已登录
And   存在 order.status = 已支付，package.status = active
And   package.total_hours = 10，consumed_count = 2，paid_amount = 180000 分
And   无待处理退款申请
When  学员请求 GET /api/orders/{order_id}/refund/check
Then  系统返回 eligible = true
And   refund_amount = 144000 分（180000 × (10-2)/10）
And   返回 calculation 含 paid_amount=180000、total_hours=10、consumed_count=2、formula="180000 × (10-2)/10"
And   返回 reason_codes 含 1=教练原因、2=个人原因、3=平台原因
And   HTTP 状态码 = 200
```

#### Scenario: 套餐已退款拒绝检查

```gherkin
Given 学员已登录
And   存在 order.status = 已退款，package.status = refunded
When  学员请求 GET /api/orders/{order_id}/refund/check
Then  系统返回 HTTP 400，错误码 PACKAGE_ALREADY_REFUNDED
And   响应体不含 refund_amount 字段
```

#### Scenario: 存在待处理退款申请拒绝检查

```gherkin
Given 学员已登录
And   存在 refund_record.status = 待审批 的记录
When  学员请求 GET /api/orders/{order_id}/refund/check
Then  系统返回 HTTP 400，错误码 REFUND_IN_PROGRESS
And   响应体不含 refund_amount 字段
```

#### Scenario: 教练离职 frozen 套餐查询可退金额

```gherkin
Given 学员已登录
And   存在 order.status = 已支付，package.status = frozen
And   package.frozen_reason = coach_resigned
And   package.total_hours = 10，consumed_count = 2，paid_amount = 180000 分
And   无待处理退款申请
When  学员请求 GET /api/orders/{order_id}/refund/check
Then  系统返回 eligible = true
And   refund_amount = 180000 分（100% 全额退款）
And   HTTP 状态码 = 200
```

### Requirement: REQ-027-2 学员提交退款申请

系统 MUST 提供 `POST /api/orders/{order_id}/refund` 接口接收学员退款申请。系统 MUST 在同一事务内创建 refund_record(status=待审批)、更新 order.status → 退款审批中、设置 package.status → frozen（frozen_reason='refund_pending'，PRD §3.6 / §5.5.1.2）、释放 package.reserved_count → 0、自动取消已预约课程（booking.status → 已取消，cancel_reason=1 学员取消，PRD §6.3.1）、触发 US-024 候补转正。系统 MUST 通过幂等键 `{user_id}:{order_id}:refund` 防止并发重复提交。系统 MUST 在提交成功后通知管理员与学员。当 `package.frozen_reason = coach_resigned` 时，退款金额 MUST 为 paid_amount 的 100% 全额退款；提交后 package.status MUST 转为 frozen(refund_pending)，并保留 frozen_reason 历史值为 coach_resigned 用于金额判定。系统 SHALL 在 package 已退款、已冻结（非教练离职原因）、订单未支付或存在待处理退款时返回对应错误码并拒绝创建 refund_record。

#### Scenario: 正常提交退款申请

```gherkin
Given 学员已登录
And   存在 order.status = 已支付，package.status = active
And   package.total_hours = 10，consumed_count = 2，paid_amount = 180000 分
And   package.reserved_count = 2（含 2 节已预约未上课程）
And   无待处理退款申请
When  学员提交 reason_type=2、reason_detail="时间冲突，无法继续学习"
Then  系统创建 refund_record，status = 待审批
And   refund_amount = 144000 分
And   order.status = 退款审批中
And   package.status = frozen（frozen_reason='refund_pending'，PRD §5.5.1.2）
And   package.reserved_count = 0（已释放，PRD §6.3.1）
And   已预约课程 booking.status → 已取消（cancel_reason=1 学员取消）
And   触发 US-024 候补转正
And   通知管理员与学员
And   HTTP 状态码 = 201
And   响应体返回 refund_id 与 status="待审批"
```

#### Scenario: 套餐已退款拒绝提交

```gherkin
Given 学员已登录
And   存在 order.status = 已退款，package.status = refunded
When  学员提交退款申请
Then  系统返回 HTTP 400，错误码 PACKAGE_ALREADY_REFUNDED
And   不创建 refund_record
And   order.status 保持已退款
And   package.status 保持 refunded
```

#### Scenario: 非教练离职 frozen 套餐拒绝提交

```gherkin
Given 学员已登录
And   存在 order.status = 已支付，package.status = frozen
And   package.frozen_reason = admin_suspended
And   无待处理退款申请
When  学员提交退款申请
Then  系统返回 HTTP 400，错误码 PACKAGE_FROZEN
And   不创建 refund_record
And   package.status 保持 frozen
```

#### Scenario: 并发重复提交退款

```gherkin
Given 学员已登录
And   存在 refund_record.status = 待审批 的记录
When  学员再次提交退款申请
Then  系统返回 HTTP 400，错误码 REFUND_IN_PROGRESS
And   不创建新的 refund_record
And   order.status 保持退款审批中
And   package.status 保持 frozen（refund_pending）
And   package.reserved_count 保持 0
```

#### Scenario: 教练离职 frozen 套餐 100% 退款

```gherkin
Given 学员已登录
And   存在 order.status = 已支付，package.status = frozen
And   package.frozen_reason = coach_resigned
And   package.total_hours = 10，consumed_count = 2，paid_amount = 180000 分
And   package.reserved_count = 0（教练离职时已释放，PRD §4.2.1）
And   无待处理退款申请
When  学员提交退款申请
Then  系统创建 refund_record，status = 待审批
And   refund_amount = 180000 分（100% 全额退款，PRD §6.4.5）
And   order.status = 退款审批中
And   package.status = frozen（frozen_reason='refund_pending'，PRD §5.5.1.2）
And   package.frozen_reason 历史值保留为 coach_resigned（用于 100% 退款计算）
And   package.reserved_count 保持 0
And   通知管理员与学员
And   HTTP 状态码 = 201
```

#### Scenario: 体验套餐退款金额为 0

```gherkin
Given 学员已登录
And   存在 order.status = 已支付，package.status = exhausted
And   package.total_hours = 1，consumed_count = 1，paid_amount = 9900 分
When  学员请求 GET /api/orders/{order_id}/refund/check
Then  系统返回 eligible = true
And   refund_amount = 0 分（9900 × (1-1)/1）
And   响应体含提示标识表明本套餐已用完无可退金额
And   HTTP 状态码 = 200
```
