# Tasks: US-002 游客查看套餐、公告与场馆信息

> 本文档对应 `docs/stories/US-002-.../test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: Package Template Repository — 公开套餐列表查询 [P0]

**Files:**
- Create: `backend/src/repositories/package_template.ts`
- Test: `backend/tests/repositories/package_template.test.ts`

**Spec coverage:** REQ-001 Scenarios "正常浏览套餐列表", "无套餐可展示", "自定义套餐不返回"

- [ ] **RED:** Write 3 failing tests — `findPublicList` returns published templates sorted by sortOrder ASC with experience first; filters out unpublished (status=0) and custom (package_type=2); returns empty array when no published templates
- [ ] **GREEN:** Implement `PackageTemplateRepository.findPublicList({ type })` — `WHERE status=1 AND package_type IN [0,1] ORDER BY sort_order ASC, package_type ASC`
- [ ] **COMMIT:** `feat(package-template): add public list query with status/type filter and sort`

## Task 2: Announcement Repository — 生效中公告查询 [P0]

**Files:**
- Create: `backend/src/repositories/announcement.ts`
- Test: `backend/tests/repositories/announcement.test.ts`

**Spec coverage:** REQ-002 Scenarios "正常浏览公告与场馆信息", "公告维护中 / 无生效公告"

- [ ] **RED:** Write 3 failing tests — `findActiveList` returns active notices within time window sorted by priority DESC; returns only `visible_scope='all'`; respects `limit` parameter (max 10); returns empty array when no active notices
- [ ] **GREEN:** Implement `AnnouncementRepository.findActiveList({ limit })` — `WHERE visible_scope='all' AND start_at <= NOW() AND end_at > NOW() ORDER BY priority DESC, start_at DESC LIMIT {min(limit,10)}`
- [ ] **COMMIT:** `feat(announcement): add active notice query with time window, scope filter, priority sort`

## Task 3: Venue Repository — 场馆信息 + 闭馆换水查询 [P0]

**Files:**
- Create: `backend/src/repositories/venue.ts`
- Test: `backend/tests/repositories/venue.test.ts`

**Spec coverage:** REQ-002 Scenarios "正常浏览公告与场馆信息", "场馆信息未配置", "闭馆换水时间窗命中"

- [ ] **RED:** Write 3 failing tests — `findVenueInfo` returns venue info with active closure notice when venue exists; returns null when venue not configured; returns null `closureNotice` when no active closure
- [ ] **GREEN:** Implement `VenueRepository.findVenueInfo()` — query `venue` table first (return null if empty), then query `venue_closure` for current time window命中记录，填充 `closureNotice` 字段
- [ ] **COMMIT:** `feat(venue): add venue info query with active closure notice lookup`

## Task 4: 3 个公开 API 端点 [P0]

**Files:**
- Create: `backend/src/controllers/guest_info.ts`
- Create: `backend/src/routes/guest_info.ts`
- Test: `backend/tests/controllers/guest_info.test.ts`

**Spec coverage:** REQ-001 全部场景 + REQ-002 全部场景

- [ ] **RED:** Write 7 failing tests — `GET /packages` returns 200 with items array + 200 with empty array; `GET /announcements` returns 200 with items + 200 with empty array + 400 for invalid limit; `GET /venue` returns 200 with venue info + 404 with VENUE_NOT_CONFIGURED
- [ ] **GREEN:** Implement 3 controllers (`listPackages`, `listAnnouncements`, `getVenue`) + Koa router with `GET /api/v1/packages`, `GET /api/v1/announcements`, `GET /api/v1/venue` + 404 error handling for venue
- [ ] **COMMIT:** `feat(api): add 3 public guest info endpoints (packages, announcements, venue) with 404 handling`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- 全部 P0 必做
- 缓存层（Redis）作为 P1 优化，不在本 US 的 TDD 任务内，后续单独迭代
