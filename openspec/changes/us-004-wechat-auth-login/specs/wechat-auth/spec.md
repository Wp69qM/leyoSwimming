# Spec Delta: wechat-auth

> 本 spec 为 US-004 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 微信授权登录

系统 MUST 提供 `POST /api/v1/auth/wechat-login` 接口，接收微信小程序 `wx.login()` 返回的 `code`，调用微信 `code2session` 接口获取 `openid`/`union_id`/`session_key`，按 `union_id` 查询或创建用户记录，并签发 JWT `access_token` 与 `refresh_token`。系统 MUST 在首次登录时将用户 `identity_status` 置为 `注册用户`（触发游客→注册用户状态转换），`profile_completed` 置为 `false`。系统 MUST 以 `code` 为幂等键，5 分钟内重复提交返回首次结果。系统 MUST NOT 将 `session_key` 返回给前端。

#### Scenario: 首次微信授权登录成功，跳转补充资料页

```gherkin
Given 用户首次使用微信授权登录，user 表中不存在其 union_id
When  用户点击"微信一键登录"按钮并同意授权
Then  系统创建新用户记录，identity_status = "注册用户"
And   profile_completed = false
And   返回 access_token 和 refresh_token
And   前端跳转到"补充资料"页（US-005）
And   user.union_id 等于微信返回的 union_id
```

#### Scenario: 已注册用户微信授权登录成功，跳转首页

```gherkin
Given 用户已注册且 profile_completed = true，user 表中存在其 union_id 且 status=0
When  用户点击"微信一键登录"按钮并同意授权
Then  系统复用已有用户记录，不新建账号
And   返回 access_token 和 refresh_token
And   is_new_user = false
And   前端跳转到小程序首页
```

#### Scenario: 登录凭证已失效

```gherkin
Given 用户提交的 code 已被使用或已超过 5 分钟有效期
When  后端调用 code2session 接口
Then  微信返回 errcode = 40029（invalid code）
And   后端返回 HTTP 401 + 错误码 "WECHAT_CODE_INVALID"
And   前端展示文案"登录凭证已失效，请重新点击登录"
And   不创建任何用户记录
```

#### Scenario: 微信接口调用失败

```gherkin
Given 后端调用微信 code2session 接口时返回错误码或超时（> 3s）
When  后端处理登录请求
Then  返回 HTTP 502 + 错误码 "WECHAT_API_ERROR"（或超时返回 504 + "WECHAT_API_TIMEOUT"）
And   前端展示文案"微信服务暂时不可用，请稍后重试"
And   不创建任何用户记录
And   不签发 token
```

#### Scenario: 相同 code 重复提交（幂等）

```gherkin
Given 用户在 5 分钟内使用相同 code 重复提交登录请求
When  后端处理重复请求
Then  返回首次登录的完整结果（相同 user_id、相同 token）
And   不创建新的用户记录
And   不重复调用微信 code2session 接口
```

---

### Requirement: REQ-002 用户身份状态转换（游客→注册用户）

系统 MUST 在首次微信授权登录成功时触发用户身份状态机转换：将新创建用户的 `identity_status` 字段从隐式的 `游客` 状态转换为显式的 `注册用户` 状态。系统 MUST NOT 在后续登录中修改已有用户的 `identity_status` 字段（老用户复用账号时保持原状态）。系统 MUST 在 `union_id` 命中已注销账号（`status=1`）时新建账号，新账号 `identity_status = '注册用户'`，且 MUST NOT 绑定原账号数据（符合 PRD [§5.2.1 第 4 条](../../../../../docs/prd/prd.md)）。

#### Scenario: 首次登录触发游客→注册用户状态转换

```gherkin
Given 用户首次使用微信授权登录，user 表中不存在其 union_id
When  系统创建新用户记录
Then  user.identity_status = "注册用户"
And   user.profile_completed = false
And   user.status = 0
And   用户身份状态机完成 游客 → 注册用户 转换
```

#### Scenario: 老用户登录不修改 identity_status

```gherkin
Given 用户已注册且 identity_status = "注册用户"（或更高：学员），status=0
When  用户再次微信授权登录
Then  系统复用已有用户记录
And   user.identity_status 保持不变
And   不触发任何状态机转换
```

#### Scenario: union_id 命中已注销账号时新建账号

```gherkin
Given 某 union_id 已绑定一个 status=1 的已注销账号
When  用户使用该 union_id 的微信账号再次登录
Then  系统新建用户记录，identity_status = "注册用户"
And   新账号与原账号数据完全隔离（不继承任何数据）
And   原账号保持 status=1 不变
```

#### Scenario: user 表唯一约束

```gherkin
Given user 表已存在 union_id='union_xxx' 且 status=0 的记录
When  系统尝试再次插入相同 union_id 且 status=0 的记录
Then  数据库拒绝插入（唯一索引冲突）
And   系统返回已有用户记录（复用而非新建）
```
