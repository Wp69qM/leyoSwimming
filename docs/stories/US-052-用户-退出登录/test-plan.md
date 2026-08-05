# US-052 实施任务清单（Tasks）

> **US 关联**：[user-story.md](./user-story.md)（US-052 用户退出登录）
> **文档角色**：TDD 任务清单 + 测试计划（双重角色）
> **状态**：🔲 待开发执行
> **作者**：开发　|　**最后更新**：2026-08-05

---

## 0. 双重角色说明

本文档承担**双重角色**：
- **TDD 任务清单**：每个 task = 2-5 分钟可执行单元
- **测试计划**：覆盖 [user-story.md](./user-story.md) 中所有 GWT 场景

每个 Task 严格遵循 **RED → GREEN → COMMIT** 循环，不允许跳步。

---

## 1. Task 概览

| Task | 标题 | 优先级 | 对应 GWT 场景 |
|------|------|--------|--------------|
| 1 | 后端退出登录接口 | P0 | [§6.1 场景 1](./user-story.md#61-场景-1用户正常退出登录)、[§6.3 场景 3](./user-story.md#63-场景-3本地-token-已过期或不存在) |
| 2 | 小程序「我的」页面退出登录逻辑 | P0 | [§6.1 场景 1](./user-story.md#61-场景-1用户正常退出登录)、[§6.2 场景 2](./user-story.md#62-场景-2用户取消退出)、[§6.3 场景 3](./user-story.md#63-场景-3本地-token-已过期或不存在) |

---

## 2. 实施任务

### Task 1: 后端退出登录接口 [P0]

**Files:**
- Create: `backend/src/controllers/auth.ts`（新增 `logout` 方法；若 US-004 已创建则复用并扩展）
- Create: `backend/src/routes/auth.ts`（新增 `POST /logout` 路由）
- Create: `backend/src/services/session.ts`（新增 `revoke` 方法；若 US-004 已创建则扩展）
- Test: `backend/tests/controllers/logout.test.ts`

**对应 GWT**：[user-story.md §6.1 场景 1](./user-story.md#61-场景-1用户正常退出登录)、[§6.3 场景 3](./user-story.md#63-场景-3本地-token-已过期或不存在)

- [ ] **Step 1: 写失败测试**

```typescript
// backend/tests/controllers/logout.test.ts
import request from 'supertest';
import { app } from '../../src/app';

describe('POST /api/v1/auth/logout', () => {
  it('场景 1: 正常退出使当前 refresh_token 失效', async () => {
    const accessToken = 'valid_access_token';
    const refreshToken = 'valid_refresh_token';

    const res = await request(app)
      .post('/api/v1/auth/logout')
      .set('Authorization', `Bearer ${accessToken}`)
      .send({ refreshToken });

    expect(res.status).toBe(200);
    expect(res.body.message).toBe('退出登录成功');

    // 验证 refresh_token 已失效
    const refreshRes = await request(app)
      .post('/api/v1/auth/refresh')
      .send({ refreshToken });
    expect(refreshRes.status).toBe(401);
  });

  it('场景 3: access_token 无效时返回 401', async () => {
    const res = await request(app)
      .post('/api/v1/auth/logout')
      .set('Authorization', 'Bearer invalid_token')
      .send({ refreshToken: 'any' });

    expect(res.status).toBe(401);
    expect(res.body.error).toBe('TOKEN_INVALID');
  });

  it('重复调用同一 refresh_token 退出应幂等返回 200', async () => {
    const accessToken = 'valid_access_token';
    const refreshToken = 'valid_refresh_token';

    const first = await request(app)
      .post('/api/v1/auth/logout')
      .set('Authorization', `Bearer ${accessToken}`)
      .send({ refreshToken });

    const second = await request(app)
      .post('/api/v1/auth/logout')
      .set('Authorization', `Bearer ${accessToken}`)
      .send({ refreshToken });

    expect(first.status).toBe(200);
    expect(second.status).toBe(200);
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- logout.test.ts`
Expected: FAIL with `Cannot find module '../../src/app'` 或 404

- [ ] **Step 3: 写最小实现**

```typescript
// backend/src/services/session.ts
export class SessionService {
  // ... create 方法已在 US-004 实现

  async revoke(refreshToken: string) {
    const hash = crypto.createHash('sha256').update(refreshToken).digest('hex');
    await db('user_session')
      .where({ refresh_token_hash: hash })
      .update({ revoked_at: new Date() });
  }
}

// backend/src/controllers/auth.ts
export async function logout(ctx) {
  const authHeader = ctx.request.headers.authorization;
  const accessToken = authHeader?.replace('Bearer ', '');

  if (!accessToken) {
    ctx.status = 401;
    ctx.body = { error: 'TOKEN_INVALID', message: '登录态已失效，请重新登录' };
    return;
  }

  try {
    jwt.verify(accessToken, process.env.JWT_SECRET || 'dev-secret');
  } catch {
    ctx.status = 401;
    ctx.body = { error: 'TOKEN_INVALID', message: '登录态已失效，请重新登录' };
    return;
  }

  const { refreshToken } = ctx.request.body || {};
  if (refreshToken) {
    await sessionService.revoke(refreshToken);
  }

  ctx.body = { message: '退出登录成功' };
}

// backend/src/routes/auth.ts
router.post('/logout', logout);
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- logout.test.ts`
Expected: PASS（3 个测试全过）

- [ ] **Step 5: Commit**

```bash
git add backend/src/controllers/auth.ts backend/src/routes/auth.ts backend/src/services/session.ts backend/tests/controllers/logout.test.ts
git commit -m "feat(auth): add POST /auth/logout endpoint to revoke session"
```

---

### Task 2: 小程序「我的」页面退出登录逻辑 [P0]

**Files:**
- Create: `miniapp-user/src/pages/mine/index.tsx`（若已存在则扩展退出登录按钮与弹窗）
- Create: `miniapp-user/src/utils/auth.ts`（新增 `clearToken` 方法）
- Test: `miniapp-user/src/pages/mine/index.test.tsx`

**对应 GWT**：[user-story.md §6.1 场景 1](./user-story.md#61-场景-1用户正常退出登录)、[§6.2 场景 2](./user-story.md#62-场景-2用户取消退出)、[§6.3 场景 3](./user-story.md#63-场景-3本地-token-已过期或不存在)

- [ ] **Step 1: 写失败测试**

```typescript
// miniapp-user/src/pages/mine/index.test.tsx
import { render, fireEvent, waitFor } from '@testing-library/react';
import Taro from '@tarojs/taro';
import MinePage from './index';

jest.mock('@tarojs/taro');

describe('MinePage logout', () => {
  beforeEach(() => {
    Taro.getStorageSync = jest.fn((key: string) => {
      if (key === 'access_token') return 'token_xxx';
      return null;
    });
    Taro.removeStorageSync = jest.fn();
  });

  it('场景 1: 确认退出后清除 token 并刷新「我的」页面为未登录态', async () => {
    (Taro.request as jest.Mock).mockResolvedValue({
      statusCode: 200,
      data: { message: '退出登录成功' },
    });
    (Taro.showModal as jest.Mock).mockResolvedValue({ confirm: true });

    const { getByText, queryByText } = render(<MinePage />);
    fireEvent.click(getByText('退出登录'));

    await waitFor(() => {
      expect(Taro.request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: expect.stringContaining('/api/v1/auth/logout'),
          method: 'POST',
        })
      );
      expect(Taro.removeStorageSync).toHaveBeenCalledWith('access_token');
      expect(Taro.removeStorageSync).toHaveBeenCalledWith('refresh_token');
      // 页面不跳转，仅刷新为未登录态：原退出登录按钮消失，出现"请登录"入口
      expect(Taro.navigateTo).not.toHaveBeenCalled();
      expect(queryByText('退出登录')).toBeNull();
      expect(getByText('请登录')).toBeDefined();
    });
  });

  it('场景 2: 取消退出后保持登录态', async () => {
    (Taro.showModal as jest.Mock).mockResolvedValue({ confirm: false });

    const { getByText } = render(<MinePage />);
    fireEvent.click(getByText('退出登录'));

    await waitFor(() => {
      expect(Taro.request).not.toHaveBeenCalled();
      expect(Taro.removeStorageSync).not.toHaveBeenCalled();
      expect(Taro.navigateTo).not.toHaveBeenCalled();
    });
  });

  it('场景 3: 本地无 token 时直接刷新「我的」页面为未登录态', async () => {
    (Taro.getStorageSync as jest.Mock).mockReturnValue(null);
    (Taro.showModal as jest.Mock).mockResolvedValue({ confirm: true });

    const { getByText, queryByText } = render(<MinePage />);
    fireEvent.click(getByText('退出登录'));

    await waitFor(() => {
      expect(Taro.request).not.toHaveBeenCalled();
      expect(Taro.removeStorageSync).toHaveBeenCalledWith('access_token');
      // 页面不跳转，仅刷新为未登录态
      expect(Taro.navigateTo).not.toHaveBeenCalled();
      expect(queryByText('退出登录')).toBeNull();
      expect(getByText('请登录')).toBeDefined();
    });
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- mine/index.test.tsx`
Expected: FAIL with `Cannot find module './index'`

- [ ] **Step 3: 写最小实现**

```tsx
// miniapp-user/src/utils/auth.ts
export function clearToken() {
  Taro.removeStorageSync('access_token');
  Taro.removeStorageSync('refresh_token');
  Taro.removeStorageSync('expires_in');
  Taro.removeStorageSync('token_expire_at');
}

// miniapp-user/src/pages/mine/index.tsx
import { useState } from 'react';
import { View, Button } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { clearToken } from '../../utils/auth';

export default function MinePage() {
  const [loading, setLoading] = useState(false);

  async function handleLogout() {
    if (loading) return;

    const { confirm } = await Taro.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
    });

    if (!confirm) return;

    setLoading(true);
    const accessToken = Taro.getStorageSync('access_token');

    if (accessToken) {
      try {
        await Taro.request({
          url: `${API_BASE}/api/v1/auth/logout`,
          method: 'POST',
          header: { Authorization: `Bearer ${accessToken}` },
          data: {
            refreshToken: Taro.getStorageSync('refresh_token'),
          },
        });
      } catch {
        // 网络异常仍继续清除本地 token
      }
    }

    clearToken();
    setLoading(false);
    // 退出后不跳转登录页，刷新当前页面为未登录态
    setIsLoggedIn(false);
  }

  return (
    <View>
      {/* 其他「我的」页面内容 */}
      <Button
        onClick={handleLogout}
        loading={loading}
        disabled={loading}
      >
        {loading ? '退出中...' : '退出登录'}
      </Button>
    </View>
  );
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- mine/index.test.tsx`
Expected: PASS（3 个测试全过）

- [ ] **Step 5: Commit**

```bash
git add miniapp-user/src/pages/mine/index.tsx miniapp-user/src/utils/auth.ts miniapp-user/src/pages/mine/index.test.tsx
git commit -m "feat(miniapp): add logout button on mine page with token cleanup"
```

---

## 3. 任务执行纪律

- **严格顺序**：Task 1 → Task 2
- **每步必须可见**：Step 1（写测试）→ Step 2（看失败）→ Step 3（写实现）→ Step 4（看通过）→ Step 5（commit）
- **不允许 placeholder**：任何 "TBD" / "TODO" / "实现 later" / "类似 Task N" = 立即返工
- **每个 Task 结束 = 1 次 commit**：禁止跨 Task 累积 commit
- **P0 必做**：MVP 阶段两个 Task 均必做
- **GWT 覆盖**：每个 Task 头部必须明确「对应 GWT」场景编号

---

## 4. GWT 场景覆盖矩阵

| GWT 场景 | Task 1 | Task 2 |
|---------|--------|--------|
| §6.1 正常退出 | ✅ | ✅ |
| §6.2 取消退出 | — | ✅ |
| §6.3 本地 token 已过期/不存在 | ✅ | ✅ |

---

## 5. 上下游引用

- **上游需求**：[./user-story.md](./user-story.md)
- **设计输入**：[./tech-design.md](./tech-design.md)
- **Figma 设计交付物**：[./user-story.md](./user-story.md#13-figma-链接) §13-15
- **测试计划规范**：[../../spec/test-plan/README.md](../../spec/test-plan/README.md)

---

## 6. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-08-05 | Dev | 初版：2 个 Task 覆盖 3 个 GWT 场景 |
