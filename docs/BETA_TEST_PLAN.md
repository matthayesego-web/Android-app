# TornFCA Community Beta Test Plan

Baseline: **v0.9.26 / versionCode 43**  
Validated restore point: `restore/v0.9.26-readability-candidate-2026-08-17`  
Validated commit: `f222139a0aeea7aab01d322ae66ec9af2ced8911`

This plan separates release blockers from important beta coverage and cosmetic polish. A failed P0 item blocks promotion. A failed P1 item should normally be fixed before broad community beta unless a documented safe workaround exists. P2 issues can be triaged after beta begins.

## Severity

- **P0 — release blocker:** security, wrong-faction data, broken sign-in/update, crash, inaccessible legal/privacy flow, exposed secret, or core member workflow unusable.
- **P1 — beta blocker/important:** major feature incorrect or confusing, important degraded/offline path broken, leadership boundary incorrect, notifications unreliable, or common-device layout unusable.
- **P2 — polish:** wording, spacing, minor visual consistency, uncommon edge case with a safe workaround.

---

# 1. Installation, update, and signing

## P0

- [ ] Fresh install launches TornFCA rather than any legacy activity.
- [ ] Only the intended launcher activity is externally exported.
- [ ] Install/update over the previous permanently signed TornFCA build succeeds without uninstalling or losing the package identity.
- [ ] Permanent production certificate continuity is verified before distributing a signed test build.
- [ ] `versionName` and visible About/footer version both show the intended candidate version.
- [ ] `versionCode` is higher than every previously distributed build.
- [ ] Release APK contains no test Torn API key, Firebase service-account private key, backend secret, plaintext developer password, or signing private key.
- [ ] `android:allowBackup="false"` and `android:usesCleartextTraffic="false"` remain present.

## P1

- [ ] Upgrade preserves normal non-sensitive preferences where intended.
- [ ] Upgrade preserves a still-valid encrypted API-key session when its selected retention period has not expired.
- [ ] Upgrade does not resurrect an expired or session-only API key.
- [ ] App launches correctly after device reboot following an update.

---

# 2. First launch, Legal & Privacy, and sign-in

## P0

- [ ] Fresh install opens **Before you continue** before the Torn API sign-in screen.
- [ ] Privacy Policy opens and is readable.
- [ ] Terms & Conditions opens and is readable.
- [ ] EULA opens and is readable.
- [ ] **Accept & Continue** is disabled until the acknowledgement checkbox is selected.
- [ ] Checkbox wording distinguishes reviewing/acknowledging Privacy from agreeing to Terms/EULA.
- [ ] Acceptance stores the current legal version and continues to Torn sign-in.
- [ ] Relaunch does not repeatedly show the same already-accepted legal version.
- [ ] Clearing/replacing the accepted legal version in a test build causes acknowledgement to be required again.
- [ ] Sign-in accepts a valid 16-character Limited Access Torn API key.
- [ ] Invalid/revoked/expired Torn API key produces a clear recoverable error and does not enter the app as a verified user.
- [ ] Full Access is not required for ordinary member use.

## P1

- [ ] Legal & Privacy remains reachable from the compact sign-in notice.
- [ ] Legal & Privacy remains reachable from **More**.
- [ ] Legal & Privacy remains reachable from **Settings**.
- [ ] Legal & Privacy remains reachable from **About TornFCA**.
- [ ] Back navigation from each legal document returns to the expected legal hub/sign-in context.
- [ ] Long legal copy remains readable on a small phone without clipped controls or horizontal scrolling.

---

# 3. API-key storage and logout

## P0

- [ ] Default sign-in is session-only when **Save API key on this device** is off.
- [ ] 7-day encrypted retention works.
- [ ] 30-day encrypted retention works.
- [ ] 90-day encrypted retention works.
- [ ] API key is encrypted through the Android-backed secure store and is not stored as plaintext preferences/files.
- [ ] Changing storage back to Session stops future persistent retention.
- [ ] **Log Out / Change API Key** clears the active key and relevant identity/cache state.
- [ ] Logout unregisters push registration where configured.
- [ ] Logout never clears the user's already accepted legal version merely to force legal acceptance again.

