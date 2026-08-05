# US-053 实施任务清单（Tasks）

> **US 关联**：[user-story.md](./user-story.md)（US-053 管理员账号密码登录）
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
| 1 | 后端管理员登录接口 | P0 | [§6.1 场景 1](./user-story.md#61-场景-1管理员正常登录)、[§6.2 场景 2](./user-story.md#62-场景-2用户名或密码错误)、[§6.3 场景 3](./user-story.md#63-场景-3管理员账号被禁用) |
| 2 | 后端管理员退出登录接口 | P0 | [§6.1 场景 1](./user-story.md#61-场景-1管理员正常登录) |
| 3 | Web 管理员登录页 + 路由守卫 | P0 | 全部 |
| 4 | 全局导航栏管理员信息/退出下拉菜单 | P0 | [§6.1 场景 1](./user-story.md#61-场景-1管理员正常登录) |

---

## 2. 实施任务

### Task 1: 后端管理员登录接口 [P0]

**Files:**
- Create: `backend/src/controllers/admin/auth.ts`
- Create: `backend/src/routes/admin/auth.ts`
- Create: `backend/src/services/admin/session.ts`
- Create: `backend/src/services/admin/user.ts`
- Test: `backend/tests/controllers/admin/login.test.ts`

**对应 GWT**：[user-story.md §6.1 场景 1](./user-story.md#61-场景-1管理员正常登录)、[§6.2 场景 2](./user-story.md#62-场景-2用户名或密码错误)、[§6.3 场景 3](./user-story.md#63-场景-3管理员账号被禁用)

- [ ] **Step 1: 写失败测试**

```typescript
// backend/tests/controllers/admin/login.test.ts
import request from 'supertest';
import { app } from '../../../src/app';

describe('POST /api/v1/admin/auth/login', () => {
  it('场景 1: 正确的用户名密码登录成功', async () => {
    const res = await request(app)
      .post('/api/v1/admin/auth/login')
      .send({ username: 'admin', password: 'correct_password' });

    expect(res.status).toBe(200);
    expect(res.body.token).toBeDefined();
    expect(res.body.expires_in).toBe(86400);
    expect(res.body.admin.username).toBe('admin');
  });

  it('场景 2: 错误的用户名或密码返回 401', async () => {
    const res = await request(app)
      .post('/api/v1/admin/auth/login')
      .send({ username: 'admin', password: 'wrong_password' });

    expect(res.status).toBe(401);
    expect(res.body.error).toBe('ADMIN_CREDENTIALS_INVALID');
  });

  it('场景 3: 被禁用的账号返回 403', async () => {
    const res = await request(app)
      .post('/api/v1/admin/auth/login')
      .send({ username: 'disabled_admin', password: 'correct_password' });

    expect(res.status).toBe(403);
    expect(res.body.error).toBe('ADMIN_DISABLED');
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- admin/login.test.ts`
Expected: FAIL

- [ ] **Step 3: 写最小实现**

```typescript
// backend/src/services/admin/user.ts
export class AdminUserService {
  async findByUsername(username: string) {
    return db('admin_user').where({ username }).first();
  }
}

// backend/src/controllers/admin/auth.ts
export async function login(ctx) {
  const { username, password } = ctx.request.body || {};
  const admin = await adminUserService.findByUsername(username);

  if (!admin || !(await bcrypt.compare(password, admin.password_hash))) {
    ctx.status = 401;
    ctx.body = { error: 'ADMIN_CREDENTIALS_INVALID', message: '用户名或密码错误' };
    return;
  }

  if (admin.status !== 0) {
    ctx.status = 403;
    ctx.body = { error: 'ADMIN_DISABLED', message: '账号已被禁用，请联系超级管理员' };
    return;
  }

  const tokens = await adminSessionService.create(admin.id);
  ctx.body = { ...tokens, admin: pick(admin, ['id', 'username', 'name', 'role']) };
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- admin/login.test.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/controllers/admin/auth.ts backend/src/routes/admin/auth.ts backend/src/services/admin/session.ts backend/src/services/admin/user.ts backend/tests/controllers/admin/login.test.ts
git commit -m "feat(admin-auth): add POST /admin/auth/login endpoint"
```

---

### Task 2: 后端管理员退出登录接口 [P0]

**Files:**
- Update: `backend/src/controllers/admin/auth.ts`
- Update: `backend/src/routes/admin/auth.ts`
- Update: `backend/src/services/admin/session.ts`
- Test: `backend/tests/controllers/admin/logout.test.ts`

**对应 GWT**：[user-story.md §6.1 场景 1](./user-story.md#61-场景-1管理员正常登录)

- [ ] **Step 1: 写失败测试**

```typescript
// backend/tests/controllers/admin/logout.test.ts
describe('POST /api/v1/admin/auth/logout', () => {
  it('退出登录后 token 失效', async () => {
    const loginRes = await request(app)
      .post('/api/v1/admin/auth/login')
      .send({ username: 'admin', password: 'correct_password' });

    const { token } = loginRes.body;

    const logoutRes = await request(app)
      .post('/api/v1/admin/auth/logout')
      .set('Authorization', `Bearer ${token}`);

    expect(logoutRes.status).toBe(200);

    const protectedRes = await request(app)
      .get('/api/v1/admin/users')
      .set('Authorization', `Bearer ${token}`);

    expect(protectedRes.status).toBe(401);
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

- [ ] **Step 3: 写最小实现**

```typescript
// backend/src/controllers/admin/auth.ts
export async function logout(ctx) {
  const authHeader = ctx.request.headers.authorization;
  const accessToken = authHeader?.replace('Bearer ', '');

  if (!accessToken) {
    ctx.status = 401;
    ctx.body = { error: 'TOKEN_INVALID', message: '登录态已失效' };
    return;
  }

  jwt.verify(accessToken, process.env.JWT_SECRET || 'dev-secret');

  const { refreshToken } = ctx.request.body || {};
  if (refreshToken) {
    await adminSessionService.revoke(refreshToken);
  }

  ctx.body = { message: '退出登录成功' };
}
```

- [ ] **Step 4: 跑测试确认通过**

- [ ] **Step 5: Commit**

```bash
git add backend/src/controllers/admin/auth.ts backend/src/routes/admin/auth.ts backend/src/services/admin/session.ts backend/tests/controllers/admin/logout.test.ts
git commit -m "feat(admin-auth): add POST /admin/auth/logout endpoint"
```

---

### Task 3: Web 管理员登录页 + 路由守卫 [P0]

**Files:**
- Create: `web-admin/src/pages/login/index.tsx`
- Create: `web-admin/src/utils/auth.ts`
- Create: `web-admin/src/router/guard.tsx`
- Test: `web-admin/src/pages/login/index.test.tsx`

**对应 GWT**：全部

- [ ] **Step 1: 写失败测试**

```typescript
// web-admin/src/pages/login/index.test.tsx
import { render, fireEvent, waitFor } from '@testing-library/react';
import LoginPage from './index';

jest.mock('../../utils/request');

describe('Admin LoginPage', () => {
  it('场景 1: 登录成功跳转后台首页', async () => {
    const { getByLabelText, getByText } = render(<LoginPage />);

    fireEvent.change(getByLabelText('用户名'), { target: { value: 'admin' } });
    fireEvent.change(getByLabelText('密码'), { target: { value: 'correct_password' } });
    fireEvent.click(getByText('登录'));

    await waitFor(() => {
      expect(localStorage.setItem).toHaveBeenCalledWith('admin_token', expect.any(String));
      expect(localStorage.setItem).toHaveBeenCalledWith('admin_user', expect.any(String));
      expect(window.location.href).toContain('/dashboard');
    });
  });

  it('场景 2: 用户名或密码错误显示提示', async () => {
    const { getByLabelText, getByText } = render(<LoginPage />);

    fireEvent.change(getByLabelText('用户名'), { target: { value: 'admin' } });
    fireEvent.change(getByLabelText('密码'), { target: { value: 'wrong' } });
    fireEvent.click(getByText('登录'));

    await waitFor(() => {
      expect(getByText('用户名或密码错误')).toBeDefined();
    });
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

- [ ] **Step 3: 写最小实现**

```tsx
// web-admin/src/pages/login/index.tsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login as loginApi } from '../../services/auth';

export default function LoginPage() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const res = await loginApi({ username, password });
      localStorage.setItem('admin_token', res.token);
      localStorage.setItem('admin_user', JSON.stringify(res.admin));
      navigate('/dashboard');
    } catch (err: any) {
      setError(err.message || '登录失败');
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <input value={username} onChange={e => setUsername(e.target.value)} placeholder="用户名" />
      <input type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="密码" />
      {error && <div>{error}</div>}
      <button type="submit" disabled={loading}>{loading ? '登录中...' : '登录'}</button>
    </form>
  );
}
```

- [ ] **Step 4: 跑测试确认通过**

- [ ] **Step 5: Commit**

```bash
git add web-admin/src/pages/login/index.tsx web-admin/src/utils/auth.ts web-admin/src/router/guard.tsx web-admin/src/pages/login/index.test.tsx
git commit -m "feat(web-admin): add admin login page and route guard"
```

---

### Task 4: 全局导航栏管理员信息/退出下拉菜单 [P0]

**Files:**
- Create: `web-admin/src/components/layout/Header.tsx`
- Test: `web-admin/src/components/layout/Header.test.tsx`

**对应 GWT**：[user-story.md §6.1 场景 1](./user-story.md#61-场景-1管理员正常登录)

- [ ] **Step 1: 写失败测试**

```typescript
// web-admin/src/components/layout/Header.test.tsx
import { render, fireEvent } from '@testing-library/react';
import Header from './Header';

describe('Admin Header', () => {
  it('点击管理员名字下拉显示退出登录选项', () => {
    const { getByText } = render(<Header admin={{ name: '管理员' }} />);
    fireEvent.click(getByText('管理员'));
    expect(getByText('退出登录')).toBeDefined();
  });

  it('点击退出登录清除 token 和 userInfo 并跳转登录页', async () => {
    const { getByText } = render(<Header admin={{ name: '管理员' }} />);
    fireEvent.click(getByText('管理员'));
    fireEvent.click(getByText('退出登录'));

    expect(localStorage.removeItem).toHaveBeenCalledWith('admin_token');
    expect(localStorage.removeItem).toHaveBeenCalledWith('admin_user');
    expect(window.location.href).toContain('/login');
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

- [ ] **Step 3: 写最小实现**

```tsx
// web-admin/src/components/layout/Header.tsx
import { useState } from 'react';
import { Dropdown } from 'antd';
import { logout as logoutApi } from '../../services/auth';

export default function Header({ admin }) {
  async function handleLogout() {
    const token = localStorage.getItem('admin_token');

    if (token) {
      try {
        await logoutApi(token);
      } catch {
        // ignore
      }
    }

    localStorage.removeItem('admin_token');
    localStorage.removeItem('admin_user');
    window.location.href = '/login';
  }

  const items = [
    { key: 'logout', label: <span onClick={handleLogout}>退出登录</span> },
  ];

  return (
    <div>
      <Dropdown menu={{ items }}>
        <span>{admin.name}</span>
      </Dropdown>
    </div>
  );
}
```

- [ ] **Step 4: 跑测试确认通过**

- [ ] **Step 5: Commit**

```bash
git add web-admin/src/components/layout/Header.tsx web-admin/src/components/layout/Header.test.tsx
git commit -m "feat(web-admin): add admin logout dropdown in header"
```

---

## 3. 任务执行纪律

- **严格顺序**：Task 1 → Task 2 → Task 3 → Task 4
- **每步必须可见**：Step 1（写测试）→ Step 2（看失败）→ Step 3（写实现）→ Step 4（看通过）→ Step 5（commit）
- **不允许 placeholder**：任何 "TBD" / "TODO" / "实现 later" / "类似 Task N" = 立即返工
- **每个 Task 结束 = 1 次 commit**：禁止跨 Task 累积 commit
- **P0 必做**：MVP 阶段四个 Task 均必做
- **GWT 覆盖**：每个 Task 头部必须明确「对应 GWT」场景编号

---

## 4. GWT 场景覆盖矩阵

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 |
|---------|--------|--------|--------|--------|
| §6.1 正常登录 | ✅ | ✅ | ✅ | ✅ |
| §6.2 用户名或密码错误 | ✅ | — | ✅ | — |
| §6.3 账号被禁用 | ✅ | — | ✅ | — |

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
| v1.0 | 2026-08-05 | Dev | 初版：4 个 Task 覆盖 3 个 GWT 场景 |
