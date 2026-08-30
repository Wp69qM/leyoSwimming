# Tech Design: US-018 体验课学员预约与取消

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `booking` | 写 | 体验课预约 |
| `package` | 改 | 预占/释放 |
| `schedule_slot` | 读/改 | 时段占用 |
| `cancel_request` | 写 | 24h 内取消审批 |

#### booking

| 字段 | 说明 |
|------|------|
| `booking_id` | PK |
| `user_id` | FK |
| `coach_id` | FK |
| `slot_id` | FK |
| `package_id` | FK |
| `course_type` | 0=体验 |
| `status` | 已预约/已取消/... |
| `created_at` | |

### 1.2 索引

```sql
CREATE UNIQUE INDEX idx_booking_slot_user ON booking(slot_id, user_id) WHERE status != '已取消';
CREATE INDEX idx_booking_user_status ON booking(user_id, status);
```

## 2. API 设计

### 2.1 POST /api/bookings/trial

- **鉴权**：需登录，身份=学员
- **Request**: `{ slot_id }`
- **Response 201**: booking 详情
- **Response 400**: `NO_TRIAL_PACKAGE`
- **Response 409**: `SLOT_TAKEN`

### 2.2 POST /api/bookings/{id}/cancel

- **鉴权**：需登录且为 booking 所有者
- **Response 200**: 取消成功
- **Response 202**: 已进入审批（24h 内）

## 3. 状态机

| 实体 | 转换 |
|------|------|
| `booking` | 无 → 已预约 → 已取消 |
| `package` | available ↔ reserved |

## 4. 缓存

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis | `slot:{id}:status` | 30s | booking 变更时失效 |

## 5. 性能

| 指标 | 目标 |
|------|------|
| 预约 P99 | < 300ms |
| 取消 P99 | < 200ms |

## 6. 安全

- 只能取消自己的 booking
- 防止并发重复预约（唯一索引）

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-014/US-016 | 依赖 | 可约时段 |
| US-017 | 依赖 | 体验套餐 |
| US-032/US-033 | 被依赖 | 签到/确认上课记录 |

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 正常预约 | `test_trial_booking_success` |
| 24h 外取消 | `test_trial_booking_cancel_free` |
| 无体验套餐 | `test_trial_booking_no_package` |
| 时段被占 | `test_trial_booking_slot_taken` |
| 24h 内取消 | `test_trial_booking_cancel_requires_approval` |