## P1

- [ ] Expired encrypted retention removes/rejects the old saved key cleanly.
- [ ] Relaunch with a valid retained key reconnects without unnecessary duplicate API calls or visible identity flicker.

---

# 4. Navigation and readability

## P0

- [ ] Ordinary member bottom navigation remains compact: Home, Faction, War, More.
- [ ] Leadership tab appears only where the current verified role/access allows it.
- [ ] No user can reach a leadership-only screen merely by knowing an Activity/route name.
- [ ] Home provides an obvious everyday starting point.
- [ ] **More → Member Center** is easy to find.

## P1

- [ ] Member Center order is easy to scan:
  - [ ] Start here
  - [ ] Daily & readiness
  - [ ] Growth & training
  - [ ] My faction
  - [ ] Community & alerts
  - [ ] Optional upgrade
- [ ] Core/free features appear before Premium.
- [ ] Each card title describes what the player is trying to do rather than an implementation concept.
- [ ] Cards are tappable in addition to their explicit action buttons where intended.
- [ ] No important screen uses unexplained developer terms such as backend, tenant, entitlement, or permission-gated in ordinary player copy.
- [ ] Small-phone text wraps cleanly and primary controls remain visible.
- [ ] Large-display layout does not become excessively stretched or unreadable.
- [ ] Android font scaling at common accessibility sizes does not hide primary actions.

---

# 5. Free member core

## My Day — P0/P1

- [ ] P0: My Day loads for an ordinary faction member with a Limited Access key.
- [ ] P1: Bars/cooldowns/refills render without confusing stale placeholders.
- [ ] P1: Personal OC status is understandable.
- [ ] P1: Chain status is understandable.
- [ ] P1: Current/upcoming war context is understandable.
- [ ] P1: Faction standing/perks gracefully handle Torn fields being missing or changed.

## Faction Directory — P0/P1

- [ ] P0: Directory only shows the member's current verified faction roster.
- [ ] P1: Search works by expected member identifiers/names.
- [ ] P1: Basic member card does not expose leadership-only analytics.
- [ ] P1: Directory handles empty/loading/error states clearly.

## Training Center — P0/P1

- [ ] P0: Universal TornFCA starter guides are visible to ordinary members.
- [ ] P0: Current faction's rules/guides are isolated to the current verified faction.
- [ ] P1: Happy-jump/training guidance is readable and clearly presented as guidance rather than a universal faction rule.
- [ ] P1: When the shared Community backend is unavailable, universal/local portions still work and the unavailable shared content is explained clearly.

## My Training Progress — P0/P1

- [ ] P0: Reads only the signed-in player's own battle stats/personal drug totals for this feature.
- [ ] P0: Baseline is scoped to player + faction.
- [ ] P0: Switching factions does not carry an old faction baseline into the new faction view.
- [ ] P1: Stat deltas, Xanax delta, days tracked, and average/day are understandable.
- [ ] P1: Reset baseline works and cannot reset another player's data.
- [ ] P1: Personal baseline remains device-local and is not written into the shared faction guide/rules datastore.

## Faction Resources / onboarding — P0/P1

- [ ] P0: Onboarding/checklist state is scoped to player + faction.
- [ ] P0: Old faction resources disappear after a verified faction change.
- [ ] P1: New-member checklist is clear and useful even before faction leaders publish custom guides.
- [ ] P1: Faction Resources links to the expected training/directory/war/member pages.

## My War Prep — P0/P1

- [ ] P0: Checklist state is scoped to player + faction + war cycle.
- [ ] P0: A new war does not inherit completion state from a prior war.
- [ ] P1: Bars, cooldowns, travel, refills, OC, and ranked-war timing load clearly.
- [ ] P1: App does not claim universal energy/Xanax/travel requirements; faction-specific requirements remain in faction-authored resources/rules.
- [ ] P1: My Day / My War / Faction Resources shortcuts work.

## My War / OC / Chain — P0/P1

