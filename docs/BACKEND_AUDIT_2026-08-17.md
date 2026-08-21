# TornFCA Backend Audit — 2026-08-17

Branch audited: `v0.9-faction-automation`
Current pre-1.0 app line: **v0.10.1 / versionCode 56**

This audit describes source state. A service is not live until its Apps Script deployment passes the live-backend workflow and device smoke test.

## Architecture now in source

### 1. Shared faction backend — `backend/AccessBackend.gs`
Version: **1.1.0**
Android client: `CompanionBackendClient`

Server-backed:
- verified player + faction identity
- faction-position capability sync/cache
- rank access rules and per-user overrides
- faction notices
- banking requests/reconciliation
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
Version: **1.6.0**
Android client: `CommunityBackendClient`

Server-backed:
- faction chat
- chat reports
- community moderation queue/resolution
- training rules/guides/resources
- push-device registration/tests/announcement pushes

Security/state:
- every authenticated request re-reads current Torn faction membership/position before selecting tenant data
- only stable basic player identity may be cached briefly by SHA-256 API-key fingerprint
- all community/training data is faction-scoped
- report dedupe, moderation resolution, training writes and device registration are ScriptLock-protected
- moderation defaults to verified TornFCA owner recovery/global access
- future faction moderator access is configurable by actual Torn position abilities and/or Leader/Co-leader capability; custom position-name strings are not the permission model
- non-owner moderators can only see/resolve reports in their verified faction
- exact final moderation capability matrix remains intentionally undecided
- launch-safe defaults: `MODERATION_ALLOW_LEADERS=false`, blank `MODERATION_ABILITIES`

Scale hardening:
- chat polling reads bounded 500-row chunks and stops after 75 matching messages or 5,000 global recent rows
- chat polling no longer calls `getDataRange()` on the entire historical message sheet
- exact message/report lookups use TextFinder-backed row searches
- duplicate report checks read only the recent report tail rather than the full report history
- this is a hot-path optimization, not a claim that Apps Script has been load-tested for thousands of simultaneous users

### 3. Premium backend — `backend/TornFcaPremiumBackend.gs`
Version: **1.2.0**
Android client: `PremiumBackendClient`
Android entitlement gate: `PremiumAccess`

Server-backed:
- verified Premium entitlement status
- owner/admin test grants
- configurable Premium policy
- optional one-minute Xanax payment scan, but only after explicit monetization approval activation
- dedupe/idempotency by Torn receipt/log ID

Security/state:
- entitlement reads are limited to the verified signed-in Torn player
- admin changes require verified TornFCA owner ID plus admin password
- client API keys are never persisted
- `OWNER_API_KEY` is a server Script Property used only by the automatic payment scanner
- scanner is ScriptLock-protected
- grant source `XANAX_LOG_4103:<logId>` makes receipt processing idempotent if a run fails after entitlement extension but before the payment/audit row is written
- `stacking` is enforced: enabled extends unexpired time; disabled starts new paid time from now
- password bootstrap hashes `PREMIUM_ADMIN_PASSWORD_SETUP` and deletes the plaintext setup property in a `finally` block

Monetization fail-closed gate:
- setup defaults `MONETIZATION_APPROVED=false`
- `installPremiumScanTrigger()` refuses while the flag is false
- `scanPremiumPayments()` also refuses while the flag is false
- this permits full Free/Premium entitlement testing through owner manual grants without accidentally accepting real paid usage
- the flag should only be enabled after the Torn charging-approval requirement and the chosen distribution/payment path are settled

Android Premium hardening:
- `PremiumEntitlementStore` contains backend-verified entitlement state only
- owner simulation defaults OFF and is accepted only by `PremiumAccess` for the verified developer player ID
- remote `disable_premium`, maintenance and minimum-version policy override entitlement/simulation
- Premium routes use the central gate
- Member Dossier and Faction Pulse self-gate before loading data so future internal navigation cannot bypass the matrix
- Free leadership keeps a 7-day Activity Tracker; Premium receives the deeper 30-day history view

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
- remote policy is cached locally and refreshed in the background; networking does not block app startup
- developer route remains a recovery path if normal features are remotely disabled
- password bootstrap hashes `DEVELOPER_ADMIN_PASSWORD_SETUP` and deletes plaintext afterward

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
- faction/war upsert reads under ScriptLock, preventing simultaneous first saves from appending duplicate records
- local receipt is saved first; cloud upload is best-effort and never breaks payout calculation
- opening WarPay starts a best-effort faction receipt refresh

