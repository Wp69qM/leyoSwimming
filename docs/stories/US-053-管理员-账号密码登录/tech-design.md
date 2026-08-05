# US-053 技术设计文档

> **US 关联**：[user-story.md](./user-story.md)（US-053 管理员账号密码登录）
> **文档角色**：技术设计（plan.md / design.md 角色）
> **作者**：开发　|　**最后更新**：2026-08-05
> **状态**：✅ 已填写

---

## 0. 文档定位

本文档由**开发**填写，作为 US-053 的**技术设计层**。

- **用户故事** = 业务层 WHAT（管理员看到什么）
- **本文档** = 设计层 HOW（数据模型 / API / 状态机 / 缓存）
- **test-plan.md** = 执行层 DO（RED→GREEN→COMMIT 的逐任务）

---

## 1. 数据模型影响

### 1.1 新增/修改的表

> 仅列出与 US-053 直接相关的字段。

| 表名 | 操作 | 字段 | 说明 |
|------|------|------|------|
| `admin_user` | 读取 | `id`, `username`, `password_hash`, `name`, `role`, `status`, `created_at`, `updated_at` | 管理员账号表，登录时校验用户名、密码、状态 |
| `admin_session` | 新增 | `id`, `admin_user_id`, `token_hash`, `expires_at`, `revoked_at`, `created_at` | 管理员会话表（单一 token，有效期 24 小时） |
| `admin_login_log` | 新增 | `id`, `admin_user_id`, `username`, `ip`, `user_agent`, `status` (success/fail), `reason`, `created_at` | 管理员登录日志 |

### 1.2 索引

```sql
-- 按用户名查询管理员
CREATE UNIQUE INDEX idx_admin_user_username ON admin_user(username);

:-- 按 token_hash 查询当前会话
CREATE INDEX idx_admin_session_token ON admin_session(token_hash);

-- 按 admin_user_id 查询管理员所有会话
CREATE INDEX idx_admin_session_admin_user_id ON admin_session(admin_user_id);
```

---

## 2. API 设计

### 2.1 POST /api/v1/admin/auth/login

| 属性 | 值 |
|------|----|
| 路径 | `POST /api/v1/admin/auth/login` |
| 鉴权 | 否（登录前无需鉴权） |
| 幂等 | 否（每次调用生成新 session） |

**Request Body**

```json
{
  "username": "admin",
  "password": "correct_password"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | string | 是 | 管理员用户名 |
| `password` | string | 是 | 管理员密码 |

**Response 200**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "expires_in": 86400,
  "admin": {
    "id": 1,
    "username": "admin",
    "name": "系统管理员",
    "role": "admin"
  }
}
```

**Response 401 — 用户名或密码错误**

```json
{
  "error": "ADMIN_CREDENTIALS_INVALID",
  "message": "用户名或密码错误"
}
```

**Response 403 — 账号被禁用**

```json
{
  "error": "ADMIN_DISABLED",
  "message": "账号已被禁用，请联系超级管理员"
}
```

**业务规则**

- 后端根据 `username` 查询 `admin_user` 记录
- 使用 bcrypt/scrypt 校验 `password_hash`
- 校验 `status = 0`（启用），否则返回 403
- 校验通过后生成单一 `token`（24h）
- 将 token hash 写入 `admin_session`
- 记录登录成功日志到 `admin_login_log`
- 失败时记录失败日志（不泄露是用户名错还是密码错）

### 2.2 POST /api/v1/admin/auth/logout

| 属性 | 值 |
|------|----|
| 路径 | `POST /api/v1/admin/auth/logout` |
| 鉴权 | 是（需携带有效 token） |
| 幂等 | 是（同一 token 重复调用视为已退出） |

**Request Headers**

```http
Authorization: Bearer {token}
```

**Response 200**

```json
{
  "message": "退出登录成功"
}
```

**业务规则**

- 后端解析并校验 `token`
- 按 token hash 查找 `admin_session` 并标记失效
- 清除前端 localStorage 中的 token 和 userInfo
- 退出后跳转回管理员登录页

---

## 3. 状态机影响

### 3.1 管理员登录态状态机

```
未登录 ──(登录成功)──→ 已登录
已登录 ──(退出登录)──→ 未登录
```

