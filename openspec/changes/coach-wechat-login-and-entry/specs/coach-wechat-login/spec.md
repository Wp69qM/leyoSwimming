> 本 spec 为 US-051 新增能力，使用 ADDED delta 标记；同时修改 US-004 的 `wechat-auth` 能力，使用 MODIFIED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 教练端微信授权登录与状态分流

系统 MUST 在教练端小程序提供微信授权登录入口。系统 MUST 改造 `POST /api/v1/auth/wechat-login` 接口以接收 `app_type=coach` 参数，并在登录成功后按 `coach.status` 返回 `redirect_page`，引导教练跳转至正确页面。系统 MUST 在用户表无记录时创建 `user` 记录（`identity_status='注册用户'`、`profile_completed=false`），但 MUST NOT 创建 `coach` 记录。系统 MUST 复用同一 `union_id` 在用户端已注册的 `user` 记录。系统 MUST 以 `code` 为幂等键，5 分钟内重复提交返回首次结果。系统 MUST NOT 将 `session_key` 返回给前端。系统 MUST 在教练端登录成功后校验隐私协议同意状态，未同意时先跳转 US-009 隐私协议页；本 Requirement 所有正常路径场景默认教练已同意当前隐私协议，若未同意则登录成功后先跳转 US-009 隐私协议页，同意后再按 `redirect_page` 跳转。

#### Scenario: 未入驻教练首次授权登录，跳转入驻资料页

```gherkin
Given 教练首次使用教练端小程序，user 表和 coach 表均不存在其 union_id
And   教练已同意当前隐私协议
When  教练点击"微信一键登录"按钮并同意授权
Then  系统创建新用户记录，identity_status = "注册用户"
And   profile_completed = false
And   系统未创建 coach 记录，coach_status = null
And   返回 access_token 和 refresh_token
And   前端跳转"入驻资料页"（US-010）
```

#### Scenario: 已入驻通过教练授权登录，跳转教练首页

```gherkin
Given 教练已存在 user 记录和 coach 记录，coach.status = 1（已通过）
And   教练已同意当前隐私协议
When  教练点击"微信一键登录"按钮并同意授权
Then  系统复用已有用户记录，不新建账号
And   返回 access_token 和 refresh_token
And   is_new_user = false
And   coach_status = 1
And   前端跳转"教练首页"
```

#### Scenario: 待审核教练授权登录，跳转等待审核页

```gherkin
Given 教练已提交入驻资料，coach.status = 0（待审核）
And   教练已同意当前隐私协议
When  教练点击"微信一键登录"按钮并同意授权
Then  系统复用已有用户和 coach 记录
And   coach_status = 0
And   返回 access_token 和 refresh_token
And   前端跳转"等待审核页"
```

#### Scenario: 教练拒绝微信授权

```gherkin
Given 教练在微信授权弹窗中点击"拒绝"
When  系统尝试调用 wx.login() 或获取用户信息
Then  登录流程终止
And   停留在登录页并展示文案"需要微信授权才能登录"
And   不创建任何用户记录
And   不签发 token
```

#### Scenario: 同一微信账号已在用户端注册，首次进入教练端

```gherkin
Given 用户已在用户端完成微信授权登录，user 表存在其 union_id，但 coach 表不存在
And   教练已同意当前隐私协议
When  教练在教练端点击"微信一键登录"按钮并同意授权
Then  系统复用已有 user 记录，不新建账号
And   返回 access_token 和 refresh_token
And   coach_status = null
And   前端跳转"入驻资料页"（US-010）
```

#### Scenario: 已驳回教练授权登录，跳转重新提交入驻页

```gherkin
Given 教练存在 coach 记录，coach.status = 2（已驳回），rejection_reason 不为空
And   教练已同意当前隐私协议
When  教练点击"微信一键登录"按钮并同意授权
Then  系统复用已有 user 和 coach 记录
And   coach_status = 2
And   前端跳转"重新提交入驻页"（US-040）
And   页面显示驳回原因
```

#### Scenario: 教练未同意隐私协议，登录后先跳转隐私协议页

```gherkin
Given 教练已完成微信授权登录，但未同意当前隐私协议
When  系统校验隐私协议同意状态
Then  前端跳转"隐私协议页"（US-009）
And   教练同意隐私协议后，再按 redirect_page 进入目标页面
```

---

### Requirement: REQ-002 登录态下查询教练入驻状态

系统 MUST 提供 `GET /api/v1/coach/me/status` 接口，供已登录的教练端小程序查询当前入驻状态。系统 MUST 在 coach 记录不存在时返回 `coach_status=null` 与 `redirect_page=coach_onboarding`；在 coach 记录存在时返回当前 `coach.status`、`rejection_reason`（如适用）与对应的 `redirect_page`。

#### Scenario: 已通过教练查询状态

```gherkin
Given 教练已登录且 coach.status = 1
When  教练端调用 GET /api/v1/coach/me/status
Then  返回 coach_status = 1
And   redirect_page = "coach_home"
```

#### Scenario: 无 coach 记录时查询状态

```gherkin
Given 教练已登录但 coach 表不存在其记录
When  教练端调用 GET /api/v1/coach/me/status
Then  返回 coach_status = null
And   redirect_page = "coach_onboarding"
```

---

## MODIFIED Requirements

### Requirement: REQ-003 扩展微信授权登录接口以支持教练端

系统 MUST 在 `POST /api/v1/auth/wechat-login` 请求中支持可选参数 `app_type`，枚举值为 `user`（默认）或 `coach`。当 `app_type=coach` 时，系统 MUST 在登录流程中额外查询 `coach` 表，并在响应中返回 `coach_status` 与 `redirect_page` 字段。当 `app_type=user` 或缺失时，系统 MUST 保持 US-004 原有行为不变（不返回 `coach_status` 与 `redirect_page`）。系统 MUST 校验 `app_type` 值，非法值返回 `VALIDATION_ERROR`。

#### Scenario: 用户端登录保持原行为

```gherkin
Given 用户调用 POST /api/v1/auth/wechat-login，未传 app_type 或 app_type=user
When  系统处理登录请求
Then  响应格式与 US-004 一致
And   响应中不包含 coach_status 与 redirect_page 字段
```

#### Scenario: 教练端登录返回新增字段

```gherkin
Given 教练调用 POST /api/v1/auth/wechat-login，app_type=coach
When  系统处理登录请求
Then  响应包含 access_token、refresh_token、is_new_user、profile_completed
And   响应包含 coach_status 与 redirect_page
```

#### Scenario: 非法 app_type 校验

```gherkin
Given 调用 POST /api/v1/auth/wechat-login，app_type=admin
When  系统校验请求参数
Then  返回 HTTP 400 + 错误码 "VALIDATION_ERROR"
And   不调用微信 code2session 接口
```
