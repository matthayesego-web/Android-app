# TornFCA v1.0 Backend Go-Live Runbook

Branch: `v0.9-faction-automation`

This is the final deployment path for the five TornFCA backend services. Source existence is not live deployment. v1.0 is gated on deployed URLs, automated audits, Google Play testing and an on-device smoke test.

## Deployment rule for every Apps Script service

Use a separate Google Sheet + container-bound Apps Script project for each backend.

For each service:

1. Create the named Google Sheet.
2. Open **Extensions → Apps Script**.
3. Replace the default `Code.gs` with the matching repository backend file.
4. Save.
5. Run the listed setup function once and authorize it.
6. **Deploy → New deployment → Web app**.
7. Execute as **Me**.
8. Access: **Anyone**. TornFCA performs its own authorization inside each service; Android clients must be able to reach the endpoint.
9. Deploy and copy the final `/exec` HTTPS URL.
10. Never place Torn API keys, Firebase private keys, passwords, TOTP secrets or service-account material in source/GitHub.

## 1. Faction backend — v1.1.0

Sheet: `TornFCA - Faction Backend`

Source: `backend/AccessBackend.gs`

Run once: `setupTornFcaFactionBackend()`

GitHub Actions secret: `TORNFCA_FACTION_BACKEND_URL`

Expected GET identity: `TornFCA Faction Backend`, version `1.1.0`.

New deployments are multi-faction by default. Existing legacy restriction settings remain compatible.

## 2. Community backend — v1.7.0

Sheet: `TornFCA - Community Backend`

Source: `backend/TornFcaCommunityBackend.gs`

Run once: `setupTornFcaCommunityBackend()`

Every Community request re-reads current faction membership/position from Torn, so changing factions or losing a leadership role does not retain stale tenant access. Stable player identity may be cached briefly without caching faction authorization. Duplicate-sensitive shared writes are ScriptLock-protected.

v1.6.0 hardened the hot chat path for growth: chat polling scans bounded recent chunks rather than loading the full historical `ChatMessages` sheet on every poll, while exact message/report lookups use server-side TextFinder-style row searches.

v1.7.0 adds faction-scoped shared War Prep:
- `WarPrepConfig` is keyed by freshly verified `faction_id`.
- `WarPrepStatus` is keyed by verified `faction_id + war_id + player_id`.
- a new ranked-war ID starts a clean member checklist automatically.
- leadership status includes only faction members who actually opened/synced War Prep in TornFCA; it never treats non-users as incomplete.
- each faction can define up to eight checklist items without leaking configuration into another faction.

### Initial moderation policy

Initial safe values:
- `MODERATION_ALLOW_LEADERS=false`
- `MODERATION_ABILITIES` = blank

This leaves the verified TornFCA owner as recovery/global moderator until the final capability matrix is explicitly approved.

For Firebase Cloud Messaging, add Apps Script Script Properties:
- `FIREBASE_PROJECT_ID`
- `FIREBASE_CLIENT_EMAIL`
- `FIREBASE_PRIVATE_KEY`

GitHub Actions secret: `TORNFCA_COMMUNITY_BACKEND_URL`

Expected GET identity: `TornFCA Community Backend`, version `1.7.0`.

## 3. Premium backend — v1.2.0

Sheet: `TornFCA - Premium Backend`

Source: `backend/TornFcaPremiumBackend.gs`

Run once: `setupTornFcaPremiumBackend()`

### Safe test deployment

The backend deliberately creates/retains `MONETIZATION_APPROVED=false`.

**Leave this false throughout the v0.10.x/backend/Google Play test phase.** Deploying the Premium backend does not authorize paid usage. Both `installPremiumScanTrigger()` and `scanPremiumPayments()` fail closed while this flag is false.

Do **not** install the automatic payment trigger merely to complete backend deployment.

### Premium admin password

Temporarily add Script Property:
- `PREMIUM_ADMIN_PASSWORD_SETUP` = private admin password, minimum 10 characters

Run once:
- `bootstrapPremiumAdminPassword()`

It stores only `PREMIUM_ADMIN_SHA256` and deletes the plaintext setup property. Confirm `PREMIUM_ADMIN_PASSWORD_SETUP` is gone.

Use the owner-only manual grant path to test Free/Premium while monetization remains disabled.

GitHub Actions secret: `TORNFCA_PREMIUM_BACKEND_URL`

Expected GET identity: `TornFCA Premium Entitlements`, version `1.2.0`.

## 4. Developer control plane — v1.4.0

Sheet: `TornFCA - Developer Backend`

Source: `backend/TornFcaDeveloperBackend.gs`

Run once: `setupTornFcaDeveloperBackend()`

### Root Admin enrollment

The hidden developer channel no longer uses an app-embedded password or a Torn-player-ID login lock. Developer accounts use individual credentials and individual authenticator secrets.

