# Test Plan: US-016 系统自动释放下周可约时段

## Task 1: ReleaseTime Calculator — 计算下次释放时间 [P0]

**Files:**
- Create: `backend/src/services/release-time-calculator.ts`
- Test: `backend/tests/services/release-time-calculator.test.ts`

**Spec coverage:** 正常释放、节假日提前

- [ ] **RED:** Write failing tests — given rule, compute next release datetime; holiday offset shifts release earlier
- [ ] **GREEN:** Implement calculator respecting weekday/time/offset
- [ ] **REFACTOR:** Extract pure function for testability
- [ ] **COMMIT:** `feat(release): add release time calculator`

## Task 2: Slot Generator — 按模板生成 slot [P0]

**Files:**
- Create: `backend/src/services/slot-generator.ts`
- Test: `backend/tests/services/slot-generator.test.ts`

**Spec coverage:** 正常释放、教练无模板、申请离职中教练继续释放

- [ ] **RED:** Write failing tests — generates slots from templates; skips coaches with empty templates; includes coaches with status=4 and excludes status=3
- [ ] **GREEN:** Implement generator with closure-day filter
- [ ] **REFACTOR:** Batch insert with chunk size
- [ ] **COMMIT:** `feat(release): add slot generator from templates`

## Task 3: Release Job — 定时任务与幂等 [P0]

**Files:**
- Create: `backend/src/jobs/release-slots.ts`
- Test: `backend/tests/jobs/release-slots.test.ts`

**Spec coverage:** 全部 5 个场景

- [ ] **RED:** Write failing tests — idempotent success; compensates missed run; writes release_log
- [ ] **GREEN:** Implement job with idempotency check and compensation
- [ ] **REFACTOR:** Extract job scheduler adapter
- [ ] **COMMIT:** `feat(release): add auto release job`

## Task 4: 兜底补偿任务 [P1]

**Files:**
- Create: `backend/src/jobs/release-compensation.ts`
- Test: `backend/tests/jobs/release-compensation.test.ts`

**Spec coverage:** 释放任务超时未执行

- [ ] **RED:** Write failing tests — detects missing release_log and triggers job
- [ ] **GREEN:** Implement 5-minute compensation scanner
- [ ] **REFACTOR:** Reuse release job entry point
- [ ] **COMMIT:** `feat(release): add compensation scanner`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
