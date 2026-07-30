# US-047 实施任务清单（Tasks）

> **US 关联**：[user-story.md](./user-story.md)
> **作者**：开发　|　**最后更新**：2026-07-30

---

## 0. 双重角色说明

本文档承担 TDD 任务清单与测试计划双重角色。

---

## 1. Task 概览

| Task | 标题 | 优先级 | 对应 GWT 场景 |
|------|------|--------|--------------|
| 1 | 场馆/公告/闭馆/用户须知 Repository 与表迁移 | P0 | §6.1, §6.2, §6.3 |
| 2 | PUT /api/admin/venue 场馆信息接口 | P0 | §6.1 |
| 3 | POST /api/admin/notices 公告接口 | P0 | §6.1 |
| 4 | POST /api/admin/venue-closures 闭馆接口 | P0 | §6.2, §6.4 |
| 5 | POST /api/admin/terms 用户须知版本接口 | P0 | §6.3, §6.5 |
| 6 | 闭馆影响计算与通知 | P0 | §6.2 |
| 7 | 用户须知签署记录查询 | P1 | §6.3 |

---

## 2. 实施任务

### Task 1: Repository 与表迁移 [P0]

**Files:**
- Create: migrations for venue, notice, venue_closure, terms_of_service, user_terms_sign
- Create: `backend/src/repositories/venueOps.ts`
- Test: `backend/tests/repositories/venueOps.test.ts`

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1管理员配置场馆信息并发布公告)、[§6.2 场景 2](./user-story.md#62-场景-2管理员设置闭馆并自动取消受影响预约)、[§6.3 场景 3](./user-story.md#63-场景-3管理员更新用户须知并触发重新签署)

- [ ] **Step 1-6**: 实现迁移与 Repository；commit `feat(venue-ops): add migrations and repository`

---

### Task 2: PUT /api/admin/venue [P0]

**Files:**
- Create: `backend/src/controllers/admin/venueOps.ts`
- Create: `backend/src/routes/admin/venueOps.ts`
- Test: `backend/tests/controllers/admin/venueOps.test.ts`

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1管理员配置场馆信息并发布公告)

- [ ] **Step 1-6**: 实现场馆信息更新；commit `feat(admin): add PUT /venue`

---

### Task 3: POST /api/admin/notices [P0]

**Files:**
- Modify: controller/route/test files

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1管理员配置场馆信息并发布公告)

- [ ] **Step 1-6**: 实现公告发布；commit `feat(admin): add POST /notices`

---

### Task 4: POST /api/admin/venue-closures [P0]

**Files:**
- Modify: controller/route/test files

**对应 GWT**：[§6.2 场景 2](./user-story.md#62-场景-2管理员设置闭馆并自动取消受影响预约)、[§6.4 场景 4](./user-story.md#64-场景-4闭馆日期冲突)

- [ ] **Step 1-6**: 实现闭馆设置与冲突校验；commit `feat(admin): add POST /venue-closures`

---

### Task 5: POST /api/admin/terms [P0]

**Files:**
- Modify: controller/route/test files

**对应 GWT**：[§6.3 场景 3](./user-story.md#63-场景-3管理员更新用户须知并触发重新签署)、[§6.5 场景 5](./user-story.md#65-场景-5用户须知内容为空)

- [ ] **Step 1-6**: 实现用户须知版本发布；commit `feat(admin): add POST /terms`

---

### Task 6: 闭馆影响计算与通知 [P0]

**Files:**
- Create: `backend/src/services/closureImpact.ts`
- Test: `backend/tests/services/closureImpact.test.ts`

**对应 GWT**：[§6.2 场景 2](./user-story.md#62-场景-2管理员设置闭馆并自动取消受影响预约)

- [ ] **Step 1-6**: 实现取消预约、释放课时、发送通知；commit `feat(closure): add impact calculation and notification`

---

### Task 7: 用户须知签署记录查询 [P1]

**Files:**
- Modify: controller/route/test files

**对应 GWT**：[§6.3 场景 3](./user-story.md#63-场景-3管理员更新用户须知并触发重新签署)

- [ ] **Step 1-6**: 实现签署记录查询；commit `feat(admin): add GET /terms/sign-records`

---

## 3. 任务执行纪律

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7
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
