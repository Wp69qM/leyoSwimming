# US-050 实施任务清单（Tasks）

> **US 关联**：[user-story.md](./user-story.md)（US-050 系统自动处理套餐过期与课时耗尽状态转换）
> **文档角色**：TDD 任务清单 + 测试计划（双重角色）
> **作者**：开发　|　**最后更新**：2026-07-30

---

## 0. 双重角色说明

本文档承担**双重角色**：
- **TDD 任务清单**：每个 task = 2-5 分钟可执行单元
- **测试计划**：覆盖 [user-story.md](./user-story.md) 中所有 GWT 场景

每个 Task 严格遵循 **RED → GREEN → REFACTOR → COMMIT** 循环。

---

## 1. Task 概览

| Task | 标题 | 优先级 | 对应 GWT 场景 |
|------|------|--------|--------------|
| 1 | package_status_log 与 cron_job_lock 表迁移 | P0 | §6.1, §6.3, §6.4 |
| 2 | 过期巡检定时任务 ExpirePackageCronJob | P0 | §6.1, §6.3, §6.4 |
| 3 | 事件处理器 PackageStatusReconciler | P0 | §6.2, §6.5 |
| 4 | Redis 缓存失效策略 | P1 | §6.1, §6.2 |
| 5 | 并发与边界场景测试 | P0 | §6.4, §6.5 |

---

## 2. 实施任务

### Task 1: package_status_log 与 cron_job_lock 表迁移 [P0]

**Files:**
- Create: `backend/src/migrations/20260730_create_package_status_log.ts`
- Create: `backend/src/migrations/20260730_create_cron_job_lock.ts`
- Create: `backend/src/repositories/packageStatusLog.ts`
- Test: `backend/tests/repositories/packageStatusLog.test.ts`

