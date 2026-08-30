# Spec Delta: admin-config-release-rule

> 本 spec 为 US-015 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 管理员查询释放规则

系统 MUST 提供管理端接口供管理员查询当前预约释放规则。系统 MUST 返回释放星期、释放时间、释放范围、候补开关、候补有效期、关注提醒提前时间、节假日提前释放配置。

#### Scenario: 正常查询默认规则

```gherkin
Given 管理员已登录并拥有 SCHEDULE_RELEASE_CONFIG 权限
When  管理员访问释放规则配置页
Then  接口返回 release_weekday = 3
And   release_time = "10:00:00"
And   release_scope = "NEXT_WEEK"
And   waitlist_enabled = true
```

### Requirement: REQ-002 管理员更新释放规则

系统 MUST 允许管理员更新预约释放规则。系统 MUST 校验释放时间格式合法、关注提醒提前时间不超过释放周期；校验失败时 MUST 拒绝更新并返回明确错误码。

#### Scenario: 正常更新释放时间

```gherkin
Given 管理员已登录并拥有 SCHEDULE_RELEASE_CONFIG 权限
When  管理员将 release_time 改为 "14:00:00" 并提交
Then  release_rule 表对应字段更新为 "14:00:00"
And   接口返回 HTTP 200 与更新后完整配置
```

#### Scenario: 配置节假日提前释放

```gherkin
Given 管理员已登录并拥有 SCHEDULE_RELEASE_CONFIG 权限
And   当前默认释放日为周三 10:00
When  管理员启用 holiday_release_enabled 并设置 offset_days = -1（提前至本周二 10:00）
Then  release_rule 表 holiday_release_enabled = true
And   holiday_release_offset_days = -1
And   接口返回 HTTP 200
```

#### Scenario: 释放时间格式非法

```gherkin
Given 管理员已登录并拥有 SCHEDULE_RELEASE_CONFIG 权限
When  管理员提交 release_time = "25:70"
Then  接口返回 HTTP 400，错误码 INVALID_RELEASE_TIME
And   release_rule 表不被修改
```

#### Scenario: 关注提醒时间超过释放周期

```gherkin
Given 管理员已登录并拥有 SCHEDULE_RELEASE_CONFIG 权限
And   当前释放周期为 7 天
When  管理员提交 watch_reminder_minutes = 10080
Then  接口返回 HTTP 400，错误码 REMINDER_TOO_LONG
And   release_rule 表不被修改
```

#### Scenario: 无权限管理员保存

```gherkin
Given 管理员已登录但无 SCHEDULE_RELEASE_CONFIG 权限
When  该管理员调用更新接口
Then  接口返回 HTTP 403，错误码 FORBIDDEN
And   release_rule 表不被修改
```
