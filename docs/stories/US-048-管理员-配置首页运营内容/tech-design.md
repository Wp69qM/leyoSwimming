# US-048 技术设计：管理员配置首页运营内容

> 本文档对应 `docs/stories/US-048-.../user-story.md` 的技术实现方案。
> 角色：开发 | 最后更新：2026-07-30

---

## 0. 文档定位

设计层，定义首页运营配置的数据模型、API、状态机、缓存与预览方案。

---

## 1. 数据模型影响

### 1.1 新增/修改表

| 表名 | 操作 | 说明 |
|------|------|------|
| `notice` | 新增/修改 | 通知栏公告，复用现有表 |
| `homepage_banner` | 新增 | 首页 Banner |
| `homepage_card` | 新增 | 首页运营卡片 |
| `audit_log` | 新增 | 配置变更审计 |
| `notice` / `homepage_banner` / `homepage_card` | 修改 | 新增 `audit_status` 字段（pending / passed / rejected） |

### 1.2 `homepage_banner` 字段

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT PK | AUTO_INCREMENT | Banner ID |
| `name` | VARCHAR(64) | NOT NULL | 名称 |
| `image_url` | VARCHAR(512) | NOT NULL | 图片地址 |
| `link_url` | VARCHAR(512) | | 跳转链接 |
| `visible_scope` | VARCHAR(16) | NOT NULL | all/visitor/registered/student |
| `sort_order` | INT | NOT NULL | 排序 |
| `start_at` | DATETIME | NOT NULL | 生效时间 |
| `end_at` | DATETIME | NOT NULL | 失效时间 |
| `status` | TINYINT | 0=offline 1=active | 状态 |
| `audit_status` | TINYINT | 0=pending 1=passed 2=rejected | 内容安全审核状态 |

### 1.3 `homepage_card` 字段

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT PK | AUTO_INCREMENT | 卡片 ID |
| `title` | VARCHAR(64) | NOT NULL | 标题 |
| `icon_url` | VARCHAR(512) | | 图标 |
| `link_url` | VARCHAR(512) | | 跳转链接 |
| `visible_scope` | VARCHAR(16) | NOT NULL | all/visitor/registered/student |
| `sort_order` | INT | NOT NULL | 排序 |
| `start_at` | DATETIME | NOT NULL | 生效时间 |
| `end_at` | DATETIME | NOT NULL | 失效时间 |
| `status` | TINYINT | 0=offline 1=active | 状态 |
| `audit_status` | TINYINT | 0=pending 1=passed 2=rejected | 内容安全审核状态 |

### 1.4 索引

```sql
CREATE INDEX idx_notice_visible_time ON notice(status, visible_scope, start_at, end_at);
CREATE INDEX idx_banner_visible_time ON homepage_banner(status, visible_scope, start_at, end_at);
CREATE INDEX idx_card_visible_time ON homepage_card(status, visible_scope, start_at, end_at);
```

---

## 2. API 设计

### 2.1 GET/POST/PUT/DELETE /api/admin/homepage/notices

- **鉴权**：管理员登录 + `homepage:write`
- **GET Response 200**：`{ items: Notice[], total, page, size }`
- **POST Body**：`{ title, content, visible_scope, priority, start_at, end_at }`
- **Response 400**：`{ error: 'INVALID_TIME_RANGE' / 'INVALID_VISIBLE_SCOPE' / 'CONTENT_SECURITY_REJECTED' }`
- **Response 403**：`{ error: 'FORBIDDEN' }`

### 2.2 GET/POST/PUT/DELETE /api/admin/homepage/banners

- **鉴权**：管理员登录 + `homepage:write`
- **POST Body**：`{ name, image_url, link_url, visible_scope, sort_order, start_at, end_at }`
- **Response 201 / 400 / 403 / 404**

### 2.3 GET/POST/PUT/DELETE /api/admin/homepage/cards

- **鉴权**：管理员登录 + `homepage:write`
- **POST Body**：`{ title, icon_url, link_url, visible_scope, sort_order, start_at, end_at }`
- **Response 201 / 400 / 403 / 404**

### 2.4 POST /api/admin/homepage/preview

- **鉴权**：管理员登录 + `homepage:read`
- **Body**：`{ identity: 'visitor' | 'registered' | 'student' }`
- **Response 200**：`{ notices: [], banners: [], cards: [] }`

### 2.5 GET /api/homepage/config

- **鉴权**：公开或登录均可；服务端根据当前身份过滤
- **Query**：`identity`（可选，未登录默认 visitor）
- **Response 200**：`{ notices: [], banners: [], cards: [] }`

---

## 3. 状态机影响

```
notice.status / homepage_banner.status / homepage_card.status:
  active ──[到达 end_at 或管理员手动下线]──→ offline
  offline ──[管理员重新上线或未到 end_at]──→ active
```

- 定时任务每小时扫描，将到达失效时间的条目置 offline

---

## 4. 缓存策略

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis | `homepage:config:{identity}` | 300s | 配置变更时主动失效 |
| Redis | `homepage:notices:active` | 300s | notice 变更时失效 |
| 前端 | 小程序首页本地缓存 | 120s | 下拉刷新时更新 |

### 4.1 降级策略

- Redis 不可用时直接查 DB，记录 warning 日志

---

## 5. 性能指标

| 指标 | 目标 |
|------|------|
| 列表接口 P50 | < 150ms |
| 列表接口 P99 | < 250ms |
| 创建/更新接口 P99 | < 200ms |
| 首页配置接口 P99 | < 150ms |
| 并发 100 QPS | 无 5xx |

---

## 6. 安全 / 鉴权

- 所有管理接口登录 + RBAC
- 内容 XSS 过滤（通知栏内容、卡片标题）
- 图片 URL 域名白名单
- 正式保存前调用内容安全审核服务（文本 + 图片），审核不通过拒绝写入并返回 `CONTENT_SECURITY_REJECTED`
- 预览接口不触发内容安全审核
- 操作日志记录管理员 ID、IP、变更前后快照

---

## 7. 跨 US 依赖

| US | 依赖方向 | 说明 |
|----|---------|------|
| US-004 / US-042 | 被依赖 | 身份与权限基础 |
| US-002 | 依赖本 US | 游客端展示首页运营内容 |
| US-019 | 依赖本 US | 学员端套餐入口可能由运营卡片承载 |

---

## 8. 异常与边界

| 场景 | 处理 |
|------|------|
| 有效期不合法 | 400 INVALID_TIME_RANGE |
| 可见范围非法 | 400 INVALID_VISIBLE_SCOPE |
| 内容安全审核不通过 | 400 CONTENT_SECURITY_REJECTED |
| 无权限 | 403 FORBIDDEN |
| 配置为空 | 首页隐藏对应模块 |
| 图片格式/大小不符 | 上传服务返回 400 |
| 定时任务扫描 offline | 到达 end_at 自动下线 |

---

## 9. 实现顺序（与 test-plan 的映射）

| tech-design 章节 | 对应 test-plan Task |
|------------------|---------------------|
| §1 数据模型 | Task 1 |
| §2 API 设计 | Task 2-6 |
| §4 缓存策略 | Task 7 |

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
| v1.1 | 2026-07-31 | Dev | v3 评审 P1 修复：增加内容安全审核字段与错误码（§1.1/§1.2/§1.3/§2.1/§6/§8）|
