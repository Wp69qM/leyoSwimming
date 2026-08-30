# US-042 管理员管理用户账号技术设计

---

## 1. 上下文

本 US 实现管理后台的用户账号管理功能，包括用户列表查询、查看用户详情、编辑用户资料、手动新建用户、封禁/解封账号。所有敏感操作需记录审计日志。

---

## 2. 目标 / 非目标

**目标：**
- 管理员可按身份、注册时间筛选用户
- 管理员可查看用户详情
- 管理员可编辑用户资料
- 管理员可手动新建用户
- 管理员可封禁/解封用户账号

**非目标：**
- 不实现用户自助修改资料（由 US-005 实现）
- 不实现账号注销（由 US-007 实现）
- 不实现管理员修改用户手机号（手机号为用户侧登录凭证，MVP 中不支持修改）
- 不实现用户角色权限配置（后台管理员角色权限由单独功能管理，不在本 US 范围内）
- 不实现复杂的组织架构权限

---

## 3. 数据模型

### 3.1 读取/修改表

> 本 US 仅操作 `user` 表（C 端用户/学员）。管理员账号存储在 `admin_user` 表，由 US-057「管理员管理管理员账号」负责维护，本 US 不读取也不修改 `admin_user`。

#### `user`

| 字段 | 类型 | 索引 | 备注 |
|------|------|------|------|
| user_id | BIGINT PK | | |
| phone | VARCHAR(20) | UK | |
| identity | TINYINT | IDX | 0=游客 1=注册用户 2=学员 |
| status | TINYINT | IDX | 0=正常 1=注销 2=封禁 |
| avatar_url | VARCHAR(512) | | 头像 URL |
| name | VARCHAR(64) | | 用户姓名/昵称 |
| gender | TINYINT | | 1=男 2=女；未完善资料时为 NULL |
| age | INT | | 年龄 |
| has_swim_basis | BOOLEAN | | 是否有游泳基础 |
| swim_strokes | VARCHAR(128) | | 会什么泳姿，逗号分隔：蛙泳/自由泳/仰泳/蝶泳 |
| swim_years | INT | | 游泳年限 |
| personal_desc | VARCHAR(512) | | 个人描述 |
| guardian_name | VARCHAR(64) | | 监护人姓名，age < 18 时必填 |
| guardian_phone | VARCHAR(20) | | 监护人手机号，age < 18 时必填 |
| profile_completed | BOOLEAN | IDX | 资料是否已完善 |
| source | VARCHAR(32) | IDX | 用户来源，如 `WECHAT`、`PHONE`、`ADMIN_CREATED` |
| created_at | DATETIME | IDX | |
| updated_at | DATETIME | | |
| version | INT | | 乐观锁 |

### 3.2 读取表

- `audit_log`：写入操作日志

---

## 4. API 设计

### 4.1 `POST /api/admin/user/list`

- **鉴权**：管理员 JWT + `USER:READ`
- **请求体**：`{ "identity": 1, "status": 0, "profileCompleted": true, "startDate": "2026-01-01", "endDate": "2026-12-31", "keyword": "", "page": 1, "pageSize": 20 }`
- **响应 200**：用户列表分页，返回字段：userId, avatarUrl, name, phone, gender, age, identity, profileCompleted, status, createdAt
- **错误码**：`ADMIN_PERMISSION_DENIED`（403）

### 4.2 `POST /api/admin/user/detail`

- **鉴权**：管理员 JWT + `USER:READ`
- **请求体**：`{ "userId": 1001 }`
- **响应 200**：用户详情含完整档案
- **返回档案字段**：avatarUrl, name, phone, gender, age, identity, status, profileCompleted, source, hasSwimBasis, swimStrokes, swimYears, personalDesc, guardianName, guardianPhone, createdAt, updatedAt
- **错误码**：`USER_NOT_FOUND`（404）、`ADMIN_PERMISSION_DENIED`（403）

### 4.3 `POST /api/admin/user/add`

- **鉴权**：管理员 JWT + `USER:WRITE`
- **功能**：管理员手动新建用户
- **请求体**：`{ "avatarUrl": "...", "phone": "13800138000", "name": "...", "gender": 1, "age": 25, "hasSwimBasis": true, "swimStrokes": "蛙泳,自由泳", "swimYears": 5, "personalDesc": "...", "guardianName": "...", "guardianPhone": "..." }`
- **业务规则**：
  - `phone` 必填，中国大陆手机号格式，提交时校验唯一性
  - `age < 18` 时，`guardian_name` 和 `guardian_phone` 必填
  - `has_swim_basis = false` 时，`swim_strokes` 和 `swim_years` 可空
  - 设置 `identity=1`（注册用户）、`status=0`（正常）、`profile_completed=true`、`source='ADMIN_CREATED'`
  - 创建成功后记录 `audit_log` action='ADMIN_CREATE_USER'
- **响应 201**：创建后的用户详情
- **错误码**：`PHONE_ALREADY_EXISTS`（409）、`INVALID_GUARDIAN_INFO`（400）、`ADMIN_PERMISSION_DENIED`（403）

### 4.4 `POST /api/admin/user/update`

