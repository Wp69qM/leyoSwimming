# Tasks: US-012 教练管理个人主页与参考单价

> 本文档对应 `docs/stories/US-012-教练-管理个人主页与参考单价/test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: 个人主页查询接口 [P0]

**Files:**
- Create: `backend/src/controllers/coach/profile.ts`
- Create: `backend/src/routes/coach.ts`
- Test: `backend/tests/controllers/coach/profile.test.ts`

**Spec coverage:** REQ-001 Scenario "正常更新个人主页"（查询部分）

- [ ] **RED:** Write 2 failing tests — `POST /api/coach/profile/detail` 未登录返回 401；已登录且 status=1 的教练返回完整主页信息
- [ ] **GREEN:** Implement `POST /api/coach/profile/detail` controller skeleton with coach auth middleware and route registration
- [ ] **COMMIT:** `feat(coach): add POST /api/coach/profile/detail endpoint skeleton`

## Task 2: 个人主页更新接口 [P0]

**Files:**
- Modify: `backend/src/controllers/coach/profile.ts`
- Create: `backend/src/repositories/coach.ts`
- Test: `backend/tests/controllers/coach/profile.test.ts`

**Spec coverage:** REQ-001 Scenario "正常更新个人主页"

- [ ] **RED:** Write 2 failing tests — `POST /api/coach/profile/update` 更新简介成功；status≠1 返回 `COACH_STATUS_NOT_ALLOWED`
- [ ] **GREEN:** Implement `POST /api/coach/profile/update` with `name`、`gender`、`age`、`email`、`wechat_qr_url`、`portrait_url`、`teaching_years`、`teaching_strokes`、`bio` update and `coach.status = 1` check
- [ ] **COMMIT:** `feat(coach): add POST /api/coach/profile/update update endpoint`

## Task 3: 证书图片管理 [P0]

**Files:**
- Create: `backend/src/services/upload/image.ts`
- Create: `backend/src/repositories/coach_certificate.ts`
- Test: `backend/tests/services/upload/image.test.ts`

**Spec coverage:** REQ-001 Scenarios "正常更新个人主页", "证书图片过大"

- [ ] **RED:** Write 3 failing tests — 5MB 内 JPG 上传成功；8MB 图片返回 `IMAGE_TOO_LARGE`；非 JPG/PNG 返回 `INVALID_IMAGE_FORMAT`
- [ ] **GREEN:** Implement image size/format validator and `CoachCertificateRepository` insert/update/delete
- [ ] **COMMIT:** `feat(coach): add certificate image validation and repository`

## Task 4: 个人简介敏感词过滤 [P0]

**Files:**
- Create: `backend/src/services/coach/sensitive_filter.ts`
- Test: `backend/tests/services/coach/sensitive_filter.test.ts`

**Spec coverage:** REQ-001 Scenario "个人简介含敏感词"

- [ ] **RED:** Write 2 failing tests — 含敏感词简介返回 `SENSITIVE_CONTENT`；正常简介通过
- [ ] **GREEN:** Implement sensitive word filter service and integrate into `POST /api/coach/profile/update`
- [ ] **COMMIT:** `feat(coach): add bio sensitive content filter`

## Task 5: 主页变更审计日志 [P0]

**Files:**
- Create: `backend/src/repositories/coach_update_log.ts`
- Modify: `backend/src/controllers/coach/profile.ts`
- Test: `backend/tests/repositories/coach_update_log.test.ts`

**Spec coverage:** REQ-001 Scenario "正常更新个人主页"

- [ ] **RED:** Write 2 failing tests — 更新 bio 后 `coach_update_log` 新增记录；未实际变更字段不记日志
- [ ] **GREEN:** Implement `CoachUpdateLogRepository` and write log after successful profile update
- [ ] **COMMIT:** `feat(coach): add coach update audit log`

## Task 6: 参考单价更新接口 [P0]

**Files:**
- Modify: `backend/src/controllers/coach/profile.ts`
- Test: `backend/tests/controllers/coach/profile.test.ts`

**Spec coverage:** REQ-002 Scenario "正常更新参考单价"

- [ ] **RED:** Write 2 failing tests — `POST /api/coach/reference-price/update` 更新 350 元成功；未登录返回 401
- [ ] **GREEN:** Implement `POST /api/coach/reference-price/update` endpoint updating `reference_price`、`price_changed_at` and `price_change_count_today`
- [ ] **COMMIT:** `feat(coach): add POST /api/coach/reference-price/update endpoint`

## Task 7: 参考单价范围校验 [P0]

**Files:**
- Create: `backend/src/services/coach/reference_price.ts`
- Test: `backend/tests/services/coach/reference_price.test.ts`

**Spec coverage:** REQ-002 Scenario "参考单价超出范围"

- [ ] **RED:** Write 3 failing tests — 49 元返回 `INVALID_REFERENCE_PRICE`；2001 元返回 `INVALID_REFERENCE_PRICE`；50 元与 2000 元通过
- [ ] **GREEN:** Implement reference price validator (50-2000 inclusive) and integrate into update endpoint
- [ ] **COMMIT:** `feat(coach): add reference price range validation`

## Task 8: 参考单价改价频率限制 [P0]

**Files:**
- Modify: `backend/src/services/coach/reference_price.ts`
- Modify: `backend/src/controllers/coach/profile.ts`
- Test: `backend/tests/services/coach/reference_price.test.ts`

**Spec coverage:** REQ-002 Scenario "参考单价修改次数超限"

- [ ] **RED:** Write 2 failing tests — 第 4 次改价返回 `PRICE_CHANGE_LIMIT`；1 天内 3 次改价通过
- [ ] **GREEN:** Implement daily change count check using `price_change_count_today` and `price_changed_at`
- [ ] **COMMIT:** `feat(coach): add daily reference price change limit`

## Task 9: 缓存失效策略 [P1]

**Files:**
- Create: `backend/src/services/cache/coach_cache.ts`
- Modify: `backend/src/controllers/coach/profile.ts`
- Test: `backend/tests/services/cache/coach_cache.test.ts`

**Spec coverage:** REQ-001 Scenario "正常更新个人主页", REQ-002 Scenario "正常更新参考单价"

- [ ] **RED:** Write 2 failing tests — 主页更新后 `coach:{coach_id}` 缓存被删除；单价更新后 `coach:list:*` 缓存被清除
- [ ] **GREEN:** Implement cache invalidation service and call it after successful profile/price updates
- [ ] **COMMIT:** `feat(coach): add profile and price cache invalidation`

## Task 10: 教练端页面 [P1]

**Files:**
- Create: `miniapp-coach/src/pages/profile/index.tsx`
- Create: `miniapp-coach/src/pages/reference-price/index.tsx`
- Test: `miniapp-coach/src/pages/profile/index.test.tsx`

**Spec coverage:** REQ-001 Scenario "正常更新个人主页", REQ-002 Scenario "正常更新参考单价"

- [ ] **RED:** Write 3 failing tests — 个人主页编辑页提交成功跳转；参考单价页输入 5000 元提示范围错误；提交 350 元成功
- [ ] **GREEN:** Implement coach profile edit page and reference price setting page with form validation
- [ ] **COMMIT:** `feat(miniapp-coach): add profile and reference price pages`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-8），P1 选做（Task 9-10）

## GWT Coverage Matrix

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 | Task 5 | Task 6 | Task 7 | Task 8 | Task 9 | Task 10 |
|---------|--------|--------|--------|--------|--------|--------|--------|--------|--------|---------|
| 正常更新个人主页 | ✅ | ✅ | ✅ | — | ✅ | — | — | — | ✅ | ✅ |
| 正常更新参考单价 | — | — | — | — | — | ✅ | ✅ | ✅ | ✅ | ✅ |
| 参考单价超出范围 | — | — | — | — | — | — | ✅ | — | — | ✅ |
| 个人简介含敏感词 | — | — | — | ✅ | — | — | — | — | — | ✅ |
| 更新后学员端可见 | — | — | — | — | — | — | — | — | ✅ | — |
