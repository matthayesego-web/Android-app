# Duck Force Companion v0.9.0 Framework

## Purpose
v0.9 turns the simplified v0.8 faction companion into a workflow/automation platform. It inherits the tested v0.8 Home / Faction / Leadership structure and increasingly answers: **Who needs attention and why?**

## Version policy
- v0.9.0 is the first wider Duck Force beta.
- Fixes and beta iterations continue as v0.9.1, v0.9.2, v0.9.3, etc.
- v1.0.0 is reserved for the first public store-ready release and must not be used until release gates are complete.

## Pre-release access
v0.9 beta builds use a preview access-code gate before Torn sign-in. The plaintext code must never be committed to GitHub. Torn API authentication and Duck Force membership verification remain a second independent access layer. The preview gate is removed/replaced by the production onboarding model before v1.0.0.

## Core v0.9 systems

### 1. Faction Task / Obligation Engine
One normalized model for requirements shown to members and exceptions shown to leadership.

Planned record:
- `faction_id`
- `task_id`
- `task_type`
- `title`
- `description`
- `target_scope` (all / role / player)
- `target_value`
- `starts_at`
- `due_at`
- `status`
- `created_by_player_id`
- `created_at`

Examples:
- minimum ranked-war hits
- chain participation
- training period requirement
- OC assignment/readiness action
- announcement acknowledgement

Member UI: progress only.
Leadership UI: completion + exception list.

### 2. Exception Engine
Instead of leadership checking every member, derive only actionable exceptions.

Initial v0.9.0 live preview signals:
- no OC assignment
- inactive 72+ hours (preview threshold)
- traveling/abroad/hospitalized/jailed
- zero ranked-war hits recorded during a live war when detailed faction attacks are available

Planned configurable exception types:
- war requirement incomplete
- no OC / OC action needed
- inactive threshold exceeded
- traveling/abroad during critical period
- hospital/jail availability issue
- missing faction-defined requirement

Every durable exception will contain:
- `faction_id`
- `player_id`
- reason/type
- severity
- detected_at
- source/freshness
- resolved state

### 3. Banking Workflow v2
Keep the existing queue and reconciliation work as the base. v0.9 goal is to reduce duplicate/manual processing, not bypass Torn.

Planned flow:
1. member submits request
2. backend deduplicates/fingerprints
3. leadership sees normalized queue
4. Torn-side transaction is performed by authorized user
5. reconciliation verifies likely completion
6. request moves to handled/paid with audit trail

Existing banking behavior remains untouched until banking-specific v0.9 work begins.

### 4. War Payout Workflow
New leadership module.

Planned inputs:
- ranked-war report
- per-member attacks / score / participation
- configurable payout policy
- manual bonuses/penalties
- optional minimum participation rule

Planned outputs:
- member-by-member payout calculation
- total payout liability
- exception list
- copy/export summary
- payout status tracking
- immutable calculation snapshot / audit record

No money movement is performed by the Android client.

### 5. Armory Workflow
Extend the existing auditor into requests + verification.

Planned states:
`REQUESTED -> APPROVED -> ISSUED -> RETURN_DUE -> RETURNED` plus `REJECTED/CANCELLED`.

Use faction/player IDs and item IDs, with timestamps and leadership actor IDs. Existing audit views remain useful as verification evidence.

### 6. Faction Intel Provider Layer
Create one provider boundary so external intel does not leak vendor-specific logic throughout the app.

Planned providers:
- Torn native data
- FFScouter estimates / Fair Fight data
- future optional intelligence providers

Each estimate presented to users must include source + freshness/confidence metadata where available and must not be presented as exact stats.

### 7. Audit Log
Every shared leadership workflow should support an audit record:
- `faction_id`
- actor player ID
- action
- target player/request/entity ID
- before/after state where relevant
- timestamp
- source

## Multi-faction readiness
Duck Force remains the only production tenant during v0.9 testing, but backend/shared records must never assume a single faction.

Rules:
- `faction_id` is the tenant boundary.
- numeric `player_id` is the identity boundary.
- permission checks are evaluated inside the authenticated faction scope.
- no cross-faction fallback data.
- caches include faction + player/key scope.
- eventual web client will consume the same backend records and permissions.

## App layout
### Home
Member obligations, personal war/OC/chain state, notices, while-away digest, and leadership attention signal when authorized.

### Faction
Shared member-safe overview, Intel, participation, requests/tools.

### Leadership
- Command / Exceptions
- Members
- War + Payouts
- OC
- Banking
- Armory
- Notices / Goals / Access

## v0.9 queue order
1. Apply all lessons/fixes from v0.8 testing.
2. Task/Obligation + Exception models.
3. War payout calculator/workflow.
4. Banking reconciliation hardening.
5. Armory request lifecycle.
6. FFScouter provider integration proof-of-concept.
7. Shared audit log.
8. Notification hooks.
9. Duck Force beta validation.

## Release gate
v0.9 is not considered final until:
- navigation/permissions are confirmed in real use
- payout calculations are independently cross-checked
- banking does not duplicate requests or mark uncertain transactions as definitively paid
- audit records are faction-scoped
- member views cannot access leadership-only records
- production signing/update path remains stable

v1.0.0 additionally requires public-release onboarding, store assets/policy review, privacy/data disclosures, and removal or replacement of the private beta access gate.
