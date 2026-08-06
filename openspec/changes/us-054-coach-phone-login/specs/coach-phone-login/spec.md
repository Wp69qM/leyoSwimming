> 本 spec 为 US-054 新增能力，全部使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 教练端手机号验证码登录入口

系统 MUST 在教练端登录页（US-051 C-微信授权页）提供「手机号登录」入口。系统 MUST 在教练点击入口后跳转至独立的手机号验证码登录页。系统 MUST 在手机号登录页顶部保留品牌 Logo 与 Slogan，中部提供手机号输入框、验证码输入框、获取验证码按钮，底部提供《用户须知》和《隐私协议》勾选区与登录按钮。

#### Scenario: 教练从微信授权页进入手机号登录页

```gherkin
Given 教练在未登录状态下打开教练端小程序
When  页面加载完成
Then  登录页展示「微信一键登录」主按钮
And   页面底部展示「手机号登录」入口
When  教练点击「手机号登录」
Then  页面跳转至教练端手机号验证码登录页
```

---

### Requirement: REQ-002 短信验证码发送

系统 MUST 提供 `POST /api/v1/auth/coach/sms/code` 接口用于发送教练端登录验证码。系统 MUST 校验手机号格式，格式无效时返回 `PHONE_FORMAT_ERROR`。系统 MUST 对同一手机号实施 60 秒发送间隔限制，60 秒内重复请求返回 `SMS_RATE_LIMITED` 并提示"请 60 秒后再试"。系统 MUST 生成 6 位数字验证码并存储于 `sms_code` 表，设置 5 分钟 TTL。系统 MUST 通过短信服务商将验证码发送至教练手机。

#### Scenario: 成功发送验证码

```gherkin
Given 教练输入格式正确的手机号 13800138000
When  教练点击「获取验证码」
Then  系统校验手机号格式通过
And   系统生成 6 位数字验证码
And   系统将验证码写入 sms_code 表并设置 5 分钟过期时间
And   系统调用短信服务发送验证码
And   前端按钮进入 60 秒倒计时
```

#### Scenario: 手机号格式错误

```gherkin
Given 教练输入手机号 1380013800
When  教练点击「获取验证码」
Then  系统返回错误码 PHONE_FORMAT_ERROR
And   前端提示"请输入正确的手机号"
And   不发送短信
```

#### Scenario: 60 秒内重复获取验证码

```gherkin
Given 教练刚刚获取过验证码
When  教练在 60 秒内再次点击「获取验证码」
Then  系统返回错误码 SMS_RATE_LIMITED
And   前端提示"请 60 秒后再试"
And   不重新生成验证码
```

---

### Requirement: REQ-003 手机号验证码登录与协议校验

系统 MUST 提供 `POST /api/v1/auth/coach/login/phone` 接口。系统 MUST 校验《用户须知》和《隐私协议》勾选状态，未勾选时返回 `TERMS_NOT_ACCEPTED`。系统 MUST 校验手机号格式与验证码，验证码错误或过期时返回 `INVALID_SMS_CODE`。系统 MUST 在校验通过后按手机号查询 `coach` 表。

#### Scenario: 未勾选协议被拦截

```gherkin
Given 教练已输入手机号 13800138000 和正确验证码 123456
When  教练未勾选《用户须知》或《隐私协议》
And   教练点击「登录」
Then  前端拦截登录请求并提示"请阅读并同意《用户须知》和《隐私协议》"
And   后端返回错误码 TERMS_NOT_ACCEPTED
And   不签发任何 token
And   不创建或修改 coach 记录
```

#### Scenario: 验证码错误

```gherkin
Given 教练输入手机号 13800138000
And   教练已勾选《用户须知》和《隐私协议》
When  教练输入错误验证码 000000
And   教练点击「登录」
Then  系统返回错误码 INVALID_SMS_CODE
And   前端提示"验证码错误或已过期"
And   不签发任何 token
```

---

### Requirement: REQ-004 已注册教练手机号登录成功

系统 MUST 在手机号和验证码均正确且教练已勾选协议时，按手机号查询 `coach` 表。若手机号存在且 `coach.status ≠ 3`，系统 MUST 复用该记录并更新 `last_login_at` 与 `login_ip`。系统 MUST 签发 JWT `access_token` + `refresh_token`，返回 `expires_in`、`is_new_coach=false`、`coach_status`。

