# US-004 技术设计文档

> **US 关联**：[user-story.md](./user-story.md)（US-004 用户微信授权登录）
> **文档角色**：技术设计（plan.md / design.md 角色）
> **作者**：开发　|　**最后更新**：2026-07-30
> **状态**：✅ 已填写

---

## 0. 文档定位

本文档由**开发**填写，作为 US-004 的**技术设计层**（对应 SDD 工作流 Step [3] plan.md 角色）。
它告诉团队"要实现什么"以及"整体技术方案是什么"，但**不替代** [test-plan.md](./test-plan.md) 中的逐任务执行细节。

- **用户故事** = 业务层 WHAT（用户看到什么）
- **本文档** = 设计层 HOW（数据模型 / API / 状态机 / 缓存）
- **test-plan.md** = 执行层 DO（RED→GREEN→COMMIT 的逐任务）

---

## 1. 数据模型影响

### 1.1 新增/修改的表

> 仅列出与 US-004 直接相关的字段；完整字段定义见 PRD §9。

| 表名 | 操作 | 字段 | 说明 |
|------|------|------|------|
| `user` | 新增（首次登录时 INSERT） | `id`, `openid`, `union_id`, `identity_status`, `profile_completed`, `status`, `created_at`, `updated_at` | 首次登录时插入新用户记录 |
| `user_session` | 新增 | `id`, `user_id`, `session_key_encrypted`, `refresh_token_hash`, `expires_at`, `created_at` | 会话管理；session_key 加密存储 |

### 1.2 user 表关键字段说明

| 字段 | 类型 | 取值范围 | 业务含义 |
|------|------|---------|---------|
| `id` | BIGINT | 自增主键 | 用户唯一标识 |
| `openid` | VARCHAR(64) | 微信 openid | 微信小程序维度唯一 |
| `union_id` | VARCHAR(64) | 微信 union_id | 跨小程序/公众号唯一（需绑定微信开放平台）|
| `identity_status` | VARCHAR(20) | `游客` / `注册用户` / `学员` | 用户身份状态机字段 |
| `profile_completed` | BOOLEAN | `true` / `false` | 是否已补充资料（US-005 完成后置 true） |
| `status` | VARCHAR(20) | `active` / `deleted` | 账号生命周期状态；注销后软删除置 `deleted` |

### 1.3 索引

```sql
-- union_id 是核心查询键，必须唯一索引
CREATE UNIQUE INDEX idx_user_union_id ON user(union_id) WHERE status = 'active';

-- openid 兜底唯一键（union_id 缺失场景）
CREATE UNIQUE INDEX idx_user_openid ON user(openid) WHERE status = 'active';

-- user_session 按用户查询
CREATE INDEX idx_user_session_user_id ON user_session(user_id);

-- refresh_token_hash 校验
CREATE INDEX idx_user_session_refresh_token ON user_session(refresh_token_hash);
```

> **约束说明**：`union_id` 唯一索引带 `WHERE status = 'active'` 条件，允许已注销账号（status='deleted'）的 union_id 被新账号复用，符合 PRD [§5.2.1 第 4 条](../../prd/prd.md)。

---

## 2. API 设计

### 2.1 POST /auth/wechat-login

| 属性 | 值 |
|------|----|
| 路径 | `POST /api/v1/auth/wechat-login` |
| 鉴权 | 否（登录入口） |
| 幂等 | 是（以 `code` 为幂等键，5 分钟内有效） |

**Request Body**

```json
{
  "code": "0a3xPP000xxx",
  "encryptedData": "...",
  "iv": "..."
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `code` | string | 是 | `wx.login()` 返回的临时登录凭证，5 分钟内有效，仅可使用一次 |
| `encryptedData` | string | 否 | 完整用户信息的加密数据（可选，本 US 不强制要求） |
| `iv` | string | 否 | 加密算法的初始向量（与 encryptedData 配套） |

**Response 200**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...",
  "expiresIn": 7200,
  "isNewUser": true,
  "profileCompleted": false,
  "userId": 1001
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `accessToken` | string | JWT 访问令牌，有效期 2h |
| `refreshToken` | string | 刷新令牌，有效期 7d |
| `expiresIn` | int | accessToken 剩余有效期（秒） |
| `isNewUser` | boolean | 是否为本次新建的用户 |
| `profileCompleted` | boolean | 是否已完成补充资料（决定前端跳转） |
| `userId` | int | 用户 ID |

**Response 401 — code 已失效**

```json
{
  "error": "WECHAT_CODE_INVALID",
  "message": "登录凭证已失效，请重新点击登录"
}
```

**Response 502 — 微信接口错误**

```json
{
  "error": "WECHAT_API_ERROR",
  "message": "微信服务暂时不可用，请稍后重试"
}
```

**Response 504 — 微信接口超时**

```json
{
  "error": "WECHAT_API_TIMEOUT",
  "message": "网络异常，请重试"
}
```

**业务规则**
- `code` 调用 `code2session` 失败时按 errcode 区分：`40029` → 401，`45011` → 502（频率限制），其他 → 502
- `union_id` 命中已有 `status='active'` 用户 → 复用，`isNewUser=false`
- `union_id` 未命中 → 新建用户，`identity_status='注册用户'`，`profile_completed=false`，`isNewUser=true`
- `union_id` 命中已有 `status='deleted'` 用户 → 按 PRD §5.2.1 第 4 条，新建账号，不绑定原数据
- 事务边界：`查询用户 + 创建用户 + 签发 token + 写 session` 必须在同一事务内

---

## 3. 状态机影响

### 3.1 用户身份状态机（PRD §4）

```
游客 ──(US-004 微信授权登录)──→ 注册用户 ──(US-020 购买正价套餐)──→ 学员
                                  ↑
                                  └──(US-007 注销后重新注册)──→ 新账号仍是注册用户
