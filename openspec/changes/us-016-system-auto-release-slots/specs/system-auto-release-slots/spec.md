# Spec Delta: system-auto-release-slots

> 本 spec 为 US-016 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 自动释放可约时段

系统 MUST 在到达 `release_rule` 配置的释放时间时，自动读取所有已通过教练的可约时段模板，并按规则生成 `schedule_slot`。系统 MUST 防止重复释放同一周期时段。

#### Scenario: 正常释放下周整周时段

```gherkin
Given 当前为周三 10:00 且 release_rule 配置为释放下周整周
And   教练 A 已设置周一至周日 9:00-10:00 可约
When  释放任务触发
Then  系统生成 coach_id=A 的 7 条 schedule_slot 记录
And   每条 slot.status = "available"
And   slot.start_time 覆盖下周一到周日 9:00
And   release_log 记录 generated_slots = 7
```

#### Scenario: 节假日提前释放

```gherkin
Given release_rule.holiday_release_enabled = true
And   holiday_release_offset_days = -6
And   下周一为法定节假日
When  系统计算释放时间
Then  释放任务于本周二 10:00 触发
And   不生成下周一闭馆日的 slot
```

#### Scenario: 教练未设置模板

```gherkin
Given 释放任务触发
And   教练 B 的可约时段模板为空
When  系统读取教练 B 的模板
Then  系统跳过教练 B
And   记录 warn 日志 "coach_id=B 无可用模板"
```

#### Scenario: 释放任务超时未执行

```gherkin
Given 上次释放任务因服务器宕机未执行
And   当前时间已超过上次释放时间 5 分钟
When  兜底定时任务扫描
Then  任务检测到漏执行并立即补偿释放
```

#### Scenario: 防止重复释放

```gherkin
Given 本周释放任务已执行成功
When  释放任务再次触发
Then  系统检查 release_log 后跳过本次释放
And   返回幂等响应 "本周已释放"
```
