# US-009 用户隐私协议授权与撤回 — 测试计划

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## TDD 任务清单

| # | Task | RED | GREEN | COMMIT |
|---|------|-----|-------|--------|
| 1 | 当前协议查询接口 | 写失败测试 | 最小实现 | commit |
| 2 | 用户授权状态查询 | 写失败测试 | 最小实现 | commit |
| 3 | 同意协议 | 写失败测试 | 最小实现 | commit |
| 4 | 撤回授权 | 写失败测试 | 最小实现 | commit |
| 5 | 版本更新后重新同意 | 写失败测试 | 最小实现 | commit |

---

## 测试用例

### 单元测试

```python
# backend/tests/test_privacy_consent.py

def test_agree_privacy_policy():
    """正常同意隐私协议"""
    pass

def test_revoke_privacy_consent():
    """正常撤回授权"""
    pass

def test_disagree_blocks_access():
    """不同意阻止进入首页"""
    pass

def test_new_version_requires_reconsent():
    """新版本需重新同意"""
    pass
```

### 集成测试

```python
# backend/tests/integration/test_privacy_api.py

def test_api_current_policy_200():
    """GET /api/privacy-policy/current 返回当前协议"""
    pass

def test_api_consent_200():
    """POST /api/user/privacy/consent 记录同意"""
    pass
```

### E2E 测试

```python
# e2e/tests/test_privacy_flow.py

def test_e2e_privacy_consent():
    """首次注册同意隐私协议流程"""
    pass
```

---

## 验收标准映射

| GWT 场景 | 测试方法 |
|----------|----------|
| 正常同意隐私协议 | test_agree_privacy_policy / test_api_consent_200 |
| 不同意隐私协议 | test_disagree_blocks_access |
| 撤回隐私授权 | test_revoke_privacy_consent |
