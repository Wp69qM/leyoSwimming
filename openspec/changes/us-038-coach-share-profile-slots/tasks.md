## 1. Setup

- [ ] 1.1 Create `coach_share_log` migration (optional but recommended)
- [ ] 1.2 Add `coach-share` and `public-coach` route modules
- [ ] 1.3 Add share entry UI in coach miniapp profile page

## 2. Share Data Generation

- [ ] 2.1 Implement `GET /api/coach/v1/share/profile-slots` — maps to REQ-001 / Scenario: Successful share card generation
- [ ] 2.2 Enforce `coach.status = 1` guard — maps to REQ-003 / Scenario: Pending coach cannot share
- [ ] 2.3 Return fallback data when no slots exist — maps to REQ-004 / Scenario: Share with no available slots

## 3. Public Landing APIs

- [ ] 3.1 Implement `GET /api/public/v1/coaches/{id}/share` with field whitelist — maps to REQ-002 / Scenario: Visitor opens valid share landing page
- [ ] 3.2 Implement `GET /api/public/v1/coaches/{id}/available-slots`
- [ ] 3.3 Validate scene and reject invalid/expired links — maps to REQ-005 / Scenario: Invalid share scene

## 4. Privacy & Security

- [ ] 4.1 Ensure `phone` and `wechat_qr` are never returned by public APIs
- [ ] 4.2 Add IP rate limiting to public endpoints (100 req/min)
- [ ] 4.3 Add scene signature verification

## 5. Frontend

- [ ] 5.1 Build coach profile share button and menu
- [ ] 5.2 Build visitor landing page with profile, slots, and CTA buttons
- [ ] 5.3 Implement invalid/expired link error page

## 6. Verification

- [ ] 6.1 Run unit tests for scene codec and field whitelist
- [ ] 6.2 Run integration tests for all 5 GWT scenarios
- [ ] 6.3 Run `openspec validate us-038-coach-share-profile-slots --json` and fix issues
