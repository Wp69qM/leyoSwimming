# Test Plan: US-024 学员候补自动转正

## Task 1: Waitlist Promotion Service [P0]

**Files:**
- Create: `backend/src/services/waitlist-promotion.ts`
- Test: `backend/tests/services/waitlist-promotion.test.ts`

**Spec coverage:** 首位候补转正、多人仅首位、跳过无课时、跳过已取消

- [ ] **RED:** Promote first waiting user; skip no-quota / cancelled; only one when multiple waiting
- [ ] **GREEN:** Implement promotion logic with eligibility checks and package reservation
- [ ] **REFACTOR:** Extract first-eligible finder and package reservation helper
- [ ] **COMMIT:** `feat(waitlist): add auto-promotion service`

## Task 2: Booking Cancelled Event Consumer [P0]

**Files:**
- Create: `backend/src/consumers/booking-cancelled-consumer.ts`
- Test: `backend/tests/consumers/booking-cancelled-consumer.test.ts`

**Spec coverage:** 取消事件触发转正流程

- [ ] **RED:** Consumer receives `BookingCancelledEvent` and calls promotion service
- [ ] **GREEN:** Implement event handler and transaction wrapper
- [ ] **REFACTOR:** Extract event payload validator
- [ ] **COMMIT:** `feat(waitlist): consume booking cancelled events`

## Task 3: Follower Notification on Release [P1]

**Files:**
- Create: `backend/src/services/slot-release-notifier.ts`
- Test: `backend/tests/services/slot-release-notifier.test.ts`

**Spec coverage:** 无候补时通知关注用户

- [ ] **RED:** No waitlist but followers exist → send release notification; no notification if no followers
- [ ] **GREEN:** Implement follower query and notification creation
- [ ] **REFACTOR:** Reuse notification repository
- [ ] **COMMIT:** `feat(notification): notify followers when slot released`

## Task 4: Cancel Other Waitlists on Promotion [P0]

**Files:**
- Modify: `backend/src/services/waitlist-promotion.ts`
- Test: `backend/tests/services/waitlist-promotion.test.ts`

**Spec coverage:** 用户其他 waiting 候补自动取消

- [ ] **RED:** When user is promoted, other waiting waitlists for same user are cancelled
- [ ] **GREEN:** Implement cascade cancellation within promotion transaction
- [ ] **REFACTOR:** Extract waitlist cancellation helper
- [ ] **COMMIT:** `feat(waitlist): cancel other waitlists on promotion`

## Task 5: Concurrency & Integration [P0]

**Files:**
- Test: `backend/tests/integration/waitlist-promotion.concurrency.test.ts`

**Spec coverage:** 并发取消与候补加入、同一用户多 slot 同时释放

- [ ] **RED:** Concurrent promotion attempts do not overbook slot; only one booking created per released seat
- [ ] **GREEN:** Add row-level locking on waitlist / schedule_slot in promotion transaction
- [ ] **REFACTOR:** Extract concurrency test fixtures
- [ ] **COMMIT:** `test(waitlist): add promotion concurrency tests`

---

## Execution Discipline

- 严格顺序：Task 1 → Task 2 → Task 3 → Task 4 → Task 5
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
