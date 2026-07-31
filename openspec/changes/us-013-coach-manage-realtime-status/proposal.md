## Why

教练管理实时状态是让学员了解教练当前是否可约/可聊的关键功能。教练可手动设置实时状态（空闲中、上课中、休息中、已下班、请假中），系统也可根据课程安排自动切换状态（课前 15 分钟上课中，课后 15 分钟恢复）。实时状态在学员端教练列表与详情页以圆点/标签展示，直接影响学员约课决策与体验。本 US 支撑后续 US-029 学员预约课程时的教练状态展示。

PRD [§5.4.1](../../../docs/prd/prd.md) 要求教练可设置实时状态，系统根据课程安排自动切换状态，教练手动设置的状态优先级高于自动状态，但请假状态由请假审批自动同步；[§5.3.1](../../../docs/prd/prd.md) 要求教练实时状态在列表/详情以圆点/标签展示。

## What Changes

- 新增 `GET /api/coach/realtime-status` 接口（获取当前实时状态）
- 新增 `PUT /api/coach/realtime-status` 接口（手动更新实时状态）
- 新增 `GET /api/admin/coach/realtime-status` 接口（管理员查看教练状态）
- 修改 `coach` 表：更新 `realtime_status`、`status_override_flag`、`override_until`
- 新增 `coach_status_log` 表：记录状态变更历史（来源 manual/auto）
- 新增定时任务：每分钟扫描未来 15 分钟内有课程的教练，自动切换为上课中；课程结束后 15 分钟自动恢复
- 新增 WebSocket/SSE 广播通道：状态变更后实时推送给查看该教练的学员端
- 新增教练端首页状态切换组件与学员端状态标签组件
- 边界处理：请假中状态不可手动修改且优先级最高（覆盖手动/自动状态）、非请假时段手动状态覆盖自动状态、课程期间自动状态优先级更高

## Capabilities

### New Capabilities

- `coach-manage-realtime-status`: 已审核教练管理实时状态，支持手动切换与按课程安排的自动切换；审批通过的请假时段内实时状态强制为「请假中」并锁定手动修改（优先级最高），非请假时段手动状态优先级高于自动状态，状态变更通过实时通道广播给学员端

### Modified Capabilities

- `admin-review-coach-application`: 审核通过后 coach.status = 1，才允许使用实时状态功能
- `coach-manage-leave`（US-036）: 请假审批通过后自动将教练实时状态置为请假中

## Impact

- **数据表**：修改 `coach`（新增 `realtime_status`、`status_override_flag`、`override_until`）；新增 `coach_status_log`；读取 `booking` 判断课程时间
- **API**：新增 3 个端点（`GET /api/coach/realtime-status`、`PUT /api/coach/realtime-status`、`GET /api/admin/coach/realtime-status`）
- **缓存**：新增 Redis key `coach:realtime:{coach_id}`（TTL 5 分钟）供学员端展示
- **状态机**：`coach.realtime_status` 任意状态 ↔ 任意状态（手动/自动切换），请假中状态由请假审批同步
- **前端**：新增教练端状态切换 Action Sheet 与学员端状态标签
- **依赖**：依赖 US-011 审核通过；与 US-036 请假审批关联；影响 US-029 学员端状态展示
- **安全**：仅 `coach.status = 1` 可操作；请假中状态锁定不可手动修改；记录状态变更日志
