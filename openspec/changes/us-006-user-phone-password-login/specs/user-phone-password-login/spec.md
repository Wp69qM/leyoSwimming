# Spec Delta: user-phone-password-login

> 本 spec 为 US-006 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 手机号验证码登录

系统 MUST 提供 `POST /api/auth/login/phone` 接口，供用户使用手机号和短信验证码登录。系统 MUST 校验手机号格式、验证码是否存在且未过期、验证码未被使用过。系统 MUST 在登录成功后生成登录态 token、更新 `user.last_login_at`、记录 `user_login_log`。系统 MUST 在验证码错误或过期时返回 `INVALID_CREDENTIALS` 或 `CODE_EXPIRED` 且不生成登录态。系统 MUST 对同一手机号 60 秒内只能发送 1 条验证码。

#### Scenario: 手机号验证码登录成功

```gherkin
Given 用户已注册且手机号 13800138000 状态正常
And   系统已为该手机号生成未过期验证码 123456
When  用户输入手机号 13800138000 和验证码 123456
And   用户点击「登录」
Then  系统返回登录态 token
And   页面跳转至首页
And   user.last_login_at 更新为当前时间
And   user_login_log 新增一条登录记录
```

#### Scenario: 验证码错误

```gherkin
Given 用户已注册且手机号 13800138000 状态正常
And   系统为该手机号生成的验证码为 123456
When  用户输入手机号 13800138000 和验证码 999999
And   用户点击「登录」
Then  返回 HTTP 401 + 错误码 INVALID_CREDENTIALS
And   前端提示"验证码错误或已过期"
And   不生成登录态 token
```

#### Scenario: 验证码 60 秒内重复获取

```gherkin
Given 用户已在 60 秒内获取过验证码
When  用户再次点击「获取验证码」
Then  后端拒绝发送
And   前端倒计时保持
And   前端提示"请 60 秒后再试"
```

### Requirement: REQ-002 账号密码登录

系统 MUST 提供 `POST /api/auth/login/password` 接口，供用户使用用户名/手机号和密码登录。系统 MUST 使用 bcrypt 校验密码（防时序攻击）。系统 MUST 在登录成功后生成登录态 token、清除失败计数、更新登录时间。系统 MUST 在账号不存在、密码错误、账号已注销/封禁时拒绝登录并返回对应错误码。系统 MUST 在连续 5 次密码错误后将账号锁定 30 分钟。

#### Scenario: 账号密码登录成功

```gherkin
Given 用户 swimmer01 的密码为 Abcd1234 且账号状态正常
When  用户输入用户名 swimmer01 和密码 Abcd1234
And   用户点击「登录」
Then  系统返回登录态 token
And   页面跳转至首页
And   user.failed_login_count 清零
And   user.last_login_at 更新为当前时间
```

#### Scenario: 密码错误

```gherkin
Given 用户 swimmer01 的密码为 Abcd1234
When  用户输入用户名 swimmer01 和密码 WrongPass
And   用户点击「登录」
Then  返回 HTTP 401 + 错误码 INVALID_CREDENTIALS
And   前端提示"手机号/用户名或密码错误"
And   user.failed_login_count 加 1
```

#### Scenario: 连续 5 次密码错误后账号锁定

```gherkin
Given 用户 swimmer01 已连续 4 次输入错误密码
When  用户第 5 次输入错误密码
Then  返回 HTTP 401 + 错误码 ACCOUNT_LOCKED
And   user.locked_until 设置为当前时间 + 30 分钟
And   前端提示"密码错误次数过多，请 30 分钟后重试或找回密码"
```

#### Scenario: 账号已注销

```gherkin
Given 用户已注销 swimmer02 的账号
When  用户输入用户名 swimmer02 和任意密码
Then  返回 HTTP 401 + 错误码 ACCOUNT_DEACTIVATED
And   前端提示"账号已注销，请重新注册"
```

### Requirement: REQ-003 登录验证码发送

系统 MUST 提供 `POST /api/auth/sms/code` 接口发送登录验证码。系统 MUST 校验手机号格式与存在性。系统 MUST 将验证码 6 位数字、TTL 5 分钟写入 `sms_code` 表。系统 MUST 对同一手机号 60 秒内只能发送 1 条验证码。

#### Scenario: 正常发送登录验证码

```gherkin
Given 用户手机号 13800138000 已注册且状态正常
When  用户调用 POST /api/auth/sms/code
Then  返回 HTTP 200
And   sms_code 表新增一条记录，code 为 6 位数字
And   expires_at 为当前时间 + 5 分钟
```
