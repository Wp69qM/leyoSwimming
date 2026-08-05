> **OpenSpec Tasks | 映射自 `docs/stories/US-006-用户-手机号验证码登录/test-plan.md`**

## Task 1: 手机号验证码登录接口 [P0]

- RED: 测试已注册手机号 + 正确验证码返回 200 与 token；未注册手机号首次登录自动创建账号并返回 token；已注销手机号（status=1）登录按 PRD §5.2.1 第 4 条重新创建账号并返回 token
- GREEN: 实现 `POST /api/auth/login/phone`，包含手机号格式校验、验证码校验、`termsAccepted`/`privacyAccepted` 校验、自动注册/注销后重新注册、JWT 签发
- COMMIT: `feat(auth): add phone code login endpoint with terms/privacy validation, auto-register and re-register after deletion`

## Task 2: 验证码发送与校验 [P0]

- RED: 测试发送成功写入 sms_code；60 秒内重复发送被拒绝；验证码错误返回 INVALID_SMS_CODE
- GREEN: 实现 `POST /api/auth/sms/code` 与验证码校验服务
- COMMIT: `feat(auth): add SMS code send and verify service`

## Task 3: 注销后重新注册隔离 [P1]

- RED: 测试已注销手机号登录生成的新 user_id 与原账号不同，且新账号无法访问原账号数据
- GREEN: 在登录服务中按 `phone + status=1` 识别已注销账号，创建新记录并确保原数据不绑定
- COMMIT: `feat(auth): ensure re-registered account after deletion is isolated from old data`

## Task 4: 登录态生成、存储与校验 [P0]

- RED: 测试登录成功后返回 `access_token`/`refresh_token`/`expires_in`；前端将 token 写入本地存储；后续请求携带 `Authorization: Bearer {access_token}`；后端校验 token 有效；token 过期后可用 refresh_token 换发
- GREEN: 实现 JWT/session 服务并复用 US-004，实现前端 token 存储与请求拦截器，实现 token 刷新逻辑
- COMMIT: `feat(auth): add session/token storage, request interceptor and refresh for phone login`

## Task 5: 小程序手机号登录页 [P1]

- RED: E2E 测试未勾选协议点击登录提示文案；已注册手机号登录成功跳转首页并存储 token；未注册手机号登录成功跳转 US-005 并存储 token；token 过期触发刷新或重新登录
- GREEN: 实现手机号登录页，含协议勾选区、手机号/验证码输入、登录按钮、登录态检查与跳转
- COMMIT: `feat(miniapp): add phone code login page with terms/privacy checkbox and session handling`

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