#### Scenario: 已通过教练手机号登录成功

```gherkin
Given coach 表中存在手机号 13800138000 且 coach.status = 1
And   教练已勾选《用户须知》和《隐私协议》
When  教练输入手机号 13800138000 和正确验证码 123456
And   教练点击「登录」
Then  系统复用已有 coach 记录
And   系统更新 coach.last_login_at 为当前时间
And   系统返回 access_token、refresh_token、expires_in
And   is_new_coach = false
And   coach_status = 1
And   前端按 coach_status = 1 跳转到教练首页
```

#### Scenario: 待审核教练手机号登录成功

```gherkin
Given coach 表中存在手机号 13800138000 且 coach.status = 0
And   教练已勾选《用户须知》和《隐私协议》
When  教练输入手机号 13800138000 和正确验证码 123456
And   教练点击「登录」
Then  系统复用已有 coach 记录
And   is_new_coach = false
And   coach_status = 0
And   前端按 coach_status = 0 跳转到等待审核页
```

---

### Requirement: REQ-005 未注册手机号首次登录自动注册

系统 MUST 在手机号不存在于 `coach` 表时，自动创建新教练记录，`coach.status = -1`（未提交入驻资料），`coach.phone` 为输入的手机号。系统 MUST 签发 JWT，返回 `is_new_coach=true`、`coach_status=-1`，前端按 coach_status = -1 跳转至入驻资料页（US-010）。

#### Scenario: 未注册手机号首次登录

```gherkin
Given coach 表中不存在手机号 13800138000
And   教练已勾选《用户须知》和《隐私协议》
When  教练输入手机号 13800138000 和正确验证码 123456
And   教练点击「登录」
Then  系统在 coach 表中创建新记录
And   coach.status = -1
And   coach.phone = 13800138000
And   系统返回 access_token、refresh_token
And   is_new_coach = true
And   coach_status = -1
And   前端按 coach_status = -1 跳转到入驻资料页（US-010）
```

---

### Requirement: REQ-006 已离职手机号重新注册登录

系统 MUST 在手机号存在但 `coach.status = 3`（已离职）时，按 PRD §5.2.1 第 4 条重新创建新教练账号，新记录 `coach.status = -1`，新记录 `coach_id` 与原记录不同，原账号数据不绑定。系统 MUST 签发 JWT，返回 `is_new_coach=true`、`coach_status=-1`。

#### Scenario: 已离职手机号重新注册

```gherkin
Given coach 表中存在手机号 13800138000 且 coach.status = 3
And   教练已勾选《用户须知》和《隐私协议》
When  教练输入手机号 13800138000 和正确验证码 123456
And   教练点击「登录」
Then  系统重新创建新 coach 记录
And   新记录 coach.status = -1
And   新记录 coach_id 与原离职账号不同
And   原离职账号数据不被新账号访问或绑定
And   is_new_coach = true
And   coach_status = -1
And   前端按 coach_status = -1 跳转到入驻资料页（US-010）
```

---

### Requirement: REQ-007 登录态维持与 token 刷新

系统 MUST 在登录成功后返回 `access_token`（有效期 2 小时）与 `refresh_token`（有效期 7 天）。前端 MUST 将 token 存储于本地，并在访问受保护接口时于 `Authorization: Bearer {access_token}` 中携带。`access_token` 过期但 `refresh_token` 有效时，前端 MUST 调用刷新接口换发新的 `access_token`；`refresh_token` 过期或不存在时，前端 MUST 引导教练重新登录。

#### Scenario: 登录成功后维持登录态

```gherkin
Given 教练已完成手机号验证码登录
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

### Requirement: REQ-008 安全与审计

系统 MUST 对 `coach.phone` 进行加密存储。系统 MUST 将验证码以安全方式存储于 `sms_code` 表并设置 TTL，使用后立即失效或标记为已使用。系统 MUST 记录登录日志至 `coach_login_log` 表，包括登录时间、IP、设备、登录方式（phone）。系统 MUST 在日志中对手机号进行脱敏展示。

#### Scenario: 登录信息被正确记录

```gherkin
Given 教练使用手机号 13800138000 成功登录
When  系统处理登录请求
Then  系统在 coach_login_log 表中新增一条记录
And   记录 login_method = "phone"
And   记录 coach_id 与登录教练一致
And   日志中手机号显示为 138****8000
```
