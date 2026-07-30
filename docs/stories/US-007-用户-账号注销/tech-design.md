# US-007 用户账号注销 — 技术设计

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## 1. 数据模型

### 1.1 涉及的表

| 表名 | 操作 | 说明 |
|------|------|------|
| `user` | 修改 | status = 2, deleted_at 赋值 |
| `user_session` | 删除 | 清除所有会话 |
| `audit_log` | 新增 | 记录注销审计日志 |

### 1.2 字段定义

**user 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `user_id` | BIGINT | PK | 用户 ID |
| `status` | TINYINT | 默认 1 | 1=正常, 2=注销, 3=封禁 |
| `deleted_at` | DATETIME | 可空 | 注销时间 |
| `anonymous_after` | DATETIME | 可空 | 90 天后匿名化时间 |

---

## 2. API 设计

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/account/cancel/check` | GET | 查询是否满足注销条件 |
| `/api/user/account/cancel` | POST | 提交注销申请 |

### 2.1 POST /api/user/account/cancel

- **请求体**：
  ```json
  {
    "verify_code": "123456",
    "agreement_version": "v1.0"
  }
  ```
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
- **错误码**：`ACTIVE_PACKAGE_EXISTS` (400201), `PENDING_ORDER_EXISTS` (400202)

---

## 3. 状态机

```
正常(1) ──[用户确认注销]──→ 已注销(2)
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

- 必须二次验证（密码或验证码）
- 注销前校验无 active 套餐、无未完成订单
- 记录审计日志（操作人、时间、IP、设备）
- 90 天后匿名化处理（定时任务）

---

## 6. 跨 US 依赖

- 依赖 US-004 登录态
- 与 US-020/US-025 等购课订单状态联动
