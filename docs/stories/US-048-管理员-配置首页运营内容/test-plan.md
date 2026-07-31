# US-048 实施任务清单（Tasks）

> **US 关联**：[user-story.md](./user-story.md)（US-048 管理员配置首页运营内容）
> **文档角色**：TDD 任务清单 + 测试计划（双重角色）
> **作者**：开发　|　**最后更新**：2026-07-30

---

## 0. 双重角色说明

本文档承担 TDD 任务清单与测试计划双重角色，覆盖 user-story.md 所有 GWT 场景。

每个 Task 严格遵循 **RED → GREEN → REFACTOR → COMMIT** 循环。

---

## 1. Task 概览

| Task | 标题 | 优先级 | 对应 GWT 场景 |
|------|------|--------|--------------|
| 1 | homepage_banner / homepage_card 表迁移与 Repository | P0 | §6.1, §6.2 |
| 2 | GET/POST /api/admin/homepage/notices | P0 | §6.1 |
| 3 | GET/POST /api/admin/homepage/banners | P0 | §6.1, §6.3 |
| 4 | GET/POST /api/admin/homepage/cards | P0 | §6.2, §6.4 |
| 5 | POST /api/admin/homepage/preview | P0 | §6.2 |
| 6 | GET /api/homepage/config | P0 | §6.1, §6.2 |
| 7 | 首页运营缓存与失效策略 | P1 | §6.1, §6.2 |
| 8 | 管理员权限中间件校验 | P0 | §6.5 |
| 9 | 内容安全审核拦截 | P0 | §6.6 |

---

## 2. 实施任务

### Task 1: 表迁移与 Repository [P0]

**Files:**
- Create: `backend/src/migrations/20260730_create_homepage_banner.ts`
- Create: `backend/src/migrations/20260730_create_homepage_card.ts`
- Create: `backend/src/repositories/homepageOps.ts`
- Test: `backend/tests/repositories/homepageOps.test.ts`

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1管理员配置通知栏与-banner-并即时生效)、[§6.2 场景 2](./user-story.md#62-场景-2管理员配置可见范围与预览运营卡片)

- [ ] **Step 1: RED — 写失败测试**

```typescript
// backend/tests/repositories/homepageOps.test.ts
import { HomepageOpsRepository } from '../../src/repositories/homepageOps';

describe('HomepageOpsRepository', () => {
  it('creates a banner with valid fields', async () => {
    const repo = new HomepageOpsRepository();
    const banner = await repo.createBanner({
      name: '清凉一夏',
      image_url: 'https://cdn.example.com/banner1.jpg',
      visible_scope: 'all',
      sort_order: 1,
      start_at: '2026-07-30T00:00:00',
      end_at: '2026-08-31T23:59:59',
      status: 1,
    });
    expect(banner.id).toBeGreaterThan(0);
    expect(banner.status).toBe(1);
  });
});
```

- [ ] **Step 2: 跑测试确认失败**
- [ ] **Step 3: GREEN — 写最小实现**
- [ ] **Step 4: 跑测试确认通过**
- [ ] **Step 5: REFACTOR — 提取字段校验**
- [ ] **Step 6: COMMIT**

```bash
git add backend/src/migrations/20260730_create_homepage_banner.ts backend/src/migrations/20260730_create_homepage_card.ts backend/src/repositories/homepageOps.ts backend/tests/repositories/homepageOps.test.ts
git commit -m "feat(homepage-ops): add banner/card migrations and repository"
```

---

### Task 2: GET/POST /api/admin/homepage/notices [P0]

**Files:**
- Create: `backend/src/controllers/admin/homepageOps.ts`
- Create: `backend/src/routes/admin/homepageOps.ts`
- Test: `backend/tests/controllers/admin/homepageOps.test.ts`

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1管理员配置通知栏与-banner-并即时生效)

