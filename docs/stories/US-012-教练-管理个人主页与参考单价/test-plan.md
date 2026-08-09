# US-012 教练管理个人主页与参考单价 — 测试计划

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## TDD 任务清单

| # | Task | RED | GREEN | COMMIT |
|---|------|-----|-------|--------|
| 1 | 个人主页查询 | 写失败测试 | 最小实现 | commit |
| 2 | 个人主页更新 | 写失败测试 | 最小实现 | commit |
| 3 | 参考单价更新 | 写失败测试 | 最小实现 | commit |
| 4 | 参考单价范围校验 | 写失败测试 | 最小实现 | commit |
| 5 | 参考单价修改频率限制 | 写失败测试 | 最小实现 | commit |
| 6 | 敏感词过滤 | 写失败测试 | 最小实现 | commit |

---

## 测试用例

### 单元测试

```python
# backend/tests/test_coach_profile.py

def test_coach_profile_update_success():
    """正常更新个人主页"""
    pass

def test_coach_profile_update_status_not_allowed():
    """教练状态非已通过时拒绝更新"""
    pass

def test_coach_profile_field_validation():
    """字段校验失败阻止保存（姓名、年龄、邮箱、个人简介）"""
    pass

def test_coach_profile_image_format_error():
    """微信二维码/个人形象照格式非法"""
    pass

def test_coach_profile_image_too_large():
    """微信二维码/个人形象照超过 5MB"""
    pass

def test_coach_reference_price_update_success():
    """正常更新参考单价"""
    pass

def test_coach_reference_price_out_of_range():
    """参考单价超出范围"""
    pass

def test_coach_bio_sensitive_content():
    """个人简介含敏感词"""
    pass

def test_coach_price_change_limit():
    """参考单价修改频率限制"""
    pass
```

### 集成测试

```python
# backend/tests/integration/test_coach_profile_api.py

def test_api_coach_profile_update_200():
    """POST /api/coach/profile/update 正常返回"""
    pass

def test_api_coach_reference_price_200():
    """POST /api/coach/reference-price/update 正常返回"""
    pass
```

### E2E 测试

```python
# e2e/tests/test_coach_profile_flow.py

def test_e2e_coach_update_profile():
    """教练更新主页后学员端可见"""
    pass
```

---

## 验收标准映射

| GWT 场景 | 测试方法 |
|----------|----------|
| 正常更新个人主页 | test_coach_profile_update_success |
| 正常更新参考单价 | test_coach_reference_price_update_success |
| 参考单价超出范围 | test_coach_reference_price_out_of_range |
| 字段校验失败阻止保存 | test_coach_profile_field_validation / test_coach_profile_image_format_error / test_coach_profile_image_too_large |
| 个人简介含敏感词 | test_coach_bio_sensitive_content |
| 更新后学员端可见 | test_e2e_coach_update_profile |
