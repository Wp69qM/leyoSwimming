# Spec Delta: admin-login

> 本 spec 为 US-053 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 管理员账号密码登录

系统 MUST 提供 Web 后台管理系统登录页。管理员 MUST 输入用户名和密码后点击「登录」按钮。系统 MUST 校验用户名和密码的正确性，并校验账号状态为启用（`status = 0`）。校验通过后，系统 MUST 生成单一管理员会话 token（有效期 24 小时），将 token 和 userInfo 写入前端 localStorage，记录登录成功日志，并跳转至后台首页。校验失败时，系统 MUST 返回明确的错误码和提示，不生成 token。token 过期时，后端 MUST 返回 401，前端 MUST 自动跳转登录页并提示"登录信息已过期，请重新登录"。

#### Scenario: 管理员正常登录

```gherkin
Given 管理员账号 "admin" 已存在且状态为启用
And   管理员当前处于未登录态
When  管理员访问后台管理系统
And   输入正确的用户名 "admin"
And   输入正确的密码 "correct_password"
And   点击「登录」按钮
Then  系统校验通过
And   系统生成单一管理员会话 token（有效期 24 小时）
And   系统将 token 和 userInfo 写入前端 localStorage
And   系统记录登录成功日志
And   页面跳转至后台首页
```

#### Scenario: 用户名或密码错误

```gherkin
Given 管理员当前处于未登录态
When  管理员在登录页输入用户名 "admin"
And   输入错误的密码 "wrong_password"
And   点击「登录」按钮
Then  系统返回 401 ADMIN_CREDENTIALS_INVALID
And   页面提示 "用户名或密码错误"
And   系统不生成 token
And   系统记录登录失败日志
```

#### Scenario: 管理员账号被禁用

```gherkin
Given 管理员账号 "disabled_admin" 存在但状态为禁用（status = 1）
And   管理员当前处于未登录态
When  管理员输入正确的用户名和密码
And   点击「登录」按钮
Then  系统返回 403 ADMIN_DISABLED
And   页面提示 "账号已被禁用，请联系超级管理员"
And   系统不生成 token
```

---

### Requirement: REQ-002 管理员退出登录

系统 MUST 在后台管理系统全局导航栏右上角展示当前登录管理员的名字（来自 localStorage 中的 userInfo）。管理员点击名字后 MUST 展开下拉菜单，菜单中包含「退出登录」选项。点击「退出登录」后，系统 MUST 调用后端退出登录接口，使当前 admin_session 失效，清除前端 localStorage 中的 token 和 userInfo，并跳转回管理员登录页。

#### Scenario: 管理员退出登录

```gherkin
Given 管理员已成功登录后台管理系统
When  管理员点击右上角管理员名字
And   点击下拉菜单中的「退出登录」
Then  系统调用 POST /api/admin/auth/logout
And   当前 admin_session 被标记为失效
And   前端清除 admin_token 及 admin_user
And   页面跳转回管理员登录页
```
