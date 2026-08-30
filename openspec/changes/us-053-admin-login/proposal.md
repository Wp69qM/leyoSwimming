## Why

后台管理系统目前缺少独立的认证入口，导致所有管理后台 US（US-041 ~ US-049）缺乏前置登录能力。必须为管理员提供账号密码登录入口，确保只有授权人员可访问管理功能。

PRD [§10.1](../../../docs/prd/prd.md) 要求后台管理系统通过独立的管理员账号密码登录，管理员账号由系统预置或超级管理员创建，不支持自助注册。

## What Changes

- 新增 `admin_user` 表：存储管理员用户名、密码哈希、姓名、角色、状态
- 新增 `admin_session` 表：存储单一管理员会话 token（有效期 24 小时）
- 新增 `admin_login_log` 表：记录登录成功/失败日志
- 新增 `POST /api/admin/auth/login` 接口：管理员账号密码登录，返回单一 token（24h）和 userInfo
- 新增 `POST /api/admin/auth/logout` 接口：管理员退出登录
- 新增 Web 管理员登录页：居中卡片式布局，含用户名、密码输入框和登录按钮
- 新增 Web 路由守卫与请求拦截器：token 缺失/过期自动跳转登录页并提示"登录信息已过期，请重新登录"
- 新增全局导航栏右上角管理员名字下拉菜单：含退出登录选项，页面展示 userInfo
- 触发管理员登录态状态机：未登录 → 已登录（登录成功）；已登录 → 未登录（退出登录 / token 过期）
- 边界处理：用户名/密码错误返回 401；账号禁用返回 403；token 过期后端返回 401 前端自动跳转登录页；登录接口限流防止暴力破解

## Capabilities

### New Capabilities

- `admin-login`: 管理员通过用户名密码登录后台管理系统
- `admin-logout`: 管理员通过右上角下拉菜单退出登录

### Modified Capabilities

- （无——本 US 仅新增登录认证能力）

## Impact

- **数据表**：新增 `admin_user`、`admin_session`（单一 token，24h）、`admin_login_log`
- **API**：新增 2 个端点 `POST /api/admin/auth/login`、`POST /api/admin/auth/logout`
- **状态机**：新增管理员登录态状态机 `未登录 ⇄ 已登录`
- **前端**：新增 Web 管理员登录页、路由守卫与 401 拦截器、全局导航栏退出菜单
- **依赖**：本 US 无前置依赖；所有管理后台 US（US-041 ~ US-049）依赖本 US
- **安全**：密码哈希存储、登录限流、JWT 单一 token（24h）、HTTPS、登录日志审计
