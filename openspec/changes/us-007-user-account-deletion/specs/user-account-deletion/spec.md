# Spec Delta: user-account-deletion

> 本 spec 为 US-007 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 账号注销

系统 MUST 提供 `POST /api/user/account/cancel` 接口，供已登录用户提交账号注销申请。系统 MUST 在注销前校验用户无 active 状态的套餐、无未完成或退款中订单、无进行中或待上课预约。系统 MUST 在注销成功后将 `user.status` 更新为 `1`（软删除）、设置 `deleted_at` 与 `anonymous_after`（当前时间 + 90 天）、清除所有登录态、记录 `audit_log`。系统 MUST 对重复提交做幂等处理，返回已注销结果。

#### Scenario: 正常注销账号

```gherkin
Given 用户已登录且无 active 套餐、无未完成订单、无进行中预约
When  用户在二次确认弹窗中点击「确认注销」
Then  user.status = 1（软删除）
And   user.deleted_at 为当前时间
And   订单/套餐历史保留 90 天后匿名化（PRD §3.7、附录 D45）
And   登录态被清除
And   页面跳转至登录页
And   audit_log 新增一条注销记录
```

#### Scenario: 存在 active 套餐时注销

```gherkin
Given 用户已登录且存在 active 套餐
When  用户尝试注销账号
Then  返回 HTTP 400 + 错误码 ACTIVE_PACKAGE_EXISTS
And   前端提示"您还有未完成的套餐，无法注销"
And   user.status 保持 0（正常）
```

#### Scenario: 存在未完成订单时注销

```gherkin
Given 用户已登录且存在待支付订单
When  用户尝试注销账号
Then  返回 HTTP 400 + 错误码 PENDING_ORDER_EXISTS
And   前端提示"您有未完成订单，请完成后注销"
And   user.status 保持 0（正常）
```

#### Scenario: 存在进行中预约时注销

```gherkin
Given 用户已登录且存在待上课的课程预约
When  用户尝试注销账号
Then  返回 HTTP 400 + 错误码 ONGOING_BOOKING_EXISTS
And   前端提示"您有未完成的课程预约，请完成后注销"
And   user.status 保持 0（正常）
```

#### Scenario: 重复提交注销

```gherkin
Given 用户已成功注销账号
When  用户再次提交注销请求
Then  系统返回已注销结果
And   不重复更新 user.status
And   不重复清除登录态
```

### Requirement: REQ-002 注销条件查询

系统 MUST 提供 `POST /api/user/account/cancel-check` 接口，供已登录用户查询当前是否满足注销条件。系统 MUST 返回 active 套餐数量、未完成订单数量、进行中预约数量、是否可注销等字段。

#### Scenario: 查询满足注销条件

```gherkin
Given 用户已登录且无 active 套餐、无未完成订单、无进行中预约
When  用户调用 POST /api/user/account/cancel-check
Then  返回 HTTP 200
And   响应体 data.can_cancel = true
And   data.active_package_count = 0
And   data.pending_order_count = 0
And   data.ongoing_booking_count = 0
```

#### Scenario: 查询不满足注销条件

```gherkin
Given 用户已登录且存在 1 个 active 套餐、2 个未完成订单和 1 个进行中预约
When  用户调用 POST /api/user/account/cancel-check
Then  返回 HTTP 200
And   响应体 data.can_cancel = false
And   data.active_package_count = 1
And   data.pending_order_count = 2
And   data.ongoing_booking_count = 1
```
