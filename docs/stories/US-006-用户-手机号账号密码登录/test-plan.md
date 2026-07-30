# US-006 用户手机号/账号密码登录 — 测试计划

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## TDD 任务清单

| # | Task | RED | GREEN | COMMIT |
|---|------|-----|-------|--------|
| 1 | 账号密码登录接口 | 写失败测试 | 最小实现 | commit |
| 2 | 手机号验证码登录接口 | 写失败测试 | 最小实现 | commit |
| 3 | 验证码发送与校验 | 写失败测试 | 最小实现 | commit |
| 4 | 连续失败锁定机制 | 写失败测试 | 最小实现 | commit |
| 5 | 登录态生成与校验 | 写失败测试 | 最小实现 | commit |

---

## 测试用例

### 单元测试

```python
# backend/tests/test_auth_login.py

def test_password_login_success():
    """账号密码登录成功"""
    pass

def test_password_login_invalid():
    """密码错误返回 INVALID_CREDENTIALS"""
    pass

def test_phone_login_success():
    """手机号验证码登录成功"""
    pass

def test_phone_login_code_expired():
    """验证码过期返回 CODE_EXPIRED"""
    pass

def test_account_locked_after_5_failures():
    """连续 5 次失败锁定账号"""
    pass
```

### 集成测试

```python
# backend/tests/integration/test_auth_api.py

def test_api_password_login_200():
    """POST /api/auth/login/password 正常返回"""
    pass

def test_api_sms_code_200():
    """POST /api/auth/sms/code 正常发送"""
    pass
```

### E2E 测试

```python
# e2e/tests/test_login_flow.py

def test_e2e_login_by_password():
    """用户通过账号密码登录完整流程"""
    pass
```

---

## 验收标准映射

| GWT 场景 | 测试方法 |
|----------|----------|
| 手机号验证码登录成功 | test_phone_login_success / test_api_sms_code_200 |
| 账号密码登录成功 | test_password_login_success |
| 密码错误 | test_password_login_invalid |
