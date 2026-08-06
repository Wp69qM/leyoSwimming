# Tasks: US-040 教练重新入驻

> 本文档对应 `docs/stories/US-040-教练-重新入驻/test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: 历史评分可见性数据模型扩展 [P0]

**Files:**
- Create: `backend/migrations/xxx_add_rating_visibility_to_new.ts`
- Test: `backend/tests/repositories/coach_rating.test.ts`

**Spec coverage:** REQ-002 / REQ-005

> **前置依赖**：本 US 依赖 US-041 产生 `coach.status = 3`；依赖 US-010 处理资料提交；依赖 US-011 提供/复用审核流程。

- [ ] **RED:** Write 1 failing test — `coach_rating` 表缺少 `is_visible_to_new` 字段时无法写入
- [ ] **GREEN:** Create Knex migration to add `is_visible_to_new` field (default true; set false when coach resigns)
- [ ] **COMMIT:** `feat(us-040): add is_visible_to_new to coach_rating for reapply`

## Task 2: 「我的」页面重新入驻入口与登录自动分流 [P0]

**Files:**
- Modify: `miniapp-coach/src/pages/profile/index.tsx`
- Modify: `miniapp-coach/src/app.tsx` 或登录回调处理逻辑
- Test: `miniapp-coach/src/pages/profile/index.test.tsx`
- Test: `miniapp-coach/src/app.test.tsx`

**Spec coverage:** REQ-001 Scenarios "已离职教练登录后自动进入 US-010 重新入驻", "非已离职教练无法重新入驻"

- [ ] **RED:** Write 3 failing tests — status=3 时展示「重新入驻」入口；status≠3 时入口隐藏/置灰；US-051 登录响应 coach_status=3 时前端跳转 US-010 C-入驻资料填写页
- [ ] **GREEN:** Implement profile page entry visibility and login callback routing
- [ ] **COMMIT:** `feat(miniapp-coach): add reapply entry and auto-redirect for resigned coach`

## Task 3: 重新入驻入口校验接口 [P0]

**Files:**
- Create: `backend/src/controllers/coach/reapply.ts`
- Create: `backend/src/routes/coach/reapply.ts`
- Test: `backend/tests/controllers/coach/reapply.test.ts`

**Spec coverage:** REQ-001 Scenario "已离职教练从「我的」页面进入重新入驻", REQ-003 Scenario "非已离职教练无法重新入驻", REQ-004 Scenario "重复发起重新入驻"

- [ ] **RED:** Write 4 failing tests — status=3 调用 `/api/coach/v1/reapply/entry` 返回 200 与 `redirect_to=coach_onboarding_page`；status=1 返回 403 `COACH_STATUS_NOT_ALLOWED`；status=0 返回 409 `REAPPLY_ALREADY_PENDING`；重复调用幂等
- [ ] **GREEN:** Implement `POST /api/coach/v1/reapply/entry` with status check and idempotency
- [ ] **REFACTOR:** Extract reapply entry service
- [ ] **COMMIT:** `feat(coach): add reapply entry validation endpoint`

## Task 4: US-010 处理重新入驻提交 [P0]

**Files:**
- Modify: `backend/src/controllers/coach/application.ts`（如 US-010 已完成则复用）
- Test: `backend/tests/controllers/coach/reapply.test.ts`

**Spec coverage:** REQ-001 Scenario "已离职教练登录后自动进入 US-010 重新入驻"

- [ ] **RED:** Write 2 failing tests — status=3 教练调用 `POST /api/coach/application` 创建 `previous_coach_status=3` 的 pending 快照；status=3 教练保存草稿后 coach.status 仍为 3
- [ ] **GREEN:** Reuse US-010 submission/draft endpoints for reapply (no new endpoint)
- [ ] **COMMIT:** `feat(coach): reapply submission via US-010 application endpoint`

## Task 5: US-011 审核重新入驻 [P0]

**Files:**
- Modify: `backend/src/controllers/admin/coach.ts`（如 US-011 已完成则复用）
- Test: `backend/tests/controllers/admin/coach_reapply_review.test.ts`

**Spec coverage:** REQ-002 Scenario "管理员通过重新入驻", REQ-005 Scenario "重新入驻审核被拒绝"

- [ ] **RED:** Write 3 failing tests — `POST /api/admin/coach/applications/{id}/approve` 对 `previous_coach_status=3` 的申请通过后 coach.status=1；`POST /api/admin/coach/applications/{id}/reject` 对 `previous_coach_status=3` 的申请拒绝后 coach.status=3；coach 表生效资料在拒绝前后保持不变
- [ ] **GREEN:** Reuse US-011 review endpoints for reapply approval/rejection
- [ ] **COMMIT:** `feat(admin): reapply review via US-011 audit endpoints`

## Task 6: 历史评分对新学员隐藏 [P1]

**Files:**
- Modify: `backend/src/services/coach/rating.ts`
- Test: `backend/tests/services/coach/rating.test.ts`

**Spec coverage:** REQ-002 Scenario "管理员通过重新入驻"（历史评分可见性部分）

- [ ] **RED:** Write 2 failing tests — 新学员查询教练主页不返回 `is_visible_to_new=false` 的历史评分；老学员仍可查看历史评分
- [ ] **GREEN:** Update rating queries to filter by `is_visible_to_new` for new students
- [ ] **COMMIT:** `feat(coach): hide historical ratings from new students after reapply`

## Task 7: 缓存失效与审计日志 [P1]

**Files:**
- Modify: `backend/src/services/coach/reapply.ts` 或 US-010/US-011 对应 service
- Test: `backend/tests/services/coach/reapply.test.ts`

**Spec coverage:** REQ-002 / REQ-005

- [ ] **RED:** Write 2 failing tests — 提交/审核后 `coach:status:{coach_id}` 与 `coach:profile:{coach_id}` 缓存失效；状态变更写入 `coach_audit_log`
- [ ] **GREEN:** Implement cache invalidation and audit log on reapply status change
- [ ] **COMMIT:** `feat(us-040): cache invalidation and audit log for reapply`

---

## Execution Discipline

- 推荐顺序：Task 1 → Task 3 → Task 4 → Task 5 → Task 6 → Task 7 → Task 2
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1, 3, 4, 5），P1 选做（Task 2, 6, 7）

## GWT Coverage Matrix

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 | Task 5 | Task 6 | Task 7 |
|---------|--------|--------|--------|--------|--------|--------|--------|
| §6.1 已离职教练进入 US-010 重新入驻 | — | ✅ | ✅ | ✅ | — | — | ✅ |
| §6.2 管理员通过重新入驻 | ✅ | — | — | — | ✅ | ✅ | ✅ |
| §6.3 非已离职教练无法重新入驻 | — | ✅ | ✅ | — | — | — | — |
| §6.4 重复发起重新入驻 | — | — | ✅ | — | — | — | — |
| §6.5 重新入驻审核被拒绝 | — | — | — | — | ✅ | — | ✅ |
