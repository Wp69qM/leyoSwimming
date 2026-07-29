# Spec Delta: release-countdown

> 本 spec 为 US-003 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 释放倒计时显示

系统 MUST 在首页展示预约释放倒计时，引导游客在释放时刻抢约。系统 MUST 根据当前服务器时间与 `release_rule` 配置计算倒计时状态，并在释放前 24 小时窗口内显示倒计时组件。

倒计时文案 MUST 遵循以下规则：
- 释放前 24 小时内且非释放当天：显示「距离下周预约开放还有 XX 小时」
- 释放当天且在释放时刻前：显示「今日 XX:XX 开放下周预约」（XX:XX 取自 `release_rule.release_time`）
- 其他时段（含释放时刻刚过）：MUST 隐藏倒计时组件

系统 MUST 使用服务器时间进行计算，不得信任客户端时间。前端 MUST 每秒本地递减倒计时，并每 10 秒拉取一次 API 校准，避免客户端时间漂移。

#### Scenario: 释放前 24 小时显示倒计时

```gherkin
Given 当前时间为周二 12:00
And  释放规则配置为"每周三 10:00 释放"
And  倒计时窗口为"释放前 24 小时"
When  游客打开小程序首页
Then  首页顶部展示倒计时组件
And   文案为"距离下周预约开放还有 22 小时"
And   倒计时随时间推移自动刷新
```

#### Scenario: 释放当天显示"今日 10:00 开放下周预约"

```gherkin
Given 当前时间为周三 08:30
And  释放规则配置为"每周三 10:00 释放"
When  游客打开小程序首页
Then  首页顶部展示倒计时组件
And   文案为"今日 10:00 开放下周预约"
And   倒计时显示距离 10:00 的剩余分钟数
```

#### Scenario: 非释放时段不显示倒计时

```gherkin
Given 当前时间为周三 10:01（释放时刻刚过）
And  下一次释放时间为下周三 10:00
And  当前时间距下次释放超过 24 小时
When  游客打开小程序首页
Then  首页不展示倒计时组件
And   首页其他内容正常加载
```

---

### Requirement: REQ-002 释放规则读取与异常处理

系统 MUST 读取 `release_rule` 表中 `status=1`（启用）的规则计算倒计时。系统 MUST 优先使用未过期的 `manual_override_at`（管理员手动覆盖时间）作为释放时刻；若该字段为 NULL 或已过期，则按常规 `release_day_of_week` + `release_time` 计算。

当 `release_rule` 未配置或被禁用时，系统 MUST 返回 `show: false` 并记录 WARN 日志，且不得影响首页其他模块的正常加载。

系统 MUST 对倒计时计算结果进行 Redis 缓存（TTL 10s），避免高频请求穿透至数据库。系统 MUST 使用数据库 `NOW()` 函数获取服务器时间，不得使用应用服务器时间，以避免时间不一致问题。

#### Scenario: 释放规则未配置时不显示倒计时

```gherkin
Given 管理员未在后台配置 release_rule
Or    release_rule.status = 0（规则被禁用）
When  游客打开小程序首页
Then  首页不展示倒计时组件
And   首页其他内容正常加载
And   后端记录一条 WARN 日志"release_rule not configured or disabled"
```

#### Scenario: 管理员手动覆盖释放时间时优先使用

```gherkin
Given 管理员设置了 manual_override_at 为"2026-08-06 10:00"
And  当前时间为"2026-08-05 12:00"（覆盖时间未过期）
When  游客打开小程序首页
Then  倒计时按 manual_override_at 计算
And   文案为"距离下周预约开放还有 22 小时"
```

#### Scenario: 手动覆盖时间已过期时忽略并按常规计算

```gherkin
Given 管理员设置了 manual_override_at 为"2026-08-03 10:00"
And  当前时间为"2026-08-04 12:00"（覆盖时间已过）
When  游客打开小程序首页
Then  倒计时按常规 release_day_of_week + release_time 计算
And   不使用已过期的 manual_override_at
```
