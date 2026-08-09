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

### Task 1：登录自动分流与重新入驻提交

- [ ] **1.1 RED**：编写 US-051 / US-054 登录响应 `coach_status=3` 时前端跳转 US-010 C-入驻资料填写页测试
- [ ] **1.2 RED**：编写 status≠3 时登录后不按重新入驻流程跳转测试
- [ ] **1.3 RED**：编写 status=3 教练调用 US-010 提交接口创建 `previous_coach_status=3` 的 pending 快照测试
- [ ] **1.4 RED**：编写 status=3 教练保存草稿后 coach.status 仍为 3 测试
- [ ] **1.5 GREEN**：实现登录回调路由分流与 US-010 重新入驻提交流程
- [ ] **1.6 COMMIT**：`feat(miniapp-coach): auto-redirect resigned coach to reapply onboarding`

**对应**：user-story.md 场景 1、3、4 / tech-design §4

### Task 2：US-010 处理重新入驻提交

- [ ] **2.1 RED**：编写 status=3 教练调用 `POST /api/coach/application/submit` 创建 `previous_coach_status=3` 的 pending 快照测试
- [ ] **2.2 RED**：编写 status=3 教练调用 `POST /api/coach/application/save-draft` 保存草稿后 coach.status 仍为 3 测试
- [ ] **2.3 GREEN**：复用 US-010 提交/草稿接口处理重新入驻（无需新建接口）
- [ ] **2.4 COMMIT**：`feat(coach): reapply submission via US-010 application endpoint`

**对应**：user-story.md 场景 1 / tech-design §4.1

### Task 3：US-011 审核重新入驻

- [ ] **3.1 RED**：编写 `POST /api/admin/coach/application/approve` 对 `previous_coach_status=3` 的申请通过后 coach.status=1 测试
- [ ] **3.2 RED**：编写 `POST /api/admin/coach/application/reject` 对 `previous_coach_status=3` 的申请拒绝后 coach.status=3 测试
- [ ] **3.3 RED**：编写 coach 表生效资料在拒绝前后保持不变测试
- [ ] **3.4 GREEN**：复用 US-011 审核接口（无需新建管理员接口）
- [ ] **3.5 COMMIT**：`feat(admin): reapply review via US-011 audit endpoints`

**对应**：user-story.md 场景 2、5 / tech-design §4.1、§4.2

### Task 4：缓存与审计日志

- [ ] **4.1 RED**：编写 coach.status 缓存失效测试
- [ ] **4.2 RED**：编写 审核操作写入 `coach_audit_log` 测试
- [ ] **4.3 GREEN**：实现缓存失效与审计日志
- [ ] **4.4 COMMIT**：`feat(us-040): cache invalidation and audit log for reapply`

**对应**：tech-design §6、§8

---

## 3. 测试用例映射

| # | 用例名称 | 对应 GWT 场景 | 测试方法 | 期望结果 |
|---|---------|--------------|---------|---------|
| 1 | 已离职教练登录后自动进入 US-010 | 6.1 | `test_reapply_auto_redirect` | 跳转 C-入驻资料填写页，顶部展示重新入驻说明条 |
| 2 | 非已离职教练登录后不按重新入驻分流 | 6.3 | `test_not_resigned_no_redirect` | 进入教练首页或对应页面，不进入 US-010 |
| 3 | 重复发起重新入驻 | 6.4 | `test_reapply_duplicate` | 跳转「等待审核页」，不产生新的 pending 申请 |
| 4 | 重新入驻提交创建 pending 快照 | 6.1 | `test_reapply_submission_creates_snapshot` | `coach.status=0`，`coach_application.previous_coach_status=3` |
| 5 | 管理员通过重新入驻 | 6.2 | `test_approve_reapply_success` | `coach.status=1`，`coach_application.status=approved`，快照覆盖 coach 生效资料 |
| 6 | 重新入驻审核被拒绝 | 6.5 | `test_reject_reapply_success` | `coach.status=3`，`coach_application.status=rejected`，coach 生效资料不变 |
| 7 | 并发提交幂等 | 8.1 | `test_reapply_idempotent` | 仅 1 条 pending 申请记录 |
| 8 | 老学员 frozen package 不自动恢复 | 8.2 | `test_frozen_package_not_auto_active` | `package.status` 仍为 frozen |
| 9 | 历史评分对新学员可见 | 8.3 | `test_rating_visible_for_new_students` | 新学员主页展示历史评分 |

---

## 4. 验收执行检查单

- [ ] 单元测试全部通过
- [ ] 集成测试全部通过
- [ ] 所有 GWT 场景均有对应自动化测试
- [ ] 状态机转换 100% 覆盖（3→0、0→1、0→3）
- [ ] 无 TBD/TODO 遗留
