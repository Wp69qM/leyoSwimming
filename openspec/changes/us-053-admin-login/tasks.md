# Tasks: US-053 管理员账号密码登录

> 本文档对应 `docs/stories/US-053-.../test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: 后端管理员登录接口 [P0]

**Files:**
- Create/Update: `backend/src/controllers/admin/auth.ts`
- Create/Update: `backend/src/routes/admin/auth.ts`
- Create/Update: `backend/src/services/admin/session.ts`
- Create/Update: `backend/src/services/admin/user.ts`
- Test: `backend/tests/controllers/admin/login.test.ts`

**Spec coverage:** REQ-001 Scenarios "管理员正常登录", "用户名或密码错误", "管理员账号被禁用"

- [ ] **RED:** Write 3 failing tests — 正常登录成功返回单一 token（24h）；错误密码返回 401；禁用账号返回 403
- [ ] **GREEN:** Implement `POST /api/admin/auth/login` — 校验用户名/密码/状态，生成单一 JWT token（24h），记录日志
- [ ] **COMMIT:** `feat(admin-auth): add POST /api/admin/auth/login endpoint`

## Task 2: 后端管理员退出登录接口 [P0]

**Files:**
- Create/Update: `backend/src/controllers/admin/auth.ts`
- Create/Update: `backend/src/routes/admin/auth.ts`
- Create/Update: `backend/src/services/admin/session.ts`
- Test: `backend/tests/controllers/admin/logout.test.ts`

**Spec coverage:** REQ-002 Scenario "管理员退出登录"

- [ ] **RED:** Write failing test — 退出后 token 失效，再次访问受保护接口返回 401
- [ ] **GREEN:** Implement `POST /api/admin/auth/logout` — 校验 token，按 token hash 失效 admin_session
- [ ] **COMMIT:** `feat(admin-auth): add POST /api/admin/auth/logout endpoint`

## Task 3: Web 管理员登录页 + 路由守卫 [P0]

**Files:**
- Create/Update: `web-admin/src/pages/login/index.tsx`
- Create/Update: `web-admin/src/utils/auth.ts`
- Create/Update: `web-admin/src/router/guard.tsx`
- Test: `web-admin/src/pages/login/index.test.tsx`

**Spec coverage:** REQ-001 all scenarios

- [ ] **RED:** Write 3 failing tests — 登录成功存储 token + userInfo 并跳转后台首页；错误密码显示提示；token 过期/401 自动跳转登录页并提示"登录信息已过期，请重新登录"
- [ ] **GREEN:** Implement 登录页表单 + 路由守卫 + 401 请求拦截器 + localStorage 存储 token/userInfo
- [ ] **COMMIT:** `feat(web-admin): add admin login page and route guard`

## Task 4: 全局导航栏管理员信息/退出下拉菜单 [P0]

**Files:**
- Create/Update: `web-admin/src/components/layout/Header.tsx`
- Test: `web-admin/src/components/layout/Header.test.tsx`

**Spec coverage:** REQ-002 Scenario "管理员退出登录"

- [ ] **RED:** Write 2 failing tests — 点击名字展开下拉；点击退出清除 token + userInfo 跳转登录页
- [ ] **GREEN:** Implement Header 组件 + 下拉菜单 + 退出逻辑
- [ ] **COMMIT:** `feat(web-admin): add admin logout dropdown in header`

---

## Execution Discipline

- 严格顺序：Task 1 → Task 2 → Task 3 → Task 4
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-4）

## GWT Coverage Matrix

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 |
|---------|--------|--------|--------|--------|
| §6.1 正常登录 | ✅ | ✅ | ✅ | ✅ |
| §6.2 用户名或密码错误 | ✅ | — | ✅ | — |
| §6.3 账号被禁用 | ✅ | — | ✅ | — |
