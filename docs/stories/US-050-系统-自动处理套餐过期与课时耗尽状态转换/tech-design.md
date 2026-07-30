# US-050 技术设计：系统自动处理套餐过期与课时耗尽状态转换

> 本文档对应 `docs/stories/US-050-.../user-story.md` 的技术实现方案。
> 角色：开发 | 最后更新：2026-07-30

---

## 0. 文档定位

本文档为设计层，回答「用什么数据模型 / 定时任务 / 事件机制 / 锁策略实现套餐状态自动转换」。具体执行步骤见 [./test-plan.md](./test-plan.md)。

---

## 1. 数据模型影响

### 1.1 新增/修改的表

| 表名 | 操作 | 说明 |
|------|------|------|
| `package` | 修改 | 复用已有字段：`status`、`available`、`reserved`、`consumed`、`expire_at` |
| `package_status_log` | 新增 | 状态转换审计日志 |
| `cron_job_lock` | 新增 | 定时任务分布式锁 |
| `course_record` | 读取 | 教练确认上课时触发计数扣减（表名与 US-032/US-033 统一，v3 评审 P1 修复） |

### 1.2 `package_status_log` 字段

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT PK | AUTO_INCREMENT | 日志 ID |
| `package_id` | BIGINT FK | IDX | 套餐 ID |
| `from_status` | VARCHAR(16) | NOT NULL | 转换前状态 |
| `to_status` | VARCHAR(16) | NOT NULL | 转换后状态 |
| `reason` | VARCHAR(32) | NOT NULL | 触发原因：EXPIRE_CRON / HOURS_EXHAUSTED / CANCEL_RECONCILE |
| `created_at` | DATETIME | NOT NULL | 转换时间 |

### 1.3 索引

```sql
CREATE UNIQUE INDEX idx_package_status_log_unique
  ON package_status_log(package_id, from_status, to_status, reason, created_at);
CREATE INDEX idx_package_status_log_package_id ON package_status_log(package_id);
CREATE INDEX idx_package_active_expire
  ON package(status, expire_at)
  WHERE status = 'active';
```

---

## 2. 定时任务设计

### 2.1 ExpirePackageCronJob

- **调度周期**：每小时 0 分执行（cron: `0 * * * *`）
- **扫描条件**：`status = 'active' AND expire_at <= NOW()`
- **批量策略**：按 `id` 升序游标分页，每批 500 条
- **更新语句**：
  ```sql
  UPDATE package
  SET status = 'expired', updated_at = NOW()
  WHERE id = :id AND status = 'active' AND expire_at <= NOW();
  ```
- **日志写入**：仅当 `ROW_COUNT() = 1` 时插入 `package_status_log`
- **分布式锁**：`cron_job_lock` 按 `job_name='expire_package_cron'` + `hour_bucket` 加锁

### 2.2 事件处理器 PackageStatusReconciler

- **订阅事件**：
  - `lesson.confirmed`（教练确认上课）
  - `lesson.cancelled`（学员取消预约）
- **处理逻辑**：
  1. 在事务内更新 `package` 计数（available / reserved / consumed）
  2. 重新读取 `package` 当前状态
  3. 若当前 `status='active'` 且 `available=0` 且 `reserved=0`，则更新为 `exhausted`
  4. 仅对发生有效状态转换的记录写入 `package_status_log`

---

## 3. 状态机影响

```
package.status:
  active ──[expire_at <= NOW()]──→ expired
  active ──[available=0 AND reserved=0]──→ exhausted
```

- `expired` / `exhausted` / `refunded` / `frozen` 均为终态，不会自动回退到 `active`
- 状态转换必须满足 PRD §3.4.4 不变量

---

## 4. 缓存策略

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis | `package:{id}` | 300s | 状态变更时主动失效 |
| Redis | `user:{user_id}:packages` | 60s | 状态变更时主动失效 |

### 4.1 降级策略

- Redis 不可用时直接查 DB，状态转换任务不依赖缓存

---

## 5. 性能指标

| 指标 | 目标 |
|------|------|
| 单次过期巡检 P99 | < 5 分钟 |
| 事件触发状态重算 P99 | < 100ms |
| 批量更新单批 500 条耗时 | < 1s |
| 并发冲突率 | < 0.01% |

---

## 6. 安全 / 鉴权

- 定时任务与事件处理器均为内部服务，不暴露外部 API
- 状态转换日志不可删除，保留 180 天
- 分布式锁超时 10 分钟，防止任务僵死

---

## 7. 跨 US 依赖

| US | 依赖方向 | 说明 |
|----|---------|------|
| US-020 | 被依赖 | 生成 `package` 记录 |
| US-029 | 被依赖 | 生成预约预占 `reserved` |
| US-032 | 被依赖 | 学员签到触发上课流程 |
| US-033 | 被依赖 | 教练确认上课触发计数变化 |
| US-021 | 依赖本 US | 「我的套餐」读取准确状态 |
| US-030 | 依赖本 US | 取消/改约触发计数恢复 |

---

## 8. 异常与边界

| 场景 | 处理 |
|------|------|
| 并发巡检 | 分布式锁 + 按 `id` 分区，避免同一实例重复扫描 |
| 并发事件与巡检 | 数据库行锁/事务串行化，后者基于最新状态重算 |
| 已退款套餐收到取消事件 | 事件处理器校验 `status='active'`，否则忽略 |
| 定时任务中断 | 记录上次游标，下次任务从断点继续 |
| 日志重复写入 | 唯一索引防止同一转换重复记录 |

---

## 9. 实现顺序（与 test-plan 的映射）

| tech-design 章节 | 对应 test-plan Task |
|------------------|---------------------|
| §1 数据模型 | Task 1 |
| §2 定时任务 | Task 2 |
| §2 事件处理器 | Task 3 |
| §4 缓存策略 | Task 4 |
| §8 异常边界 | Task 5 |

---

## 10. 上下游引用

- 上游需求：[./user-story.md](./user-story.md)
- 执行计划：[./test-plan.md](./test-plan.md)
- 全局规范：[docs/spec/tech-design/README.md](../../spec/tech-design/README.md)

---

## 11. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版 |
| v1.1 | 2026-07-31 | Dev | v3 评审 P1 修复：§1.1 表名 lesson_record 改为 course_record（与 US-032/US-033 统一） |
