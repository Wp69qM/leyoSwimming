> 本文档对应 `docs/stories/US-010-教练-提交入驻资料/test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: 入驻资料提交接口基础结构 [P0]

**Files:**
- Create: `backend/src/controllers/coach/application.ts`
- Create: `backend/src/routes/coach.ts`
- Test: `backend/tests/controllers/coach/application.test.ts`

**Spec coverage:** REQ-001 Scenario "正常提交入驻资料"

> **前置依赖**：本 US 依赖 US-051 / US-054（教练端登录）与 US-009（隐私协议授权）；测试用例需假设教练已完成登录并同意隐私协议。

- [ ] **RED:** Write 2 failing tests — 已登录教练提交完整资料返回 200，创建 `coach_application` pending 快照，`coach.status=0`；未登录返回 401
- [ ] **GREEN:** Implement `POST /api/coach/application/submit` controller skeleton with route and auth
- [ ] **COMMIT:** `feat(coach): add application submission endpoint skeleton`

## Task 2: 字段完整性校验（含身份证、必填资质、图片数量） [P0]

**Files:**
- Modify: `backend/src/controllers/coach/application.ts`
- Test: `backend/tests/controllers/coach/application.test.ts`

**Spec coverage:** REQ-001 Scenarios "必填项缺失", "身份证号不合法", "必填资质缺失"

- [ ] **RED:** Write 3 failing tests — 缺失必填字段返回 400；缺少身份证正反面/健康证/形象照返回 MISSING_REQUIRED_FIELDS；非法身份证号返回 INVALID_ID_CARD
- [ ] **GREEN:** Add required fields validation, ID card format validation, and certificate type count validation
- [ ] **COMMIT:** `feat(coach): add application field validation including id card and certificates`

## Task 3: 证书图片上传（大小/格式） [P0]

**Files:**
- Create: `backend/src/services/upload/image.ts`
- Create: `backend/src/controllers/upload.ts`
- Test: `backend/tests/services/upload/image.test.ts`

**Spec coverage:** REQ-004 Scenarios "上传合规图片", "上传超大图片"

- [ ] **RED:** Write 3 failing tests — 正确格式大小上传返回 URL；超大图片返回 `IMAGE_TOO_LARGE`；非图片格式返回 `INVALID_IMAGE_FORMAT`
- [ ] **GREEN:** Implement image upload with size/format validation
- [ ] **COMMIT:** `feat(upload): add certificate image upload with size and format validation`

## Task 4: 重复提交拦截 [P0]

**Files:**
- Modify: `backend/src/controllers/coach/application.ts`
- Modify: `backend/src/repositories/coach_application.ts`
- Test: `backend/tests/controllers/coach/application.test.ts`

**Spec coverage:** REQ-001 Scenario "重复提交入驻申请"

- [ ] **RED:** Write 3 failing tests — 已有 pending 状态的 coach_application 返回 `COACH_APPLICATION_PENDING`；status=2 已驳回教练可重新提交；status=3 已离职教练可重新提交且复用原记录
- [ ] **GREEN:** Add duplicate application check based on `coach_application.status = 'pending'`; allow resubmission for status=2/3 with new snapshot
- [ ] **COMMIT:** `feat(coach): block duplicate coach application and allow resubmission`

## Task 5: 参考单价范围校验 [P0]

**Files:**
- Modify: `backend/src/controllers/coach/application.ts`
- Test: `backend/tests/controllers/coach/application.test.ts`

**Spec coverage:** REQ-002 Scenario "参考单价超出范围"

- [ ] **RED:** Write 2 failing tests — 参考单价 5000 返回 `INVALID_REFERENCE_PRICE`；参考单价 100 通过
- [ ] **GREEN:** Add reference price range validation (50-2000)
- [ ] **COMMIT:** `feat(coach): add reference price range validation`

## Task 6: 草稿保存功能 [P1]

**Files:**
- Modify: `backend/src/controllers/coach/application.ts`
- Modify: `backend/src/repositories/coach_application.ts`
- Test: `backend/tests/controllers/coach/application.test.ts`

**Spec coverage:** REQ-001 Scenario "保存入驻资料草稿"

- [ ] **RED:** Write 4 failing tests — status=-1 首次保存草稿返回 200 且 `coach.status=-1`；创建 `coach_application` status=draft；status=2 保存草稿后 coach.status 仍为 2；status=3 保存草稿后复用原记录
- [ ] **GREEN:** Implement `POST /api/coach/application/save-draft`（保存草稿写入 `coach_application` 快照，`status=draft`，不修改 `coach.status`）
- [ ] **COMMIT:** `feat(coach): add application draft save as snapshot`

## Task 7: 已驳回 / 已离职教练重新提交 [P0]

**Files:**
- Modify: `backend/src/controllers/coach/application.ts`
- Modify: `backend/src/repositories/coach_application.ts`
- Test: `backend/tests/controllers/coach/application.test.ts`

**Spec coverage:** REQ-001 Scenarios "已驳回教练重新提交入驻资料", "已离职教练重新入驻提交资料"

- [ ] **RED:** Write 3 failing tests — status=2 教练重新提交后 status=0 且新建 previous_coach_status=2 快照；status=3 教练重新入驻提交后 status=0 且新建 previous_coach_status=3 快照；历史 coach 生效数据不回滚
- [ ] **GREEN:** Implement resubmission/reapply logic for rejected and resigned applications with new snapshot
- [ ] **COMMIT:** `feat(coach): support resubmission after rejection and reapply after resignation`

## Task 8: 等待审核页查询完整资料 [P1]

**Files:**
- Modify: `backend/src/controllers/coach/application.ts`
- Create/Modify: `backend/src/repositories/coach_certificate_application.ts`
- Test: `backend/tests/controllers/coach/application.test.ts`

**Spec coverage:** REQ-003 Scenario "等待审核页查看已提交资料"

- [ ] **RED:** Write 2 failing tests — POST /api/coach/application/detail 返回最新 coach_application 快照完整资料含证书列表；手机号与身份证号脱敏展示
- [ ] **GREEN:** Implement POST /api/coach/application/detail with snapshot data and certificate list
- [ ] **COMMIT:** `feat(coach): add application detail query for pending page`

## Task 9: 教练小程序入驻资料页/等待审核页 [P1]

**Files:**
- Create: `miniapp-coach/src/pages/onboarding/index.tsx`
- Create: `miniapp-coach/src/pages/onboarding/success.tsx`
- Create: `miniapp-coach/src/pages/onboarding/pending.tsx`
- Test: `miniapp-coach/src/pages/onboarding/index.test.tsx`

**Spec coverage:** REQ-001 / REQ-002 / REQ-003 / REQ-004 正常/异常场景

- [ ] **RED:** Write 4 failing tests — 提交成功后跳转成功页；2 秒后自动跳转等待审核页；图片过大提示错误；必填项缺失阻止提交；等待审核页可打开资料详情浮层
- [ ] **GREEN:** Implement coach onboarding pages with form, upload, and pending state view
- [ ] **COMMIT:** `feat(miniapp-coach): add coach onboarding and pending pages`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-5, 7），P1 选做（Task 6, 8, 9）

## GWT Coverage Matrix

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 | Task 5 | Task 6 | Task 7 | Task 8 | Task 9 |
|---------|--------|--------|--------|--------|--------|--------|--------|--------|--------|
| §6.1 正常提交（正常） | ✅ | ✅ | ✅ | ✅ | ✅ | — | — | — | ✅ |
| §6.2 保存草稿（正常） | — | — | — | — | — | ✅ | — | — | ✅ |
| §6.3 必填项缺失（异常） | — | ✅ | — | — | — | — | — | — | ✅ |
| §6.4 身份证号不合法（异常） | — | ✅ | — | — | — | — | — | — | ✅ |
| §6.5 证书图片过大/格式错误（异常） | — | — | ✅ | — | — | — | — | — | ✅ |
| §6.6 参考单价超出范围（异常） | — | — | — | — | ✅ | — | — | — | ✅ |
| §6.7 重复提交（异常） | — | — | — | ✅ | — | — | — | — | ✅ |
| §6.8 已驳回重新提交（正常） | — | — | — | — | — | — | ✅ | — | ✅ |
| §6.9 已离职重新入驻提交资料（正常） | — | — | — | ✅ | — | ✅ | ✅ | — | ✅ |
| §6.10 等待审核页查看资料（正常） | — | — | — | — | — | — | — | ✅ | ✅ |
| §6.11 入驻提交成功页展示（正常） | — | — | — | — | — | — | — | — | ✅ |
| §6.12 已提交入驻资料教练登录跳转等待审核页（正常） | — | — | — | — | — | — | — | — | ✅ |
