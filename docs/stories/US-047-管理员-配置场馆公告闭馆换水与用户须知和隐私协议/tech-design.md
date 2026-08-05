# US-047 技术设计：管理员配置场馆、公告、闭馆换水、《用户须知》与《隐私协议》

> 角色：开发 | 最后更新：2026-07-30

---

## 0. 文档定位

设计层，定义场馆运营相关数据模型、API、状态机、通知与签署合规方案。

---

## 1. 数据模型影响

### 1.1 新增/修改表

| 表名 | 操作 | 说明 |
|------|------|------|
| `venue` | 新增/修改 | 场馆信息 |
| `notice` | 新增/修改 | 公告 |
| `venue_closure` | 新增 | 闭馆/换水 |
| `terms_policy` | 新增/修改 | 《用户须知》版本（与 US-009 统一表名） |
| `privacy_policy` | 新增/修改 | 《隐私协议》版本（与 US-009 统一表名） |
| `user_terms_consent` | 新增/修改 | 用户/教练《用户须知》同意记录（与 US-009 统一表名） |
| `user_privacy_consent` | 新增/修改 | 用户/教练《隐私协议》同意记录（与 US-009 统一表名） |
| `booking` / `package` | 修改 | 闭馆时取消并释放课时 |

### 1.2 索引

```sql
CREATE UNIQUE INDEX idx_venue_closure_date ON venue_closure(date);
CREATE INDEX idx_notice_status_time ON notice(status, publish_time);
CREATE INDEX idx_terms_current ON terms_policy(is_current);
CREATE INDEX idx_privacy_current ON privacy_policy(is_current);
CREATE INDEX idx_user_terms_user_id ON user_terms_consent(user_id);
CREATE INDEX idx_user_privacy_user_id ON user_privacy_consent(user_id);
```

---

## 2. API 设计

### 2.1 GET/PUT /api/admin/venue

- 鉴权：管理员登录 + `venue:write`
- Response 200 / 403

### 2.2 GET/POST/PUT /api/admin/notices

- 鉴权：管理员登录 + `venue:write`
- Body: `{ title, content, target_roles, publish_time, expire_time }`
- Response 200 / 400

### 2.3 GET/POST /api/admin/venue-closures

- 鉴权：管理员登录 + `venue:write`
- Body: `{ date, reason }`
- Response 200 / 409

### 2.4 GET/POST /api/admin/terms

- 鉴权：管理员登录 + `venue:write`
- Body: `{ version, content }`
- Response 200 / 400

### 2.5 GET/POST /api/admin/privacy

- 鉴权：管理员登录 + `venue:write`
- Body: `{ version, content }`
- Response 200 / 400

### 2.6 GET /api/admin/terms/consent-records

- 鉴权：管理员登录 + `venue:read`
- Query: `version`, `user_id`, `page`, `size`
- Response 200

### 2.7 GET /api/admin/privacy/consent-records

- 鉴权：管理员登录 + `venue:read`
- Query: `version`, `user_id`, `page`, `size`
- Response 200

---

## 3. 状态机影响

```
booking.status: 已预约 ──[闭馆]──→ 已取消 (cancel_reason = 4，场馆闭馆)
terms_policy.is_current: 仅一个 true
privacy_policy.is_current: 仅一个 true
user_terms_consent.status: 已同意 ──[版本更新]──→ 待重新同意
user_privacy_consent.status: 已同意 ──[版本更新]──→ 待重新同意
```

> **cancel_reason 字段类型**（v3 评审 P0 修复）：TINYINT 整型，全项目统一枚举 `1=学员取消 / 2=教练离职 / 3=学员旷课 / 4=场馆闭馆 / 5=教练请假 / 6=套餐冻结`。本 US 闭馆取消课程使用 `4=场馆闭馆`。

---

## 4. 缓存策略

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis | `venue:info` | 600s | 场馆信息变更时失效 |
| Redis | `notices:active` | 300s | 公告变更时失效 |
| Redis | `terms_policy:current` | 1h | 《用户须知》变更时失效 |
| Redis | `privacy_policy:current` | 1h | 《隐私协议》变更时失效 |

---

## 5. 性能指标

| 指标 | 目标 |
|------|------|
| 场馆配置接口 P99 | < 200ms |
| 公告列表 P99 | < 200ms |
| 闭馆影响计算 | 异步，< 5s |

### 5.1 闭馆异步任务失败补偿策略（P1-14 修复）

- **重试机制**：单条 booking 取消失败时自动重试最多 3 次，间隔指数退避（1s / 2s / 4s）
- **部分失败标记**：3 次重试仍失败的 booking，任务状态置为 `partial_failed`，写入 `closure_task_failed` 表（字段：task_id, booking_id, fail_reason, retry_count, created_at）
- **人工介入入口**：管理员后台「场馆运营 → 闭馆任务」查看 `partial_failed` 列表，支持手动重试 / 标记「已人工处理」
- **通知一致性**：取消成功立即发通知；失败的待人工处理后再补发，避免用户收到「取消」但系统未实际取消的歧义

---

## 6. 安全 / 鉴权

- 登录 + RBAC
- 操作日志记录
- 《用户须知》与《隐私协议》内容 XSS 过滤
- 协议内容不可篡改：仅管理员可发布新版本，历史版本不可删除

---

## 7. 跨 US 依赖

| US | 依赖方向 | 说明 |
|----|---------|------|
| US-014 / US-029 | 被依赖 | 预约数据 |
| US-002 / US-019 | 依赖本 US | 展示场馆/公告/用户须知 |
| US-009 | 依赖本 US | 提供《用户须知》与《隐私协议》版本内容及同意记录 |

---

## 8. 异常与边界

| 场景 | 处理 |
|------|------|
| 闭馆日期冲突 | 409 CLOSURE_DATE_CONFLICT |
| 《用户须知》内容为空 | 400 TERMS_CONTENT_EMPTY |
| 《隐私协议》内容为空 | 400 PRIVACY_CONTENT_EMPTY |
| 无权限 | 403 FORBIDDEN |
| 闭馆包含已完成课程 | 跳过，仅取消未上课 |

---

## 9. 实现顺序（与 test-plan 的映射）

| tech-design 章节 | 对应 test-plan Task |
|------------------|---------------------|
| §1 数据模型 | Task 1 |
| §2 API 设计 | Task 2-7 |
| §4 缓存策略 | Task 8 |

---

## 10. 上下游引用

- 上游需求：[./user-story.md](./user-story.md)
- 执行计划：[./test-plan.md](./test-plan.md)
- 全局规范：[docs/spec/tech-design/README.md](../../spec/tech-design/README.md)

---

## 11. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版 |
| v1.1 | 2026-07-31 | Dev | v3 评审 P0 修复：§3 booking 状态机 cancel_reason 从字符串 'venue_closure' 改为整型 `4`（场馆闭馆），与全项目枚举一致 |
| v1.2 | 2026-07-31 | Dev | P1-14 修复：§5.1 新增「闭馆异步任务失败补偿策略」（3 次指数退避重试 + `partial_failed` 标记 + `closure_task_failed` 表 + 人工介入入口 + 通知一致性约束） |
| v1.3 | 2026-08-05 | Dev | 扩展：数据模型/API/缓存/状态机增加《隐私协议》管理；统一表名为 terms_policy / privacy_policy / user_terms_consent / user_privacy_consent |
