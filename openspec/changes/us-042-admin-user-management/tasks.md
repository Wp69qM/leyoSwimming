> **OpenSpec Tasks | 映射自 `docs/stories/US-042-管理员-管理用户账号/user-story.md` §? 与配套 test-plan.md**

## Task 1: 用户列表与详情查询 [P0]

- RED: 测试 `GET /api/admin/v1/users` 分页、筛选（身份/状态/注册时间/关键字）；测试 `GET /api/admin/v1/users/{id}` 返回详情含监护人信息脱敏
- GREEN: 实现用户查询服务与 RBAC 数据权限控制
- COMMIT: `feat(admin): add user list and detail query`

## Task 2: 重置密码 [P0]

- RED: 测试 `POST /api/admin/v1/users/{id}/reset-password` 生成随机密码，`force_change_password=true`，记录 audit_log
- GREEN: 实现密码重置服务
- COMMIT: `feat(admin): add admin reset user password`

## Task 3: 修改手机号与邮箱 [P0]

- RED: 测试手机号修改：短信验证通过、强制变更并记录原因、手机号冲突、无权限；测试邮箱修改与冲突
- GREEN: 实现手机号/邮箱修改服务
- COMMIT: `feat(admin): add admin update user phone and email`

## Task 4: 角色权限配置 [P0]

- RED: 测试角色分配、admin 不可授予 super_admin 角色、admin 不可修改 super_admin 账号
- GREEN: 实现角色分配服务与权限校验
- COMMIT: `feat(admin): add admin assign user roles`

## Task 5: 封禁/解封账号 [P0]

- RED: 测试封禁/解封接口更新 `user.status`，记录原因与 audit_log
- GREEN: 实现账号封禁/解封服务
- COMMIT: `feat(admin): add admin ban/unban user`

## Task 6: 管理后台用户管理页面 [P1]

- RED: E2E 测试用户列表筛选、详情查看、重置密码、修改手机号/邮箱、角色配置、封禁/解封交互
- GREEN: 实现 `web-admin/src/pages/users/index.tsx` 与详情/编辑弹窗
- COMMIT: `feat(web-admin): add user management page`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-5），P1 选做（Task 6）

## GWT Coverage Matrix

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 | Task 5 | Task 6 |
|---------|--------|--------|--------|--------|--------|--------|
| §6.1 重置密码 | — | ✅ | — | — | — | ✅ |
| §6.2 短信验证修改手机号 | — | — | ✅ | — | — | ✅ |
| §6.3 强制变更手机号 | — | — | ✅ | — | — | ✅ |
| §6.4 封禁账号 | — | — | — | — | ✅ | ✅ |
| §6.5 无权限操作 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| §6.6 目标用户不存在 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| §6.7 手机号已被占用 | — | — | ✅ | — | — | ✅ |
