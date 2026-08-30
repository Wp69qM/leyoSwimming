# Tech Design: US-044 管理员管理教练排班与请假

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `schedule_slot` | 读/写/改/删 | 排班管理 |
| `coach_leave_request` | 读/改 | 请假审批 |
| `booking` | 写 | 请假通过后批量取消课程 |
| `package` | 写 | 释放 reserved 课时 |
| `audit_log` | 写 | 记录排班与请假操作 |
| `coach` | 读 | 校验教练状态 |
| `notification` | 写 | 通知学员 |

#### schedule_slot

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK | — |
| `coach_id` | BIGINT | FK, IDX | 教练 ID |
| `start_time` | DATETIME | NOT NULL | 时段开始 |
| `end_time` | DATETIME | NOT NULL | 时段结束 |
| `status` | TINYINT | 默认 0 | 0=可约 1=已预约 2=已锁 |
| `created_at` / `updated_at` | DATETIME | — | 时间戳 |

#### coach_leave_request

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK | — |
| `coach_id` | BIGINT | FK, IDX | 教练 ID |
| `start_time` | DATETIME | NOT NULL | 请假开始 |
| `end_time` | DATETIME | NOT NULL | 请假结束 |
| `reason` | VARCHAR(500) | NOT NULL | 请假原因 |
| `status` | TINYINT | 默认 0 | 0=pending 1=approved 2=rejected |
| `reviewer_id` | BIGINT | NULL | 审批管理员 ID |
| `reviewed_at` | DATETIME | NULL | 审批时间 |
| `reject_reason` | VARCHAR(500) | NULL | 拒绝原因 |

### 1.2 索引

```sql
CREATE INDEX idx_schedule_coach_time ON schedule_slot(coach_id, start_time, end_time);
CREATE INDEX idx_leave_coach_status ON coach_leave_request(coach_id, status);
CREATE INDEX idx_leave_status ON coach_leave_request(status);
```

---

## 2. API 设计

### 2.1 排班管理

| 接口 | 方法 | 鉴权 | 说明 |
|------|------|------|------|
| `/api/admin/v1/coaches/{id}/schedule-slots` | GET | `MANAGE_SCHEDULE` | 查询教练排班（支持日期范围） |
| `/api/admin/v1/coaches/{id}/schedule-slots` | POST | `MANAGE_SCHEDULE` | 批量添加排班 |
| `/api/admin/v1/schedule-slots/{id}` | PUT | `MANAGE_SCHEDULE` | 修改排班 |
| `/api/admin/v1/schedule-slots/{id}` | DELETE | `MANAGE_SCHEDULE` | 删除排班 |

#### POST /api/admin/v1/coaches/{id}/schedule-slots（批量添加）

- **Request**:
  ```json
  {
    "slots": [
      { "start_time": "2026-08-01T09:00:00+08:00", "end_time": "2026-08-01T10:00:00+08:00" }
    ]
  }
  ```
- **Response 201**: `{ created: 1 }`
- **Response 409**: `{ code: SCHEDULE_TIME_CONFLICT, conflicts: [...] }`

### 2.2 请假审批

| 接口 | 方法 | 鉴权 | 说明 |
|------|------|------|------|
| `/api/admin/v1/leave-requests` | GET | `MANAGE_SCHEDULE` | 查询请假队列（支持 status 筛选） |
| `/api/admin/v1/leave-requests/{id}/approve` | POST | `MANAGE_SCHEDULE` | 通过请假 |
| `/api/admin/v1/leave-requests/{id}/reject` | POST | `MANAGE_SCHEDULE` | 拒绝请假 |

#### POST /api/admin/v1/leave-requests/{id}/approve

- **Response 200**: `{ cancelled_bookings: 2, notified_users: 2 }`
- **Response 409**: `{ code: LEAVE_ALREADY_PROCESSED }`

#### POST /api/admin/v1/leave-requests/{id}/reject

- **Request**: `{ "reject_reason": "场馆活动需要" }`
- **Response 200**: `{ status: "rejected" }`

---

## 3. 状态机

### 3.1 coach_leave_request 状态机

```
pending ──[管理员通过]──→ approved  → 触发批量取消 booking + 释放课时 + 通知学员
        └──[管理员拒绝]──→ rejected  → 保持原排班，已预约课程不受影响
```

### 3.2 booking 状态机（请假通过触发）

```
已预约/待上课 ──[请假通过批量取消]──→ 已取消（cancel_reason = 教练请假）
```

---

## 4. 缓存策略

| 缓存 | 键 | TTL | 失效策略 |
|------|----|-----|---------|
| 教练排班列表 | `schedule:{coach_id}:{week}` | 5 分钟 | 排班变更时主动失效 |
| 请假队列计数 | `leave:pending:count` | 1 分钟 | 审批时主动失效 |

---

## 5. 性能

| 指标 | 目标 |
|------|------|
| 排班列表查询 P99 | < 200ms |
| 批量添加排班 P99 | < 500ms |
| 请假审批 P99 | < 500ms（含通知发送） |
| 批量取消课程（单请假最多 50 个 booking） | < 2s |

---

## 6. 安全

- 管理员鉴权 `MANAGE_SCHEDULE` 权限
- 排班时间冲突校验（同教练同时段不可重复）
- 请假审批幂等（基于 `leave_request.id + updated_at` 乐观锁）
- 删除有预约的排班需二次确认 + 通知学员
- 所有操作记录 audit_log

---

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-014 | 依赖 | schedule_slot 数据模型 |
| US-036 | 依赖 | 产生 pending 请假申请 |
| US-029 / US-030 | 被依赖 | 排班变化影响预约/取消 |
| US-049 | 被依赖 | 客服工单可能涉及排班问题 |

---

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 通过请假并取消课程 | `test_approve_leave_cancels_bookings` |
| 修改排班时间冲突 | `test_update_slot_time_conflict` |
| 拒绝请假保持原排班 | `test_reject_leave_keeps_schedule` |
| 重复审批请假 | `test_approve_leave_idempotent` |
| 删除有预约的排班 | `test_delete_slot_with_booking_cancels_and_notifies` |
| 批量添加排班冲突项高亮 | `test_batch_add_slots_with_conflicts` |
| 请假时段与已上课重叠 | `test_leave_overlap_with_completed_booking_skipped` |
