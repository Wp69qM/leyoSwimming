# US-057 实施任务清单（Tasks）

> **US 关联**：[user-story.md](./user-story.md)（US-057 管理员管理管理员账号）
> **文档角色**：TDD 任务清单 + 测试计划（双重角色）
> **状态**：🔲 待开发执行
> **作者**：开发　|　**最后更新**：2026-08-08

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
| 1 | 后端管理员账号列表/详情接口 | P0 | [§6.1 场景 1](./user-story.md#61-场景-1super_admin-查看管理员列表) |
| 2 | 后端新建/编辑管理员接口 | P0 | [§6.2 场景 2](./user-story.md#62-场景-2super_admin-新建管理员账号)、[§6.3 场景 3](./user-story.md#63-场景-3super_admin-编辑管理员角色)、[§6.6 场景 6](./user-story.md#66-场景-6admin-尝试新建管理员账号无权限) |
| 3 | 后端禁用/删除管理员接口 | P0 | [§6.4 场景 4](./user-story.md#64-场景-4super_admin-禁用管理员账号)、[§6.5 场景 5](./user-story.md#65-场景-5super_admin-删除管理员账号)、[§6.7 场景 7](./user-story.md#67-场景-7super_admin-尝试删除自己的账号)、[§6.8 场景 8](./user-story.md#68-场景-8super_admin-尝试禁用最后一个-super_admin) |
| 4 | 后端重置密码接口 | P0 | user-story.md §4.1 主路径 |
| 5 | Web 管理员账号管理列表页 | P0 | [§6.1 场景 1](./user-story.md#61-场景-1super_admin-查看管理员列表) |
| 6 | Web 新建/编辑管理员弹窗 | P0 | [§6.2 场景 2](./user-story.md#62-场景-2super_admin-新建管理员账号)、[§6.3 场景 3](./user-story.md#63-场景-3super_admin-编辑管理员角色) |
| 7 | Web 禁用/删除/重置密码交互 | P0 | [§6.4 场景 4](./user-story.md#64-场景-4super_admin-禁用管理员账号)、[§6.5 场景 5](./user-story.md#65-场景-5super_admin-删除管理员账号) |

---

## 2. 实施任务

### Task 1: 后端管理员账号列表/详情接口 [P0]

**Files:**
- Create: `backend/src/controllers/admin/admin-user.ts`
- Create: `backend/src/routes/admin/admin-user.ts`
- Create: `backend/src/services/admin/admin-user.ts`
- Test: `backend/tests/controllers/admin/admin-user/list.test.ts`
- Test: `backend/tests/controllers/admin/admin-user/detail.test.ts`

**对应 GWT**：[user-story.md §6.1 场景 1](./user-story.md#61-场景-1super_admin-查看管理员列表)

- [ ] **Step 1: 写失败测试**

```typescript
// backend/tests/controllers/admin/admin-user/list.test.ts
describe('POST /api/admin/admin/list', () => {
  it('场景 1: super_admin 查看管理员列表', async () => {
    const res = await request(app)
      .post('/api/admin/admin/list')
      .set('Authorization', `Bearer ${superAdminToken}`)
      .send({ page: 1, size: 20 });

    expect(res.status).toBe(200);
    expect(res.body.data.list.length).toBeGreaterThan(0);
    expect(res.body.data.list[0]).toHaveProperty('allowed_actions');
  });

  it('admin 查看列表只返回 VIEW 权限', async () => {
    const res = await request(app)
      .post('/api/admin/admin/list')
      .set('Authorization', `Bearer ${adminToken}`)
      .send({});

    expect(res.status).toBe(200);
    expect(res.body.data.list[0].allowed_actions).toEqual(['VIEW']);
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- admin/admin-user/list.test.ts`
Expected: FAIL

- [ ] **Step 3: 写最小实现**

```typescript
// backend/src/services/admin/admin-user.ts
export async function listAdmins(filters, page, size) {
  const query = db('admin_user')
    .whereNull('deleted_at')
    .orderBy('created_at', 'desc');

  if (filters.role) query.where('role', filters.role);
  if (filters.status !== null && filters.status !== undefined) query.where('status', filters.status);
  if (filters.keyword) {
    query.where((builder) => {
      builder.where('username', 'like', `%${filters.keyword}%`)
        .orWhere('name', 'like', `%${filters.keyword}%`);
    });
  }

  const total = await query.clone().count('* as count').first();
  const list = await query.limit(size).offset((page - 1) * size);

  return {
    list: list.map((item) => ({
      ...pick(item, ['id', 'username', 'name', 'role', 'status', 'last_login_at', 'created_at']),
      allowed_actions: computeAllowedActions(currentAdmin, item),
    })),
    total: total.count,
  };
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- admin/admin-user/list.test.ts`
Expected: PASS

- [ ] **Step 5: COMMIT**

```bash
git commit -m "feat(us-057): admin user list and detail APIs"
```

---

### Task 2: 后端新建/编辑管理员接口 [P0]

**Files:**
- Update: `backend/src/controllers/admin/admin-user.ts`
- Update: `backend/src/services/admin/admin-user.ts`
- Test: `backend/tests/controllers/admin/admin-user/create.test.ts`
- Test: `backend/tests/controllers/admin/admin-user/update.test.ts`

**对应 GWT**：[§6.2 场景 2](./user-story.md#62-场景-2super_admin-新建管理员账号)、[§6.3 场景 3](./user-story.md#63-场景-3super_admin-编辑管理员角色)、[§6.6 场景 6](./user-story.md#66-场景-6admin-尝试新建管理员账号无权限)

- [ ] **Step 1: 写失败测试**

```typescript
// backend/tests/controllers/admin/admin-user/create.test.ts
describe('POST /api/admin/admin/add', () => {
  it('场景 2: super_admin 新建管理员账号', async () => {
    const res = await request(app)
      .post('/api/admin/admin/add')
      .set('Authorization', `Bearer ${superAdminToken}`)
      .send({ username: 'new_admin', name: '新管理员', password: 'Admin@1234', role: 'admin' });

    expect(res.status).toBe(200);
    expect(res.body.data.username).toBe('new_admin');
    expect(res.body.data.status).toBe(0);
  });

  it('场景 6: admin 尝试新建返回 403', async () => {
    const res = await request(app)
      .post('/api/admin/admin/add')
      .set('Authorization', `Bearer ${adminToken}`)
      .send({ username: 'hacker', name: 'H', password: 'Admin@1234', role: 'admin' });

    expect(res.status).toBe(403);
    expect(res.body.error).toBe('ADMIN_PERMISSION_DENIED');
  });
});
```

- [ ] **Step 2-5**: RED → GREEN → COMMIT

Commit message:
```bash
git commit -m "feat(us-057): create and update admin user APIs"
```

---

### Task 3: 后端禁用/删除管理员接口 [P0]

**Files:**
- Update: `backend/src/controllers/admin/admin-user.ts`
- Update: `backend/src/services/admin/admin-user.ts`
- Test: `backend/tests/controllers/admin/admin-user/toggle-status.test.ts`
- Test: `backend/tests/controllers/admin/admin-user/delete.test.ts`

**对应 GWT**：[§6.4 场景 4](./user-story.md#64-场景-4super_admin-禁用管理员账号)、[§6.5 场景 5](./user-story.md#65-场景-5super_admin-删除管理员账号)、[§6.7 场景 7](./user-story.md#67-场景-7super_admin-尝试删除自己的账号)、[§6.8 场景 8](./user-story.md#68-场景-8super_admin-尝试禁用最后一个-super_admin)

- [ ] **Step 1: 写失败测试**

```typescript
// backend/tests/controllers/admin/admin-user/delete.test.ts
describe('POST /api/admin/admin/delete', () => {
  it('场景 5: super_admin 删除其他管理员', async () => {
    const res = await request(app)
      .post('/api/admin/admin/delete')
      .set('Authorization', `Bearer ${superAdminToken}`)
      .send({ id: targetAdminId, reason: '清理' });

    expect(res.status).toBe(200);
    const deleted = await db('admin_user').where('id', targetAdminId).first();
    expect(deleted.deleted_at).toBeDefined();
  });

  it('场景 7: 不能删除自己', async () => {
    const res = await request(app)
      .post('/api/admin/admin/delete')
      .set('Authorization', `Bearer ${superAdminToken}`)
      .send({ id: currentAdminId, reason: '自删' });

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('ADMIN_CANNOT_DELETE_SELF');
  });
});
```

- [ ] **Step 2-5**: RED → GREEN → COMMIT

Commit message:
```bash
git commit -m "feat(us-057): toggle status and delete admin user APIs"
```

---

### Task 4: 后端重置密码接口 [P0]

**Files:**
- Update: `backend/src/controllers/admin/admin-user.ts`
- Update: `backend/src/services/admin/admin-user.ts`
- Test: `backend/tests/controllers/admin/admin-user/reset-password.test.ts`

**对应 GWT**：user-story.md §4.1 主路径

- [ ] **Step 1: 写失败测试**

```typescript
// backend/tests/controllers/admin/admin-user/reset-password.test.ts
describe('POST /api/admin/admin/reset-password', () => {
  it('super_admin 重置其他管理员密码', async () => {
    const res = await request(app)
      .post('/api/admin/admin/reset-password')
      .set('Authorization', `Bearer ${superAdminToken}`)
      .send({ id: targetAdminId });

    expect(res.status).toBe(200);
    expect(res.body.data.temp_password).toBeDefined();
    expect(res.body.data.temp_password.length).toBe(12);
  });
});
```

- [ ] **Step 2-5**: RED → GREEN → COMMIT

Commit message:
```bash
git commit -m "feat(us-057): reset admin password API"
```

---

### Task 5: Web 管理员账号管理列表页 [P0]

**Files:**
- Create: `web/src/pages/admin/admin-user/index.tsx`
- Create: `web/src/api/admin-user.ts`
- Test: `web/src/pages/admin/admin-user/index.test.tsx`

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1super_admin-查看管理员列表)

- [ ] **Step 1: 写失败测试**

```typescript
// web/src/pages/admin/admin-user/index.test.tsx
describe('AdminUserListPage', () => {
  it('渲染列表并展示操作按钮', async () => {
    render(<AdminUserListPage />);
    expect(await screen.findByText('新建管理员')).toBeInTheDocument();
    expect(await screen.findByText('系统管理员')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2-5**: RED → GREEN → COMMIT

Commit message:
```bash
git commit -m "feat(us-057): admin user management list page"
```

---

### Task 6: Web 新建/编辑管理员弹窗 [P0]

**Files:**
- Create: `web/src/pages/admin/admin-user/components/AdminUserModal.tsx`
- Test: `web/src/pages/admin/admin-user/components/AdminUserModal.test.tsx`

**对应 GWT**：[§6.2 场景 2](./user-story.md#62-场景-2super_admin-新建管理员账号)、[§6.3 场景 3](./user-story.md#63-场景-3super_admin-编辑管理员角色)

- [ ] **Step 1-5**: RED → GREEN → COMMIT

Commit message:
```bash
git commit -m "feat(us-057): create and edit admin user modal"
```

---

### Task 7: Web 禁用/删除/重置密码交互 [P0]

**Files:**
- Update: `web/src/pages/admin/admin-user/index.tsx`
- Create: `web/src/pages/admin/admin-user/components/ResetPasswordResultModal.tsx`
- Test: `web/src/pages/admin/admin-user/index.test.tsx`

**对应 GWT**：[§6.4 场景 4](./user-story.md#64-场景-4super_admin-禁用管理员账号)、[§6.5 场景 5](./user-story.md#65-场景-5super_admin-删除管理员账号)

- [ ] **Step 1-5**: RED → GREEN → COMMIT

Commit message:
```bash
git commit -m "feat(us-057): disable, delete and reset password interactions"
```

---

## 3. 集成测试清单

| # | 场景 | 关键断言 |
|---|------|----------|
| 1 | super_admin 完整管理另一个管理员 | 新建 → 编辑 → 禁用 → 启用 → 重置密码 → 删除，全流程审计日志 +1 |
| 2 | 禁用后无法登录 | 使用 US-053 登录接口，被禁用账号返回 `ADMIN_DISABLED` |
| 3 | 删除后会话失效 | 删除前账号已登录，删除后该 token 访问任意管理接口返回 401 |
| 4 | admin 角色只读 | admin 访问 `add`/`update`/`delete` 接口均返回 403 |

---

## 4. 回归测试范围

- [ ] US-053 管理员账号密码登录（共用 `admin_user` 表，确保登录逻辑未受影响）
- [ ] US-042 管理员管理用户账号（列表页组件可复用，确认无冲突）
- [ ] US-055 管理员管理教练账号（列表页组件可复用，确认无冲突）

---

## 5. 变更日志

| 版本 | 日期 | 作者 | 变更内容 |
|------|------|------|----------|
| v1.0 | 2026-08-08 | Dev | 初版：7 个 TDD Task，覆盖 8 个 GWT 场景及集成/回归测试范围 |
