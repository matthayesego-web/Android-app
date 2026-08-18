# TornFCA v1.0 Backend Go-Live Runbook

Branch: `v0.9-faction-automation`

This is the final deployment path for the five TornFCA backend services. Source existence is not considered live deployment. The v1.0 readiness gate is complete only after all five deployed web-app URLs pass `.github/workflows/tornfca-backend-live-audit.yml`.

## Deployment rule for every Apps Script service

Use a separate Google Sheet + container-bound Apps Script project for each backend below.

For each service:

1. Create a Google Sheet with the service name.
2. Open **Extensions → Apps Script**.
3. Replace the default `Code.gs` content with the matching repository backend file.
4. Save.
5. Run the listed setup function once and accept the Google authorization prompts.
6. Use **Deploy → New deployment → Web app**.
7. Execute as: **Me**.
8. Who has access: **Anyone**. TornFCA performs its own Torn identity/permission checks; the web app must be reachable by installed Android clients.
9. Deploy and copy the final `/exec` HTTPS URL.
10. Never place Torn API keys, Firebase private keys, admin passwords, or service-account material in the source file or GitHub repository.

## 1. Faction backend

Google Sheet name: `TornFCA - Faction Backend`

Source:
- `backend/AccessBackend.gs`

Run once:
- `setupTornFcaFactionBackend()`

No mandatory Script Properties are required for a new multi-faction deployment after setup.

GitHub Actions secret:
- `TORNFCA_FACTION_BACKEND_URL`

Expected GET identity:
- `TornFCA Faction Backend`

## 2. Community backend

Google Sheet name: `TornFCA - Community Backend`

Source:
- `backend/TornFcaCommunityBackend.gs`

Run once:
- `setupTornFcaCommunityBackend()`

### v1.0 moderation policy

Use Torn's actual faction-position capability instead of custom position-name matching.

Set these Apps Script **Script Properties** after setup:

- `MODERATION_ALLOW_LEADERS` = `false`
- `MODERATION_ABILITIES` = `["Forum Management"]`

This makes **Forum Management** the v1.0 TornFCA community-moderator capability. The verified TornFCA owner account remains the global recovery moderator. Leader/Co-leader accounts qualify through their actual Torn abilities rather than their displayed rank name, and custom faction positions also qualify when they have Forum Management.

Do not broaden the list casually. Future capability changes can be made in Script Properties without hard-coding faction-specific rank names.

For Firebase Cloud Messaging, add these Apps Script **Script Properties**:
- `FIREBASE_PROJECT_ID`
- `FIREBASE_CLIENT_EMAIL`
- `FIREBASE_PRIVATE_KEY`

The private key may contain escaped `\n`; the backend normalizes it before signing the OAuth JWT.

GitHub Actions secret:
- `TORNFCA_COMMUNITY_BACKEND_URL`

Expected GET identity:
- `TornFCA Community Backend`

## 3. Premium backend

Google Sheet name: `TornFCA - Premium Backend`

Source:
- `backend/TornFcaPremiumBackend.gs`

Run once:
- `setupTornFcaPremiumBackend()`

Required Apps Script Script Property:
- `OWNER_API_KEY` — server-only Torn key used by the payment scanner. Prefer a custom Torn key restricted to the log access actually required by the Premium payment scanner.

Set the Premium admin password without sharing it or committing it. A simple one-time method is to temporarily add this wrapper in the Apps Script editor:

```javascript
function configurePremiumPasswordOnce(){
  setPremiumAdminPassword('REPLACE_WITH_YOUR_PRIVATE_PASSWORD');
}
```

Run `configurePremiumPasswordOnce()`, then delete that temporary wrapper and save the project again. Only the SHA-256 hash remains in Script Properties.

Then run once:
- `installPremiumScanTrigger()`

Confirm exactly one time-based `scanPremiumPayments` trigger exists and is scheduled every minute.

GitHub Actions secret:
- `TORNFCA_PREMIUM_BACKEND_URL`

Expected GET identity:
- `TornFCA Premium Entitlements`

## 4. Developer control-plane backend

Google Sheet name: `TornFCA - Developer Backend`

Source:
- `backend/TornFcaDeveloperBackend.gs`

Run once:
- `setupTornFcaDeveloperBackend()`

Set the remote developer-admin password without sharing or committing it. Temporarily add:

```javascript
function configureDeveloperPasswordOnce(){
  setTornFcaDeveloperAdminPassword('REPLACE_WITH_YOUR_PRIVATE_PASSWORD');
}
```

Run `configureDeveloperPasswordOnce()`, delete the temporary wrapper, and save. Only the SHA-256 hash remains in Script Properties.

GitHub Actions secret:
- `TORNFCA_DEVELOPER_BACKEND_URL`

Expected GET identity:
- `TornFCA Developer Control Plane`

The Android Developer Gate remains a separate local access boundary. The remote control plane is for server-side policy, emergency feature switches, minimum version policy, and audited operator changes.

## 5. WarPay backend

Google Sheet name: `TornFCA - WarPay Backend`

Source:
- `backend/TornFcaWarPayBackend.gs`

Run once:
- `setupTornFcaWarPayBackend()`

No additional Script Property is required after setup.

GitHub Actions secret:
- `TORNFCA_WARPAY_BACKEND_URL`

Expected GET identity:
- `TornFCA WarPay Backend`

WarPay keeps the local device copy as its offline cache and cloud sync remains faction-scoped.

## Android/Firebase GitHub Actions secrets

These must already be available for a fully cloud-enabled Beta/release candidate:

- `TORNFCA_FACTION_BACKEND_URL`
- `TORNFCA_COMMUNITY_BACKEND_URL`
- `TORNFCA_PREMIUM_BACKEND_URL`
- `TORNFCA_DEVELOPER_BACKEND_URL`
- `TORNFCA_WARPAY_BACKEND_URL`
- `TORNFCA_FIREBASE_APP_ID`
- `TORNFCA_FIREBASE_API_KEY`
- `TORNFCA_FIREBASE_PROJECT_ID`
- `TORNFCA_FIREBASE_SENDER_ID`

Permanent Android signing secrets remain separate and must not be rotated as part of backend deployment.

## Final live verification

After all five URLs are stored in GitHub Actions Secrets:

1. Run workflow **TornFCA Backend Live Audit** on `v0.9-faction-automation`.
2. The workflow must confirm all five HTTPS deployments respond with the correct backend identity.
3. It compiles both the side-by-side Beta and release candidate with all backend/Firebase values present.
4. It verifies the Beta package remains `com.matthayesego.duckforcetoolkit.beta` and the release candidate remains `com.matthayesego.duckforcetoolkit`.
5. Then run **TornFCA Cloud Candidate**; it now refuses to build a cloud candidate unless all five deployed backends are reachable and identify themselves correctly.
6. Do not promote to `main` until these audits pass and the signed Beta receives an on-device smoke test.

## On-device v1.0 backend smoke test

Minimum pass list:

- sign in and reload faction scope
- read faction notices
- submit/read a banking request
- open Community chat and send/read a message
- submit a chat report
- verify the authorized moderation queue with a Forum Management-capable account
- read/save training content with an authorized account
- register push token and send personal push test
- read Premium status
- verify Developer Control Plane status/config read
- calculate/save WarPay receipt, restart app, and re-read it from cloud
- disable one non-critical feature remotely, verify the app blocks it, then re-enable it
- verify airplane/offline behavior still shows local-safe data without corrupting cloud state

A failure in one service should not silently grant broader access to another service. Torn API keys must remain request-only and must never be persisted by the Apps Script backends.
