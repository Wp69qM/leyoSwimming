> 本 spec 为 US-051 新增能力，使用 ADDED delta 标记；同时修改 US-004 的 `wechat-auth` 能力，使用 MODIFIED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 教练端微信授权登录与状态分流

系统 MUST 在教练端小程序提供微信授权登录入口。系统 MUST 改造 `POST /api/v1/auth/wechat-login` 接口以接收 `app_type=coach` 参数，并在登录成功后返回 `coach.status`，由前端映射跳转至正确页面。系统 MUST 在 `coach` 表无记录时直接创建 `coach` 记录（`status=-1` 未提交入驻资料，`phone` 为解密后的微信手机号，`nickname` 来自微信授权），并设置 `is_new_coach=true`。系统 MUST 在 `coach` 表已存在记录且 `status != 3` 时复用该记录，设置 `is_new_coach=false`。系统 MUST 在 `coach.status=3`（已离职）时视为未命中，新建 coach 记录且不绑定旧数据。系统 MUST NOT 在 `app_type=coach` 时创建、查询或复用 `user` 表记录。系统 MUST 以 `code` 为幂等键，5 分钟内重复提交返回首次结果。系统 MUST NOT 将 `session_key` 返回给前端。系统 MUST 校验《用户须知》和《隐私协议》勾选，未勾选时返回 `TERMS_NOT_ACCEPTED`。

#### Scenario: 未入驻教练首次授权登录，跳转入驻资料页

```gherkin
Given 教练首次使用教练端小程序，coach 表中不存在其 union_id
And   教练已勾选《用户须知》和《隐私协议》
And   教练已同意授权小程序获取手机号
When  教练点击"微信一键登录"按钮并同意授权
Then  系统创建新 coach 记录，coach.status = -1（未提交入驻资料）
And   coach.phone 等于解密后的微信手机号
And   返回 access_token 和 refresh_token
And   is_new_coach = true
And   coach_status = -1
And   前端按 coach_status=-1 跳转到"入驻资料页"（US-010）
And   user 表无新增记录
```

#### Scenario: 已入驻通过教练授权登录，跳转教练首页

```gherkin
Given 教练已存在 coach 记录，coach.status = 1（已通过）
And   教练已勾选《用户须知》和《隐私协议》
And   教练已同意授权小程序获取手机号
When  教练点击"微信一键登录"按钮并同意授权
Then  系统复用已有 coach 记录，不新建账号
And   返回 access_token 和 refresh_token
And   is_new_coach = false
And   coach_status = 1
And   前端按 coach_status=1 跳转到"教练首页"
And   user 表无新增记录
```

#### Scenario: 待审核教练授权登录，跳转等待审核页

```gherkin
Given 教练已提交入驻资料，coach.status = 0（待审核）
And   教练已勾选《用户须知》和《隐私协议》
And   教练已同意授权小程序获取手机号
When  教练点击"微信一键登录"按钮并同意授权
Then  系统复用已有 coach 记录
And   is_new_coach = false
And   coach_status = 0
And   返回 access_token 和 refresh_token
And   前端按 coach_status=0 跳转到"等待审核页"
```

#### Scenario: 申请离职中教练授权登录，跳转离职处理中页

```gherkin
Given 教练存在 coach 记录，coach.status = 4（申请离职中）
And   存在 status = processing 的离职工单
And   教练已勾选《用户须知》和《隐私协议》
And   教练已同意授权小程序获取手机号
When  教练点击"微信一键登录"按钮并同意授权
Then  系统复用已有 coach 记录
And   is_new_coach = false
And   coach_status = 4
And   返回 access_token 和 refresh_token
And   前端按 coach_status=4 跳转到"离职处理中页"（US-039）
```

#### Scenario: 教练拒绝微信授权或手机号授权

```gherkin
Given 教练已勾选《用户须知》和《隐私协议》
And   教练在微信授权弹窗或手机号授权弹窗中点击"拒绝"
When  系统尝试调用 wx.login() 或 wx.getPhoneNumber()
Then  登录流程终止
And   停留在登录页并展示文案"需要微信授权和手机号授权才能登录"
And   不创建任何 coach 记录
And   不签发 token
```

#### Scenario: 已离职教练重新登录，新建账号

```gherkin
Given 教练存在已离职 coach 记录，coach.status = 3（已离职）
And   教练已勾选《用户须知》和《隐私协议》
And   教练已同意授权小程序获取手机号
When  教练点击"微信一键登录"按钮并同意授权
Then  系统视为未命中，新建 coach 记录，coach.status = -1
And   is_new_coach = true
And   coach_status = -1
And   旧 coach 记录数据不关联到新记录
And   返回 access_token 和 refresh_token
```

