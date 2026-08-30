# Tasks: US-019 学员浏览正价套餐

## Task 1: Coach Package Service [P0]

**Files:**
- Create: `backend/src/services/coach-package.ts`
- Test: `backend/tests/services/coach-package.test.ts`

**Spec coverage:** REQ-001 标准套餐列表、参考单价、自定义课时入口开关、教练状态可见性校验

- [ ] **RED:** Return standard packages + reference price for valid coach (status=1); return customPackageEnabled=false when referencePrice is null; return COACH_NOT_FOUND for status ∉ {1, 4} or not bookable; return empty standardPackages when no active package template
- [ ] **GREEN:** Implement query with coach status filter IN (1, 4) + package_template.status=active filter
- [ ] **REFACTOR:** Extract coach status visibility rule into shared helper
- [ ] **COMMIT:** `feat(package): add coach package browsing service`

## Task 2: POST /api/coach/package/list [P0]

**Files:**
- Create: `backend/src/controllers/coach-package.ts`, `backend/src/routes/coach-package.ts`
- Test: `backend/tests/controllers/coach-package.test.ts`

**Spec coverage:** API 端点 + HTTP 状态码 + 错误码

- [ ] **RED:** 200 with full payload for valid coach; 200 with customPackageEnabled=false when no reference price; 200 with empty standardPackages array when no enabled package; 404 COACH_NOT_FOUND for invisible coach
- [ ] **GREEN:** Implement endpoint calling coach-package service
- [ ] **REFACTOR:** Share response DTO schema between controller and service
- [ ] **COMMIT:** `feat(api): add POST /api/coach/package/list`

## Task 3: Cache & Rate Limit [P1]

**Files:**
- Modify: `backend/src/services/coach-package.ts`
- Test: `backend/tests/services/coach-package.test.ts`

**Spec coverage:** 缓存命中、缓存失效、限流

- [ ] **RED:** Second call within 5min hits cache; cache invalidated when US-045/US-012 events fire; 429 returned when same IP exceeds 200 req/min
- [ ] **GREEN:** Implement Redis cache wrapper + IP rate limiter
- [ ] **REFACTOR:** Extract cache key builder and rate limiter into reusable utilities
- [ ] **COMMIT:** `feat(package): add 5min cache and rate limit for coach packages`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
