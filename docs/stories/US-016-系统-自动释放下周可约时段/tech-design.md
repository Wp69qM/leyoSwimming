# Tech Design: US-016 系统自动释放下周可约时段

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `schedule_slot` | 写 | 生成的可约时段 |
| `release_log` | 写 | 释放执行记录 |
| `coach_availability_template` | 读 | 教练模板 |
| `release_rule` | 读 | 全局规则 |
| `venue_closure` | 读 | 闭馆日 |

#### schedule_slot（新增/更新）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | |
| `coach_id` | BIGINT FK | |
| `start_time` | DATETIME | |
| `end_time` | DATETIME | |
| `status` | TINYINT/VARCHAR | available / booked / closed |
| `course_type` | TINYINT | 0=体验 1=正价 |
| `created_at` | DATETIME | |

#### release_log

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | |
| `release_date` | DATE | 本次释放对应的日期 |
| `release_scope` | VARCHAR | NEXT_WEEK / NEXT_7_DAYS |
| `planned_at` | DATETIME | 计划释放时间 |
| `executed_at` | DATETIME | 实际执行时间 |
| `generated_slots` | INT | 生成 slot 数 |
| `skipped_coaches` | INT | 无模板教练数 |
| `status` | VARCHAR | success / partial / failed |

### 1.2 索引

```sql
CREATE INDEX idx_release_log_date ON release_log(release_date, status);
CREATE UNIQUE INDEX idx_slot_coach_time ON schedule_slot(coach_id, start_time);
```

## 2. API 设计

（无新增用户接口；内部定时任务）

## 3. 状态机

| 实体 | 转换 | 触发 | 说明 |
|------|------|------|------|
| `schedule_slot` | 无 → available | 释放任务 | 仅对 `coach.status IN (1, 4)` 生成；`status=3` 已离职教练排除 |

## 4. 缓存

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis | `release_rule:current` | 60s | 本 US 只读 |
| Redis | `next_release_time` | 60s | 释放成功后更新 |

## 5. 性能

| 指标 | 目标 |
|------|------|
| 单教练生成 | < 50ms |
| 100 教练总完成 | P99 < 30s |

## 6. 安全

- 定时任务仅内部触发
- slot 生成使用唯一索引防止重复

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-014 | 依赖 | 读取教练模板 |
| US-015 | 依赖 | 读取释放规则 |
| US-003 | 被依赖 | 倒计时读取 next_release_time |
| US-018/US-029 | 被依赖 | 预约读取 schedule_slot |

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 正常释放下周整周时段 | `test_release_job_generates_next_week_slots` |
| 节假日提前释放 | `test_release_job_holiday_early_release` |
| 教练未设置模板 | `test_release_job_skips_coach_without_template` |
| 释放任务超时未执行 | `test_release_job_compensates_missed_run` |
| 防止重复释放 | `test_release_job_idempotent` |
