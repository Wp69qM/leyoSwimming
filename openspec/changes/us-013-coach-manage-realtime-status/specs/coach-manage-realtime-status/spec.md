# Spec Delta: coach-manage-realtime-status

> 本 spec 为 US-013 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 教练手动更新实时状态

系统 MUST 提供 `GET /api/coach/realtime-status` 接口，供已登录且 `coach.status = 1` 的教练查询当前实时状态。系统 MUST 提供 `PUT /api/coach/realtime-status` 接口，供已登录且 `coach.status = 1` 的教练手动设置实时状态。实时状态 SHALL 为以下枚举之一：1=空闲中、2=上课中、3=休息中、4=已下班、5=请假中。审批通过的请假时段内，系统 MUST 将 `coach.realtime_status` 强制为 5（请假中）并锁定手动修改入口。系统 MUST 拒绝将状态设置为请假中，除非该教练存在审批通过的请假申请（由 US-036 同步）。系统 MUST 在手动更新成功后将 `status_override_flag` 置为 1，并记录 `coach_status_log`（source=manual）。系统 MUST 通过 WebSocket/SSE 将状态变更广播给正在查看该教练的学员端，P99 延迟小于 1 秒。

#### Scenario: 教练手动设置为空闲中

```gherkin
Given 教练已通过审核且 coach.status = 1
And   教练当前 realtime_status = 4（已下班）
When  教练在教练端点击状态切换并选择"空闲中"
And   教练点击「确认」
Then  系统返回 HTTP 200
And   coach.realtime_status = 1（空闲中）
And   coach.status_override_flag = 1（手动覆盖）
And   coach_status_log 新增 source=manual 的变更记录
And   学员端教练详情页展示"空闲中"标签
```

#### Scenario: 手动设置请假中被拒绝

```gherkin
Given 教练已通过审核且当前无审批通过的请假申请
When  教练尝试将实时状态设置为 5（请假中）
Then  系统返回 HTTP 400 + 错误码 LEAVE_REQUIRED
And   前端提示"请先提交请假申请"
And   coach.realtime_status 保持不变
```

#### Scenario: 请假中状态锁定

```gherkin
Given 教练当前 realtime_status = 5（请假中）且存在审批通过的请假申请
When  教练尝试将实时状态手动修改为"空闲中"
Then  系统返回 HTTP 403 + 错误码 STATUS_LOCKED_BY_LEAVE
And   前端提示"请假期间状态不可手动修改"
And   coach.realtime_status 保持 5（请假中）
```

### Requirement: REQ-002 系统自动切换实时状态

系统 MUST 每分钟执行一次定时任务，扫描未来 15 分钟内有已确认预约课程的教练，并将其 `realtime_status` 自动切换为 2（上课中）。系统 MUST 在课程结束后 15 分钟将教练 `realtime_status` 自动恢复为 1（空闲中）或 4（已下班），具体恢复目标由教练当前排班决定。系统 MUST 在自动切换时记录 `coach_status_log`（source=auto）。若教练处于手动覆盖状态（`status_override_flag = 1`）且未超过手动覆盖有效期，系统 MUST 跳过自动切换并保持手动状态，同时记录一次手动覆盖事件。

#### Scenario: 系统自动切换为上课中

```gherkin
Given 教练已通过审核且 coach.status = 1
And   教练明日 9:00 有一节已确认的预约课程
And   教练当前 realtime_status = 1（空闲中）
And   coach.status_override_flag = 0
When  当前时间到达明日 8:45
Then  系统自动将 coach.realtime_status 切换为 2（上课中）
And   coach.status_override_flag 保持 0
And   coach_status_log 新增 source=auto 的变更记录
And   学员端教练列表展示"上课中"标签
```

#### Scenario: 非请假时段手动状态优先级高于自动状态

```gherkin
Given 教练已通过审核且 coach.status = 1
And   教练已手动设置为"休息中"且 status_override_flag = 1
And   手动覆盖有效期未过期
And   教练当前不在审批通过的请假时段
And   教练明日 9:00 有一节已确认的预约课程
When  当前时间到达明日 8:45
Then  coach.realtime_status 保持 3（休息中）
And   系统记录一次手动覆盖事件
And   学员端教练详情页继续展示"休息中"标签
```

#### Scenario: 课程结束后自动恢复

```gherkin
Given 教练已通过审核且当前 realtime_status = 2（上课中）
And   教练 10:00 的课程已结束
And   coach.status_override_flag = 0
When  当前时间到达 10:15
Then  系统自动将 coach.realtime_status 恢复为 1（空闲中）
And   coach_status_log 新增 source=auto 的变更记录
```
