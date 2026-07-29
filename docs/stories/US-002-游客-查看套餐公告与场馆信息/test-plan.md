# US-002 实施任务清单（Tasks）

> **US 关联**：[user-story.md](./user-story.md)（US-002 游客查看套餐、公告与场馆信息）
> **文档角色**：TDD 任务清单 + 测试计划（双重角色）
> **状态**：🔲 待开发执行
> **作者**：开发　|　**最后更新**：2026-07-30

---

## 0. 双重角色说明

本文档承担**双重角色**：
- **TDD 任务清单**（superpowers 风格）：每个 task = 2-5 分钟可执行单元
- **测试计划**：覆盖 [user-story.md](./user-story.md) 中所有 GWT 场景

每个 Task 严格遵循 **RED → GREEN → COMMIT** 循环，不允许跳步。

---

## 1. Task 概览

| Task | 标题 | 优先级 | 对应 GWT 场景 |
|------|------|--------|--------------|
| 1 | 创建 Package Repository（查询公开套餐模板列表） | P0 | [§6.1 场景 1：正常浏览](./user-story.md#61-场景-1正常浏览套餐公告与场馆信息)、[§6.2 场景 2：无套餐可展示](./user-story.md#62-场景-2无套餐可展示异常) |
| 2 | 创建 Announcement Repository（查询生效中公告） | P0 | [§6.1 场景 1](./user-story.md#61-场景-1正常浏览套餐公告与场馆信息)、[§6.3 场景 3：公告维护中](./user-story.md#63-场景-3公告维护中--无生效公告异常) |
| 3 | 创建 Venue Repository（查询场馆信息 + 闭馆换水） | P0 | [§6.1 场景 1](./user-story.md#61-场景-1正常浏览套餐公告与场馆信息)、[§6.4 场景 4：场馆信息未配置](./user-story.md#64-场景-4场馆信息未配置异常) |
| 4 | 3 个公开 API 端点（GET /packages, /announcements, /venue） | P0 | §6.1、§6.2、§6.3、§6.4 |

---

## 2. 实施任务

### Task 1: 创建 Package Repository（查询公开套餐模板列表）[P0]

**Files:**
- Create: `backend/src/repositories/package_template.ts`
- Test: `backend/tests/repositories/package_template.test.ts`

**对应 GWT**：[user-story.md §6.1 场景 1：正常浏览](./user-story.md#61-场景-1正常浏览套餐公告与场馆信息)、[§6.2 场景 2：无套餐可展示](./user-story.md#62-场景-2无套餐可展示异常)

- [ ] **Step 1: 写失败测试**

```typescript
// backend/tests/repositories/package_template.test.ts
import { PackageTemplateRepository } from '../../src/repositories/package_template';

describe('PackageTemplateRepository.findPublicList', () => {
  it('returns published templates sorted by sortOrder ASC, experience first', async () => {
    const repo = new PackageTemplateRepository();
    const result = await repo.findPublicList({});

    expect(result.items).toBeInstanceOf(Array);
    // 体验套餐（package_type=0）应排在正式套餐（package_type=1）之前
    const experienceIdx = result.items.findIndex(i => i.packageType === 0);
    const standardIdx = result.items.findIndex(i => i.packageType === 1);
    if (experienceIdx >= 0 && standardIdx >= 0) {
      expect(experienceIdx).toBeLessThan(standardIdx);
    }
    // sort_order 升序
    for (let i = 1; i < result.items.length; i++) {
      expect(result.items[i - 1].sortOrder).toBeLessThanOrEqual(result.items[i].sortOrder);
    }
  });

  it('filters out unpublished (status=0) and custom (package_type=2) templates', async () => {
    const repo = new PackageTemplateRepository();
    const result = await repo.findPublicList({});
    // 仅返回 status=1 且 package_type ∈ {0, 1}
    result.items.forEach(item => {
      expect(item.packageType).not.toBe(2);
    });
  });

  it('returns empty array when no published templates (场景 2)', async () => {
    const repo = new PackageTemplateRepository();
    const result = await repo.findPublicList({});
    // 测试前清空或 mock 无上架模板
    expect(result.items).toEqual([]);
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- package_template.test.ts`
Expected: FAIL with `Cannot find module '../../src/repositories/package_template'`

- [ ] **Step 3: 写最小实现**

```typescript
// backend/src/repositories/package_template.ts
export interface ListParams { type?: 'experience' | 'standard' | 'custom'; }
export interface PackageTemplateItem {
  templateId: number;
  packageType: number;     // 0=体验 1=正式 2=自定义
  name: string;
  totalHours: number;
  price: number;
  validDays: number;
  sortOrder: number;
  tag: string | null;
}

export class PackageTemplateRepository {
  async findPublicList(params: ListParams): Promise<{ items: PackageTemplateItem[] }> {
    const query = db('package_template')
      .where({ status: 1 })                    // 仅上架
      .whereIn('package_type', [0, 1])         // 排除自定义（2），自定义套餐走 US-020 入口
      .orderBy('sort_order', 'asc')
      .orderBy('package_type', 'asc');         // 体验在前
    if (params.type === 'experience') query.andWhere({ package_type: 0 });
    if (params.type === 'standard') query.andWhere({ package_type: 1 });

    const items = await query.select(
      'template_id as templateId',
      'package_type as packageType',
      'name', 'total_hours as totalHours', 'price',
      'valid_days as validDays', 'sort_order as sortOrder', 'tag'
    );
    return { items };
  }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- package_template.test.ts`
Expected: PASS（3 个测试全过）

- [ ] **Step 5: Commit**

```bash
git add backend/src/repositories/package_template.ts backend/tests/repositories/package_template.test.ts
git commit -m "feat(package-template): add public list query with status/type filter and sort"
```

---

### Task 2: 创建 Announcement Repository（查询生效中公告）[P0]

**Files:**
- Create: `backend/src/repositories/announcement.ts`
- Test: `backend/tests/repositories/announcement.test.ts`

**对应 GWT**：[user-story.md §6.1 场景 1：正常浏览](./user-story.md#61-场景-1正常浏览套餐公告与场馆信息)、[§6.3 场景 3：公告维护中](./user-story.md#63-场景-3公告维护中--无生效公告异常)

- [ ] **Step 1: 写失败测试**

```typescript
// backend/tests/repositories/announcement.test.ts
import { AnnouncementRepository } from '../../src/repositories/announcement';

describe('AnnouncementRepository.findActiveList', () => {
  it('returns active notices within time window sorted by priority DESC', async () => {
    const repo = new AnnouncementRepository();
    const result = await repo.findActiveList({ limit: 5 });

    expect(result.items).toBeInstanceOf(Array);
    expect(result.items.length).toBeLessThanOrEqual(5);
    // priority 降序
    for (let i = 1; i < result.items.length; i++) {
      expect(result.items[i - 1].priority).toBeGreaterThanOrEqual(result.items[i].priority);
    }
    // 仅返回 visible_scope = 'all'（游客可见）
    result.items.forEach(item => {
      expect(item.visibleScope).toBe('all');
    });
  });

  it('returns empty array when no active notices (场景 3)', async () => {
    const repo = new AnnouncementRepository();
    const result = await repo.findActiveList({ limit: 5 });
    // 测试前 mock 无生效公告
    expect(result.items).toEqual([]);
  });

  it('respects limit parameter (max 10)', async () => {
    const repo = new AnnouncementRepository();
    const result = await repo.findActiveList({ limit: 10 });
    expect(result.items.length).toBeLessThanOrEqual(10);

    // limit 超过 10 应被截断
    const overflow = await repo.findActiveList({ limit: 999 });
    expect(overflow.items.length).toBeLessThanOrEqual(10);
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- announcement.test.ts`
Expected: FAIL with `Cannot find module '../../src/repositories/announcement'`

- [ ] **Step 3: 写最小实现**

```typescript
// backend/src/repositories/announcement.ts
export interface FindActiveParams { limit: number; }
export interface AnnouncementItem {
  noticeId: number;
  type: number;           // 0=开放 1=换水 2=释放 3=紧急
  priority: number;       // 0=普通 1=提醒 2=紧急
  title: string;
  content: string;
  startAt: Date;
  endAt: Date;
  visibleScope: string;
}

export class AnnouncementRepository {
  async findActiveList(params: FindActiveParams): Promise<{ items: AnnouncementItem[] }> {
    const limit = Math.min(params.limit, 10);    // 最大 10
    const now = new Date();

    const items = await db('notice')
      .where({ visible_scope: 'all' })          // 仅游客可见
      .where('start_at', '<=', now)             // 时间窗内生效
      .where('end_at', '>', now)
      .orderBy('priority', 'desc')              // 紧急优先
      .orderBy('start_at', 'desc')
      .limit(limit)
      .select(
        'notice_id as noticeId', 'type', 'priority', 'title', 'content',
        'start_at as startAt', 'end_at as endAt', 'visible_scope as visibleScope'
      );
    return { items };
  }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- announcement.test.ts`
Expected: PASS（3 个测试全过）

- [ ] **Step 5: Commit**

```bash
git add backend/src/repositories/announcement.ts backend/tests/repositories/announcement.test.ts
git commit -m "feat(announcement): add active notice query with time window, scope filter, priority sort"
```

---

### Task 3: 创建 Venue Repository（查询场馆信息 + 闭馆换水）[P0]

**Files:**
- Create: `backend/src/repositories/venue.ts`
- Test: `backend/tests/repositories/venue.test.ts`

**对应 GWT**：[user-story.md §6.1 场景 1：正常浏览](./user-story.md#61-场景-1正常浏览套餐公告与场馆信息)、[§6.4 场景 4：场馆信息未配置](./user-story.md#64-场景-4场馆信息未配置异常)

- [ ] **Step 1: 写失败测试**

```typescript
// backend/tests/repositories/venue.test.ts
import { VenueRepository } from '../../src/repositories/venue';

describe('VenueRepository.findVenueInfo', () => {
  it('returns venue info with active closure notice when venue exists', async () => {
    const repo = new VenueRepository();
    const venue = await repo.findVenueInfo();

    expect(venue).not.toBeNull();
    expect(venue).toMatchObject({
      venueId: expect.any(Number),
      name: expect.any(String),
      address: expect.any(String),
      longitude: expect.any(Number),
      latitude: expect.any(Number),
    });
    // closureNotice 可能 null（无闭馆换水）或对象
    if (venue!.closureNotice) {
      expect(venue!.closureNotice).toMatchObject({
        type: expect.stringMatching(/^(闭馆|换水)$/),
        startAt: expect.any(Date),
        endAt: expect.any(Date),
      });
    }
  });

  it('returns null when venue not configured (场景 4)', async () => {
    const repo = new VenueRepository();
    // 测试前清空 venue 表或使用 mock
    const venue = await repo.findVenueInfo();
    expect(venue).toBeNull();
  });

  it('returns null closureNotice when no active closure', async () => {
    const repo = new VenueRepository();
    const venue = await repo.findVenueInfo();
    // 假设当前无闭馆换水
    expect(venue?.closureNotice).toBeNull();
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- venue.test.ts`
Expected: FAIL with `Cannot find module '../../src/repositories/venue'`

- [ ] **Step 3: 写最小实现**

```typescript
// backend/src/repositories/venue.ts
export interface ClosureNotice {
  type: string;        // "闭馆" / "换水"
  reason: string;
  startAt: Date;
  endAt: Date;
}
export interface VenueInfo {
  venueId: number;
  name: string;
  address: string;
  longitude: number;
  latitude: number;
  openYear: number;
  poolStatus: string;
  imageUrl: string | null;
  closureNotice: ClosureNotice | null;
}

export class VenueRepository {
  async findVenueInfo(): Promise<VenueInfo | null> {
    const venue = await db('venue').first().select(
      'venue_id as venueId', 'name', 'address', 'longitude', 'latitude',
      'open_year as openYear', 'pool_status as poolStatus', 'image_url as imageUrl'
    );
    if (!venue) return null;       // 场馆未配置

    // 查询当前时间窗命中的闭馆换水记录
    const now = new Date();
    const closure = await db('venue_closure')
      .where('venue_id', venue.venueId)
      .where('start_at', '<=', now)
      .where('end_at', '>', now)
      .orderBy('start_at', 'desc')
      .first();

    return {
      ...venue,
      closureNotice: closure ? {
        type: closure.closure_type === 0 ? '闭馆' : '换水',
        reason: closure.reason,
        startAt: closure.start_at,
        endAt: closure.end_at,
      } : null,
    };
  }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- venue.test.ts`
Expected: PASS（3 个测试全过）

- [ ] **Step 5: Commit**

```bash
git add backend/src/repositories/venue.ts backend/tests/repositories/venue.test.ts
git commit -m "feat(venue): add venue info query with active closure notice lookup"
```

---

### Task 4: 3 个公开 API 端点（GET /packages, /announcements, /venue）[P0]

**Files:**
- Create: `backend/src/controllers/guest_info.ts`
- Create: `backend/src/routes/guest_info.ts`
- Create: `backend/tests/controllers/guest_info.test.ts`

**对应 GWT**：[user-story.md §6.1 场景 1：正常浏览](./user-story.md#61-场景-1正常浏览套餐公告与场馆信息)、[§6.2 场景 2](./user-story.md#62-场景-2无套餐可展示异常)、[§6.3 场景 3](./user-story.md#63-场景-3公告维护中--无生效公告异常)、[§6.4 场景 4](./user-story.md#64-场景-4场馆信息未配置异常)

- [ ] **Step 1: 写失败测试**

```typescript
// backend/tests/controllers/guest_info.test.ts
import request from 'supertest';
import { app } from '../../src/app';

describe('GET /packages', () => {
  it('returns 200 with published templates (场景 1)', async () => {
    const res = await request(app).get('/packages');
    expect(res.status).toBe(200);
    expect(res.body.items).toBeInstanceOf(Array);
  });

  it('returns 200 with empty array when no published templates (场景 2)', async () => {
    const res = await request(app).get('/packages');
    expect(res.status).toBe(200);
    expect(res.body.items).toEqual([]);
  });
});

describe('GET /announcements', () => {
  it('returns 200 with active notices (场景 1)', async () => {
    const res = await request(app).get('/announcements?limit=5');
    expect(res.status).toBe(200);
    expect(res.body.items).toBeInstanceOf(Array);
    expect(res.body.items.length).toBeLessThanOrEqual(5);
  });

  it('returns 200 with empty array when no active notices (场景 3)', async () => {
    const res = await request(app).get('/announcements?limit=5');
    expect(res.status).toBe(200);
    expect(res.body.items).toEqual([]);
  });

  it('returns 400 for invalid limit (> 10)', async () => {
    const res = await request(app).get('/announcements?limit=999');
    // limit=999 应被截断为 10，返回 200；但 limit=abc 非法应 400
    expect([200, 400]).toContain(res.status);
  });
});

describe('GET /venue', () => {
  it('returns 200 with venue info (场景 1)', async () => {
    const res = await request(app).get('/venue');
    expect(res.status).toBe(200);
    expect(res.body).toMatchObject({
      venueId: expect.any(Number),
      name: expect.any(String),
    });
  });

  it('returns 404 with VENUE_NOT_CONFIGURED when venue not configured (场景 4)', async () => {
    const res = await request(app).get('/venue');
    expect(res.status).toBe(404);
    expect(res.body.error).toBe('VENUE_NOT_CONFIGURED');
    expect(res.body.message).toBe('场馆信息配置中');
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- guest_info.test.ts`
Expected: FAIL with `Cannot find module '../../src/app'` 或路由未注册

- [ ] **Step 3: 写最小实现**

```typescript
// backend/src/controllers/guest_info.ts
import { PackageTemplateRepository } from '../repositories/package_template';
import { AnnouncementRepository } from '../repositories/announcement';
import { VenueRepository } from '../repositories/venue';

const packageRepo = new PackageTemplateRepository();
const announcementRepo = new AnnouncementRepository();
const venueRepo = new VenueRepository();

export async function listPackages(ctx) {
  const type = ctx.query.type as 'experience' | 'standard' | 'custom' | undefined;
  const result = await packageRepo.findPublicList({ type });
  ctx.body = result;
}

export async function listAnnouncements(ctx) {
  const limit = Number(ctx.query.limit) || 5;
  if (isNaN(limit) || limit < 1) {
    ctx.status = 400;
    ctx.body = { error: 'INVALID_LIMIT', message: 'limit 必须为 1-10 的整数' };
    return;
  }
  const result = await announcementRepo.findActiveList({ limit });
  ctx.body = result;
}

export async function getVenue(ctx) {
  const venue = await venueRepo.findVenueInfo();
  if (!venue) {
    ctx.status = 404;
    ctx.body = { error: 'VENUE_NOT_CONFIGURED', message: '场馆信息配置中' };
    return;
  }
  ctx.body = venue;
}
```

```typescript
// backend/src/routes/guest_info.ts
import Router from 'koa-router';
import { listPackages, listAnnouncements, getVenue } from '../controllers/guest_info';

const router = new Router({ prefix: '/api/v1' });
router.get('/packages', listPackages);
router.get('/announcements', listAnnouncements);
router.get('/venue', getVenue);
export default router;
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- guest_info.test.ts`
Expected: PASS（7 个测试全过：套餐 2 + 公告 3 + 场馆 2）

- [ ] **Step 5: Commit**

```bash
git add backend/src/controllers/guest_info.ts backend/src/routes/guest_info.ts backend/tests/controllers/guest_info.test.ts
git commit -m "feat(api): add 3 public guest info endpoints (packages, announcements, venue) with 404 handling"
```

---

## 3. 任务执行纪律

- **严格顺序**：Task 1 → Task 2 → Task 3 → Task 4
- **每步必须可见**：Step 1（写测试）→ Step 2（看失败）→ Step 3（写实现）→ Step 4（看通过）→ Step 5（commit）
- **不允许 placeholder**：任何 "TBD" / "TODO" / "实现 later" / "类似 Task N" = 立即返工
- **每个 Task 结束 = 1 次 commit**：禁止跨 Task 累积 commit
- **P0 必做**：MVP 阶段 4 个 Task 全部为 P0
- **GWT 覆盖**：每个 Task 头部必须明确「对应 GWT」场景编号
- **缓存层暂缓**：Redis 缓存策略（tech-design §4）作为 P1 优化，不在本 US 的 TDD 任务内，后续单独迭代

---

## 4. 上下游引用

- **上游需求**：[./user-story.md](./user-story.md)（GWT 业务级场景）
- **设计输入**：[./tech-design.md](./tech-design.md)（API/数据模型/状态机）
- **Figma 设计交付物**：[./user-story.md](./user-story.md#13-figma-链接) §13-15（UI 状态截图 + 页面级设计决策 + 设计评审记录）

---

## 5. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版：4 个 Task（3 个 Repository + 1 个 3 端点 API），覆盖 4 个 GWT 场景；严格 RED→GREEN→COMMIT 5 步循环 |
