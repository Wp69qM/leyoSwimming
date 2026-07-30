# Spec Delta: admin-config-homepage-ops

> 本 spec 为 US-048 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 管理员配置首页通知栏与 Banner

系统 MUST 提供管理员配置首页通知栏与 Banner 的接口。系统 MUST 校验标题/名称非空、图片 URL 非空、有效期合法、可见范围合法；校验通过后写入 `notice` / `homepage_banner` 表并即时生效。

#### Scenario: 管理员配置通知栏与 Banner 并即时生效

```gherkin
Given 管理员已登录且具有首页运营权限
And   当前不存在标题为"暑期班招生中"的通知栏
And   当前不存在名称为"清凉一夏"的 Banner
When  管理员提交通知栏：标题="暑期班招生中", 可见范围=全部, 生效时间=2026-07-30 00:00, 失效时间=2026-08-31 23:59
And   管理员提交 Banner：名称="清凉一夏", 图片URL="https://cdn.example.com/banner1.jpg", 排序=1, 可见范围=全部
Then  系统返回 HTTP 200
And   notice 表新增 1 条记录，title="暑期班招生中"，status='published'，visible_scope='all'
And   homepage_banner 表新增 1 条记录，name="清凉一夏"，sort_order=1，status='active'
And   游客端/学员端首页立即展示"暑期班招生中"通知栏与"清凉一夏" Banner
```

#### Scenario: 有效期不合法

```gherkin
Given 管理员已登录且具有首页运营权限
When  管理员提交 Banner：生效时间=2026-08-01 00:00, 失效时间=2026-07-30 23:59
Then  系统返回 HTTP 400
And   返回错误码 INVALID_TIME_RANGE
And   homepage_banner 表不新增记录
```

---

### Requirement: REQ-002 管理员配置运营卡片与预览

系统 MUST 提供管理员配置首页运营卡片并按身份预览的接口。系统 MUST 支持可见范围 `all/visitor/registered/student`；预览接口 MUST 根据所选身份过滤展示内容。

#### Scenario: 管理员配置可见范围与预览运营卡片

```gherkin
Given 管理员已登录且具有首页运营权限
When  管理员提交运营卡片：标题="老学员续费入口", 可见范围=student, 排序=2, 生效时间=2026-07-30 00:00, 失效时间=2026-09-30 23:59
And   管理员点击"预览"并选择身份=学员
Then  系统返回预览结果中展示"老学员续费入口"卡片
And   选择身份=游客时预览结果中不展示该卡片
And   homepage_card 表新增 1 条记录，visible_scope='student'，status='active'
```

#### Scenario: 可见范围参数非法

```gherkin
Given 管理员已登录
When  管理员提交运营卡片：可见范围="vip"
Then  系统返回 HTTP 400
And   返回错误码 INVALID_VISIBLE_SCOPE
And   homepage_card 表不新增记录
```

---

### Requirement: REQ-003 管理员权限控制

系统 MUST 对首页运营接口进行权限控制。无 `homepage:write` 权限的管理员 MUST 无法调用写接口，系统 MUST 返回 HTTP 403。

#### Scenario: 无权限管理员访问配置接口

```gherkin
Given 管理员已登录但角色无"首页运营"权限
When  管理员调用 POST /api/admin/homepage/notices
Then  系统返回 HTTP 403
And   返回错误码 FORBIDDEN
And   notice 表不新增记录
```
