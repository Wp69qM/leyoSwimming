# Spec Delta: user-complete-profile

> 本 spec 为 US-005 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 用户补充注册资料

系统 MUST 提供 `PUT /api/user/profile` 接口，供已登录用户补充手机号、用户名、密码、邮箱等注册资料。系统 MUST 校验手机号格式（11 位中国大陆手机号）、手机号唯一性、用户名唯一性、密码强度（8-20 位同时包含字母与数字）。系统 MUST 在资料保存成功后将用户 `identity` 从 `0（游客）` 置为 `1（注册用户）`，并记录 `user_identity_log`。系统 MUST 以 `Idempotency-Key` 为幂等键，重复提交返回与首次一致的结果，不创建重复用户。系统 MUST NOT 在未填写手机号时允许提交。

#### Scenario: 正常补充注册资料

```gherkin
Given 用户已完成微信授权登录且资料缺失
And   系统未存在手机号 13800138000 的注册用户
When  用户填写手机号 13800138000、用户名 swimmer01、密码 Abcd1234、邮箱 a@b.com
And   用户点击「完成注册」
Then  系统保存成功
And   user.identity = 1（注册用户）
And   user.status = 1（正常）
And   页面跳转至首页，顶部显示用户名 swimmer01
And   user_identity_log 新增一条游客 → 注册用户的记录
```

#### Scenario: 手机号已被注册

```gherkin
Given 系统中已存在手机号 13800138000 的注册用户
When  用户填写手机号 13800138000 并提交
Then  返回 HTTP 400 + 错误码 PHONE_ALREADY_BOUND
And   前端提示"该手机号已绑定其他账号，请更换或登录"
And   用户资料未被保存
And   user.identity 保持 0（游客）
```

#### Scenario: 必填项缺失

```gherkin
Given 用户已完成微信授权登录
When  用户未填写手机号直接点击「完成注册」
Then  前端阻止提交
And   手机号输入框下方提示"手机号不能为空"
And   后端未收到请求
```

#### Scenario: 用户名已被占用

```gherkin
Given 系统中已存在用户名 swimmer01
When  用户填写用户名 swimmer01 并提交
Then  返回 HTTP 400 + 错误码 USERNAME_TAKEN
And   前端提示"用户名已被占用"
And   user.username 未被保存
```

#### Scenario: 密码强度不足

```gherkin
Given 用户已完成微信授权登录
When  用户填写密码 123456 并提交
Then  返回 HTTP 400 + 错误码 WEAK_PASSWORD
And   前端提示"密码需包含 8-20 位字母与数字"
And   user.password_hash 未被保存
```

#### Scenario: 重复提交幂等

```gherkin
Given 用户已提交补充资料请求并收到成功响应
When  用户在 5 分钟内使用相同 Idempotency-Key 再次提交
Then  系统返回与首次相同的结果
And   不创建新的 user 记录
And   不重复记录 user_identity_log
```

### Requirement: REQ-002 手机号存在性查询

系统 MUST 提供 `GET /api/user/phone/exists` 接口，供已登录用户查询指定手机号是否已注册。系统 MUST 返回明确的布尔结果与 HTTP 200。系统 MUST 对查询接口做限流，防止被用于撞库。

#### Scenario: 查询未注册手机号

```gherkin
Given 系统未存在手机号 13900139000 的注册用户
When  用户调用 GET /api/user/phone/exists?phone=13900139000
Then  返回 HTTP 200
And   响应体 data.exists = false
```

#### Scenario: 查询已注册手机号

```gherkin
Given 系统已存在手机号 13800138000 的注册用户
When  用户调用 GET /api/user/phone/exists?phone=13800138000
Then  返回 HTTP 200
And   响应体 data.exists = true
```
