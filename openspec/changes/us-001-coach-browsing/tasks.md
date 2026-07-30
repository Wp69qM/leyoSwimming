# Tasks: US-001 游客浏览教练列表与详情

> 本文档对应 `docs/stories/US-001-.../test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → REFACTOR → COMMIT 循环。

## Task 1: Coach Repository — 列表查询 + 排序 + 分页 [P0]

**Files:**
- Create: `backend/src/repositories/coach.ts`
- Test: `backend/tests/repositories/coach.test.ts`

**Spec coverage:** REQ-001 Scenarios "正常浏览教练列表", "申请离职中教练仍可见且无标签"

- [ ] **RED:** Write failing tests — `findPublicList` returns active coaches sorted by rating DESC with pagination; includes status=4 coaches and excludes status=0/2/3
- [ ] **GREEN:** Implement `CoachRepository.findPublicList({ page, size })` — extract `PUBLIC_COACH_STATUSES = [1, 4]` and use `WHERE status IN (1, 4) ORDER BY rating DESC LIMIT/OFFSET`
- [ ] **REFACTOR:** Move `PUBLIC_COACH_STATUSES` to module-level constant and reuse it in list query and count query
- [ ] **COMMIT:** `feat(coach): add public list query with rating sort and status filter`

## Task 2: Coach Repository — 详情查询 + 状态过滤 [P0]

**Files:**
- Modify: `backend/src/repositories/coach.ts`
- Modify: `backend/tests/repositories/coach.test.ts`

**Spec coverage:** REQ-002 Scenarios "正常查看教练详情", "教练请假中", "申请离职中教练详情可见且无标签", "待审核、驳回或已离职教练不出现"

- [ ] **RED:** Write failing tests — `findById` returns full info for status=1 and status=4 coaches; throws `COACH_NOT_FOUND` for non-existent id, status=0, status=2 and status=3
- [ ] **GREEN:** Implement `CoachRepository.findById(id)` — join certificate table, reuse `PUBLIC_COACH_STATUSES` (`status IN [1, 4]`), throw `COACH_NOT_FOUND` on not found
- [ ] **REFACTOR:** Reuse `PUBLIC_COACH_STATUSES` from Task 1; ensure `findById` returns `status` field so UI can decide label visibility
- [ ] **COMMIT:** `feat(coach): add findById with status filter (hide pending coaches)`

## Task 3: GET /coaches 列表 API [P0]

**Files:**
- Create: `backend/src/controllers/coach.ts`
- Create: `backend/src/routes/coach.ts`
- Test: `backend/tests/controllers/coach.test.ts`

**Spec coverage:** REQ-001 Scenarios "正常浏览教练列表", "空状态", "申请离职中教练仍可见且无标签"

- [ ] **RED:** Write failing tests — 200 with items array; 200 with empty array when no public coaches; includes status=4 and excludes status=0/2/3
- [ ] **GREEN:** Implement `listCoaches` controller + Koa router `GET /coaches` (delegates status filter to Repository)
- [ ] **REFACTOR:** Extract common pagination parameter parsing to avoid duplication with future endpoints
- [ ] **COMMIT:** `feat(api): add GET /coaches list endpoint`

## Task 4: GET /coaches/:id 详情 API [P0]

**Files:**
- Modify: `backend/src/controllers/coach.ts`
- Modify: `backend/src/routes/coach.ts`
- Modify: `backend/tests/controllers/coach.test.ts`

**Spec coverage:** REQ-002 Scenarios "正常查看教练详情", "教练请假中", "申请离职中教练详情可见且无标签", "待审核、驳回或已离职教练不出现"

- [ ] **RED:** Write failing tests — 200 with full info for status=1 (空闲中); 200 with realTimeStatus=请假中; 200 for status=4 (申请离职中，无标签); 404 for status=0, status=2, status=3 and non-existent id
- [ ] **GREEN:** Implement `getCoachById` controller + route `GET /coaches/:id` + 404 error handling (delegates status filter to Repository)
- [ ] **REFACTOR:** Standardize `COACH_NOT_FOUND` error handling into a shared error handler or helper
- [ ] **COMMIT:** `feat(api): add GET /coaches/:id detail endpoint with 404 handling`

## Task 5: 微信小程序列表页 + 缓存 [P1]

**Files:**
- Create: `miniapp-user/src/pages/coaches/index.tsx`
- Test: `miniapp-user/src/pages/coaches/index.test.tsx`

**Spec coverage:** REQ-001 Scenarios "正常浏览教练列表", "空状态", "申请离职中教练仍可见且无标签"

- [ ] **RED:** Write failing tests — renders 5 coach cards on success; shows empty state text "暂无教练入驻，敬请期待" when no coaches; does not show resignation label for status=4 coaches
- [ ] **GREEN:** Implement `CoachesPage` — fetch `/coaches`, Taro storage cache, empty state, loading state, hide any status label when `status=4`
- [ ] **REFACTOR:** Extract `<CoachCard />` component and share `PUBLIC_COACH_STATUSES` constant from backend/shared to keep label logic in one place
- [ ] **COMMIT:** `feat(miniapp): add coaches list page with storage cache`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-4），P1 选做（Task 5）
