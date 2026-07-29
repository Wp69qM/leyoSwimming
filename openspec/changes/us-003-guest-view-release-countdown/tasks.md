# Tasks: US-003 游客查看预约释放倒计时

> 本文档对应 `docs/stories/US-003-.../test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: ReleaseRule Repository — 查询启用的释放规则 [P0]

**Files:**
- Create: `backend/src/repositories/release-rule.ts`
- Test: `backend/tests/repositories/release-rule.test.ts`

**Spec coverage:** REQ-002 Scenario "释放规则未配置时不显示倒计时"

- [ ] **RED:** Write 3 failing tests — `findActive` returns active rule with all fields; returns null when no active rule; returns rule with `manualOverrideAt` field
- [ ] **GREEN:** Implement `ReleaseRuleRepository.findActive()` — `WHERE status=1` 查询单条启用规则，字段名驼峰映射
- [ ] **COMMIT:** `feat(release): add ReleaseRuleRepository.findActive for active rule query`

## Task 2: 倒计时计算逻辑 — ReleaseCountdownService [P0]

**Files:**
- Create: `backend/src/services/release-countdown-service.ts`
- Test: `backend/tests/services/release-countdown-service.test.ts`

**Spec coverage:** REQ-001 Scenarios "释放前 24 小时显示倒计时", "释放当天显示今日 10:00 开放下周预约", "非释放时段不显示倒计时"; REQ-002 Scenarios "管理员手动覆盖释放时间时优先使用", "手动覆盖时间已过期时忽略并按常规计算"

- [ ] **RED:** Write 7 failing tests — 释放前 24h 显示倒计时（周二 12:00）; 释放当天显示今日文案（周三 08:30）; 非释放时段不显示（周三 10:01）; 非释放时段不显示（周四）; 规则为 null 时 show=false; manual_override_at 未过期时优先使用; manual_override_at 已过期时忽略
- [ ] **GREEN:** Implement `ReleaseCountdownService.calculate(rule, serverNow)` — 计算 `calculateNextReleaseAt`（优先 manual_override_at，否则本周/下周常规时刻）+ `shouldShowCountdown`（24h 窗口判断）+ 文案生成（按 isReleaseDay 切换两种文案）
- [ ] **COMMIT:** `feat(release): add countdown calculation service with 24h window logic`

## Task 3: GET /release-countdown API 端点 [P0]

**Files:**
- Create: `backend/src/controllers/release-countdown.ts`
- Create: `backend/src/routes/release-countdown.ts`
- Test: `backend/tests/controllers/release-countdown.test.ts`

**Spec coverage:** REQ-001 所有场景 + REQ-002 Scenario "释放规则未配置时不显示倒计时"

- [ ] **RED:** Write 5 failing tests — 释放前 24h 返回 show=true + 倒计时文案; 释放当天返回 isReleaseDay=true + 今日文案; 非释放时段返回 show=false; 规则未配置返回 show=false; 响应含 serverNow 字段（ISO 8601）
- [ ] **GREEN:** Implement `getReleaseCountdown` controller + Koa router `GET /api/v1/release-countdown` — 使用 DB `NOW()` 取服务器时间; 查 Redis 缓存（`release:countdown`，TTL 10s）; 未命中则查规则 + 调用 service 计算 + 写缓存; 规则未配置时记录 WARN 日志
- [ ] **COMMIT:** `feat(api): add GET /release-countdown endpoint with 10s redis cache`

## Task 4: 小程序首页倒计时组件 [P1]

**Files:**
- Create: `miniapp-user/src/components/release-countdown/index.tsx`
- Test: `miniapp-user/src/components/release-countdown/index.test.tsx`

**Spec coverage:** REQ-001 Scenarios "释放前 24 小时显示倒计时", "释放当天显示今日 10:00 开放下周预约", "非释放时段不显示倒计时"

- [ ] **RED:** Write 4 failing tests — show=true 且非释放当天时显示"距离下周预约开放还有"文案; show=true 且 isReleaseDay=true 时显示"今日 10:00 开放下周预约"; show=false 时组件不渲染（container.firstChild 为 null）; 每 10s 拉取一次 API 校准（fetch 被调用 2 次）
- [ ] **GREEN:** Implement `ReleaseCountdown` 组件 — `useEffect` 拉取 `/api/v1/release-countdown`; 每 10s `setInterval` 校准; 每秒本地递减 `remainingSeconds`; show=false 时 return null; 网络异常时 setData(null) 隐藏组件不影响首页
- [ ] **COMMIT:** `feat(miniapp): add release countdown component with 10s api calibration`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-3），P1 选做（Task 4）
