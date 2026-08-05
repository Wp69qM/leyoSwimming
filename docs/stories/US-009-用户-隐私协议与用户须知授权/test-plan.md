# US-009 用户/教练隐私协议与用户须知授权 — 测试计划

> **状态**：已评审　|　**最后更新**：2026-07-31

---

## TDD 任务清单

| # | Task | RED | GREEN | REFACTOR | COMMIT |
|---|------|-----|-------|----------|--------|
| 1 | 当前协议查询接口 | 写失败测试 | 最小实现 | 提取 service | commit |
| 2 | 用户授权状态查询 | 写失败测试 | 最小实现 | 提取 service | commit |
| 3 | 同意协议 | 写失败测试 | 最小实现 | 幂等封装 | commit |
| 4 | 撤回授权 | 写失败测试 | 最小实现 | 状态检查封装 | commit |
| 5 | 版本更新后重新同意 | 写失败测试 | 最小实现 | 中间件复用 | commit |
| 6 | 小程序隐私协议页 | 写失败测试 | 最小实现 | — | commit |
| 7 | 小程序隐私设置页 | 写失败测试 | 最小实现 | — | commit |

---

## Task 1: 当前协议查询接口 [P0]

**Files:**
- Create: `backend/src/services/privacy_policy.ts`
- Create: `backend/src/controllers/privacy_policy.ts`
- Create: `backend/src/routes/privacy_policy.ts`
- Test: `backend/tests/controllers/privacy_policy.test.ts`

**Spec coverage:** REQ-003 Scenario "协议版本更新后未重新同意"

- [ ] **RED:** Write 2 failing tests — 存在当前协议返回版本内容；无当前协议返回 404 `NO_CURRENT_PRIVACY_POLICY`；游客未登录可访问。
- [ ] **GREEN:** Implement `GET /api/privacy-policy/current` with current version lookup.
- [ ] **REFACTOR:** Extract `PrivacyPolicyService.getCurrent()`.
- [ ] **COMMIT:** `feat(privacy): add current privacy policy endpoint`

## Task 2: 用户授权状态查询 [P0]

**Files:**
- Create: `backend/src/services/user_privacy_consent.ts`
- Create: `backend/src/controllers/user/privacy.ts`
- Create: `backend/src/routes/user/privacy.ts`
- Test: `backend/tests/controllers/user/privacy.test.ts`

**Spec coverage:** REQ-003 Scenario "协议版本更新后未重新同意"

- [ ] **RED:** Write 3 failing tests — 游客访问返回 401；未同意用户返回 `none` + `required_version`；已同意用户返回 `agreed` + 版本号。
- [ ] **GREEN:** Implement `GET /api/user/privacy/status` with current policy comparison.
- [ ] **REFACTOR:** Extract `UserPrivacyConsentService.getStatus(user_id)`.
- [ ] **COMMIT:** `feat(privacy): add user privacy status endpoint`

## Task 3: 同意协议 [P0]

**Files:**
- Modify: `backend/src/controllers/user/privacy.ts`
- Modify: `backend/src/services/user_privacy_consent.ts`
- Test: `backend/tests/controllers/user/privacy.test.ts`

**Spec coverage:** REQ-001 Scenario "正常同意隐私协议"

- [ ] **RED:** Write 3 failing tests — 登录用户同意当前版本返回 200 并写入 `status='agreed'`；非当前版本返回 `VERSION_MISMATCH`；游客调用返回 401。
- [ ] **GREEN:** Implement `POST /api/user/privacy/consent` with `action='agree'`.
- [ ] **REFACTOR:** Wrap agree logic in `UserPrivacyConsentService.agree(user_id, version)` with `INSERT ... ON DUPLICATE KEY UPDATE` 幂等.
- [ ] **COMMIT:** `feat(privacy): add privacy consent agreement endpoint`

## Task 4: 撤回授权 [P0]

**Files:**
- Modify: `backend/src/controllers/user/privacy.ts`
- Modify: `backend/src/services/user_privacy_consent.ts`
- Test: `backend/tests/controllers/user/privacy.test.ts`

**Spec coverage:** REQ-002 Scenario "撤回隐私授权"

- [ ] **RED:** Write 3 failing tests — 已同意用户撤回成功，`status='revoked'`；未同意用户撤回返回 `ALREADY_REVOKED`；游客调用返回 401。
- [ ] **GREEN:** Implement `POST /api/user/privacy/consent` with `action='revoke'`.
- [ ] **REFACTOR:** Add `UserPrivacyConsentService.revoke(user_id, version)` with pre-condition check.
- [ ] **COMMIT:** `feat(privacy): add privacy consent revoke endpoint`

## Task 5: 版本更新后重新同意 / 授权拦截 [P0]

**Files:**
- Create: `backend/src/middleware/privacy_consent.ts`
- Modify: `backend/src/routes/*`（需授权功能路由，如购买套餐）
- Test: `backend/tests/middleware/privacy_consent.test.ts`

**Spec coverage:** REQ-003 Scenario "协议版本更新后未重新同意"；REQ-002 Scenario "撤回授权后访问需授权功能"