**对应 GWT**：[user-story.md §6.1 场景 1](./user-story.md#61-场景-1定时任务将到期套餐标记为-expired)、[§6.3 场景 3](./user-story.md#63-场景-3过期套餐上仍有-reserved-课时仍正确标记为-expired)、[§6.4 场景 4](./user-story.md#64-场景-4并发巡检保证同一套餐仅转换一次)

- [ ] **Step 1: RED — 写失败测试**

```typescript
// backend/tests/repositories/packageStatusLog.test.ts
import { PackageStatusLogRepository } from '../../src/repositories/packageStatusLog';

describe('PackageStatusLogRepository', () => {
  it('creates a status transition log', async () => {
    const repo = new PackageStatusLogRepository();
    const log = await repo.create({
      package_id: 1,
      from_status: 'active',
      to_status: 'expired',
      reason: 'EXPIRE_CRON',
    });
    expect(log.id).toBeGreaterThan(0);
    expect(log.to_status).toBe('expired');
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- packageStatusLog.test.ts`
Expected: FAIL with `Cannot find module '../../src/repositories/packageStatusLog'`

- [ ] **Step 3: GREEN — 写最小实现**

```typescript
// backend/src/repositories/packageStatusLog.ts
export class PackageStatusLogRepository {
  async create(data) {
    const [id] = await db('package_status_log').insert(data);
    return await db('package_status_log').where({ id }).first();
  }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- packageStatusLog.test.ts`
Expected: PASS

- [ ] **Step 5: REFACTOR — 添加唯一索引校验**

- 将唯一索引 `(package_id, from_status, to_status, reason, created_at)` 放入迁移文件

- [ ] **Step 6: COMMIT**

```bash
git add backend/src/migrations/20260730_create_package_status_log.ts backend/src/migrations/20260730_create_cron_job_lock.ts backend/src/repositories/packageStatusLog.ts backend/tests/repositories/packageStatusLog.test.ts
git commit -m "feat(package-status): add status log and cron lock migrations"
```

---

### Task 2: 过期巡检定时任务 ExpirePackageCronJob [P0]

**Files:**
- Create: `backend/src/jobs/expirePackageCron.ts`
- Create: `backend/src/services/packageExpiration.ts`
- Test: `backend/tests/jobs/expirePackageCron.test.ts`

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1定时任务将到期套餐标记为-expired)、[§6.3 场景 3](./user-story.md#63-场景-3过期套餐上仍有-reserved-课时仍正确标记为-expired)

- [ ] **Step 1: RED — 写失败测试**

```typescript
// backend/tests/jobs/expirePackageCron.test.ts
describe('ExpirePackageCronJob', () => {
  it('marks expired packages and logs transitions', async () => {
    await db('package').insert({
      id: 100,
      user_id: 1,
      status: 'active',
      total_hours: 10,
      available: 2,
      reserved: 1,
      consumed: 7,
      expire_at: '2026-07-29T23:59:59',
    });

    const job = new ExpirePackageCronJob();
    await job.run('2026-07-30T01:00:00');

    const pkg = await db('package').where({ id: 100 }).first();
    expect(pkg.status).toBe('expired');
    expect(pkg.available).toBe(2);
    expect(pkg.reserved).toBe(1);

    const logs = await db('package_status_log').where({ package_id: 100 });
    expect(logs).toHaveLength(1);
    expect(logs[0].reason).toBe('EXPIRE_CRON');
  });
});
```

- [ ] **Step 2-6**: 实现定时任务、跑测试、重构、commit（略，遵循 RED→GREEN→COMMIT）

Commit message: `feat(cron): add expire package cron job`

---

### Task 3: 事件处理器 PackageStatusReconciler [P0]

**Files:**
- Create: `backend/src/services/packageStatusReconciler.ts`
- Test: `backend/tests/services/packageStatusReconciler.test.ts`

**对应 GWT**：[§6.2 场景 2](./user-story.md#62-场景-2教练确认最后一节课后套餐变为-exhausted)、[§6.5 场景 5](./user-story.md#65-场景-5取消预约恢复-available-后不应误将-exhausted-回退为-active)

- [ ] **Step 1: RED — 写失败测试**

```typescript
// backend/tests/services/packageStatusReconciler.test.ts
describe('PackageStatusReconciler', () => {
  it('transitions active package to exhausted when hours run out', async () => {
    await db('package').insert({
      id: 101,
      user_id: 1,
      status: 'active',
      total_hours: 10,
      available: 0,
      reserved: 1,
      consumed: 9,
      expire_at: '2026-08-31T23:59:59',
    });

    const reconciler = new PackageStatusReconciler();
    await reconciler.onLessonConfirmed(101);

    const pkg = await db('package').where({ id: 101 }).first();
    expect(pkg.status).toBe('exhausted');
    expect(pkg.reserved).toBe(0);
    expect(pkg.consumed).toBe(10);
  });
});
```

- [ ] **Step 2-6**: 实现事件处理器、跑测试、重构、commit（略，遵循 RED→GREEN→COMMIT）

Commit message: `feat(reconciler): add package status reconciler on lesson events`

---

### Task 4: Redis 缓存失效策略 [P1]

**Files:**
- Modify: `backend/src/services/packageExpiration.ts`
- Modify: `backend/src/services/packageStatusReconciler.ts`
- Modify: `backend/src/services/cache.ts`
- Test: `backend/tests/services/packageStatusReconciler.test.ts`

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1定时任务将到期套餐标记为-expired)、[§6.2 场景 2](./user-story.md#62-场景-2教练确认最后一节课后套餐变为-exhausted)

- [ ] **Step 1-6**: 状态变更后删除 `package:{id}` 与 `user:{user_id}:packages`；写缓存失效测试；commit message `feat(cache): invalidate package status caches`

---

### Task 5: 并发与边界场景测试 [P0]

**Files:**
- Modify: `backend/tests/jobs/expirePackageCron.test.ts`
- Modify: `backend/tests/services/packageStatusReconciler.test.ts`

**对应 GWT**：[§6.4 场景 4](./user-story.md#64-场景-4并发巡检保证同一套餐仅转换一次)、[§6.5 场景 5](./user-story.md#65-场景-5取消预约恢复-available-后不应误将-exhausted-回退为-active)

- [ ] **Step 1: RED — 写失败测试**

```typescript
// backend/tests/jobs/expirePackageCron.test.ts
describe('ExpirePackageCronJob concurrency', () => {
  it('does not duplicate status logs under concurrent runs', async () => {
    await db('package').insert({
      id: 102,
      user_id: 1,
      status: 'active',
      total_hours: 10,
      available: 2,
      reserved: 0,
      consumed: 8,
      expire_at: '2026-07-29T23:59:59',
    });

    const job = new ExpirePackageCronJob();
    await Promise.all([job.run('2026-07-30T01:00:00'), job.run('2026-07-30T01:00:00')]);

    const logs = await db('package_status_log').where({ package_id: 102 });
    expect(logs).toHaveLength(1);
  });
});
```

```typescript
// backend/tests/services/packageStatusReconciler.test.ts
describe('PackageStatusReconciler edge cases', () => {
  it('ignores cancel event on exhausted package', async () => {
    await db('package').insert({
      id: 103,
      user_id: 1,
      status: 'exhausted',
      total_hours: 10,
      available: 0,
      reserved: 0,
      consumed: 10,
      expire_at: '2026-08-31T23:59:59',
    });

    const reconciler = new PackageStatusReconciler();
    await reconciler.onLessonCancelled(103);

    const pkg = await db('package').where({ id: 103 }).first();
    expect(pkg.status).toBe('exhausted');
    expect(pkg.available).toBe(0);
  });
});
```

- [ ] **Step 2-6**: 实现并发锁与终态校验、跑测试、重构、commit（略，遵循 RED→GREEN→COMMIT）

Commit message: `feat(package-status): add concurrency and terminal-state guards`

---

## 3. 任务执行纪律

- **严格顺序**：Task 1 → Task 2 → Task 3 → Task 4 → Task 5
- **每步必须可见**：Step 1（RED）→ Step 2（看失败）→ Step 3（GREEN）→ Step 4（看通过）→ Step 5（REFACTOR）→ Step 6（COMMIT）
- **不允许 placeholder**：任何 "TBD" / "TODO" / "实现 later" / "类似 Task N" = 立即返工
- **每个 Task 结束 = 1 次 commit**：禁止跨 Task 累积 commit
- **P0 必做 / P1 选做**：MVP 阶段先完成 P0（Task 1-3, 5），P1（Task 4）视进度决定
- **GWT 覆盖**：每个 Task 头部必须明确「对应 GWT」场景编号

---

## 4. 上下游引用

- **上游需求**：[./user-story.md](./user-story.md)
- **设计输入**：[./tech-design.md](./tech-design.md)
- **Figma 设计交付物**：[./user-story.md](./user-story.md#13-figma-链接) §13-15

---

## 5. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版：5 个 task 覆盖 5 个 GWT 场景 |
