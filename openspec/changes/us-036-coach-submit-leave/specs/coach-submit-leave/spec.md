# Spec Delta: coach-submit-leave

> 本 spec 为 US-036 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-036-1 教练提交请假申请

系统 MUST 允许已通过的教练提交请假申请。系统 MUST 校验教练状态为已通过（`coach.status = 1`），请假开始时间不早于当前时间（已开始课程不可请假），结束时间晚于开始时间。系统 MUST 拒绝与已批准请假时间重叠的申请。系统创建请假记录后，其状态 MUST 为待审批（`status = 0`），并返回成功提示。

#### Scenario: 正常提交请假申请

```gherkin
Given 教练状态为已通过（coach.status = 1）
And   当前时间为 2026-07-30 08:00
And   教练不存在与该时段重叠的已批准请假
When  教练提交 2026-08-01 09:00 至 2026-08-01 18:00 的请假申请
And   填写请假原因为 "病假"
Then  coach_leave 记录创建成功
And   coach_leave.status = 0（待审批）
And   返回提示 "提交成功，等待管理员审批"
```

#### Scenario: 请假时间与已批准请假冲突

```gherkin
Given 教练已存在 2026-08-01 09:00 至 2026-08-01 12:00 的已批准请假
When  教练提交 2026-08-01 10:00 至 2026-08-01 14:00 的请假申请
Then  返回错误码 LEAVE_TIME_CONFLICT
And   前端提示 "请假时间冲突，请重新选择"
And   不创建新的 coach_leave 记录
```

#### Scenario: 请假开始时间已过

```gherkin
Given 当前时间为 2026-08-01 10:00
And   教练状态为已通过（coach.status = 1）
When  教练提交 2026-08-01 09:00 至 2026-08-01 12:00 的请假申请
Then  返回错误码 LEAVE_START_PAST
And   前端提示 "请假开始时间不能早于当前时间"
And   不创建新的 coach_leave 记录
```

#### Scenario: 结束时间早于开始时间

```gherkin
Given 教练状态为已通过（coach.status = 1）
When  教练提交 2026-08-01 18:00 至 2026-08-01 09:00 的请假申请
Then  返回错误码 INVALID_TIME_RANGE
And   前端提示 "结束时间不能早于开始时间"
And   不创建新的 coach_leave 记录
```

#### Scenario: 教练状态非已通过

```gherkin
Given 教练状态为待审核（coach.status = 0）
When  教练提交 2026-08-01 09:00 至 2026-08-01 12:00 的请假申请
Then  返回错误码 COACH_STATUS_INVALID
And   前端提示 "当前状态不可提交请假申请"
And   不创建新的 coach_leave 记录
```

#### Scenario: 重复提交幂等

```gherkin
Given 教练状态为已通过（coach.status = 1）
And   教练在 3 秒内已提交过同一时段请假申请
When  教练再次提交完全相同的请假申请
Then  不创建新的 coach_leave 记录
And   返回已有请假单信息
And   前端提示 "请勿重复提交"
```

---

### Requirement: REQ-036-2 教练查询请假列表

系统 MUST 允许教练分页查询自己的请假记录。系统 MUST 仅返回当前登录教练的请假记录，禁止返回其他教练的数据。

#### Scenario: 正常查询请假列表

```gherkin
Given 教练已提交 2 条请假申请
When  教练请求第 1 页（size=10）
Then  返回 200 与包含 2 条记录的列表
And   每条记录包含 leave_id / start_time / end_time / reason / status
```

#### Scenario: 越权查询其他教练请假列表

```gherkin
Given 教练 A 已登录
When  教练 A 尝试通过接口查询教练 B 的请假列表
Then  系统仅返回教练 A 自己的请假记录
```