Temporarily add Script Properties:
- `DEVELOPER_ROOT_USERNAME_SETUP` = desired Root Admin login name (optional; defaults to `root`)
- `DEVELOPER_ADMIN_PASSWORD_SETUP` = Root Admin password, minimum 14 characters

Run once:
- `bootstrapTornFcaDeveloperRoot()`

The function returns the Root Admin's one-time TOTP setup secret and `otpauth://` enrollment URI. Immediately add it to an authenticator app and retain a secure recovery record outside the repository. Confirm both plaintext setup properties were deleted.

Normal hidden access is:
**About TornFCA → tap Version 5 times → developer username → password → current 6-digit authenticator code.**

Root/Admin can then use **Developer Access** inside the hidden Developer Panel to:
- create one-time developer invitations
- assign Developer or permitted Admin roles
- revoke access and active sessions
- reset/re-enroll a developer with a new authenticator secret

Security controls include per-account salted/peppered password hashes, unique TOTP secrets, progressive failed-login lockouts, two-hour developer sessions and audit records. Root cannot be revoked through the app.

Developer credentials authorize only the hidden developer channel for the assigned role. They do **not** create faction authority, banking authority, Community moderation rights, Premium entitlements or Premium-admin authority. Those systems retain their own authorization checks.

For this release, global remote-product policy mutation remains a separate Root recovery boundary and still performs Torn-owner verification in addition to Root password verification.

GitHub Actions secret: `TORNFCA_DEVELOPER_BACKEND_URL`

Expected GET identity: `TornFCA Developer Control Plane`, version `1.4.0`.

## 5. WarPay backend — v1.1.0

Sheet: `TornFCA - WarPay Backend`

Source: `backend/TornFcaWarPayBackend.gs`

Run once: `setupTornFcaWarPayBackend()`

No additional Script Property is required after setup.

GitHub Actions secret: `TORNFCA_WARPAY_BACKEND_URL`

Expected GET identity: `TornFCA WarPay Backend`, version `1.1.0`.

Faction membership/Leader/Co-leader status is re-read from Torn on every backend request and receipt upserts are ScriptLock-protected.

## Android/Firebase GitHub Actions secrets

A fully cloud-enabled candidate requires:
- `TORNFCA_FACTION_BACKEND_URL`
- `TORNFCA_COMMUNITY_BACKEND_URL`
- `TORNFCA_PREMIUM_BACKEND_URL`
- `TORNFCA_DEVELOPER_BACKEND_URL`
- `TORNFCA_WARPAY_BACKEND_URL`
- `TORNFCA_FIREBASE_APP_ID`
- `TORNFCA_FIREBASE_API_KEY`
- `TORNFCA_FIREBASE_PROJECT_ID`
- `TORNFCA_FIREBASE_SENDER_ID`

Permanent Android signing material remains separate and must not be rotated during backend deployment.

## Final live verification

After all five URLs are configured:

1. Run **TornFCA Backend Live Audit**.
2. It must identify exact audited backend versions: Faction 1.1.0, Community 1.7.0, Premium 1.2.0, Developer 1.4.0, WarPay 1.1.0.
3. It compiles both side-by-side Beta and release candidates.
4. It verifies Beta package `com.matthayesego.duckforcetoolkit.beta` and release package `com.matthayesego.duckforcetoolkit`.
5. Run **TornFCA Premium Matrix Audit**.
6. Run **TornFCA Permission Freshness Audit**.
7. Run **TornFCA Cloud Candidate**; it refuses to build against missing/stale backends.
8. Keep the app on the v0.10.x line through Google Play testing.
9. Do not promote to `main` or v1.0 until Google Play/device testing passes.

## On-device backend smoke test

Minimum pass list:
- sign in and reload faction scope
- re-acknowledge legal version v4 and verify the API-key disclosure
- read/publish faction notices with authorized leadership
- submit/read banking requests
- test Community chat/report/moderation boundaries
- switch faction scope and confirm old tenant data is not retained
- verify War Prep resets on a new ranked-war ID
- verify separate faction War Prep configurations
- verify leadership War Prep only lists TornFCA users who synced the current war
- test training content/push registration
- verify Free/Premium behavior with manual grants while `MONETIZATION_APPROVED=false`
- verify automatic payment scanning refuses while monetization is disabled
- tap About Version five times and verify hidden developer login appears
- verify incorrect developer password/OTP attempts trigger server lockouts
- enroll a second Developer with a one-time invite and unique authenticator
- revoke that Developer and confirm the session immediately loses server access
- reset/re-enroll a Developer and confirm the old authenticator no longer works
- verify Developer credentials alone cannot obtain faction, banking, moderation or Premium-admin authority
- calculate/save/re-read WarPay receipt
- test concurrent WarPay save dedupe
- verify leadership-only routes re-check current leadership
- verify remote disable behavior and offline/local-safe behavior

A failure in one service must not grant broader access to another. Client Torn API keys remain request-only and must never be persisted by the Apps Script backends.
