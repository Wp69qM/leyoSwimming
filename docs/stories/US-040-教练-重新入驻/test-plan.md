# US-040 教练重新入驻测试计划

---

## 1. 测试策略

| 层级 | 占比 | 工具 | 说明 |
|------|------|------|------|
| 单元测试 | 35% | Jest | 状态机转换、可见性过滤逻辑 |
| 集成测试 | 50% | Jest + Supertest | 重新入驻入口、US-010 提交、US-011 审核 API 端到端 |
| E2E 测试 | 10% | 微信小程序自动化 | 教练端重新入驻入口与状态页 |
| 性能测试 | 5% | k6 | 提交接口并发 |

---

## 2. TDD 任务清单

### Task 1：历史评分可见性数据模型扩展

- [ ] **1.1 RED**：编写 `coach_rating.is_visible_to_new` 字段缺失测试
- [ ] **1.2 GREEN**：创建 Knex migration 为 `coach_rating` / `review` 增加 `is_visible_to_new` 字段（默认 true；教练离职时置为 false，重新入驻后保持 false）
- [ ] **1.3 COMMIT**：`feat(us-040): add is_visible_to_new to coach_rating for reapply`

**对应**：user-story.md 场景 2 / tech-design §3

### Task 2：「我的」页面重新入驻入口与登录自动分流

- [ ] **2.1 RED**：编写 status=3 时展示「重新入驻」入口测试
- [ ] **2.2 RED**：编写 status≠3 时入口隐藏/置灰测试
- [ ] **2.3 RED**：编写 US-051 登录响应 `coach_status=3` 时前端跳转 US-010 C-入驻资料填写页测试
- [ ] **2.4 GREEN**：实现「我的」页面入口可见性与登录回调路由
- [ ] **2.5 COMMIT**：`feat(miniapp-coach): add reapply entry and auto-redirect for resigned coach`

**对应**：user-story.md 场景 1、3 / tech-design §4.1

### Task 3：重新入驻入口校验接口

- [ ] **3.1 RED**：编写 `POST /api/coach/v1/reapply/entry` status=3 返回 200 与 `redirect_to=coach_onboarding_page` 测试
- [ ] **3.2 RED**：编写 status=1 返回 403 `COACH_STATUS_NOT_ALLOWED` 测试
- [ ] **3.3 RED**：编写 status=0 返回 409 `REAPPLY_ALREADY_PENDING` 测试
- [ ] **3.4 GREEN**：实现 `POST /api/coach/v1/reapply/entry` 校验接口（不修改 coach.status）
- [ ] **3.5 REFACTOR**：封装 reapply entry service
- [ ] **3.6 COMMIT**：`feat(coach): add reapply entry validation endpoint`

**对应**：user-story.md 场景 1、3、4 / tech-design §4.1

### Task 4：US-010 处理重新入驻提交

- [ ] **4.1 RED**：编写 status=3 教练调用 `POST /api/coach/application` 创建 `previous_coach_status=3` 的 pending 快照测试
- [ ] **4.2 RED**：编写 status=3 教练保存草稿后 coach.status 仍为 3 测试
- [ ] **4.3 GREEN**：复用 US-010 提交/草稿接口处理重新入驻（无需新建接口）
- [ ] **4.4 COMMIT**：`feat(coach): reapply submission via US-010 application endpoint`

**对应**：user-story.md 场景 1 / tech-design §4.2

### Task 5：US-011 审核重新入驻

- [ ] **5.1 RED**：编写 `POST /api/admin/coach/applications/{id}/approve` 对 `previous_coach_status=3` 的申请通过后 coach.status=1 测试
- [ ] **5.2 RED**：编写 `POST /api/admin/coach/applications/{id}/reject` 对 `previous_coach_status=3` 的申请拒绝后 coach.status=3 测试
- [ ] **5.3 RED**：编写 coach 表生效资料在拒绝前后保持不变测试
- [ ] **5.4 GREEN**：复用 US-011 审核接口（无需新建管理员接口）
- [ ] **5.5 COMMIT**：`feat(admin): reapply review via US-011 audit endpoints`

**对应**：user-story.md 场景 2、5 / tech-design §4.3、§4.4

### Task 6：历史评分对新学员隐藏

- [ ] **6.1 RED**：编写新学员查看教练主页不展示 `is_visible_to_new=false` 历史评分测试
- [ ] **6.2 RED**：编写老学员仍可查看历史评分测试
- [ ] **6.3 GREEN**：实现评分查询的可见性过滤
- [ ] **6.4 COMMIT**：`feat(coach): hide historical ratings from new students after reapply`

**对应**：user-story.md 场景 2、边界场景 3 / tech-design §3

### Task 7：缓存与审计

- [ ] **7.1 RED**：编写 coach.status 缓存失效测试
- [ ] **7.2 RED**：编写审核操作写入 `coach_audit_log` 测试
- [ ] **7.3 GREEN**：实现缓存失效与审计日志
- [ ] **7.4 COMMIT**：`feat(us-040): cache invalidation and audit log for reapply`

**对应**：tech-design §6、§8

---

## 3. 测试用例映射

| # | 用例名称 | 对应 GWT 场景 | 测试方法 | 期望结果 |
|---|---------|--------------|---------|---------|
| 1 | 已离职教练登录后自动进入 US-010 | 6.1 | `test_reapply_auto_redirect` | 跳转 C-入驻资料填写页，顶部展示重新入驻说明条 |
| 2 | 已离职教练从「我的」页面进入重新入驻 | 6.1 | `test_reapply_entry_success` | `POST /api/coach/v1/reapply/entry` 返回 200，进入 US-010 |
| 3 | 非已离职教练无法重新入驻 | 6.3 | `test_not_resigned_cannot_reapply` | 入口隐藏或接口 403 `COACH_STATUS_NOT_ALLOWED` |
| 4 | 重复发起重新入驻 | 6.4 | `test_reapply_duplicate` | `POST /api/coach/v1/reapply/entry` 返回 409 `REAPPLY_ALREADY_PENDING` |
| 5 | 重新入驻提交创建 pending 快照 | 6.1 | `test_reapply_submission_creates_snapshot` | `coach.status=0`，`coach_application.previous_coach_status=3` |
| 6 | 管理员通过重新入驻 | 6.2 | `test_approve_reapply_success` | `coach.status=1`，`coach_application.status=approved`，快照覆盖 coach 生效资料 |
| 7 | 重新入驻审核被拒绝 | 6.5 | `test_reject_reapply_success` | `coach.status=3`，`coach_application.status=rejected`，coach 生效资料不变 |
| 8 | 并发发起幂等 | 8.1 | `test_reapply_idempotent` | 仅 1 条 pending 申请记录 |
| 9 | 老学员 frozen package 不自动恢复 | 8.2 | `test_frozen_package_not_auto_active` | `package.status` 仍为 frozen |
| 10 | 历史评分对新学员隐藏 | 8.3 | `test_rating_hidden_for_new_students` | 新学员主页无历史评分 |

---

## 4. 验收执行检查单

- [ ] 单元测试全部通过
- [ ] 集成测试全部通过
- [ ] 所有 GWT 场景均有对应自动化测试
- [ ] 状态机转换 100% 覆盖（3→0、0→1、0→3）
- [ ] 无 TBD/TODO 遗留
