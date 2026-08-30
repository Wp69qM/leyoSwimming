> **OpenSpec Tasks | 映射自 `docs/stories/US-057-管理员-管理管理员账号/test-plan.md`**
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: 后端管理员账号列表/详情接口 [P0]

**Files:**
- Create/Update: `backend/src/controllers/admin/admin-user.ts`
- Create/Update: `backend/src/routes/admin/admin-user.ts`
- Create/Update: `backend/src/services/admin/admin-user.ts`
- Test: `backend/tests/controllers/admin/admin-user/list.test.ts`
- Test: `backend/tests/controllers/admin/admin-user/detail.test.ts`

**Spec coverage:** REQ-001

- [ ] **RED:** Write failing tests — `POST /api/admin/admin/list` 返回分页列表；`admin` 仅返回 VIEW；`POST /api/admin/admin/detail` 返回详情
- [ ] **GREEN:** Implement `POST /api/admin/admin/list` with filtering and pagination — maps to REQ-001
- [ ] **GREEN:** Implement `POST /api/admin/admin/detail` returning admin profile — maps to REQ-001
- [ ] **COMMIT:** `feat(us-057): add admin account list and detail APIs`

## Task 2: 后端新建/编辑管理员接口 [P0]

**Files:**
- Create/Update: `backend/src/controllers/admin/admin-user.ts`
- Create/Update: `backend/src/services/admin/admin-user.ts`
- Test: `backend/tests/controllers/admin/admin-user/add.test.ts`
- Test: `backend/tests/controllers/admin/admin-user/update.test.ts`

**Spec coverage:** REQ-002 / REQ-003

- [ ] **RED:** Write failing tests — 新建成功；登录账号重复返回 400；编辑成功；不允许修改 username
- [ ] **GREEN:** Implement `POST /api/admin/admin/add` with username uniqueness and password hashing — maps to REQ-002
- [ ] **GREEN:** Implement `POST /api/admin/admin/update` — maps to REQ-003
- [ ] **COMMIT:** `feat(us-057): add admin account add and update APIs`

## Task 3: 后端禁用/删除管理员接口 [P0]

**Files:**
- Create/Update: `backend/src/controllers/admin/admin-user.ts`
- Create/Update: `backend/src/services/admin/admin-user.ts`
- Test: `backend/tests/controllers/admin/admin-user/toggle-status.test.ts`
- Test: `backend/tests/controllers/admin/admin-user/delete.test.ts`

**Spec coverage:** REQ-004 / REQ-005 / REQ-007

- [ ] **RED:** Write failing tests — 禁用成功且会话失效；删除成功；自删除/自禁用/最后 super_admin 保护返回对应错误码
- [ ] **GREEN:** Implement `POST /api/admin/admin/toggle-status` with session invalidation — maps to REQ-004
- [ ] **GREEN:** Implement `POST /api/admin/admin/delete` with soft delete and session invalidation — maps to REQ-005
- [ ] **COMMIT:** `feat(us-057): add admin account toggle-status and delete APIs`

## Task 4: 后端重置密码接口 [P0]

**Files:**
- Create/Update: `backend/src/controllers/admin/admin-user.ts`
- Create/Update: `backend/src/services/admin/admin-user.ts`
- Test: `backend/tests/controllers/admin/admin-user/reset-password.test.ts`

**Spec coverage:** REQ-006

- [ ] **RED:** Write failing tests — 重置成功返回明文临时密码；目标不存在返回 404
- [ ] **GREEN:** Implement `POST /api/admin/admin/reset-password` with random strong password — maps to REQ-006
- [ ] **COMMIT:** `feat(us-057): add admin account reset-password API`

## Task 5: 权限校验与审计日志 [P0]

**Files:**
- Create/Update: `backend/src/middleware/admin-rbac.ts`
- Create/Update: `backend/src/services/admin/admin-audit-log.ts`
- Test: `backend/tests/middleware/admin-rbac.test.ts`

**Spec coverage:** REQ-007

- [ ] **GREEN:** Enforce `super_admin` for write endpoints and `admin` read-only restriction
- [ ] **GREEN:** Write `admin_audit_log` entries for all write operations
- [ ] **COMMIT:** `feat(us-057): add RBAC and admin audit logging`

## Task 6: Web 管理员账号管理列表页 [P0]

**Files:**
- Create/Update: `web-admin/src/pages/system/admin/index.tsx`
- Create/Update: `web-admin/src/pages/system/admin/ViewModal.tsx`
- Test: `web-admin/src/pages/system/admin/index.test.tsx`

**Spec coverage:** REQ-001

- [ ] **RED:** Write failing tests — 列表渲染、筛选、分页、点击查看弹窗
- [ ] **GREEN:** Build admin account list page with filters and view modal
- [ ] **COMMIT:** `feat(web-admin): add admin account list and view modal`

## Task 7: Web 新建/编辑管理员弹窗 [P0]

**Files:**
- Create/Update: `web-admin/src/pages/system/admin/CreateModal.tsx`
- Create/Update: `web-admin/src/pages/system/admin/EditModal.tsx`
- Test: `web-admin/src/pages/system/admin/CreateModal.test.tsx`
- Test: `web-admin/src/pages/system/admin/EditModal.test.tsx`

**Spec coverage:** REQ-002 / REQ-003

- [ ] **RED:** Write failing tests — 新建/编辑表单提交、校验提示
- [ ] **GREEN:** Build create and edit admin modals
- [ ] **COMMIT:** `feat(web-admin): add admin create and edit modals`

## Task 8: Web 禁用/删除/重置密码交互 [P0]

**Files:**
- Create/Update: `web-admin/src/pages/system/admin/index.tsx`
- Create/Update: `web-admin/src/pages/system/admin/ResetPasswordModal.tsx`
- Test: `web-admin/src/pages/system/admin/action-buttons.test.tsx`

**Spec coverage:** REQ-004 / REQ-005 / REQ-006

- [ ] **RED:** Write failing tests — 禁用/删除二次确认、重置密码后展示临时密码
- [ ] **GREEN:** Build toggle-status, delete confirmation, and reset-password interactions
- [ ] **COMMIT:** `feat(web-admin): add admin account actions UI`

## Task 9: 验证

- [ ] **9.1** Run integration tests for all 8 GWT scenarios
- [ ] **9.2** Run `openspec validate us-057-admin-manage-admin-accounts --json` and fix issues