| 转换 | 触发 US | 触发条件 | 字段变更 |
|------|---------|---------|---------|
| 未登录 → 已登录 | **US-053（本 US）** | 用户名密码校验通过且账号启用 | 生成 `admin_session` 记录 |
| 已登录 → 未登录 | **US-053（本 US）** | 管理员点击右上角退出登录 | `admin_session.revoked_at` 置为当前时间 |

---

## 4. 前端流程

### 4.1 登录时序图

```
管理员                     后端
  │                        │
  │ 1. 访问后台系统         │
  │ 2. 未登录，展示登录页   │
  │ 3. 输入用户名、密码     │
  │ 4. 点击登录             │
  ├─ POST /admin/auth/login →│
  │                        │
  │ 5. 校验用户名/密码      │
  │ 6. 校验账号状态         │
  │ 7. 生成单一 token（24h） │
   │<─ 200 + token ─────────┤
   │                        │
   │ 8. 存储 token + userInfo │
   │ 9. 跳转后台首页         │
```

### 4.2 本地存储

存储以下 key：

- `admin_token`
- `admin_user`（管理员基本信息，含 `name`、`role` 等，用于页面展示）

### 4.3 路由守卫

- Web 前端在路由进入前检查 `admin_token` 是否存在且未过期
- 若不存在或已过期，跳转登录页并提示"登录信息已过期，请重新登录"
- API 请求统一在 Header 中携带 `Authorization: Bearer {admin_token}`
- 请求拦截器捕获后端返回的 401，自动清除 localStorage 并跳转登录页，提示"登录信息已过期，请重新登录"

---

## 5. 安全 / 鉴权

- 密码使用 bcrypt/scrypt 哈希存储，禁止明文
- 登录接口限流：同一 IP/用户名 5 分钟内最多 5 次失败尝试
- token 使用 JWT，单一 token 有效期 24h
- 管理后台必须使用 HTTPS
- 前端将 token 存储在 localStorage（Web 后台管理场景下 XSS 风险可控，且便于页面展示 userInfo）
- 登录日志记录 IP、UA、成功/失败状态，用于安全审计

---

## 6. 跨 US 依赖

| US | 依赖方向 | 说明 |
|----|---------|------|
| US-041 ~ US-049 | 被本 US 依赖（反向：后续 US 依赖本 US）| 所有管理后台 US 均依赖本 US 提供的管理员认证能力 |
| US-054（待创建）| 相邻 | 管理员账号管理：创建/编辑/禁用管理员账号，与本 US 共用 `admin_user` 表 |

---

## 7. 异常与边界

| 场景 | 处理 |
|------|------|
| 用户名或密码错误 | 返回 401 ADMIN_CREDENTIALS_INVALID，记录失败日志 |
| 账号被禁用 | 返回 403 ADMIN_DISABLED，记录失败日志 |
| 连续多次登录失败 | 可临时锁定账号 15 分钟（MVP 可选，至少记录日志） |
| token 过期 | 后端返回 401，前端自动跳转登录页并提示"登录信息已过期，请重新登录" |
| 多浏览器登录 | 各浏览器生成独立 admin_session，互不影响 |

---

## 8. 实现顺序（与 test-plan.md 的映射）

| tech-design 章节 | 对应 test-plan Task |
|------------------|---------------------|
| §2.1 POST /admin/auth/login | Task 1（后端登录接口） |
| §2.2 POST /admin/auth/logout | Task 2（后端退出接口） |
| §4 前端流程 | Task 3（Web 登录页 + 路由守卫） |
| §4.3 路由守卫 | Task 4（全局导航栏退出菜单） |

---

## 9. 上下游引用

- **用户故事**：[./user-story.md](./user-story.md)
- **测试计划**：[./test-plan.md](./test-plan.md)
- **全局设计规范**：[../../figma/README.md](../../figma/README.md)
- **测试计划规范**：[../../spec/test-plan/README.md](../../spec/test-plan/README.md)
- **PRD §10.1 管理后台**：[../../prd/prd.md](../../prd/prd.md)

---

## 10. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-08-05 | Dev | 初版：管理员账号密码登录技术设计 |
