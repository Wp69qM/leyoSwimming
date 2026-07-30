# Tech Design: US-029 学员预约正价课程

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `package` | 读写 | FIFO 选择 active 套餐并预占课时 |
| `booking` | 写 | 创建预约记录 |
| `schedule_slot` | 读 | 校验时段有效 |
| `user` | 读 | 校验登录与身份 |

#### booking

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `user_id` | FK |
| `coach_id` | FK |
| `package_id` | FK |
| `schedule_slot_id` | FK |
| `status` | 已预约 / 待上课 / 上课中 / 已完成 / 已取消 / 旷课 |
| `start_time` | 课程开始时间 |
| `end_time` | 课程结束时间 |
| `created_at` | 创建时间 |

### 1.2 索引

```sql
CREATE UNIQUE INDEX idx_booking_slot_user ON booking(schedule_slot_id, user_id) WHERE status != '已取消';
CREATE INDEX idx_booking_user_status ON booking(user_id, status);
CREATE INDEX idx_package_user_status ON package(user_id, status);
CREATE INDEX idx_schedule_slot_coach_time ON schedule_slot(coach_id, start_time);
```

## 2. API 设计

### 2.1 GET /api/coaches/{coach_id}/slots

- **鉴权**：必须登录
- **Query**: `start_date`, `end_date`
- **Response 200**: `{ items: Slot[] }`

### 2.2 GET /api/packages/active

- **鉴权**：必须登录
- **Response 200**: `{ items: Package[] }`

### 2.3 POST /api/bookings

- **鉴权**：必须登录
- **Request**:
  ```json
  {
    "coach_id": 100,
    "schedule_slot_id": 200,
    "idempotency_key": "uuid"
  }
  ```
- **Response 201**: `{ booking_id, status, start_time, end_time }`
- **Response 400**: `SLOT_TAKEN | NO_QUOTA | NO_BOUND_COACH | SLOT_NOT_AVAILABLE | PACKAGE_NOT_BOOKABLE`
- **Response 409**: `DUPLICATE_BOOKING`

## 3. 状态机

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| `booking` | 无 → 已预约 | 预约校验通过 |
| `package` | available → reserved | 预约成功 |

## 4. 缓存

- 可约时段缓存：`slots:{coach_id}:{date}`，TTL 60s，预约成功后删除
- package 缓存：`package:{package_id}`，TTL 300s，预占后删除
- 分布式锁：`booking:lock:{schedule_slot_id}`，TTL 5s，防止并发冲突

## 5. 性能

| 指标 | 目标 |
|------|------|
| 时段列表查询 P99 | < 200ms |
| 预约提交接口 P99 | < 300ms |
| 并发预约冲突处理 | 1000 QPS P99 < 500ms |

## 6. 安全

- 严格校验订单归属与套餐归属
- 禁止预约他人套餐
- 幂等键防重放
- 时段释放校验防止预约未释放时段

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-014 | 依赖 | 教练可约时段 |
| US-020 | 依赖 | active 正价套餐 |
| US-030 | 被依赖 | 取消/改约正价课程 |
| US-031 | 被依赖 | 教练代约/改约正价课程 |
| US-032 | 被依赖 | 学员签到/签退课程 |
| US-033 | 被依赖 | 教练确认上课记录 |

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 正常预约 | `test_booking_formal_success` |
| 时段已被预约 | `test_booking_formal_slot_taken` |
| 无可用课时 | `test_booking_formal_no_quota` |
| 未绑定教练 | `test_booking_formal_no_bound_coach` |
| 重复提交 | `test_booking_formal_idempotent` |
