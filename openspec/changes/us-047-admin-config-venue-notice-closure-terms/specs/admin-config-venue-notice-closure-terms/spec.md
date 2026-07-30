# Spec Delta: admin-config-venue-notice-closure-terms

> 本 spec 为 US-047 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 管理员配置场馆信息并发布公告

系统 MUST 提供管理员更新场馆信息和发布公告接口。公告 MUST 支持设置展示目标身份与有效期。

#### Scenario: 管理员配置场馆信息并发布公告

```gherkin
Given 管理员已登录且具有场馆运营权限
When  管理员更新场馆信息：名称="乐游游泳馆", 地址="XX路XX号", 泳池状态="开放"
And   管理员发布公告：标题="暑期课程安排", 内容="7月起增加晚间时段", 展示身份="全部"
Then  系统返回 HTTP 200
And   venue 表更新为名称="乐游游泳馆"
And   notice 表新增 1 条记录，status='published'
And   游客端/学员端首页通知栏展示"暑期课程安排"
```

---

### Requirement: REQ-002 管理员设置闭馆并自动取消受影响预约

系统 MUST 提供闭馆/换水设置接口。设置闭馆后 MUST 自动取消该日期内未上课的预约并释放课时，且 MUST 向学员与教练发送通知。

#### Scenario: 管理员设置闭馆并自动取消受影响预约

```gherkin
Given 场馆正常开放
And   存在 2026-08-01 09:00 的已预约课程 5 节
When  管理员设置 2026-08-01 全天闭馆（原因：换水）
Then  系统返回 HTTP 200
And   closure 表新增记录
And   5 节预约状态变更为"已取消"，cancel_reason='venue_closure'
And   释放 package.reserved → available
And   向学员与教练发送闭馆通知
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

### Requirement: REQ-003 管理员更新《用户须知》并触发重新签署

系统 MUST 提供《用户须知》版本管理接口。新版本发布时 MUST 将旧版本置为 inactive，并将所有已签署用户标记为"待重新签署"，同时 MUST 向所有注册用户推送更新提醒。

#### Scenario: 管理员更新《用户须知》并触发重新签署

```gherkin
Given 当前《用户须知》版本为 v1.0
And   学员 U-001 已签署 v1.0
When  管理员发布 v2.0《用户须知》
Then  系统返回 HTTP 200
And   terms 表新增 v2.0 记录，is_active=true，v1.0 is_active=false
And   学员 U-001 的签署状态变为"待重新签署"
And   系统向所有注册用户推送更新提醒
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

### Requirement: REQ-004 管理员查看签署记录

系统 MUST 提供《用户须知》签署记录查询接口，支持按版本、用户筛选。

#### Scenario: 管理员查看签署记录

```gherkin
Given 管理员已登录
And   存在 10 条 v2.0 签署记录
When  管理员查询 v2.0 签署记录
Then  系统返回 10 条记录
And   每条记录包含用户、签署时间、版本号
```
