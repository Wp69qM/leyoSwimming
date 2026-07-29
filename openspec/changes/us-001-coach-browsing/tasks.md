# Tasks: US-001 游客浏览教练列表与详情

> 本文档对应 `docs/stories/US-001-.../test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: Coach Repository — 列表查询 + 排序 + 分页 [P0]

**Files:**
- Create: `backend/src/repositories/coach.ts`
- Test: `backend/tests/repositories/coach.test.ts`

**Spec coverage:** REQ-001 Scenario "正常浏览教练列表"

- [ ] **RED:** Write failing test — `findPublicList` returns active coaches sorted by rating DESC with pagination
- [ ] **GREEN:** Implement `CoachRepository.findPublicList({ page, size })` — `WHERE status=1 ORDER BY rating DESC LIMIT/OFFSET`
- [ ] **COMMIT:** `feat(coach): add public list query with rating sort and status filter`

## Task 2: Coach Repository — 详情查询 + 状态过滤 [P0]

**Files:**
- Modify: `backend/src/repositories/coach.ts`
- Modify: `backend/tests/repositories/coach.test.ts`

**Spec coverage:** REQ-002 Scenarios "正常查看教练详情", "教练请假中", "教练申请中不出现"

- [ ] **RED:** Write 3 failing tests — `findById` returns full info for active coach; throws `COACH_NOT_FOUND` for non-existent id; throws for status=0
- [ ] **GREEN:** Implement `CoachRepository.findById(id)` — join certificate table, filter `status IN [1,2]`, throw on not found
- [ ] **COMMIT:** `feat(coach): add findById with status filter (hide pending coaches)`

## Task 3: GET /coaches 列表 API [P0]

**Files:**
- Create: `backend/src/controllers/coach.ts`
- Create: `backend/src/routes/coach.ts`
- Test: `backend/tests/controllers/coach.test.ts`

**Spec coverage:** REQ-001 Scenarios "正常浏览教练列表", "空状态"

- [ ] **RED:** Write 2 failing tests — 200 with items array; 200 with empty array when no active coaches
- [ ] **GREEN:** Implement `listCoaches` controller + Koa router `GET /coaches`
- [ ] **COMMIT:** `feat(api): add GET /coaches list endpoint`

## Task 4: GET /coaches/:id 详情 API [P0]

**Files:**
- Modify: `backend/src/controllers/coach.ts`
- Modify: `backend/src/routes/coach.ts`
- Modify: `backend/tests/controllers/coach.test.ts`

**Spec coverage:** REQ-002 Scenarios "正常查看教练详情", "教练请假中", "教练申请中不出现"

- [ ] **RED:** Write 4 failing tests — 200 with full info (空闲中); 200 with realTimeStatus=请假中; 404 for pending coach (status=0); 404 for non-existent id
- [ ] **GREEN:** Implement `getCoachById` controller + route `GET /coaches/:id` + 404 error handling
- [ ] **COMMIT:** `feat(api): add GET /coaches/:id detail endpoint with 404 handling`

## Task 5: 微信小程序列表页 + 缓存 [P1]

**Files:**
- Create: `miniapp-user/src/pages/coaches/index.tsx`
- Test: `miniapp-user/src/pages/coaches/index.test.tsx`

**Spec coverage:** REQ-001 Scenarios "正常浏览教练列表", "空状态"

- [ ] **RED:** Write 2 failing tests — renders 5 coach cards on success; shows empty state text "暂无教练入驻，敬请期待" when no coaches
- [ ] **GREEN:** Implement `CoachesPage` — fetch `/coaches`, Taro storage cache, empty state, loading state
- [ ] **COMMIT:** `feat(miniapp): add coaches list page with storage cache`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-4），P1 选做（Task 5）
