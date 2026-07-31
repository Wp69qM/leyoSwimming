## Why

场馆运营需要管理员统一查看和调整教练排班，以应对临时闭馆、教练请假、学员需求变化等情况。同时，教练提交的请假申请必须由管理员审批，审批通过后需要自动取消受影响课程并通知学员，避免学员到场无课可上。

## What Changes

- 管理后台新增「排班管理」页面，支持按教练、日期范围查看排班
- 管理员可对排班进行新增、修改、删除操作，修改时校验时间冲突
- 管理后台新增「请假审批」队列，列出 pending 状态的教练请假申请
- 管理员可通过或拒绝请假申请
- 请假通过后，系统将请假日期内未来 schedule_slot 标记为 disabled（不物理删除，便于复用），自动取消请假时段内已预约但未上课的 booking，释放 package.reserved，并向学员发送包含请假原因与改约建议的通知
- 删除已有预约的排班时段时，二次确认后取消课程并通知

## Capabilities

### New Capabilities

- `admin-manage-coach-schedule-leave`: 管理员管理教练排班并审批请假

### Modified Capabilities

- 无

## Impact

- 后端：新增 `/api/admin/v1/coaches/{id}/schedule-slots`、`/api/admin/v1/schedule-slots/{id}`、`/api/admin/v1/leave-requests/*` 系列接口、时间冲突校验、请假审批事务与通知
- 管理端 Web：新增排班管理页（周视图）、请假审批队列页
- 依赖：US-014（教练管理可约时段）、US-036（教练提交请假申请）
- 影响：US-029 / US-030（学员预约/取消改约）
