# Tasks: US-010 教练提交入驻资料

> 本文档对应 `docs/stories/US-010-教练-提交入驻资料/test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: 入驻资料提交接口基础结构 [P0]

**Files:**
- Create: `backend/src/controllers/coach/application.ts`
- Create: `backend/src/routes/coach.ts`
- Test: `backend/tests/controllers/coach/application.test.ts`

**Spec coverage:** REQ-001 Scenario "正常提交入驻资料"

- [ ] **RED:** Write 2 failing tests — 已登录教练提交完整资料返回 200，`coach.status=0`；未登录返回 401
- [ ] **GREEN:** Implement `POST /api/coach/application` controller skeleton with route and auth
- [ ] **COMMIT:** `feat(coach): add application submission endpoint skeleton`

## Task 2: 字段完整性校验 [P0]

**Files:**
- Modify: `backend/src/controllers/coach/application.ts`
- Test: `backend/tests/controllers/coach/application.test.ts`

**Spec coverage:** REQ-001 Scenario "必填项缺失"

- [ ] **RED:** Write 2 failing tests — 缺失必填字段返回 400；缺少证书返回 400
- [ ] **GREEN:** Add required fields validation
- [ ] **COMMIT:** `feat(coach): add application field validation`

## Task 3: 证书图片上传 [P0]

**Files:**
- Create: `backend/src/services/upload/image.ts`
- Create: `backend/src/controllers/upload.ts`
- Test: `backend/tests/services/upload/image.test.ts`

**Spec coverage:** REQ-001 Scenario "证书图片过大"

- [ ] **RED:** Write 3 failing tests — 正确格式大小上传返回 URL；超大图片返回 `IMAGE_TOO_LARGE`；非图片格式返回 `INVALID_IMAGE_FORMAT`
- [ ] **GREEN:** Implement image upload with size/format validation
- [ ] **COMMIT:** `feat(upload): add certificate image upload`

## Task 4: 重复提交拦截 [P0]

**Files:**
- Modify: `backend/src/controllers/coach/application.ts`
- Modify: `backend/src/repositories/coach.ts`
- Test: `backend/tests/controllers/coach/application.test.ts`

**Spec coverage:** REQ-001 Scenario "重复提交入驻申请"

- [ ] **RED:** Write 2 failing tests — 已有待审核记录返回 `COACH_APPLICATION_PENDING`；已离职教练可重新提交
- [ ] **GREEN:** Add duplicate application check
- [ ] **COMMIT:** `feat(coach): block duplicate coach application`

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
- Modify: `backend/src/repositories/coach.ts`
- Test: `backend/tests/controllers/coach/application.test.ts`

**Spec coverage:** REQ-001 Scenario "保存入驻资料草稿"

- [ ] **RED:** Write 2 failing tests — 部分字段保存草稿返回 200 且 `status=0`；`submitted_at` 为 NULL 表示草稿
- [ ] **GREEN:** Implement `PUT /api/coach/application/draft`（保存草稿与提交审核均写入 `status=0`，不新增独立草稿态）
- [ ] **COMMIT:** `feat(coach): add application draft save without separate draft state`

## Task 7: 教练小程序入驻资料页 [P1]

**Files:**
- Create: `miniapp-coach/src/pages/application/index.tsx`
- Test: `miniapp-coach/src/pages/application/index.test.tsx`

**Spec coverage:** REQ-001 正常/异常场景

- [ ] **RED:** Write 3 failing tests — 提交成功后跳转成功页；图片过大提示错误；必填项缺失阻止提交
- [ ] **GREEN:** Implement coach application page with form and upload
- [ ] **COMMIT:** `feat(miniapp-coach): add coach application page`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-5），P1 选做（Task 6-7）

## GWT Coverage Matrix

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 | Task 5 | Task 6 | Task 7 |
|---------|--------|--------|--------|--------|--------|--------|--------|
| §6.1 正常提交（正常） | ✅ | ✅ | ✅ | ✅ | ✅ | — | ✅ |
| §6.2 保存草稿（正常） | — | — | — | — | — | ✅ | — |
| §6.3 必填项缺失（异常） | — | ✅ | — | — | — | — | ✅ |
| §6.4 重复提交（异常） | — | — | — | ✅ | — | — | — |
| §6.5 证书图片过大（异常） | — | — | ✅ | — | — | — | ✅ |
| 参考单价超出范围（异常） | — | — | — | — | ✅ | — | — |
