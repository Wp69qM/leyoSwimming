# US-009 用户隐私协议授权与撤回 — 技术设计

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## 1. 数据模型

### 1.1 涉及的表

| 表名 | 操作 | 说明 |
|------|------|------|
| `user_privacy_consent` | 新增/修改 | 记录同意/撤回 |
| `privacy_policy` | 新增 | 协议版本管理 |
| `audit_log` | 新增 | 审计日志 |

### 1.2 字段定义

**user_privacy_consent 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `consent_id` | BIGINT | PK | ID |
| `user_id` | BIGINT | FK | 用户 ID |
| `version` | VARCHAR(16) | 非空 | 协议版本 |
| `status` | TINYINT | 默认 1 | 1=同意, 2=撤回 |
| `agreed_at` | DATETIME | 可空 | 同意时间 |
| `revoked_at` | DATETIME | 可空 | 撤回时间 |

**privacy_policy 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `version` | VARCHAR(16) | PK | 版本号 |
| `content` | TEXT | 非空 | 协议内容 |
| `effective_at` | DATETIME | 非空 | 生效时间 |
| `is_current` | TINYINT | 默认 0 | 是否当前生效 |

---

## 2. API 设计

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/privacy-policy/current` | GET | 获取当前生效协议 |
| `/api/user/privacy/status` | GET | 查询用户授权状态 |
| `/api/user/privacy/consent` | POST | 提交同意/撤回 |

### 2.1 POST /api/user/privacy/consent

- **请求体**：
  ```json
  {
    "version": "v2.0",
    "action": "agree"
  }
  ```
- **响应体**：
  ```json
  {
    "code": 0,
    "data": {
      "status": "agreed",
      "version": "v2.0"
    }
  }
  ```

---

## 3. 状态机

```
未同意 ──[同意]──→ 已同意 ──[撤回]──→ 已撤回
```

---

## 4. 缓存策略

| 缓存 | 键 | TTL | 说明 |
|------|----|-----|------|
| 当前协议 | `privacy_policy:current` | 1 小时 | 当前生效协议内容 |
| 用户授权状态 | `user:privacy:{user_id}` | 30 分钟 | 用户同意状态 |

---

## 5. 性能与安全

### 5.1 性能

- 协议查询 P99 < 100ms
- 同意/撤回接口 P99 < 200ms

### 5.2 安全

- 协议内容不可篡改（仅管理员可更新版本）
- 同意/撤回记录不可删除，仅可追加
- 撤回后限制非必要数据收集

---

## 6. 跨 US 依赖

- 依赖 US-005 用户身份
- 与 US-008 账号安全设置相邻
