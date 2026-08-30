# US-049 实施任务清单（Tasks）

> **US 关联**：[user-story.md](./user-story.md)（US-049 管理员查看数据看板与处理客服工单）
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
| 1 | support_ticket / ticket_reply 表迁移与 Repository | P0 | §6.2 |
| 2 | GET /api/admin/dashboard/peak-hours | P0 | §6.1, §6.3 |
| 3 | GET /api/admin/dashboard/monthly-hours | P0 | §6.1, §6.3 |
| 4 | GET /api/admin/dashboard/coach-ratings | P0 | §6.1 |
| 5 | GET /api/admin/dashboard/student-portrait | P0 | §6.1 |
| 6 | GET /api/admin/tickets 与 /:id | P0 | §6.2, §6.4 |
| 7 | PUT /api/admin/tickets/:id/assign 与 POST /reply | P0 | §6.2 |
| 8 | PUT /api/admin/tickets/:id/status | P0 | §6.2 |
| 9 | 看板缓存策略 | P1 | §6.1 |
| 10 | 管理员权限中间件 | P0 | §6.5, §6.6 |

---

## 2. 实施任务

### Task 1: 工单表迁移与 Repository [P0]

**Files:**
- Create: `backend/src/migrations/20260730_create_support_ticket.ts`
- Create: `backend/src/migrations/20260730_create_ticket_reply.ts`
- Create: `backend/src/repositories/supportTicket.ts`
- Test: `backend/tests/repositories/supportTicket.test.ts`

**对应 GWT**：[§6.2 场景 2](./user-story.md#62-场景-2管理员处理客服工单)

- [ ] **Step 1: RED — 写失败测试**

```typescript
// backend/tests/repositories/supportTicket.test.ts
import { SupportTicketRepository } from '../../src/repositories/supportTicket';

describe('SupportTicketRepository', () => {
  it('creates a ticket and assigns handler', async () => {
    const repo = new SupportTicketRepository();
    const ticket = await repo.create({
      ticket_no: 'T-001',
      type: 2,
      user_id: 1,
      title: '换教练申请',
      content: '想换教练',
      status: 0,
    });
    expect(ticket.id).toBeGreaterThan(0);
    const updated = await repo.assign(ticket.id, 5);
    expect(updated.handler_id).toBe(5);
    expect(updated.status).toBe(1);
  });
});
```

- [ ] **Step 2: 跑测试确认失败**
- [ ] **Step 3: GREEN — 写最小实现**
- [ ] **Step 4: 跑测试确认通过**
- [ ] **Step 5: REFACTOR — 提取状态校验**
- [ ] **Step 6: COMMIT**

```bash
git add backend/src/migrations/20260730_create_support_ticket.ts backend/src/migrations/20260730_create_ticket_reply.ts backend/src/repositories/supportTicket.ts backend/tests/repositories/supportTicket.test.ts
git commit -m "feat(support-ticket): add ticket/reply migrations and repository"
```

---

### Task 2: GET /api/admin/dashboard/peak-hours [P0]

**Files:**
- Create: `backend/src/controllers/admin/dashboard.ts`
- Create: `backend/src/routes/admin/dashboard.ts`
- Test: `backend/tests/controllers/admin/dashboard.test.ts`

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1管理员查看数据看板)、[§6.3 场景 3](./user-story.md#63-场景-3时间范围不合法)

- [ ] **Step 1-6**: 实现高峰期看板，覆盖 200 / 400；commit message `feat(admin): add peak hours dashboard`

---

### Task 3: GET /api/admin/dashboard/monthly-hours [P0]

**Files:**
- Modify: controller/route/test files

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1管理员查看数据看板)、[§6.3 场景 3](./user-story.md#63-场景-3时间范围不合法)

- [ ] **Step 1-6**: 实现每月课时看板；commit message `feat(admin): add monthly hours dashboard`

---

### Task 4: GET /api/admin/dashboard/coach-ratings [P0]

**Files:**
- Modify: controller/route/test files

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1管理员查看数据看板)

- [ ] **Step 1-6**: 实现教练评分排行榜；commit message `feat(admin): add coach ratings dashboard`

---

### Task 5: GET /api/admin/dashboard/student-portrait [P0]

**Files:**
- Modify: controller/route/test files

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1管理员查看数据看板)

- [ ] **Step 1-6**: 实现学员画像看板；commit message `feat(admin): add student portrait dashboard`

---

### Task 6: GET /api/admin/tickets 与 /:id [P0]

**Files:**
- Create: `backend/src/controllers/admin/supportTicket.ts`
- Create: `backend/src/routes/admin/supportTicket.ts`
- Test: `backend/tests/controllers/admin/supportTicket.test.ts`

**对应 GWT**：[§6.2 场景 2](./user-story.md#62-场景-2管理员处理客服工单)、[§6.4 场景 4](./user-story.md#64-场景-4工单不存在)

- [ ] **Step 1-6**: 实现工单列表与详情，覆盖 200 / 404；commit message `feat(admin): add ticket list and detail`

---

### Task 7: PUT /api/admin/tickets/:id/assign 与 POST /reply [P0]

**Files:**
- Modify: controller/route/test files

**对应 GWT**：[§6.2 场景 2](./user-story.md#62-场景-2管理员处理客服工单)

- [ ] **Step 1-6**: 实现分配与回复，覆盖 200 / 400 TICKET_CLOSED；commit message `feat(admin): add ticket assign and reply`

---

### Task 8: PUT /api/admin/tickets/:id/status [P0]

**Files:**
- Modify: controller/route/test files

**对应 GWT**：[§6.2 场景 2](./user-story.md#62-场景-2管理员处理客服工单)

- [ ] **Step 1-6**: 实现状态更新；commit message `feat(admin): add ticket status update`

---

### Task 9: 看板缓存策略 [P1]

**Files:**
- Modify: `backend/src/services/cache.ts`
- Test: `backend/tests/controllers/admin/dashboard.test.ts`

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1管理员查看数据看板)

- [ ] **Step 1-6**: 加 Redis 缓存，按看板维度失效；commit message `feat(cache): add dashboard cache`

---

### Task 10: 管理员权限中间件 [P0]

**Files:**
- Modify: `backend/src/middlewares/adminAuth.ts`
- Test: `backend/tests/middlewares/adminAuth.test.ts`

**对应 GWT**：[§6.5 场景 5](./user-story.md#65-场景-5无权限管理员访问看板接口)、[§6.6 场景 6](./user-story.md#66-场景-6无权限管理员访问客服工单接口)

- [ ] **Step 1-6**: 校验 `dashboard:read` / `ticket:read` / `ticket:write` 权限，无权限返回 403 FORBIDDEN；commit message `feat(auth): enforce dashboard and ticket permissions`

---

## 3. 任务执行纪律

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10
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
| v1.1 | 2026-07-31 | Dev | v3 评审 P1 修复：Task 10 覆盖看板与客服工单无权限场景|
