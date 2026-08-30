> **OpenSpec Spec | 映射自 `docs/stories/US-047-管理员-配置场馆公告闭馆换水与用户须知和隐私协议/user-story.md` §6**

## Capability

管理员配置场馆、公告、闭馆换水、《用户须知》与《隐私协议》（含签署记录）

## ADDED Requirements

### Requirement: REQ-001 管理员配置场馆信息

系统 MUST 提供管理员更新场馆信息接口。系统 MUST 校验字段合法性，更新后 MUST 同步影响游客端/学员端场馆展示。

#### Scenario: 管理员更新场馆信息

```gherkin
Given 管理员已登录且具有场馆运营权限
And   当前场馆名称为"乐游健身中心"
When  管理员提交场馆信息：名称="乐游游泳馆", 地址="XX路XX号", 泳池状态="开放"
Then  系统返回 HTTP 200
And   venue 表 name 更新为"乐游游泳馆"
And   游客端/学员端场馆展示页同步显示最新场馆信息
```

---

### Requirement: REQ-002 管理员发布公告

系统 MUST 提供管理员发布公告接口。公告 MUST 支持设置展示目标身份与有效期，发布后 MUST 按目标身份即时展示。

#### Scenario: 管理员发布公告

```gherkin
Given 管理员已登录且具有场馆运营权限
When  管理员发布公告：标题="暑期课程安排", 内容="7月起增加晚间时段", 展示身份="全部"
Then  系统返回 HTTP 200
And   notice 表新增 1 条记录，title="暑期课程安排"，status='published'
And   游客端/学员端首页通知栏展示"暑期课程安排"
```

---

### Requirement: REQ-003 管理员设置闭馆并自动取消受影响预约

系统 MUST 提供闭馆/换水设置接口。设置闭馆后 MUST 自动取消该日期内未上课的预约并释放课时，且 MUST 向学员与教练发送通知。异步取消任务 MUST 实现失败补偿：单条 booking 取消失败时自动重试最多 3 次（指数退避），仍失败则标记 `partial_failed` 并写入 `closure_task_failed` 表，供管理员人工介入。

#### Scenario: 管理员设置闭馆并异步取消受影响预约

```gherkin
Given 场馆正常开放
And   存在 2026-08-01 09:00 的已预约课程 5 节
When  管理员设置 2026-08-01 全天闭馆（原因：换水）
Then  系统返回 HTTP 202 Accepted
And   响应体含 { task_id, affected_bookings: 5, status: "processing" }
And   venue_closure 表新增记录
And   系统异步执行取消任务（与 §12 性能要求一致）
When  异步任务完成（数秒内）
Then  5 节预约状态变更为"已取消"，cancel_reason=4（场馆闭馆）
And   释放 package.reserved → available
And   向学员与教练发送闭馆通知
And   管理员后台可查询 task_id 的最终状态为 "completed"
```

#### Scenario: 闭馆日期冲突

```gherkin
Given 已存在 2026-08-01 的闭馆记录
When  管理员再次设置 2026-08-01 闭馆
Then  系统返回 HTTP 409
And   返回错误码 CLOSURE_DATE_CONFLICT
And   不新增重复闭馆记录
```

---

### Requirement: REQ-004 管理员更新《用户须知》并触发重新同意

系统 MUST 提供《用户须知》版本管理接口。新版本发布时 MUST 将旧版本 `is_current` 置为 `false`，并将所有已同意旧版本的用户/教练标记为"待重新同意"，同时 MUST 向所有注册用户推送更新提醒。

#### Scenario: 管理员更新《用户须知》并触发重新同意

```gherkin
Given 当前《用户须知》版本为 v1.0
And   学员 U-001 已同意《用户须知》v1.0
When  管理员在「协议管理」Tab 的「《用户须知》」子 Tab 中发布 v2.0《用户须知》
Then  系统返回 HTTP 200
And   terms_policy 表新增 v2.0 记录，is_current=true，v1.0 is_current=false
And   学员 U-001 的《用户须知》同意状态变为"待重新同意"
And   系统向所有注册用户推送《用户须知》更新提醒
```

#### Scenario: 《用户须知》内容为空

```gherkin
Given 管理员已登录
When  管理员提交空内容的《用户须知》
Then  系统返回 HTTP 400
And   返回错误码 TERMS_CONTENT_EMPTY
And   不创建新版本
```

---

### Requirement: REQ-005 管理员更新《隐私协议》并触发重新同意

系统 MUST 提供《隐私协议》版本管理接口。新版本发布时 MUST 将旧版本 `is_current` 置为 `false`，并将所有已同意旧版本的用户/教练标记为"待重新同意"，同时 MUST 向所有注册用户推送更新提醒。

#### Scenario: 管理员更新《隐私协议》并触发重新同意

```gherkin
Given 当前《隐私协议》版本为 v1.0
And   学员 U-001 已同意《隐私协议》v1.0
When  管理员在「协议管理」Tab 的「《隐私协议》」子 Tab 中发布 v2.0《隐私协议》
Then  系统返回 HTTP 200
And   privacy_policy 表新增 v2.0 记录，is_current=true，v1.0 is_current=false
And   学员 U-001 的《隐私协议》同意状态变为"待重新同意"
And   系统向所有注册用户推送《隐私协议》更新提醒
```

#### Scenario: 《隐私协议》内容为空

```gherkin
Given 管理员已登录
When  管理员提交空内容的《隐私协议》
Then  系统返回 HTTP 400
And   返回错误码 PRIVACY_CONTENT_EMPTY
And   不创建新版本
```

---

### Requirement: REQ-006 管理员查看签署记录

系统 MUST 提供《用户须知》与《隐私协议》签署记录查询接口，支持按协议类型、版本、用户筛选。

#### Scenario: 管理员查看《用户须知》签署记录

```gherkin
Given 管理员已登录
And   存在 10 条 v2.0《用户须知》签署记录
When  管理员查询 v2.0《用户须知》签署记录
Then  系统返回 10 条记录
And   每条记录包含用户、签署时间、版本号、状态
```

#### Scenario: 管理员查看《隐私协议》签署记录

```gherkin
Given 管理员已登录
And   存在 10 条 v2.0《隐私协议》签署记录
When  管理员查询 v2.0《隐私协议》签署记录
Then  系统返回 10 条记录
And   每条记录包含用户、签署时间、版本号、状态
```

## Delta Header

```yaml
delta:
  change: us-047-admin-config-venue-notice-closure-terms
  capability: admin-config-venue-notice-closure-terms
  type: revise
  rationale: 扩展协议管理能力：新增《隐私协议》管理，统一表名为 terms_policy / privacy_policy / user_terms_consent / user_privacy_consent
  scope: docs/stories/US-047, openspec/changes/us-047-admin-config-venue-notice-closure-terms
```