- [ ] P0: Ordinary member can view their personal/current relevant war participation.
- [ ] P0: Ordinary member OC view does not unlock faction-wide leadership data.
- [ ] P1: Recent completed-war history loads correctly.
- [ ] P1: Chain status remains useful when no chain is active.

---

# 6. Faction-local content and faction changes

## P0

Test this with two real/test Torn factions if possible.

- [ ] Sign in while belonging to Faction A and load its training rules/resources/chat.
- [ ] Change verified Torn faction membership to Faction B.
- [ ] Refresh/reopen training/resources.
- [ ] Faction A training rules/guides are no longer accessible.
- [ ] Faction A local onboarding/training-progress/war-prep state is not displayed as Faction B state.
- [ ] Faction B content appears only after Faction B is the verified current faction.
- [ ] Direct attempts to fetch/archive/edit a Faction A guide while verified in Faction B are rejected server-side.

## P1

- [ ] Returning later to Faction A does not corrupt Faction B's local state.
- [ ] Cached old faction identity never visibly overrides a freshly verified faction on protected shared-resource actions.

---

# 7. Leadership training/resource controls

## P0

- [ ] Ordinary member cannot save faction training rules.
- [ ] Ordinary member cannot publish/archive faction guides through direct route calls.
- [ ] Leader/Co-leader authorization is rechecked server-side.
- [ ] Guide archive/edit rejects a guide belonging to another faction.

## P1

- [ ] Leader/Co-leader can set expected stat gain.
- [ ] Leader/Co-leader can set regular Xanax expectation.
- [ ] Leader/Co-leader can include notes/exceptions.
- [ ] Leader/Co-leader can publish useful categories such as New Player, Training, Happy Jump, War Prep, Trading, Rules, or custom resources.
- [ ] Archived content disappears from ordinary member library as expected.
- [ ] Long guide text remains readable on mobile.

---

# 8. Community chat and notifications

## P0

- [ ] Faction Chat only displays verified current-faction content.
- [ ] Leadership channel cannot be opened/read by an ordinary member through direct calls.
- [ ] FCM service remains non-exported.
- [ ] Target-player notifications are ignored on another player's device/session.
- [ ] Target-faction notifications are ignored after a faction change.
- [ ] Torn API key is not stored as part of FCM registration.

## P1

- [ ] Notification permission prompt occurs at an appropriate time and denial does not break the app.
- [ ] Notification categories can be enabled/disabled.
- [ ] Notification Inbox retains intended recent alerts locally.
- [ ] Device sync/register action is understandable and safe to repeat.
- [ ] Chat/backend-unconfigured build gives a clear unavailable state instead of a crash/spinner forever.
- [ ] Network loss while reading/sending chat fails gracefully.

---

# 9. Optional FFScouter and TornStats

## P0

- [ ] Neither provider is contacted before its required explicit consent path.
- [ ] Disabling providers calls both FFScouter and TornStats consent-disable behavior where applicable.
- [ ] Real Torn API key is never injected into embedded JavaScript as a raw value.

## P1

- [ ] Provider terms/data-policy links open correctly.
- [ ] Provider unavailable/rate-limited state does not block core TornFCA member tools.
- [ ] Re-enable flow requires the intended consent again.

---

# 10. Premium boundary

## P0

- [ ] Core member navigation remains usable with no Premium entitlement.
- [ ] Core faction training/resources/war participation are not silently converted into Premium-only functionality.
- [ ] Leadership access is based on faction permissions/role, not payment.

## P1

- [ ] Premium preview clearly explains what remains free.
- [ ] Premium Insights opens only according to the intended player-ID/server entitlement state.
- [ ] Premium backend unavailable does not break free core navigation.

---

# 11. Existing leadership and legacy regression

## P0

- [ ] Activity Tracker still opens for authorized leadership.
- [ ] Faction Pulse still opens for authorized leadership.
- [ ] Armory Auditor still opens for authorized leadership.
- [ ] War payout remains leadership-gated.
- [ ] Banking flows remain scoped to user + faction where applicable.
- [ ] Developer preview/member preview cannot bypass leadership restrictions.
- [ ] Embedded tools use the native Torn API proxy/throttle and do not receive the real key in JavaScript.

