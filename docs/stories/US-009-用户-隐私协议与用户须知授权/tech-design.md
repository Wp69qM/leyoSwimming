# US-009 用户/教练隐私协议与用户须知授权 — 技术设计

> **状态**：已评审　|　**最后更新**：2026-07-31

---

## 1. 数据模型

### 1.1 涉及的表

| 表名 | 操作 | 说明 |
|------|------|------|
| `terms_policy` | 新增 | 《用户须知》版本与内容，仅管理员可写入 |
| `user_terms_consent` | 新增 | 记录用户/教练对每个版本《用户须知》的同意 |
| `privacy_policy` | 新增 | 《隐私协议》版本与内容，仅管理员可写入 |
| `user_privacy_consent` | 新增 | 记录用户/教练对每个版本《隐私协议》的同意 |
| `audit_log` | 新增 | 审计日志（复用 PRD §9.2.14） |

### 1.2 字段定义

**`terms_policy` 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `version` | VARCHAR(32) | PK | 版本号，如 `v2.0` |
| `content` | TEXT | NOT NULL | 协议正文（Markdown / 富文本） |
| `effective_at` | DATETIME | NOT NULL | 生效时间 |
| `is_current` | TINYINT | 默认 0 | `1`=当前生效，同一时刻仅 1 条可为 1 |
| `created_at` | DATETIME | — | 创建时间 |

**`user_terms_consent` 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `consent_id` | BIGINT | PK | 自增 ID |
| `user_id` | BIGINT | FK, UK(user_id, version) | 用户/教练 ID |
| `version` | VARCHAR(32) | 非空 | 协议版本 |
| `status` | ENUM('agreed') | 非空 | 同意（本 US 不支持撤回） |
| `agreed_at` | DATETIME | 非空 | 同意时间 |
| `created_at` | DATETIME | — | 创建时间 |
| `updated_at` | DATETIME | — | 更新时间 |

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
| `user_id` | BIGINT | FK, UK(user_id, version) | 用户/教练 ID |
| `version` | VARCHAR(32) | 非空 | 协议版本 |
| `status` | ENUM('agreed') | 非空 | 同意（本 US 不支持撤回） |
| `agreed_at` | DATETIME | 非空 | 同意时间 |
| `created_at` | DATETIME | — | 创建时间 |
| `updated_at` | DATETIME | — | 更新时间 |

### 1.3 索引

```sql
-- 同一用户对同一版本仅保留一条记录，保证幂等
CREATE UNIQUE INDEX idx_user_terms_user_version
  ON user_terms_consent(user_id, version);

CREATE UNIQUE INDEX idx_user_privacy_user_version
  ON user_privacy_consent(user_id, version);

-- 当前生效协议查询
CREATE INDEX idx_terms_policy_current
  ON terms_policy(is_current, effective_at);

CREATE INDEX idx_privacy_policy_current
  ON privacy_policy(is_current, effective_at);
```

---

## 2. API 设计

| 接口 | 方法 | 鉴权 | 说明 |
|------|------|------|------|
| `/api/common/terms/current` | POST | 否（游客可见） | 获取当前生效《用户须知》 |
| `/api/common/privacy/current` | POST | 否（游客可见） | 获取当前生效《隐私协议》 |
| `/api/user/terms/status` | POST | 是 | 查询当前用户《用户须知》同意版本 |
| `/api/user/privacy/status` | POST | 是 | 查询当前用户《隐私协议》同意版本 |
| `/api/user/terms/consent` | POST | 是 | 记录用户/教练同意《用户须知》 |
| `/api/user/privacy/consent` | POST | 是 | 记录用户/教练同意《隐私协议》 |

> **版本对齐说明**：本 US 的《用户须知》与《隐私协议》为两套独立内容，分别由 `terms_policy` / `privacy_policy` 管理，同意记录分别写入 `user_terms_consent` / `user_privacy_consent`。登录成功后由 US-004 / US-006 / US-051 / US-054 调用 consent 接口记录同意版本。

### 2.1 POST /api/common/terms/current

- **鉴权**：否（游客可见）
- **Response 200**：
  ```json
  {
    "version": "v2.0",
    "content": "<协议正文>",
    "effective_at": "2026-07-31T00:00:00+08:00"
  }
  ```
