# Spec Delta: coach-manage-schedule-slots

> 本 spec 为 US-014 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 教练添加可约时段

系统 MUST 提供 `POST /api/coach/schedule-slots` 接口，供已登录且 `coach.status = 1` 的教练批量添加可约时段。系统 MUST 校验每个时段的 `start_time` 与 `end_time` 均晚于当前时间，否则返回 `PAST_TIME_NOT_ALLOWED`。系统 MUST 校验同一教练在同一 `start_time` 下不存在重叠时段，否则返回 `SLOT_TIME_CONFLICT`。系统 MUST 在保存成功后将 `schedule_slot.status` 置为 `available`，并立即失效 `coach:slots:{coach_id}:{date}` 缓存。系统 MUST 在添加成功后写入 `coach_schedule_log`（action=create）。

#### Scenario: 正常添加可约时段

```gherkin
Given 教练已通过审核且 coach.status = 1
And   明日 9:00-10:00 无其他可约时段
When  教练添加明日 9:00-10:00 的可约时段
And   教练点击「发布」
Then  系统返回 HTTP 201
And   schedule_slot 记录创建成功
And   schedule_slot.status = 'available'
And   coach_schedule_log 新增 action=create 的记录
And   学员端教练详情页展示该时段可预约
```

#### Scenario: 添加时段冲突

```gherkin
Given 教练已通过审核
And   教练已添加明日 9:00-10:00 的可约时段
When  教练尝试添加明日 9:30-10:30 的可约时段
Then  系统返回 HTTP 409 + 错误码 SLOT_TIME_CONFLICT
And   前端提示"该时段与已有可约时段冲突"
And   未创建新的 schedule_slot 记录
```

#### Scenario: 添加过去时间的时段

```gherkin
Given 教练已通过审核
When  教练尝试添加今日 8:00-9:00 的可约时段（当前时间已超过 8:00）
Then  系统返回 HTTP 400 + 错误码 PAST_TIME_NOT_ALLOWED
And   前端提示"不能添加过去时间的可约时段"
And   未创建新的 schedule_slot 记录
```

### Requirement: REQ-002 教练修改与删除可约时段

系统 MUST 提供 `PUT /api/coach/schedule-slots/{id}` 接口，供已登录且 `coach.status = 1` 的教练修改指定可约时段，修改时仍需满足时间不重叠与过去时间限制。系统 MUST 提供 `DELETE /api/coach/schedule-slots/{id}` 接口，供已登录且 `coach.status = 1` 的教练删除指定可约时段。系统 MUST 拒绝删除已被学员预约（`schedule_slot.status = 'booked'` 或存在关联 `booking` 记录）的时段，返回 `SLOT_HAS_BOOKING`。系统 MUST 校验教练对指定 `slot_id` 拥有所有权，否则返回 `SLOT_NOT_FOUND`。系统 MUST 在修改/删除成功后立即失效对应日期缓存并写入 `coach_schedule_log`（action=update/delete）。

#### Scenario: 删除已有预约的时段

```gherkin
Given 教练已通过审核
And   教练明日 9:00-10:00 的时段已被学员预约
When  教练尝试删除该时段
Then  系统返回 HTTP 409 + 错误码 SLOT_HAS_BOOKING
And   前端提示"该时段已有预约，无法删除"
And   schedule_slot 记录保持不变
```

#### Scenario: 修改时段时所有权校验失败

```gherkin
Given 教练 A 已通过审核
And   存在一条属于教练 B 的 schedule_slot 记录
When  教练 A 调用 PUT /api/coach/schedule-slots/{id} 修改该记录
Then  系统返回 HTTP 404 + 错误码 SLOT_NOT_FOUND
And   schedule_slot 记录保持不变
```

### Requirement: REQ-003 教练复制上周排班

系统 MUST 提供 `POST /api/coach/schedule-slots/copy-last-week` 接口，供已登录且 `coach.status = 1` 的教练一键复制上周已发布排班到本周。系统 MUST 读取 `venue_closure` 表，自动跳过闭馆日期。系统 MUST 对生成的每个时段执行与 REQ-001 相同的时间冲突与过去时间校验。系统 MUST 返回生成结果，包括成功创建数量与冲突/跳过的明细。系统 MUST 在复制成功后写入 `coach_schedule_log`（action=copy）。

#### Scenario: 批量复制上周排班

```gherkin
Given 教练已通过审核
And   教练上周已发布 5 个可约时段
And   本周其中 1 天为闭馆日
When  教练点击「复制上周排班」
Then  系统返回 HTTP 200
And   系统按上周模板生成本周 schedule_slot 记录
And   闭馆日期自动跳过
And   与已有 booking 冲突的时段标记为冲突待处理
And   coach_schedule_log 新增 action=copy 的记录
```

#### Scenario: 复制后全部冲突

```gherkin
Given 教练已通过审核
And   教练上周已发布 3 个可约时段
And   本周对应时间均已存在可约时段
When  教练点击「复制上周排班」
Then  系统返回 HTTP 200
And   响应体 data.created = 0
And   响应体 data.conflicts 包含 3 条冲突明细
And   未新增 schedule_slot 记录
```

### Requirement: REQ-004 学员端可见可约时段

系统 MUST 保证教练发布的可约时段在学员端教练详情页实时可见。系统 MUST 在 `schedule_slot` 创建、修改、删除或复制成功后，立即失效 `coach:slots:{coach_id}:{date}` 缓存，确保学员端查询到最新数据。系统 MUST 仅向学员展示 `status = 'available'` 且 `start_time` 晚于当前时间的时段。

#### Scenario: 发布后学员端可见

```gherkin
Given 教练已通过审核
And   教练已发布明日 9:00-10:00 的可约时段
When  学员打开该教练详情页
Then  页面展示明日 9:00-10:00 为可预约状态
And   学员点击后可进入预约流程
```
