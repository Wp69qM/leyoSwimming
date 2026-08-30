# US-003 实施任务清单（Tasks）

> **US 关联**：[user-story.md](./user-story.md)（US-003 游客查看预约释放倒计时）
> **文档角色**：TDD 任务清单 + 测试计划（双重角色）
> **状态**：🔲 待开发执行
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
| 1 | ReleaseRule Repository（查询释放规则） | P0 | [§6.1 场景 1](./user-story.md#61-场景-1释放前-24-小时显示倒计时正常路径)、[§6.4 场景 4](./user-story.md#64-场景-4释放规则未配置时不显示倒计时异常路径) |
| 2 | 倒计时计算逻辑（根据当前时间 + 规则计算） | P0 | [§6.1 场景 1](./user-story.md#61-场景-1释放前-24-小时显示倒计时正常路径)、[§6.2 场景 2](./user-story.md#62-场景-2释放当天显示今日-1000-开放下周预约正常路径)、[§6.3 场景 3](./user-story.md#63-场景-3非释放时段不显示倒计时异常路径) |
| 3 | GET /release-countdown API 端点 | P0 | §6.1、§6.2、§6.3、§6.4 |
| 4 | 小程序首页倒计时组件 | P1 | §6.1、§6.2、§6.3 |

---

## 2. 实施任务

### Task 1: ReleaseRule Repository（查询释放规则）[P0]

**Files:**
- Create: `backend/src/repositories/release-rule.ts`
- Test: `backend/tests/repositories/release-rule.test.ts`

**对应 GWT**：[user-story.md §6.1 场景 1](./user-story.md#61-场景-1释放前-24-小时显示倒计时正常路径)、[§6.4 场景 4](./user-story.md#64-场景-4释放规则未配置时不显示倒计时异常路径)

- [ ] **Step 1: RED — 写失败测试**

```typescript
// backend/tests/repositories/release-rule.test.ts
import { ReleaseRuleRepository } from '../../src/repositories/release-rule';

describe('ReleaseRuleRepository.findActive', () => {
  it('returns the active release rule when status=1', async () => {
    const repo = new ReleaseRuleRepository();
    const rule = await repo.findActive();

    expect(rule).toMatchObject({
      id: expect.any(Number),
      releaseDayOfWeek: expect.any(Number),    // 1-7
      releaseTime: expect.stringMatching(/^\d{2}:\d{2}:\d{2}$/),  // HH:MM:SS
      status: 1,
    });
  });

  it('returns null when no active rule exists (规则未配置)', async () => {
    // 测试前清空 release_rule 表或 mock 返回空
    const repo = new ReleaseRuleRepository();
    const rule = await repo.findActive();

    expect(rule).toBeNull();
  });

  it('returns rule with manual_override_at field when set', async () => {
    const repo = new ReleaseRuleRepository();
    const rule = await repo.findActive();

    // manual_override_at 可为 null，但字段必须存在
    expect(rule).toHaveProperty('manualOverrideAt');
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- release-rule.test.ts`
Expected: FAIL with `Cannot find module '../../src/repositories/release-rule'`

- [ ] **Step 3: GREEN — 写最小实现**

```typescript
// backend/src/repositories/release-rule.ts
import { db } from '../db';

export interface ReleaseRule {
  id: number;
  releaseDayOfWeek: number;       // 1=周一 ... 7=周日
  releaseTime: string;             // HH:MM:SS
  releaseScope: string;            // next_week | next_7_days
  status: number;                  // 0=禁用, 1=启用
  manualOverrideAt: Date | null;   // 手动覆盖释放时间
  manualOverrideNote: string | null;
  updatedAt: Date;
}

export class ReleaseRuleRepository {
  /**
   * 查询当前启用的释放规则（仅 1 条）
   * 对应 GWT §6.1 / §6.4
   */
  async findActive(): Promise<ReleaseRule | null> {
    const rule = await db('release_rule')
      .where({ status: 1 })
      .select(
        'id',
        'release_day_of_week as releaseDayOfWeek',
        'release_time as releaseTime',
        'release_scope as releaseScope',
        'status',
        'manual_override_at as manualOverrideAt',
        'manual_override_note as manualOverrideNote',
        'updated_at as updatedAt'
      )
      .first();

    return rule || null;
  }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- release-rule.test.ts`
Expected: PASS（3 个测试全过）

- [ ] **Step 5: REFACTOR — 优化代码**

- 检查字段别名映射是否统一，避免多处拼写不一致。
- 若存在重复查询条件，提取私有方法或复用已有的 query builder。
- 确认 `findActive` 语义明确（仅返回 `status=1` 的规则）。

- [ ] **Step 6: COMMIT**

```bash
git add backend/src/repositories/release-rule.ts backend/tests/repositories/release-rule.test.ts
git commit -m "feat(release): add ReleaseRuleRepository.findActive for active rule query"
```

---

### Task 2: 倒计时计算逻辑（根据当前时间 + 规则计算）[P0]

**Files:**
- Create: `backend/src/services/release-countdown-service.ts`
- Test: `backend/tests/services/release-countdown-service.test.ts`

**对应 GWT**：[user-story.md §6.1 场景 1](./user-story.md#61-场景-1释放前-24-小时显示倒计时正常路径)、[§6.2 场景 2](./user-story.md#62-场景-2释放当天显示今日-1000-开放下周预约正常路径)、[§6.3 场景 3](./user-story.md#63-场景-3非释放时段不显示倒计时异常路径)

- [ ] **Step 1: RED — 写失败测试**

```typescript
// backend/tests/services/release-countdown-service.test.ts
import { ReleaseCountdownService } from '../../src/services/release-countdown-service';
import { ReleaseRule } from '../../src/repositories/release-rule';

const mockRule: ReleaseRule = {
  id: 1,
  releaseDayOfWeek: 3,       // 周三
  releaseTime: '10:00:00',
  releaseScope: 'next_week',
  status: 1,
  manualOverrideAt: null,
  manualOverrideNote: null,
  updatedAt: new Date(),
};

describe('ReleaseCountdownService.calculate', () => {
  const service = new ReleaseCountdownService();

  it('§6.1 场景1: 释放前 24 小时内显示倒计时（周二 12:00 → 周三 10:00）', () => {
    // 周二 12:00，距周三 10:00 还有 22 小时
    const serverNow = new Date('2026-08-04T12:00:00+08:00');  // 2026-08-04 是周二
    const result = service.calculate(mockRule, serverNow);

    expect(result.show).toBe(true);
    expect(result.isReleaseDay).toBe(false);
    expect(result.message).toBe('距离下周预约开放还有 22 小时');
    expect(result.remainingSeconds).toBe(22 * 3600);  // 22 小时
  });

  it('§6.2 场景2: 释放当天显示"今日 10:00 开放下周预约"（周三 08:30）', () => {
    const serverNow = new Date('2026-08-05T08:30:00+08:00');  // 2026-08-05 是周三
    const result = service.calculate(mockRule, serverNow);

    expect(result.show).toBe(true);
    expect(result.isReleaseDay).toBe(true);
    expect(result.message).toBe('今日 10:00 开放下周预约');
    expect(result.remainingSeconds).toBe(1.5 * 3600);  // 1.5 小时 = 5400s
  });

  it('§6.3 场景3: 非释放时段不显示（周三 10:01，刚过释放时刻）', () => {
    const serverNow = new Date('2026-08-05T10:01:00+08:00');  // 周三 10:01
    const result = service.calculate(mockRule, serverNow);

    expect(result.show).toBe(false);
    expect(result.message).toBeNull();
    expect(result.remainingSeconds).toBeNull();
  });

  it('§6.3 场景3: 非释放时段不显示（周四，距下次释放 > 24h）', () => {
    const serverNow = new Date('2026-08-06T10:00:00+08:00');  // 周四
    const result = service.calculate(mockRule, serverNow);

    expect(result.show).toBe(false);
  });

  it('§6.4 场景4: 规则为 null 时返回 show=false', () => {
    const serverNow = new Date('2026-08-04T12:00:00+08:00');
    const result = service.calculate(null, serverNow);

    expect(result.show).toBe(false);
  });

  it('边界: manual_override_at 未过期时优先使用', () => {
    const overrideRule = { ...mockRule, manualOverrideAt: new Date('2026-08-06T10:00:00+08:00') };
    const serverNow = new Date('2026-08-05T12:00:00+08:00');  // 距覆盖时间 22 小时
    const result = service.calculate(overrideRule, serverNow);

    expect(result.show).toBe(true);
    expect(result.releaseAt).toBe(overrideRule.manualOverrideAt);
  });

  it('边界: manual_override_at 已过期时忽略，按常规计算', () => {
    const overrideRule = { ...mockRule, manualOverrideAt: new Date('2026-08-03T10:00:00+08:00') };
    const serverNow = new Date('2026-08-04T12:00:00+08:00');  // 覆盖时间已过
    const result = service.calculate(overrideRule, serverNow);

    expect(result.show).toBe(true);
    expect(result.releaseAt).not.toBe(overrideRule.manualOverrideAt);
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- release-countdown-service.test.ts`
Expected: FAIL with `Cannot find module '../../src/services/release-countdown-service'`

- [ ] **Step 3: GREEN — 写最小实现**

```typescript
// backend/src/services/release-countdown-service.ts
import { ReleaseRule } from '../repositories/release-rule';

export interface CountdownResult {
  show: boolean;
  releaseAt: Date | null;
  message: string | null;
  remainingSeconds: number | null;
  isReleaseDay: boolean;
}

export class ReleaseCountdownService {
  /**
   * 根据释放规则和当前时间计算倒计时状态
   * 对应 GWT §6.1 / §6.2 / §6.3 / §6.4
   */
  calculate(rule: ReleaseRule | null, serverNow: Date): CountdownResult {
    // §6.4 规则未配置
    if (!rule) {
      return { show: false, releaseAt: null, message: null, remainingSeconds: null, isReleaseDay: false };
    }

    const releaseAt = this.calculateNextReleaseAt(rule, serverNow);
    const diffMs = releaseAt.getTime() - serverNow.getTime();
    const diffHours = diffMs / (1000 * 60 * 60);

    // 已过释放时刻
    if (diffMs <= 0) {
      return { show: false, releaseAt: null, message: null, remainingSeconds: null, isReleaseDay: false };
    }

    // 超过 24 小时窗口
    if (diffHours > 24) {
      return { show: false, releaseAt: null, message: null, remainingSeconds: null, isReleaseDay: false };
    }

    const isReleaseDay = this.isSameDay(releaseAt, serverNow);
    const remainingSeconds = Math.floor(diffMs / 1000);
    const message = isReleaseDay
      ? `今日 ${rule.releaseTime.substring(0, 5)} 开放下周预约`
      : `距离下周预约开放还有 ${Math.ceil(diffHours)} 小时`;

    return { show: true, releaseAt, message, remainingSeconds, isReleaseDay };
  }

  /**
   * 计算下次释放时刻
   */
  private calculateNextReleaseAt(rule: ReleaseRule, serverNow: Date): Date {
    // 1. 优先使用未过期的 manual_override_at
    if (rule.manualOverrideAt && rule.manualOverrideAt > serverNow) {
      return rule.manualOverrideAt;
    }

    // 2. 计算本周常规释放时刻
    const thisWeekRelease = this.getThisWeekDateTime(rule.releaseDayOfWeek, rule.releaseTime, serverNow);

    // 3. 本周未到则返回本周
    if (thisWeekRelease > serverNow) {
      return thisWeekRelease;
    }

    // 4. 否则返回下周
    const nextWeekRelease = new Date(thisWeekRelease);
    nextWeekRelease.setDate(nextWeekRelease.getDate() + 7);
    return nextWeekRelease;
  }

  /**
   * 获取本周指定星期几 + 时间的 Date 对象
   */
  private getThisWeekDateTime(dayOfWeek: number, time: string, serverNow: Date): Date {
    const [hours, minutes, seconds] = time.split(':').map(Number);
    const result = new Date(serverNow);
    const currentDay = result.getDay() === 0 ? 7 : result.getDay();  // 周日=7

    let diff = dayOfWeek - currentDay;
    result.setDate(result.getDate() + diff);
    result.setHours(hours, minutes, seconds, 0);
    return result;
  }

  private isSameDay(a: Date, b: Date): boolean {
    return a.getFullYear() === b.getFullYear()
      && a.getMonth() === b.getMonth()
      && a.getDate() === b.getDate();
  }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- release-countdown-service.test.ts`
Expected: PASS（7 个测试全过）

- [ ] **Step 5: REFACTOR — 优化代码**

- 提取时间格式化/日期比较等纯函数到独立 util，便于单测复用。
- 将 24h 窗口判断、释放当天判断、文案生成分解为独立私有方法，提高可读性。
- 消除 `calculate` 中重复返回的隐藏对象，必要时引入常量或工厂函数。

- [ ] **Step 6: COMMIT**

```bash
git add backend/src/services/release-countdown-service.ts backend/tests/services/release-countdown-service.test.ts
git commit -m "feat(release): add countdown calculation service with 24h window logic"
```

---

### Task 3: GET /release-countdown API 端点 [P0]

**Files:**
- Create: `backend/src/controllers/release-countdown.ts`
- Create: `backend/src/routes/release-countdown.ts`
- Create: `backend/tests/controllers/release-countdown.test.ts`

**对应 GWT**：[user-story.md §6.1](./user-story.md#61-场景-1释放前-24-小时显示倒计时正常路径)、[§6.2](./user-story.md#62-场景-2释放当天显示今日-1000-开放下周预约正常路径)、[§6.3](./user-story.md#63-场景-3非释放时段不显示倒计时异常路径)、[§6.4](./user-story.md#64-场景-4释放规则未配置时不显示倒计时异常路径)

- [ ] **Step 1: RED — 写失败测试**

```typescript
// backend/tests/controllers/release-countdown.test.ts
import request from 'supertest';
import { app } from '../../src/app';

describe('GET /api/v1/release-countdown', () => {
  it('§6.1 场景1: 释放前 24h 内返回 show=true + "距离下周预约开放还有 XX 小时" 文案', async () => {
    const res = await request(app).get('/api/v1/release-countdown');
    expect(res.status).toBe(200);
    expect(res.body).toMatchObject({
      show: expect.any(Boolean),
      serverNow: expect.any(String),
    });
    if (res.body.show && !res.body.isReleaseDay) {
      expect(res.body.message).toMatch(/^距离下周预约开放还有 \d+ 小时$/);
    }
  });

  it('§6.2 场景2: 释放当天返回 isReleaseDay=true + "今日 10:00 开放下周预约" 文案', async () => {
    // 需 mock 当前时间为释放当天，或 mock service 返回值
    const res = await request(app).get('/api/v1/release-countdown');
    expect(res.status).toBe(200);
    if (res.body.isReleaseDay) {
      expect(res.body.message).toBe('今日 10:00 开放下周预约');
    }
  });

  it('§6.3 场景3: 非释放时段返回 show=false', async () => {
    const res = await request(app).get('/api/v1/release-countdown');
    expect(res.status).toBe(200);
    // 当处于非窗口期时 show=false
    expect(res.body).toHaveProperty('show');
  });

  it('§6.4 场景4: 规则未配置返回 show=false', async () => {
    // mock ReleaseRuleRepository.findActive 返回 null
    const res = await request(app).get('/api/v1/release-countdown');
    expect(res.status).toBe(200);
    expect(res.body.show).toBe(false);
  });

  it('响应包含 serverNow 字段用于客户端校准', async () => {
    const res = await request(app).get('/api/v1/release-countdown');
    expect(res.body.serverNow).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/);
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- release-countdown.test.ts`
Expected: FAIL with `Cannot find module '../../src/app'` 或 404

- [ ] **Step 3: GREEN — 写最小实现**

```typescript
// backend/src/controllers/release-countdown.ts
import { ReleaseRuleRepository } from '../repositories/release-rule';
import { ReleaseCountdownService } from '../services/release-countdown-service';

const ruleRepo = new ReleaseRuleRepository();
const countdownService = new ReleaseCountdownService();

/**
 * GET /api/v1/release-countdown
 * 游客查看预约释放倒计时（无需登录）
 * 对应 GWT §6.1-§6.4
 */
export async function getReleaseCountdown(ctx) {
  // 使用 DB 时间，避免应用服务器时间漂移
  const serverNow = await db.raw('SELECT NOW() as now').first().then(r => new Date(r.now));

  // 查缓存
  const cached = await redis.get('release:countdown');
  if (cached) {
    ctx.body = JSON.parse(cached);
    return;
  }

  // 查规则
  const rule = await ruleRepo.findActive();
  if (!rule) {
    logger.warn('release_rule not configured or disabled');
    const result = { show: false, releaseAt: null, serverNow: serverNow.toISOString(), message: null, remainingSeconds: null, isReleaseDay: false };
    await redis.set('release:countdown', JSON.stringify(result), 'EX', 10);
    ctx.body = result;
    return;
  }

  // 计算倒计时
  const result = countdownService.calculate(rule, serverNow);
  const response = {
    ...result,
    releaseAt: result.releaseAt ? result.releaseAt.toISOString() : null,
    serverNow: serverNow.toISOString(),
  };

  // 缓存 10s
  await redis.set('release:countdown', JSON.stringify(response), 'EX', 10);
  ctx.body = response;
}

// backend/src/routes/release-countdown.ts
import Router from 'koa-router';
import { getReleaseCountdown } from '../controllers/release-countdown';

const router = new Router({ prefix: '/api/v1' });
router.get('/release-countdown', getReleaseCountdown);
export default router;
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- release-countdown.test.ts`
Expected: PASS（5 个测试全过）

- [ ] **Step 5: REFACTOR — 优化代码**

- 将 `db.raw('SELECT NOW()')` 和 Redis 缓存逻辑抽取到可复用的 server-time / cache 工具中。
- 检查 `logger.warn` 调用是否统一，确保规则缺失/禁用均记录 WARN 日志。
- 验证响应结构序列化/反序列化不会丢失 `null` 字段语义。

- [ ] **Step 6: COMMIT**

```bash
git add backend/src/controllers/release-countdown.ts backend/src/routes/release-countdown.ts backend/tests/controllers/release-countdown.test.ts
git commit -m "feat(api): add GET /release-countdown endpoint with 10s redis cache"
```

---

### Task 4: 小程序首页倒计时组件 [P1]

**Files:**
- Create: `miniapp-user/src/components/release-countdown/index.tsx`
- Create: `miniapp-user/src/components/release-countdown/index.test.tsx`

**对应 GWT**：[user-story.md §6.1 场景 1](./user-story.md#61-场景-1释放前-24-小时显示倒计时正常路径)、[§6.2 场景 2](./user-story.md#62-场景-2释放当天显示今日-1000-开放下周预约正常路径)、[§6.3 场景 3](./user-story.md#63-场景-3非释放时段不显示倒计时异常路径)

- [ ] **Step 1: RED — 写失败测试**

```typescript
// miniapp-user/src/components/release-countdown/index.test.tsx
import { render } from '@testing-library/react';
import ReleaseCountdown from './index';

describe('ReleaseCountdown', () => {
  it('§6.1 场景1: show=true 且非释放当天时显示"距离下周预约开放还有 XX 小时"', async () => {
    jest.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      json: async () => ({
        show: true,
        isReleaseDay: false,
        message: '距离下周预约开放还有 22 小时',
        remainingSeconds: 79200,
        serverNow: '2026-08-04T12:00:00+08:00',
        releaseAt: '2026-08-05T10:00:00+08:00',
      }),
    } as any);

    const { findByText } = render(<ReleaseCountdown />);
    expect(await findByText('距离下周预约开放还有 22 小时')).toBeInTheDocument();
  });

  it('§6.2 场景2: show=true 且 isReleaseDay=true 时显示"今日 10:00 开放下周预约"', async () => {
    jest.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      json: async () => ({
        show: true,
        isReleaseDay: true,
        message: '今日 10:00 开放下周预约',
        remainingSeconds: 5400,
        serverNow: '2026-08-05T08:30:00+08:00',
        releaseAt: '2026-08-05T10:00:00+08:00',
      }),
    } as any);

    const { findByText } = render(<ReleaseCountdown />);
    expect(await findByText('今日 10:00 开放下周预约')).toBeInTheDocument();
  });

  it('§6.3 场景3: show=false 时组件不渲染', async () => {
    jest.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      json: async () => ({
        show: false,
        serverNow: '2026-08-05T10:01:00+08:00',
      }),
    } as any);

    const { container } = render(<ReleaseCountdown />);
    // 等待异步加载完成后组件应为空
    await new Promise(r => setTimeout(r, 100));
    expect(container.firstChild).toBeNull();
  });

  it('每 10s 拉取一次 API 校准', async () => {
    const fetchSpy = jest.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      json: async () => ({ show: true, isReleaseDay: false, message: '距离下周预约开放还有 22 小时', remainingSeconds: 79200, serverNow: '2026-08-04T12:00:00+08:00' }),
    } as any);

    render(<ReleaseCountdown />);
    // 快进 10s（jest fake timers）
    jest.advanceTimersByTime(10000);
    expect(fetchSpy).toHaveBeenCalledTimes(2);  // 初始 + 10s 后
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- release-countdown/index.test.tsx`
Expected: FAIL with `Cannot find module './index'`

- [ ] **Step 3: GREEN — 写最小实现**

```tsx
// miniapp-user/src/components/release-countdown/index.tsx
import { useEffect, useState, useRef } from 'react';
import { View, Text } from '@tarojs/components';
import Taro from '@tarojs/taro';

interface CountdownData {
  show: boolean;
  releaseAt: string | null;
  serverNow: string;
  message: string | null;
  remainingSeconds: number | null;
  isReleaseDay: boolean;
}

/**
 * 预约释放倒计时组件
 * 对应 GWT §6.1 / §6.2 / §6.3
 */
export default function ReleaseCountdown() {
  const [data, setData] = useState<CountdownData | null>(null);
  const [localRemaining, setLocalRemaining] = useState<number | null>(null);
  const fetchTimerRef = useRef<NodeJS.Timeout>();
  const tickTimerRef = useRef<NodeJS.Timeout>();

  useEffect(() => {
    fetchCountdown();

    // 每 10s 拉一次 API 校准（user-story.md §14.2）
    fetchTimerRef.current = setInterval(fetchCountdown, 10_000);

    return () => {
      if (fetchTimerRef.current) clearInterval(fetchTimerRef.current);
      if (tickTimerRef.current) clearInterval(tickTimerRef.current);
    };
  }, []);

  useEffect(() => {
    // 每秒本地递减
    if (data?.show && data.remainingSeconds != null) {
      setLocalRemaining(data.remainingSeconds);
      tickTimerRef.current = setInterval(() => {
        setLocalRemaining(prev => (prev != null && prev > 0 ? prev - 1 : 0));
      }, 1000);
    } else {
      setLocalRemaining(null);
    }

    return () => {
      if (tickTimerRef.current) clearInterval(tickTimerRef.current);
    };
  }, [data]);

  async function fetchCountdown() {
    try {
      const res = await Taro.request({
        url: '/api/v1/release-countdown',
        method: 'GET',
      });
      setData(res.data);
    } catch (err) {
      // 网络异常：组件隐藏，不影响首页其他模块
      setData(null);
    }
  }

  // §6.3 场景3：show=false 时组件不渲染
  if (!data || !data.show) {
    return null;
  }

  return (
    <View className="release-countdown" style={{ background: '#FF6B35', height: '56px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <Text style={{ color: '#fff', fontSize: '14px' }}>
        {data.message}
      </Text>
    </View>
  );
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- release-countdown/index.test.tsx`
Expected: PASS（4 个测试全过）

- [ ] **Step 5: REFACTOR — 优化代码**

- 将定时器清理逻辑提取为统一 hooks 或工具函数，避免重复 `clearInterval`。
- 检查 `localRemaining` 是否与 `data.message` 文案保持一致（释放当天固定文案不显示动态小时）。
- 将内联样式抽取为组件级 CSS/SCSS 或 Taro 样式变量，便于主题切换。

- [ ] **Step 6: COMMIT**

```bash
git add miniapp-user/src/components/release-countdown/
git commit -m "feat(miniapp): add release countdown component with 10s api calibration"
```

---

## 3. 任务执行纪律

- **严格顺序**：Task 1 → Task 2 → Task 3 → Task 4
- **每步必须可见**：Step 1（RED 写测试）→ Step 2（看失败）→ Step 3（GREEN 写实现）→ Step 4（看通过）→ Step 5（REFACTOR）→ Step 6（COMMIT）
- **不允许 placeholder**：任何 "TBD" / "TODO" / "实现 later" / "类似 Task N" = 立即返工
- **每个 Task 结束 = 1 次 commit**：禁止跨 Task 累积 commit
- **P0 必做 / P1 选做**：MVP 阶段只跑 P0（Task 1-3），P1（Task 4）视进度决定
- **GWT 覆盖**：每个 Task 头部必须明确「对应 GWT」场景编号

---

## 4. 上下游引用

- **上游需求**：[./user-story.md](./user-story.md)（GWT 业务级场景）
- **设计输入**：[./tech-design.md](./tech-design.md)（API/数据模型/计算逻辑/缓存）
- **Figma 设计交付物**：[./user-story.md](./user-story.md#13-figma-链接) §13-15（UI 状态截图 + 页面级设计决策 + 设计评审记录）

---

## 5. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版：4 个 task 完整示范（双重角色：TDD 任务清单 + 测试计划） |
