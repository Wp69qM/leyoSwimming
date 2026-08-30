## ADDED Requirements

### Requirement: REQ-001 Coach shall generate a shareable profile card with available slots
The system MUST allow an approved coach to generate a share card or poster containing their public profile and upcoming available schedule slots.

#### Scenario: Successful share card generation
- **WHEN** an approved coach with a complete profile and available slots taps "Share Profile"
- **THEN** the system returns a share scene, coach public data, and up to 3 upcoming available slots suitable for rendering a card or poster

### Requirement: REQ-002 Visitor shall view coach profile and slots from a share link
The system MUST allow a visitor to open a share link and view the coach's public profile and available slots without logging in.

#### Scenario: Visitor opens valid share landing page
- **WHEN** a visitor taps a valid share card with scene "coach=123&ts=1753879200"
- **THEN** the landing page displays the coach's name, certificates, years of teaching, rating, reference price, available slots, and "Book Trial" / "Buy Package" buttons

### Requirement: REQ-003 System shall prevent non-approved coaches from sharing
The system MUST hide or disable the share entry for coaches whose status is not approved.

#### Scenario: Pending coach cannot share
- **WHEN** a coach with status 0 (pending audit) opens the profile page
- **THEN** the "Share Profile" button is hidden or disabled and a tooltip states "Available after audit approval"

### Requirement: REQ-004 System shall handle the no-slots case gracefully
The system MUST allow share generation even when the coach has no available slots, and the landing page MUST show a fallback message instead of an empty slot list.

#### Scenario: Share with no available slots
- **WHEN** an approved coach generates a share card but has no available slots in the next 7 days
- **THEN** the card and landing page display "No available slots recently, follow and wait for release" and do not render an empty slot list

### Requirement: REQ-005 System shall reject invalid or expired share links
The system MUST validate the share scene and show an error page for expired or tampered links.

#### Scenario: Invalid share scene
- **WHEN** a visitor opens a share link with an expired or malformed scene
- **THEN** the landing page displays "Link expired, return to home" and a return-to-home button
