# Tasks: US-045 管理员配置标准与自定义套餐

> 本文档对应 `docs/stories/US-045-.../test-plan.md` 的 OpenSpec 映射版本。

## Task 1: PackageTemplate 表迁移与 Repository [P0]

**Spec coverage:** REQ-001 Scenario "管理员新增标准套餐成功"

- [ ] **RED:** Write failing tests for `PackageTemplateRepository.create`
- [ ] **GREEN:** Implement migration + repository
- [ ] **REFACTOR:** Extract field validation
- [ ] **COMMIT:** `feat(package-template): add migration and repository`

## Task 2: Repository 校验（重复名称、非法参数）[P0]

**Spec coverage:** REQ-001 Scenarios "新增标准套餐时名称重复", "新增标准套餐参数非法"

- [ ] **RED:** Write failing tests for duplicate name and invalid params
- [ ] **GREEN:** Implement validation in repository/service
- [ ] **REFACTOR:** Reuse validation helper
- [ ] **COMMIT:** `feat(package-template): add create validation`

## Task 3: POST /api/admin/package-template/list 列表 API [P0]

**Spec coverage:** REQ-001 Scenario "管理员新增标准套餐成功"

- [ ] **RED:** Write failing controller tests
- [ ] **GREEN:** Implement list endpoint with body params `page`/`pageSize`/`coachId`/`status`
- [ ] **REFACTOR:** Extract pagination helper
- [ ] **COMMIT:** `feat(admin): add POST /package-template/list`

## Task 4: POST /api/admin/package-template/add 新增 API [P0]

**Spec coverage:** REQ-001 Scenarios "管理员新增标准套餐成功", "新增标准套餐时名称重复", "新增标准套餐参数非法"

- [ ] **RED:** Write failing tests for 201/400/409
- [ ] **GREEN:** Implement create endpoint with body params `name`/`coachIds`/`totalHours`/`validDays`/`price`/`status`
- [ ] **REFACTOR:** Standardize error response
- [ ] **COMMIT:** `feat(admin): add POST /package-template/add`

## Task 5: POST /api/admin/package-template/update / detail / toggle-status 编辑、详情与上下架 API [P0]

**Spec coverage:** REQ-002 Scenarios "管理员编辑标准套餐并下架", "编辑不存在的套餐", "查询套餐详情"

- [ ] **RED:** Write failing tests for update/detail/toggle-status
- [ ] **GREEN:** Implement endpoints with body param `packageTemplateId`
- [ ] **REFACTOR:** Reuse not-found handler
- [ ] **COMMIT:** `feat(admin): add POST /package-template/update|detail|toggle-status`

## Task 6: Redis 缓存与失效 [P1]

**Spec coverage:** REQ-001 / REQ-002 成功场景

- [ ] **RED:** Write failing tests for cache hit/miss/invalidation
- [ ] **GREEN:** Implement Redis caching
- [ ] **REFACTOR:** Extract cache helper
- [ ] **COMMIT:** `feat(cache): add package template cache`

## Task 7: 管理员权限中间件 [P0]

**Spec coverage:** REQ-003 Scenario "无权限管理员访问配置接口"

- [ ] **RED:** Write failing tests for 403
- [ ] **GREEN:** Enforce `package:write` permission
- [ ] **REFACTOR:** Standardize permission check
- [ ] **COMMIT:** `feat(auth): enforce package config permission`

## Task 8: 自定义套餐规则配置 [P0]

**Spec coverage:** REQ-004 Scenarios "管理员配置自定义套餐规则成功", "自定义套餐规则参数非法"

- [ ] **RED:** Write failing tests for custom config validation
- [ ] **GREEN:** Implement POST /api/admin/package-template/custom-config with body params `minHours`/`maxHours`/`defaultValidDays`/`unitPriceFloor`
- [ ] **REFACTOR:** Reuse validation helper
- [ ] **COMMIT:** `feat(admin): add POST /package-template/custom-config`

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
- P0 必做，P1 选做
