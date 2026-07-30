# US-008 用户账号安全设置 — 测试计划

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## TDD 任务清单

| # | Task | RED | GREEN | COMMIT |
|---|------|-----|-------|--------|
| 1 | 换绑手机号接口 | 写失败测试 | 最小实现 | commit |
| 2 | 修改密码接口 | 写失败测试 | 最小实现 | commit |
| 3 | 邮箱绑定接口 | 写失败测试 | 最小实现 | commit |
| 4 | 设备列表查询 | 写失败测试 | 最小实现 | commit |
| 5 | 下线设备功能 | 写失败测试 | 最小实现 | commit |
| 6 | 换绑频率限制 | 写失败测试 | 最小实现 | commit |

---

## 测试用例

### 单元测试

```python
# backend/tests/test_user_security.py

def test_change_phone_success():
    """正常换绑手机号"""
    pass

def test_change_phone_already_bound():
    """新手机号已被绑定"""
    pass

def test_change_password_success():
    """正常修改密码"""
    pass

def test_change_password_wrong_old():
    """原密码错误"""
    pass

def test_revoke_device_session():
    """下线设备使 token 失效"""
    pass

def test_phone_change_rate_limit():
    """24 小时内频繁换绑被限流"""
    pass
```

### 集成测试

```python
# backend/tests/integration/test_user_security_api.py

def test_api_change_phone_200():
    """PUT /api/user/security/phone 正常返回"""
    pass

def test_api_devices_list_200():
    """GET /api/user/security/devices 返回设备列表"""
    pass
```

### E2E 测试

```python
# e2e/tests/test_security_settings.py

def test_e2e_change_password():
    """用户修改密码后使用新密码登录"""
    pass
```

---

## 验收标准映射

| GWT 场景 | 测试方法 |
|----------|----------|
| 正常换绑手机号 | test_change_phone_success / test_api_change_phone_200 |
| 正常修改密码 | test_change_password_success |
| 新手机号已被绑定 | test_change_phone_already_bound |
| 原密码错误 | test_change_password_wrong_old |
| 下线登录设备 | test_revoke_device_session |