- **Response 404**：`NO_CURRENT_TERMS_POLICY`

### 2.2 POST /api/common/privacy/current

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

### 2.3 POST /api/user/terms/status

- **鉴权**：是（游客调用返回 `401 UNAUTHORIZED`）
- **Response 200**：
  ```json
  {
    "status": "agreed",
    "version": "v2.0",
    "required_version": "v2.0"
  }
  ```
  - `status` 取值：`agreed` / `none`
  - 当 `version` < `required_version` 时，前端强制重新授权
- **Response 401**：`UNAUTHORIZED`

### 2.4 POST /api/user/privacy/status

- **鉴权**：是（游客调用返回 `401 UNAUTHORIZED`）
- **Response 200**：
  ```json
  {
    "status": "agreed",
    "version": "v2.0",
    "required_version": "v2.0"
  }
  ```
  - `status` 取值：`agreed` / `none`
  - 当 `version` < `required_version` 时，前端强制重新授权
- **Response 401**：`UNAUTHORIZED`

### 2.5 POST /api/user/terms/consent

- **鉴权**：是（游客调用返回 `401 UNAUTHORIZED`）
- **请求体**：
  ```json
  {
    "version": "v2.0"
  }
  ```
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
  - `409 ALREADY_AGREED`：重复同意当前版本（幂等返回 200，不报错也可接受）

> **游客态拦截**：`POST /api/user/terms/consent` 必须校验登录态；未登录统一返回 `401`。

### 2.6 POST /api/user/privacy/consent

- **鉴权**：是（游客调用返回 `401 UNAUTHORIZED`）
- **请求体**：
  ```json
  {
    "version": "v2.0"
  }
  ```
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
  - `409 ALREADY_AGREED`：重复同意当前版本（幂等返回 200，不报错也可接受）

> **游客态拦截**：`POST /api/user/privacy/consent` 必须校验登录态；未登录统一返回 `401`。

---

## 3. 状态机

```
未同意 ──[agree]──→ 已同意
  │                    │
  └────[版本更新]──────┘
```

| 转换 | 触发条件 | 字段变更 |
|------|---------|---------|
| 未同意 → 已同意 | 用户点击同意 | `status='agreed'`, `agreed_at=NOW()` |
| 已同意 → 未同意 | 协议版本更新 | 业务层通过比较 `user.version` 与 `required_version` 强制重新授权，不直接修改记录 |

> 本 US 不支持撤回授权，登录后设置页仅可查看协议内容。`status` 仅存在 `agreed` 状态。

---

## 4. 缓存策略

| 缓存 | 键 | TTL | 说明 | 失效策略 |
|------|----|-----|------|---------|
| 当前协议 | `privacy_policy:current` / `terms_policy:current` | 1 小时 | 当前生效协议内容 | 管理员发布新版本时主动失效 |
| 用户授权状态 | `user:privacy:{user_id}` / `user:terms:{user_id}` | 30 分钟 | 用户最近一次同意状态 | 同意时主动失效 |

---

## 5. 性能与安全

### 5.1 性能

- 协议查询 P99 < 100ms
- 同意/撤回接口 P99 < 200ms
- 授权状态查询 P99 < 100ms
- 审计日志写入 < 50ms

### 5.2 安全

- 协议内容不可篡改（仅管理员可更新版本）
- 同意记录不可删除，仅可追加
- 敏感操作记录审计日志（`action='privacy_agree'` / `'terms_agree'`）
- 游客态禁止提交授权变更
- 合规留存：同意记录至少 3 年

---

## 6. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-004 | 依赖 | 微信授权登录后调用 consent 接口记录同意版本 |
| US-006 | 依赖 | 手机号验证码登录后调用 consent 接口记录同意版本 |
| US-047 | 依赖 | 管理员配置当前生效的《用户须知》与《隐私协议》版本 |
| US-051 | 依赖 | 教练微信授权登录后调用 consent 接口记录同意版本 |
| US-054 | 依赖 | 教练手机号验证码登录后调用 consent 接口记录同意版本 |
| US-052 | 相邻 | 用户退出登录后重新登录需再次勾选协议 |