```

| 转换 | 触发 US | 触发条件 | 字段变更 |
|------|---------|---------|---------|
| 游客 → 注册用户 | **US-004（本 US）** | 首次微信授权登录成功 | `identity_status: 游客 → 注册用户`（新建记录时直接置为 `注册用户`） |
| 注册用户 → 学员 | US-020 | 购买正价套餐并支付成功 | `identity_status: 注册用户 → 学员` |
| 学员 → 注册用户 | US-007 | 账号注销后重新注册 | 新账号 `identity_status = 注册用户` |

### 3.2 本 US 的状态转换实现

```typescript
// 伪代码：首次登录时的状态转换
async function loginWithWechat(code: string) {
  const wechatSession = await code2session(code);  // 调用微信
  const existingUser = await userRepo.findByUnionId(wechatSession.unionid);

  if (existingUser && existingUser.status === 'active') {
    // 老用户：复用账号，不修改 identity_status
    return issueToken(existingUser, { isNewUser: false });
  }

  // 新用户：identity_status 直接置为 '注册用户'（状态机转换：游客→注册用户）
  const newUser = await userRepo.create({
    openid: wechatSession.openid,
    union_id: wechatSession.unionid,
    identity_status: '注册用户',  // 状态转换
    profile_completed: false,
    status: 'active',
  });

  return issueToken(newUser, { isNewUser: true });
}
```

> **注意**：本 US 是第一个涉及"写入"和"状态机转换"的 US。`游客` 状态是隐式的（即未登录或未注册），不需要在 user 表中持久化；只有 `注册用户` 及以上状态才对应 user 表记录。

---

## 4. 微信 OAuth 流程

### 4.1 时序图

```
小程序                 后端                  微信开放平台
  │                     │                       │
  │ 1. wx.login()       │                       │
  ├─────────────────────────────────────────────→│
  │ 2. code             │                       │
  ←─────────────────────────────────────────────┤
  │                     │                       │
  │ 3. POST /auth/wechat-login (code)           │
  ├────────────────────→│                       │
  │                     │ 4. code2session(code) │
  │                     ├──────────────────────→│
  │                     │ 5. openid+union_id+   │
  │                     │    session_key        │
  │                     ←──────────────────────┤
  │                     │                       │
  │                     │ 6. findByUnionId      │
  │                     │   / create user       │
  │                     │ 7. 签发 JWT +         │
  │                     │   写 session          │
  │ 8. accessToken +    │                       │
  │    refreshToken     │                       │
  ←────────────────────┤                       │
  │                     │                       │
  │ 9. 按 profile_completed 跳转                │
  │                     │                       │
```

### 4.2 code2session 接口调用

- 接口：`GET https://api.weixin.qq.com/sns/jscode2session`
- 参数：`appid`、`secret`、`js_code`、`grant_type=authorization_code`
- 超时：3s（超时返回 504 `WECHAT_API_TIMEOUT`）
- 重试：不重试（code 只能用一次，重试无意义）
- 返回字段：`openid`、`session_key`、`unionid`（可能缺失）

### 4.3 session_key 缓存

| 维度 | 策略 |
|------|------|
| 缓存层 | Redis |
| Key | `wechat:session_key:{user_id}` |
| TTL | 7200s（微信官方有效期） |
| 用途 | 后续解密用户敏感数据（如手机号）|
| 失效 | 用户重新登录时覆盖 |

---

## 5. JWT 与会话管理

### 5.1 JWT 结构

```json
{
  "sub": "1001",              // user_id
  "identity_status": "注册用户",
  "profile_completed": false,
  "iat": 1780000000,
  "exp": 1780007200           // 2h 后
}
```

### 5.2 Token 策略

| Token | 用途 | 有效期 | 存储 |
|-------|------|--------|------|
| `access_token` | API 鉴权 | 2h | 前端内存 + Taro storage |
| `refresh_token` | 刷新 access_token | 7d | 前端 Taro storage（加密） + 后端 user_session 表（hash） |

### 5.3 refresh_token 安全

- 后端存储 `refresh_token` 的 SHA-256 hash，不存明文
- 刷新时校验 hash + `expires_at`
- 用户注销（US-007）时删除所有 session 记录

