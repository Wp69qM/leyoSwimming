# Test Plan: US-023 学员候补与关注时段

## Task 1: Waitlist Service [P0]

**Files:**
- Create: `backend/src/services/waitlist.ts`
- Test: `backend/tests/services/waitlist.test.ts`

**Spec coverage:** 加入候补、时段未满、无 active 套餐、重复候补

- [ ] **RED:** Join full slot; reject not-full / no-package / duplicate
- [ ] **GREEN:** Implement waitlist creation and eligibility rules
- [ ] **REFACTOR:** Extract slot capacity checker
- [ ] **COMMIT:** `feat(waitlist): add join waitlist service`

## Task 2: Follow Service [P0]

**Files:**
- Create: `backend/src/services/slot-follow.ts`
- Test: `backend/tests/services/slot-follow.test.ts`

**Spec coverage:** 关注时段

- [ ] **RED:** Follow slot creates record; 401 guest
- [ ] **GREEN:** Implement follow creation
- [ ] **REFACTOR:** Extract follow repository
- [ ] **COMMIT:** `feat(follow): add slot follow service`

## Task 3: Waitlist/Follow APIs [P0]

**Files:**
- Create: `backend/src/controllers/waitlist.ts`, `backend/src/routes/waitlist.ts`
- Create: `backend/src/controllers/slot-follow.ts`, `backend/src/routes/slot-follow.ts`
- Test: `backend/tests/controllers/waitlist.test.ts`, `backend/tests/controllers/slot-follow.test.ts`

- [ ] **RED:** 201 join/follow; 204 cancel; 400 for invalid cases
- [ ] **GREEN:** Implement endpoints
- [ ] **REFACTOR:** Share auth and ownership checks
- [ ] **COMMIT:** `feat(api): add waitlist and follow endpoints`

## Task 4: My Waitlist & Follows Query [P0]

**Files:**
- Create: `backend/src/services/my-waitlist-follows.ts`
- Test: `backend/tests/services/my-waitlist-follows.test.ts`

- [ ] **RED:** Returns user's waitlist and follows grouped
- [ ] **GREEN:** Implement query
- [ ] **REFACTOR:** Combine repositories
- [ ] **COMMIT:** `feat(api): add GET /api/users/me/waitlist-and-follows`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
