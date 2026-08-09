# Spec Delta: user-logout

> 本 spec 为 US-052 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 用户退出登录

系统 MUST 在小程序「我的」TabBar 页面底部展示「退出登录」按钮。用户点击后 MUST 弹出二次确认弹窗，含「确定」和「取消」两个按钮。用户点击「确定」后，系统 MUST 调用后端 `POST /api/user/auth/logout` 接口，携带当前 `access_token` 与 `refresh_token`。后端 MUST 校验 `access_token` 有效，并将当前 `refresh_token` 对应的 `user_session` 记录标记为失效。前端在收到成功响应后 MUST 清除本地 `access_token`、`refresh_token`、`expires_in`、`token_expire_at`，并停留在「我的」页面刷新为未登录态（展示游客可见内容）。用户点击「取消」后 MUST 关闭弹窗并保持当前登录态。

#### Scenario: 用户正常退出登录

```gherkin
Given 用户已登录且本地存在有效的 access_token 和 refresh_token
And   用户位于「我的」页面
When  用户点击底部「退出登录」按钮
And   用户在弹窗中点击「确定」
Then  系统调用后端退出登录接口
And   后端将当前 session/refresh_token 标记为失效
And   前端清除本地 access_token、refresh_token 及 expires_in
And   系统停留在「我的」页面，展示未登录态（游客可见内容）
And   用户再次访问受登录态保护的接口时返回 401
```

#### Scenario: 用户取消退出

```gherkin
Given 用户已登录且位于「我的」页面
When  用户点击「退出登录」按钮
And   用户在弹窗中点击「取消」
Then  弹窗关闭
And   前端不调用后端退出登录接口
And   本地 token 保持不变
And   用户仍停留在「我的」页面且保持登录态
```

#### Scenario: 本地 token 已过期或不存在

```gherkin
Given 用户位于「我的」页面
And   本地 access_token 不存在或已过期
When  用户点击「退出登录」按钮并确认
Then  前端直接清除本地残留的 token
And   系统停留在「我的」页面，展示未登录态（游客可见内容）
And   后端不执行任何 token 失效操作（无可失效 token）
```

---

### Requirement: REQ-002 退出登录后端会话失效

系统 MUST 确保用户退出登录后，当前会话的 `refresh_token` 无法继续用于换发新的 `access_token`。后端 `POST /api/user/auth/logout` 必须登录鉴权，且 MUST 支持幂等调用（同一 token 重复退出仍返回成功）。

#### Scenario: 退出后 refresh_token 失效

```gherkin
Given 用户已登录且持有有效的 refresh_token
When  用户完成退出登录流程
Then  该 refresh_token 无法通过刷新接口换取新的 access_token
And   刷新接口返回 HTTP 401，响应体为 { "code": 40101, "message": "登录态已失效，请重新登录", "data": null }
```

#### Scenario: 重复退出幂等

```gherkin
Given 用户已登录
When  用户连续两次调用退出登录接口
Then  两次调用均返回 200
And   当前 session 处于失效状态
```
