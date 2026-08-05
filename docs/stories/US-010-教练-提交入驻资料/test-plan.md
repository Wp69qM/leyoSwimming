# US-010 教练提交入驻资料 — 测试计划

> **状态**：初稿　|　**最后更新**：2026-08-05

---

## TDD 任务清单

| # | Task | RED | GREEN | COMMIT |
|---|------|-----|-------|--------|
| 1 | 入驻资料提交接口基础结构 | 写失败测试 | 最小实现 | commit |
| 2 | 字段完整性校验（含身份证、必填资质、图片数量） | 写失败测试 | 最小实现 | commit |
| 3 | 证书图片上传（大小/格式） | 写失败测试 | 最小实现 | commit |
| 4 | 重复提交拦截 | 写失败测试 | 最小实现 | commit |
| 5 | 参考单价范围校验 | 写失败测试 | 最小实现 | commit |
| 6 | 草稿保存功能（status=-1 → 0，submitted_at=NULL） | 写失败测试 | 最小实现 | commit |
| 7 | 已驳回教练重新提交（status=2 → 0，清空 rejection_reason） | 写失败测试 | 最小实现 | commit |
| 8 | 等待审核页查询完整资料（GET /api/coach/application） | 写失败测试 | 最小实现 | commit |
| 9 | 教练小程序入驻资料页/等待审核页 | 写失败测试 | 最小实现 | commit |

---

## 测试用例

### 单元测试

```python
# backend/tests/test_coach_application.py

def test_coach_application_success():
    """正常提交入驻资料：status=-1 → 0，submitted_at 写入，证书记录写入"""
    pass

def test_coach_application_draft_save():
    """保存草稿：status=-1 → 0，submitted_at = NULL，不进入审核队列"""
    pass

def test_coach_application_missing_fields():
    """必填项缺失拒绝：前端/后端阻止，status 保持 -1"""
    pass

def test_coach_application_invalid_id_card():
    """身份证号不合法返回 INVALID_ID_CARD"""
    pass

def test_coach_application_missing_certificates():
    """必填资质缺失（如缺少健康证/形象照）返回 MISSING_REQUIRED_FIELDS"""
    pass

def test_coach_application_duplicate():
    """重复提交拒绝：已正式提交返回 COACH_APPLICATION_PENDING"""
    pass

def test_coach_application_invalid_price():
    """参考单价超出 50-2000 范围返回 INVALID_REFERENCE_PRICE"""
    pass

def test_coach_application_resubmit_after_rejection():
    """已驳回教练重新提交：status=2 → 0，rejection_reason 清空"""
    pass

def test_coach_application_image_too_large():
    """单张图片超过 5MB 返回 IMAGE_TOO_LARGE"""
    pass

def test_coach_application_invalid_image_format():
    """非 JPG/PNG 图片返回 INVALID_IMAGE_FORMAT"""
    pass
```

### 集成测试

```python
# backend/tests/integration/test_coach_application_api.py

def test_api_coach_application_200():
    """POST /api/coach/application 正常返回 200"""
    pass

def test_api_coach_application_draft_200():
    """PUT /api/coach/application/draft 正常返回 200 且 submitted_at 为空"""
    pass

def test_api_coach_application_400_pending():
    """重复提交返回 400 + COACH_APPLICATION_PENDING"""
    pass

def test_api_coach_application_400_invalid_id_card():
    """非法身份证号返回 400 + INVALID_ID_CARD"""
    pass

def test_api_coach_application_400_invalid_price():
    """参考单价超出范围返回 400 + INVALID_REFERENCE_PRICE"""
    pass

def test_api_coach_application_get_200():
    """GET /api/coach/application 返回完整资料含证书列表"""
    pass

def test_api_upload_image_200():
    """POST /api/upload/image 上传合规图片成功"""
    pass

def test_api_upload_image_413():
    """POST /api/upload/image 超大图片返回 IMAGE_TOO_LARGE"""
    pass

def test_api_upload_image_400_format():
    """POST /api/upload/image 非法格式返回 INVALID_IMAGE_FORMAT"""
    pass
```

### E2E 测试

```python
# e2e/tests/test_coach_apply_flow.py

def test_e2e_coach_application_flow():
    """教练首次提交入驻资料完整流程：填写页 → 成功页 → 等待审核页"""
    pass

def test_e2e_coach_application_draft_and_continue():
    """教练保存草稿后再次进入可继续编辑并提交"""
    pass

def test_e2e_coach_application_rejected_resubmit():
    """已驳回教练查看原因后重新提交"""
    pass

def test_e2e_coach_pending_page_view_detail():
    """等待审核页查看完整入驻资料浮层"""
    pass
```

---

## 验收标准映射

| GWT 场景 | 测试方法 |
|----------|----------|
| 场景 1：正常提交入驻资料 | test_coach_application_success / test_api_coach_application_200 / test_e2e_coach_application_flow |
| 场景 2：保存入驻资料草稿 | test_coach_application_draft_save / test_api_coach_application_draft_200 / test_e2e_coach_application_draft_and_continue |
| 场景 3：必填项缺失 | test_coach_application_missing_fields |
| 场景 4：身份证号不合法 | test_coach_application_invalid_id_card / test_api_coach_application_400_invalid_id_card |
| 场景 5：证书图片过大或格式错误 | test_coach_application_image_too_large / test_coach_application_invalid_image_format / test_api_upload_image_413 / test_api_upload_image_400_format |
| 场景 6：参考单价超出范围 | test_coach_application_invalid_price / test_api_coach_application_400_invalid_price |
| 场景 7：重复提交入驻申请 | test_coach_application_duplicate / test_api_coach_application_400_pending |
| 场景 8：已驳回教练重新提交 | test_coach_application_resubmit_after_rejection / test_e2e_coach_application_rejected_resubmit |
| 场景 9：等待审核页查看已提交资料 | test_api_coach_application_get_200 / test_e2e_coach_pending_page_view_detail |

---

## 变更日志

| 版本 | 日期 | 变更 |
|------|------|------|
| v1.0 | 2026-07-30 | 初版 |
| v2.0 | 2026-08-05 | 按字段设计新增身份证校验、必填资质校验、图片格式校验、等待审核页查询、驳回重新提交等测试用例与任务 |
