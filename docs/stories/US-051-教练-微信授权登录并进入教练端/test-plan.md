# US-051 教练微信授权登录并进入教练端 — 测试计划

> **状态**：待开发/QA 填写
> **对应用户故事**：[./user-story.md](./user-story.md)
> **最后更新**：2026-08-03

---

## 1. 测试目标

验证教练端微信授权登录在不同入驻状态下的跳转行为正确，且与用户端 OAuth 复用同一基础设施时不冲突。

---

## 2. 测试范围

| 范围 | 说明 |
|------|------|
| 单元测试 | `redirect_page` 计算逻辑、幂等逻辑 |
| 集成测试 | `POST /api/v1/auth/wechat-login`（含 `app_type=coach`）、`GET /api/v1/coach/me/status` |
| E2E 测试 | 教练端登录 → 按状态跳转完整流程 |

---

## 3. TDD 任务清单

### Task 1：改造登录接口支持 `app_type=coach`

- **RED**：编写测试用例，验证 `app_type=coach` 时返回 `coach_status` 和 `redirect_page`
- **GREEN**：在 `/api/v1/auth/wechat-login` 中读取 `coach` 表并返回新增字段
- **REFACTOR**：提取 `redirect_page` 计算函数
- **COMMIT**：`feat(auth): support app_type=coach in wechat login`

### Task 2：新增 `/api/v1/coach/me/status` 接口

- **RED**：编写测试用例，验证登录态下返回教练入驻状态
- **GREEN**：实现接口
- **REFACTOR**：复用登录接口中的状态查询逻辑
- **COMMIT**：`feat(coach): add GET /api/v1/coach/me/status`

### Task 3：教练端登录页、隐私协议前置校验与跳转逻辑

- **RED**：编写 E2E 测试，模拟各 `redirect_page` 的跳转；模拟未同意隐私协议时跳转 US-009 隐私协议页，同意后再跳转目标页
- **GREEN**：实现教练端登录页和路由守卫；登录成功后调用 US-009 `/api/user/privacy/status` 校验隐私协议状态
- **REFACTOR**：提取跳转决策函数和隐私协议前置检查函数
- **COMMIT**：`feat(coach-miniapp): login page, privacy consent guard and redirect by coach status`

---

## 4. 测试用例映射

| 场景 | 测试方法 | 层级 |
|------|---------|------|
| 未入驻教练首次登录跳转入驻资料页 | `test_coach_login_redirect_to_onboarding` | 集成 |
| 已通过教练登录跳转教练首页 | `test_coach_login_redirect_to_home` | 集成 |
| 待审核教练登录跳转等待审核页 | `test_coach_login_redirect_to_pending` | 集成 |
| 拒绝授权 | `test_coach_login_rejected_authorization` | E2E |
| 用户端已注册账号进入教练端 | `test_coach_login_reuse_user_record` | 集成 |
| 已驳回教练登录跳转重新提交页 | `test_coach_login_redirect_to_rejected` | 集成 |
| 未同意隐私协议时跳转隐私协议页 | `test_coach_login_redirect_to_privacy_first` | E2E |
| 已同意隐私协议时直接按 redirect_page 跳转 | `test_coach_login_skip_privacy_when_agreed` | E2E |

---

## 5. 待补充

- 详细测试数据构造
- Mock 微信 code2session 策略
- Mock US-009 隐私协议状态接口策略
- 错误码断言表
