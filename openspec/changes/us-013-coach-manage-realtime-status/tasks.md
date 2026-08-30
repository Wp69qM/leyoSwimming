# Tasks: US-013 教练管理实时状态

> 本文档对应 `docs/stories/US-013-教练-管理实时状态/test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: 实时状态查询与手动更新接口 [P0]

**Files:**
- Create: `backend/src/controllers/coach/realtime_status.ts`
- Create: `backend/src/routes/coach.ts`
- Test: `backend/tests/controllers/coach/realtime_status.test.ts`

**Spec coverage:** REQ-001 Scenario "教练手动设置为空闲中"

- [ ] **RED:** Write 3 failing tests — `GET /api/coach/realtime-status` 未登录返回 401；已登录返回当前状态；`PUT /api/coach/realtime-status` 切换为空闲中成功
- [ ] **GREEN:** Implement `GET/PUT /api/coach/realtime-status` controller skeleton with coach auth and status enum validation
- [ ] **COMMIT:** `feat(coach): add realtime status GET/PUT endpoints`

## Task 2: 手动覆盖标记 [P0]

**Files:**
- Modify: `backend/src/controllers/coach/realtime_status.ts`
- Create: `backend/src/repositories/coach_status_log.ts`
- Test: `backend/tests/repositories/coach_status_log.test.ts`

**Spec coverage:** REQ-001 Scenario "教练手动设置为空闲中"

- [ ] **RED:** Write 2 failing tests — 手动切换后 `status_override_flag = 1`；`coach_status_log` 新增 source=manual 记录
- [ ] **GREEN:** Implement manual override flag update and status log repository
- [ ] **COMMIT:** `feat(coach): add manual override flag and status log`

## Task 3: 请假状态校验 [P0]

**Files:**
- Create: `backend/src/services/coach/leave_status.ts`
- Modify: `backend/src/controllers/coach/realtime_status.ts`
- Test: `backend/tests/services/coach/leave_status.test.ts`

**Spec coverage:** REQ-001 Scenarios "手动设置请假中被拒绝", "请假中状态锁定"

- [ ] **RED:** Write 3 failing tests — 无请假申请设请假中返回 `LEAVE_REQUIRED`；请假中状态改空闲中返回 `STATUS_LOCKED_BY_LEAVE`；正常状态切换通过
- [ ] **GREEN:** Implement leave approval check and locked-status guard
- [ ] **COMMIT:** `feat(coach): add leave status validation for realtime status`

## Task 4: 自动切换定时任务 [P0]

**Files:**
- Create: `backend/src/jobs/coach_auto_status.ts`
- Create: `backend/src/repositories/booking.ts`
- Test: `backend/tests/jobs/coach_auto_status.test.ts`

**Spec coverage:** REQ-002 Scenario "系统自动切换为上课中"

- [ ] **RED:** Write 2 failing tests — 课前 15 分钟自动切换为上课中；课程结束后 15 分钟自动恢复为空闲中
- [ ] **GREEN:** Implement cron job scanning upcoming/just-finished bookings and updating `realtime_status`
- [ ] **COMMIT:** `feat(coach): add auto status switching cron job`

## Task 5: 手动覆盖优先级 [P0]

**Files:**
- Modify: `backend/src/jobs/coach_auto_status.ts`
- Test: `backend/tests/jobs/coach_auto_status.test.ts`

**Spec coverage:** REQ-002 Scenario "手动状态优先级高于自动状态"

- [ ] **RED:** Write 2 failing tests — `status_override_flag = 1` 且未过期时跳过自动切换；过期后允许自动切换
- [ ] **GREEN:** Implement override check with `override_until` expiration logic
- [ ] **COMMIT:** `feat(coach): respect manual override in auto status switching`

## Task 6: 状态变更广播 [P1]

**Files:**
- Create: `backend/src/services/websocket/coach_status.ts`
- Modify: `backend/src/controllers/coach/realtime_status.ts`
- Modify: `backend/src/jobs/coach_auto_status.ts`
- Test: `backend/tests/services/websocket/coach_status.test.ts`

**Spec coverage:** REQ-001 Scenario "教练手动设置为空闲中", REQ-002 Scenario "系统自动切换为上课中"

- [ ] **RED:** Write 2 failing tests — 手动切换状态后广播到学员端；自动切换后也触发广播
- [ ] **GREEN:** Implement WebSocket/SSE broadcast service and trigger on status change
- [ ] **COMMIT:** `feat(coach): add realtime status broadcast`

## Task 7: 管理员查询接口 [P1]

**Files:**
- Create: `backend/src/controllers/admin/coach.ts`
- Create: `backend/src/routes/admin.ts`
- Test: `backend/tests/controllers/admin/coach.test.ts`

**Spec coverage:** REQ-001 管理员查询能力

- [ ] **RED:** Write 2 failing tests — 非管理员返回 403；管理员查询返回指定教练实时状态
- [ ] **GREEN:** Implement `GET /api/admin/coach/realtime-status` with admin auth middleware
- [ ] **COMMIT:** `feat(admin): add coach realtime status query endpoint`

## Task 8: 教练端状态切换组件 [P1]

**Files:**
- Create: `miniapp-coach/src/components/realtime-status-picker/index.tsx`
- Create: `miniapp-coach/src/pages/home/index.tsx`
- Test: `miniapp-coach/src/components/realtime-status-picker/index.test.tsx`

**Spec coverage:** REQ-001 Scenario "教练手动设置为空闲中"

- [ ] **RED:** Write 2 failing tests — 点击状态按钮弹出 Action Sheet；选择新状态后调用 PUT 接口
- [ ] **GREEN:** Implement status picker component and integrate into home page
- [ ] **COMMIT:** `feat(miniapp-coach): add realtime status picker component`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-5），P1 选做（Task 6-8）

## GWT Coverage Matrix

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 | Task 5 | Task 6 | Task 7 | Task 8 |
|---------|--------|--------|--------|--------|--------|--------|--------|--------|
| 教练手动设置为空闲中 | ✅ | ✅ | — | — | — | ✅ | — | ✅ |
| 手动设置请假中被拒绝 | — | — | ✅ | — | — | — | — | ✅ |
| 请假中状态锁定 | — | — | ✅ | — | — | — | — | ✅ |
| 系统自动切换为上课中 | — | — | — | ✅ | — | ✅ | — | — |
| 手动状态优先级高于自动状态 | — | — | — | — | ✅ | — | — | — |
| 课程结束后自动恢复 | — | — | — | ✅ | — | — | — | — |
