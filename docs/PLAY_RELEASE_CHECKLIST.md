# TornFCA Google Play release checklist

Last reviewed: 2026-08-17

This checklist is a release-preparation aid, not legal advice. Re-check Google Play policy immediately before production submission.

## 1. Identity and Play Console

- [ ] Complete/resubmit Google Play developer identity verification.
- [ ] Confirm the public developer/display name that will appear on the Play listing.
- [ ] Confirm the public support/privacy contact method.
- [ ] Confirm package name remains `com.matthayesego.duckforcetoolkit` for the production track.
- [ ] Confirm production signing uses the permanent TornFCA/Duck Force release certificate and never a test key.
- [ ] Keep `main` untouched until an explicitly approved/tested promotion.

## 2. App legal surfaces

Current in-app legal version: `2026-08-17-v2`.

- [x] Versioned local legal acknowledgement store.
- [x] First-run/current-version legal acknowledgement before Torn API sign-in.
- [x] Privacy Policy available in-app.
- [x] Terms & Conditions available in-app.
- [x] EULA available in-app.
- [x] Legal documents reachable again from More, Settings and About.
- [x] Privacy is acknowledged separately from agreement to Terms/EULA in the acceptance wording.
- [x] FCM Messaging auto-init is disabled before the current legal version is acknowledged; TornFCA explicitly enables push after acknowledgement.
- [ ] Final developer/legal review of the production wording.
- [ ] If material legal wording changes, bump `LegalAcceptanceStore.LEGAL_VERSION` so users are asked again.

## 3. Public privacy-policy hosting

Google Play requires a public privacy-policy URL as well as in-app access.

- [ ] Publish a static, public, non-geofenced HTTPS privacy-policy page.
- [ ] Do not use a PDF as the Play Console privacy-policy URL.
- [ ] Add the final developer/privacy contact mechanism to the public policy.
- [ ] Ensure the page explicitly names TornFCA and matches the Play developer/app identity.
- [ ] Verify the URL works in a logged-out/incognito browser.
- [ ] Enter the URL in Play Console → Policy/App content → Privacy policy.
- [ ] Keep public wording synchronized with `LegalDocumentActivity`.

A `docs/privacy-policy.html` publication draft can be hosted after a public HTTPS host is configured. It is intentionally marked `noindex` and as a draft until the public privacy contact and production hosting are ready.

## 4. Data Safety inventory — current Android/community architecture

Use this inventory to complete Play Console's Data safety form. Confirm against the exact production build and backend before submission.

### Data accessed from Torn

Depending on the feature and Torn permissions, the app can access:

- Torn player identity/profile and faction membership/position.
- Faction/member information.
- Energy/bars, cooldowns and refill-related state.
- Organized-crime assignment/readiness data.
- Chain and ranked-war information/participation.
- Battle stats and personal statistics for the signed-in player where needed for My Training Progress.
- Other Torn API fields explicitly required by a selected feature.

Core requests go to Torn's official API using the user-provided Torn API key.

### Local/device storage

The app may keep:

- Torn API key: session-only by default, or encrypted locally for 7/30/90 days when the user opts in.
- Notification history/preferences.
- Faction identity/cache needed for current-session UX.
- Faction onboarding/checklist state.
- War-prep checklist state.
- Personal training baselines scoped to player + faction.
- Optional intelligence-provider consent state.
- Premium entitlement/cache state.
- Legal acknowledgement version/timestamp.
- FCM registration token after Messaging is enabled.

### TornFCA-hosted/shared data

When those features are enabled/configured, TornFCA services may store/process:

- Verified Torn player ID and faction ID/role/scope.
- Firebase Cloud Messaging device token for push delivery.
- Faction chat messages.
- Faction training rules and custom guides/resources.
- Other faction-scoped shared notices/content used by enabled TornFCA community features.

The shared-services design verifies current faction identity/permissions. Torn API keys may be transmitted over HTTPS for verification but are not intended to be persisted by the TornFCA backend.

### Third parties / SDKs

- Torn official API — required for core Torn data.
- Firebase Cloud Messaging — push delivery. Messaging auto-init is disabled until current legal acknowledgement. Current Android dependencies do not directly include Firebase Analytics, and Analytics collection is explicitly disabled in the manifest.
- Firebase Installations — transitively used by FCM; may generate a per-installation Firebase installation ID (FID) and process Firebase user-agent/app/device metadata needed by Firebase services.
- FFScouter — optional, explicit opt-in before use.
- TornStats — optional, explicit opt-in before use.
- Google Play services/billing if and when paid Play-distributed features are enabled.

### Likely Data safety categories to review carefully

Do not blindly copy these; answer from the exact production build and Google definitions:

- User IDs / identifiers: likely applicable because verified Torn player IDs and faction scope can be processed by TornFCA services.
- Device or other IDs: review FCM registration tokens and Firebase Installation IDs under Google's current definitions.
- Device/app information: review FCM/Firebase user-agent and app-version processing under Google's current definitions.
- Messages / user-generated content: applicable when faction chat is enabled.
- Other user-generated content: applicable to faction-authored guides/rules/notices where the backend stores them.
- App activity/analytics: Firebase Analytics collection is explicitly disabled; confirm no other production SDK adds analytics and answer from Google's exact Data safety definitions.
- Crash/diagnostics: confirm production dependencies before declaring.
- Location, contacts, SMS/call log, microphone and camera: not part of the current TornFCA architecture; confirm permissions remain absent in the production manifest.

## 5. Retention and deletion

- [ ] Publish a clear privacy/deletion contact mechanism before public release.
- [ ] Document how TornFCA-hosted player/community data is deleted on request.
- [ ] If TornFCA ever adds a true app-account creation flow, implement Google Play's in-app and external account-deletion requirements before shipping that flow.
- [ ] Make clear that deleting TornFCA data cannot delete independent data held by Torn, Google/Firebase, FFScouter or TornStats; users may need to use those providers' own controls.
- [ ] Review backend sheets/storage for stale FCM tokens and establish a reasonable cleanup rule.

## 6. Store/review readiness

- [ ] Current production target SDK remains compliant with the submission deadline (currently targetSdk 36 in this branch).
- [ ] Complete Data safety form and make sure every answer matches the privacy policy and exact production SDK list.
- [ ] Complete content rating and target-audience declarations.
- [ ] Declare ads accurately.
- [ ] Provide Google reviewers enough access/instructions to test API-key sign-in and gated features without exposing a real user's permanent credential.
- [ ] Test first install, legal acknowledgement, API login, logout/change-key, reinstall/update behavior, notifications, no-network behavior and faction-change isolation.
- [ ] Verify About, Legal & Privacy, Settings and Member Center remain readable on small phones and large Android displays.
- [ ] Verify release build contains no developer PIN/secrets, test API keys, private backend credentials or debug-only access paths.
- [ ] Verify merged release manifest still has both `firebase_analytics_collection_enabled=false` and `firebase_messaging_auto_init_enabled=false`.

## 7. Navigation/usability release bar

- [ ] A normal member can reach My Day in one obvious action.
- [ ] Member Center is grouped by: Start Here; Daily & Readiness; Growth & Training; My Faction; Community & Alerts; Optional Upgrade.
- [ ] Labels describe the user's task, not internal implementation names.
- [ ] Core/free tools appear before Premium.
- [ ] Leadership tools do not crowd ordinary member navigation.
- [ ] Every screen has a clear back path and one obvious primary action.
- [ ] Long explanatory copy uses short paragraphs/cards rather than walls of text.
- [ ] Legal documents are discoverable but do not dominate day-to-day navigation after acknowledgement.
