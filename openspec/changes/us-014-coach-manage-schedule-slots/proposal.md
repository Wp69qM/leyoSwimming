## Why

教练管理可约时段是学员能够预约课程的前提条件。教练通过维护未来可约时段，向学员展示自己的可用时间；学员在教练详情页看到可约时段后，才能进入预约流程。本 US 支撑学员单课/套餐预约（US-018/US-029）、系统自动释放下周时段（US-016）、学员候补与关注（US-023）等后续功能。可约时段冲突校验、已预约时段保护、批量复制上周排班是保障教练与学员权益的核心规则。

PRD [§5.4.2](../../../docs/prd/prd.md) 要求教练可管理自己的可约时段；[§5.3.2](../../../docs/prd/prd.md) 要求同一日同时段只能预约 1 节、每周三上午 10:00 释放下周时段；[§5.5.2](../../../docs/prd/prd.md) 要求管理员可配置下周可约时间并一键发布（由 US-015/US-016/US-044 负责，本 US 仅维护教练端可约时段模板与实例）。

## What Changes

- 新增 `GET /api/coach/schedule-slots` 接口（获取可约时段列表）
- 新增 `POST /api/coach/schedule-slots` 接口（批量添加可约时段）
- 新增 `PUT /api/coach/schedule-slots/{id}` 接口（修改可约时段）
- 新增 `DELETE /api/coach/schedule-slots/{id}` 接口（删除可约时段）
- 新增 `POST /api/coach/schedule-slots/copy-last-week` 接口（复制上周排班）
- 新增 `schedule_slot` 表：存储教练可约时段（`slot_id`、`coach_id`、`start_time`、`end_time`、`status`）
- 新增 `coach_schedule_log` 表：记录排班变更审计日志
- 读取 `booking` 表判断时段是否已被预约
- 读取 `venue_closure` 表在复制上周排班时跳过闭馆日期
- 新增唯一索引 `uk_coach_start_time (coach_id, start_time)` 保证时段唯一性
- 新增 Redis 缓存 `coach:slots:{coach_id}:{date}`（TTL 5 分钟），排班变更后立即失效
- 新增教练端「我的 → 排班管理」周历/列表视图与添加时段弹窗
- 边界处理：时段冲突校验、过去时间禁止、已预约时段禁止删除、跨天时段支持、闭馆日期跳过

## Capabilities

### New Capabilities

- `coach-manage-schedule-slots`: 已审核教练添加、修改、删除、批量复制可约时段，系统校验时段冲突、过去时间、已预约保护，学员端实时展示可约时段

### Modified Capabilities

- `admin-review-coach-application`: 审核通过后 coach.status = 1，才允许管理可约时段
- `coach-manage-realtime-status`: 实时状态自动切换依赖 schedule_slot 与 booking 时间判断

## Impact

- **数据表**：新增 `schedule_slot`；新增 `coach_schedule_log`；读取 `booking`、`venue_closure`
- **API**：新增 5 个端点（均需教练登录鉴权且 `coach.status = 1`）
- **缓存**：新增 Redis key `coach:slots:{coach_id}:{date}`（TTL 5 分钟），排班变更后立即失效
- **状态机**：`schedule_slot.status` 无 → available（本 US）；available → booked（后续 US-018/US-029）
- **前端**：新增教练端排班管理页（周历/列表视图）、添加时段弹窗
- **依赖**：依赖 US-011 审核通过；支撑 US-016 系统自动释放、US-018/US-029 学员预约、US-023 候补关注
- **安全**：仅 `coach.status = 1` 可操作；删除/修改校验教练所有权；已预约时段禁止删除；操作记录审计日志
