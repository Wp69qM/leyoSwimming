> **OpenSpec Spec | 映射自 `docs/stories/US-007-用户-账号注销/user-story.md` §6**

## Capability

用户账号注销

## ADDED Requirements

### Requirement: REQ-001 注销入口与页面展示

系统 MUST 在「我的」页面提供「注销账号」入口，点击后进入注销确认页。注销确认页 MUST 展示结构化风险提示，内容包括但不限于：注销后账号不可找回、原账号数据与新账号隔离、重新登录视为新用户不绑定原数据、90 天后历史数据匿名化。系统 MUST 以 checklist 形式展示需满足条件：无 active 套餐、无未完成订单、无进行中/待上课预约。条件未满足时，「确认注销」按钮 MUST 置灰不可点击。

#### Scenario: 进入注销确认页，条件全部满足
- **GIVEN** 用户已登录且 `user.status = 0`（正常）
- **AND** 用户无 active 套餐、无未完成订单、无进行中预约
- **WHEN** 用户从「我的」页面点击「注销账号」
- **THEN** 系统进入注销确认页
- **AND** 页面展示结构化风险提示
- **AND** checklist 中「无活跃套餐」「无未完成订单」「无进行中预约」均显示为已通过
- **AND** 「确认注销」按钮可点击

#### Scenario: 进入注销确认页，存在 active 套餐
- **GIVEN** 用户已登录且存在 active 套餐
- **WHEN** 用户进入注销确认页
- **THEN** checklist 中「无活跃套餐」显示为未通过
- **AND** 「确认注销」按钮置灰不可点击
- **AND** 页面提示"您还有未完成的套餐，无法注销"

### Requirement: REQ-002 弹窗二次确认

用户点击「确认注销」后，系统 MUST 弹出二次确认弹窗，提示「注销后账号不可找回，是否确认注销？」。用户点击「确认」后，系统执行注销；用户点击「取消」时，系统关闭弹窗并停留在注销确认页，不执行注销。

#### Scenario: 用户确认弹窗，完成注销
- **GIVEN** 用户已满足所有注销条件
- **WHEN** 用户点击「确认注销」
- **AND** 系统弹出二次确认弹窗，提示「注销后账号不可找回，是否确认注销？」
- **AND** 用户点击「确认」
- **THEN** 系统返回成功
- **AND** `user.status = 1`（软删除）
- **AND** `user.deleted_at` 等于当前时间
- **AND** 系统清除用户所有登录态
- **AND** 页面跳转至登录页

### Requirement: REQ-003 用户取消二次确认弹窗

系统在二次确认弹窗中提供「取消」按钮。用户点击「取消」时，系统 MUST 关闭弹窗并停留在注销确认页，不执行注销。

#### Scenario: 用户取消二次确认
- **GIVEN** 用户已满足所有注销条件
- **WHEN** 用户点击「确认注销」
- **AND** 系统弹出二次确认弹窗
- **AND** 用户点击「取消」
- **THEN** 弹窗关闭
- **AND** 用户停留在注销确认页
- **AND** `user.status` 保持 0（正常）

### Requirement: REQ-004 注销条件不满足时阻止注销

系统在接收注销执行请求时，MUST 再次校验注销条件。存在 active 套餐、未完成订单或进行中预约时，系统 MUST 返回对应错误码且 `user.status` 保持不变。

#### Scenario: 提交注销时新增未完成订单
- **GIVEN** 用户进入注销确认页时条件全部满足
- **AND** 用户在点击弹窗「确认」前新增了待支付订单
- **WHEN** 用户点击弹窗「确认」
- **THEN** 系统返回 `PENDING_ORDER_EXISTS`
- **AND** 前端提示"您有未完成订单，请完成后注销"
- **AND** `user.status` 保持 0（正常）

### Requirement: REQ-005 注销后数据保留与重新注册

账号注销后，系统 MUST 将 `user.status` 置为 1（软删除）并记录 `deleted_at`。历史订单、交易记录 MUST 保留 90 天后匿名化。用户使用原手机号/微信重新登录时，系统 MUST 视为新用户创建新账号，不绑定原账号数据。

#### Scenario: 注销后重新登录
- **GIVEN** 用户已成功注销，`user.status = 1`
- **WHEN** 用户使用原手机号重新登录
- **THEN** 系统按 PRD §5.2.1 第 4 条创建新用户记录
- **AND** 新记录 `user_id` 与原账号不同
- **AND** 新记录 `identity_status = "注册用户"`
- **AND** 新记录不关联原账号的订单、套餐、教练关系等数据

## Delta Header

```yaml
delta:
  change: us-007-user-account-cancel
  capability: user-account-cancel
  type: revise
  rationale: 触发入口改为「我的 → 注销账号」；移除注销协议勾选，改为结构化风险提示 + 条件 checklist；MVP 二次确认简化为弹窗确认/取消，移除短信验证码；前置 US 补充 US-006/US-009；后续依赖补充 US-042；GWT 场景最终为 5 个
  scope: docs/stories/US-007, openspec/changes/us-007-user-account-cancel
```
