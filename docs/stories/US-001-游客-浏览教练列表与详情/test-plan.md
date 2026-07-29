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

每个 Task 严格遵循 **RED → GREEN → COMMIT** 循环，不允许跳步。

---

## 1. Task 概览

| Task | 标题 | 优先级 | 对应 GWT 场景 |
|------|------|--------|--------------|
| 1 | 创建 Coach Repository（列表查询 + 排序 + 分页） | P0 | [§6.1 场景 1：正常浏览](./user-story.md#61-场景-1正常浏览) |
| 2 | 创建 Coach Repository（详情查询 + 状态过滤） | P0 | [§6.3 场景 3](./user-story.md#63-场景-3教练请假中)、[§6.4 场景 4](./user-story.md#64-场景-4教练申请中不出现) |
| 3 | GET /coaches 列表 API 端点 | P0 | §6.1、[§6.2 场景 2：空状态](./user-story.md#62-场景-2空状态) |
| 4 | GET /coaches/:id 详情 API | P0 | §6.3、§6.4 |
| 5 | 微信小程序列表页 + 缓存 | P1 | §6.1 |

---

## 2. 实施任务

### Task 1: 创建 Coach Repository（列表查询 + 排序 + 分页）[P0]

**Files:**
- Create: `backend/src/repositories/coach.ts`
- Test: `backend/tests/repositories/coach.test.ts`

**对应 GWT**：[user-story.md §6.1 场景 1：正常浏览](./user-story.md#61-场景-1正常浏览)

- [ ] **Step 1: 写失败测试**

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
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- coach.test.ts`
Expected: FAIL with `Cannot find module '../../src/repositories/coach'`

- [ ] **Step 3: 写最小实现**

```typescript
// backend/src/repositories/coach.ts
export interface ListParams { page: number; size: number; }
export interface CoachListItem { id: number; name: string; rating: number; }

export class CoachRepository {
  async findPublicList(params: ListParams): Promise<{ items: CoachListItem[]; total: number }> {
    const offset = (params.page - 1) * params.size;
    return {
      items: await db('coach')
        .where({ status: 1 })              // 仅在职（status=1），过滤申请中（status=0）
        .orderBy('rating', 'desc')         // 评分降序
        .limit(params.size)
        .offset(offset)
        .select('id', 'name', 'rating'),
      total: await db('coach').where({ status: 1 }).count('id as count').first(),
    };
  }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- coach.test.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/repositories/coach.ts backend/tests/repositories/coach.test.ts
git commit -m "feat(coach): add public list query with rating sort and status filter"
```

---

### Task 2: 创建 Coach Repository（详情查询 + 状态过滤）[P0]

**Files:**
- Modify: `backend/src/repositories/coach.ts`
- Modify: `backend/tests/repositories/coach.test.ts`

**对应 GWT**：[user-story.md §6.3 场景 3：教练请假中](./user-story.md#63-场景-3教练请假中)、[§6.4 场景 4：教练申请中不出现](./user-story.md#64-场景-4教练申请中不出现)

- [ ] **Step 1: 写失败测试**

```typescript
// 追加到 backend/tests/repositories/coach.test.ts
describe('CoachRepository.findById', () => {
  it('returns full coach info when coach is active (空闲中)', async () => {
    const repo = new CoachRepository();
    const coach = await repo.findById(1);  // 假设 id=1 为在职空闲教练

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

  it('throws COACH_NOT_FOUND for non-existent id', async () => {
    const repo = new CoachRepository();
    await expect(repo.findById(99999)).rejects.toThrow('COACH_NOT_FOUND');
  });

  it('throws COACH_NOT_FOUND for pending (status=0) coach', async () => {
    const repo = new CoachRepository();
    await expect(repo.findById(2)).rejects.toThrow('COACH_NOT_FOUND');  // id=2 状态=申请中
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- coach.test.ts`
Expected: FAIL with `repo.findById is not a function`

- [ ] **Step 3: 写最小实现**

```typescript
// 修改 backend/src/repositories/coach.ts，添加 findById 方法
export class CoachRepository {
  // ... 已有 findPublicList ...

  async findById(id: number): Promise<CoachDetail> {
    const coach = await db('coach')
      .leftJoin('coach_certificate', 'coach.id', 'coach_certificate.coach_id')
      .where('coach.id', id)
      .whereIn('coach.status', [1, 2, 3, 4])  // 在职/休息/上课/请假均可查；申请中（0）不可见
      .select(
        'coach.id', 'coach.name', 'coach.years_of_teaching', 'coach.rating',
        'coach.real_time_status', 'coach_certificate.name as certificate_name'
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
Expected: PASS（3 个新测试全过，共 4 个）

- [ ] **Step 5: Commit**

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

**对应 GWT**：[user-story.md §6.1 场景 1：正常浏览](./user-story.md#61-场景-1正常浏览)、[§6.2 场景 2：空状态](./user-story.md#62-场景-2空状态)

- [ ] **Step 1: 写失败测试**

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

  it('returns 200 with empty array when no active coaches (空状态)', async () => {
    // 测试前清空 coach 表或使用 mock DB
    const res = await request(app).get('/coaches?page=1&size=10');
    expect(res.status).toBe(200);
    expect(res.body.items).toEqual([]);
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- coach.test.ts`
Expected: FAIL with `Cannot find module '../../src/app'`

- [ ] **Step 3: 写最小实现**

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
Expected: PASS（2 个测试全过）

- [ ] **Step 5: Commit**

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

**对应 GWT**：[user-story.md §6.3 场景 3：教练请假中](./user-story.md#63-场景-3教练请假中)、[§6.4 场景 4：教练申请中不出现](./user-story.md#64-场景-4教练申请中不出现)

- [ ] **Step 1: 写失败测试**

```typescript
// 追加到 backend/tests/controllers/coach.test.ts
describe('GET /coaches/:id', () => {
  it('returns 200 with full coach info (空闲中)', async () => {
    const res = await request(app).get('/coaches/1');
    expect(res.status).toBe(200);
    expect(res.body).toMatchObject({
      id: 1,
      realTimeStatus: '空闲中',
    });
  });

  it('returns 200 with realTimeStatus=请假中 (场景 3)', async () => {
    const res = await request(app).get('/coaches/3');  // 假设 id=3 状态为请假中
    expect(res.status).toBe(200);
    expect(res.body.realTimeStatus).toBe('请假中');
  });

  it('returns 404 with COACH_NOT_FOUND for pending coach (场景 4)', async () => {
    const res = await request(app).get('/coaches/2');  // 假设 id=2 状态为申请中
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

- [ ] **Step 3: 写最小实现**

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
Expected: PASS（4 个新测试全过，共 6 个）

- [ ] **Step 5: Commit**

```bash
git add backend/src/controllers/coach.ts backend/src/routes/coach.ts backend/tests/controllers/coach.test.ts
git commit -m "feat(api): add GET /coaches/:id detail endpoint with 404 handling"
```

---

### Task 5: 微信小程序列表页 + 缓存 [P1]

**Files:**
- Create: `miniapp-user/src/pages/coaches/index.tsx`
- Create: `miniapp-user/src/pages/coaches/index.test.tsx`

**对应 GWT**：[user-story.md §6.1 场景 1：正常浏览](./user-story.md#61-场景-1正常浏览)

- [ ] **Step 1: 写失败测试**

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
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- coaches/index.test.tsx`
Expected: FAIL with `Cannot find module './index'`

- [ ] **Step 3: 写最小实现**

```tsx
// miniapp-user/src/pages/coaches/index.tsx
import { useEffect, useState } from 'react';
import { View, Text } from '@tarojs/components';
import Taro from '@tarojs/taro';

interface Coach { id: number; name: string; rating: number; realTimeStatus: string; }

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
          <Text>{c.realTimeStatus}</Text>
        </View>
      ))}
    </View>
  );
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- coaches/index.test.tsx`
Expected: PASS（2 个测试全过）

- [ ] **Step 5: Commit**

```bash
git add miniapp-user/src/pages/coaches/
git commit -m "feat(miniapp): add coaches list page with storage cache"
```

---

## 3. 任务执行纪律

- **严格顺序**：Task 1 → Task 2 → Task 3 → Task 4 → Task 5
- **每步必须可见**：Step 1（写测试）→ Step 2（看失败）→ Step 3（写实现）→ Step 4（看通过）→ Step 5（commit）
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
