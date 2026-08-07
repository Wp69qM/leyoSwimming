# US-056 技术设计文档

> **US 关联**：[user-story.md](./user-story.md)（US-056 教练退出登录）
> **文档角色**：技术设计（plan.md / design.md 角色）
> **作者**：开发　|　**最后更新**：2026-08-07
> **状态**：✅ 已填写

---

## 0. 文档定位

本文档由**开发**填写，作为 US-056 的**技术设计层**。

- **用户故事** = 业务层 WHAT（教练看到什么）
- **本文档** = 设计层 HOW（数据模型 / API / 状态机 / 缓存）
- **test-plan.md** = 执行层 DO（RED→GREEN→COMMIT 的逐任务）

---

## 1. 数据模型影响

### 1.1 修改的表

> 仅列出与 US-056 直接相关的字段；完整字段定义见 US-051 / US-054。

| 表名 | 操作 | 字段 | 说明 |
|------|------|------|------|
| `coach_session` | 修改 | `id`, `coach_id`, `refresh_token_hash`, `expires_at`, `revoked_at`, `created_at` | 退出登录时将当前会话标记为失效（写入 `revoked_at`）或删除记录 |

> **coach_session 表说明**：本表为 US-051 / US-054 / US-056 共享的会话表。US-051 / US-054 写入会话记录；US-056 通过 `revoked_at` 字段或删除记录使会话失效。

### 1.2 索引

```sql
-- 按 refresh_token_hash 查询当前会话
CREATE INDEX idx_coach_session_refresh_token ON coach_session(refresh_token_hash);

-- 按 coach_id 查询教练所有会话
CREATE INDEX idx_coach_session_coach_id ON coach_session(coach_id);
```

---

## 2. API 设计

### 2.1 POST /api/v1/auth/logout

| 属性 | 值 |
|------|----|
| 路径 | `POST /api/v1/auth/logout` |
| 鉴权 | 是（需携带有效 access_token） |
| 幂等 | 是（同一 token 重复调用视为已退出） |

**Request Headers**

```http
Authorization: Bearer {access_token}
```

**Request Body**

```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4..."
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `refreshToken` | string | 否 | 当前会话 refresh_token，用于定位并失效服务端 coach_session |

**Response 200**

```json
{
  "message": "退出登录成功"
}
```

**Response 401 — token 已失效或不存在**

```json
{
  "error": "TOKEN_INVALID",
  "message": "登录态已失效，请重新登录"
}
```

**业务规则**

- 后端从 `Authorization` Header 解析 `access_token`，校验其有效性
- 校验 JWT payload 中 `app_type = 'coach'`，确保操作的是教练会话
- 若请求体包含 `refreshToken`，后端按 SHA-256 hash 查找 `coach_session` 记录，将 `revoked_at` 置为当前时间或删除该记录
- 若请求体未包含 `refreshToken`，仅校验 access_token 有效后返回 200；具体服务端失效策略可降级为依赖 access_token 短期过期
- 退出后，旧 `access_token` 在剩余有效期内仍可能被使用（JWT 无状态特性），建议配合黑名单或短有效期控制；MVP 阶段以清除前端 token + 失效 refresh_token 为主
- 事务边界：标记 coach_session 失效 + 记录 logout 日志在同一事务内

---

## 3. 状态机影响

### 3.1 教练登录态状态机

```
已登录 ──(US-056 教练确认退出)──→ 未登录
```

| 转换 | 触发 US | 触发条件 | 字段变更 |
|------|---------|---------|---------|
| 已登录 → 未登录 | **US-056（本 US）** | 教练点击「退出登录」并确认 | `coach_session.revoked_at` 置为当前时间；前端清除 token |

> **注意**：本 US 不修改 `coach.status` 等教练账号生命周期字段，仅失效当前会话。

---

## 4. 前端流程

### 4.1 时序图

```
教练端「我的」页面                 后端
   │                                │
   │ 1. 点击「退出登录」             │
   │ 2. 弹出确认弹窗                 │
   │ 3. 教练点击「确定」             │
   ├──── POST /auth/logout ────────→│
   │   Authorization: Bearer        │
   │                                │
   │ 4. 校验 access_token           │
   │ 5. 校验 app_type='coach'       │
   │ 6. 失效 refresh_token          │
   │<── 200 message ────────────────┤
   │                                │
   │ 7. 清除本地 token               
   │ 8. 跳转回教练登录页（pages/login/index） │
```

### 4.2 本地存储清除

清除以下 key：

- `access_token`
- `refresh_token`
- `expires_in`
- `token_expire_at`

---

## 5. 安全 / 鉴权

- `POST /api/v1/auth/logout` 必须登录鉴权
- 必须校验请求中的 `access_token` 属于当前登录教练（`app_type='coach'`）
- 退出后 `refresh_token` 必须失效，禁止用其换发新的 `access_token`
- 清除前端存储时需同时清除内存与持久化 storage
- MVP 阶段对 `access_token` 黑名单可做可不做；如不做，依赖其 2h 短有效期自然过期

---

## 6. 跨 US 依赖

| US | 依赖方向 | 说明 |
|----|---------|------|
| US-051 | 被本 US 依赖 | 微信授权登录创建教练会话与 token 机制 |
| US-054 | 被本 US 依赖 | 手机号验证码登录创建教练会话与 token 机制 |

---

## 7. 异常与边界

| 场景 | 处理 |
|------|------|
| 网络异常导致后端退出接口失败 | 前端仍清除本地 token，跳转回教练登录页（pages/login/index）；服务端 token 按 TTL 自然过期 |
| 教练快速重复点击退出登录 | 前端按钮 loading + disable；后端接口幂等 |
| 本地 token 已不存在 | 前端直接清除残留 token，跳转回教练登录页（pages/login/index） |
| refresh_token 已过期 | 后端返回 401；前端清除本地 token，跳转回教练登录页（pages/login/index） |
| 多设备登录 | 仅失效当前设备 session，不影响其他设备 |
| 用户端 token 误调用教练端逻辑 | 后端通过 `app_type` 白名单拒绝，返回 401 TOKEN_INVALID |

---

## 8. 实现顺序（与 test-plan.md 的映射）

| tech-design 章节 | 对应 test-plan Task |
|------------------|---------------------|
| §2.1 POST /auth/logout | Task 1（后端退出接口） |
| §4 前端流程 | Task 2（前端退出逻辑） |
| §5 安全/鉴权 | Task 1 + Task 2 |

---

## 9. 上下游引用

- **用户故事**：[./user-story.md](./user-story.md)
- **测试计划**：[./test-plan.md](./test-plan.md)
- **全局设计规范**：[../../figma/README.md](../../figma/README.md)
- **测试计划规范**：[../../spec/test-plan/README.md](../../spec/test-plan/README.md)
- **PRD §5.2.1 注册登录**：[../../prd/prd.md](../../prd/prd.md)

---

## 10. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-08-07 | Dev | 初版：教练退出登录接口设计、状态机、安全策略、跨 US 依赖 |
