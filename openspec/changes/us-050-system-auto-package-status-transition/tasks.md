# Tasks: US-050 系统自动处理套餐过期与课时耗尽状态转换

> 本文档对应 `docs/stories/US-050-.../test-plan.md` 的 OpenSpec 映射版本。

## Task 1: package_status_log 与 cron_job_lock 表迁移 [P0]

**Spec coverage:** REQ-003 Scenario "状态转换后缓存被失效"（日志表准备）

- [ ] **RED:** Write failing tests for `PackageStatusLogRepository.create`
- [ ] **GREEN:** Implement migrations for `package_status_log` and `cron_job_lock`
- [ ] **REFACTOR:** Add unique index on status log
- [ ] **COMMIT:** `feat(package-status): add status log and cron lock migrations`

## Task 2: 过期巡检定时任务 ExpirePackageCronJob [P0]

**Spec coverage:** REQ-001 Scenarios "定时任务将到期套餐标记为 expired", "过期套餐上仍有 reserved 课时仍正确标记为 expired"

- [ ] **RED:** Write failing job tests for expired package transition
- [ ] **GREEN:** Implement `ExpirePackageCronJob` with cursor pagination
- [ ] **REFACTOR:** Extract batch update helper
- [ ] **COMMIT:** `feat(cron): add expire package cron job`

## Task 3: 事件处理器 PackageStatusReconciler [P0]

**Spec coverage:** REQ-002 Scenarios "教练确认最后一节课后套餐变为 exhausted", "取消预约恢复 available 后不应误将 exhausted 回退为 active"

- [ ] **RED:** Write failing tests for exhausted transition and terminal-state guard
- [ ] **GREEN:** Implement `PackageStatusReconciler` on `lesson.confirmed` / `lesson.cancelled`
- [ ] **REFACTOR:** Extract state transition helper
- [ ] **COMMIT:** `feat(reconciler): add package status reconciler on lesson events`

## Task 4: Redis 缓存失效策略 [P1]

**Spec coverage:** REQ-003 Scenario "状态转换后缓存被失效"

- [ ] **RED:** Write failing tests for cache invalidation
- [ ] **GREEN:** Delete `package:{id}` and `user:{user_id}:packages` on status change
- [ ] **REFACTOR:** Extract cache invalidation helper
- [ ] **COMMIT:** `feat(cache): invalidate package status caches`

## Task 5: 并发与边界场景测试 [P0]

**Spec coverage:** REQ-001 Scenario "并发巡检保证同一套餐仅转换一次", REQ-002 Scenario "取消预约恢复 available 后不应误将 exhausted 回退为 active"

- [ ] **RED:** Write failing tests for concurrent cron runs and terminal-state ignore
- [ ] **GREEN:** Add distributed lock and status guard
- [ ] **REFACTOR:** Extract lock helper and state guard helper
- [ ] **COMMIT:** `feat(package-status): add concurrency and terminal-state guards`

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
- P0 必做，P1 选做
