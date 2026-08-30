## Why

教练需要在教学过程中查看学员在 US-005 中完善的完整档案，并记录学员的学习特征、沟通备注以及未成年人监护人信息，以便提供个性化教学并满足未成年人合规要求。当前系统仅有学员自行维护的基础资料，缺少教练视角的「信息切片」与备注能力。

## What Changes

- 教练端新增「我的学员」列表与学员详情/编辑页
- 教练可查看学员完整资料：US-005 自主档案（头像、姓名、手机号脱敏、年龄、性别、游泳基础、是否未成年人等，只读）+ 教练视角切片
- 教练可更新学员信息切片字段：学习泳姿、游泳等级、基础情况、沟通备注
- 系统仅允许教练维护与自己存在 booking/package 关联的学员
- 教练修改的信息切片不影响 `user` 表中的学员本人资料
- 敏感字段加密存储，操作记录审计日志

## Capabilities

### New Capabilities

- `coach-view-student-profile`: 教练查看关联学员完整资料（含 US-005 只读档案）
- `coach-manage-student-info`: 教练维护关联学员信息切片与备注（含未成年人及监护人信息）

### Modified Capabilities

- 无

## Impact

- 后端：新增 `coach_student_profile` 表、`POST /api/coach/student/{list|detail|update|package/list}` 系列接口、字段校验与加密逻辑；detail 接口需联查 `user` 表返回 US-005 字段
- 教练端小程序：新增「我的学员」页面、学员详情/编辑页；US-005 字段仅展示不可编辑
- 管理端：审计日志中新增 `UPDATE_STUDENT_PROFILE` 操作类型
- 依赖：US-005（用户完善个人资料）、US-012（教练主页管理）
