# US-009 用户隐私协议授权与撤回 — 技术设计

> **状态**：已评审　|　**最后更新**：2026-07-31

---

## 1. 数据模型

### 1.1 涉及的表

| 表名 | 操作 | 说明 |
|------|------|------|
| `privacy_policy` | 新增 | 协议版本与内容，仅管理员可写入 |
| `user_privacy_consent` | 新增/修改 | 记录用户对每个版本的同意/撤回 |
| `audit_log` | 新增 | 审计日志（复用 PRD §9.2.14） |

### 1.2 字段定义

**`privacy_policy` 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `version` | VARCHAR(32) | PK | 版本号，如 `v2.0` |
| `content` | TEXT | NOT NULL | 协议正文（Markdown / 富文本） |
| `effective_at` | DATETIME | NOT NULL | 生效时间 |
| `is_current` | TINYINT | 默认 0 | `1`=当前生效，同一时刻仅 1 条可为 1 |
| `created_at` | DATETIME | — | 创建时间 |

**`user_privacy_consent` 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `consent_id` | BIGINT | PK | 自增 ID |
| `user_id` | BIGINT | FK, UK(user_id, version) | 用户 ID |
| `version` | VARCHAR(32) | 非空 | 协议版本 |
| `status` | ENUM('agreed','revoked') | 非空 | 同意 / 撤回 |
| `agreed_at` | DATETIME | 可空 | 同意时间 |
| `revoked_at` | DATETIME | 可空 | 撤回时间 |
| `created_at` | DATETIME | — | 创建时间 |
| `updated_at` | DATETIME | — | 更新时间 |

### 1.3 索引

```sql
-- 同一用户对同一版本仅保留一条记录，保证幂等
CREATE UNIQUE INDEX idx_user_privacy_user_version
  ON user_privacy_consent(user_id, version);

-- 当前生效协议查询
CREATE INDEX idx_privacy_policy_current
  ON privacy_policy(is_current, effective_at);
```

---

## 2. API 设计

| 接口 | 方法 | 鉴权 | 说明 |
|------|------|------|------|
| `/api/privacy-policy/current` | GET | 否（游客可见） | 获取当前生效协议 |
| `/api/user/privacy/status` | GET | 是 | 查询当前用户授权状态 |
| `/api/user/privacy/consent` | POST | 是 | 提交同意 / 撤回 |

### 2.1 GET /api/privacy-policy/current

- **鉴权**：否（游客可见）
- **Response 200**：
  ```json
  {
    "version": "v2.0",
    "content": "<协议正文>",
    "effective_at": "2026-07-31T00:00:00+08:00"
  }
  ```
- **Response 404**：`NO_CURRENT_PRIVACY_POLICY`

### 2.2 GET /api/user/privacy/status

- **鉴权**：是（游客调用返回 `401 UNAUTHORIZED`）
- **Response 200**：
  ```json
  {
    "status": "agreed",
    "version": "v2.0",
    "required_version": "v2.0"
  }
  ```
  - `status` 取值：`agreed` / `revoked` / `none`
  - 当 `version` < `required_version` 时，前端强制重新授权
- **Response 401**：`UNAUTHORIZED`

### 2.3 POST /api/user/privacy/consent

- **鉴权**：是（游客调用返回 `401 UNAUTHORIZED`）
- **请求体**：
  ```json
  {
    "version": "v2.0",
    "action": "agree"
  }
  ```
  - `action` 取值：`agree` / `revoke`
- **Response 200**：
  ```json
  {
    "status": "agreed",
    "version": "v2.0"
  }
  ```
- **错误码**：
  - `401 UNAUTHORIZED`：未登录
  - `400 VERSION_MISMATCH`：version 不是当前生效版本
  - `400 INVALID_ACTION`：action 非法
  - `409 ALREADY_REVOKED`：未同意用户调用 revoke
  - `409 ALREADY_AGREED`：重复同意当前版本（幂等返回 200，不报错也可接受）

> **游客态拦截**：所有会改变授权状态的接口（`POST /api/user/privacy/consent`、`GET /api/user/privacy/status`）必须校验登录态；未登录统一返回 `401`。

---

## 3. 状态机

```
未同意 ──[agree]──→ 已同意 ──[revoke]──→ 已撤回
  │                    │
  └────[版本更新]──────┘
```

| 转换 | 触发条件 | 字段变更 |
|------|---------|---------|
| 未同意 → 已同意 | 用户点击同意 | `status='agreed'`, `agreed_at=NOW()` |
| 已同意 → 已撤回 | 用户点击撤回 | `status='revoked'`, `revoked_at=NOW()` |
| 已同意 → 未同意 | 协议版本更新 | 业务层通过比较 `user.version` 与 `required_version` 强制重新授权，不直接修改记录 |

---

## 4. 缓存策略

| 缓存 | 键 | TTL | 说明 | 失效策略 |
|------|----|-----|------|---------|
| 当前协议 | `privacy_policy:current` | 1 小时 | 当前生效协议内容 | 管理员发布新版本时主动失效 |
| 用户授权状态 | `user:privacy:{user_id}` | 30 分钟 | 用户最近一次同意/撤回状态 | 同意/撤回时主动失效 |

---

## 5. 性能与安全

### 5.1 性能

- 协议查询 P99 < 100ms
- 同意/撤回接口 P99 < 200ms
- 授权状态查询 P99 < 100ms
- 审计日志写入 < 50ms

### 5.2 安全

- 协议内容不可篡改（仅管理员可更新版本）
- 同意/撤回记录不可删除，仅可追加 / 更新状态
- 撤回后限制非必要数据收集
- 敏感操作记录审计日志（`action='privacy_agree'` / `'privacy_revoke'`）
- 游客态禁止提交授权变更
- 合规留存：同意记录至少 3 年

---

## 6. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-004 | 依赖 | 微信授权登录后才有注册用户身份 |
| US-005 | 相邻 | 资料补充与隐私协议授权在注册流程中衔接 |
| US-008 | 相邻 | 隐私设置同处账号安全模块 |
| US-017 / US-020 | 被依赖 | 购买套餐等需授权功能需检查隐私状态 |
