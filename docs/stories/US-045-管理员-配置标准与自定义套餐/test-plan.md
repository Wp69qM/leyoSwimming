# US-045 实施任务清单（Tasks）

> **US 关联**：[user-story.md](./user-story.md)（US-045 管理员配置标准与自定义套餐）
> **文档角色**：TDD 任务清单 + 测试计划（双重角色）
> **作者**：开发　|　**最后更新**：2026-07-30

---

## 0. 双重角色说明

本文档承担**双重角色**：
- **TDD 任务清单**：每个 task = 2-5 分钟可执行单元
- **测试计划**：覆盖 [user-story.md](./user-story.md) 中所有 GWT 场景

每个 Task 严格遵循 **RED → GREEN → REFACTOR → COMMIT** 循环。

---

## 1. Task 概览

| Task | 标题 | 优先级 | 对应 GWT 场景 |
|------|------|--------|--------------|
| 1 | 创建 PackageTemplate 表迁移与 Repository | P0 | §6.1, §6.2 |
**| 2 | 新增标准套餐 Repository 校验方法 | P0 | §6.4, §6.5 |**
| 3 | POST /api/admin/package-template/list 列表 API | P0 | §6.1, §6.2 |
| 4 | POST /api/admin/package-template/add 新增 API | P0 | §6.1, §6.4, §6.5, §6.6 |
| 5 | POST /api/admin/package-template/detail 详情 API | P0 | §6.2 |
| 6 | POST /api/admin/image/upload 图片上传 API | P0 | §6.1, §6.2 |
| 7 | POST /api/admin/package-template/update 编辑 API & POST /api/admin/package-template/toggle-status 上下架 API | P0 | §6.2 |
| 8 | Redis 缓存与失效策略 | P1 | §6.1, §6.2 |
| 9 | 管理员权限中间件校验 | P0 | §6.6 |
| 10 | 自定义套餐规则配置 | P0 | §6.3 |

---

## 2. 实施任务

### Task 1: 创建 PackageTemplate 表迁移与 Repository [P0]

**Files:**
- Create: `backend/src/migrations/20260730_create_package_template.ts`
- Create: `backend/src/repositories/packageTemplate.ts`
- Test: `backend/tests/repositories/packageTemplate.test.ts`

