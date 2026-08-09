## Why

后台管理系统需要独立的账号管理能力，让 `super_admin` 集中维护管理员账号（新建、查看、编辑、禁用/启用、删除、重置密码），保障后台权限可控与账号安全，避免无管理员可用或权限失控。

## What Changes

- 管理后台新增「系统设置 → 管理员账号」模块
- `super_admin` 可查看管理员账号列表，支持按角色、状态、关键词筛选
- `super_admin` 可新建管理员账号，设置登录账号、姓名、初始密码、角色
- `super_admin` 可编辑管理员资料（姓名、角色）
- `super_admin` 可禁用/启用管理员账号，禁用后账号无法登录
- `super_admin` 可删除管理员账号（逻辑删除），删除后相关会话失效
- `super_admin` 可重置管理员密码，生成随机强密码并展示一次
- `admin` 角色仅可查看管理员列表和查看自身资料详情
- 所有敏感操作写入 `admin_audit_log`

## Capabilities

### New Capabilities

- `admin-manage-admin-accounts`: 超级管理员管理后台管理员账号

### Modified Capabilities

- 无

## Impact

- **数据表**：复用并写入 `admin_user` 表；新增/更新 `admin_session` 会话失效；新增 `admin_audit_log` 记录写操作
- **API**：新增 7 个端点 `POST /api/admin/admin/list`、`POST /api/admin/admin/detail`、`POST /api/admin/admin/add`、`POST /api/admin/admin/update`、`POST /api/admin/admin/toggle-status`、`POST /api/admin/admin/delete`、`POST /api/admin/admin/reset-password`
- **状态机**：管理员账号启用状态 `0 ⇄ 1`；管理员账号存在性 `存在 → 已删除`
- **前端**：新增管理员账号列表页、查看弹窗、新建/编辑弹窗、禁用/启用确认、删除确认、重置密码弹窗
- **依赖**：US-053（管理后台登录认证与 `admin_user` 表）
- **影响**：为所有管理后台 US 提供操作者账号生命周期维护
