# US-057 技术设计文档

> **US 关联**：[user-story.md](./user-story.md)（US-057 管理员管理管理员账号）
> **文档角色**：技术设计（plan.md / design.md 角色）
> **作者**：开发　|　**最后更新**：2026-08-08
> **状态**：✅ 已填写

---

## 0. 文档定位

本文档由**开发**填写，作为 US-057 的**技术设计层**。

- **用户故事** = 业务层 WHAT（管理员看到什么）
- **本文档** = 设计层 HOW（数据模型 / API / 状态机 / 缓存）
- **test-plan.md** = 执行层 DO（RED→GREEN→COMMIT 的逐任务）

---

## 1. 数据模型影响

### 1.1 新增/修改的表

> 仅列出与 US-057 直接相关的字段。

| 表名 | 操作 | 字段 | 说明 |
|------|------|------|------|
| `admin_user` | 查询/新增/更新/删除 | `id`, `username`, `password_hash`, `name`, `role` (`super_admin`/`admin`), `status` (0=启用/1=禁用), `last_login_at`, `deleted_at`, `created_at`, `updated_at` | 与 US-053 共用；US-057 负责写操作；`last_login_at` 在每次登录成功时由 US-053 更新 |
| `admin_session` | 查询/更新/删除 | `id`, `admin_user_id`, `token_hash`, `expires_at`, `revoked_at`, `created_at` | 禁用/删除账号时失效相关会话 |
| `admin_audit_log` | 新增 | `id`, `admin_user_id`（操作人）, `target_admin_user_id`, `action` (`CREATE`/`UPDATE`/`DISABLE`/`ENABLE`/`DELETE`/`RESET_PASSWORD`), `before_snapshot`, `after_snapshot`, `reason`, `ip`, `created_at` | 记录所有管理员账号写操作 |

### 1.2 索引

```sql
-- 按用户名唯一查询/校验
CREATE UNIQUE INDEX idx_admin_user_username ON admin_user(username) WHERE deleted_at IS NULL;

-- 按角色+状态查询列表
CREATE INDEX idx_admin_user_role_status ON admin_user(role, status);

-- 按管理员查询所有会话
CREATE INDEX idx_admin_session_admin_user_id ON admin_session(admin_user_id);

-- 按目标账号查询操作日志
CREATE INDEX idx_admin_audit_log_target ON admin_audit_log(target_admin_user_id);
```

---

## 2. API 设计

> 遵循 [api-convention.md](../../tech/api-convention.md)：统一使用 `POST`，参数通过 JSON body 传递。

### 2.1 POST /api/admin/admin/list

| 属性 | 值 |
|------|----|
| 路径 | `POST /api/admin/admin/list` |
| 鉴权 | 是（需管理员 token；`admin` 与 `super_admin` 均可） |
| 幂等 | 是 |

**Request Body**

```json
{
  "keyword": "",
  "role": "",
  "status": null,
  "page": 1,
  "size": 20
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `keyword` | string | 否 | 按姓名/登录账号模糊搜索 |
| `role` | string | 否 | `super_admin` / `admin` |
| `status` | int | 否 | `0`=启用 / `1`=禁用 |
| `page` | int | 否 | 默认 1 |
| `size` | int | 否 | 默认 20，最大 100 |

**Response 200**

```json
{
  "success": true,
  "data": {
    "list": [
      {
        "id": 1,
        "username": "admin",
        "name": "系统管理员",
        "role": "super_admin",
        "status": 0,
        "last_login_at": "2026-08-08T10:00:00Z",
        "created_at": "2026-08-01T10:00:00Z",
        "allowed_actions": ["VIEW"]
      }
    ],
    "total": 1
  }
}
```

> `allowed_actions` 由后端根据当前登录管理员角色与目标账号状态计算，前端只渲染允许的入口。`admin` 角色对其他账号仅返回 `["VIEW"]`；`super_admin` 对自身可能返回 `["VIEW", "EDIT"]`，对他人返回 `["VIEW", "EDIT", "DISABLE", "DELETE", "RESET_PASSWORD"]`。

---

### 2.2 POST /api/admin/admin/detail

| 属性 | 值 |
|------|----|
| 路径 | `POST /api/admin/admin/detail` |
| 鉴权 | 是 |
| 幂等 | 是 |

**Request Body**

```json
{
  "id": 1
}
```

**Response 200**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "admin",
    "name": "系统管理员",
    "role": "super_admin",
    "status": 0,
    "last_login_at": "2026-08-08T10:00:00Z",
    "created_at": "2026-08-01T10:00:00Z",
    "updated_at": "2026-08-08T10:00:00Z"
  }
}
```

