# Spec Delta: user-account-security

> 本 spec 为 US-008 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 换绑手机号

系统 MUST 提供 `PUT /api/user/security/phone` 接口，供已登录用户更换绑定手机号。系统 MUST 校验新手机号格式、新手机号未被其他账号绑定、新手机号验证码正确。系统 MUST 要求二次验证（原手机号短信验证码或当前密码）。系统 MUST 限制 24 小时内只能换绑 1 次手机号。系统 MUST 在换绑成功后更新 `user.phone`、`phone_changed_at`，并发送换绑通知。

#### Scenario: 正常换绑手机号

```gherkin
Given 用户已登录且当前手机号为 13800138000
And   新手机号 13900139000 未注册
And   用户 24 小时内未换绑过手机号
When  用户输入新手机号及新手机号验证码
And   用户通过原手机号短信验证码（或当前密码）完成二次验证
Then  user.phone 更新为 13900139000
And   user.phone_changed_at 更新为当前时间
And   旧手机号 13800138000 可重新注册
And   系统发送换绑通知短信
```

#### Scenario: 新手机号已被绑定

```gherkin
Given 用户已登录
And   手机号 13900139000 已绑定其他账号
When  用户尝试换绑到 13900139000
Then  返回 HTTP 400 + 错误码 PHONE_ALREADY_BOUND
And   前端提示"该手机号已被其他账号绑定"
And   user.phone 保持不变
```

#### Scenario: 24 小时内频繁换绑

```gherkin
Given 用户已在 24 小时内换绑过手机号
When  用户再次尝试换绑手机号
Then  返回 HTTP 429 + 错误码 PHONE_CHANGE_LIMIT
And   前端提示"手机号换绑过于频繁，请 24 小时后再试"
```

### Requirement: REQ-002 修改密码

系统 MUST 提供 `PUT /api/user/security/password` 接口，供已登录用户修改密码。系统 MUST 校验原密码正确、新密码符合强度要求（8-20 位字母+数字）、新密码不能与旧密码相同。系统 MUST 在修改成功后更新 `password_hash`、可选清除其他设备登录态、发送密码修改通知。

#### Scenario: 正常修改密码

```gherkin
Given 用户已登录且原密码为 Abcd1234
When  用户输入原密码 Abcd1234 和新密码 NewPass123
And   用户点击「确认修改」
Then  password_hash 更新
And   所有登录态保留（可选）或仅当前设备保留
And   系统发送密码修改通知
```

#### Scenario: 原密码错误

```gherkin
Given 用户已登录且原密码为 Abcd1234
When  用户输入原密码 WrongPass 和新密码 NewPass123
And   用户点击「确认修改」
Then  返回 HTTP 400 + 错误码 INVALID_OLD_PASSWORD
And   前端提示"原密码错误"
And   password_hash 保持不变
```

#### Scenario: 新密码与旧密码相同

```gherkin
Given 用户已登录且原密码为 Abcd1234
When  用户输入原密码 Abcd1234 和新密码 Abcd1234
Then  返回 HTTP 400 + 错误码 NEW_PASSWORD_SAME_AS_OLD
And   前端提示"新密码不能与旧密码相同"
And   password_hash 保持不变
```

### Requirement: REQ-003 登录设备管理

系统 MUST 提供 `GET /api/user/security/devices` 接口返回用户最近 30 天登录设备列表。系统 MUST 提供 `DELETE /api/user/security/devices/{id}` 接口供用户下线指定设备。系统 MUST 在下线设备后立即使对应 token 失效。系统 MUST 记录设备下线审计日志。

#### Scenario: 下线登录设备

```gherkin
Given 用户已登录且存在 2 个登录设备
When  用户在设备列表中点击「下线」其中一个设备
Then  被下线设备的 token 立即失效
And   设备列表刷新，显示 1 个在线设备
And   被下线设备再次访问时返回 401
And   audit_log 新增设备下线记录
```
