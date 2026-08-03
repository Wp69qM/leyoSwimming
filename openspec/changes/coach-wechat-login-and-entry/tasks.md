> 本文档对应 `docs/stories/US-051-.../test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: Coach Repository — findCoachByUserId [P0]

**Files:**
- Create: `backend/src/repositories/coach.ts`
- Test: `backend/tests/repositories/coach.test.ts`

**Spec coverage:** REQ-001 Scenarios "未入驻教练首次授权登录", "已入驻通过教练授权登录", "待审核教练授权登录", "已驳回教练授权登录"

- [ ] **RED:** Write 4 failing tests — `findCoachByUserId` returns coach with status=1; returns coach with status=0; returns null when no coach record; returns rejection_reason when status=2
- [ ] **GREEN:** Implement `CoachRepository.findCoachByUserId(userId)` — `WHERE user_id=?`
- [ ] **COMMIT:** `feat(coach): add CoachRepository.findCoachByUserId for login redirect`

## Task 2: 扩展 WechatAuthService 以支持教练端 [P0]

**Files:**
- Modify: `backend/src/services/wechat-auth.ts`
- Test: `backend/tests/services/wechat-auth.test.ts`

**Spec coverage:** REQ-001 All Scenarios; REQ-003 Scenarios "教练端登录返回新增字段", "非法 app_type 校验"

- [ ] **RED:** Write 5 failing tests — `app_type=coach` 且无 coach 记录返回 `redirect_page=coach_onboarding`; `coach.status=1` 返回 `coach_home`; `coach.status=0` 返回 `coach_pending`; `coach.status=2` 返回 `coach_rejected` 并带 rejection_reason; 非法 `app_type` 抛 `VALIDATION_ERROR`
- [ ] **GREEN:** Extend `WechatAuthService.authenticate(code, appType)` — validate `appType`, call `CoachRepository.findCoachByUserId`, compute `redirect_page`, return `coach_status` + `redirect_page` when `app_type=coach`
- [ ] **REFACTOR:** Extract `resolveCoachRedirectPage(coachStatus, rejectionReason)` pure function
- [ ] **COMMIT:** `feat(auth): extend WechatAuthService to support coach login redirect`

## Task 3: 改造 POST /auth/wechat-login 端点 [P0]

**Files:**
- Modify: `backend/src/controllers/auth.ts`
- Modify: `backend/src/routes/auth.ts`
- Test: `backend/tests/controllers/auth.test.ts`

**Spec coverage:** REQ-001 All Scenarios; REQ-003 Scenario "用户端登录保持原行为"

- [ ] **RED:** Write 6 failing tests — `app_type=coach` 未入驻返回 200 + redirect_page; `app_type=coach` 已通过返回 200 + coach_home; `app_type=user` 保持 US-004 响应格式；非法 `app_type` 返回 400；幂等返回首次结果；拒绝授权/微信错误按 US-004 处理
- [ ] **GREEN:** Update controller to accept `app_type` body param, pass to service, conditionally include `coach_status`/`redirect_page` in response
- [ ] **COMMIT:** `feat(api): extend POST /auth/wechat-login with app_type and coach redirect`

## Task 4: 新增 GET /api/v1/coach/me/status 端点 [P0]

**Files:**
- Create: `backend/src/controllers/coach.ts`（或扩展已有 coach controller）
- Create: `backend/src/routes/coach.ts`
- Test: `backend/tests/controllers/coach.test.ts`

**Spec coverage:** REQ-002 Scenarios "已通过教练查询状态", "无 coach 记录时查询状态"

- [ ] **RED:** Write 3 failing tests — 已登录已通过教练返回 coach_status=1 + coach_home; 已登录无 coach 记录返回 null + coach_onboarding; 未登录返回 401
- [ ] **GREEN:** Implement `GET /api/v1/coach/me/status` with auth middleware, query coach by user_id, return redirect_page
- [ ] **COMMIT:** `feat(api): add GET /api/v1/coach/me/status`

## Task 5: 教练端登录页与路由守卫 [P1]

**Files:**
- Create: `miniapp-coach/src/pages/login/index.tsx`
- Create: `miniapp-coach/src/utils/auth.ts`（路由守卫）
- Test: `miniapp-coach/src/pages/login/index.test.tsx`

**Spec coverage:** REQ-001 Scenarios "教练拒绝微信授权", "未入驻教练首次授权登录"; REQ-002 登录态兜底

- [ ] **RED:** Write 5 failing tests — 拒绝授权展示提示文案; 首次登录跳转入驻资料页; 已通过跳转教练首页; 已登录且 token 有效时打开小程序直接调用状态接口并跳转; 未同意隐私协议时跳转隐私协议页（US-009），同意后再按 redirect_page 跳转
- [ ] **GREEN:** Implement `CoachLoginPage` — call `Taro.login()` → POST `/auth/wechat-login` with `app_type=coach` → save tokens → check privacy consent (US-009 `/api/user/privacy/status`) → redirect to privacy page or by `redirect_page`; implement route guard to call `/coach/me/status` when token exists
- [ ] **COMMIT:** `feat(coach-miniapp): add login page and coach status route guard`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-4），P1 选做（Task 5）
- **用户端兼容性**：Task 3 必须断言 `app_type=user` 时响应与 US-004 完全一致
- **隐私协议前置**：Task 5 必须断言登录成功后未同意隐私协议时跳转 US-009 隐私协议页，已同意时按 `redirect_page` 跳转

## GWT Coverage Matrix

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 | Task 5 |
|---------|--------|--------|--------|--------|--------|
| §6.1 未入驻首次登录 | ✅ | ✅ | ✅ | — | ✅ |
| §6.2 已通过登录 | ✅ | ✅ | ✅ | — | ✅ |
| §6.3 待审核登录 | ✅ | ✅ | ✅ | — | — |
| §6.4 拒绝授权 | — | — | — | — | ✅ |
| §6.5 用户端账号进入教练端 | — | ✅ | ✅ | — | ✅ |
| §6.6 已驳回登录 | ✅ | ✅ | ✅ | — | — |
| §6.7 未同意隐私协议 | — | — | — | — | ✅ |
