> **OpenSpec Spec | 映射自 `docs/stories/US-009-用户-隐私协议与用户须知授权/user-story.md` §6**

## Capability

用户/教练隐私协议与用户须知授权

## ADDED Requirements

### Requirement: REQ-001 用户须知与隐私协议同意

系统 MUST 在用户端/教练端登录页提供「我已阅读并同意《用户须知》和《隐私协议》」勾选区。系统 MUST 在用户/教练点击勾选框或「我已阅读并同意」这几个字（不含协议名）时，仅切换勾选框状态，不唤起协议浮层弹窗。系统 MUST 在用户/教练点击「《用户须知》」或「《隐私协议》」协议名时，唤起协议浮层弹窗，并在其中通过两个 Tab 分别展示《用户须知》与《隐私协议》内容。系统 MUST 在浮层底部提供「同意」和「不同意」按钮；点击「同意」后浮层关闭且登录页勾选框变为已勾选；点击「不同意」后浮层关闭且勾选框变为未勾选。系统 MUST 在用户/教练点击登录按钮时校验勾选框已勾选，否则阻止登录。系统 MUST 在登录成功后分别记录 `user_terms_consent` 与 `user_privacy_consent` 的版本号与同意时间。本 Requirement 适用于用户端（US-004 / US-006）与教练端（US-051）。

#### Scenario: 登录页点击协议名并同意后成功登录

```gherkin
Given 用户进入用户端登录页
And   当前《用户须知》版本为 v2.0，《隐私协议》版本为 v2.0
And   登录页勾选框当前为未勾选
When  用户点击「《用户须知》」协议名
Then  协议浮层弹窗展示《用户须知》Tab
When  用户切换到《隐私协议》Tab 阅读
And   用户点击「同意」按钮
Then  浮层关闭
And   登录页勾选框状态变为已勾选
And   用户完成登录后，系统记录 user_terms_consent.version = 'v2.0'
And   系统记录 user_privacy_consent.version = 'v2.0'
And   两条记录的 agreed_at 均为当前时间
```

#### Scenario: 登录页不同意协议导致无法登录

```gherkin
Given 用户进入用户端登录页
And   协议浮层处于打开状态
When  用户点击「不同意」按钮
Then  浮层关闭
And   登录页勾选框状态为未勾选
And   用户点击登录按钮时系统阻止登录
And   前端提示「请阅读并同意《用户须知》和《隐私协议》」
```

#### Scenario: 教练端登录页点击协议名并同意后进入教练端

```gherkin
Given 教练进入教练端登录页
And   当前《用户须知》版本为 v2.0，《隐私协议》版本为 v2.0
And   登录页勾选框当前为未勾选
When  教练点击「《隐私协议》」协议名
Then  协议浮层弹窗展示《隐私协议》Tab
When  教练点击「同意」按钮
Then  浮层关闭
And   登录页勾选框状态变为已勾选
And   教练完成 US-051 微信授权登录
Then  系统记录教练同意的《用户须知》和《隐私协议》版本号
And   教练按 US-051 的 redirect_page 进入目标页面
```

#### Scenario: 点击勾选框或外层文字直接切换勾选状态

```gherkin
Given 用户进入用户端登录页
And   登录页勾选框当前为未勾选
When  用户点击勾选框
Then  登录页勾选框状态变为已勾选
And   不唤起协议浮层弹窗
When  用户再次点击「我已阅读并同意」这几个字
Then  登录页勾选框状态变为未勾选
And   不唤起协议浮层弹窗
```

### Requirement: REQ-002 登录后查看协议

系统 MUST 在「我的 → 设置」中提供「用户须知」与「隐私协议」两个入口。系统 MUST 在用户/教练进入对应页面后展示当前生效的协议完整内容。系统 MUST NOT 在查看页提供「撤回授权」按钮或任何授权变更操作。

#### Scenario: 登录后查看用户须知和隐私协议

```gherkin
Given 用户已完成登录并同意过《用户须知》v2.0 和《隐私协议》v2.0
When  用户进入「我的 → 设置 → 用户须知」
Then  页面展示《用户须知》v2.0 完整内容
And   页面不提供「撤回授权」按钮
When  用户进入「我的 → 设置 → 隐私协议」
Then  页面展示《隐私协议》v2.0 完整内容
And   页面不提供「撤回授权」按钮
```

### Requirement: REQ-003 协议浮层交互

系统 MUST 在用户点击浮层外部蒙层区域时关闭浮层，且登录页勾选框状态保持唤起浮层前的状态。

#### Scenario: 点击浮层外部关闭浮层且状态不变

```gherkin
Given 用户已打开协议浮层，勾选框当前为未勾选
When  用户点击浮层外部蒙层区域
Then  浮层关闭
And   登录页勾选框状态保持唤起浮层前的状态
```

## Delta Header

```yaml
delta:
  change: us-009-user-privacy-consent
  capability: user-privacy-consent
  type: revise
  rationale: 交互细化：点击勾选框或外层文字仅切换勾选状态，点击协议名才唤起浮层弹窗
  scope: docs/stories/US-009, openspec/changes/us-009-user-privacy-consent
```