- [ ] **RED:** Write 3 failing tests — 同意版本 < 当前版本访问购买接口被拦截；已撤回访问购买接口被拦截；已同意当前版本可正常访问。
- [ ] **GREEN:** Implement `privacyConsentMiddleware` blocking unauthorized functions.
- [ ] **REFACTOR:** Reuse middleware on protected routes (US-017 / US-020).
- [ ] **COMMIT:** `feat(privacy): enforce privacy consent on protected routes`

## Task 6: 小程序隐私协议页 [P1]

**Files:**
- Create: `miniapp-user/src/pages/privacy/index.tsx`
- Test: `miniapp-user/src/pages/privacy/index.test.tsx`

**Spec coverage:** REQ-001 Scenarios "正常同意隐私协议", "不同意隐私协议"

- [ ] **RED:** Write 2 failing tests — 点击同意进入首页；点击不同意停留在当前页并提示。
- [ ] **GREEN:** Implement privacy policy page with agree/disagree handling.
- [ ] **COMMIT:** `feat(miniapp): add privacy policy page`

## Task 7: 小程序隐私设置页 [P1]

**Files:**
- Create: `miniapp-user/src/pages/privacy/settings.tsx`
- Test: `miniapp-user/src/pages/privacy/settings.test.tsx`

**Spec coverage:** REQ-002 Scenario "撤回隐私授权"

- [ ] **RED:** Write 1 failing test — 点击撤回授权后状态更新并提示影响。
- [ ] **GREEN:** Implement privacy settings page with revoke flow and impact notice.
- [ ] **COMMIT:** `feat(miniapp): add privacy settings page`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder（`TBD` / `TODO` / `pass`）
- P0 必做（Task 1-5），P1 选做（Task 6-7）

---

## 测试用例（参考实现）

### 单元测试

```python
# backend/tests/services/test_user_privacy_consent.py

def test_agree_creates_record(db, user):
    svc = UserPrivacyConsentService(db)
    result = svc.agree(user_id=user.id, version='v2.0')
    assert result.status == 'agreed'
    assert result.agreed_at is not None
    assert db.query(UserPrivacyConsent).count() == 1

def test_agree_idempotent(db, user):
    svc = UserPrivacyConsentService(db)
    svc.agree(user.id, 'v2.0')
    svc.agree(user.id, 'v2.0')
    assert db.query(UserPrivacyConsent).count() == 1

def test_revoke_after_agree(db, user):
    svc = UserPrivacyConsentService(db)
    svc.agree(user.id, 'v2.0')
    result = svc.revoke(user.id, 'v2.0')
    assert result.status == 'revoked'
    assert result.revoked_at is not None

def test_revoke_without_agree_fails(db, user):
    svc = UserPrivacyConsentService(db)
    with pytest.raises(AlreadyRevokedError):
        svc.revoke(user.id, 'v2.0')
```

### 集成测试

```python
# backend/tests/integration/test_privacy_api.py

def test_api_current_policy_200(client):
    res = client.get('/api/privacy-policy/current')
    assert res.status_code == 200
    assert res.json()['version'] == 'v2.0'

def test_api_current_policy_404(client):
    res = client.get('/api/privacy-policy/current')
    assert res.status_code == 404
    assert res.json()['code'] == 'NO_CURRENT_PRIVACY_POLICY'

def test_api_status_401_for_guest(client):
    res = client.get('/api/user/privacy/status')
    assert res.status_code == 401

def test_api_consent_401_for_guest(client):
    res = client.post('/api/user/privacy/consent', json={
        'version': 'v2.0', 'action': 'agree'
    })
    assert res.status_code == 401

def test_api_consent_agree_200(client, auth_headers):
    res = client.post('/api/user/privacy/consent', json={
        'version': 'v2.0', 'action': 'agree'
    }, headers=auth_headers)
    assert res.status_code == 200
    assert res.json()['status'] == 'agreed'

def test_api_consent_version_mismatch(client, auth_headers):
    res = client.post('/api/user/privacy/consent', json={
        'version': 'v1.0', 'action': 'agree'
    }, headers=auth_headers)
    assert res.status_code == 400
    assert res.json()['code'] == 'VERSION_MISMATCH'
```

### E2E 测试

```python
# e2e/tests/test_privacy_flow.py

def test_e2e_first_time_consent(page):
    page.goto('/pages/privacy/index')
    page.check('同意协议')
    page.click('同意')
    assert page.url.endswith('/pages/home/index')

def test_e2e_revoke_impact(page, logged_in_user):
    page.goto('/pages/privacy/settings')
    page.click('撤回授权')
    page.click('确认')
    assert page.text_content('.toast') == '已撤回授权，部分功能可能受限'
```

---

## 验收标准映射

| GWT 场景 | 覆盖 Task | 测试方法 |
|----------|----------|----------|
| 正常同意隐私协议 | Task 3, 6 | `test_agree_creates_record` / `test_api_consent_agree_200` / `test_e2e_first_time_consent` |
| 不同意隐私协议 | Task 6 | E2E 不同意分支 |
| 撤回隐私授权 | Task 4, 7 | `test_revoke_after_agree` / `test_e2e_revoke_impact` |
| 协议版本更新后未重新同意 | Task 1, 2, 5 | `test_api_status_mismatch_blocks` / middleware tests |
| 游客态尝试同意/撤回 | Task 2, 3, 4 | `test_api_status_401_for_guest` / `test_api_consent_401_for_guest` |
