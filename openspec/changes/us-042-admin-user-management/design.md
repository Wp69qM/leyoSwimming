> **OpenSpec Design | 映射自 `docs/stories/US-042-管理员-管理用户账号/user-story.md` §7**

## 数据模型

| 表名 | 字段 | 类型 | 说明 |
|------|------|------|------|
| user | id | BIGINT | 用户主键 |
| user | phone | VARCHAR(64) | AES-256 加密 |
| user | email | VARCHAR(128) | 邮箱 |
| user | password_hash | VARCHAR(255) | bcrypt |
| user | force_change_password | BOOLEAN | 重置密码后强制修改 |
| user | status | TINYINT | 0=正常，1=注销，2=封禁 |
| user | identity_status | VARCHAR(32) | 游客/注册用户/学员 |
| user | avatar_url | VARCHAR(512) | 头像 |
| user | name | VARCHAR(64) | 姓名 |
| user | age | TINYINT | 年龄 |
| user | gender | ENUM | male/female/secret |
| user | guardian_name | VARCHAR(64) | 监护人姓名（age < 18） |
| user | guardian_phone | VARCHAR(64) | 监护人手机号（age < 18），AES-256 加密 |
| user_role | user_id / role_id | BIGINT | 用户角色关联 |
| role | id / name / permissions | — | RBAC 角色 |
| audit_log | action / target_type / target_id / operator_id / remark | — | 审计日志 |

## API

### GET /api/admin/v1/users

- 鉴权：access_token + `USER:READ`
- Query: `{ page, size, identity_status, status, start_date, end_date, keyword }`
- Response 200: `{ list: [...], total }`

### GET /api/admin/v1/users/{id}

- 鉴权：access_token + `USER:READ`
- Response 200: 用户详情（含监护人信息脱敏展示）

### POST /api/admin/v1/users/{id}/reset-password

- 鉴权：access_token + `USER:PASSWORD_RESET`
- Response 200: `{ temp_password }`

### PUT /api/admin/v1/users/{id}/phone

- 鉴权：access_token + `USER:WRITE`
- Request: `{ phone, verification_code?, force?, reason? }`
- Response 200 / 409 `PHONE_ALREADY_EXISTS` / 403 `ADMIN_PERMISSION_DENIED`

### PUT /api/admin/v1/users/{id}/email

- 鉴权：access_token + `USER:WRITE`
- Request: `{ email }`
- Response 200 / 409 `EMAIL_ALREADY_EXISTS`

### PUT /api/admin/v1/users/{id}/roles

- 鉴权：access_token + `USER:ROLE_ASSIGN`
- Request: `{ role_ids }`
- Response 200 / 403 `ADMIN_PERMISSION_DENIED`

### POST /api/admin/v1/users/{id}/ban

- 鉴权：access_token + `USER:BAN`
- Request: `{ reason }`
- Response 200

### POST /api/admin/v1/users/{id}/unban

- 鉴权：access_token + `USER:BAN`
- Request: `{ reason }`
- Response 200

## RBAC

| 角色 | 可读用户 | 可写用户 | 可重置密码 | 可分配角色 | 可封禁 |
|------|:-------:|:-------:|:---------:|:---------:|:------:|
| super_admin | ✅ | ✅ | ✅ | ✅ | ✅ |
| admin | ✅ | ✅（除 super_admin 外） | ✅ | ✅（除 super_admin 角色外） | ✅ |

## 安全

- 所有写操作记录 audit_log
- 修改手机号强制变更需填写原因并记录 remark
- 管理员不可修改自己的关键字段
- 用户手机号/监护人手机号加密存储，前端脱敏展示
