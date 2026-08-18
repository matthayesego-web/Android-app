# TornFCA v1.0 Backend Go-Live Runbook

Branch: `v0.9-faction-automation`

This is the final deployment path for the five TornFCA backend services. Source existence is not live deployment. v1.0 is gated on deployed URLs, automated audits and an on-device smoke test.

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
8. Access: **Anyone**. TornFCA performs Torn identity/permission checks inside the service; Android clients must be able to reach the endpoint.
9. Deploy and copy the final `/exec` HTTPS URL.
10. Never place Torn API keys, Firebase private keys, admin passwords or service-account material in source/GitHub.

## 1. Faction backend — v1.1.0

Sheet: `TornFCA - Faction Backend`

Source: `backend/AccessBackend.gs`

Run once: `setupTornFcaFactionBackend()`

GitHub Actions secret: `TORNFCA_FACTION_BACKEND_URL`

Expected GET identity: `TornFCA Faction Backend`, version `1.1.0`.

New deployments are multi-faction by default. Existing legacy restriction settings remain compatible.

## 2. Community backend — v1.5.0

Sheet: `TornFCA - Community Backend`

Source: `backend/TornFcaCommunityBackend.gs`

Run once: `setupTornFcaCommunityBackend()`

Every Community request re-reads current faction membership/position from Torn, so changing factions or losing a leadership role does not retain stale tenant access. Stable player identity may be cached briefly without caching faction authorization. Duplicate-sensitive shared writes are ScriptLock-protected.

### Initial moderation policy

Do not invent a custom-position rule during deployment. The backend supports capability-configured moderation later.

Initial safe values:

- `MODERATION_ALLOW_LEADERS=false`
- `MODERATION_ABILITIES` = blank

This leaves the verified TornFCA owner as recovery/global moderator until the final capability matrix is explicitly approved.

For Firebase Cloud Messaging, add Apps Script Script Properties:

- `FIREBASE_PROJECT_ID`
- `FIREBASE_CLIENT_EMAIL`
- `FIREBASE_PRIVATE_KEY`

GitHub Actions secret: `TORNFCA_COMMUNITY_BACKEND_URL`

Expected GET identity: `TornFCA Community Backend`, version `1.5.0`.

## 3. Premium backend — v1.2.0

Sheet: `TornFCA - Premium Backend`

Source: `backend/TornFcaPremiumBackend.gs`

Run once: `setupTornFcaPremiumBackend()`

### Safe test deployment

The backend deliberately creates/retains:

- `MONETIZATION_APPROVED=false`

**Leave this false for the v0.10.1/backend test phase.** Deploying the Premium backend does not authorize paid usage. Both `installPremiumScanTrigger()` and `scanPremiumPayments()` fail closed while this flag is false.

Do **not** install the automatic payment trigger merely to complete backend deployment.

### Premium admin password

In **Project Settings → Script Properties**, temporarily add:

- `PREMIUM_ADMIN_PASSWORD_SETUP` = private admin password, minimum 10 characters

Run once:

- `bootstrapPremiumAdminPassword()`

It stores only `PREMIUM_ADMIN_SHA256` and deletes the plaintext setup property in a `finally` block. Confirm `PREMIUM_ADMIN_PASSWORD_SETUP` is gone.

### Testing entitlement without monetization

Use the owner-only manual grant path from TornFCA's Premium Admin screen/backend to test:

- Free → Premium transition
- expiry
- remote `disable_premium`
- Premium Matrix behavior

Manual developer/support grants do not mean production monetization is enabled.

### Automatic payment activation — separate release gate

Before any real automatic paid entitlement processing:

1. Handle Torn's current requirement for API-tool creators to contact Torn before charging users for usage.
2. Decide the distribution/payment path. A Google Play-distributed build must also use a payment method permitted by the applicable Google Play billing/payment program.
3. Only after the required approval/path is settled, explicitly set `MONETIZATION_APPROVED=true` in Script Properties.
4. Configure `OWNER_API_KEY` as a server-only Torn key with the minimum log access needed by the scanner.
5. Run `installPremiumScanTrigger()` once.
6. Confirm exactly one one-minute `scanPremiumPayments` trigger exists.
7. Run payment replay/stacking/expiry tests before accepting production payments.

Payment processing is ScriptLock-protected, receipt-idempotent, and honors the `stacking` setting.

GitHub Actions secret: `TORNFCA_PREMIUM_BACKEND_URL`

Expected GET identity: `TornFCA Premium Entitlements`, version `1.2.0`.

## 4. Developer control plane — v1.3.0

Sheet: `TornFCA - Developer Backend`

Source: `backend/TornFcaDeveloperBackend.gs`

Run once: `setupTornFcaDeveloperBackend()`

Temporarily add Script Property:

- `DEVELOPER_ADMIN_PASSWORD_SETUP` = private developer-admin password, minimum 10 characters

Run once:

- `bootstrapTornFcaDeveloperAdminPassword()`

Confirm the plaintext setup property is deleted afterward.

GitHub Actions secret: `TORNFCA_DEVELOPER_BACKEND_URL`

Expected GET identity: `TornFCA Developer Control Plane`, version `1.3.0`.

This service owns remote policy, emergency feature switches, minimum version, audit records and aggregate user counts. It does not replace the local Developer Gate.

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

Permanent Android signing secrets remain separate and must not be rotated during backend deployment.

## Final live verification

After all five URLs are configured:

1. Run **TornFCA Backend Live Audit**.
2. It must identify exact audited backend versions: Faction 1.1.0, Community 1.5.0, Premium 1.2.0, Developer 1.3.0, WarPay 1.1.0.
3. It compiles both side-by-side Beta and release candidates.
4. It verifies Beta package `com.matthayesego.duckforcetoolkit.beta` and release package `com.matthayesego.duckforcetoolkit`.
5. Run **TornFCA Premium Matrix Audit**.
6. Run **TornFCA Cloud Candidate**; it refuses to build against missing/stale backends.
7. Do not promote to `main` until the signed Beta receives an on-device smoke test.

## On-device v1.0 backend smoke test

Minimum pass list:

- sign in and reload faction scope
- re-acknowledge legal version v4 and verify the API-key disclosure is visible at key entry
- read faction notices
- submit/read a banking request
- open Community chat and send/read a message
- switch/change faction scope during testing and confirm old Community tenant data is not retained
- submit a chat report
- verify owner moderation/recovery; test wider moderator capabilities only after policy is explicitly configured
- read/save training content with an authorized account
- register push token and send personal push test
- verify Free Premium status
- use an owner manual grant to test Premium activation/expiry while `MONETIZATION_APPROVED=false`
- verify automatic payment scanning refuses to start while `MONETIZATION_APPROVED=false`
- verify Developer Control Plane status/config/user counts
- calculate/save WarPay receipt, restart, and re-read it from cloud
- perform two near-simultaneous WarPay saves for one war and confirm one faction/war row
- disable Premium remotely and verify Premium convenience locks while Free tools remain usable
- disable/re-enable one noncritical remote feature
- verify offline/local-safe behavior without corrupting cloud state

A failure in one service must not grant broader access to another. Client Torn API keys remain request-only and must never be persisted by the Apps Script backends.
