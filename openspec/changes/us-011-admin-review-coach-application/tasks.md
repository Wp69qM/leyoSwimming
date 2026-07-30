# Tasks: US-011 管理员审核教练入驻资质

> 本文档对应 `docs/stories/US-011-管理员-审核教练入驻资质/test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: 待审核列表查询 [P0]

**Files:**
- Create: `backend/src/controllers/admin/coach.ts`
- Create: `backend/src/routes/admin/coach.ts`
- Test: `backend/tests/controllers/admin/coach.test.ts`

**Spec coverage:** REQ-003 Scenario "查询待审核列表"

- [ ] **RED:** Write 2 failing tests — 有权限管理员返回 status=0 列表；无权限返回 403
- [ ] **GREEN:** Implement `GET /api/admin/coach/applications` with RBAC and status filter
- [ ] **COMMIT:** `feat(admin): add pending coach applications list`

## Task 2: 审核通过 [P0]

**Files:**
- Modify: `backend/src/controllers/admin/coach.ts`
- Create: `backend/src/services/admin/coach_audit.ts`
- Test: `backend/tests/services/admin/coach_audit.test.ts`

**Spec coverage:** REQ-001 Scenario "审核通过"

- [ ] **RED:** Write 2 failing tests — 待审核记录通过返回 status=1；非待审核记录返回 `NOT_PENDING`
- [ ] **GREEN:** Implement approve service with state machine validation
- [ ] **COMMIT:** `feat(admin): add coach application approve`

## Task 3: 审核驳回 [P0]

**Files:**
- Modify: `backend/src/controllers/admin/coach.ts`
- Modify: `backend/src/services/admin/coach_audit.ts`
- Test: `backend/tests/services/admin/coach_audit.test.ts`

**Spec coverage:** REQ-002 Scenario "审核驳回"

- [ ] **RED:** Write 2 failing tests — 待审核记录驳回返回 status=2 及原因；未填原因返回 `MISSING_REJECTION_REASON`
- [ ] **GREEN:** Implement reject service with reason validation
- [ ] **COMMIT:** `feat(admin): add coach application reject`

## Task 4: 权限校验 [P0]

**Files:**
- Create: `backend/src/middleware/rbac.ts`
- Modify: `backend/src/routes/admin/coach.ts`
- Test: `backend/tests/middleware/rbac.test.ts`

**Spec coverage:** REQ-001 Scenario "无权限审核"

- [ ] **RED:** Write 2 failing tests — 无 `coach:audit` 权限访问列表/审核接口返回 403；有权限通过
- [ ] **GREEN:** Implement RBAC middleware checking `coach:audit`
- [ ] **COMMIT:** `feat(auth): add RBAC middleware for coach audit`

## Task 5: 状态机校验 [P0]

**Files:**
- Modify: `backend/src/services/admin/coach_audit.ts`
- Test: `backend/tests/services/admin/coach_audit.test.ts`

**Spec coverage:** REQ-002 Scenario "重复审核"

- [ ] **RED:** Write 2 failing tests — 已通过记录再次通过返回 `ALREADY_REVIEWED`；已驳回记录再次驳回返回 `ALREADY_REVIEWED`
- [ ] **GREEN:** Add state machine guard allowing only 0 → 1/2
- [ ] **COMMIT:** `feat(admin): enforce coach audit state machine`

## Task 6: 发送审核通知 [P1]

**Files:**
- Create: `backend/src/services/notification.ts`
- Modify: `backend/src/services/admin/coach_audit.ts`
- Test: `backend/tests/services/notification.test.ts`

**Spec coverage:** REQ-001 / REQ-002 通知部分

- [ ] **RED:** Write 2 failing tests — 通过时发送通过通知；驳回时发送含原因通知
- [ ] **GREEN:** Implement async notification service for audit results
- [ ] **COMMIT:** `feat(notification): send coach audit notifications`

## Task 7: Web 管理后台审核页面 [P1]

**Files:**
- Create: `web-admin/src/pages/coach/applications/index.tsx`
- Create: `web-admin/src/pages/coach/applications/detail.tsx`
- Test: `web-admin/src/pages/coach/applications/index.test.tsx`

**Spec coverage:** REQ-001 / REQ-002 / REQ-003 正常场景

- [ ] **RED:** Write 3 failing tests — 列表展示待审核记录；通过按钮更新状态；驳回需填原因
- [ ] **GREEN:** Implement admin coach audit list and detail pages
- [ ] **COMMIT:** `feat(web-admin): add coach application audit pages`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-5），P1 选做（Task 6-7）

## GWT Coverage Matrix

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 | Task 5 | Task 6 | Task 7 |
|---------|--------|--------|--------|--------|--------|--------|--------|
| §6.1 审核通过（正常） | — | ✅ | — | ✅ | ✅ | ✅ | ✅ |
| §6.2 审核驳回（正常） | — | — | ✅ | ✅ | ✅ | ✅ | ✅ |
| §6.3 无权限审核（异常） | ✅ | — | — | ✅ | — | — | — |
| 重复审核（边界） | — | — | — | — | ✅ | — | — |
| 待审核列表查询 | ✅ | — | — | ✅ | — | — | ✅ |
