# Tasks: US-049 管理员查看数据看板与处理客服工单

> 本文档对应 `docs/stories/US-049-.../test-plan.md` 的 OpenSpec 映射版本。

## Task 1: support_ticket / ticket_reply 表迁移与 Repository [P0]

**Spec coverage:** REQ-002 / REQ-003 Scenario "管理员处理客服工单"

- [ ] **RED:** Write failing tests for `SupportTicketRepository.create` / `assign`
- [ ] **GREEN:** Implement migrations + repository
- [ ] **REFACTOR:** Extract status validation
- [ ] **COMMIT:** `feat(support-ticket): add ticket/reply migrations and repository`

## Task 2: GET /api/admin/dashboard/peak-hours [P0]

**Spec coverage:** REQ-001 Scenario "管理员查看数据看板"

- [ ] **RED:** Write failing controller tests
- [ ] **GREEN:** Implement peak-hours dashboard
- [ ] **REFACTOR:** Extract date range validation
- [ ] **COMMIT:** `feat(admin): add peak hours dashboard`

## Task 3: GET /api/admin/dashboard/monthly-hours [P0]

**Spec coverage:** REQ-001 Scenario "管理员查看数据看板"

- [ ] **RED:** Write failing tests
- [ ] **GREEN:** Implement monthly hours dashboard
- [ ] **REFACTOR:** Share query builder
- [ ] **COMMIT:** `feat(admin): add monthly hours dashboard`

## Task 4: GET /api/admin/dashboard/coach-ratings [P0]

**Spec coverage:** REQ-001 Scenario "管理员查看数据看板"

- [ ] **RED:** Write failing tests
- [ ] **GREEN:** Implement coach ratings dashboard
- [ ] **REFACTOR:** Extract ranking helper
- [ ] **COMMIT:** `feat(admin): add coach ratings dashboard`

## Task 5: GET /api/admin/dashboard/student-portrait [P0]

**Spec coverage:** REQ-001 Scenario "管理员查看数据看板"

- [ ] **RED:** Write failing tests
- [ ] **GREEN:** Implement student portrait dashboard
- [ ] **REFACTOR:** Extract aggregation helper
- [ ] **COMMIT:** `feat(admin): add student portrait dashboard`

## Task 6: GET /api/admin/tickets 与 /:id [P0]

**Spec coverage:** REQ-002 Scenarios "管理员处理客服工单", "工单不存在"

- [ ] **RED:** Write failing tests for list and detail
- [ ] **GREEN:** Implement ticket list and detail
- [ ] **REFACTOR:** Extract search query
- [ ] **COMMIT:** `feat(admin): add ticket list and detail`

## Task 7: PUT /api/admin/tickets/:id/assign 与 POST /reply [P0]

**Spec coverage:** REQ-003 Scenario "管理员处理客服工单"

- [ ] **RED:** Write failing tests
- [ ] **GREEN:** Implement assign and reply
- [ ] **REFACTOR:** Extract notification trigger
- [ ] **COMMIT:** `feat(admin): add ticket assign and reply`

## Task 8: PUT /api/admin/tickets/:id/status [P0]

**Spec coverage:** REQ-003 Scenario "管理员处理客服工单"

- [ ] **RED:** Write failing tests
- [ ] **GREEN:** Implement status update
- [ ] **REFACTOR:** Standardize state machine guard
- [ ] **COMMIT:** `feat(admin): add ticket status update`

## Task 9: 看板缓存策略 [P1]

**Spec coverage:** REQ-001 Scenario "管理员查看数据看板"

- [ ] **RED:** Write failing tests for cache hit/miss/invalidation
- [ ] **GREEN:** Implement Redis caching
- [ ] **REFACTOR:** Extract cache helper
- [ ] **COMMIT:** `feat(cache): add dashboard cache`

## Task 10: 管理员权限中间件 [P0]

**Spec coverage:** REQ-004 Scenarios "无权限管理员访问看板接口", "无权限管理员访问客服工单接口"

- [ ] **RED:** Write failing tests for 403 on dashboard and ticket APIs
- [ ] **GREEN:** Enforce `dashboard:read` / `ticket:read` / `ticket:write` permissions
- [ ] **REFACTOR:** Standardize permission check
- [ ] **COMMIT:** `feat(auth): enforce dashboard and ticket permissions`

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
- P0 必做，P1 选做