- **鉴权**：管理员 JWT + `USER:WRITE`
- **功能**：管理员编辑用户资料（头像、姓名、性别、年龄、游泳基础、泳姿、游泳年限、个人描述、监护人信息等）
- **请求体**：`{ "userId": 1001, "profile": { "avatarUrl": "...", "name": "...", "gender": 1, "age": 25, "hasSwimBasis": true, "swimStrokes": "蛙泳,自由泳", "swimYears": 5, "personalDesc": "...", "guardianName": "...", "guardianPhone": "..." }, "version": 1 }`
- **业务规则**：
  - `age < 18` 时，`guardian_name` 和 `guardian_phone` 必填
  - `has_swim_basis = false` 时，`swim_strokes` 和 `swim_years` 可空
  - 手机号不可通过本接口修改
  - 修改成功后记录 `audit_log` action='ADMIN_UPDATE_PROFILE'
- **响应 200**：更新后的用户详情
- **错误码**：`USER_NOT_FOUND`（404）、`USER_CONCURRENTLY_UPDATED`（409）、`INVALID_GUARDIAN_INFO`（400）、`ADMIN_PERMISSION_DENIED`（403）

### 4.5 `POST /api/admin/user/ban`

- **鉴权**：管理员 JWT + `USER:BAN`
- **请求体**：`{ "userId": 1001, "reason": "涉嫌违规" }`
- **响应 200**：更新后的用户信息
- **业务规则**：仅允许对 `status=0` 的用户执行；更新 `status=2` 并写入 `audit_log` action='ADMIN_BAN_USER'
- **错误码**：`USER_NOT_FOUND`（404）、`ADMIN_PERMISSION_DENIED`（403）

### 4.6 `POST /api/admin/user/unban`

- **鉴权**：管理员 JWT + `USER:BAN`
- **请求体**：`{ "userId": 1001, "reason": "申诉通过" }`
- **响应 200**：更新后的用户信息
- **业务规则**：仅允许对 `status=2` 的用户执行；更新 `status=0` 并写入 `audit_log` action='ADMIN_UNBAN_USER'
- **错误码**：`USER_NOT_FOUND`（404）、`ADMIN_PERMISSION_DENIED`（403）

---

## 5. 状态机

本 US 不涉及业务状态机转换。

---

## 6. 缓存策略

- 用户列表缓存 30 秒
- 用户详情缓存 1 分钟，修改后失效

---

## 7. 性能指标

- `POST /api/admin/user/list` P99 < 200ms
- `POST /api/admin/user/detail` P99 < 150ms
- 修改类接口 P99 < 200ms

---

## 8. 安全 / RBAC 权限映射

### 8.1 权限码定义

| 权限码 | 说明 |
|--------|------|
| `USER:READ` | 查看用户列表与详情 |
| `USER:WRITE` | 编辑用户资料、手动新建用户 |
| `USER:BAN` | 封禁/解封用户账号 |

### 8.2 角色权限矩阵

| 操作 | `super_admin` | `admin` | 说明 |
|------|---------------|---------|------|
| 查看用户列表/详情 | ✅ | ✅ | 均持有 `USER:READ` |
| 编辑用户资料 | ✅ | ✅ | 均持有 `USER:WRITE`；仅针对 C 端用户/学员 |
| 手动新建用户 | ✅ | ✅ | 均持有 `USER:WRITE` |
| 封禁/解封账号 | ✅ | ✅ | 均持有 `USER:BAN`；仅针对 C 端用户/学员 |
| 删除账号 | ❌ | ❌ | 管理员无删除权限；账号注销由 US-007 用户自助完成 |

### 8.3 其他安全约束

- 接口按上表校验 JWT 与细粒度权限码
- 本 US 仅管理 `user` 表中的 C 端用户/学员账号，不涉及 `admin_user` 表的写操作
- 敏感操作写入 audit_log
- 新建用户时校验手机号唯一性

---

## 9. 跨 US 依赖

- 依赖 US-004 / US-006 产生 user 记录
- 依赖 US-005 产生用户档案字段（头像、姓名、年龄、性别、游泳档案、监护人信息等），本 US 在后台展示、编辑与新建这些字段
- 依赖 US-053 提供管理员登录认证（使用 `admin_user` 表）
- 与 US-057「管理员管理管理员账号」相互独立：US-057 操作 `admin_user` 表，本 US 操作 `user` 表

---

## 10. 接口测试映射

| 场景 | 测试方法 | 层级 |
|------|---------|------|
| 手动新建用户 | `test_admin_create_user_success` | 集成 |
| 新建用户手机号已存在 | `test_admin_create_user_phone_exists` | 集成 |
| 新建未成年人缺少监护人 | `test_admin_create_minor_missing_guardian` | 集成 |
| 编辑用户资料 | `test_admin_update_profile_success` | 集成 |
| 编辑未成年人缺少监护人 | `test_admin_update_minor_missing_guardian` | 集成 |
| 无权限 | `test_admin_no_permission_denied` | 集成 |
| 用户不存在 | `test_admin_user_not_found` | 集成 |
| 并发编辑资料 | `test_admin_update_profile_concurrent` | 集成 |
| 封禁账号 | `test_admin_ban_user_success` | 集成 |
| 解封账号 | `test_admin_unban_user_success` | 集成 |
