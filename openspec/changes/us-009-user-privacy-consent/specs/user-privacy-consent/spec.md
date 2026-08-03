# Spec Delta: user-privacy-consent

> 本 spec 为 US-009 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 隐私协议同意

系统 MUST 提供 `POST /api/user/privacy/consent` 接口，供已登录用户/教练提交隐私协议同意；游客调用 MUST 返回 `401 UNAUTHORIZED`。系统 MUST 记录 `user_privacy_consent.version`、`agreed_at`、`status='agreed'`。系统 MUST 比对用户/教练最近一次同意的协议版本号与当前生效版本号；若版本号发生变化，触发重新授权流程。系统 MUST 在首次注册/首次登录教练端/协议版本更新后强制用户/教练同意，否则阻止进入首页或教练端目标页面。系统 MUST 对同一版本的重复同意做幂等处理。本 Requirement 适用于用户端与教练端；教练端首次登录后需先完成本 US 授权，再进入 US-051 的 `redirect_page` 目标页面。

#### Scenario: 正常同意隐私协议

```gherkin
Given 用户已完成资料补充
And   当前隐私协议版本为 v2.0
When  用户阅读并勾选「我已阅读并同意《隐私协议》」
And   用户点击「同意」
Then  系统记录 user_privacy_consent.version = 'v2.0'
And   user_privacy_consent.agreed_at 为当前时间
And   user_privacy_consent.status = 'agreed'
And   用户可继续进入首页
```

#### Scenario: 教练端首次登录同意隐私协议

```gherkin
Given 教练已完成 US-051 微信授权登录且未同意过隐私协议
And   当前隐私协议版本为 v2.0
When  教练阅读并勾选「我已阅读并同意《隐私协议》」
And   教练点击「同意」
Then  系统记录 user_privacy_consent.version = 'v2.0'
And   user_privacy_consent.agreed_at 为当前时间
And   user_privacy_consent.status = 'agreed'
And   教练按 US-051 的 redirect_page 进入目标页面
```

#### Scenario: 不同意隐私协议

```gherkin
Given 用户首次注册进入隐私协议页
When  用户点击「不同意」
Then  前端提示"需同意隐私协议后方可继续使用"
And   用户无法进入首页
And   user_privacy_consent 无记录
```

#### Scenario: 重复同意同一版本

```gherkin
Given 用户已同意隐私协议 v2.0
When  用户再次点击「同意」v2.0
Then  系统幂等处理，不新增重复记录
And   返回已同意状态
```

### Requirement: REQ-002 隐私协议撤回

系统 MUST 提供 `POST /api/user/privacy/consent` 接口（action=revoke），供已登录且已同意用户撤回隐私授权；游客调用 MUST 返回 `401 UNAUTHORIZED`。系统 MUST 更新 `user_privacy_consent.status='revoked'` 并记录 `revoked_at`。系统 MUST 停止非必要的数据收集。系统 MUST 阻止用户访问需授权的功能（如购买套餐）。

#### Scenario: 撤回隐私授权

```gherkin
Given 用户已同意隐私协议 v2.0
When  用户在隐私设置页点击「撤回授权」
And   用户确认撤回
Then  user_privacy_consent.status = 'revoked'
And   user_privacy_consent.revoked_at 为当前时间
And   系统停止非必要的数据收集
And   前端提示"已撤回授权，部分功能可能受限"
```

#### Scenario: 撤回授权后访问需授权功能

```gherkin
Given 用户已撤回隐私授权
When  用户尝试购买套餐
Then  系统阻止购买流程
And   前端提示"您已撤回隐私授权，无法使用购买功能，请重新同意"
```

### Requirement: REQ-003 隐私协议版本管理与查询

系统 MUST 提供 `GET /api/privacy-policy/current` 接口返回当前生效的隐私协议版本与内容。系统 MUST 提供 `GET /api/user/privacy/status` 接口返回当前用户授权状态与版本。系统 MUST 在协议版本更新后要求用户重新同意。

#### Scenario: 协议版本更新后未重新同意

```gherkin
Given 系统更新隐私协议至 v3.0
And   用户仅同意过 v2.0
When  用户下次进入需授权场景
Then  系统强制重新展示隐私协议
And   前端提示"隐私协议已更新，请重新阅读并同意"
And   用户同意 v3.0 后方可继续使用
```
