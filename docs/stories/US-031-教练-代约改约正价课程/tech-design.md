# Tech Design: US-031 教练代约/改约正价课程

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `package` | 读写 | FIFO 选择 active 套餐并预占/释放课时 |
| `booking` | 写/修改 | 代约新增；改约取消原记录并新增 |
| `schedule_slot` | 读 | 校验时段有效 |
| `user` | 读 | 校验学员身份 |
| `coach` | 读 | 校验教练身份与资质 |
| `coach_student` | 读 | 校验教练与学员绑定关系（可选，若 package.coach_id 已足够则不单独建表）|

#### booking（扩展）

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `user_id` | FK（学员）|
| `coach_id` | FK（教练）|
| `package_id` | FK |
| `schedule_slot_id` | FK |
| `status` | 已预约 / 待上课 / 上课中 / 已完成 / 已取消 / 旷课 |
| `operator` | 操作来源：`student` / `coach` / `system`，代约/改约时 = coach |
| `start_time` | 课程开始时间 |
| `end_time` | 课程结束时间 |
| `created_at` | 创建时间 |

### 1.2 索引

```sql
CREATE UNIQUE INDEX idx_booking_slot_user ON booking(schedule_slot_id, user_id) WHERE status != '已取消';
CREATE INDEX idx_booking_user_status ON booking(user_id, status);
CREATE INDEX idx_booking_coach_status ON booking(coach_id, status);
CREATE INDEX idx_package_user_status ON package(user_id, status);
CREATE INDEX idx_schedule_slot_coach_time ON schedule_slot(coach_id, start_time);
```

## 2. API 设计

### 2.1 POST /api/coach/bookings

- **鉴权**：教练登录且为 booking.coach_id
- **Request**:
  ```json
  {
    "student_id": 100,
    "schedule_slot_id": 200,
    "idempotency_key": "uuid"
  }
  ```
- **Response 201**: `{ booking_id, status, start_time, end_time }`
- **Response 400**: `SLOT_TAKEN | NO_QUOTA | NO_BOUND_COACH | SLOT_NOT_AVAILABLE | PACKAGE_NOT_BOOKABLE`
- **Response 403**: `BOOKING_ACCESS_DENIED`

### 2.2 POST /api/coach/bookings/{booking_id}/reschedule

- **鉴权**：教练登录且为原 booking.coach_id
- **Request**:
  ```json
  {
    "new_schedule_slot_id": 300,
    "idempotency_key": "uuid"
  }
  ```
- **Response 200**: `{ old_booking_id, new_booking_id, status, start_time, end_time }`
- **Response 400**: `SLOT_TAKEN | NO_QUOTA | SLOT_NOT_AVAILABLE | BOOKING_NOT_RESCHEDULABLE`
- **Response 403**: `BOOKING_ACCESS_DENIED`

### 2.3 GET /api/coaches/{coach_id}/slots

- **鉴权**：教练登录
- **Query**: `start_date`, `end_date`
- **Response 200**: `{ items: Slot[] }`

### 2.4 GET /api/coach/students/{student_id}/packages

- **鉴权**：教练登录且与该学员存在绑定关系
- **Response 200**: `{ items: Package[] }`

## 3. 状态机

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| `booking` | 无 → 已预约 | 教练代约成功 |
| `booking` | 已预约 → 已取消 | 教练改约释放原预约 |
| `package` | available → reserved | 代约成功或改约创建新预约 |
| `package` | reserved → available | 改约释放原预约 |

## 4. 缓存

- 可约时段缓存：`slots:{coach_id}:{date}`，TTL 60s，预约成功后删除
- package 缓存：`package:{package_id}`，TTL 300s，预占/释放后删除
- 学员预约列表：`bookings:list:{user_id}`，变更后删除
- 分布式锁：`booking:lock:{schedule_slot_id}`，TTL 5s，防止并发冲突

## 5. 性能

| 指标 | 目标 |
|------|------|
| 代约提交接口 P99 | < 300ms |
| 改约接口 P99 | < 400ms |
| 时段列表查询 P99 | < 200ms |
| 并发预约冲突处理 | 1000 QPS P99 < 500ms |

## 6. 安全

- 严格校验教练身份与 coach_id
- 严格校验 package 归属与教练绑定关系
- 禁止教练预约/改约非绑定学员的课程
- 幂等键防重放
- 改约事务内完成，避免课时丢失

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-012 | 依赖 | 教练资质与主页 |
| US-014 | 依赖 | 教练可约时段 |
| US-020 | 依赖 | 学员购买正价套餐 |
| US-029 | 依赖 | 复用正价预约规则 |
| US-032 | 被依赖 | 学员签到/签退课程 |
| US-033 | 被依赖 | 教练确认上课记录 |

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 教练成功代约 | `test_coach_book_for_student_success` |
| 教练成功改约 | `test_coach_reschedule_for_student_success` |
| 时段已被预约 | `test_coach_book_slot_taken` |
| 学员未绑定当前教练 | `test_coach_book_no_bound_coach` |
| 改约时 booking 已取消 | `test_coach_reschedule_cancelled_booking` |
