# TornFCA Google Play release checklist

Last reviewed: 2026-08-17

This checklist is a release-preparation aid, not legal advice. Re-check Google Play policy immediately before production submission.

## 1. Identity and Play Console
- [ ] Complete/resubmit Google Play developer identity verification.
- [ ] Confirm public developer/display name.
- [ ] Confirm public support/privacy contact method.
- [ ] Keep package `com.matthayesego.duckforcetoolkit` and permanent production signing identity.
- [ ] Keep `main` untouched until explicitly approved/tested promotion.

## 2. App legal surfaces
Current in-app legal version: `2026-08-17-v3`.
- [x] Versioned local legal acknowledgement store.
- [x] Legal acknowledgement before Torn API sign-in.
- [x] In-app Privacy Policy, Terms & Conditions and EULA.
- [x] Concise pre-sign-in data-use disclosure.
- [x] Privacy/data-use acknowledgement separated from Terms/EULA agreement.
- [x] FCM disabled until current legal acknowledgement.
- [x] Push client independently enforces the legal gate.
- [ ] Final developer/legal review of production wording.
- [ ] Insert final public privacy/support contact before release.

## 3. Public privacy-policy hosting
- [ ] Publish public non-geofenced HTTPS privacy-policy page.
- [ ] Add final developer/privacy/deletion contact.
- [ ] Remove draft/noindex markers.
- [ ] Verify logged-out/incognito access.
- [ ] Enter URL in Play Console and keep wording synchronized with the app.

## 4. Data Safety inventory
Production review must cover:
- Torn identity/profile, faction scope, bars/cooldowns, OC, chain/war, battle/personal statistics used by selected features.
- Session-only or opt-in encrypted API-key storage.
- Local preferences, notification history, faction cache, onboarding/war-prep/training baselines, provider consent, entitlement and legal acknowledgement state.
- TornFCA-hosted verified player/faction identifiers, FCM token, faction chat, faction guides/rules/notices and moderation reports when enabled.
- Firebase Cloud Messaging / Firebase Installations; Firebase Analytics remains disabled in the current design.
- Optional FFScouter and TornStats only after explicit opt-in.

## 5. UGC/community moderation release bar
Faction Chat is user-generated content and must keep safety controls operational.
- [x] Terms accepted before community posting.
- [x] Terms define prohibited conduct/content and moderation rights.
- [x] In-app Report action on another member's chat message.
- [x] Reports are verified against the reporter's current faction and stored in a faction-scoped `ChatReports` moderation sheet with message snapshot, author, reporter, reason, time and status fields.
- [x] In-app Block User action.
- [x] Blocked users' chat messages are hidden locally for that faction.
- [x] Chat push notifications from blocked authors are ignored on that device.
- [x] Users can clear their faction block list from Faction Chat.
- [ ] Define the production moderator/reviewer workflow for open `ChatReports` rows and how status/resolution fields are updated.
- [ ] Decide moderation-report retention period.
- [ ] Test reports, duplicate reports, blocking, unblocking, faction changes and push suppression against the deployed backend.
- [ ] Complete Play UGC/content-rating declarations accurately.
- [ ] Do not call public community chat production-ready until moderation operations are tested.

## 6. Retention and deletion
- [ ] Publish privacy/deletion contact mechanism.
- [ ] Document TornFCA-hosted deletion handling.
- [ ] Establish stale FCM-token cleanup.
- [ ] Clarify that TornFCA deletion cannot delete independent Torn/Google/FFScouter/TornStats data.

## 7. Store/review readiness
- [x] Current branch targets API 36.
- [ ] Complete Data Safety, content rating, audience and ads declarations.
- [ ] Provide reviewer instructions/test access without exposing a permanent personal credential.
- [ ] Test first install, legal flow, login/logout, update/reinstall, notifications, no-network and faction isolation.
- [ ] Verify About, Legal, Settings and Member Center on small and large Android displays.
- [ ] Verify release build contains no secrets/test credentials/debug-only access paths.
- [ ] Verify merged release manifest keeps Firebase Analytics and Messaging auto-init disabled by default.

## 8. Navigation/usability release bar
- [x] Member Center is the obvious home for everyday faction tools.
- [x] Member Center sections: Start Here; Daily & Readiness; Growth & Training; My Faction; Community & Alerts; Optional Upgrade.
- [x] More no longer duplicates Chat/Inbox as competing destinations.
- [x] Settings is grouped by notifications/community, account/security, optional services/plan, legal/privacy and account action.
- [x] Core/free tools appear before Premium and leadership tools do not crowd normal-member navigation.
- [ ] Complete device readability/back-path review across all new member screens.

## 9. v0.9.27 completion rule
Do not promote the v0.9.27 candidate until member features are coherent, chat moderation operations are tested if chat is enabled, public privacy/Data Safety work is ready, the exact candidate passes build/device audits, and the user explicitly approves promotion. 
