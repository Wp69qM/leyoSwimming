# US-010 教练提交入驻资料 — 测试计划

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## TDD 任务清单

| # | Task | RED | GREEN | COMMIT |
|---|------|-----|-------|--------|
| 1 | 入驻资料提交接口 | 写失败测试 | 最小实现 | commit |
| 2 | 字段完整性校验 | 写失败测试 | 最小实现 | commit |
| 3 | 证书图片上传 | 写失败测试 | 最小实现 | commit |
| 4 | 重复提交拦截 | 写失败测试 | 最小实现 | commit |
| 5 | 参考单价范围校验 | 写失败测试 | 最小实现 | commit |
| 6 | 草稿保存功能 | 写失败测试 | 最小实现 | commit |

---

## 测试用例

### 单元测试

```python
# backend/tests/test_coach_application.py

def test_coach_application_success():
    """正常提交入驻资料"""
    pass

def test_coach_application_missing_fields():
    """必填项缺失拒绝"""
    pass

def test_coach_application_duplicate():
    """重复提交拒绝"""
    pass

def test_coach_application_invalid_price():
    """参考单价超出范围"""
    pass

def test_coach_application_image_too_large():
    """证书图片过大"""
    pass
```

### 集成测试

```python
# backend/tests/integration/test_coach_application_api.py

def test_api_coach_application_200():
    """POST /api/coach/application 正常返回"""
    pass

def test_api_upload_image_200():
    """POST /api/upload/image 上传成功"""
    pass
```

### E2E 测试

```python
# e2e/tests/test_coach_apply_flow.py

def test_e2e_coach_application():
    """教练提交入驻资料完整流程"""
    pass
```

---

## 验收标准映射

| GWT 场景 | 测试方法 |
|----------|----------|
| 正常提交入驻资料 | test_coach_application_success / test_api_coach_application_200 |
| 保存入驻资料草稿 | 草稿保存测试 |
| 必填项缺失 | test_coach_application_missing_fields |
| 重复提交入驻申请 | test_coach_application_duplicate |
| 证书图片过大 | test_coach_application_image_too_large |
