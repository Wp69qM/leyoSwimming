# US-011 管理员审核教练入驻资质 — 测试计划

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## TDD 任务清单

| # | Task | RED | GREEN | COMMIT |
|---|------|-----|-------|--------|
| 1 | 待审核列表查询 | 写失败测试 | 最小实现 | commit |
| 2 | 审核通过 | 写失败测试 | 最小实现 | commit |
| 3 | 审核驳回 | 写失败测试 | 最小实现 | commit |
| 4 | 权限校验 | 写失败测试 | 最小实现 | commit |
| 5 | 状态机校验 | 写失败测试 | 最小实现 | commit |

---

## 测试用例

### 单元测试

```python
# backend/tests/test_coach_audit.py

def test_coach_approve_success():
    """审核通过成功"""
    pass

def test_coach_reject_success():
    """审核驳回成功"""
    pass

def test_coach_approve_no_permission():
    """无权限审核返回 403"""
    pass

def test_coach_approve_not_pending():
    """非待审核状态拒绝"""
    pass

def test_coach_audit_resubmitted_application():
    """已驳回教练重新提交后再次审核"""
    pass
```

### 集成测试

```python
# backend/tests/integration/test_coach_audit_api.py

def test_api_coach_approve_200():
    """POST /api/admin/coach/applications/{id}/approve 成功"""
    pass

def test_api_coach_reject_200():
    """POST /api/admin/coach/applications/{id}/reject 成功"""
    pass
```

### E2E 测试

```python
# e2e/tests/test_coach_audit_flow.py

def test_e2e_coach_audit():
    """管理员审核教练入驻完整流程"""
    pass
```

---

## 验收标准映射

| GWT 场景 | 测试方法 |
|----------|----------|
| 审核通过 | test_coach_approve_success / test_api_coach_approve_200 |
| 审核驳回 | test_coach_reject_success / test_api_coach_reject_200 |
| 无权限审核 | test_coach_approve_no_permission |
| 已驳回重新提交后审核 | test_coach_audit_resubmitted_application |