- [ ] **Step 1-6**: 实现通知栏管理，覆盖 200/400/403；commit message `feat(admin): add homepage notices CRUD`

---

### Task 3: GET/POST /api/admin/homepage/banners [P0]

**Files:**
- Modify: controller/route/test files

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1管理员配置通知栏与-banner-并即时生效)、[§6.3 场景 3](./user-story.md#63-场景-3有效期不合法)

- [ ] **Step 1-6**: 实现 Banner CRUD，覆盖 201 / 400 INVALID_TIME_RANGE；commit message `feat(admin): add homepage banners CRUD`

---

### Task 4: GET/POST /api/admin/homepage/cards [P0]

**Files:**
- Modify: controller/route/test files

**对应 GWT**：[§6.2 场景 2](./user-story.md#62-场景-2管理员配置可见范围与预览运营卡片)、[§6.4 场景 4](./user-story.md#64-场景-4可见范围参数非法)

- [ ] **Step 1-6**: 实现卡片 CRUD，覆盖 201 / 400 INVALID_VISIBLE_SCOPE；commit message `feat(admin): add homepage cards CRUD`

---

### Task 5: POST /api/admin/homepage/preview [P0]

**Files:**
- Modify: controller/route/test files

**对应 GWT**：[§6.2 场景 2](./user-story.md#62-场景-2管理员配置可见范围与预览运营卡片)

- [ ] **Step 1-6**: 实现预览接口，按身份过滤；commit message `feat(admin): add homepage preview`

---

### Task 6: GET /api/homepage/config [P0]

**Files:**
- Modify: controller/route/test files

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1管理员配置通知栏与-banner-并即时生效)、[§6.2 场景 2](./user-story.md#62-场景-2管理员配置可见范围与预览运营卡片)

- [ ] **Step 1-6**: 实现公开首页配置接口；commit message `feat(homepage): add public homepage config`

---

### Task 7: 首页运营缓存与失效 [P1]

**Files:**
- Modify: `backend/src/repositories/homepageOps.ts`
- Modify: `backend/src/services/cache.ts`
- Test: `backend/tests/repositories/homepageOps.test.ts`

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1管理员配置通知栏与-banner-并即时生效)、[§6.2 场景 2](./user-story.md#62-场景-2管理员配置可见范围与预览运营卡片)

- [ ] **Step 1-6**: 加 Redis 缓存，写操作主动失效；commit message `feat(cache): add homepage config cache`

---

### Task 8: 管理员权限中间件 [P0]

**Files:**
- Modify: `backend/src/middlewares/adminAuth.ts`
- Test: `backend/tests/middlewares/adminAuth.test.ts`

**对应 GWT**：[§6.5 场景 5](./user-story.md#65-场景-5无权限管理员访问配置接口)

- [ ] **Step 1-6**: 校验 `homepage:write` 权限，无权限返回 403 FORBIDDEN；commit message `feat(auth): enforce homepage ops permission`

---

### Task 9: 内容安全审核拦截 [P0]

**Files:**
- Modify: `backend/src/controllers/admin/homepageOps.ts`
- Modify: `backend/src/services/contentSecurity.ts`
- Test: `backend/tests/controllers/admin/homepageOps.test.ts`

**对应 GWT**：[§6.6 场景 6](./user-story.md#66-场景-6运营卡片内容安全审核不通过)

- [ ] **Step 1-6**: 集成文本/图片内容安全审核，审核不通过返回 400 CONTENT_SECURITY_REJECTED；commit message `feat(homepage-ops): add content security audit`

---

## 3. 任务执行纪律

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
- P0 必做，P1 视进度

---

## 4. 上下游引用

- 上游需求：[./user-story.md](./user-story.md)
- 设计输入：[./tech-design.md](./tech-design.md)

---

## 5. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版 |
| v1.1 | 2026-07-31 | Dev | v3 评审 P1 修复：新增 Task 9 内容安全审核拦截|
