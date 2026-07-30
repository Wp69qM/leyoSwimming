# Tasks: US-009 用户隐私协议授权与撤回

> 本文档对应 `docs/stories/US-009-用户-隐私协议授权与撤回/test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: 当前协议查询接口 [P0]

**Files:**
- Create: `backend/src/controllers/privacy_policy.ts`
- Create: `backend/src/routes/privacy_policy.ts`
- Create: `backend/src/repositories/privacy_policy.ts`
- Test: `backend/tests/controllers/privacy_policy.test.ts`

**Spec coverage:** REQ-003 Scenario "协议版本更新后未重新同意"

- [ ] **RED:** Write 2 failing tests — 存在当前协议返回版本内容；无当前协议返回 404
- [ ] **GREEN:** Implement `GET /api/privacy-policy/current` with current version lookup
- [ ] **COMMIT:** `feat(privacy): add current privacy policy endpoint`

## Task 2: 用户授权状态查询 [P0]

**Files:**
- Create: `backend/src/controllers/user/privacy.ts`
- Create: `backend/src/routes/user/privacy.ts`
- Create: `backend/src/repositories/user_privacy_consent.ts`
- Test: `backend/tests/controllers/user/privacy.test.ts`

**Spec coverage:** REQ-003 Scenario "协议版本更新后未重新同意"

- [ ] **RED:** Write 3 failing tests — 未同意返回 `none`；已同意返回版本；已撤回返回 `revoked`
- [ ] **GREEN:** Implement `GET /api/user/privacy/status` with current policy comparison
- [ ] **COMMIT:** `feat(privacy): add user privacy status endpoint`

## Task 3: 同意协议 [P0]

**Files:**
- Modify: `backend/src/controllers/user/privacy.ts`
- Test: `backend/tests/controllers/user/privacy.test.ts`

**Spec coverage:** REQ-001 Scenario "正常同意隐私协议"

- [ ] **RED:** Write 2 failing tests — 同意当前版本返回 200 并写入记录；非当前版本返回 `VERSION_MISMATCH`
- [ ] **GREEN:** Implement `POST /api/user/privacy/consent` with action='agree'
- [ ] **COMMIT:** `feat(privacy): add privacy consent agreement endpoint`

## Task 4: 撤回授权 [P0]

**Files:**
- Modify: `backend/src/controllers/user/privacy.ts`
- Modify: `backend/src/repositories/user_privacy_consent.ts`
- Test: `backend/tests/controllers/user/privacy.test.ts`

**Spec coverage:** REQ-002 Scenario "撤回隐私授权"

- [ ] **RED:** Write 2 failing tests — 已同意用户撤回成功；未同意用户撤回返回 `ALREADY_REVOKED`
- [ ] **GREEN:** Implement `POST /api/user/privacy/consent` with action='revoke'
- [ ] **COMMIT:** `feat(privacy): add privacy consent revoke endpoint`

## Task 5: 版本更新后重新同意 [P0]

**Files:**
- Modify: `backend/src/middleware/privacy_consent.ts`
- Modify: `backend/src/routes/*`（需授权功能路由）
- Test: `backend/tests/middleware/privacy_consent.test.ts`

**Spec coverage:** REQ-003 Scenario "协议版本更新后未重新同意"；REQ-002 Scenario "撤回授权后访问需授权功能"

- [ ] **RED:** Write 2 failing tests — 版本不一致访问购买接口被拦截；已撤回访问购买接口被拦截
- [ ] **GREEN:** Implement privacy consent middleware blocking unauthorized functions
- [ ] **COMMIT:** `feat(privacy): enforce privacy consent on protected routes`

## Task 6: 小程序隐私协议页 [P1]

**Files:**
- Create: `miniapp-user/src/pages/privacy/index.tsx`
- Test: `miniapp-user/src/pages/privacy/index.test.tsx`

**Spec coverage:** REQ-001 Scenarios "正常同意隐私协议", "不同意隐私协议"

- [ ] **RED:** Write 2 failing tests — 同意协议后进入首页；不同意协议无法进入首页
- [ ] **GREEN:** Implement privacy policy page with agree/disagree handling
- [ ] **COMMIT:** `feat(miniapp): add privacy policy page`

## Task 7: 小程序隐私设置页 [P1]

**Files:**
- Create: `miniapp-user/src/pages/privacy/settings.tsx`
- Test: `miniapp-user/src/pages/privacy/settings.test.tsx`

**Spec coverage:** REQ-002 Scenario "撤回隐私授权"

- [ ] **RED:** Write 1 failing test — 点击撤回授权后状态更新并提示影响
- [ ] **GREEN:** Implement privacy settings page with revoke flow and impact notice
- [ ] **COMMIT:** `feat(miniapp): add privacy settings page`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-5），P1 选做（Task 6-7）

## GWT Coverage Matrix

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 | Task 5 | Task 6 | Task 7 |
|---------|--------|--------|--------|--------|--------|--------|--------|
| §6.1 正常同意（正常） | — | — | ✅ | — | — | ✅ | — |
| §6.2 不同意（异常） | — | — | — | — | — | ✅ | — |
| §6.3 撤回授权（正常） | — | — | — | ✅ | — | — | ✅ |
| 版本更新后未重新同意（边界） | ✅ | ✅ | — | — | ✅ | — | — |
| 撤回后访问需授权功能（边界） | — | — | — | ✅ | ✅ | — | — |
