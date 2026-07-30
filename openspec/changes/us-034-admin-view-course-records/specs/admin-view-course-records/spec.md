# Spec Delta: admin-view-course-records

> 本 spec 为 US-034 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-034-1 管理员查看上课记录列表

系统 MUST 为具有 `course_record:read` 权限的管理员提供上课记录列表查询。系统 MUST 默认返回最近 30 天记录，支持按教练、学员、状态、时间范围筛选与分页，单次时间范围不得超过 1 年。

#### Scenario: 管理员正常查看上课记录列表

```gherkin
Given 管理员已登录且有查看权限
And   系统中存在 10 条已完成上课记录
When  管理员进入「上课记录」页面
Then  系统默认返回最近 30 天的记录列表
And   每条记录展示课程时间、教练姓名、学员姓名、状态、消耗课时
And   HTTP 状态码 = 200
```

#### Scenario: 管理员按条件筛选上课记录

```gherkin
Given 管理员已登录
And   系统中存在教练 100 的 3 条上课记录和教练 200 的 5 条上课记录
When  管理员选择教练 100 并点击筛选
Then  系统返回 3 条教练 100 的记录
And   返回 HTTP 200
```

#### Scenario: 无权限管理员访问

```gherkin
Given 管理员已登录但无上课记录查看权限
When  管理员请求上课记录列表
Then  系统返回 HTTP 403，错误码 ADMIN_PERMISSION_DENIED
And   不返回任何记录
```

#### Scenario: 筛选时间范围过大

```gherkin
Given 管理员已登录
When  管理员选择时间范围超过 1 年
Then  系统返回 HTTP 400，错误码 DATE_RANGE_TOO_LARGE
And   提示"时间范围不能超过 1 年"
```

### Requirement: REQ-034-2 管理员查看单条上课记录详情

系统 MUST 允许具有权限的管理员查看单条上课记录详情，展示课程时间、教练、学员、状态、消耗课时、课程内容、学员课后总结及课时变动日志。

#### Scenario: 管理员查看单条上课记录详情

```gherkin
Given 管理员已登录
And   存在 booking.status = 已完成，对应 course_record.content = "自由泳打腿"
When  管理员点击该记录详情
Then  系统返回课程时间、教练、学员、状态、消耗课时、课程内容、学员课后总结
And   HTTP 状态码 = 200
```
