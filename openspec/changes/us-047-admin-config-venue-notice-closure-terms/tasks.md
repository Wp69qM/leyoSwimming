# Tasks: US-047 管理员配置场馆、公告、闭馆换水与《用户须知》

## Task 1: Repository 与表迁移 [P0]

**Spec coverage:** REQ-001 / REQ-002 / REQ-003 / REQ-004

- [ ] **RED / GREEN / REFACTOR / COMMIT:** `feat(venue-ops): add migrations and repository`

## Task 2: PUT /api/admin/venue [P0]

**Spec coverage:** REQ-001 Scenario "管理员配置场馆信息并发布公告"

- [ ] **RED / GREEN / REFACTOR / COMMIT:** `feat(admin): add PUT /venue`

## Task 3: POST /api/admin/notices [P0]

**Spec coverage:** REQ-001 Scenario "管理员配置场馆信息并发布公告"

- [ ] **RED / GREEN / REFACTOR / COMMIT:** `feat(admin): add POST /notices`

## Task 4: POST /api/admin/venue-closures [P0]

**Spec coverage:** REQ-002 Scenarios "管理员设置闭馆并自动取消受影响预约", "闭馆日期冲突"

- [ ] **RED / GREEN / REFACTOR / COMMIT:** `feat(admin): add POST /venue-closures`

## Task 5: 闭馆影响计算与通知 [P0]

**Spec coverage:** REQ-002 Scenario "管理员设置闭馆并自动取消受影响预约"

- [ ] **RED / GREEN / REFACTOR / COMMIT:** `feat(closure): add impact calculation and notification`

## Task 6: POST /api/admin/terms [P0]

**Spec coverage:** REQ-003 Scenarios "管理员更新《用户须知》并触发重新签署", "《用户须知》内容为空"

- [ ] **RED / GREEN / REFACTOR / COMMIT:** `feat(admin): add POST /terms`

## Task 7: GET /api/admin/terms/sign-records [P1]

**Spec coverage:** REQ-004 Scenario "管理员查看签署记录"

- [ ] **RED / GREEN / REFACTOR / COMMIT:** `feat(admin): add GET /terms/sign-records`

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