---

## 6. 缓存策略

| 层 | Key | TTL | 用途 | 失效策略 |
|----|-----|-----|------|---------|
| Redis | `wechat:session_key:{user_id}` | 7200s | 微信 session_key | 重新登录时覆盖 |
| Redis | `user:info:{user_id}` | 3600s | 用户基本信息缓存 | 用户资料变更时失效 |
| Redis | `auth:idempotent:wechat-login:{code}` | 300s | code 幂等键 | 自然过期 |

---

## 7. 性能指标

| 指标 | 目标 | 验证方式 |
|------|------|---------|
| 登录接口 P50 | < 500ms | test-plan.md Task 4 + 监控 |
| 登录接口 P99 | < 1500ms（含微信 code2session） | test-plan.md Task 4 + 压测 |
| code2session 调用 | < 1s（95 分位） | 后端调用日志 |
| DB 写入（新用户） | < 50ms | 慢查询监控 |
| 并发 | 200 QPS 下 P99 < 2s | P1 压测 |

---

## 8. 安全 / 鉴权

- `POST /auth/wechat-login` **无需登录**（登录入口）
- `code` 必须来自前端 `wx.login()`，后端不接受手工构造的 code
- `session_key` **绝不返回给前端**（仅在后端使用，解密敏感数据时用）
- `access_token` 使用 HS256 签名，密钥从环境变量读取
- `refresh_token` 使用密码学安全的随机数生成（`crypto.randomBytes(32)`）
- 防刷：同一 IP 1 分钟内 > 30 次登录请求 → 429 限流
- 防 code 重放：Redis 记录已使用的 `code`，5 分钟内重复提交返回首次结果（幂等）

---

## 9. 跨 US 依赖

| US | 依赖方向 | 说明 |
|----|---------|------|
| US-005 | 依赖本 US | 用户补充资料：本 US 创建用户记录并置 `profile_completed=false`，US-005 完成后置 `true` |
| US-006 | 共享 | 手机号/密码登录：共享 `user` 表、JWT 签发逻辑、`user_session` 表 |
| US-007 | 依赖本 US | 账号注销：软删除本 US 创建的用户记录，`status='deleted'` |
| US-008 | 依赖本 US | 账号安全设置：依赖已登录态 |
| US-009 | 依赖本 US | 隐私协议授权：依赖已登录态 |
| US-017 | 依赖本 US | 购买体验课：需先完成登录 |
| US-020 | 依赖本 US | 购买正价套餐：触发 注册用户→学员 状态转换 |

---

## 10. 异常与边界

| 场景 | 处理 |
|------|------|
| 微信 code2session 返回 errcode=40029 | 401 `WECHAT_CODE_INVALID` |
| 微信 code2session 返回 errcode=45011 | 502 `WECHAT_API_ERROR`（频率限制） |
| 微信 code2session 超时（> 3s） | 504 `WECHAT_API_TIMEOUT` |
| `union_id` 缺失（用户未绑定开放平台） | 以 `openid` 兜底，记录告警日志 |
| `union_id` 命中已注销账号（status='deleted'） | 新建账号，不绑定原数据（PRD §5.2.1 第 4 条） |
| 相同 code 5 分钟内重复提交 | 返回首次结果（幂等） |
| 事务部分失败 | 整个事务回滚，返回 500 `LOGIN_FAILED` |
| Redis 宕机 | 降级不缓存 session_key（影响后续解密能力），记录告警，登录仍可成功 |

---

## 11. 实现顺序（与 test-plan.md 的映射）

| tech-design 章节 | 对应 test-plan Task |
|------------------|---------------------|
| §1 数据模型 + 索引 | Task 1（User Repository） |
| §2.1 POST /auth/wechat-login | Task 4（API 端点） |
| §3 状态机影响 | Task 2（OAuth Service，含状态转换） |
| §4 微信 OAuth 流程 | Task 2（OAuth Service） |
| §5 JWT 签发 | Task 3（JWT Service） |
| §6 缓存策略 | Task 2 + Task 3 |
| §7 性能指标 | Task 4 的验收指标 |
| 小程序登录页 | Task 5 |

---

## 12. 上下游引用

- **用户故事**：[./user-story.md](./user-story.md)（业务需求 + GWT + Figma 章节）
- **测试计划**：[./test-plan.md](./test-plan.md)（TDD 任务清单）
- **全局设计规范**：[../../spec/figma/README.md](../../spec/figma/README.md)
- **测试计划规范**：[../../spec/test-plan/README.md](../../spec/test-plan/README.md)
- **PRD §4 用户身份状态机**：[../../prd/prd.md](../../prd/prd.md)
- **PRD §5.2.1 注册登录**：[../../prd/prd.md](../../prd/prd.md)

---

## 13. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版：数据模型 / API / 状态机 / 微信 OAuth 流程 / JWT / 缓存 / 性能 / 安全 / 跨 US 依赖 |
