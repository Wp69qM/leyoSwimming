# Tasks: US-016 系统自动释放下周可约时段

## Task 1: ReleaseTime Calculator [P0]

**Files:**
- Create: `backend/src/services/release-time-calculator.ts`
- Test: `backend/tests/services/release-time-calculator.test.ts`

**Spec coverage:** REQ-001 正常释放、节假日提前

- [ ] **RED:** Compute next release datetime; holiday offset shifts earlier
- [ ] **GREEN:** Implement calculator
- [ ] **REFACTOR:** Pure function
- [ ] **COMMIT:** `feat(release): add release time calculator`

## Task 2: Slot Generator [P0]

**Files:**
- Create: `backend/src/services/slot-generator.ts`
- Test: `backend/tests/services/slot-generator.test.ts`

**Spec coverage:** REQ-001 正常释放、教练无模板

- [ ] **RED:** Generate slots from templates; skip empty templates
- [ ] **GREEN:** Implement generator with closure filter
- [ ] **REFACTOR:** Batch insert chunked
- [ ] **COMMIT:** `feat(release): add slot generator`

## Task 3: Release Job + Idempotency [P0]

**Files:**
- Create: `backend/src/jobs/release-slots.ts`
- Test: `backend/tests/jobs/release-slots.test.ts`

**Spec coverage:** All scenarios

- [ ] **RED:** Idempotent success; compensates missed run; writes log
- [ ] **GREEN:** Implement job
- [ ] **REFACTOR:** Scheduler adapter
- [ ] **COMMIT:** `feat(release): add auto release job`

## Task 4: Compensation Scanner [P1]

**Files:**
- Create: `backend/src/jobs/release-compensation.ts`
- Test: `backend/tests/jobs/release-compensation.test.ts`

**Spec coverage:** REQ-001 释放任务超时未执行

- [ ] **RED:** Detect missing release_log and trigger job
- [ ] **GREEN:** Implement 5-minute scanner
- [ ] **REFACTOR:** Reuse release job
- [ ] **COMMIT:** `feat(release): add compensation scanner`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
