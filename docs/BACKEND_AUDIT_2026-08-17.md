# TornFCA Backend Audit — 2026-08-17

Branch audited: `v0.9-faction-automation`
Current pre-1.0 app line: **v0.10.1 / versionCode 56**

This audit describes source state. A service is not considered live until its Apps Script deployment passes the live-backend workflow and device smoke test.

## Architecture now in source

### 1. Shared faction backend — `backend/AccessBackend.gs`
Version: **1.1.0**
Android client: `CompanionBackendClient`

Server-backed:
- verified player + faction identity
- faction-position capability sync/cache
- rank access rules and per-user overrides
- faction notices
- banking requests
- banking reconciliation
- faction-scoped banking listener tokens

Security/state:
- shared rows are tenant-scoped by verified `faction_id`
- client API keys are never stored server-side
- faction membership and position are read fresh from Torn on every authenticated request
- only stable basic player identity may be cached briefly using an API-key SHA-256 fingerprint
- sensitive banking/notice writes re-check current Torn position abilities
- faction listener-token rotation and position replacement are ScriptLock-protected
- legacy single-faction settings/listener token remain for migration compatibility

### 2. Community backend — `backend/TornFcaCommunityBackend.gs`
Version: **1.4.0**
Android client: `CommunityBackendClient`

Server-backed:
- faction chat
- chat reports
- community moderation queue/resolution
- training rules
- training guides/resources
- push-device registration
- push tests
- announcement pushes

Security/state:
- all community/training data is faction-scoped
- moderation defaults to verified TornFCA owner access
- future faction moderator access is configurable by actual Torn position abilities and/or Leader/Co-leader capability; custom position-name strings are not used as the permission model
- non-owner moderators can only see/resolve reports in their verified faction
- the exact final moderation permission matrix remains intentionally undecided
- launch-safe defaults remain `MODERATION_ALLOW_LEADERS=false` and blank `MODERATION_ABILITIES`

### 3. Premium backend — `backend/TornFcaPremiumBackend.gs`
Version: **1.1.0**
Android client: `PremiumBackendClient`
Android entitlement gate: `PremiumAccess`

Server-backed:
- verified Premium entitlement status
- one-minute Xanax payment scan
- dedupe/idempotency by Torn receipt/log ID
- Premium configuration
- owner/admin grants

Security/state:
- entitlement reads are limited to the verified signed-in Torn player
- admin changes require verified TornFCA owner ID plus admin password
- client API keys are never persisted
- `OWNER_API_KEY` remains a server Script Property used only by the payment scanner
- scanner is ScriptLock-protected
- grant source `XANAX_LOG_4103:<logId>` makes receipt processing idempotent if a run fails after entitlement extension but before the audit/payment row is written
- `stacking` is actually enforced: enabled extends unexpired time; disabled starts the new grant from the current time
- safe password bootstrap hashes `PREMIUM_ADMIN_PASSWORD_SETUP` and deletes the plaintext setup property in a `finally` block

Android Premium hardening:
- `PremiumEntitlementStore` contains backend-verified entitlement state only
- owner simulation defaults OFF and is accepted only by `PremiumAccess` for the verified developer player ID
- remote `disable_premium`, maintenance and minimum-version policy override entitlement/simulation
- Premium target routes use the central gate
- Member Dossier and Faction Pulse also self-gate so a future internal navigation change cannot bypass the matrix
- Free leadership keeps a 7-day Activity Tracker; Premium may select the deeper 14/30-day window

### 4. Developer control plane — `backend/TornFcaDeveloperBackend.gs`
Version: **1.3.0**
Android client: `DeveloperBackendClient`
Android cache/enforcement: `RemoteFeaturePolicy`
Protected UI: `DeveloperBackendActivity`

Server-backed:
- maintenance mode
- minimum supported `versionCode`
- Beta/operator message
- emergency remote switches for Activity, War/WarPay, Chain, OC, Pulse, Lookup and Premium
- append-only developer mutation audit log
- aggregate user telemetry

User telemetry:
- total unique verified users since Developer backend go-live
- current users = verified users active in the last 24 hours
- one telemetry write at most every six hours per user
- stores salted hash of player ID, first/last seen and app version only
- does not store Torn API key or player name in telemetry

