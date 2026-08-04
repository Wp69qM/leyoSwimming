> **OpenSpec Tasks | 映射自 `docs/stories/US-006-用户-手机号验证码登录/test-plan.md`**

## Task 1: 手机号验证码登录接口 [P0]

- RED: 测试已注册手机号 + 正确验证码返回 200 与 token；未注册手机号首次登录自动创建账号并返回 token
- GREEN: 实现 `POST /api/auth/login/phone`，包含手机号格式校验、验证码校验、`termsAccepted`/`privacyAccepted` 校验、自动注册、JWT 签发
- COMMIT: `feat(auth): add phone code login endpoint with terms/privacy validation and auto-register`

## Task 2: 验证码发送与校验 [P0]

- RED: 测试发送成功写入 sms_code；60 秒内重复发送被拒绝；验证码错误返回 INVALID_SMS_CODE
- GREEN: 实现 `POST /api/auth/sms/code` 与验证码校验服务
- COMMIT: `feat(auth): add SMS code send and verify service`

## Task 3: 账号状态拦截（注销） [P1]

- RED: 测试已注销账号登录返回 ACCOUNT_DELETED
- GREEN: 在登录服务中增加 user.status 检查
- COMMIT: `feat(auth): reject login for deleted accounts`

## Task 4: 登录态生成与校验 [P0]

- RED: 测试登录成功后生成 30 天有效 token；token 可校验出 user_id
- GREEN: 实现 JWT/session 服务，复用 US-004 的会话管理
- COMMIT: `feat(auth): add session/token service for phone login`

## Task 5: 小程序手机号登录页 [P1]

- RED: E2E 测试未勾选协议点击登录提示文案；已注册手机号登录成功跳转首页；未注册手机号登录成功跳转 US-005
- GREEN: 实现手机号登录页，含协议勾选区、手机号/验证码输入、登录按钮
- COMMIT: `feat(miniapp): add phone code login page with terms/privacy checkbox`

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
