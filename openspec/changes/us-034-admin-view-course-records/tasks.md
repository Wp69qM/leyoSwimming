# Tasks: US-034 管理员查看上课记录

> 本文档对应 `docs/stories/US-034-.../test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → REFACTOR → COMMIT 循环。

## Task 1: Admin Course Record Repository — 列表查询与筛选 [P0]

**Files:**
- Create: `backend/src/repositories/admin-course-record.ts`
- Test: `backend/tests/repositories/admin-course-record.test.ts`

**Spec coverage:** REQ-034-1

- [ ] **RED:** Write failing tests — list returns recent 30 days records with pagination; filter by coach_id / student_id / status / date range; default sort by start_time DESC
- [ ] **GREEN:** Implement `AdminCourseRecordRepository.findList(filters, pagination)`
- [ ] **REFACTOR:** Extract filter builder and pagination helper
- [ ] **COMMIT:** `feat(admin): add course record list repository`

## Task 2: Admin Course Record Detail Query [P0]

**Files:**
- Modify: `backend/src/repositories/admin-course-record.ts`
- Test: `backend/tests/repositories/admin-course-record.test.ts`

**Spec coverage:** REQ-034-2

- [ ] **RED:** Write failing tests — get detail returns booking + course_record + package + audit_logs; return null/throw when booking not found
- [ ] **GREEN:** Implement `AdminCourseRecordRepository.findById(bookingId)`
- [ ] **REFACTOR:** Reuse joins with list query
- [ ] **COMMIT:** `feat(admin): add course record detail repository`

## Task 3: Admin Course Record API [P0]

**Files:**
- Create: `backend/src/controllers/admin-course-record.ts`, `backend/src/routes/admin-course-record.ts`
- Test: `backend/tests/controllers/admin-course-record.test.ts`

**Spec coverage:** REQ-034-1 / REQ-034-2

- [ ] **RED:** Write failing tests — 200 list with filters; 200 detail; 403 without permission; 400 date range too large; 404 not found
- [ ] **GREEN:** Implement controller + route with permission middleware
- [ ] **REFACTOR:** Share admin pagination and permission helpers
- [ ] **COMMIT:** `feat(api): add admin course record endpoints`

## Task 4: 管理端上课记录页 [P1]

**Files:**
- Create: `web-admin/src/pages/course-records/index.tsx`, `web-admin/src/pages/course-records/detail.tsx`
- Test: 对应测试文件

**Spec coverage:** REQ-034-1 / REQ-034-2

- [ ] **RED:** Write failing tests — renders list with filters; shows detail modal/page; handles empty state and errors
- [ ] **GREEN:** Implement pages
- [ ] **REFACTOR:** Extract `<CourseRecordFilters />` and `<CourseRecordDetail />`
- [ ] **COMMIT:** `feat(web-admin): add course records pages`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-3），P1 选做（Task 4）
