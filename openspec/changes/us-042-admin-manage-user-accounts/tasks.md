> **OpenSpec Tasks | 映射自 `docs/stories/US-042-管理员-管理用户账号/test-plan.md`**
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: 后端用户列表/详情接口 [P0]

**Files:**
- Create/Update: `backend/src/controllers/admin/user.ts`
- Create/Update: `backend/src/routes/admin/user.ts`
- Create/Update: `backend/src/services/admin/user.ts`
- Test: `backend/tests/controllers/admin/user/list.test.ts`
- Test: `backend/tests/controllers/admin/user/detail.test.ts`

**Spec coverage:** REQ-001

- [ ] **RED:** Write failing tests — `POST /api/admin/user/list` 返回分页列表；`POST /api/admin/user/detail` 返回完整档案；无权限返回 403
- [ ] **GREEN:** Implement `POST /api/admin/user/list` with filtering and pagination — maps to REQ-001 / Scenario: List users with filters
- [ ] **GREEN:** Implement `POST /api/admin/user/detail` returning full profile — maps to REQ-001 / Scenario: View user details
- [ ] **COMMIT:** `feat(us-042): add admin user list and detail APIs`

## Task 2: 后端手动新建用户接口 [P0]

**Files:**
- Create/Update: `backend/src/controllers/admin/user.ts`
- Create/Update: `backend/src/services/admin/user.ts`
- Test: `backend/tests/controllers/admin/user/add.test.ts`

**Spec coverage:** REQ-002

- [ ] **RED:** Write failing tests — 新建成功返回 201；手机号重复返回 409；未成年人缺监护人返回 400
- [ ] **GREEN:** Implement `POST /api/admin/user/add` with phone uniqueness and guardian validation — maps to REQ-002 / Scenario: Create user successfully
- [ ] **COMMIT:** `feat(us-042): add admin user creation API`

## Task 3: 后端编辑用户资料接口 [P0]

**Files:**
- Create/Update: `backend/src/controllers/admin/user.ts`
- Create/Update: `backend/src/services/admin/user.ts`
- Test: `backend/tests/controllers/admin/user/update.test.ts`

**Spec coverage:** REQ-003

- [ ] **RED:** Write failing tests — 编辑成功；并发编辑返回 `USER_CONCURRENTLY_UPDATED`；目标不存在返回 404
- [ ] **GREEN:** Implement `POST /api/admin/user/update` with optimistic lock — maps to REQ-003 / Scenario: Update user profile successfully
- [ ] **COMMIT:** `feat(us-042): add admin user update API with optimistic lock`

## Task 4: 后端封禁/解封接口 [P0]

**Files:**
- Create/Update: `backend/src/controllers/admin/user.ts`
- Create/Update: `backend/src/services/admin/user.ts`
- Test: `backend/tests/controllers/admin/user/ban.test.ts`
- Test: `backend/tests/controllers/admin/user/unban.test.ts`

**Spec coverage:** REQ-004

- [ ] **RED:** Write failing tests — 封禁成功 status=2；解封成功 status=0；无权限返回 403
- [ ] **GREEN:** Implement `POST /api/admin/user/ban` — maps to REQ-004 / Scenario: Ban user account successfully
- [ ] **GREEN:** Implement `POST /api/admin/user/unban` — maps to REQ-004 / Scenario: Unban user account successfully
- [ ] **COMMIT:** `feat(us-042): add admin user ban/unban APIs`

## Task 5: 权限校验与审计日志 [P0]

**Files:**
- Create/Update: `backend/src/middleware/admin-rbac.ts`
- Create/Update: `backend/src/services/admin/audit-log.ts`
- Test: `backend/tests/middleware/admin-rbac.test.ts`

**Spec coverage:** REQ-005

- [ ] **GREEN:** Verify `USER:READ` / `USER:WRITE` / `USER:BAN` permissions on endpoints — maps to REQ-005 / Scenario: Admin without permission is denied
- [ ] **GREEN:** Write `audit_log` entries for add/update/ban/unban operations
- [ ] **COMMIT:** `feat(us-042): add RBAC and audit logging for user management`

## Task 6: Web 用户账号列表页与查看弹窗 [P0]

**Files:**
- Create/Update: `web-admin/src/pages/user/account/index.tsx`
- Create/Update: `web-admin/src/pages/user/account/ViewModal.tsx`
- Test: `web-admin/src/pages/user/account/index.test.tsx`

**Spec coverage:** REQ-001

- [ ] **RED:** Write failing tests — 列表渲染、筛选、分页、点击查看弹窗
- [ ] **GREEN:** Build user account list page with filters and view modal
- [ ] **COMMIT:** `feat(web-admin): add user account list and view modal`

## Task 7: Web 新建/编辑用户弹窗 [P0]

**Files:**
- Create/Update: `web-admin/src/pages/user/account/CreateModal.tsx`
- Create/Update: `web-admin/src/pages/user/account/EditModal.tsx`
- Test: `web-admin/src/pages/user/account/CreateModal.test.tsx`
- Test: `web-admin/src/pages/user/account/EditModal.test.tsx`

**Spec coverage:** REQ-002 / REQ-003

- [ ] **RED:** Write failing tests — 新建/编辑表单提交、校验提示
- [ ] **GREEN:** Build create and edit user modals with guardian validation
- [ ] **COMMIT:** `feat(web-admin): add user create and edit modals`

## Task 8: Web 封禁/解封交互 [P0]

**Files:**
- Create/Update: `web-admin/src/pages/user/account/index.tsx`
- Create/Update: `web-admin/src/pages/user/account/BanModal.tsx`
- Test: `web-admin/src/pages/user/account/BanModal.test.tsx`

**Spec coverage:** REQ-004

- [ ] **RED:** Write failing tests — 点击封禁/解封按钮弹出确认框、填写原因后调用 API
- [ ] **GREEN:** Build ban/unban confirmation modal with reason input
- [ ] **COMMIT:** `feat(web-admin): add user ban/unban modal`

## Task 9: 验证

- [ ] **9.1** Run integration tests for all 6 GWT scenarios
- [ ] **9.2** Run `openspec validate us-042-admin-manage-user-accounts --json` and fix issues
