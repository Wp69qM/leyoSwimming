> **状态**：待开发填写
> **对应用户故事**：[./user-story.md](./user-story.md)
> **最后更新**：2026-08-04

---

## 1. 测试目标

验证手机号验证码登录功能：
- 已注册手机号验证码登录成功
- 未注册手机号首次验证码登录自动注册
- 验证码错误/过期/重复获取等异常场景
- 登录成功后按 `profile_completed` 分流

---

## 2. 测试范围

- **后端**：`POST /api/auth/sms/code`、`POST /api/auth/login/phone`
- **前端**：手机号登录页
- **状态机**：未注册手机号首次登录触发 `游客 → 注册用户` 转换

---

## 3. TDD 任务清单

### Task 1：发送验证码接口

- RED: 测试发送验证码成功；手机号格式错误返回 400；60s 内重复发送返回 429
- GREEN: 实现 `POST /api/auth/sms/code`
- COMMIT: `feat(auth): add send login sms code endpoint`

### Task 2：手机号验证码登录接口

- RED: 测试已注册手机号+正确验证码返回 token；未注册手机号首次登录创建账号；错误验证码返回 400
- GREEN: 实现 `POST /api/auth/login/phone`
- COMMIT: `feat(auth): add phone code login endpoint`

### Task 3：自动注册逻辑

- RED: 测试未注册手机号登录成功后 `user` 表新增记录，`identity_status='注册用户'`，`profile_completed=false`
- GREEN: 实现自动注册分支
- COMMIT: `feat(auth): auto-register on first phone login`

### Task 4：登录后分流

- RED: 测试 `profile_completed=false` 跳转 US-005；`profile_completed=true` 跳转首页
- GREEN: 实现登录回调页路由判断
- COMMIT: `feat(miniapp): route by profile_completed after phone login`

### Task 5：限流与安全

- RED: 测试连续 5 次错误验证码锁定 30 分钟
- GREEN: 实现验证码校验失败次数计数与锁定
- COMMIT: `feat(auth): add sms verify fail lock`

---

## 4. 测试用例映射

| 场景 | 测试方法 | 层级 |
|------|---------|------|
| 已注册手机号验证码登录成功 | `test_phone_login_existing_user` | 集成 |
| 未注册手机号首次登录自动注册 | `test_phone_login_auto_register` | 集成 |
| 验证码错误 | `test_phone_login_wrong_code` | 集成 |
| 验证码过期 | `test_phone_login_expired_code` | 集成 |
| 60s 内重复获取验证码 | `test_sms_rate_limit` | 集成 |
| 连续 5 次错误锁定 | `test_sms_verify_lock` | 集成 |
| 账号已注销登录 | `test_phone_login_deleted_account` | 集成 |
| 登录后分流 | `test_phone_login_route_by_profile` | E2E |

---

## 5. 待补充

- Mock 短信服务商
- 验证码生成策略
- 设备指纹获取方式
