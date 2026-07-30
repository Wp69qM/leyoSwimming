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
| 2 | 新增标准套餐 Repository 方法 | P0 | §6.1, §6.3, §6.4 |
| 3 | GET /api/admin/package-templates 列表 API | P0 | §6.1 |
| 4 | POST /api/admin/package-templates 新增 API | P0 | §6.1, §6.3, §6.4 |
| 5 | PUT /api/admin/package-templates/:id 编辑与上下架 API | P0 | §6.2 |
| 6 | Redis 缓存与失效策略 | P1 | §6.1, §6.2 |
| 7 | 管理员权限中间件校验 | P0 | §6.5 |

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
      coach_id: 1,
      total_hours: 10,
      valid_days: 180,
      price: 3000.00,
      status: 1,
    });
    expect(tpl.id).toBeGreaterThan(0);
    expect(tpl.status).toBe(1);
    expect(tpl.total_hours).toBe(10);
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

- 将 `total_hours > 0` 和 `price >= 0` 校验下沉到 Repository 或 Service

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

**对应 GWT**：[§6.3 场景 3](./user-story.md#63-场景-3新增标准套餐时名称重复)、[§6.4 场景 4](./user-story.md#64-场景-4新增标准套餐参数非法)

- [ ] **Step 1: RED — 写失败测试**

```typescript
// backend/tests/repositories/packageTemplate.test.ts
describe('PackageTemplateRepository validation', () => {
  it('rejects duplicate name', async () => {
    const repo = new PackageTemplateRepository();
    await repo.create({ name: '暑期 10 节课', coach_id: 1, total_hours: 10, valid_days: 180, price: 3000, status: 1 });
    await expect(repo.create({ name: '暑期 10 节课', coach_id: 2, total_hours: 8, valid_days: 120, price: 2400, status: 1 }))
      .rejects.toThrow('DUPLICATE_PACKAGE_NAME');
  });

  it('rejects invalid total_hours and price', async () => {
    const repo = new PackageTemplateRepository();
    await expect(repo.create({ name: '非法套餐', coach_id: 1, total_hours: 0, valid_days: 180, price: -100, status: 1 }))
      .rejects.toThrow('INVALID_PACKAGE_PARAM');
  });
});
```

- [ ] **Step 2-6**: 实现校验、跑测试、重构、commit（略，遵循 RED→GREEN→COMMIT）

---

### Task 3: GET /api/admin/package-templates 列表 API [P0]

**Files:**
- Create: `backend/src/controllers/admin/packageTemplate.ts`
- Create: `backend/src/routes/admin/packageTemplate.ts`
- Test: `backend/tests/controllers/admin/packageTemplate.test.ts`

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1管理员新增标准套餐成功)

- [ ] **Step 1-6**: 实现列表查询、分页、按 coach/status 过滤，测试 200 返回；commit message `feat(admin): add GET /package-templates`

---

### Task 4: POST /api/admin/package-templates 新增 API [P0]

**Files:**
- Modify: `backend/src/controllers/admin/packageTemplate.ts`
- Modify: `backend/src/routes/admin/packageTemplate.ts`
- Modify: `backend/tests/controllers/admin/packageTemplate.test.ts`

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1管理员新增标准套餐成功)、[§6.3 场景 3](./user-story.md#63-场景-3新增标准套餐时名称重复)、[§6.4 场景 4](./user-story.md#64-场景-4新增标准套餐参数非法)

- [ ] **Step 1-6**: 实现新增接口，覆盖 201 / 400 / 409；commit message `feat(admin): add POST /package-templates`

---

### Task 5: PUT /api/admin/package-templates/:id 编辑与上下架 API [P0]

**Files:**
- Modify: `backend/src/controllers/admin/packageTemplate.ts`
- Modify: `backend/src/routes/admin/packageTemplate.ts`
- Modify: `backend/tests/controllers/admin/packageTemplate.test.ts`

**对应 GWT**：[§6.2 场景 2](./user-story.md#62-场景-2管理员编辑标准套餐并下架)

- [ ] **Step 1-6**: 实现编辑与 toggle-status，覆盖 200 / 404；commit message `feat(admin): add PUT /package-templates/:id`

---

### Task 6: Redis 缓存与失效策略 [P1]

**Files:**
- Modify: `backend/src/repositories/packageTemplate.ts`
- Modify: `backend/src/services/cache.ts`
- Test: `backend/tests/repositories/packageTemplate.test.ts`

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1管理员新增标准套餐成功)、[§6.2 场景 2](./user-story.md#62-场景-2管理员编辑标准套餐并下架)

- [ ] **Step 1-6**: 加 Redis 缓存，写操作主动失效；commit message `feat(cache): add package template cache`

---

### Task 7: 管理员权限中间件校验 [P0]

**Files:**
- Modify: `backend/src/middlewares/adminAuth.ts`
- Test: `backend/tests/middlewares/adminAuth.test.ts`

**对应 GWT**：[§6.5 场景 5](./user-story.md#65-场景-5无权限管理员访问配置接口)

- [ ] **Step 1-6**: 校验 `package:write` 权限，无权限返回 403 FORBIDDEN；commit message `feat(auth): enforce package:write permission`

---

## 3. 任务执行纪律

- **严格顺序**：Task 1 → Task 2 → Task 3 → Task 4 → Task 5 → Task 6 → Task 7
- **每步必须可见**：Step 1（RED）→ Step 2（看失败）→ Step 3（GREEN）→ Step 4（看通过）→ Step 5（REFACTOR）→ Step 6（COMMIT）
- **不允许 placeholder**：任何 "TBD" / "TODO" / "实现 later" / "类似 Task N" = 立即返工
- **每个 Task 结束 = 1 次 commit**：禁止跨 Task 累积 commit
- **P0 必做 / P1 选做**：MVP 阶段先完成 P0（Task 1-5, 7），P1（Task 6）视进度决定
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
