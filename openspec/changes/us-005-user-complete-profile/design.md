> **OpenSpec Design | 映射自 `docs/stories/US-005-用户-完善个人资料/tech-design.md`**

## 数据模型

### user 表字段更新

| 字段 | 类型 | 说明 |
|------|------|------|
| avatar_url | VARCHAR(512) | 头像 URL，默认带入微信头像 |
| name | VARCHAR(64) | 姓名，必填 |
| phone | VARCHAR(64) | 手机号，AES-256 加密，唯一索引 |
| age | TINYINT | 年龄 3-99 |
| gender | ENUM('male','female','secret') | 性别 |
| has_swim_basis | BOOLEAN | 有无游泳基础 |
| swim_strokes | JSON | 会什么泳姿 |
| swim_years | INT | 游泳年限 ≥0 |
| personal_desc | VARCHAR(500) | 个人描述 |
| profile_completed | BOOLEAN | 资料是否已完善 |

## API

### PUT /api/user/profile
完善/更新个人资料。需要登录鉴权，幂等。

### GET /api/user/phone/exists
校验手机号是否被其他账号绑定。

### POST /api/upload/avatar
上传头像，限制格式 jpg/png/webp，大小 ≤2MB。

## 状态机

```
profile_completed=false ──(US-005 资料完善完成)──→ profile_completed=true
```

`identity_status` 保持「注册用户」不变。

## 缓存

- `phone:exists:{phone_hash}` TTL 300s
- `user:{user_id}` TTL 1800s，资料更新时失效
- `idempotency:{key}` TTL 300s

## 安全

- 登录鉴权
- 手机号加密存储，前端脱敏展示
- 头像格式/大小限制
- 姓名敏感词过滤
- 接口限流
- 必须校验隐私协议已同意
- 幂等键防重

## 跨 US 依赖

- 依赖 US-004（微信授权登录获取手机号/头像）
- 依赖 US-006（手机号验证码登录已有手机号）
- 依赖 US-009（隐私协议授权）
- 被 US-008（账号安全）依赖
- 被 US-037（教练管理学员信息）依赖
