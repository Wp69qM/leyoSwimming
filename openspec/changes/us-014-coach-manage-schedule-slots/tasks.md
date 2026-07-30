# Tasks: US-014 教练管理可约时段

> 本文档对应 `docs/stories/US-014-教练-管理可约时段/test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: 可约时段查询与批量添加接口基础结构 [P0]

**Files:**
- Create: `backend/src/controllers/coach/schedule_slot.ts`
- Create: `backend/src/routes/coach.ts`
- Create: `backend/src/repositories/schedule_slot.ts`
- Test: `backend/tests/controllers/coach/schedule_slot.test.ts`

**Spec coverage:** REQ-001 Scenario "正常添加可约时段"

- [ ] **RED:** Write 3 failing tests — `GET /api/coach/schedule-slots` 未登录返回 401；`POST /api/coach/schedule-slots` 批量添加成功；返回结果包含 `created` 数量
- [ ] **GREEN:** Implement `GET/POST /api/coach/schedule-slots` controller skeleton with coach auth and `schedule_slot` repository insert
- [ ] **COMMIT:** `feat(coach): add schedule slots GET/POST endpoints skeleton`

## Task 2: 时段冲突校验 [P0]

**Files:**
- Modify: `backend/src/repositories/schedule_slot.ts`
- Modify: `backend/src/controllers/coach/schedule_slot.ts`
- Test: `backend/tests/repositories/schedule_slot.test.ts`

**Spec coverage:** REQ-001 Scenario "添加时段冲突"

- [ ] **RED:** Write 2 failing tests — 9:00-10:00 已存在时添加 9:30-10:30 返回 `SLOT_TIME_CONFLICT`；不重叠时段通过
- [ ] **GREEN:** Implement overlap detection using `(coach_id, start_time, end_time)` query and integrate into POST handler
- [ ] **COMMIT:** `feat(coach): add schedule slot conflict validation`

## Task 3: 过去时间校验 [P0]

**Files:**
- Create: `backend/src/services/coach/schedule_time.ts`
- Modify: `backend/src/controllers/coach/schedule_slot.ts`
- Test: `backend/tests/services/coach/schedule_time.test.ts`

**Spec coverage:** REQ-001 Scenario "添加过去时间的时段"

- [ ] **RED:** Write 2 failing tests — 添加过去时间返回 `PAST_TIME_NOT_ALLOWED`；未来时间通过
- [ ] **GREEN:** Implement past-time validator and apply to POST/PUT endpoints
- [ ] **COMMIT:** `feat(coach): add past time validation for schedule slots`

## Task 4: 修改与删除接口 [P0]

**Files:**
- Modify: `backend/src/controllers/coach/schedule_slot.ts`
- Modify: `backend/src/repositories/schedule_slot.ts`
- Test: `backend/tests/controllers/coach/schedule_slot.test.ts`

**Spec coverage:** REQ-002 Scenarios "删除已有预约的时段", "修改时段时所有权校验失败"

- [ ] **RED:** Write 3 failing tests — `PUT /api/coach/schedule-slots/{id}` 修改本教练时段成功；修改其他教练时段返回 `SLOT_NOT_FOUND`；`DELETE` 本教练时段成功
- [ ] **GREEN:** Implement PUT/DELETE handlers with ownership check and conflict/past-time validation
- [ ] **COMMIT:** `feat(coach): add schedule slot update and delete endpoints`

## Task 5: 已预约时段删除保护 [P0]

**Files:**
- Modify: `backend/src/controllers/coach/schedule_slot.ts`
- Create: `backend/src/repositories/booking.ts`
- Test: `backend/tests/controllers/coach/schedule_slot.test.ts`

**Spec coverage:** REQ-002 Scenario "删除已有预约的时段"

- [ ] **RED:** Write 2 failing tests — 已预约时段删除返回 `SLOT_HAS_BOOKING`；未预约时段删除成功
- [ ] **GREEN:** Implement booking existence check before delete
- [ ] **COMMIT:** `feat(coach): prevent deletion of booked schedule slots`

## Task 6: 复制上周排班 [P0]

**Files:**
- Modify: `backend/src/controllers/coach/schedule_slot.ts`
- Modify: `backend/src/repositories/schedule_slot.ts`
- Create: `backend/src/services/coach/schedule_copy.ts`
- Test: `backend/tests/services/coach/schedule_copy.test.ts`

**Spec coverage:** REQ-003 Scenarios "批量复制上周排班", "复制后全部冲突"

- [ ] **RED:** Write 3 failing tests — 复制上周排班生成本周记录；闭馆日期跳过；全部冲突时 `created = 0` 且返回冲突明细
- [ ] **GREEN:** Implement copy-last-week service with venue_closure skip and conflict detection
- [ ] **COMMIT:** `feat(coach): add copy last week schedule service`

## Task 7: 排班变更审计日志 [P0]

**Files:**
- Create: `backend/src/repositories/coach_schedule_log.ts`
- Modify: `backend/src/controllers/coach/schedule_slot.ts`
- Test: `backend/tests/repositories/coach_schedule_log.test.ts`

**Spec coverage:** REQ-001 Scenario "正常添加可约时段", REQ-003 Scenario "批量复制上周排班"

- [ ] **RED:** Write 2 failing tests — 添加时段后 `coach_schedule_log` 新增 action=create 记录；复制排班后新增 action=copy 记录
- [ ] **GREEN:** Implement `CoachScheduleLogRepository` and write log after create/update/delete/copy
- [ ] **COMMIT:** `feat(coach): add schedule change audit log`

## Task 8: 学员端可见性同步 [P1]

**Files:**
- Create: `backend/src/services/cache/coach_slots_cache.ts`
- Modify: `backend/src/controllers/coach/schedule_slot.ts`
- Test: `backend/tests/services/cache/coach_slots_cache.test.ts`

**Spec coverage:** REQ-004 Scenario "发布后学员端可见"

- [ ] **RED:** Write 2 failing tests — 发布排班后 `coach:slots:{coach_id}:{date}` 缓存被删除；学员端查询仅返回 available 且未来时段
- [ ] **GREEN:** Implement slots cache invalidation and user-side query filtering
- [ ] **COMMIT:** `feat(coach): add schedule slots cache invalidation`

## Task 9: 教练端排班管理页 [P1]

**Files:**
- Create: `miniapp-coach/src/pages/schedule/index.tsx`
- Create: `miniapp-coach/src/components/schedule-slot-form/index.tsx`
- Test: `miniapp-coach/src/pages/schedule/index.test.tsx`

**Spec coverage:** REQ-001 Scenario "正常添加可约时段", REQ-003 Scenario "批量复制上周排班"

- [ ] **RED:** Write 3 failing tests — 周历视图展示当前排班；点击日期添加时段后调用 POST；点击复制上周按钮调用复制接口
- [ ] **GREEN:** Implement schedule management page with weekly calendar view and add/copy interactions
- [ ] **COMMIT:** `feat(miniapp-coach): add schedule management page`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-7），P1 选做（Task 8-9）

## GWT Coverage Matrix

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 | Task 5 | Task 6 | Task 7 | Task 8 | Task 9 |
|---------|--------|--------|--------|--------|--------|--------|--------|--------|--------|
| 正常添加可约时段 | ✅ | ✅ | ✅ | — | — | — | ✅ | ✅ | ✅ |
| 添加时段冲突 | — | ✅ | — | — | — | — | — | — | — |
| 添加过去时间的时段 | — | — | ✅ | — | — | — | — | — | ✅ |
| 删除已有预约的时段 | — | — | — | — | ✅ | — | — | — | — |
| 修改时段时所有权校验失败 | — | — | — | ✅ | — | — | — | — | — |
| 批量复制上周排班 | — | — | — | — | — | ✅ | ✅ | ✅ | ✅ |
| 复制后全部冲突 | — | — | — | — | — | ✅ | — | — | — |
| 发布后学员端可见 | — | — | — | — | — | — | — | ✅ | — |