## Android permission freshness

`TornApiClient.authenticateFreshFaction()` now invalidates cached faction identity, faction alias, position abilities and the cached AuthSession before leadership-only navigation is authorized.

The guarded Android routes are:
- Activity Tracker
- Faction Pulse
- Member Dossier
- WarPay

This re-verification occurs on tool entry rather than on every ordinary navigation action. Leadership authorization is evaluated before Premium authorization, so Premium cannot create faction authority. `tornfca-permission-freshness-audit.yml` protects this invariant.

## Local-only by design

These do not need a TornFCA server merely because they use local persistence:
- developer simulations/diagnostic preferences
- training battle-stat baseline/progress baseline
- saved notification inbox/history
- legal acceptance
- ordinary app preferences
- caches/session state
- blocked-user UI state
- War Prep confirmation checklist
- faction onboarding checklist
- banking outage drafts that synchronize when connectivity returns

## Direct Torn data — no TornFCA persistence required

These primarily compute/display current Torn data and should continue using the rate-limited Torn client instead of copying Torn into our database without a product reason:
- current member/profile/faction views
- ranked-war and territory status/history
- OC views
- faction activity/pulse/strength calculations
- live bars/cooldowns/refills/travel data
- command dashboard gauges

FFScouter/TornStats remain optional external integrations, not TornFCA data stores. Basic provider access is not a TornFCA Premium paywall; Premium may add convenience/aggregation around separately authorized provider data.

## Monetization boundary

Authoritative matrix: `docs/PREMIUM_MATRIX_0.10.1.md`.

Free protects:
- basic WarPay/current receipt persistence
- current Banking workflow
- current Armory Auditor
- Company Train Calculator
- basic FFScouter/TornStats access after provider consent/entitlement
- core war/territory/chain/OC/member/training/community functions
- safety/moderation and legal/security controls
- essential permission-aware faction administration

Premium is convenience/depth: longer history, Personal Insights, advanced alert timing, Faction Pulse, all-in-one Member Dossier and future saved/automated/export workflow.

## Legal/API disclosure state

Legal acknowledgement version is **`2026-08-17-v4`**.

The API-key entry surface explicitly discloses:
- Data Storage
- Data Sharing
- Purpose
- Key Storage & Sharing
- Key Access Level

The in-app Privacy Policy further documents cloud faction workflow data, aggregate user telemetry, optional providers and the no-client-key-persistence design. Material changes require renewed acknowledgement.

## Automated source/release gates

- `.github/workflows/tornfca-premium-matrix-audit.yml`
- `.github/workflows/tornfca-pre1-source-audit.yml`
- `.github/workflows/tornfca-permission-freshness-audit.yml`
- `.github/workflows/tornfca-member-core-audit.yml`
- `.github/workflows/tornfca-community-security-audit.yml`
- `.github/workflows/tornfca-beta-overhaul-audit.yml`
- `.github/workflows/tornfca-current-shell-audit.yml`
- `.github/workflows/tornfca-mobile-navigation-audit.yml`
- `.github/workflows/tornfca-backend-live-audit.yml`
- `.github/workflows/tornfca-cloud-candidate.yml`

Push-triggered audits protect source invariants. Live-backend/cloud-candidate workflows remain intentionally blocked until Google Apps Script deployment URLs/secrets exist and identify themselves at the exact audited versions.

## Deployment boundary

Each Apps Script service must be deployed and its HTTPS URL supplied through:

- `TORNFCA_FACTION_BACKEND_URL`
- `TORNFCA_COMMUNITY_BACKEND_URL`
- `TORNFCA_PREMIUM_BACKEND_URL`
- `TORNFCA_DEVELOPER_BACKEND_URL`
- `TORNFCA_WARPAY_BACKEND_URL`

Firebase values remain separate configuration. See `docs/V1_BACKEND_GO_LIVE.md`.

## Still intentionally pending

- final Torn capability matrix for Community Moderation/custom-position moderator access
- any decision to cloud-sync personal checklist/baseline state (currently intentionally local)
- legacy faction-chat/listener compatibility cleanup if a future breaking release warrants it
- live deployment of all five Apps Script services
- signed v0.10.1 Beta + on-device Free/Premium/permission-change smoke test
- monetization approval/payment-path decision before enabling automatic paid scanning
- actual load/concurrency testing against deployed Apps Script services
- final promotion decision to v1.0.0/main only after live gates pass
