# Tasks: US-048 管理员配置首页运营内容

> 本文档对应 `docs/stories/US-048-.../test-plan.md` 的 OpenSpec 映射版本。

## Task 1: homepage_banner / homepage_card 表迁移与 Repository [P0]

**Spec coverage:** REQ-001 Scenario "管理员配置通知栏与 Banner 并即时生效"

- [ ] **RED:** Write failing tests for `HomepageOpsRepository.createBanner` / `createCard`
- [ ] **GREEN:** Implement migrations + repository
- [ ] **REFACTOR:** Extract field validation
- [ ] **COMMIT:** `feat(homepage-ops): add banner/card migrations and repository`

## Task 2: GET/POST /api/admin/homepage/notices [P0]

**Spec coverage:** REQ-001 Scenario "管理员配置通知栏与 Banner 并即时生效"

- [ ] **RED:** Write failing controller tests
- [ ] **GREEN:** Implement notices CRUD
- [ ] **REFACTOR:** Extract response helper
- [ ] **COMMIT:** `feat(admin): add homepage notices CRUD`

## Task 3: GET/POST /api/admin/homepage/banners [P0]

**Spec coverage:** REQ-001 Scenarios "管理员配置通知栏与 Banner 并即时生效", "有效期不合法"

- [ ] **RED:** Write failing tests for 201 / 400 INVALID_TIME_RANGE
- [ ] **GREEN:** Implement banners CRUD
- [ ] **REFACTOR:** Reuse time range validation
- [ ] **COMMIT:** `feat(admin): add homepage banners CRUD`

## Task 4: GET/POST /api/admin/homepage/cards [P0]

**Spec coverage:** REQ-002 Scenarios "管理员配置可见范围与预览运营卡片", "可见范围参数非法"

- [ ] **RED:** Write failing tests for 201 / 400 INVALID_VISIBLE_SCOPE
- [ ] **GREEN:** Implement cards CRUD
- [ ] **REFACTOR:** Reuse visible scope validation
- [ ] **COMMIT:** `feat(admin): add homepage cards CRUD`

## Task 5: POST /api/admin/homepage/preview [P0]

**Spec coverage:** REQ-002 Scenario "管理员配置可见范围与预览运营卡片"

- [ ] **RED:** Write failing tests for preview filtering
- [ ] **GREEN:** Implement preview endpoint
- [ ] **REFACTOR:** Share filter logic with public config
- [ ] **COMMIT:** `feat(admin): add homepage preview`

## Task 6: GET /api/homepage/config [P0]

**Spec coverage:** REQ-001 / REQ-002 成功场景

- [ ] **RED:** Write failing tests for public config
- [ ] **GREEN:** Implement public config endpoint
- [ ] **REFACTOR:** Extract config builder
- [ ] **COMMIT:** `feat(homepage): add public homepage config`

## Task 7: 首页运营缓存与失效 [P1]

**Spec coverage:** REQ-001 / REQ-002 成功场景

- [ ] **RED:** Write failing tests for cache hit/miss/invalidation
- [ ] **GREEN:** Implement Redis caching
- [ ] **REFACTOR:** Extract cache helper
- [ ] **COMMIT:** `feat(cache): add homepage config cache`

## Task 8: 管理员权限中间件 [P0]

**Spec coverage:** REQ-003 Scenario "无权限管理员访问配置接口"

- [ ] **RED:** Write failing tests for 403
- [ ] **GREEN:** Enforce `homepage:write` permission
- [ ] **REFACTOR:** Standardize permission check
- [ ] **COMMIT:** `feat(auth): enforce homepage ops permission`

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
- P0 必做，P1 选做