**业务规则**

- `admin` 角色只能查看自身资料；查看其他管理员返回 `ADMIN_PERMISSION_DENIED`
- 目标账号不存在返回 `ADMIN_NOT_FOUND`

---

### 2.3 POST /api/admin/admin/add

| 属性 | 值 |
|------|----|
| 路径 | `POST /api/admin/admin/add` |
| 鉴权 | 是（仅 `super_admin`） |
| 幂等 | 否 |

**Request Body**

```json
{
  "username": "new_admin",
  "name": "新管理员",
  "password": "Admin@1234",
  "role": "admin"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | string | 是 | 3-32 位，字母/数字/下划线，唯一 |
| `name` | string | 是 | 2-32 位 |
| `password` | string | 是 | 8-32 位，需包含字母+数字+特殊字符 |
| `role` | string | 是 | `super_admin` / `admin` |

**Response 200**

```json
{
  "success": true,
  "data": {
    "id": 2,
    "username": "new_admin",
    "name": "新管理员",
    "role": "admin",
    "status": 0,
    "created_at": "2026-08-08T10:00:00Z"
  }
}
```

**错误响应**

- `ADMIN_PERMISSION_DENIED`（403）：当前非 `super_admin`
- `ADMIN_USERNAME_ALREADY_EXISTS`（400）：登录账号已存在
- `VALIDATION_ERROR`（400）：字段格式不满足要求

**业务规则**

- 对 `password` 使用 bcrypt 生成 `password_hash`
- 新建账号 `status=0`
- 写入 `admin_audit_log`，`action='CREATE'`，`after_snapshot` 包含新建记录（`password_hash` 脱敏）

---

### 2.4 POST /api/admin/admin/update

| 属性 | 值 |
|------|----|
| 路径 | `POST /api/admin/admin/update` |
| 鉴权 | 是（仅 `super_admin`） |
| 幂等 | 是 |

**Request Body**

```json
{
  "id": 2,
  "name": "管理员改名",
  "role": "super_admin"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | long | 是 | 目标管理员 ID |
| `name` | string | 否 | 2-32 位 |
| `role` | string | 否 | `super_admin` / `admin` |

**Response 200**

```json
{
  "success": true,
  "data": {
    "id": 2,
    "username": "new_admin",
    "name": "管理员改名",
    "role": "super_admin",
    "status": 0,
    "updated_at": "2026-08-08T10:05:00Z"
  }
}
```

**业务规则**

- 不允许修改 `username`
- 目标不存在返回 `ADMIN_NOT_FOUND`
- 写入 `admin_audit_log`，`action='UPDATE'`，记录变更前后快照
- 若角色变更导致列表中只剩一个 `super_admin`，仍允许（禁用/删除时才做最后 super_admin 保护）

---

### 2.5 POST /api/admin/admin/toggle-status

| 属性 | 值 |
|------|----|
| 路径 | `POST /api/admin/admin/toggle-status` |
| 鉴权 | 是（仅 `super_admin`） |
| 幂等 | 否（首次禁用，再次启用） |

**Request Body**

```json
{
  "id": 2,
  "status": 1,
  "reason": "离职交接"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | long | 是 | 目标管理员 ID |
| `status` | int | 是 | `0`=启用 / `1`=禁用 |
| `reason` | string | 是 | 操作原因，2-200 位 |

**Response 200**

```json
{
  "success": true,
  "data": {
    "id": 2,
    "status": 1,
    "updated_at": "2026-08-08T10:10:00Z"
  }
}
```

**业务规则**

- 禁用前校验：目标不是当前登录账号自身；禁用后系统中至少还有一个 status=0 的 `super_admin`
- 若目标为当前登录账号 → 返回 `ADMIN_CANNOT_DISABLE_SELF`
- 若禁用后无启用状态的 `super_admin` → 返回 `ADMIN_LAST_SUPER_ADMIN_PROTECTED`
- 禁用后需将该管理员的 `admin_session` 全部失效（`revoked_at=now()` 或物理删除）
- 写入 `admin_audit_log`，`action='DISABLE'` 或 `'ENABLE'`

---

### 2.6 POST /api/admin/admin/delete

| 属性 | 值 |
|------|----|
| 路径 | `POST /api/admin/admin/delete` |
| 鉴权 | 是（仅 `super_admin`） |
| 幂等 | 否 |

**Request Body**

```json
{
  "id": 2,
  "reason": "账号清理"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | long | 是 | 目标管理员 ID |
| `reason` | string | 是 | 删除原因，2-200 位 |

**Response 200**

```json
{
  "success": true,
  "data": null
}
```

**业务规则**

- 删除前校验：目标不是当前登录账号自身；删除后系统中至少还有一个 status=0 的 `super_admin`
- 若目标为当前登录账号 → 返回 `ADMIN_CANNOT_DELETE_SELF`
- 若删除后无启用状态的 `super_admin` → 返回 `ADMIN_LAST_SUPER_ADMIN_PROTECTED`
- 删除方式推荐**逻辑删除**（`deleted_at=now()`），保留 `admin_audit_log` 中 `target_admin_user_id` 关联
- 删除前将该管理员的 `admin_session` 全部失效
- 写入 `admin_audit_log`，`action='DELETE'`，`before_snapshot` 保留完整快照

---

### 2.7 POST /api/admin/admin/reset-password

| 属性 | 值 |
|------|----|
| 路径 | `POST /api/admin/admin/reset-password` |
| 鉴权 | 是（仅 `super_admin`） |
| 幂等 | 否（每次生成新随机密码） |

**Request Body**

```json
{
  "id": 2
}
```

**Response 200**

```json
{
  "success": true,
  "data": {
    "temp_password": "xK9#mP2$vL7@"
  }
}
```

> `temp_password` 仅在此响应中明文返回一次；后续无法再次查询明文。

**业务规则**

- 生成 12 位随机强密码（大小写字母+数字+特殊字符）
- 使用 bcrypt 更新 `admin_user.password_hash`
- 不强制登出已登录会话（由业务决定；建议重置后提示是否登出）
- 写入 `admin_audit_log`，`action='RESET_PASSWORD'`（不记录明文密码）

---

## 3. 状态机影响

| 状态维度 | 状态值 | 触发 US | 转换说明 |
|----------|--------|---------|----------|
| 管理员账号启用状态 | `status=0`（启用） | US-057 | 新建账号默认启用；由禁用恢复 |
| 管理员账号启用状态 | `status=1`（禁用） | US-057 | 由启用变为禁用，禁用后无法登录 |
| 管理员账号存在性 | 存在 | US-057（add） | 创建新账号 |
| 管理员账号存在性 | 已删除 | US-057（delete） | 逻辑删除，保留审计关联 |

---

## 4. 跨 US 依赖

| US | 依赖方向 | 说明 |
|----|---------|------|
| US-053 | 被本 US 依赖 | 共用 `admin_user`、`admin_session` 表；US-053 负责登录态创建，US-057 负责账号生命周期与会话失效 |
| US-041 ~ US-049 | 反向依赖 | 这些管理后台 US 的操作者账号由本 US 维护 |

---

## 5. 异常与边界

| 场景 | 处理 |
|------|------|
| 无权限 | 403 `ADMIN_PERMISSION_DENIED` |
| 目标管理员不存在 | 404 `ADMIN_NOT_FOUND` |
| 登录账号重复 | 400 `ADMIN_USERNAME_ALREADY_EXISTS` |
| 字段校验失败 | 400 `VALIDATION_ERROR` |
| 删除/禁用自身 | 400 `ADMIN_CANNOT_DELETE_SELF` / `ADMIN_CANNOT_DISABLE_SELF` |
| 最后 super_admin 保护 | 400 `ADMIN_LAST_SUPER_ADMIN_PROTECTED` |
| 重置密码时目标账号已删除 | 404 `ADMIN_NOT_FOUND` |

---

## 6. 安全与性能

### 6.1 安全

- 所有接口必须鉴权，且仅 `super_admin` 可执行写操作
- `password_hash` 使用 bcrypt（cost factor ≥ 10）
- 响应中不返回 `password_hash`
- `admin_audit_log` 记录所有写操作及变更前后快照，满足审计要求
- 删除/禁用账号后立即失效其 session，防止「僵尸会话」

### 6.2 性能

- 管理员账号数量通常 < 100，列表查询无需复杂优化
- 按 `role`、`status` 建立组合索引即可满足筛选需求
- 审计日志表数据量随时间增长，后续可按时间分区

---

## 7. 变更日志

| 版本 | 日期 | 作者 | 变更内容 |
|------|------|------|----------|
| v1.0 | 2026-08-08 | Dev | 初版：定义 `admin_user` 生命周期 7 个接口、数据模型、状态机、异常边界与审计要求 |
