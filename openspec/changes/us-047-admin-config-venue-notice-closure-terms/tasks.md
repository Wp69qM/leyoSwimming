> **OpenSpec Tasks | 映射自 `docs/stories/US-047-管理员-配置场馆公告闭馆换水与用户须知和隐私协议/test-plan.md`**

## Task 1: Repository 与表迁移 [P0]

**Spec coverage:** REQ-001 / REQ-002 / REQ-003 / REQ-004 / REQ-005 / REQ-006

- [ ] **RED / GREEN / REFACTOR / COMMIT:** `feat(venue-ops): add migrations and repositories for venue, notice, closure, terms_policy, privacy_policy`

## Task 2: PUT /api/admin/venue [P0]

**Spec coverage:** REQ-001 Scenario "管理员更新场馆信息"

- [ ] **RED / GREEN / REFACTOR / COMMIT:** `feat(admin): add PUT /api/admin/venue`

## Task 3: POST /api/admin/notices [P0]

**Spec coverage:** REQ-002 Scenario "管理员发布公告"

- [ ] **RED / GREEN / REFACTOR / COMMIT:** `feat(admin): add POST /api/admin/notices`

## Task 4: POST /api/admin/venue-closures [P0]

**Spec coverage:** REQ-003 Scenarios "管理员设置闭馆并异步取消受影响预约", "闭馆日期冲突"

- [ ] **RED / GREEN / REFACTOR / COMMIT:** `feat(admin): add POST /api/admin/venue-closures with conflict detection`

## Task 5: 闭馆影响计算与通知 [P0]

**Spec coverage:** REQ-003 Scenario "管理员设置闭馆并异步取消受影响预约"

- [ ] **RED / GREEN / REFACTOR / COMMIT:** `feat(closure): add async impact calculation, booking cancellation and notification`

## Task 6: POST /api/admin/terms [P0]

**Spec coverage:** REQ-004 Scenarios "管理员更新《用户须知》并触发重新同意", "《用户须知》内容为空"

- [ ] **RED / GREEN / REFACTOR / COMMIT:** `feat(admin): add POST /api/admin/terms with version switch and consent revalidation`

## Task 7: POST /api/admin/privacy [P0]

**Spec coverage:** REQ-005 Scenarios "管理员更新《隐私协议》并触发重新同意", "《隐私协议》内容为空"

- [ ] **RED / GREEN / REFACTOR / COMMIT:** `feat(admin): add POST /api/admin/privacy with version switch and consent revalidation`

## Task 8: GET /api/admin/terms/consent-records [P1]

**Spec coverage:** REQ-006 Scenario "管理员查看《用户须知》签署记录"

- [ ] **RED / GREEN / REFACTOR / COMMIT:** `feat(admin): add GET /api/admin/terms/consent-records`

## Task 9: GET /api/admin/privacy/consent-records [P1]

**Spec coverage:** REQ-006 Scenario "管理员查看《隐私协议》签署记录"

- [ ] **RED / GREEN / REFACTOR / COMMIT:** `feat(admin): add GET /api/admin/privacy/consent-records`

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder

## GWT Coverage Matrix

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 | Task 5 | Task 6 | Task 7 | Task 8 | Task 9 |
|---------|--------|--------|--------|--------|--------|--------|--------|--------|--------|
| §6.1 管理员更新场馆信息 | ✅ | ✅ | — | — | — | — | — | — | — |
| §6.2 管理员发布公告 | ✅ | — | ✅ | — | — | — | — | — | — |
| §6.3 管理员设置闭馆并异步取消受影响预约 | ✅ | — | — | ✅ | ✅ | — | — | — | — |
| §6.4 管理员更新《用户须知》与《隐私协议》并触发重新同意 | ✅ | — | — | — | — | ✅ | ✅ | — | — |
| §6.5 闭馆日期冲突 | ✅ | — | — | ✅ | — | — | — | — | — |
| §6.6 《用户须知》或《隐私协议》内容为空 | ✅ | — | — | — | — | ✅ | ✅ | — | — |