**对应 GWT**：[user-story.md §6.1 场景 1](./user-story.md#61-场景-1管理员新增标准套餐成功)、[§6.2 场景 2](./user-story.md#62-场景-2管理员编辑标准套餐并下架)

- [ ] **Step 1: RED — 写失败测试**

```typescript
// backend/tests/repositories/packageTemplate.test.ts
import { PackageTemplateRepository } from '../../src/repositories/packageTemplate';

describe('PackageTemplateRepository', () => {
  it('creates a template with valid fields', async () => {
    const repo = new PackageTemplateRepository();
    const tpl = await repo.create({
      name: '暑期 10 节课',
      coachIds: [1],
      totalHours: 10,
      validDays: 180,
      price: 3000.00,
      status: 1,
    });
    expect(tpl.id).toBeGreaterThan(0);
    expect(tpl.status).toBe(1);
    expect(tpl.totalHours).toBe(10);
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- packageTemplate.test.ts`
Expected: FAIL with `Cannot find module '../../src/repositories/packageTemplate'`

- [ ] **Step 3: GREEN — 写最小实现**

```typescript
// backend/src/repositories/packageTemplate.ts
export class PackageTemplateRepository {
  async create(data) {
    const [id] = await db('package_template').insert(data);
    return await db('package_template').where({ id }).first();
  }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- packageTemplate.test.ts`
Expected: PASS

- [ ] **Step 5: REFACTOR — 提取字段校验**

- 将 `totalHours > 0` 和 `price >= 0` 校验下沉到 Repository 或 Service

- [ ] **Step 6: COMMIT**

```bash
git add backend/src/migrations/20260730_create_package_template.ts backend/src/repositories/packageTemplate.ts backend/tests/repositories/packageTemplate.test.ts
git commit -m "feat(package-template): add migration and repository"
```

---

### Task 2: 新增标准套餐 Repository 校验方法 [P0]

**Files:**
- Modify: `backend/src/repositories/packageTemplate.ts`
- Modify: `backend/tests/repositories/packageTemplate.test.ts`

**对应 GWT**：[§6.4 场景 4](./user-story.md#64-场景-4新增标准套餐时名称重复)、[§6.5 场景 5](./user-story.md#65-场景-5新增标准套餐参数非法)

- [ ] **Step 1: RED — 写失败测试**

```typescript
// backend/tests/repositories/packageTemplate.test.ts
describe('PackageTemplateRepository validation', () => {
  it('rejects duplicate name', async () => {
    const repo = new PackageTemplateRepository();
    await repo.create({ name: '暑期 10 节课', coachIds: [1], totalHours: 10, validDays: 180, price: 3000, status: 1 });
    await expect(repo.create({ name: '暑期 10 节课', coachIds: [2], totalHours: 8, validDays: 120, price: 2400, status: 1 }))
      .rejects.toThrow('DUPLICATE_PACKAGE_NAME');
  });

  it('rejects invalid totalHours and price', async () => {
    const repo = new PackageTemplateRepository();
    await expect(repo.create({ name: '非法套餐', coachIds: [1], totalHours: 0, validDays: 180, price: -100, status: 1 }))
      .rejects.toThrow('INVALID_PACKAGE_PARAM');
  });
});
```

- [ ] **Step 2-6**: 实现校验、跑测试、重构、commit（略，遵循 RED→GREEN→COMMIT）

---

### Task 3: POST /api/admin/package-template/list 列表 API [P0]

**Files:**
- Create: `backend/src/controllers/admin/packageTemplate.ts`
- Create: `backend/src/routes/admin/packageTemplate.ts`
- Test: `backend/tests/controllers/admin/packageTemplate.test.ts`

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1管理员新增标准套餐成功)

- [ ] **Step 1-6**: 实现列表查询、分页（page/pageSize）、按 coach/status 过滤，测试 200 返回；commit message `feat(admin): add POST /api/admin/package-template/list`

---

### Task 4: POST /api/admin/package-template/add 新增 API [P0]

**Files:**
- Modify: `backend/src/controllers/admin/packageTemplate.ts`
- Modify: `backend/src/routes/admin/packageTemplate.ts`
- Modify: `backend/tests/controllers/admin/packageTemplate.test.ts`

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1管理员新增标准套餐成功)、[§6.4 场景 4](./user-story.md#64-场景-4新增标准套餐时名称重复)、[§6.5 场景 5](./user-story.md#65-场景-5新增标准套餐参数非法)、[§6.6 场景 6](./user-story.md#66-场景-6无权限管理员访问配置接口)

- [ ] **Step 1-6**: 实现新增接口，覆盖 201 / 400 / 409；commit message `feat(admin): add POST /api/admin/package-template/add`

---

### Task 5: POST /api/admin/package-template/detail 详情 API [P0]

**Files:**
- Modify: `backend/src/controllers/admin/packageTemplate.ts`
- Modify: `backend/src/routes/admin/packageTemplate.ts`
- Modify: `backend/tests/controllers/admin/packageTemplate.test.ts`

**对应 GWT**：[§6.2 场景 2](./user-story.md#62-场景-2管理员编辑标准套餐并下架)

- [ ] **Step 1: RED — 写失败测试**

```typescript
// backend/tests/controllers/admin/packageTemplate.test.ts
describe('POST /api/admin/package-template/detail', () => {
  it('returns template detail for existing template', async () => {
    const created = await repo.create({ name: '暑期 10 节课', coachIds: [1], totalHours: 10, validDays: 180, price: 3000, status: 'inactive' });
    const res = await request(app)
      .post('/api/admin/package-template/detail')
      .send({ packageTemplateId: created.id })
      .expect(200);
    expect(res.body.data.name).toBe('暑期 10 节课');
    expect(res.body.data.coachIds).toEqual([1]);
  });

  it('returns 404 for non-existent template', async () => {
    await request(app)
      .post('/api/admin/package-template/detail')
      .send({ packageTemplateId: 99999 })
      .expect(404);
  });
});
```

- [ ] **Step 2-6**: 实现详情查询接口，JSON body 传入 `packageTemplateId`，返回模板详情与关联教练列表；commit message `feat(admin): add POST /api/admin/package-template/detail`

---

### Task 6: POST /api/admin/image/upload 图片上传 API [P0]

**Files:**
- Create: `backend/src/controllers/admin/image.ts`
- Create: `backend/src/routes/admin/image.ts`
- Test: `backend/tests/controllers/admin/image.test.ts`

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1管理员新增标准套餐成功)、[§6.2 场景 2](./user-story.md#62-场景-2管理员编辑标准套餐并下架)

- [ ] **Step 1: RED — 写失败测试**

```typescript
// backend/tests/controllers/admin/image.test.ts
describe('POST /api/admin/image/upload', () => {
  it('returns uploaded image URLs for valid images', async () => {
    const res = await request(app)
      .post('/api/admin/image/upload')
      .attach('images', Buffer.from('fake-image'), 'test.png')
      .expect(200);
    expect(res.body.data.urls).toHaveLength(1);
    expect(res.body.data.urls[0]).toMatch(/^https?:\/\//);
  });

  it('returns 400 for invalid file type', async () => {
    await request(app)
      .post('/api/admin/image/upload')
      .attach('images', Buffer.from('not-an-image'), 'test.txt')
      .expect(400);
  });
});
```

- [ ] **Step 2-6**: 实现图片上传接口，校验格式与大小，返回 URL 列表；commit message `feat(admin): add POST /api/admin/image/upload`

---

### Task 7: POST /api/admin/package-template/update 编辑 API & POST /api/admin/package-template/toggle-status 上下架 API [P0]

**Files:**
- Modify: `backend/src/controllers/admin/packageTemplate.ts`
- Modify: `backend/src/routes/admin/packageTemplate.ts`
- Modify: `backend/tests/controllers/admin/packageTemplate.test.ts`

**对应 GWT**：[§6.2 场景 2](./user-story.md#62-场景-2管理员编辑标准套餐并下架)

- [ ] **Step 1-6 (update)**: 实现 `POST /api/admin/package-template/update` 编辑接口，覆盖 200 / 400 / 404；commit message `feat(admin): add POST /api/admin/package-template/update`
- [ ] **Step 7-12 (toggle-status)**: 实现 `POST /api/admin/package-template/toggle-status` 上下架接口，覆盖 200 / 404；commit message `feat(admin): add POST /api/admin/package-template/toggle-status`

---

### Task 8: Redis 缓存与失效策略 [P1]

**Files:**
- Modify: `backend/src/repositories/packageTemplate.ts`
- Modify: `backend/src/services/cache.ts`
- Test: `backend/tests/repositories/packageTemplate.test.ts`

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1管理员新增标准套餐成功)、[§6.2 场景 2](./user-story.md#62-场景-2管理员编辑标准套餐并下架)

- [ ] **Step 1-6**: 加 Redis 缓存，写操作主动失效；commit message `feat(cache): add package template cache`

---

### Task 9: 管理员权限中间件校验 [P0]

**Files:**
- Modify: `backend/src/middlewares/adminAuth.ts`
- Test: `backend/tests/middlewares/adminAuth.test.ts`

**对应 GWT**：[§6.6 场景 6](./user-story.md#66-场景-6无权限管理员访问配置接口)

- [ ] **Step 1-6**: 校验 `package:write` 权限，无权限返回 403 FORBIDDEN；commit message `feat(auth): enforce package:write permission`

---

### Task 10: 自定义套餐规则配置 [P0]

**Files:**
- Modify: `backend/src/controllers/admin/packageTemplate.ts`
- Modify: `backend/src/routes/admin/packageTemplate.ts`
- Modify: `backend/tests/controllers/admin/packageTemplate.test.ts`

**对应 GWT**：[§6.3 场景 3](./user-story.md#63-场景-3管理员配置自定义套餐规则成功)

- [ ] **Step 1-6**: 实现 `POST /api/admin/package-template/custom-config`，覆盖 200 / 400；commit message `feat(admin): add POST /api/admin/package-template/custom-config`

---

## 3. 任务执行纪律

- **严格顺序**：Task 1 → Task 2 → Task 3 → Task 4 → Task 5 → Task 6 → Task 7 → Task 8 → Task 9 → Task 10
- **每步必须可见**：Step 1（RED）→ Step 2（看失败）→ Step 3（GREEN）→ Step 4（看通过）→ Step 5（REFACTOR）→ Step 6（COMMIT）
- **不允许 placeholder**：任何 "TBD" / "TODO" / "实现 later" / "类似 Task N" = 立即返工
- **每个 Task 结束 = 1 次 commit**：禁止跨 Task 累积 commit（Task 7 含 2 个独立 API，拆分为 2 次 commit）
- **P0 必做 / P1 选做**：MVP 阶段先完成 P0（Task 1-7, 9-10），P1（Task 8）视进度决定
- **GWT 覆盖**：每个 Task 头部必须明确「对应 GWT」场景编号

---

## 4. 上下游引用

- **上游需求**：[./user-story.md](./user-story.md)
- **设计输入**：[./tech-design.md](./tech-design.md)
- **Figma 设计交付物**：[./user-story.md](./user-story.md#13-figma-链接) §13-15

---

## 5. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版：7 个 task 覆盖 5 个 GWT 场景 |
| v1.1 | 2026-07-31 | Dev | 增加 Task 8 覆盖自定义套餐规则配置 |
| v1.2 | 2026-08-14 | Dev | 增加 Task 5 标准套餐详情 API、Task 6 图片上传 API，补齐 §7.2 API 影响表 |
