# Spec Delta: system-auto-package-status-transition

> 本 spec 为 US-050 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 系统自动将到期套餐标记为 expired

系统 MUST 提供每小时运行的过期巡检任务。系统 MUST 仅扫描 `status='active'` 且 `expire_at <= NOW()` 的套餐；更新时必须原子执行，仅当更新前行状态为 `active` 时才允许转换为 `expired`；转换成功后 MUST 写入 `package_status_log`。

#### Scenario: 定时任务将到期套餐标记为 expired

```gherkin
Given 系统中存在套餐 A，status='active'，expire_at='2026-07-29 23:59:59'
And   当前系统时间为 2026-07-30 01:00:00
When  系统每小时套餐过期巡检任务执行
Then  套餐 A 的 status 更新为 'expired'
And   不修改 total_hours / available / reserved / consumed 字段
And   系统记录状态转换日志：from='active', to='expired', reason='EXPIRE_CRON'
```

#### Scenario: 过期套餐上仍有 reserved 课时仍正确标记为 expired

```gherkin
Given 套餐 C 的 expire_at='2026-07-29 23:59:59'，status='active'
And   套餐 C 当前 available=2，reserved=1，consumed=7
When  系统过期巡检任务执行
Then  套餐 C 的 status 更新为 'expired'
And   available 保持为 2，reserved 保持为 1，consumed 保持为 7
And   学员端「我的套餐」显示该套餐已过期，但已预约的 1 节课仍可正常上课
```

#### Scenario: 并发巡检保证同一套餐仅转换一次

```gherkin
Given 套餐 D 的 expire_at='2026-07-29 23:59:59'，status='active'
When  两个定时任务实例在相同时刻扫描到套餐 D 并尝试更新
Then  仅有一个实例成功将 status 更新为 'expired'
And   另一个实例收到 0 行更新或锁冲突，不重复写入状态转换日志
And   套餐 D 的状态转换日志表中仅有 1 条 from='active', to='expired' 记录
```

---

### Requirement: REQ-002 系统在课时耗尽时自动将套餐标记为 exhausted

系统 MUST 在教练确认上课、学员取消预约等事件后重算套餐状态。系统 MUST 仅在当前 `status='active'` 且更新后 `available=0` 且 `reserved=0` 时，将套餐转换为 `exhausted`；终态 `exhausted` / `expired` / `refunded` / `frozen` 不回退为 `active`。

#### Scenario: 教练确认最后一节课后套餐变为 exhausted

```gherkin
Given 学员持有套餐 B，status='active'，total_hours=10，available=1，reserved=0，consumed=9
And   该学员已预约明日 09:00 的 1 节课（此时 reserved=1，available=0）
When  教练确认该节课已完成
Then  系统扣减 reserved 至 0，增加 consumed 至 10
And   套餐 B 的 status 自动更新为 'exhausted'
And   系统记录状态转换日志：from='active', to='exhausted', reason='HOURS_EXHAUSTED'
```

#### Scenario: 取消预约恢复 available 后不应误将 exhausted 回退为 active

```gherkin
Given 套餐 E 的 status='exhausted'，total_hours=10，available=0，reserved=0，consumed=10
When  学员尝试取消一个已确认上课的历史记录（系统不允许取消已确认课程）
Then  系统拒绝该取消操作，返回错误码 COURSE_ALREADY_CONFIRMED
And   套餐 E 的 status 保持 'exhausted'
And   available / reserved / consumed 保持不变
```

---

### Requirement: REQ-003 状态转换日志与缓存一致性

系统 MUST 在每次有效状态转换后写入 `package_status_log`；系统 MUST 在状态变更后主动失效相关 Redis 缓存，保证下游读取到最新状态。

#### Scenario: 状态转换后缓存被失效

```gherkin
Given 套餐 F 的 status='active'，expire_at='2026-07-29 23:59:59'
And   Redis 中存在 key `package:F`
When  过期巡检任务将套餐 F 更新为 'expired'
Then  `package:F` 与 `user:F_owner:packages` 被删除
And   下一次查询命中数据库返回 status='expired'
```
