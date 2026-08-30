# Tech Design: US-033 教练确认上课记录

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `booking` | 修改 | 确认后 status → 已完成 |
| `package` | 修改 | 确认扣课时 |
| `course_record` | 新增/修改 | 教练填写上课记录 |
| `student_summary_draft` | 读取/删除 | 学员在 US-032 预填的总结草稿，本 US 创建 course_record 时合并并删除 |
| `audit_log` | 写 | 确认审计 |

> **管理员返还课时已迁移至 US-035**（v3 评审 P0 修复）：原 `hour_return` 表与 `/api/admin/bookings/{booking_id}/return-hour` API 已移至 US-035 tech-design §1.2/§2.1 定义。本 US 仅负责教练侧确认/旷课标记，不再包含管理员返还职责。

### 1.2 course_record 创建时序（三阶段）

> **统一规范**（P1 修复 C2）：course_record 的生命周期分三阶段，跨 US-032 与 US-033 协作。

| 阶段 | 触发 US | 操作 | 说明 |
|------|---------|------|------|
| 1. 草稿阶段 | US-032 | 学员填写课后总结，若 course_record 尚不存在，写入 `student_summary_draft` 表 | 学员可在教练确认前预填 |
| 2. 创建阶段 | US-033 | 教练确认上课时创建 `course_record`（含 content/focus_tags/mastery_level/homework/media） | 若存在 `student_summary_draft`，合并到 `course_record.student_summary_json` 并删除 draft |
| 3. 锁定阶段 | US-033 | booking.status → 已完成，course_record 进入只读状态 | 仅管理员可通过 US-035 返还课时修改 package，不修改 course_record |

### 1.3 索引

```sql
CREATE UNIQUE INDEX idx_course_record_booking ON course_record(booking_id);
CREATE UNIQUE INDEX idx_student_summary_draft_booking ON student_summary_draft(booking_id);
CREATE INDEX idx_booking_coach_status ON booking(coach_id, status);
CREATE INDEX idx_package_user_status ON package(user_id, status);
```

### 1.4 package 表新增字段（v3 评审 P0 修复）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `first_lesson_confirmed_at` | DATETIME | 可空，默认 NULL | 本套餐首次上课确认时间；教练确认上课时，若该字段为 NULL 则写入当前时间，并触发未成年人监护人短信通知（US-037 协同）；非首次确认时该字段已有值，不重复触发短信 |

**写入逻辑**：
- 教练点击「确认上课并扣除课时」时：
  - 若 `package.first_lesson_confirmed_at IS NULL`：写入当前时间，并检查 `coach_student_profile.is_minor = true`，若为真则异步触发监护人短信
  - 若 `package.first_lesson_confirmed_at IS NOT NULL`：不写入，不触发短信
- 该字段在套餐购买时（US-020）初始化为 NULL，在套餐退款/作废时不清空（保留历史记录）

**索引**：`INDEX idx_package_first_lesson (package_id, first_lesson_confirmed_at)`（供运营统计使用）

## 2. API 设计

### 2.1 POST /api/coach/bookings/{booking_id}/confirm

- **鉴权**：教练且为 booking.coach_id
- **Request**: `{ content: string, focus_tags: string[], mastery_level: int, homework: string, media: string[] }`
- **Response 200**: `{ booking_id, status: "已完成", package: { reserved_count, consumed_count, available_count } }`
- **Response 400**: `CLASS_NOT_ENDED | BOOKING_NOT_CONFIRMABLE`
- **Response 403**: `BOOKING_ACCESS_DENIED`

### 2.2 POST /api/coach/bookings/{booking_id}/mark-absent

- **鉴权**：教练且为 booking.coach_id
- **Request**: `{ remark?: string }`
- **Response 200**: `{ booking_id, status: "旷课", cancel_reason: 3, package: { reserved_count, consumed_count, available_count } }`
- **Response 400**: `CLASS_NOT_ENDED | BOOKING_NOT_CONFIRMABLE`
- **Response 403**: `BOOKING_ACCESS_DENIED`

> 注：管理员返还课时 API 已迁移至 US-035 tech-design §2.1。

## 3. 状态机

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| `booking` | 待上课/上课中 → 已完成 | 教练确认 |
| `booking` | 待上课/上课中 → 旷课（cancel_reason=3） | 教练标记学员未到课 |
| `package` | reserved → consumed | 教练确认 或 旷课标记 |
| `package` | active → exhausted | 确认最后一课时 |

> **首次上课判定**（v3 评审 P0 修复）：通过 package.first_lesson_confirmed_at 字段判定。该字段为 NULL 表示未首次确认，写入当前时间后表示已首次确认。监护人短信仅在 first_lesson_confirmed_at 从 NULL → 有值 时触发。

## 4. 缓存

- booking 缓存：`booking:{booking_id}`，TTL 300s，确认/旷课标记后删除
- package 缓存：`package:{package_id}`，TTL 300s，变更后删除
- 我的预约列表：`bookings:list:{user_id}`，确认后删除

## 5. 性能

| 指标 | 目标 |
|------|------|
| 教练确认接口 P99 | < 300ms |
| 旷课标记接口 P99 | < 300ms |
| 并发确认 | 单 booking 串行（分布式锁/乐观锁）|

## 6. 安全

- 教练只能确认/标记旷课自己的课程
- 幂等键防止重复确认/重复标记旷课
- 防止课程未结束或已终态的确认/旷课标记

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-029 | 依赖 | 已存在 booking |
| US-032 | 依赖 | 学员签到/记录页已存在 |
| US-035 | 被依赖 | 管理员返还课时（独立 US） |
| US-034 | 被依赖 | 管理员查看上课记录 |

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 教练确认上课 | `test_coach_confirm_class` |
| 教练标记旷课 | `test_coach_mark_absent` |
| 课程尚未结束 | `test_confirm_class_not_ended` |
| booking 已取消 | `test_confirm_cancelled_booking` |
| 非本课程教练 | `test_confirm_forbidden_coach` |

## 9. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版：数据模型 / API / 状态机 / 缓存 / 性能 / 安全 / 跨 US 依赖 / 测试映射 |
| v1.1 | 2026-07-31 | Dev | v3 评审 P0 修复：package 表新增 first_lesson_confirmed_at 字段，用于判定本套餐首次上课确认（触发未成年人监护人短信） |
| v1.2 | 2026-07-31 | Dev | v3 评审 P0 修复：删除残留的 hour_return 表定义与 return-hour API（已迁移至 US-035 tech-design）；新增 mark-absent API；§3 状态机补充旷课转换；§7 跨 US 依赖补充 US-035 |
| v1.3 | 2026-07-31 | Dev | P0 修复：mark-absent API 返回码由 `CLASS_NOT_STARTED` 修正为 `CLASS_NOT_ENDED`，与 PRD §5.3.2 line 457「课程结束后标记旷课」保持一致；§6/§8 同步更新 |

---