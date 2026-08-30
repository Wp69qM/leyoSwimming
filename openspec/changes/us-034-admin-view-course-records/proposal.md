## Why

US-029~US-033 完成了学员预约、取消/改约、签到、教练确认等上课流程，但管理员缺乏一个统一入口查看所有上课记录、统计课时消耗。本 US 让管理员在后台查看所有上课记录，支撑运营分析、客诉处理与财务对账。

## What Changes

- 新增 `GET /api/admin/course-records`：管理员查询上课记录列表，支持教练、学员、状态、时间范围筛选与分页
- 新增 `GET /api/admin/course-records/{booking_id}`：管理员查看单条上课记录详情
- 新增 `GET /api/admin/course-records/export`：导出筛选结果（CSV/Excel，可选）
- 新增 web-admin「上课记录」页面，默认展示最近 30 天数据
- 列表展示：课程时间、教练、学员、状态、消耗课时、课程内容简介
- 详情展示：教练记录、学员课后总结、课时变动日志

## Capabilities

### New Capabilities

- `admin-view-course-records`: 管理员查看上课记录，含列表筛选、详情、导出

### Modified Capabilities

- `coach-confirm-course-record` (US-033): 提供 course_record 与课时消耗数据供管理端展示

## Impact

- **数据表**：读取 `booking`、`course_record`、`package`、`user`、`audit_log`
- **API**：新增 3 个管理端端点
- **缓存**：管理端上课记录列表与详情缓存
- **前端**：新增 web-admin 上课记录列表/详情页
- **依赖**：依赖 US-029、US-033
