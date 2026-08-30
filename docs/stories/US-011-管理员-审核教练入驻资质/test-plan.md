# US-011 管理员审核教练入驻资质 — 测试计划

> **状态**：初稿　|　**最后更新**：2026-08-05

---

## TDD 任务清单

| # | Task | RED | GREEN | COMMIT |
|---|------|-----|-------|--------|
| 1 | 待审核列表查询（查询 coach_application.status=pending 快照） | 写失败测试 | 最小实现 | commit |
| 2 | 审核通过（快照字段覆盖 coach 生效资料与 coach_certificate） | 写失败测试 | 最小实现 | commit |
| 3 | 审核驳回（coach.status 恢复为 previous_coach_status） | 写失败测试 | 最小实现 | commit |
| 4 | 权限校验 | 写失败测试 | 最小实现 | commit |
| 5 | 状态机校验（仅 pending → approved/rejected） | 写失败测试 | 最小实现 | commit |
| 6 | 发送审核通知 | 写失败测试 | 最小实现 | commit |
| 7 | Web 管理后台审核页面 | 写失败测试 | 最小实现 | commit |

---

## 测试用例

### 单元测试

```python
# backend/tests/test_coach_audit.py

def test_coach_approve_success():
    """审核通过：coach_application.status=approved，coach 生效资料被快照覆盖，coach.status=1"""
    pass

def test_coach_approve_snapshot_overwrites_coach():
    """审核通过时 coach_application 快照字段覆盖写入 coach 表生效资料"""
    pass

def test_coach_approve_snapshot_certificates_overwrite():
    """审核通过时 coach_certificate_application 快照证书覆盖写入 coach_certificate"""
    pass

def test_coach_reject_success():
    """首次申请审核驳回：coach_application.status=rejected，coach.status=2"""
    pass

def test_coach_reject_resignation_reapply():
    """已离职重新入驻申请驳回：coach.status 恢复为 3，生效资料不变"""
    pass

def test_coach_reject_no_permission():
    """无权限审核返回 403"""
    pass

def test_coach_approve_not_pending():
    """非 pending 申请通过返回 NOT_PENDING"""
    pass

def test_coach_audit_already_reviewed():
    """已审核记录再次审核返回 ALREADY_REVIEWED"""
    pass

def test_coach_audit_resubmitted_application():
    """已驳回教练重新提交后再次审核：按新 pending 快照处理"""
    pass
```

### 集成测试

```python
# backend/tests/integration/test_coach_audit_api.py

def test_api_coach_approve_200():
    """POST /api/admin/coach/application/approve 成功，快照覆盖生效资料"""
    pass

def test_api_coach_reject_200():
    """POST /api/admin/coach/application/reject 成功，status 恢复 previous_coach_status"""
    pass

def test_api_coach_applications_list_200():
    """POST /api/admin/coach/application/list 仅返回 pending 快照列表"""
    pass

def test_api_coach_application_detail_200():
    """POST /api/admin/coach/application/detail 返回快照完整资料与申请历史"""
    pass

def test_api_coach_approve_not_pending_400():
    """POST /api/admin/coach/application/approve 对非 pending 申请返回 NOT_PENDING/ALREADY_REVIEWED"""
    pass

def test_api_coach_reject_no_permission_403():
    """POST /api/admin/coach/application/reject 无权限返回 403 FORBIDDEN"""
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
| 审核列表直接通过 | test_coach_approve_from_list / test_coach_approve_success / test_coach_approve_snapshot_overwrites_coach / test_coach_approve_snapshot_certificates_overwrite / test_api_coach_approve_200 |
| 审核详情页通过 | test_coach_approve_from_detail / test_coach_approve_success / test_coach_approve_snapshot_overwrites_coach / test_coach_approve_snapshot_certificates_overwrite / test_api_coach_approve_200 |
| 审核驳回 | test_coach_reject_success / test_api_coach_reject_200 |
| 无权限审核 | test_coach_reject_no_permission |
| 已离职重新入驻申请被驳回 | test_coach_reject_resignation_reapply |
| 重复审核 | test_coach_audit_already_reviewed |
| 待审核列表查询 | test_api_coach_applications_list_200 |
| 审核详情查询 | test_api_coach_application_detail_200 |
| 非 pending 状态审核 | test_api_coach_approve_not_pending_400 |
| 无权限驳回 | test_api_coach_reject_no_permission_403 |

---

## 变更日志

| 版本 | 日期 | 变更 |
|------|------|------|
| v1.0 | 2026-07-30 | 初版 |
| v2.0 | 2026-08-05 | 重构为快照表设计：审核目标改为 coach_application；通过时快照覆盖 coach 与 coach_certificate；驳回时恢复 previous_coach_status；新增已离职重新入驻驳回测试用例 |
| v2.1 | 2026-08-08 | 验收标准映射拆分为「审核列表直接通过」与「审核详情页通过」 |
