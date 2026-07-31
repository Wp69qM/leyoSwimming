# US-005 用户补充注册资料 — 测试计划

> **状态**：初稿　|　**最后更新**：2026-07-31

---

## TDD 任务清单

| # | Task | RED | GREEN | COMMIT |
|---|------|-----|-------|--------|
| 1 | 资料补充接口基础结构 | 写失败测试 | 最小实现 | commit |
| 2 | 手机号唯一性校验 | 写失败测试 | 最小实现 | commit |
| 3 | 用户名唯一性校验 | 写失败测试 | 最小实现 | commit |
| 4 | 密码强度校验 | 写失败测试 | 最小实现 | commit |
| 5 | 资料完成状态转换 | 写失败测试 | 最小实现 | commit |
| 6 | 幂等性处理 | 写失败测试 | 最小实现 | commit |

---

## 测试用例

### 单元测试

```python
# backend/tests/test_user_profile.py

def test_user_profile_update_success():
    """正常补充资料，profile_completed 变为 true，identity 保持注册用户"""
    pass

def test_user_profile_phone_already_bound():
    """手机号已注册，返回 PHONE_ALREADY_BOUND"""
    pass

def test_user_profile_username_taken():
    """用户名已存在，返回 USERNAME_TAKEN"""
    pass

def test_user_profile_weak_password():
    """密码强度不足，返回 WEAK_PASSWORD"""
    pass

def test_user_profile_idempotent():
    """重复提交幂等处理"""
    pass
```

### 集成测试

```python
# backend/tests/integration/test_user_profile_api.py

def test_api_user_profile_201():
    """PUT /api/user/profile 正常返回 200"""
    pass

def test_api_phone_exists_200():
    """GET /api/user/phone/exists 返回正确存在性"""
    pass
```

### E2E 测试

```python
# e2e/tests/test_register_flow.py

def test_e2e_complete_profile():
    """微信登录后补充资料并进入首页"""
    pass
```

---

## 验收标准映射

| GWT 场景 | 测试方法 |
|----------|----------|
| 正常补充注册资料 | test_user_profile_update_success / test_api_user_profile_201 |
| 手机号已被注册 | test_user_profile_phone_already_bound |
| 必填项缺失 | 前端表单校验 + E2E |
