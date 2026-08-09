> **OpenSpec Design | 映射自 `docs/stories/US-007-用户-账号注销/user-story.md` §7**

## 数据模型

| 表名 | 字段 | 类型 | 说明 |
|------|------|------|------|
| user | status | TINYINT | 0=正常，1=软删除，2=封禁 |
| user | deleted_at | TIMESTAMP | 注销时间 |
| user | phone | VARCHAR(64) | AES-256 加密 |
| user_identity_log | event_type | ENUM | 'cancel' |
| user_identity_log | user_id | BIGINT | 关联 user |
| user_identity_log | created_at | TIMESTAMP | 事件时间 |
| user_session | token_hash | VARCHAR(255) | 登录态，注销后删除 |
| order | status | ENUM | 校验未完成订单 |
| package_purchase | status | ENUM | 校验 active 套餐 |
| booking | status | ENUM | 校验进行中/待上课预约 |

## API

### GET /api/user/account/cancel/check

- 鉴权：access_token
- Response 200: `{ can_cancel: boolean, reasons: ['active_package'|'pending_order'|'active_booking'] }`

### POST /api/user/account/cancel

- 鉴权：access_token
- Request: `{ confirm: true }`（MVP 简化，仅前端弹窗确认后调用）
- Response 200: `{ success: true }`
- Response 409: `ACTIVE_PACKAGE_EXISTS` / `PENDING_ORDER_EXISTS` / `ACTIVE_BOOKING_EXISTS`

## 核心流程

1. 用户进入「我的 → 注销账号」
2. 前端调用 `GET /api/user/account/cancel/check`
3. 后端校验 user 是否存在、status=0
4. 后端查询 active 套餐、未完成订单、进行中预约
5. 后端返回 `can_cancel` 与未满足原因列表
6. 前端展示风险提示 + checklist
7. 用户点击「确认注销」
8. 前端展示二次确认弹窗
9. 用户点击弹窗「确认」
10. 前端调用 `POST /api/user/account/cancel`
11. 后端再次检查注销条件
12. 后端更新 `user.status=1`、`deleted_at=now()`
13. 后端删除 `user_session` 全部记录
14. 后端写入 `user_identity_log`
15. 返回成功，前端清除本地 token，跳转登录页

## 安全

- 幂等：以 `cancel:{user_id}:{date}` 为幂等键，防止重复注销
- 事务：账号状态更新、会话删除、日志记录在同一事务
- 审计：注销操作必须记录审计日志
- 防误触：前端二次确认弹窗 + 后端条件二次校验

## 数据保留

- 订单、交易记录保留 90 天后匿名化
- user 软删除记录保留，用于审计与反欺诈
