> **状态**：待开发填写
> **对应用户故事**：[./user-story.md](./user-story.md)
> **最后更新**：2026-08-04

---

## 1. 概述

本文档承接 [user-story.md](./user-story.md)，聚焦用户完善个人资料的技术实现：
- 首次登录后如何强制进入资料完善页
- 「我的 → 编辑资料」如何复用同一页面与 API
- 头像上传、字段校验、手机号唯一性、敏感词过滤如何实现
- `profile_completed: false → true` 的状态转换细节

> **核心设计**：
> - 微信授权登录（US-004）或手机号验证码登录（US-006）已获取手机号，本页手机号字段为「可修改的已绑定手机号」。
> - 不设置密码；微信登录走微信授权，手机号登录走验证码。
> - 教练侧查看学员档案并添加备注的能力由 US-037 扩展，本 US 只负责用户资料维护。

---

## 2. 数据模型

### 2.1 新增/修改的表

| 表 | 操作 | 关键字段 | 说明 |
|----|------|---------|------|
| `user` | UPDATE | `avatar_url` | 头像 URL，首次带入微信头像，可修改 |
| `user` | UPDATE | `name` | 用户姓名，必填，展示用 |
| `user` | UPDATE | `phone` | 手机号，AES-256 加密，登录时已获取，可修改 |
| `user` | UPDATE | `age` | 年龄，整数 3-99 |
| `user` | UPDATE | `gender` | 性别，枚举：male/female/secret |
| `user` | UPDATE | `has_swim_basis` | 有无游泳基础，布尔 |
| `user` | UPDATE | `swim_strokes` | 会什么泳姿，JSON 数组：["breaststroke", "freestyle", ...] |
| `user` | UPDATE | `swim_years` | 游泳年限，整数 ≥0 |
| `user` | UPDATE | `personal_desc` | 个人描述，字符串 |
| `user` | UPDATE | `profile_completed` | true 表示资料已完善 |
| `user` | UPDATE | `identity_status` | 保持 '注册用户'，本 US 不修改 |

### 2.2 索引

```sql
-- 手机号唯一索引（加密后存储，仍保持唯一）
CREATE UNIQUE INDEX idx_user_phone ON user(phone);

-- 姓名不需要全局唯一，但需做敏感词校验
-- 头像 URL 索引（可选，用于快速清理）
CREATE INDEX idx_user_avatar_url ON user(avatar_url);
```

### 2.3 profile_completed 字段

| 值 | 业务含义 | 触发 US |
|----|---------|---------|
| `false` | 用户资料不完整，需完善 | US-004 / US-006（首次登录时设置） |
| `true` | 用户资料已完善 | **US-005（本 US）** |

---

## 3. API 设计

### 3.1 PUT /api/user/profile

完善或更新个人资料。

- **鉴权**：是（需登录态）
- **幂等**：是（`Idempotency-Key: {user_id}:{timestamp}`，TTL 300s）
- **Request**:
  ```json
  {
    "avatar_url": "https://cdn.example.com/avatar/xxx.jpg",
    "name": "张 swimmer",
    "phone": "13800138000",
    "age": 25,
    "gender": "male",
    "has_swim_basis": true,
    "swim_strokes": ["breaststroke", "freestyle"],
    "swim_years": 3,
    "personal_desc": "想提高自由泳",
    "idempotency_key": "uuid"
  }
  ```
- **Response 200**:
  ```json
  {
    "user_id": "xxx",
    "identity_status": "注册用户",
    "profile_completed": true,
    "name": "张 swimmer"
  }
  ```
- **Response 400**:
  - `PHONE_ALREADY_BOUND`（手机号已绑定其他账号）
  - `INVALID_PHONE`（手机号格式非法）
  - `SENSITIVE_NAME`（姓名含敏感词）
  - `INVALID_AGE`（年龄超出范围）
  - `INVALID_AVATAR`（头像格式/大小非法）
- **Response 409**:
  - `IDEMPOTENCY_REUSED`（幂等键已使用但请求体不一致）

