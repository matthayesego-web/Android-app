# TornFCA Google Play beta / release checklist

Last reviewed: 2026-08-20  
Working line: **v0.10.18 / versionCode 73**  
Branch: `work/v0.10.18-beta-hardening`

This is the release-preparation checklist for the exact Play-beta line. `main` / production remains out of scope until explicit promotion approval.

## 1. Identity, package and signing
- [ ] Complete/resubmit Google Play developer identity verification if Play Console still requires it.
- [ ] Confirm public developer/display name.
- [ ] Confirm public support/privacy contact method.
- [x] Production package remains `com.matthayesego.duckforcetoolkit`.
- [x] Parallel sideload beta package remains `com.matthayesego.duckforcetoolkit.beta`.
- [x] Permanent TornFCA certificate fingerprint is documented and verified against the private signing backup.
- [ ] Restore the four Android signing secrets in GitHub Actions: keystore base64, store password, alias and key password.
- [ ] Run the signed v0.10.18 beta workflow and verify the permanent certificate + `.beta` package.
- [ ] Run the manual signed Play AAB workflow and verify the permanent certificate + production package.
- [x] Keep `main` untouched until explicit tested promotion approval.

## 2. Android / Play technical gate
Google Play requires new apps and app updates to target Android 16 / API 36 starting August 31, 2026.
- [x] `compileSdk = 36`.
- [x] `targetSdk = 36`.
- [x] v0.10.18 uses versionCode 73.
- [x] Release AAB workflow builds the normal production package, not the `.beta` package.
- [x] Only the intended launcher is externally exported; new Premium/Review screens remain internal-only.
- [x] `android:allowBackup="false"`.
- [x] `android:usesCleartextTraffic="false"`.
- [x] Firebase Analytics collection is disabled in the manifest.
- [x] Firebase Messaging auto-init is disabled in the manifest and gated by legal acknowledgement in app code.
- [x] Branch-native compile/syntax validation workflows exist for v0.10.18.
- [ ] Exact final source snapshot passes validation after feature freeze.

## 3. Google Play review access
Google requires usable reviewer access for restricted functionality rather than a permanent production-user credential.
- [x] Isolated **Google Play Review Sandbox** implemented.
- [x] Review sandbox uses deterministic synthetic Member and Faction Leader data.
- [x] Review sandbox never stores/reads a Torn API key.
- [x] Review sandbox never calls Torn or TornFCA production backends.
- [x] Review sandbox never registers a Firebase recipient or sends production push.
- [x] Review sandbox writes remain local/synthetic.
- [x] Review sandbox visibly identifies itself as synthetic review mode.
- [x] Reusable review code/instructions are documented in `docs/PLAY_REVIEW_SANDBOX.md`.
- [ ] Test the review sandbox from the exact signed Play AAB build before upload.
- [ ] Enter the reusable review instructions/code in Play Console App access.

## 4. App legal surfaces
Current in-app legal version: `2026-08-17-v3`.
- [x] Versioned local legal acknowledgement store.
- [x] Legal acknowledgement before Torn API sign-in.
- [x] In-app Privacy Policy, Terms & Conditions and EULA.
- [x] Concise pre-sign-in data-use disclosure.
- [x] Privacy/data-use acknowledgement separated from Terms/EULA agreement.
- [x] FCM disabled until current legal acknowledgement.
- [x] Push client independently enforces the legal gate.
- [ ] Final developer/legal review of production wording.
- [ ] Insert final public privacy/support/deletion contact before release.

## 5. Public privacy-policy hosting
- [ ] Publish public non-geofenced HTTPS privacy-policy page.
- [ ] Add final developer/privacy/deletion contact.
- [ ] Remove draft/noindex markers.
- [ ] Verify logged-out/incognito access.
- [ ] Enter URL in Play Console and keep wording synchronized with the app.

## 6. Data Safety inventory
Production declarations must cover the actual v0.10.18 behavior:
- Torn identity/profile and current faction scope.
- Bars/cooldowns, OC, chain/war/territory data used by selected features.
- Personal battle/personal statistics used by Training Progress, Premium Training Goal Pacing and Personal Insights.
- Session-only or opt-in encrypted API-key storage.
- Local preferences, notification history, faction cache, onboarding/war-prep/training baselines and private Premium goal state.
- Provider consent/configuration and backend-verified Premium entitlement cache.
- TornFCA-hosted verified player/faction identifiers, FCM tokens, chat, guides/rules/notices, banking requests, WarPay receipts and moderation reports where enabled.
- Firebase Cloud Messaging / Firebase Installations; Firebase Analytics remains disabled.
- Optional FFScouter and TornStats only after explicit opt-in.
- [ ] Complete final Play Data Safety form from `docs/PLAY_DATA_SAFETY_WORKSHEET.md` against the exact submitted AAB.

