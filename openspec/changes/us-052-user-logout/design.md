# Design: US-052 用户退出登录

> 本文档对应 `docs/stories/US-052-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-052 是一个简单的 L1 级 US，核心是 1 个写入 API + 小程序「我的」页面退出登录按钮 + 二次确认弹窗。本 US 触发登录态状态机 `已登录 → 未登录`，但不修改用户账号生命周期字段。

## Data Model

### 修改的表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `user_session` | UPDATE | `id`, `user_id`, `refresh_token_hash`, `expires_at`, `revoked_at`, `created_at` |

### user_session 表变更

退出登录时，按 `refresh_token_hash` 定位当前会话记录，将 `revoked_at` 置为当前时间（或删除记录）。

### 索引

```sql
CREATE INDEX idx_user_session_refresh_token ON user_session(refresh_token_hash);
CREATE INDEX idx_user_session_user_id ON user_session(user_id);
```

## API Design

### POST /api/user/auth/logout

- 鉴权：是（需有效 access_token）
- 幂等：是（同一 token 重复调用视为已退出）
- Request Headers: `Authorization: Bearer {access_token}`
- Request Body: `{ refreshToken?: string }`
- Response 200: `{ code: 0, message: "退出登录成功", data: null }`
- Response 401: `{ code: 40101, message: "登录态已失效，请重新登录", data: null }`

### 业务规则

- 后端解析并校验 `access_token` 有效性
- 若请求体包含 `refreshToken`，按 SHA-256 hash 查找 `user_session` 并标记失效
- 事务边界：标记 session 失效 + 记录 logout 日志在同一事务内
- MVP 阶段 access_token 不强制加入黑名单，依赖 2h 短有效期自然过期

## State Machine

### 登录态状态机

```
已登录 ──(US-052 用户确认退出)──→ 未登录
```

本 US 触发的转换：

| 转换 | 触发条件 | 字段变更 |
|------|---------|---------|
| 已登录 → 未登录 | 用户点击「退出登录」并确认 | `user_session.revoked_at` 置为当前时间；前端清除 token |

> 注意：本 US 不修改 `user.status` 或 `user.identity_status`。

## Frontend Flow

```
「我的」页面 → 点击「退出登录」 → 弹出确认弹窗
  → 用户点击「取消」 → 关闭弹窗，保持登录态
  → 用户点击「确定」 → 调用 POST /api/user/auth/logout → 清除本地 token → 刷新「我的」页面为未登录态
```

## Security

- `POST /api/user/auth/logout` 必须登录鉴权
- 退出后 `refresh_token` 必须失效
- 前端清除 storage 时需同时清除内存与持久化 storage
- 多设备场景下仅失效当前 session，不影响其他设备

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-004 | 被本 US 依赖 | 微信授权登录创建会话与 token 机制 |
| US-006 | 被本 US 依赖 | 手机号验证码登录创建会话与 token 机制 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-052-.../tech-design.md` §1 |
| API Design | `docs/stories/US-052-.../tech-design.md` §2 |
| State Machine | `docs/stories/US-052-.../tech-design.md` §3 |
| Frontend Flow | `docs/stories/US-052-.../tech-design.md` §4 |
| Security | `docs/stories/US-052-.../tech-design.md` §5 |
