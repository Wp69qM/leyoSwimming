# Spec Delta: admin-view-dashboard-tickets

> 本 spec 为 US-049 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 管理员查看数据看板

系统 MUST 提供管理员数据看板接口，默认展示今日/本周/本月关键指标与 5 项基础看板。系统 MUST 校验时间范围合法性；校验通过后返回对应图表数据。

#### Scenario: 管理员查看数据看板

```gherkin
Given 管理员已登录且具有数据看板权限
And   系统已存在体验课预约记录 120 条，正价课预约记录 300 条
When  管理员选择时间范围 2026-07-01 至 2026-07-31 查看"游泳高峰期"看板
Then  系统返回 HTTP 200
And   返回 24 个时段的体验课预约分布数据，每个时段 count ≥ 0
And   响应时间 < 500ms
And   页面展示折线图
```

---

### Requirement: REQ-002 管理员查看客服工单列表与详情

系统 MUST 提供客服工单列表与详情查询接口。列表 MUST 支持按状态、类型筛选与分页；详情 MUST 返回工单信息与回复记录。

#### Scenario: 管理员处理客服工单

```gherkin
Given 管理员已登录且具有客服工单处理权限
And   存在工单 T-001，status='pending'，类型="换教练申请"
When  管理员将工单 T-001 分配给客服 A
And   管理员提交回复："已为您安排新教练，请查看短信通知"
And   管理员将工单状态更新为"closed"
Then  系统返回 HTTP 200
And   support_ticket 表 T-001 的 handler_id=客服 A ID，status='closed'
And   ticket_reply 表新增 1 条记录
And   提交者收到工单处理结果通知
```

#### Scenario: 工单不存在

```gherkin
Given 管理员已登录且具有客服工单处理权限
When  管理员查看 id=99999 的工单详情
Then  系统返回 HTTP 404
And   返回错误码 TICKET_NOT_FOUND
```

---

### Requirement: REQ-003 管理员处理客服工单

系统 MUST 允许管理员分配工单处理人、提交回复、更新工单状态。系统 MUST 校验工单当前状态；closed 后 MUST 禁止回复与分配。

#### Scenario: 工单已关闭后仍尝试回复

```gherkin
Given 管理员已登录且具有客服工单处理权限
And   存在工单 T-002，status='closed'
When  管理员对 T-002 提交回复
Then  系统返回 HTTP 400
And   返回错误码 TICKET_CLOSED
And   ticket_reply 表不新增记录
```

---

### Requirement: REQ-004 管理员权限控制

系统 MUST 对数据看板接口与客服工单接口分别进行权限控制。无对应权限的管理员 MUST 无法调用接口，系统 MUST 返回 HTTP 403。

#### Scenario: 无权限管理员访问看板接口

```gherkin
Given 管理员已登录但角色无"数据看板"权限
When  管理员调用 GET /api/admin/dashboard/peak-hours
Then  系统返回 HTTP 403
And   返回错误码 FORBIDDEN
And   不返回任何看板数据
```

#### Scenario: 无权限管理员访问客服工单接口

```gherkin
Given 管理员已登录但角色无"客服工单"权限
When  管理员调用 GET /api/admin/tickets
Then  系统返回 HTTP 403
And   返回错误码 FORBIDDEN
And   不返回任何工单数据
```