## P1

- [ ] War payout handles upcoming/live/completed state correctly.
- [ ] Existing armory/xanax/training-calculator assets still render and accept input.
- [ ] Banking listener remains interaction-scoped and does not unexpectedly use Torn API calls.

---

# 12. Failure/degraded-state testing

## P0

- [ ] No network at launch does not expose stale protected faction content from another identity.
- [ ] Revoked API key after an authenticated session eventually forces a safe re-authentication path rather than silently trusting it forever.
- [ ] Shared backend errors do not leak another faction's data in fallback UI.

## P1

- [ ] Torn API 429/rate-limit response produces a recoverable state.
- [ ] Torn API 5xx response produces a recoverable state.
- [ ] Community backend unavailable leaves local/universal Training/Resources portions usable where designed.
- [ ] Firebase unavailable leaves the app usable without push.
- [ ] FFScouter/TornStats unavailable leaves core app usable.
- [ ] Empty/no current war, no chain, no OC, no published guides, and empty notification inbox all have understandable empty states.

---

# 13. Performance and API discipline

## P0

- [ ] A normal Member Center open does not fan out leadership-only API requests.
- [ ] Shared/embedded tools stay behind the central Torn API throttle.
- [ ] Rapid navigation does not trigger an uncontrolled request storm.

## P1

- [ ] Reopening Home/Member Center shortly after a load feels responsive and uses cache/deduplication where intended.
- [ ] Profile avatar refresh does not refetch on every layout pass.
- [ ] No obvious UI freeze during network requests on a typical Android phone.
- [ ] Scroll performance remains acceptable on long Member Center/legal/guide pages.

---

# 14. Google Play / production-readiness checks

These remain separate from app functional QA.

## P0 before Play production submission

- [ ] Google Play developer identity verification is successfully resubmitted/completed.
- [ ] Public HTTPS privacy-policy page is live, accessible without login, and no longer marked as a draft/noindex page.
- [ ] Public developer/privacy contact is present in the policy and Play listing.
- [ ] Play Data safety form matches the exact production APK/AAB and backend behavior.
- [ ] Any account-deletion requirement triggered by future account creation is implemented before that account system ships.
- [ ] Production app is signed with the correct permanent signing identity / Play signing configuration.
- [ ] Store listing/version/build/package identifiers all match the production candidate.

## P1

- [ ] Reviewer instructions explain Torn API-key sign-in without exposing a permanent real user's credential.
- [ ] Content rating, target audience, ads declaration, and app-access declarations are complete.
- [ ] Screenshots/listing copy reflect the member-first experience rather than only leadership screens.

---

# 15. Beta tester feedback prompts

Ask testers for concrete task feedback instead of “do you like it?”

- Could you find **My Day** immediately?
- If you wanted training help, where did you look first?
- Did **Member Center** feel organized or too long?
- Was any wording unclear or too technical?
- Could you tell which tools were personal, faction-shared, leadership-only, or optional Premium?
- Did the first-run Legal & Privacy flow make sense without feeling overwhelming?
- Did you understand why TornFCA asks for a Limited Access API key and whether it would be saved?
- Did anything appear to belong to the wrong faction after refreshing/changing faction context?
- Which screen felt slowest?
- What was the first thing you expected to do but could not find?

---

# Exit criteria for broad community beta

Broad community beta can proceed when:

1. Every applicable **P0** item is passing or explicitly documented as not applicable.
2. The exact candidate passes the four automated gates: Android build, Legacy Restriction Audit, Community Security Audit, and Member Core Audit.
3. At least one ordinary-member test path and one leadership test path are completed on real Android hardware.
4. Faction-switch isolation has been exercised rather than assumed from code review alone.
5. Upgrade installation has been tested with the permanent signing identity.
6. Any remaining P1 failures have a documented decision/workaround and do not threaten security, tenant isolation, or core navigation.
