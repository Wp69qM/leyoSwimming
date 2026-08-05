# Design: US-053 管理员账号密码登录

> 本文档对应 `docs/stories/US-053-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-053 是一个 L1 级 US，为 Web 后台管理系统提供独立的账号密码认证入口。核心包括 2 个 API（login/logout）、3 张数据表、1 个登录页、1 套路由守卫、1 个全局导航栏退出菜单。

## Data Model

### 新增表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `admin_user` | CREATE | `id`, `username`, `password_hash`, `name`, `role`, `status`, `created_at`, `updated_at` |
| `admin_session` | CREATE | `id`, `admin_user_id`, `token_hash`, `expires_at`, `revoked_at`, `created_at` |
| `admin_login_log` | CREATE | `id`, `admin_user_id`, `username`, `ip`, `user_agent`, `status`, `reason`, `created_at` |

### 索引

```sql
CREATE UNIQUE INDEX idx_admin_user_username ON admin_user(username);
CREATE INDEX idx_admin_session_token ON admin_session(token_hash);
CREATE INDEX idx_admin_session_admin_user_id ON admin_session(admin_user_id);
```

## API Design

### POST /api/v1/admin/auth/login

- 鉴权：否
- 幂等：否
- Request Body: `{ username: string, password: string }`
- Response 200: `{ token, expires_in: 86400, admin }`
- Response 401: `ADMIN_CREDENTIALS_INVALID` 或 token 过期
- Response 403: `ADMIN_DISABLED`

### POST /api/v1/admin/auth/logout

- 鉴权：是
- 幂等：是
- Request Headers: `Authorization: Bearer {token}`
- Response 200: `{ message: "退出登录成功" }`

### 业务规则

- 使用 bcrypt/scrypt 校验密码哈希
- 账号 `status = 0` 才可登录
- 登录成功后生成单一 JWT token，有效期 24 小时
- 失败日志不区分用户名错还是密码错
- 登录接口限流：同一 IP/用户名 5 分钟内最多 5 次失败
- 前端 localStorage 存储 `admin_token` 和 `admin_user`

## State Machine

### 管理员登录态状态机

```
未登录 ──(登录成功)──→ 已登录
已登录 ──(退出登录)──→ 未登录
```

| 转换 | 触发条件 | 字段变更 |
|------|---------|---------|
| 未登录 → 已登录 | 用户名密码校验通过且账号启用 | 生成 `admin_session` |
| 已登录 → 未登录 | 管理员点击退出登录 | `admin_session.revoked_at` 置为当前时间 |

## Frontend Flow

```
访问后台系统 → 未登录 → 展示登录页
  → 输入用户名/密码 → POST /admin/auth/login
    → 成功 → 存储 token + userInfo 到 localStorage → 跳转后台首页
    → 失败 → 展示错误提示

后台任意页面 → token 过期/无效 → 后端返回 401 → 前端跳转登录页并提示"登录信息已过期，请重新登录"

后台任意页面 → 点击右上角管理员名字 → 展开下拉菜单
  → 点击退出登录 → POST /admin/auth/logout → 清除 localStorage 中的 token + userInfo → 跳转登录页
```

## Security

- 密码 bcrypt/scrypt 哈希存储
- HTTPS 强制
- JWT 单一 token：有效期 24h
- 前端 localStorage 存储 token + userInfo
- 登录限流防暴力破解
- 登录日志审计

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-041 ~ US-049 | 被依赖 | 所有管理后台 US 依赖本 US 提供的认证能力 |
| US-054（待创建）| 相邻 | 管理员账号管理，共用 `admin_user` 表 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-053-.../tech-design.md` §1 |
| API Design | `docs/stories/US-053-.../tech-design.md` §2 |
| State Machine | `docs/stories/US-053-.../tech-design.md` §3 |
| Frontend Flow | `docs/stories/US-053-.../tech-design.md` §4 |
| Security | `docs/stories/US-053-.../tech-design.md` §5 |
