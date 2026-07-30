# Tech Design: US-033 教练确认上课记录

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `booking` | 修改 | 确认后 status → 已完成 |
| `package` | 修改 | 确认扣课时 / 管理员返还 |
| `course_record` | 新增/修改 | 教练填写上课记录 |
| `hour_return` | 新增 | 管理员返还课时记录 |
| `audit_log` | 写 | 确认/返还审计 |

#### hour_return

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `booking_id` | FK |
| `package_id` | FK |
| `admin_id` | FK |
| `reason` | 返还原因 |
| `hours` | 返还课时数，默认 1 |
| `created_at` | 创建时间 |

### 1.2 索引

```sql
CREATE INDEX idx_hour_return_booking ON hour_return(booking_id);
CREATE INDEX idx_hour_return_package ON hour_return(package_id);
CREATE INDEX idx_course_record_booking ON course_record(booking_id);
CREATE INDEX idx_booking_coach_status ON booking(coach_id, status);
CREATE INDEX idx_package_user_status ON package(user_id, status);
```

## 2. API 设计

### 2.1 POST /api/coach/bookings/{booking_id}/confirm

- **鉴权**：教练且为 booking.coach_id
- **Request**: `{ content: string, focus_tags: string[], mastery_level: int, homework: string, media: string[] }`
- **Response 200**: `{ booking_id, status: "已完成", package: { reserved_count, consumed_count, available_count } }`
- **Response 400**: `CLASS_NOT_STARTED | BOOKING_NOT_CONFIRMABLE`
- **Response 403**: `BOOKING_ACCESS_DENIED`

### 2.2 POST /api/admin/bookings/{booking_id}/return-hour

- **鉴权**：管理员
- **Request**: `{ reason: string, hours: int = 1 }`
- **Response 200**: `{ hour_return_id, package: { consumed_count, available_count } }`
- **Response 400**: `NO_CONSUMED_HOUR | INVALID_RETURN_HOURS`

## 3. 状态机

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| `booking` | 待上课/上课中 → 已完成 | 教练确认 |
| `package` | reserved → consumed | 教练确认 |
| `package` | consumed → available | 管理员返还 |
| `package` | active → exhausted | 确认最后一课时 |

## 4. 缓存

- booking 缓存：`booking:{booking_id}`，TTL 300s，确认/返还后删除
- package 缓存：`package:{package_id}`，TTL 300s，变更后删除
- 我的预约列表：`bookings:list:{user_id}`，确认后删除

## 5. 性能

| 指标 | 目标 |
|------|------|
| 教练确认接口 P99 | < 300ms |
| 管理员返还接口 P99 | < 300ms |
| 并发确认 | 单 booking 串行（分布式锁/乐观锁）|

## 6. 安全

- 教练只能确认自己的课程
- 管理员返还需记录原因与审计日志
- 幂等键防止重复确认
- 防止课程未开始或已终态的确认

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-029 | 依赖 | 已存在 booking |
| US-032 | 依赖 | 学员签到/记录页已存在 |
| US-034 | 被依赖 | 管理员查看上课记录 |

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 教练确认上课 | `test_coach_confirm_class` |
| 管理员返还课时 | `test_admin_return_hour` |
| 课程尚未开始 | `test_confirm_class_not_started` |
| booking 已取消 | `test_confirm_cancelled_booking` |
| 非本课程教练 | `test_confirm_forbidden_coach` |
