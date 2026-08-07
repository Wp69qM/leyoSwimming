# Tasks: US-040 教练重新入驻

> 本文档对应 `docs/stories/US-040-教练-重新入驻/test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: 登录自动分流与重新入驻提交 [P0]

**Files:**
- Modify: `miniapp-coach/src/app.tsx` 或登录回调处理逻辑
- Modify: `miniapp-coach/src/pages/onboarding/index.tsx`（US-010 C-入驻资料填写页，新增重新入驻说明条）
- Test: `miniapp-coach/src/app.test.tsx`
- Test: `miniapp-coach/src/pages/onboarding/index.test.tsx`

**Spec coverage:** REQ-001 Scenario "已离职教练登录后自动进入 US-010 重新入驻"

> **前置依赖**：本 US 依赖 US-041 产生 `coach.status = 3`；依赖 US-010 处理资料提交；依赖 US-011 提供/复用审核流程。

- [ ] **RED:** Write 2 failing tests — US-051 / US-054 登录响应 `coach_status=3` 时前端跳转 US-010 C-入驻资料填写页；status≠3 时登录后不按重新入驻流程跳转
- [ ] **GREEN:** Implement login callback routing split and onboarding page reapply prompt
- [ ] **COMMIT:** `feat(miniapp-coach): auto-redirect resigned coach to reapply onboarding`

## Task 2: US-010 处理重新入驻提交 [P0]

**Files:**
- Modify: `backend/src/controllers/coach/application.ts`（如 US-010 已完成则复用）
- Test: `backend/tests/controllers/coach/reapply.test.ts`

**Spec coverage:** REQ-001 Scenario "已离职教练登录后自动进入 US-010 重新入驻"

- [ ] **RED:** Write 2 failing tests — status=3 教练调用 `POST /api/coach/application` 创建 `previous_coach_status=3` 的 pending 快照；status=3 教练保存草稿后 coach.status 仍为 3
- [ ] **GREEN:** Reuse US-010 submission/draft endpoints for reapply (no new endpoint)
- [ ] **COMMIT:** `feat(coach): reapply submission via US-010 application endpoint`

## Task 3: US-011 审核重新入驻 [P0]

**Files:**
- Modify: `backend/src/controllers/admin/coach.ts`（如 US-011 已完成则复用）
- Test: `backend/tests/controllers/admin/coach_reapply_review.test.ts`

**Spec coverage:** REQ-002 Scenario "管理员通过重新入驻", REQ-005 Scenario "重新入驻审核被拒绝"

- [ ] **RED:** Write 3 failing tests — `POST /api/admin/coach/applications/{id}/approve` 对 `previous_coach_status=3` 的申请通过后 coach.status=1；`POST /api/admin/coach/applications/{id}/reject` 对 `previous_coach_status=3` 的申请拒绝后 coach.status=3；coach 表生效资料在拒绝前后保持不变
- [ ] **GREEN:** Reuse US-011 review endpoints for reapply approval/rejection
- [ ] **COMMIT:** `feat(admin): reapply review via US-011 audit endpoints`

## Task 4: 缓存失效与审计日志 [P1]

**Files:**
- Modify: `backend/src/services/coach/reapply.ts` 或 US-010/US-011 对应 service
- Test: `backend/tests/services/coach/reapply.test.ts`

**Spec coverage:** REQ-002 / REQ-005

- [ ] **RED:** Write 2 failing tests — 提交/审核后 `coach:status:{coach_id}` 与 `coach:profile:{coach_id}` 缓存失效；状态变更写入 `coach_audit_log`
- [ ] **GREEN:** Implement cache invalidation and audit log on reapply status change
- [ ] **COMMIT:** `feat(us-040): cache invalidation and audit log for reapply`

---

## Execution Discipline

- 推荐顺序：Task 1 → Task 2 → Task 3 → Task 4
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1, 2, 3），P1 选做（Task 4）

## GWT Coverage Matrix

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 |
|---------|--------|--------|--------|--------|
| §6.1 已离职教练进入 US-010 重新入驻 | ✅ | ✅ | — | ✅ |
| §6.2 管理员通过重新入驻 | — | — | ✅ | ✅ |
| §6.3 非已离职教练无法重新入驻 | ✅ | ✅ | — | — |
| §6.4 重复发起重新入驻 | — | ✅ | — | — |
| §6.5 重新入驻审核被拒绝 | — | — | ✅ | ✅ |
