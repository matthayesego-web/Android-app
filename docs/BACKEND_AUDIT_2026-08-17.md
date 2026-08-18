# TornFCA Backend Audit — 2026-08-17

Branch audited: `v0.9-faction-automation`

## Architecture now in source

### 1. Shared faction backend — `backend/AccessBackend.gs`
Android client: `CompanionBackendClient`

Server-backed:
- verified player + faction identity
- faction-position capability cache/sync
- rank access rules and per-user overrides
- faction notices
- banking requests
- banking reconciliation
- faction-scoped banking listener tokens

Security/state:
- shared rows are tenant-scoped by verified `faction_id`
- client API keys are never stored server-side
- identity cache keys use only an API-key SHA-256 fingerprint
- sensitive banking/notice writes re-check current Torn position abilities
- legacy single-faction settings/listener token remain for migration compatibility

### 2. Community backend — `backend/TornFcaCommunityBackend.gs`
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

### 3. Premium backend — `backend/TornFcaPremiumBackend.gs`
Android client: `PremiumBackendClient`

Server-backed:
- verified premium entitlement status
- one-minute Xanax payment scan
- dedupe by Torn log ID
- premium configuration
- manual/beta grants

Security/state:
- entitlement reads are limited to the verified signed-in Torn player
- admin changes require verified TornFCA owner ID plus developer password
- client API keys are never persisted
- `OWNER_API_KEY` remains a server Script Property used only by the payment scanner

### 4. Developer control plane — `backend/TornFcaDeveloperBackend.gs`
Android client: `DeveloperBackendClient`
Android cache/enforcement: `RemoteFeaturePolicy`
Protected UI: `DeveloperBackendActivity`

Server-backed:
- maintenance mode
- minimum supported `versionCode`
- Beta/operator message
- emergency remote switches for Activity, War/WarPay, Chain, OC, Pulse, Lookup and Premium
- append-only developer mutation audit log

Security/state:
- non-secret public product policy still requires a valid Torn identity
- status/config writes/audit require verified developer Torn ID `3987363`
- writes additionally require developer password
- API keys are never persisted
- remote policy is cached locally and refreshed in the background; networking never blocks app startup
- developer route remains available as a recovery path if normal features are remotely disabled

### 5. WarPay persistence backend — `backend/TornFcaWarPayBackend.gs`
Android client: `WarPayBackendClient`
Local/offline cache: `WarPayoutReceiptStore`

Server-backed:
- calculated WarPay receipts
- cross-device retrieval for current faction leadership

Security/state:
- every request verifies the Torn user and current faction
- receipts are isolated by `faction_id`
- current access intentionally matches WarPay UI: Leader/Co-leader only
- receipt payload and row count are validated before storage
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

External services such as TornStats/FFScouter remain external integrations, not TornFCA data stores.

## Deployment boundary

Repository source is now separated into deliberate backend services. A backend is not considered live merely because its `.gs` source exists in GitHub. Each Apps Script service must be deployed and its HTTPS URL supplied to the Android build through the matching environment variable:

- `TORNFCA_FACTION_BACKEND_URL`
- `TORNFCA_COMMUNITY_BACKEND_URL`
- `TORNFCA_PREMIUM_BACKEND_URL`
- `TORNFCA_DEVELOPER_BACKEND_URL`
- `TORNFCA_WARPAY_BACKEND_URL`

Firebase values remain separate build/deployment configuration.

## Still intentionally pending

- final Torn capability matrix for Community Moderation and any wider leadership tools
- any decision to cloud-sync personal checklist/baseline state (currently intentionally local)
- legacy faction-chat listener branding/modernization; compatibility remains in place for existing deployments
