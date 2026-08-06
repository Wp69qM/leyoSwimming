# Tasks: coach-phone-login

## Task 1: 发送短信验证码接口 RED
- 编写失败测试：`POST /api/v1/auth/coach/sms/code` 在手机号格式错误时返回 400。
- 运行测试确认失败。

## Task 2: 发送短信验证码接口 GREEN
- 实现接口：校验手机号格式、限流、生成验证码、调用短信服务、写入 `sms_code`。
- 运行测试确认通过。

## Task 3: 发送短信验证码接口 REFACTOR + COMMIT
- 重构限流逻辑为可复用中间件。
- commit。

## Task 4: 手机号验证码登录接口 RED
- 编写失败测试：验证码错误返回 `INVALID_SMS_CODE`。
- 运行测试确认失败。

## Task 5: 手机号验证码登录接口 GREEN
- 实现接口：校验协议勾选、校验验证码、查询/创建 coach 记录、签发 JWT、返回 coach_status。
- 运行测试确认通过。

## Task 6: 手机号验证码登录接口 REFACTOR + COMMIT
- 复用 US-051 的 coach 查询与 JWT 签发逻辑。
- commit。

## Task 7: 自动注册与已离职账号隔离 RED
- 编写失败测试：未注册手机号登录后创建 status=-1 的新 coach 记录；已离职手机号登录后创建新记录且 id 不同。
- 运行测试确认失败。

## Task 8: 自动注册与已离职账号隔离 GREEN
- 实现 `coach` 记录的查询/创建逻辑，确保已离职账号隔离。
- 运行测试确认通过。

## Task 9: 前端手机号登录页 RED
- 编写失败测试：手机号输入框存在、获取验证码按钮存在。
- 运行测试确认失败。

## Task 10: 前端手机号登录页 GREEN
- 实现手机号登录页 UI 与接口调用。
- 运行测试确认通过。

## Task 11: 登录成功后按 coach_status 分流 RED
- 编写失败测试：登录成功后按 coach_status 跳转到对应页面。
- 运行测试确认失败。

## Task 12: 登录成功后按 coach_status 分流 GREEN
- 实现前端跳转逻辑。
- 运行测试确认通过。

## Task 13: 集成测试 + COMMIT
- 跑通完整登录流程集成测试。
- commit。
