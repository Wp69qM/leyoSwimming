# US-009 用户/教练隐私协议与用户须知授权 — 测试计划

> **状态**：已评审　|　**最后更新**：2026-08-08

---

## 1. 测试目标

验证隐私协议与用户须知授权功能：
- 游客/未登录用户可查询当前生效的《用户须知》和《隐私协议》内容
- 登录成功后系统记录用户/教练同意的协议版本
- 用户/教练可在设置页查看协议内容，但不可撤回授权
- 协议版本更新后，未登录用户再次登录需重新同意

---

## 2. 测试范围

- **后端**：
  - `POST /api/common/terms/current`
  - `POST /api/common/privacy/current`
  - `POST /api/user/terms/status`
  - `POST /api/user/privacy/status`
  - `POST /api/user/terms/consent`
  - `POST /api/user/privacy/consent`
- **前端**：
  - 登录页协议浮层弹窗（同意/不同意/关闭）
  - 「我的 → 设置 → 用户须知/隐私协议」查看页

---

## 3. TDD 任务清单

### Task 1：当前协议查询接口 [P0]

- **RED**：编写测试 — 存在当前《用户须知》/《隐私协议》时返回版本与内容；无当前版本时返回 404 `NO_CURRENT_TERMS_POLICY` / `NO_CURRENT_PRIVACY_POLICY`；游客未登录可访问。
- **GREEN**：实现 `POST /api/common/terms/current` 与 `POST /api/common/privacy/current`。
- **COMMIT**：`feat(common): add current terms and privacy policy endpoints`

### Task 2：用户授权状态查询 [P0]

- **RED**：编写测试 — 游客访问返回 401；未同意用户返回 `none` + `required_version`；已同意当前版本用户返回 `agreed` + 版本号。
- **GREEN**：实现 `POST /api/user/terms/status` 与 `POST /api/user/privacy/status`。
- **COMMIT**：`feat(privacy): add user terms and privacy status endpoints`

### Task 3：同意协议 [P0]

- **RED**：编写测试 — 登录用户同意当前版本《用户须知》/《隐私协议》返回 200 并写入 `status='agreed'`；非当前版本返回 `VERSION_MISMATCH`；重复同意当前版本幂等返回 200。
- **GREEN**：实现 `POST /api/user/terms/consent` 与 `POST /api/user/privacy/consent`。
- **COMMIT**：`feat(privacy): add terms and privacy consent agreement endpoints`

### Task 4：版本更新后重新同意 / 授权拦截 [P0]

- **RED**：编写测试 — 已同意旧版本的用户访问需授权功能时被拦截；同意当前版本后可正常访问。
- **GREEN**：实现协议版本比对中间件并在需授权功能路由上复用。
- **COMMIT**：`feat(privacy): enforce latest terms and privacy consent on protected routes`

### Task 5：小程序登录页协议浮层弹窗 [P1]

- **RED**：编写 E2E 测试 — 点击协议名唤起浮层；点击「同意」后勾选框变为已勾选并记录同意版本；点击「不同意」后勾选框未勾选并阻止登录；点击浮层外部关闭浮层且状态不变。
- **GREEN**：实现登录页协议浮层弹窗组件。
- **COMMIT**：`feat(miniapp): add login protocol modal with agree/disagree`

### Task 6：小程序协议查看页 [P1]

- **RED**：编写 E2E 测试 — 从「我的 → 设置」进入「用户须知」/「隐私协议」查看页，展示当前协议内容，页面无「撤回授权」按钮。
- **GREEN**：实现设置页协议查看页。
- **COMMIT**：`feat(miniapp): add terms and privacy view pages in settings`

---

## 4. 测试用例映射

| 场景 | 测试方法 | 层级 |
|------|---------|------|
| 登录页点击协议名并同意后成功登录 | `test_login_protocol_agree_and_login` | E2E |
| 登录页不同意协议导致无法登录 | `test_login_protocol_disagree_blocks_login` | E2E |
| 登录后查看用户须知和隐私协议 | `test_view_terms_and_privacy_in_settings` | E2E |
| 教练端登录页点击协议名并同意后进入教练端 | `test_coach_login_protocol_agree` | E2E |
| 点击勾选框或外层文字直接切换勾选状态 | `test_protocol_checkbox_toggle` | E2E |
| 点击浮层外部关闭浮层且状态不变 | `test_protocol_modal_dismiss_keep_state` | E2E |
| 当前协议查询 | `test_api_current_terms_and_privacy` | 集成 |
| 游客态查询用户授权状态返回 401 | `test_api_terms_status_401_guest` / `test_api_privacy_status_401_guest` | 集成 |
| 同意当前版本协议 | `test_api_terms_consent_agree` / `test_api_privacy_consent_agree` | 集成 |
| 非当前版本同意返回 VERSION_MISMATCH | `test_api_terms_consent_version_mismatch` / `test_api_privacy_consent_version_mismatch` | 集成 |
| 协议版本更新后未重新同意被拦截 | `test_api_version_mismatch_blocks_protected_route` | 集成 |

---

## 5. Mock / 样本数据

- **当前生效协议**：`terms_policy.version = 'v2.0'`, `privacy_policy.version = 'v2.0'`
- **历史协议版本**：`v1.0`

---

## 6. 验收标准映射

| GWT 场景 | 覆盖 Task | 测试方法 |
|----------|----------|----------|
| §6.1 登录页点击协议名并同意后成功登录 | Task 3, 5 | `test_login_protocol_agree_and_login` |
| §6.2 登录页不同意协议导致无法登录 | Task 5 | `test_login_protocol_disagree_blocks_login` |
| §6.3 登录后查看用户须知和隐私协议 | Task 6 | `test_view_terms_and_privacy_in_settings` |
| §6.4 教练端登录页点击协议名并同意后进入教练端 | Task 5 | `test_coach_login_protocol_agree` |
| §6.5 点击勾选框或外层文字直接切换勾选状态 | Task 5 | `test_protocol_checkbox_toggle` |
| §6.6 点击浮层外部关闭浮层且状态不变 | Task 5 | `test_protocol_modal_dismiss_keep_state` |

---

## 7. 验收执行检查单

- [ ] 单元测试全部通过
- [ ] 集成测试全部通过
- [ ] 所有 GWT 场景均有对应自动化测试
- [ ] 覆盖率：service 层 ≥ 80%，validator 层 100%
- [ ] 无 TBD/TODO 遗留
