# US-007 用户账号注销 — 技术设计

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## 1. 数据模型

### 1.1 涉及的表

| 表名 | 操作 | 说明 |
|------|------|------|
| `user` | 修改 | status = 1（软删除）, deleted_at 赋值 |
| `user_session` | 删除记录 | 清除所有登录态 |
| `audit_log` | 新增 | 记录注销审计日志 |

### 1.2 字段定义

**user 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `user_id` | BIGINT | PK | 用户 ID |
| `status` | TINYINT | 默认 0 | 0=正常, 1=软删除, 2=封禁（PRD §9.2.1） |
| `deleted_at` | DATETIME | 可空 | 注销时间 |
| `anonymous_after` | DATETIME | 可空 | 90 天后匿名化时间 |

---

## 2. API 设计

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/account/cancel-check` | POST | 查询是否满足注销条件 |
| `/api/user/account/cancel` | POST | 提交注销申请 |

### 2.1 POST /api/user/account/cancel-check

- **鉴权**：是（需登录态）
- **请求体**：
  ```json
  {}
  ```
- **响应体**：
  ```json
  {
    "code": 0,
    "data": {
      "can_cancel": true,
      "checks": {
        "no_active_package": true,
        "no_pending_order": true,
        "no_ongoing_booking": true
      }
    }
  }
  ```
- **错误码**：`AUTH_001`（未登录/Token 无效）

> 说明：与 user-story §4.1 步骤 2-4 对应，进入注销页时前置展示 checklist。

### 2.2 POST /api/user/account/cancel

- **鉴权**：是（需登录态）
- **请求体**：
  ```json
  {}
  ```
  > 注：MVP 阶段仅通过弹窗二次确认，无需短信验证码或协议版本号。
- **响应体**：
  ```json
  {
    "code": 0,
    "data": {
      "cancelled": true,
      "anonymous_after": "2026-10-28T12:00:00Z"
    }
  }
  ```
- **错误码**：`ACTIVE_PACKAGE_EXISTS` (400201), `PENDING_ORDER_EXISTS` (400202), `ONGOING_BOOKING_EXISTS` (400203)

---

## 3. 状态机

```
正常(0) ──[用户确认注销]──→ 软删除(1)
```

- 注销后不可恢复，再次登录视为新用户

---

## 4. 缓存策略

| 缓存 | 键 | TTL | 说明 |
|------|----|-----|------|
| 注销条件缓存 | `cancel:check:{user_id}` | 1 分钟 | 减少重复查询 |
| 会话缓存 | `session:{token}` | 立即清除 | 注销后失效 |

---

## 5. 性能与安全

### 5.1 性能

- 注销接口 P99 < 300ms

### 5.2 安全

- MVP 阶段二次确认采用弹窗确认/取消，无需密码或验证码
- 注销前校验无 active 套餐、无未完成订单、无进行中预约
- 记录审计日志（操作人、时间、IP、设备）
- 90 天后匿名化处理（定时任务）

---

## 6. 跨 US 依赖

- 依赖 US-004 登录态
- 与 US-020/US-025 等购课订单状态联动
