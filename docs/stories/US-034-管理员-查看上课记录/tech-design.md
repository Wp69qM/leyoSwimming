# Tech Design: US-034 管理员查看上课记录

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `booking` | 读 | 上课记录列表与详情主表 |
| `course_record` | 读 | 详情展示教练记录与学员总结 |
| `package` | 读 | 展示消耗课时与套餐信息 |
| `user` | 读 | 展示学员/教练姓名 |
| `audit_log` | 读 | 展示课时变动日志（可选）|

#### 查询视图字段

| 字段 | 来源 | 说明 |
|------|------|------|
| `booking_id` | booking | 预约 ID |
| `start_time` | booking | 课程开始时间 |
| `end_time` | booking | 课程结束时间 |
| `status` | booking | 已预约 / 待上课 / 上课中 / 已完成 / 已取消 / 旷课 |
| `coach_name` | user | 教练姓名 |
| `student_name` | user | 学员姓名 |
| `content` | course_record | 课程内容 |
| `consumed_hours` | package | 本节课消耗的课时数（默认 1）|
| `student_summary` | course_record | 学员课后总结 |

### 1.2 索引

```sql
CREATE INDEX idx_booking_status_start_time ON booking(status, start_time DESC);
CREATE INDEX idx_booking_coach_status ON booking(coach_id, status);
CREATE INDEX idx_booking_user_status ON booking(user_id, status);
CREATE INDEX idx_course_record_booking ON course_record(booking_id);
```

## 2. API 设计

### 2.1 GET /api/admin/course-records

- **鉴权**：管理员且拥有 `course_record:read` 权限
- **Query**:
  - `coach_id?: int`
  - `student_id?: int`
  - `status?: string`（可逗号分隔多个状态）
  - `start_date?: date`（YYYY-MM-DD）
  - `end_date?: date`（YYYY-MM-DD，最大跨度 1 年）
  - `page?: int = 1`
  - `size?: int = 20`（最大 100）
- **Response 200**: `{ items: CourseRecordListItem[], total, page, size }`
- **Response 400**: `DATE_RANGE_TOO_LARGE | INVALID_PAGE | INVALID_SIZE`
- **Response 403**: `ADMIN_PERMISSION_DENIED`

### 2.2 GET /api/admin/course-records/{booking_id}

- **鉴权**：管理员且拥有 `course_record:read` 权限
- **Response 200**: `{ booking_id, status, start_time, end_time, coach, student, package, course_record, audit_logs }`
- **Response 404**: `COURSE_RECORD_NOT_FOUND`
- **Response 403**: `ADMIN_PERMISSION_DENIED`

### 2.3 GET /api/admin/course-records/export（可选）

- **鉴权**：管理员且拥有 `course_record:read` 权限
- **Query**: 同列表查询
- **Response 200**: CSV/Excel 文件流
- **Response 400**: `DATE_RANGE_TOO_LARGE | EXPORT_LIMIT_EXCEEDED`

## 3. 状态机

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| 无 | — | 本 US 为只读查询，不触发状态转换 |

## 4. 缓存

- 上课记录列表：`admin:course-records:{hash}`，TTL 60s，状态变更后删除
- 单条详情：`admin:course-record:{booking_id}`，TTL 300s，关联数据变更后删除

## 5. 性能

| 指标 | 目标 |
|------|------|
| 列表查询 P99 | < 300ms |
| 详情查询 P99 | < 200ms |
| 导出 10000 条 | < 5s |
| 单页最大 100 条 | 响应 < 200ms |

## 6. 安全

- 严格校验管理员权限
- 防止越权访问其他场馆/数据（后续多租户扩展）
- 导出功能增加审计日志
- 不返回敏感字段（如完整手机号、身份证号）

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-029 | 依赖 | 已存在 booking |
| US-033 | 依赖 | 已存在 course_record |

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 正常查看列表 | `test_admin_list_course_records` |
| 按教练筛选 | `test_admin_filter_course_records_by_coach` |
| 查看详情 | `test_admin_get_course_record_detail` |
| 无权限访问 | `test_admin_course_record_forbidden` |
| 时间范围过大 | `test_admin_course_record_date_range_invalid` |
