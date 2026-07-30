# Tasks: US-015 管理员配置预约释放规则

> 每个 Task 严格遵循 RED → GREEN → REFACTOR → COMMIT 循环。

## Task 1: ReleaseRule Repository — 读取与更新 [P0]

**Files:**
- Create: `backend/src/repositories/release-rule.ts`
- Test: `backend/tests/repositories/release-rule.test.ts`

**Spec coverage:** REQ-001 "正常查询默认规则", REQ-002 "正常更新释放时间"

- [ ] **RED:** Write failing tests — `get()` returns the single global row; `update()` persists valid changes
- [ ] **GREEN:** Implement `ReleaseRuleRepository.get()` and `update(dto, adminId)`
- [ ] **REFACTOR:** Extract default constant object; ensure single-row invariant
- [ ] **COMMIT:** `feat(release-rule): add repository get/update`

## Task 2: ReleaseRule Service — 业务校验 [P0]

**Files:**
- Create: `backend/src/services/release-rule.ts`
- Test: `backend/tests/services/release-rule.test.ts`

**Spec coverage:** REQ-002 "释放时间格式非法", "关注提醒时间超过释放周期"

- [ ] **RED:** Write failing tests — invalid time throws `INVALID_RELEASE_TIME`; reminder too long throws `REMINDER_TOO_LONG`
- [ ] **GREEN:** Implement `ReleaseRuleService.update()` with validation
- [ ] **REFACTOR:** Extract pure validation function
- [ ] **COMMIT:** `feat(release-rule): add service validation`

## Task 3: PUT /api/admin/release-rule [P0]

**Files:**
- Create: `backend/src/controllers/admin/release-rule.ts`
- Create: `backend/src/routes/admin/release-rule.ts`
- Test: `backend/tests/controllers/admin/release-rule.test.ts`

**Spec coverage:** REQ-002 all scenarios

- [ ] **RED:** Write failing tests — 200 for valid update; 400 for invalid time/reminder; 403 for no permission
- [ ] **GREEN:** Implement controller + Koa router with permission middleware
- [ ] **REFACTOR:** Share DTO schema between controller and service
- [ ] **COMMIT:** `feat(api): add PUT /api/admin/release-rule`

## Task 4: GET /api/admin/release-rule [P0]

**Files:**
- Modify: 同上 controller/route
- Test: 同上 test file

**Spec coverage:** REQ-001 "正常查询默认规则"

- [ ] **RED:** Write failing tests — 200 returns default rule; 403 for no permission
- [ ] **GREEN:** Implement GET handler
- [ ] **REFACTOR:** Reuse permission middleware
- [ ] **COMMIT:** `feat(api): add GET /api/admin/release-rule`

## Task 5: web-admin 配置页面 [P1]

**Files:**
- Create: `web-admin/src/pages/ReleaseRule/index.tsx`
- Test: `web-admin/src/pages/ReleaseRule/index.test.tsx`

**Spec coverage:** REQ-001/REQ-002 UI 验证

- [ ] **RED:** Write failing tests — form renders with defaults; submit calls PUT; validation errors show
- [ ] **GREEN:** Implement form page
- [ ] **REFACTOR:** Extract `WeekdayTimePicker`
- [ ] **COMMIT:** `feat(admin-ui): add release rule config page`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder（TBD / TODO）
- P0 必做（Task 1-4），P1 选做（Task 5）
