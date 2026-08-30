# Spec Delta: student-view-my-packages

> 本 spec 为 US-021 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 我的套餐列表查询

系统 MUST 提供登录接口返回当前用户名下所有 package，按状态分组，并对 active 套餐汇总课时。frozen 套餐 MUST 展示冻结原因文案。满足退款条件时（package.status ∈ {active, expired} 或 frozen 且 frozen_reason='coach_resigned'，且 refund_enabled=true，未超过 refund_valid_days），套餐详情页 MUST 展示「申请退款」入口，点击后跳转 US-027；exhausted 套餐 MUST 不展示退款入口。

#### Scenario: 正常查看我的套餐

```gherkin
Given 用户已登录且名下有 2 个 active 套餐
And   套餐 A：10 节，已用 3 节，剩余 7 节
And   套餐 B：6 节，已用 0 节，剩余 6 节
When  用户从底部导航「我的」进入个人中心页，点击「我的套餐」入口
Then  进入「我的套餐」页面
And   页面展示 2 个 active 套餐卡片
And   顶部汇总：总课时 16，已用 3，剩余 13
And   每个卡片展示教练姓名、套餐模式标签（正价/体验课）、教学类型、每节课时长、有效期、购买时价格、剩余课时、状态标签
And   接口返回 HTTP 200
```

#### Scenario: 无套餐空状态

```gherkin
Given 用户已登录且名下无任何 package
When  用户从底部导航「我的」进入个人中心页，点击「我的套餐」入口
Then  进入「我的套餐」页面
And   页面展示空状态插画
And   文案为"您还没有套餐，去选购吧"
And   接口返回 HTTP 200 + items = []
```

#### Scenario: 存在教练离职冻结套餐

```gherkin
Given 用户已登录且名下有 1 个 active 套餐和 1 个 frozen 套餐（frozen_reason='coach_resigned'）
When  用户从底部导航「我的」进入个人中心页，点击「我的套餐」入口
Then  进入「我的套餐」页面
And   active 套餐正常展示
And   frozen 套餐显示"教练已离职，请更换教练或申请退款"
And   frozen 套餐卡片展示「申请退款」入口
And   接口返回 HTTP 200
```

#### Scenario: active 套餐详情页展示「申请退款」入口

```gherkin
Given 用户已登录且名下有 1 个 active 套餐
And   package 快照 total_hours = 10，consumed_count = 2，paid_amount = 1800 元，refund_enabled = true，refund_ratio = 1.0，未超过 refund_valid_days
When  用户进入「我的套餐」页面
And   用户点击 active 套餐卡片进入套餐详情页
Then  套餐详情页展示完整快照字段
And   套餐详情页展示「申请退款」按钮
When  用户点击「申请退款」
Then  跳转 US-027 退款申请页
And   US-027 按 1800 × (10-2)/10 × 1.0 = 1440 元 计算可退金额
```

#### Scenario: 已过期套餐展示「申请退款」入口

```gherkin
Given 用户已登录且名下有 1 个 expired 套餐（available_count = 3，now() > expire_at）
And   package 快照 refund_enabled = true，未超过 refund_valid_days
When  用户进入「我的套餐」页面
Then  expired 套餐在「已过期」分组展示
And   expired 套餐卡片展示「申请退款」入口
When  用户点击「申请退款」
Then  跳转 US-027 退款申请页
And   US-027 按 package 快照计算可退金额
```

#### Scenario: 已耗尽套餐不展示「申请退款」入口

```gherkin
Given 用户已登录且名下有 1 个 exhausted 套餐（consumed_count = total_hours）
And   package 快照 refund_enabled = true，未超过 refund_valid_days
When  用户进入「我的套餐」页面
Then  exhausted 套餐在「已耗尽」分组展示
And   exhausted 套餐卡片不展示「申请退款」入口
And   点击进入套餐详情页后不展示「申请退款」按钮
```

### Requirement: REQ-004 系统应提供教练视角套餐使用详情页

The system MUST provide a coach-view package usage detail page that is reachable from the US-037 student detail page package card. The page MUST display the package snapshot, usage statistics, student mini-card, and booking history. It MUST NOT expose purchase/upgrade actions.

#### Scenario: Coach opens package usage detail from US-037

```gherkin
Given 教练 C 已登录且为已通过状态
And   学员 S 与教练 C 存在 package P 关联
And   package P：status='active'，package_mode='standard'，total_hours=10，available=4，consumed_count=6
When  教练 C 在 US-037 学员详情页点击 package P 卡片
Then  系统跳转至 US-021 教练视角套餐使用详情页
And   页面展示 package P 快照：套餐名、模式、有效期、教学类型、泳姿、每节课时长
And   页面展示状态标签「使用中」与剩余 4 课时
And   页面展示套餐统计摘要：总课时 10 / 已用 6 / 剩余 4
And   页面展示学员 S 迷你卡，点击进入 C-学员详情编辑页
And   页面展示该 package 的使用记录列表
And   页面不展示购买/加课入口
```
