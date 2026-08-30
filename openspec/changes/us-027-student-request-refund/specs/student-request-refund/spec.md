# Spec Delta: student-request-refund

> 本 spec 为 US-027 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-027-1 学员退款资格检查

系统 MUST 提供 `POST /api/package/refund-check` 接口，仅允许套餐所属学员查询退款资格与可退金额。系统 MUST 校验套餐归属当前用户、套餐状态 ∈ {active, expired} 或（套餐状态 = frozen 且 frozenReason = coach_resigned），且不存在 status=待审批 的 refund_record。可退金额 MUST 按 PRD §6.4.2 公式计算：`paidAmount × (totalHours - consumedCount) / totalHours`，单位为分；当 frozenReason = coach_resigned 时，可退金额 MUST 为 paidAmount 的 100%。系统 MUST 返回计算明细（实付金额、总课时、已消耗课时、计算公式）及退款原因码列表。

#### Scenario: 正常查询可退金额

```gherkin
Given 学员已登录
And   存在 order.status = 已支付，package.status = active
And   package.totalHours = 10，consumedCount = 2，paidAmount = 180000 分
And   无待处理退款申请
When  学员请求 POST /api/package/refund-check
Then  系统返回 eligible = true
And   refundAmount = 144000 分（180000 × (10-2)/10）
And   返回 calculation 含 paidAmount=180000、totalHours=10、consumedCount=2、formula="180000 × (10-2)/10"
And   返回 reasonCodes 含 1=教练原因、2=个人原因、3=平台原因
And   HTTP 状态码 = 200
```

#### Scenario: 套餐已退款拒绝检查

```gherkin
Given 学员已登录
And   存在 order.status = 已退款，package.status = refunded
When  学员请求 POST /api/package/refund-check
Then  系统返回 HTTP 400，错误码 PACKAGE_ALREADY_REFUNDED
And   响应体不含 refundAmount 字段
```

#### Scenario: 存在待处理退款申请拒绝检查

```gherkin
Given 学员已登录
And   存在 refund_record.status = 待审批 的记录
When  学员请求 POST /api/package/refund-check
Then  系统返回 HTTP 400，错误码 REFUND_IN_PROGRESS
And   响应体不含 refundAmount 字段
```

#### Scenario: 教练离职 frozen 套餐查询可退金额

```gherkin
Given 学员已登录
And   存在 order.status = 已支付，package.status = frozen
And   package.frozenReason = coach_resigned
And   package.totalHours = 10，consumedCount = 2，paidAmount = 180000 分
And   无待处理退款申请
When  学员请求 POST /api/package/refund-check
Then  系统返回 eligible = true
And   refundAmount = 180000 分（100% 全额退款）
And   HTTP 状态码 = 200
```

### Requirement: REQ-027-2 学员提交退款申请

系统 MUST 提供 `POST /api/package/refund` 接口接收学员退款申请。系统 MUST 在同一事务内创建 refund_record(status=待审批，关联原购买订单 orderId 与 packageId)、更新原购买订单 order.status → 退款审批中、设置 package.status → frozen（frozenReason='refund_pending'，PRD §3.6 / §5.5.1.2）、释放 package.reservedCount → 0、自动取消已预约课程（booking.status → 已取消，cancelReason=1 学员取消，PRD §6.3.1）、触发 US-024 候补转正。系统 MUST 通过幂等键 `{userId}:{packageId}:refund` 防止并发重复提交。系统 MUST 在提交成功后通知管理员与学员。当 `package.frozenReason = coach_resigned` 时，退款金额 MUST 为 paidAmount 的 100% 全额退款；提交后 package.status MUST 转为 frozen(refund_pending)，并保留 frozenReason 历史值为 coach_resigned 用于金额判定。系统 SHALL 在 package 已退款、已耗尽、已冻结（非教练离职原因）、退款不支持、已过期或存在待处理退款时返回对应错误码并拒绝创建 refund_record。

#### Scenario: 正常提交退款申请

```gherkin
Given 学员已登录
And   存在 order.status = 已支付，package.status = active
And   package.totalHours = 10，consumedCount = 2，paidAmount = 180000 分
And   package.reservedCount = 2（含 2 节已预约未上课程）
And   无待处理退款申请
When  学员提交 reasonType=2、reasonDetail="时间冲突，无法继续学习"
Then  系统创建 refund_record，status = 待审批
And   refundAmount = 144000 分
And   order.status = 退款审批中
And   package.status = frozen（frozenReason='refund_pending'，PRD §5.5.1.2）
And   package.reservedCount = 0（已释放，PRD §6.3.1）
And   已预约课程 booking.status → 已取消（cancelReason=1 学员取消）
And   触发 US-024 候补转正
And   通知管理员与学员
And   HTTP 状态码 = 201
And   响应体返回 refundOrderId 与 status="refund_pending"
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
And   package.frozenReason = admin_suspended
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
And   package.reservedCount 保持 0
```

#### Scenario: 教练离职 frozen 套餐 100% 退款

```gherkin
Given 学员已登录
And   存在 order.status = 已支付，package.status = frozen
And   package.frozenReason = coach_resigned
And   package.totalHours = 10，consumedCount = 2，paidAmount = 180000 分
And   package.reservedCount = 0（教练离职时已释放，PRD §4.2.1）
And   无待处理退款申请
When  学员提交退款申请
Then  系统创建 refund_record，status = 待审批
And   refundAmount = 180000 分（100% 全额退款，PRD §6.4.5）
And   order.status = 退款审批中
And   package.status = frozen（frozenReason='refund_pending'，PRD §5.5.1.2）
And   package.frozenReason 历史值保留为 coach_resigned（用于 100% 退款计算）
And   package.reservedCount 保持 0
And   通知管理员与学员
And   HTTP 状态码 = 201
```

#### Scenario: 已耗尽套餐拒绝退款

```gherkin
Given 学员已登录
And   存在 order.status = 已支付，package.status = exhausted
And   package.totalHours = 10，consumedCount = 10，paidAmount = 180000 分
When  学员请求 POST /api/package/refund-check
Then  系统返回 HTTP 400，错误码 PACKAGE_EXHAUSTED_NOT_REFUNDABLE
And   响应体不含 refundAmount 字段
```