#### Scenario: 已驳回教练授权登录，跳转重新提交入驻页

```gherkin
Given 教练存在 coach 记录，coach.status = 2（已驳回），rejection_reason 不为空
And   教练已勾选《用户须知》和《隐私协议》
And   教练已同意授权小程序获取手机号
When  教练点击"微信一键登录"按钮并同意授权
Then  系统复用已有 coach 记录
And   is_new_coach = false
And   coach_status = 2
And   返回 access_token 和 refresh_token
And   前端按 coach_status=2 跳转到"重新提交入驻页"（US-040）
And   页面显示驳回原因
```

#### Scenario: 教练未勾选《用户须知》或《隐私协议》

```gherkin
Given 教练未勾选《用户须知》或《隐私协议》
When  教练点击"微信一键登录"按钮
Then  前端拦截登录请求并展示文案"请阅读并同意《用户须知》和《隐私协议》"
And   不调用 wx.login() 或 wx.getPhoneNumber()
And   不发起后端登录请求
And   若教练绕过前端直接调用后端接口，后端返回错误码 TERMS_NOT_ACCEPTED
And   不创建任何 coach 记录
And   不签发 token
```

#### Scenario: 登录成功后维持登录态并刷新 token

```gherkin
Given 教练已完成教练端微信授权登录
And   后端返回 access_token、refresh_token 和 expires_in = 7200
When  前端收到登录响应
Then  前端将 access_token 和 refresh_token 写入本地存储
And   前端记录 access_token 过期时间
When  教练访问受登录态保护的接口
Then  前端在 Authorization Header 中携带 Bearer {access_token}
And   后端校验 token 有效后返回教练数据
When  access_token 过期但 refresh_token 未过期
Then  前端调用刷新接口换取新的 access_token
And   后续请求使用新的 access_token
When  本地 token 不存在或 refresh_token 已过期
Then  前端引导教练重新进入登录页
```

---

### Requirement: REQ-002 登录态下查询教练入驻状态

系统 MUST 提供 `GET /api/v1/coach/me/status` 接口，供已登录的教练端小程序查询当前入驻状态。系统 MUST 在 coach 记录不存在时返回 `coach_status=-1`；在 coach 记录存在时返回当前 `coach.status` 与 `rejection_reason`（如适用）。系统 MUST NOT 在响应中包含 `user` 表相关字段。

#### Scenario: 已通过教练查询状态

```gherkin
Given 教练已登录且 coach.status = 1
When  教练端调用 GET /api/v1/coach/me/status
Then  返回 coach_status = 1
And   rejection_reason = null
```

#### Scenario: 无 coach 记录时查询状态

```gherkin
Given 教练已登录但 coach 表不存在其记录
When  教练端调用 GET /api/v1/coach/me/status
Then  返回 coach_status = -1
```

#### Scenario: 已驳回教练查询状态

```gherkin
Given 教练已登录且 coach.status = 2，rejection_reason = "资质照片不清晰"
When  教练端调用 GET /api/v1/coach/me/status
Then  返回 coach_status = 2
And   rejection_reason = "资质照片不清晰"
```

---

## MODIFIED Requirements

### Requirement: REQ-003 扩展微信授权登录接口以支持教练端

系统 MUST 在 `POST /api/v1/auth/wechat-login` 请求中支持参数 `app_type`，枚举值为 `user` 或 `coach`。当 `app_type=coach` 时，系统 MUST 在登录流程中直接查询/写入 `coach` 表，并在响应中返回 `is_new_coach`、`coach_status` 字段；系统 MUST NOT 返回 `is_new_user` 或 `profile_completed`。当 `app_type=user` 时，系统 MUST 保持 US-004 原有行为不变（不返回 `is_new_coach`/`coach_status`）。系统 MUST 校验 `app_type` 值，非法值返回 `VALIDATION_ERROR`。

#### Scenario: 用户端登录保持原行为

```gherkin
Given 用户调用 POST /api/v1/auth/wechat-login，app_type=user
When  系统处理登录请求
Then  响应格式与 US-004 一致
And   响应中不包含 coach_status、is_new_coach 字段
```

#### Scenario: 教练端登录返回新增字段

```gherkin
Given 教练调用 POST /api/v1/auth/wechat-login，app_type=coach
When  系统处理登录请求
Then  响应包含 access_token、refresh_token、expires_in
And   响应包含 is_new_coach、coach_status
And   响应不包含 is_new_user、profile_completed
```

#### Scenario: 非法 app_type 校验

```gherkin
Given 调用 POST /api/v1/auth/wechat-login，app_type=admin
When  系统校验请求参数
Then  返回 HTTP 400 + 错误码 "VALIDATION_ERROR"
And   不调用微信 code2session 接口
```