Security/state:
- non-secret public product policy still requires a valid Torn identity
- status/config writes/audit require verified developer Torn ID `3987363`
- writes additionally require developer password
- API keys are never persisted
- remote policy is cached locally and refreshed in the background; networking never blocks app startup
- developer route remains available as a recovery path if normal features are remotely disabled
- safe password bootstrap hashes `DEVELOPER_ADMIN_PASSWORD_SETUP` and deletes the plaintext setup property afterward

### 5. WarPay persistence backend — `backend/TornFcaWarPayBackend.gs`
Version: **1.1.0**
Android client: `WarPayBackendClient`
Local/offline cache: `WarPayoutReceiptStore`

Server-backed:
- calculated WarPay receipts
- cross-device retrieval for current faction leadership

Security/state:
- every request reads current Torn faction membership/position fresh
- stable basic identity may be cached briefly by API-key fingerprint
- receipts are isolated by `faction_id`
- current access intentionally matches WarPay UI: Leader/Co-leader only
- receipt payload and row count are validated before storage
- faction/war upsert reads under ScriptLock, preventing two simultaneous first saves from appending duplicate records
- local receipt is saved first; cloud upload is best-effort and never breaks payout calculation
- opening WarPay starts a best-effort faction receipt refresh

## Local-only by design

These do not need a TornFCA server merely because they use local persistence:
- developer simulations and diagnostic preferences
- training battle-stat baseline/progress baseline
- saved notification inbox/history on the device
- legal acceptance
- ordinary app preferences
- caches/session state
- blocked-user UI state
- War Prep confirmation checklist
- faction onboarding checklist
- banking outage drafts (already synchronize to the shared faction backend when connectivity returns)

## Direct Torn data — no TornFCA persistence required

The following tools primarily compute/display current Torn data and should continue using the rate-limited Torn client rather than copying Torn into our own database without a product reason:
- current member/profile/faction views
- ranked-war and territory status/history
- OC views
- faction activity/pulse/strength calculations
- live bars/cooldowns/refills/travel data
- Beta live dashboard gauges

External services such as TornStats/FFScouter remain external integrations, not TornFCA data stores. Basic provider access is not a TornFCA Premium paywall; Premium may add convenience/aggregation around separately authorized provider data.

## Monetization boundary

The authoritative v0.10.1 matrix is `docs/PREMIUM_MATRIX_0.10.1.md`.

Free protects:
- basic WarPay
- current Banking workflow
- current Armory Auditor
- Company Train Calculator
- basic FFScouter/TornStats access after provider consent/entitlement
- core war/territory/chain/OC/member/training/community functions
- safety/moderation and legal/security controls
- essential permission-aware faction administration

Premium is the convenience/depth tier: longer history, Personal Insights, advanced alert timing, Faction Pulse, all-in-one Member Dossier, and future saved/automated/export workflow.

## Automated source gates

- `.github/workflows/tornfca-premium-matrix-audit.yml`
- `.github/workflows/tornfca-pre1-source-audit.yml`
- `.github/workflows/tornfca-beta-overhaul-audit.yml`
- `.github/workflows/tornfca-backend-live-audit.yml`
- `.github/workflows/tornfca-cloud-candidate.yml`

The push-triggered audits protect source invariants. The live-backend and cloud-candidate workflows are intentionally blocked until the Google Apps Script deployment URLs/secrets exist and identify themselves at the exact audited backend versions.

## Deployment boundary

Repository source is now separated into deliberate backend services. A backend is not considered live merely because its `.gs` source exists in GitHub. Each Apps Script service must be deployed and its HTTPS URL supplied to the Android build through the matching environment variable:

- `TORNFCA_FACTION_BACKEND_URL`
- `TORNFCA_COMMUNITY_BACKEND_URL`
- `TORNFCA_PREMIUM_BACKEND_URL`
- `TORNFCA_DEVELOPER_BACKEND_URL`
- `TORNFCA_WARPAY_BACKEND_URL`

Firebase values remain separate build/deployment configuration. See `docs/V1_BACKEND_GO_LIVE.md` for the exact manual Google sequence.

## Still intentionally pending

- final Torn capability matrix for Community Moderation and any wider custom-position leadership policy
- any decision to cloud-sync personal checklist/baseline state (currently intentionally local)
- legacy faction-chat/listener compatibility cleanup if a later breaking release warrants it
- live deployment of all five Apps Script services
- signed v0.10.1 Beta + on-device Free/Premium matrix smoke test
- final promotion decision to v1.0.0/main only after live gates pass
