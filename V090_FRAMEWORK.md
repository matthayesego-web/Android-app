# Duck Force Companion v0.9.0 Framework

## Purpose
v0.9 turns the simplified v0.8 faction companion into a workflow/automation platform. This branch intentionally starts from the v0.8 blueprint so real Duck Force testing can change the v0.9 design before release.

## Release dependency
**Do not finalize v0.9 until v0.8 has been tested by Duck Force.**
Any navigation, permission, data-scope, or usability problems found in v0.8 should be corrected here before v0.9 is promoted.

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

Initial exception types:
- war requirement incomplete
- no OC / OC action needed
- inactive threshold exceeded
- traveling/abroad during critical period
- hospital/jail availability issue
- missing faction-defined requirement

Every exception must contain:
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

Must preserve explicit human review where Torn actions cannot be safely or legally automated.

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

No money movement is performed by the Android client. The Companion calculates, tracks and reconciles the workflow.

### 5. Armory Workflow
Extend the existing auditor into requests + verification.

Planned states:
`REQUESTED -> APPROVED -> ISSUED -> RETURN_DUE -> RETURNED` plus `REJECTED/CANCELLED`.

Use faction/player IDs and item IDs, with timestamps and leadership actor IDs. Existing audit views remain useful as verification evidence.

### 6. Faction Intel Provider Layer
Create one provider boundary so external intel does not leak vendor-specific logic throughout the app.

Planned providers:
- Torn native data
- FFScouter estimates / Fair Fight data (after terms/key flow is implemented)
- future optional intelligence providers

Each estimate presented to users should include source + freshness/confidence metadata where available.

### 7. Audit Log
Every shared leadership workflow should support an audit record:
- `faction_id`
- actor player ID
- action
- target player/request/entity ID
- before/after state where relevant
- timestamp
- source

This becomes critical before global multi-faction release.

## Multi-faction readiness
Duck Force remains the only production tenant during v0.9 testing, but backend/shared records must never assume a single faction.

Rules:
- `faction_id` is the tenant boundary.
- numeric `player_id` is the identity boundary.
- permission checks are evaluated inside the authenticated faction scope.
- no cross-faction fallback data.
- caches include faction + player/key scope.
- eventual web client will consume the same backend records and permissions.

## Proposed app layout after v0.8
### Home
Member obligations, personal war/OC/chain state, notices, while-away digest.

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

## Premium direction
Premium should enhance workflows rather than create a separate product area.
Candidates after core workflows are stable:
- advanced personal/faction history
- configurable smart alerts
- deeper war/member analytics
- advanced FFScouter history/comparison where permitted
- exports / retained reports
- custom automation rules

Core participation, requests, obligations and required faction workflows remain usable without premium.

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
v0.9 is not considered release-ready until:
- v0.8 navigation/permissions are confirmed in real use
- payout calculations are independently cross-checked
- banking does not duplicate requests or mark uncertain transactions as definitively paid
- audit records are faction-scoped
- member views cannot access leadership-only records
- production signing/update path remains stable
