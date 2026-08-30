> 本文档对应 `docs/stories/US-051-.../test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: Coach Repository — findCoachByUnionId / createCoach [P0]

**Files:**
- Create: `backend/src/repositories/coach.ts`
- Test: `backend/tests/repositories/coach.test.ts`

**Spec coverage:** REQ-001 Scenarios "未入驻教练首次授权登录", "已入驻通过教练授权登录", "待审核教练授权登录", "已驳回教练授权登录", "已离职教练重新登录"

- [ ] **RED:** Write failing tests — `findCoachByUnionId` returns coach with status=1/0/2; returns null when no coach record; treats status=3 as null; `createCoach` creates status=-1 record with phone/nickname.
- [ ] **GREEN:** Implement `CoachRepository.findCoachByUnionId(unionId)` — `WHERE union_id=? AND status != 3`; implement `CoachRepository.createCoach({ openid, unionId, phone, nickname })`.
- [ ] **COMMIT:** `feat(coach): add CoachRepository for direct coach login`

## Task 2: 扩展 WechatAuthService 以支持教练端 [P0]

**Files:**
- Modify: `backend/src/services/wechat-auth.ts`
- Test: `backend/tests/services/wechat-auth.test.ts`

**Spec coverage:** REQ-001 All Scenarios; REQ-003 Scenarios "教练端登录返回新增字段", "非法 app_type 校验"

- [ ] **RED:** Write failing tests — `app_type=coach` 且无 coach 记录返回 `coach_status=-1` 且 `is_new_coach=true`; `coach.status=1` 返回 `coach_status=1` 且 `is_new_coach=false`; `coach.status=0` 返回 `coach_status=0`; `coach.status=2` 返回 `coach_status=2` 并带 `rejection_reason`; `coach.status=3` 视为未命中并新建 coach; 未勾选协议抛 `TERMS_NOT_ACCEPTED`; 非法 `app_type` 抛 `VALIDATION_ERROR`; 不调用 user repo。
- [ ] **GREEN:** Extend `WechatAuthService.authenticate(...)` — when `appType='coach'`, validate terms/privacy, call `CoachRepository.findCoachByUnionId`, create coach if needed, return `is_new_coach`/`coach_status`; do not touch `user` repo。
- [ ] **REFACTOR:** Extract terms/privacy validation。
- [ ] **COMMIT:** `feat(auth): extend WechatAuthService to support independent coach login`

## Task 3: 改造 POST /auth/wechat-login 端点 [P0]

**Files:**
- Modify: `backend/src/controllers/auth.ts`
- Modify: `backend/src/routes/auth.ts`
- Test: `backend/tests/controllers/auth.test.ts`

**Spec coverage:** REQ-001 All Scenarios

- [ ] **RED:** Write failing tests — `app_type=coach` 未入驻返回 200 + `coach_status=-1`、`is_new_coach=true`; `app_type=coach` 已通过返回 200 + `coach_status=1`、`is_new_coach=false`; `app_type=coach` 缺失手机号加密数据返回 400; `app_type=coach` 未勾选协议返回 400 + TERMS_NOT_ACCEPTED; 非法 `app_type` 返回 400；幂等返回首次结果；拒绝授权/微信错误按 US-004 处理。
- [ ] **GREEN:** Implement `POST /api/coach/auth/wechat-login` controller — accept `code`, `encryptedData`, `iv`, `terms_accepted`, `privacy_accepted`, `app_type` body params, pass to service, return coach-specific fields。
- [ ] **COMMIT:** `feat(api): add POST /api/coach/auth/wechat-login for coach login`

## Task 4: 新增 POST /api/coach/status/detail 端点 [P0]

**Files:**
- Create: `backend/src/controllers/coach.ts`（或扩展已有 coach controller）
- Create: `backend/src/routes/coach.ts`
- Test: `backend/tests/controllers/coach.test.ts`

**Spec coverage:** REQ-002 Scenarios "已通过教练查询状态", "无 coach 记录时查询状态", "已驳回教练查询状态"

- [ ] **RED:** Write failing tests — 已登录已通过教练返回 coach_status=1、rejection_reason=null; 已登录无 coach 记录返回 coach_status=-1; 已登录已驳回教练返回 rejection_reason; 未登录返回 401。
- [ ] **GREEN:** Implement `POST /api/coach/status/detail` with auth middleware (coach_id from JWT), query coach by coach_id, return coach_status and rejection_reason。
- [ ] **COMMIT:** `feat(api): add POST /api/coach/status/detail`

## Task 5: 教练端登录页与路由守卫 [P1]

**Files:**
- Create: `miniapp-coach/src/pages/login/index.tsx`
- Create: `miniapp-coach/src/utils/auth.ts`（路由守卫）
- Test: `miniapp-coach/src/pages/login/index.test.tsx`

**Spec coverage:** REQ-001 Scenarios "教练拒绝微信授权", "未入驻教练首次授权登录"; REQ-002 登录态兜底

- [ ] **RED:** Write failing tests — 未勾选协议前端拦截不调用 wx.login(); 拒绝授权展示提示文案; 首次登录跳转入驻资料页; 已通过跳转教练首页; 已登录且 token 有效时打开小程序直接调用状态接口并跳转; token 过期调用刷新; refresh_token 过期重新登录。
- [ ] **GREEN:** Implement `CoachLoginPage` — check terms/privacy → call `Taro.login()` → `wx.getPhoneNumber()` → POST `/api/coach/auth/wechat-login` with `app_type=coach` → save tokens → redirect by `coach_status`; implement route guard to call `POST /api/coach/status/detail` when token exists and handle refresh/relogin。
- [ ] **COMMIT:** `feat(coach-miniapp): add login page and coach status route guard`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-4），P1 选做（Task 5）
- **教练端独立性**：Task 2 必须断言 `app_type=coach` 时不调用 user repo、不创建 user 记录、响应不含 `is_new_user`/`profile_completed`
- **协议校验**：Task 2/3 必须断言未勾选协议时返回 `TERMS_NOT_ACCEPTED`

## GWT Coverage Matrix

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 | Task 5 |
|---------|--------|--------|--------|--------|--------|
| 未入驻首次登录 | ✅ | ✅ | ✅ | — | ✅ |
| 已通过登录 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 待审核登录 | ✅ | ✅ | ✅ | — | — |
| 拒绝授权 | — | — | — | — | ✅ |
| 已离职重新登录 | ✅ | ✅ | ✅ | — | — |
| 已驳回登录 | ✅ | ✅ | ✅ | ✅ | — |
| 未勾选协议 | — | ✅ | ✅ | — | ✅ |
| 登录后 token 管理 | — | — | — | — | ✅ |
| /coach/me/status 查询 | — | — | — | ✅ | — |
