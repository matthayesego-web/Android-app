# TornFCA Project State

This file is the durable recovery note for TornFCA. Keep it current whenever architecture, branch policy, release state, major features, credentials/deployment assumptions, or next steps change.

## Branch policy
- `main` = Google Play / production source. Do not develop directly here.
- `development` = accepted next-version development source.
- `work/*` = temporary implementation/validation branches created from `development`.
- `restore/*` = immutable recovery snapshots before risky changes.
- Production app identity/icon uses **T**. Development app identity/icon uses **D**.

## Current versions
- Production `main`: v0.10.19, versionCode 74.
- Development target: v0.10.21, versionCode 76.
- Development package remains isolated from production using the existing `.beta` applicationId suffix, but visible naming is **TornFCA Development** and version suffix is `-development`.

## Current architecture
- Android native app, Java/Kotlin Gradle project, target/compile SDK 36.
- Torn API key stored through `SecureApiKeyStore`; never commit user keys.
- Torn API v2 is used for authenticated player/faction data.
- Shared TornFCA services use separate backend endpoints for faction, community, premium, developer, WarPay and feedback.
- Firebase/FCM supplies cloud push when configured through build secrets.
- `main` must remain Play-safe and review-safe; Play Review mode stays synthetic and isolated from Torn/backend writes.

## Accepted feature state through v0.10.20 development
- Automatic Android notification permission onboarding after authenticated startup; denial is remembered.
- Automatic push initialization/sync after startup.
- Notification deep links route to Chat, War, Chain/OC, Banking, announcements or moderation as appropriate.
- Native TornFCA community chat remains available with General / War / OC / Leadership channels.
- Chat safety actions are behind tapping another member message: View Torn Profile / Report / Block.
- Leaders have a Reports & Moderation entry in Chat; server-side moderation policy still governs actual access.
- War Chain Live Tracker is opt-in and uses a foreground notification/service: ~30 s API refresh during active chain, ~60 s during ranked war with no active chain, local chronometer for second-level timeout display, Android 16 promoted-ongoing request where eligible.
- Development build is permanently signed by CI before being accepted into `development`.

## Real Torn chat integration plan (v0.10.21 experiment)
Goal: test true two-way Torn faction chat without copying/extracting any third-party Sendbird privileged credential.

Preferred experiment:
1. Add a foreground, full-screen Torn-backed chat activity using Android WebView.
2. Load Torn while the user is actively viewing the WebView; do not run a hidden/unfocused Torn scraper.
3. Keep authentication owned by Torn's normal web session/cookies.
4. Navigate/open Torn's faction chat UI and visually reduce unrelated page chrome so faction chat behaves like a full-screen chat surface.
5. Use DOM interaction only while the user is actively viewing the Torn page. Sending should drive Torn's actual textarea/send control rather than calling Sendbird with an unowned credential.
6. DOM selectors must be defensive because Torn can change markup. Detect faction chat using selectors/patterns such as `div[id^="faction-"]` plus fallbacks; never assume one selector forever.
7. If the Torn session is logged out or the chat DOM cannot be found, show a clear recovery UI instead of silently scraping/retrying in background.
8. This experiment is foreground-only. No hidden WebView harvesting, external aggregation, or background WebSocket interception.
9. Keep the existing native TornFCA community chat as a fallback during testing; do not remove it until real Torn chat proves stable.
10. Continue researching whether Torn exposes a legitimate first-party user-scoped Sendbird/session-token exchange. Do not ship or extract another app's Sendbird API token.

## Development icon rule
- Production: launcher mark/crest must remain **T**.
- Development: launcher mark/crest must visibly use **D** so installed builds are instantly distinguishable.
- Switching a validated development version to production requires restoring the production **T** icon and production app label/package behavior before Play promotion.

## Moderation plan
- Reports are faction-scoped.
- Normal members can report/block; blocking remains device/faction local.
- Leaders/Co-leaders should be able to review faction reports when backend moderation policy enables them.
- Owner/developer retains global/owner override where backend policy allows.
- Never trust only the Android UI for permission enforcement; backend must re-verify Torn identity/faction/position/abilities.

## Release discipline
- Before risky work, make a `restore/...` branch from `development`.
- Implement on `work/...`.
- Open a PR into `development`; compile/sign/test the Development APK there.
- Merge only after CI passes.
- Never merge to `main` merely to produce a test APK.
- Keep restore branches and signed build artifacts long enough for rollback/audit.

## Immediate next work
- v0.10.21: foreground real Torn faction-chat WebView experiment.
- v0.10.21: replace Development launcher T with D; production assets stay unchanged on `main`.
- Validate real Torn chat read/send behavior on device, including login loss and Torn DOM changes.
- Verify moderation backend policy for non-owner faction leaders before production promotion.
- Continue improving live war/chain notification behavior based on device testing.

## Recovery checklist
When resuming after a lost chat/session:
1. Read this file first.
2. Read `docs/TORN_CHAT_INTEGRATION_RESEARCH.md`.
3. Compare `main...development` before editing.
4. Inspect latest successful Development APK workflow and latest restore branch.
5. Never expose API keys, Firebase secrets, Android signing material, or privileged third-party credentials in source/chat.
