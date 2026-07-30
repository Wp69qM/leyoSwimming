# US-001 实施任务清单（Tasks）

> **US 关联**：[user-story.md](./user-story.md)（US-001 游客浏览教练列表与详情）
> **文档角色**：TDD 任务清单 + 测试计划（双重角色）
> **状态**：🔲 待开发填写（标准 tasks 模板示范）
> **作者**：开发　|　**最后更新**：2026-07-30

---

## 0. 双重角色说明

本文档承担**双重角色**：
- **TDD 任务清单**（superpowers 风格）：每个 task = 2-5 分钟可执行单元
- **测试计划**：覆盖 [user-story.md](./user-story.md) 中所有 GWT 场景

每个 Task 严格遵循 **RED → GREEN → REFACTOR → COMMIT** 循环，不允许跳步。

---

## 1. Task 概览

| Task | 标题 | 优先级 | 对应 GWT 场景 |
|------|------|--------|--------------|
| 1 | 创建 Coach Repository（列表查询 + 排序 + 分页） | P0 | [§6.1 场景 1：正常浏览](./user-story.md#61-场景-1正常浏览)、[§6.6 场景 6：教练申请离职中仍可见且无标签](./user-story.md#66-场景-6教练申请离职中仍可见且无标签) |
| 2 | 创建 Coach Repository（详情查询 + 状态过滤） | P0 | [§6.3 场景 3](./user-story.md#63-场景-3教练请假中)、[§6.4 场景 4](./user-story.md#64-场景-4教练待审核或驳回不出现)、[§6.5 场景 5](./user-story.md#65-场景-5教练已离职不出现)、[§6.6 场景 6](./user-story.md#66-场景-6教练申请离职中仍可见且无标签) |
| 3 | GET /coaches 列表 API 端点 | P0 | §6.1、[§6.2 场景 2：空状态](./user-story.md#62-场景-2空状态)、§6.6 |
| 4 | GET /coaches/:id 详情 API | P0 | §6.3、§6.4、§6.5、§6.6 |
| 5 | 微信小程序列表页 + 缓存 | P1 | §6.1、§6.6 |

---

## 2. 实施任务

### Task 1: 创建 Coach Repository（列表查询 + 排序 + 分页）[P0]

**Files:**
- Create: `backend/src/repositories/coach.ts`
- Test: `backend/tests/repositories/coach.test.ts`

**对应 GWT**：[user-story.md §6.1 场景 1：正常浏览](./user-story.md#61-场景-1正常浏览)、[§6.6 场景 6：教练申请离职中仍可见且无标签](./user-story.md#66-场景-6教练申请离职中仍可见且无标签)

- [ ] **Step 1: RED — 写失败测试**

```typescript
// backend/tests/repositories/coach.test.ts
import { CoachRepository } from '../../src/repositories/coach';

describe('CoachRepository.findPublicList', () => {
  it('returns active coaches sorted by rating DESC with pagination', async () => {
    const repo = new CoachRepository();
    const result = await repo.findPublicList({ page: 1, size: 10 });

    expect(result.items).toBeInstanceOf(Array);
    expect(result.items.length).toBeLessThanOrEqual(10);
    // 评分降序
    for (let i = 1; i < result.items.length; i++) {
      expect(result.items[i - 1].rating).toBeGreaterThanOrEqual(result.items[i].rating);
    }
  });

  it('includes status=4 coaches and excludes status=0/2/3 coaches', async () => {
    const repo = new CoachRepository();
    const result = await repo.findPublicList({ page: 1, size: 10 });
    const statuses = result.items.map(c => c.status);

    expect(statuses).toContain(1);
    expect(statuses).toContain(4);
    expect(statuses).not.toContain(0);
    expect(statuses).not.toContain(2);
    expect(statuses).not.toContain(3);
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- coach.test.ts`
Expected: FAIL with `Cannot find module '../../src/repositories/coach'`

- [ ] **Step 3: GREEN — 写最小实现**

```typescript
// backend/src/repositories/coach.ts
export const PUBLIC_COACH_STATUSES = [1, 4];
export interface ListParams { page: number; size: number; }
export interface CoachListItem { id: number; name: string; rating: number; status: number; }

export class CoachRepository {
  async findPublicList(params: ListParams): Promise<{ items: CoachListItem[]; total: number }> {
    const offset = (params.page - 1) * params.size;
    return {
      items: await db('coach')
        .whereIn('status', PUBLIC_COACH_STATUSES)  // 已通过（1）+ 申请离职中（4）
        .orderBy('rating', 'desc')                 // 评分降序
        .limit(params.size)
        .offset(offset)
        .select('id', 'name', 'rating', 'status'),
      total: await db('coach').whereIn('status', PUBLIC_COACH_STATUSES).count('id as count').first(),
    };
  }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- coach.test.ts`
Expected: PASS

- [ ] **Step 5: REFACTOR — 提取常量并消除重复**

- 已将 `PUBLIC_COACH_STATUSES` 提取到模块级常量
- 确认 `findPublicList` 中查询与计数使用同一常量，避免未来新增可见状态时遗漏

- [ ] **Step 6: COMMIT**

```bash
git add backend/src/repositories/coach.ts backend/tests/repositories/coach.test.ts
git commit -m "feat(coach): add public list query with rating sort and status filter"
```

---

### Task 2: 创建 Coach Repository（详情查询 + 状态过滤）[P0]

**Files:**
- Modify: `backend/src/repositories/coach.ts`
- Modify: `backend/tests/repositories/coach.test.ts`

**对应 GWT**：[user-story.md §6.3 场景 3：教练请假中](./user-story.md#63-场景-3教练请假中)、[§6.4 场景 4：教练待审核或驳回不出现](./user-story.md#64-场景-4教练待审核或驳回不出现)、[§6.5 场景 5：教练已离职不出现](./user-story.md#65-场景-5教练已离职不出现)、[§6.6 场景 6：教练申请离职中仍可见且无标签](./user-story.md#66-场景-6教练申请离职中仍可见且无标签)

- [ ] **Step 1: RED — 写失败测试**

```typescript
// 追加到 backend/tests/repositories/coach.test.ts
describe('CoachRepository.findById', () => {
  it('returns full coach info when coach is active (status=1, 空闲中)', async () => {
    const repo = new CoachRepository();
    const coach = await repo.findById(1);  // 假设 id=1 为已通过且空闲教练

    expect(coach).toMatchObject({
      id: 1,
      status: 1,
      name: expect.any(String),
      certificates: expect.any(Array),
      yearsOfTeaching: expect.any(Number),
      rating: expect.any(Number),
      realTimeStatus: expect.stringMatching(/^(空闲中|上课中|休息中|已下班|请假中)$/),
    });
  });

  it('returns full coach info when coach is in resignation (status=4)', async () => {
    const repo = new CoachRepository();
    const coach = await repo.findById(4);  // 假设 id=4 为申请离职中

    expect(coach).toMatchObject({
      id: 4,
      status: 4,
      name: expect.any(String),
    });
  });

  it('throws COACH_NOT_FOUND for non-existent id', async () => {
    const repo = new CoachRepository();
    await expect(repo.findById(99999)).rejects.toThrow('COACH_NOT_FOUND');
  });

  it('throws COACH_NOT_FOUND for pending (status=0) coach', async () => {
    const repo = new CoachRepository();
    await expect(repo.findById(2)).rejects.toThrow('COACH_NOT_FOUND');  // id=2 状态=待审核
  });

  it('throws COACH_NOT_FOUND for rejected (status=2) coach', async () => {
    const repo = new CoachRepository();
    await expect(repo.findById(5)).rejects.toThrow('COACH_NOT_FOUND');  // id=5 状态=驳回
  });

  it('throws COACH_NOT_FOUND for resigned (status=3) coach', async () => {
    const repo = new CoachRepository();
    await expect(repo.findById(3)).rejects.toThrow('COACH_NOT_FOUND');  // id=3 状态=已离职
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- coach.test.ts`
Expected: FAIL with `repo.findById is not a function`

- [ ] **Step 3: GREEN — 写最小实现**

```typescript
// 修改 backend/src/repositories/coach.ts，添加 findById 方法
import { PUBLIC_COACH_STATUSES } from './coach';  // 同模块常量

export class CoachRepository {
  // ... 已有 findPublicList ...

  async findById(id: number): Promise<CoachDetail> {
    const coach = await db('coach')
      .leftJoin('coach_certificate', 'coach.id', 'coach_certificate.coach_id')
      .where('coach.id', id)
      .whereIn('coach.status', PUBLIC_COACH_STATUSES)  // 仅已通过（1）和申请离职中（4）可见
      .select(
        'coach.id', 'coach.name', 'coach.years_of_teaching', 'coach.rating',
        'coach.status', 'coach.real_time_status', 'coach_certificate.name as certificate_name'
      )
      .first();

    if (!coach) {
      throw new Error('COACH_NOT_FOUND');
    }
    return coach;
  }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- coach.test.ts`
Expected: PASS（6 个新测试全过，共 7 个）

- [ ] **Step 5: REFACTOR — 复用公开状态常量**

- `findById` 与 `findPublicList` 共用 `PUBLIC_COACH_STATUSES`，避免硬编码
- 如需支持 UI 层判断 status=4 是否隐藏标签，可在返回对象中保留 `status` 字段

- [ ] **Step 6: COMMIT**

```bash
git add backend/src/repositories/coach.ts backend/tests/repositories/coach.test.ts
git commit -m "feat(coach): add findById with status filter (hide pending coaches)"
```

---

### Task 3: GET /coaches 列表 API 端点 [P0]

**Files:**
- Create: `backend/src/controllers/coach.ts`
- Create: `backend/src/routes/coach.ts`
- Create: `backend/tests/controllers/coach.test.ts`

**对应 GWT**：[user-story.md §6.1 场景 1：正常浏览](./user-story.md#61-场景-1正常浏览)、[§6.2 场景 2：空状态](./user-story.md#62-场景-2空状态)、[§6.6 场景 6：教练申请离职中仍可见且无标签](./user-story.md#66-场景-6教练申请离职中仍可见且无标签)

- [ ] **Step 1: RED — 写失败测试**

```typescript
// backend/tests/controllers/coach.test.ts
import request from 'supertest';
import { app } from '../../src/app';

describe('GET /coaches', () => {
  it('returns 200 with coaches array (正常浏览)', async () => {
    const res = await request(app).get('/coaches?page=1&size=10');
    expect(res.status).toBe(200);
    expect(res.body.items).toBeInstanceOf(Array);
  });

  it('returns 200 with empty array when no public coaches (空状态)', async () => {
    // 测试前清空 coach 表或使用 mock DB
    const res = await request(app).get('/coaches?page=1&size=10');
    expect(res.status).toBe(200);
    expect(res.body.items).toEqual([]);
  });

  it('includes status=4 coaches and excludes status=0/2/3 coaches', async () => {
    const res = await request(app).get('/coaches?page=1&size=10');
    const statuses = res.body.items.map(c => c.status);

    expect(statuses).toContain(1);
    expect(statuses).toContain(4);
    expect(statuses).not.toContain(0);
    expect(statuses).not.toContain(2);
    expect(statuses).not.toContain(3);
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- coach.test.ts`
Expected: FAIL with `Cannot find module '../../src/app'`

- [ ] **Step 3: GREEN — 写最小实现**

```typescript
// backend/src/controllers/coach.ts
import { CoachRepository } from '../repositories/coach';

const coachRepo = new CoachRepository();

export async function listCoaches(ctx) {
  const { page = 1, size = 10 } = ctx.query;
  const result = await coachRepo.findPublicList({ page: Number(page), size: Number(size) });
  ctx.body = result;
}

// backend/src/routes/coach.ts
import Router from 'koa-router';
import { listCoaches } from '../controllers/coach';

const router = new Router({ prefix: '/coaches' });
router.get('/', listCoaches);
export default router;
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- coach.test.ts`
Expected: PASS（3 个测试全过）

- [ ] **Step 5: REFACTOR — 统一参数解析与常量复用**

- 将 `page/size` 的 `Number()` 转换提取到 small helper 或 middleware，避免控制器重复
- 确认控制器不自行判断 status，过滤逻辑下沉到 Repository

- [ ] **Step 6: COMMIT**

```bash
git add backend/src/controllers/coach.ts backend/src/routes/coach.ts backend/tests/controllers/coach.test.ts
git commit -m "feat(api): add GET /coaches list endpoint"
```

---

### Task 4: GET /coaches/:id 详情 API [P0]

**Files:**
- Modify: `backend/src/controllers/coach.ts`
- Modify: `backend/src/routes/coach.ts`
- Modify: `backend/tests/controllers/coach.test.ts`

**对应 GWT**：[user-story.md §6.3 场景 3：教练请假中](./user-story.md#63-场景-3教练请假中)、[§6.4 场景 4：教练待审核或驳回不出现](./user-story.md#64-场景-4教练待审核或驳回不出现)、[§6.5 场景 5：教练已离职不出现](./user-story.md#65-场景-5教练已离职不出现)、[§6.6 场景 6：教练申请离职中仍可见且无标签](./user-story.md#66-场景-6教练申请离职中仍可见且无标签)

- [ ] **Step 1: RED — 写失败测试**

```typescript
// 追加到 backend/tests/controllers/coach.test.ts
describe('GET /coaches/:id', () => {
  it('returns 200 with full coach info (status=1, 空闲中)', async () => {
    const res = await request(app).get('/coaches/1');
    expect(res.status).toBe(200);
    expect(res.body).toMatchObject({
      id: 1,
      status: 1,
      realTimeStatus: '空闲中',
    });
  });

  it('returns 200 with realTimeStatus=请假中 (场景 3)', async () => {
    const res = await request(app).get('/coaches/3');  // 假设 id=3 状态为请假中
    expect(res.status).toBe(200);
    expect(res.body.realTimeStatus).toBe('请假中');
  });

  it('returns 200 with status=4 coach and no resignation label (场景 6)', async () => {
    const res = await request(app).get('/coaches/4');  // 假设 id=4 状态为申请离职中
    expect(res.status).toBe(200);
    expect(res.body.status).toBe(4);
    // UI 层断言：详情组件不展示"申请离职中"标签（由前端测试覆盖）
  });

  it('returns 404 with COACH_NOT_FOUND for pending coach (status=0)', async () => {
    const res = await request(app).get('/coaches/2');  // 假设 id=2 状态为待审核
    expect(res.status).toBe(404);
    expect(res.body.error).toBe('COACH_NOT_FOUND');
  });

  it('returns 404 with COACH_NOT_FOUND for resigned coach (status=3)', async () => {
    const res = await request(app).get('/coaches/5');  // 假设 id=5 状态为已离职
    expect(res.status).toBe(404);
    expect(res.body.error).toBe('COACH_NOT_FOUND');
  });

  it('returns 404 with COACH_NOT_FOUND for non-existent id', async () => {
    const res = await request(app).get('/coaches/99999');
    expect(res.status).toBe(404);
    expect(res.body.error).toBe('COACH_NOT_FOUND');
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- coach.test.ts`
Expected: FAIL with `404 expected but got 200`（route 还没注册）

- [ ] **Step 3: GREEN — 写最小实现**

```typescript
// 修改 backend/src/controllers/coach.ts
export async function getCoachById(ctx) {
  try {
    const coach = await coachRepo.findById(Number(ctx.params.id));
    ctx.body = coach;
  } catch (err) {
    if (err.message === 'COACH_NOT_FOUND') {
      ctx.status = 404;
      ctx.body = { error: 'COACH_NOT_FOUND', message: '教练信息不存在' };
    } else {
      throw err;
    }
  }
}

// 修改 backend/src/routes/coach.ts
router.get('/:id', getCoachById);
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- coach.test.ts`
Expected: PASS（6 个新测试全过，共 9 个）

- [ ] **Step 5: REFACTOR — 错误处理标准化**

- 将 `COACH_NOT_FOUND` 错误处理逻辑提取为通用 error handler 或 helper，避免各 controller 重复 try/catch
- 确认 404 响应体统一为 `{ error: 'COACH_NOT_FOUND', message: '教练信息不存在' }`

- [ ] **Step 6: COMMIT**

```bash
git add backend/src/controllers/coach.ts backend/src/routes/coach.ts backend/tests/controllers/coach.test.ts
git commit -m "feat(api): add GET /coaches/:id detail endpoint with 404 handling"
```

---

### Task 5: 微信小程序列表页 + 缓存 [P1]

**Files:**
- Create: `miniapp-user/src/pages/coaches/index.tsx`
- Create: `miniapp-user/src/pages/coaches/index.test.tsx`

**对应 GWT**：[user-story.md §6.1 场景 1：正常浏览](./user-story.md#61-场景-1正常浏览)、[§6.6 场景 6：教练申请离职中仍可见且无标签](./user-story.md#66-场景-6教练申请离职中仍可见且无标签)

- [ ] **Step 1: RED — 写失败测试**

```typescript
// miniapp-user/src/pages/coaches/index.test.tsx
import { render } from '@testing-library/react';
import CoachesPage from './index';

describe('CoachesPage', () => {
  it('renders 5 coach cards on success', async () => {
    jest.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      json: async () => ({ items: [/* 5 mock coaches */] }),
    } as any);

    const { findAllByTestId } = render(<CoachesPage />);
    const cards = await findAllByTestId('coach-card');
    expect(cards).toHaveLength(5);
  });

  it('shows empty state when no coaches (场景 2)', async () => {
    jest.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      json: async () => ({ items: [] }),
    } as any);

    const { findByText } = render(<CoachesPage />);
    expect(await findByText('暂无教练入驻，敬请期待')).toBeInTheDocument();
  });

  it('does not show resignation label for status=4 coaches (场景 6)', async () => {
    jest.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      json: async () => ({
        items: [{ id: 4, name: '李教练', status: 4, realTimeStatus: '空闲中' }],
      }),
    } as any);

    const { queryByText } = render(<CoachesPage />);
    expect(queryByText('申请离职中')).not.toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- coaches/index.test.tsx`
Expected: FAIL with `Cannot find module './index'`

- [ ] **Step 3: GREEN — 写最小实现**

```tsx
// miniapp-user/src/pages/coaches/index.tsx
import { useEffect, useState } from 'react';
import { View, Text } from '@tarojs/components';
import Taro from '@tarojs/taro';

interface Coach { id: number; name: string; rating: number; status: number; realTimeStatus: string; }

export default function CoachesPage() {
  const [coaches, setCoaches] = useState<Coach[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchCoaches();
  }, []);

  async function fetchCoaches() {
    try {
      // 优先用本地缓存
      const cached = Taro.getStorageSync('coaches_list');
      if (cached) {
        setCoaches(cached);
        setLoading(false);
      }

      const res = await Taro.request({ url: '/coaches?page=1&size=10' });
      setCoaches(res.data.items);
      Taro.setStorageSync('coaches_list', res.data.items);
    } finally {
      setLoading(false);
    }
  }

  if (loading) return <View>加载中...</View>;
  if (coaches.length === 0) return <View>暂无教练入驻，敬请期待</View>;

  return (
    <View>
      {coaches.map(c => (
        <View
          key={c.id}
          data-testid="coach-card"
          onClick={() => Taro.navigateTo({ url: `/pages/coach-detail/index?id=${c.id}` })}
        >
          <Text>{c.name}</Text>
          {/* status=4 时不展示任何状态标签，仅展示实时状态徽标（如需） */}
          {c.status !== 4 && <Text>{c.realTimeStatus}</Text>}
        </View>
      ))}
    </View>
  );
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- coaches/index.test.tsx`
Expected: PASS（3 个测试全过）

- [ ] **Step 5: REFACTOR — 卡片组件化与常量复用**

- 将教练卡片提取为独立 `<CoachCard />` 组件，统一处理 status=4 的标签隐藏逻辑
- 从 shared 常量或 API 层复用 `PUBLIC_COACH_STATUSES`，前端不单独维护状态白名单

- [ ] **Step 6: COMMIT**

```bash
git add miniapp-user/src/pages/coaches/
git commit -m "feat(miniapp): add coaches list page with storage cache"
```

---

## 3. 任务执行纪律

- **严格顺序**：Task 1 → Task 2 → Task 3 → Task 4 → Task 5
- **每步必须可见**：Step 1（RED）→ Step 2（看失败）→ Step 3（GREEN）→ Step 4（看通过）→ Step 5（REFACTOR）→ Step 6（COMMIT）
- **不允许 placeholder**：任何 "TBD" / "TODO" / "实现 later" / "类似 Task N" = 立即返工
- **每个 Task 结束 = 1 次 commit**：禁止跨 Task 累积 commit
- **P0 必做 / P1 选做**：MVP 阶段只跑 P0（Task 1-4），P1（Task 5）视进度决定
- **GWT 覆盖**：每个 Task 头部必须明确「对应 GWT」场景编号

---

## 4. 上下游引用

- **上游需求**：[./user-story.md](./user-story.md)（GWT 业务级场景）
- **设计输入**：[./tech-design.md](./tech-design.md)（API/数据模型/状态机）
- **Figma 设计交付物**：[./user-story.md](./user-story.md#12-figma-链接) §12-14（UI 状态截图 + 页面级设计决策 + 设计评审记录）

---

## 5. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 升级为标准 tasks 模板（双重角色：TDD 任务清单 + 测试计划）；5 个 task 完整示范 |
| v0.1 | 2026-07-30 | QA | 初版占位（纯测试用例表格） |
