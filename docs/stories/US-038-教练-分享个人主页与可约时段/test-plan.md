# US-038 教练分享个人主页与可约时段测试计划

---

## 1. 测试策略

| 层级 | 占比 | 工具 | 说明 |
|------|------|------|------|
| 单元测试 | 40% | Jest | scene 编解码、字段过滤、缓存键 |
| 集成测试 | 40% | Jest + Supertest | 分享生成、公开接口、游客落地页 |
| E2E 测试 | 15% | 微信小程序自动化 | 分享卡片点击、落地页转化 |
| 性能测试 | 5% | k6 | 公开接口并发 |

---

## 2. TDD 任务清单

### Task 1：分享数据生成接口

- [ ] 1.1 RED：编写 `GET /api/coach/v1/share/profile-slots` 成功返回 scene 与资料的失败测试
- [ ] 1.2 GREEN：实现接口，聚合 coach 资料与 schedule_slot
- [ ] 1.3 REFACTOR：抽取分享数据组装服务
- [ ] 1.4 COMMIT：`feat(us-038): coach share profile-slots data generation`

**对应**：user-story.md 场景 6.1、6.3、6.4

### Task 2：公开落地页接口

- [ ] 2.1 RED：编写公开资料接口返回不含 phone/wechat_qr 的测试
- [ ] 2.2 RED：编写公开可约时段接口测试
- [ ] 2.3 GREEN：实现 `/api/public/v1/coaches/{id}/share` 与 `/available-slots`
- [ ] 2.4 REFACTOR：统一公开 coach DTO
- [ ] 2.5 COMMIT：`feat(us-038): public coach share landing APIs`

**对应**：user-story.md 场景 6.2、6.5

### Task 3：教练状态与隐私校验

- [ ] 3.1 RED：编写非已通过教练调用分享接口返回 403 的测试
- [ ] 3.2 RED：编写公开接口不暴露隐私字段的测试
- [ ] 3.3 GREEN：实现 coach.status 校验与字段白名单
- [ ] 3.4 COMMIT：`feat(us-038): share status and privacy guard`

**对应**：user-story.md 场景 6.3、边界场景 2

### Task 4：scene 解析与兜底

- [ ] 4.1 RED：编写非法/过期 scene 返回错误的测试
- [ ] 4.2 RED：编写无可约时段时返回兜底文案的测试
- [ ] 4.3 GREEN：实现 scene 校验与兜底文案
- [ ] 4.4 REFACTOR：抽取 scene 工具模块
- [ ] 4.5 COMMIT：`feat(us-038): scene validation and empty slots fallback`

**对应**：user-story.md 场景 6.4、6.5

### Task 5：缓存与限流

- [ ] 5.1 RED：编写公开接口缓存命中测试
- [ ] 5.2 RED：编写单 IP 限流测试
- [ ] 5.3 GREEN：实现 Redis 缓存与限流中间件
- [ ] 5.4 COMMIT：`perf(us-038): cache and rate limit public share APIs`

**对应**：tech-design §6、§8

### Task 6：前端落地页

- [ ] 6.1 RED：编写落地页组件渲染测试
- [ ] 6.2 GREEN：实现 /pages/coach-share/index 页面与 CTA
- [ ] 6.3 COMMIT：`feat(us-038): coach share landing page`

**对应**：user-story.md 场景 6.2

---

## 3. 测试用例映射

| # | 用例名称 | 对应 GWT 场景 | 测试方法 | 期望结果 |
|---|---------|--------------|---------|---------|
| 1 | 教练成功生成分享卡片 | 6.1 | `test_generate_share_card_success` | 返回 scene 与教练资料+时段 |
| 2 | 游客打开分享落地页 | 6.2 | `test_visitor_open_share_landing_success` | 展示公开资料、可约时段、CTA |
| 3 | 非已通过教练无法分享 | 6.3 | `test_not_approved_coach_cannot_share` | 按钮隐藏或接口 403 |
| 4 | 无可约时段兜底 | 6.4 | `test_share_no_slots_fallback` | 返回兜底文案，不展示空列表 |
| 5 | 非法/过期 scene | 6.5 | `test_invalid_share_scene_returns_error` | 落地页错误提示 |
| 6 | 隐私字段不暴露 | 边界 2 | `test_public_share_hides_private_fields` | 响应不含 phone / wechat_qr |
| 7 | 并发生成去重 | 边界 3 | `test_concurrent_share_generation_idempotent` | 相同 scene，仅 1 条 share_log |

---

## 4. 验收执行检查单

- [ ] 单元测试全部通过
- [ ] 集成测试全部通过
- [ ] 所有 GWT 场景均有对应自动化测试
- [ ] 公开接口限流与缓存验证通过
- [ ] 无 TBD/TODO 遗留