## 7. UGC/community moderation release bar
Faction Chat is user-generated content; safety functions are Free and must remain operational.
- [x] Terms accepted before community posting.
- [x] Terms define prohibited conduct/content and moderation rights.
- [x] In-app Report action on another member's chat message.
- [x] In-app Block User action and faction-scoped local suppression.
- [x] Chat push from blocked authors is suppressed locally.
- [x] Users can clear their faction block list.
- [x] Hidden central moderation queue remains owner-authorized server-side.
- [x] Moderator dismiss/remove actions and resolution audit data exist in source.
- [x] Community backend source is hardened through v1.8.1 for fresh-faction push fan-out verification.
- [ ] Decide moderation-report retention period.
- [ ] Deploy the audited v1.8.1 Community backend to the production Community Apps Script.
- [ ] Device-test report → review → dismiss/remove, duplicate reports, block/unblock, faction changes and push suppression against the deployed backend.
- [ ] Complete Play UGC/content-rating declarations accurately.
- [ ] Do not call public community chat production-ready until deployed moderation operations pass testing.

## 8. Premium / entitlement beta gate
Free remains the complete core; Premium must never create faction authority.
- [x] Premium backend source advanced to v1.3.0.
- [x] Seven-day launch conversion is authoritative in source.
- [x] Existing-backend migration helper `applyPremiumSevenDayLaunchPricing()` exists.
- [x] Complimentary Premium path exists and is distinguishable as `COMPLIMENTARY_GRANT`.
- [x] Personal Insights includes recent 7-day vs prior-window trend information.
- [x] Training Goal Pacing is Premium while current stats/baseline/gains remain Free.
- [x] Premium Activity Trends independently re-verifies leadership and Premium.
- [x] Smart alert lead-time control remains Premium while standard alerts stay Free.
- [x] Faction Pulse / Member Dossier remain convenience layers over Free roster/provider functionality.
- [x] Premium matrix CI protects the Free/core boundary.
- [ ] Deploy Premium backend v1.3.0.
- [ ] Apply/confirm `days_per_xanax=7` on the deployed PremiumSettings sheet.
- [ ] Smoke-test Complimentary Premium → recipient refresh → expiry/source.
- [ ] Smoke-test Free member / Premium member / Free leader / Premium leader / expired Premium / remote disable.

## 9. Retention and deletion
- [ ] Publish privacy/deletion contact mechanism.
- [ ] Document TornFCA-hosted deletion handling.
- [ ] Establish stale FCM-token cleanup.
- [ ] Establish moderation-report retention/cleanup rule after operational/legal need ends.
- [ ] Clarify that TornFCA deletion cannot delete independent Torn/Google/FFScouter/TornStats data.

## 10. Closed-test readiness
For personal Play developer accounts created after November 13, 2023, Google currently requires a closed test with at least 12 opted-in testers continuously for 14 days before production access can be requested.
- [ ] Confirm whether this Play developer account is subject to the 12-testers / 14-days production-access requirement.
- [ ] Prepare tester list / Google Group or email list.
- [ ] Upload signed v0.10.18 AAB to the intended Play testing track.
- [ ] Verify install/update from Google Play rather than only sideloading.
- [ ] Collect tester feedback for login, navigation, Free/Premium boundary, notifications, war/OC/training, banking/WarPay, chat and error/offline states.
- [ ] Keep at least 12 testers continuously opted in for 14 days if the account requirement applies.

## 11. Exact-candidate device smoke test
- [ ] Fresh install → legal → Review Sandbox path.
- [ ] Fresh install → legal → real Torn sign-in path.
- [ ] Session-only API key behavior.
- [ ] 7/30/90-day encrypted retention behavior.
- [ ] Logout clears credential/session state without erasing accepted legal version.
- [ ] Free member navigation and all core tools.
- [ ] Premium member: Personal Insights, Training Goal Pacing, Smart Alerts.
- [ ] Free leader: all essential admin tools + only 7-day Activity.
- [ ] Premium leader: Activity Trends / 30-day Activity, Pulse and Dossier.
- [ ] Complimentary Premium grant and expiry.
- [ ] Banking request / banker queue behavior.
- [ ] WarPay calculation / receipt / Torn handoff.
- [ ] Armory Auditor current audit.
- [ ] Faction change invalidates old faction-shared content/state correctly.
- [ ] Push registration/dedupe and announcement/chat/banking notifications.
- [ ] No-network / slow-backend / stale-cache recovery.
- [ ] Small phone, large phone and increased font-scale readability.
- [ ] Release AAB/APK contains no signing key, API test key, admin password or service-account private key.

## 12. v0.10.18 completion rule
Do not promote v0.10.18 to `main` or call it a production candidate until:
1. the exact source snapshot passes CI,
2. permanent signing is restored to GitHub Actions and verified,
3. Premium v1.3.0 and Community v1.8.1 deployment smoke tests pass,
4. privacy/support/deletion information is public and complete,
5. Play Console declarations/reviewer access are completed,
6. the signed AAB passes device testing, and
7. explicit production-promotion approval is given.