### 3.2 GET /api/user/phone/exists

校验手机号是否已被其他账号绑定。

- **鉴权**：是
- **Request query**: `phone`
- **Response 200**:
  ```json
  { "exists": true }
  ```
- **Response 400**: `INVALID_PHONE`
- **Response 429**: `RATE_LIMIT_EXCEEDED`

### 3.3 POST /api/upload/avatar

上传头像。

- **鉴权**：是
- **Request**: `multipart/form-data`，字段 `file`
- **限制**：格式 jpg/png/webp，大小 ≤ 2MB
- **Response 200**:
  ```json
  { "avatar_url": "https://cdn.example.com/avatar/xxx.jpg" }
  ```
- **Response 400**: `INVALID_AVATAR`

---

## 4. 状态机

### 4.1 用户资料完成状态机

```
profile_completed=false ──(US-005 资料完善完成)──→ profile_completed=true
```

> `identity_status` 由 US-004/US-006 在首次登录时置为「注册用户」，本 US 不再修改。

---

## 5. 缓存策略

| 层 | Key | TTL | 用途 | 失效策略 |
|----|-----|-----|------|---------|
| Redis | `phone:exists:{phone_hash}` | 300s | 手机号唯一性校验缓存 | 用户资料变更时失效 |
| Redis | `user:{user_id}` | 1800s | 用户资料缓存 | 资料更新时立即失效 |
| Redis | `idempotency:{key}` | 300s | 幂等键去重 | 自然过期 |

---

## 6. 性能目标

| 指标 | 目标 |
|------|------|
| 完善资料接口 P50 | < 150ms |
| 完善资料接口 P99 | < 300ms |
| 手机号存在性查询 P99 | < 100ms |
| 头像上传 P99 | < 1000ms |
| DB 写入 | < 50ms |

---

## 7. 安全

- `PUT /api/user/profile` 必须登录鉴权
- 手机号 AES-256 加密存储，返回前端时脱敏
- 头像限制格式与大小，上传后做安全扫描/压缩
- 姓名敏感词过滤
- 接口限流：同一用户 1 分钟 > 10 次 → 429
- 必须校验用户已同意当前生效的隐私协议
- 敏感操作记录审计日志
- 幂等键防止重复提交

---

## 8. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-004 | 依赖 | 微信授权登录后创建用户并置 `profile_completed=false`，获取手机号与头像 |
| US-006 | 依赖 | 手机号验证码登录后创建用户并置 `profile_completed=false`，已有手机号 |
| US-009 | 依赖 | 隐私协议与用户须知授权能力：本 US 需校验用户已勾选《用户须知》和《隐私协议》 |
| US-037 | 被依赖 | 教练查看学员档案并添加备注，依赖本 US 维护的用户资料 |
| US-052 | 被依赖 | 用户退出登录，依赖本 US 完善资料后进入「我的」页面 |

---

## 9. 异常与边界

| 场景 | 处理 |
|------|------|
| 快速重复点击保存 | 幂等键 + 前端按钮 loading |
| 网络中断 | 前端提示重试，不丢失已填内容 |
| 头像上传失败 | 保留其他字段，提示重传 |
| 从「我的」编辑清空必填项 | 前端校验阻止 |
| 手机号已被其他账号绑定 | 后端返回 PHONE_ALREADY_BOUND |

---

## 10. 前端交互要点

- 首次完善：无返回按钮，强制流程
- 编辑资料：从「我的」进入，可返回「我的」
- 「有无游泳基础」选择「无」时，隐藏「会什么泳姿」「游泳年限」
- 头像支持拍照/从相册选择
- 手机号字段显示已绑定手机号，修改时实时校验格式

---

## 11. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版：字段为手机号/用户名/密码/邮箱 |
| v2.0 | 2026-08-04 | Dev | 重大修订：字段改为头像/姓名/手机号/年龄/性别/游泳基础/泳姿/年限/个人描述；删除用户名/密码/邮箱；新增头像上传 API；新增从「我的」编辑资料支持 |
