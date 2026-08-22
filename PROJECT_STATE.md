# TornFCA Project State

This is the durable recovery note for TornFCA. Read this first after any lost chat/session. Update it whenever branch policy, release state, architecture, major features, deployment assumptions, restore points, or next work changes.

## Branch policy
- `main` = Google Play / production source. Do not develop directly here.
- `development` = accepted next-version development source.
- `work/*` = temporary implementation/validation branches created from `development`.
- `restore/*` = recovery snapshots before risky changes.
- Production launcher identity = **T**. Development launcher identity = **D**.
- Never merge to `main` merely to produce a test APK.

## Current versions
- Production `main`: **v0.10.19**, versionCode **74**. This remains the Google Play line.
- Accepted `development`: **v0.10.25**, versionCode **80**, Development package `com.matthayesego.duckforcetoolkit.beta`, visible name **TornFCA Development**.
- v0.10.23 feature PR: **#24**, validated and merged as `3785b273575499e97094d7b4f9140795c30d3b28`.
- v0.10.24 navigation PR: **#25**, validated/merged as `aba8e7d61b9fb9016588e8d3790196a23e47a6ca`, but device testing proved its navigation changes targeted the non-canonical shell and therefore were not visible after login.
- v0.10.25 canonical-shell correction PR: **#26**, validated and merged as `5d338de150083caa4bc598b7d62b39e088c02bf2`.
- Validated v0.10.25 APK SHA-256: `27677b9bd1a14adb818d6a3e7c05ec1a0f1b11f89ae60968f831a3c1b50d86f1`.
- Pre-feature restore: `restore/development-pre-v0.10.23-features-2026-08-22`.
- Feature-complete restore before navigation: `restore/development-v0.10.23-feature-complete-2026-08-22`.
- v0.10.24 mismatch restore: `restore/development-v0.10.24-beta-shell-mismatch-2026-08-22`.
- Accepted v0.10.25 navigation-fixed restore: `restore/development-v0.10.25-navigation-fixed-2026-08-22`.
- Older restore points remain available for pre-v0.10.21 and pre-v0.10.22 states.

## Core architecture/security
- Android native app, Java/Kotlin Gradle project, compile/target SDK 36, min SDK 24.
- Torn API key is stored through `SecureApiKeyStore`; never commit/expose user keys.
- Torn API v2 supplies authenticated player/faction data.
- Shared `TornApiClient` owns Torn request throttling/caching and keeps sustained direct Android traffic around 30 calls/minute.
- Separate TornFCA backend endpoints exist for faction, community, premium, developer, WarPay and feedback.
- Firebase/FCM supplies cloud push where configured.
- Android signing material remains in GitHub secrets; Development CI permanently signs and verifies test APKs.
- Play Review mode remains synthetic/isolated from Torn and production backend writes.
- Android UI visibility is never the sole security boundary; backend-controlled actions must independently verify identity/role.
- **Canonical visible authenticated shell is `BetaCommandActivity` via `TornFcaCommandRuntime.homeIntent()`.** Do not audit or redesign only `TornFcaCurrentActivity` and assume users will see it.

## Accepted notification / live features
- First authenticated launch asks for Android notification permission; denial is remembered instead of nagging every launch.
- Automatic FCM initialization/sync.
- Notification deep links route to relevant Chat/War/Chain-OC/Banking/Announcements/Moderation destinations.
- Opt-in War Chain Live Tracker foreground service:
  - ~30s Torn refresh while chain active
  - ~60s during Ranked War without active chain
  - local second-level timeout chronometer
  - Android 16 promoted-ongoing/Live Update request where eligible
  - older Android uses ongoing notification.

## Chat state
### Native TornFCA chat
- Community chat supports General / War / OC / Leadership channels.
- Report/Block are not giant permanent per-message buttons; tapping another member message exposes contextual actions.
- Leadership has Reports & Moderation UI.
- One normal app-navigation home for Chat: **Faction -> Faction Chat**.
- Community backend must still be verified/enabled for Leader/Co-leader moderation policy before production promotion.

### Real Torn faction chat experiment
- `RealTornChatActivity` hosts Torn itself in a foreground-only WebView and tries to expand Torn's real faction-chat DOM.
- Torn owns website authentication, chat transport and send behavior.
- WebView is blanked when backgrounded; do not convert it into hidden scraping/background alerts.
- Torn API key cannot create a Torn website login.
- Google/Apple OAuth cannot be assumed to work inside Android WebView; user is still working on a normal Torn web login/session.
- Torn Chat is Sendbird-backed; Torn PDA proves native Sendbird faction chat is technically possible, but its privileged provider credentials are not ours and must never be copied/extracted/shipped.
- Continue researching a legitimate user-scoped Torn/Sendbird bootstrap.
- Research: `docs/TORN_CHAT_INTEGRATION_RESEARCH.md`.

## v0.10.23 accepted feature layer
### Spy Intel
- `SpyIntelActivity` is leadership-only in normal navigation.
- Reads official Torn faction `stats` reports from `/faction/reports`.
- Supports Torn player-ID lookup and recent faction spy list.
- Displays STR/DEF/SPD/DEX/total where returned.
- Null stats remain `Unknown`; app visibly distinguishes **FULL SPY** vs **PARTIAL SPY**.
- Shows report freshness/timestamp/reporter.
- Statistical estimates are never presented as official spies.

### RW Cache Market Advisor
- `CacheMarketAdvisorActivity` is leadership-only in normal navigation.
- Starts from completed Ranked War reports and reads official reward item IDs/names/quantities.
- Compares current official Item Market average/current asks and market cache freshness.
- Reads Torn's specialized Bazaar directory as context; does not invent bazaar prices because that API shape does not expose per-item prices.
- Scans recent public forum threads for likely cache-buying leads and conservatively parses recognizable percentage/cash offers.
- Forum values are clearly labeled leads/heuristics requiring verification, never guaranteed quotes.
- Decision support only: TornFCA never executes a trade or sale.

### Revive requests
- **Deferred/back burner** until the user obtains permission from Nuke.
- Nuke integration should be server-to-server; never embed a Nuke integration key in the APK.
- Partner handoff: `docs/NUKE_REVIVE_PARTNER_HANDOFF.md`.
- Research: `docs/REVIVE_REQUEST_INTEGRATION_RESEARCH.md`.

## v0.10.24 / v0.10.25 navigation architecture
Detailed design/audit: `docs/NAVIGATION_AUDIT_V01024.md` and `docs/NAVIGATION_INFORMATION_ARCHITECTURE_PLAN.md`.

### Important correction discovered by device test
- v0.10.24 implemented the desired hierarchy in `TornFcaCurrentActivity` and its CI audited that class.
- The real authenticated runtime had already standardized on `BetaCommandActivity` through `TornFcaCommandRuntime.homeIntent()`.
- Result: v0.10.24 showed the new version number but still displayed the old Home / Members / Training / Operations / More shell on-device.
- v0.10.25 fixes the **actual `BetaCommandActivity` command shell** and updates CI to prove the runtime launches the same class being audited.
- CI now fails if Members, Training or Operations reappear as permanent bottom tabs.

### Top-level navigation
Leadership account:
- **Home**
- **Faction**
- **War**
- **Leadership**
- **More**

Normal member:
- Home / Faction / War / More (Leadership hidden).

**Training is no longer a permanent bottom tab.**

### Home
- My Day
- Training & Progress -> Training Center / My Training Progress / My War Prep
- Notification Inbox
- No permanent leadership/admin cards.

### Faction
- Faction Chat as the single normal prominent chat entry
- Faction Announcements
- Faction Overview
- Faction Directory
- Faction Resources
- Faction Tools -> My OC / Chain Status / Faction Strength Intel

### War
- Ranked War
- Chain Status
- Territories
- My War Prep
- No payout/banking/cache/armory admin clutter.

### Leadership
Top-level job groups:
- Needs Attention
- People & Activity
- War & Intel
- Finance & Assets
- Faction Admin

People & Activity:
- Activity Tracker
- Faction Pulse
- Member Dossier

War & Intel:
- Spy Intel
- Faction Strength Intel estimates
- Ranked War Payout Calculator

Finance & Assets:
- Banking
- RW Cache Market Advisor
- Armory Auditor

Faction Admin:
- Announcement Management
- Guide & Training Management
- Reports & Moderation

The vague **Operations/Faction Operations** navigation bucket is retired.

### More
- Settings
- Notification Inbox
- TornFCA Premium
- Feedback & Requests
- Legal & Privacy
- About TornFCA

Feedback is restored to visible navigation. Developer/test tools are not ordinary public navigation cards.

### Navigation rules
- Target 3-6 persistent cards per page; larger feature families use named sub-pages.
- One obvious primary home per feature; duplicate only for genuine context (e.g. Chain in Faction Tools and War).
- Leadership/admin controls must not leak into member pages merely because the signed-in user happens to be a leader.
- Chat remains one normal Faction destination, not a global button.
- Shell Back from a sub-page returns to its parent before leaving the shell.
- Any future navigation CI must validate the **canonical runtime destination**, not merely a similar/legacy Activity.

## Premium/admin state
- v0.10.22 removed the separate Premium Developer Password from Android and stopped sending `admin_password`.
- Canonical `backend/TornFcaPremiumBackend.gs` v1.4.0 uses verified Torn owner identity for admin mutations.
- **Backend deployment is separate from GitHub source. Do not claim Premium backend v1.4.0 is live until its Apps Script deployment is published/verified.**
- Future desired work:
  - delegated Premium admin via real Developer Console session/token, not UI-only access
  - target Premium status lookup
  - grant/extend/revoke with audit history
  - restrained verified-Premium gold styling.

## Remaining validation / follow-up
- Device-test **v0.10.25 Development** as member/leadership where possible:
  - leadership bottom bar must show Home / Faction / War / Leadership / More
  - normal member bottom bar must show Home / Faction / War / More
  - no Members / Training / Operations permanent tabs
  - Training & Progress opens from Home and Back returns Home
  - no leadership cards leaking into Home/Faction/War
  - leadership submenus and Back behavior
  - Spy Intel runtime response shape/permissions
  - Cache Market Advisor runtime reward/market/forum response handling
  - Feedback route
  - Chat one-location rule
  - D Development launcher identity.
- Continue Real Torn Chat login/device testing once Torn website login is available.
- Verify/enable Community backend Leader/Co-leader moderation policy.
- Publish/verify Premium backend v1.4.0 when ready.
- Continue War Chain Live Tracker real-device tuning.
- Revive implementation waits for Nuke approval/credentials.
- Before Play promotion, perform targeted public-build cleanup of debug/test controls and verify production T launcher identity.

## Release discipline
1. Read this file plus relevant research/audit doc before work.
2. Compare `main...development`.
3. Create a `restore/...` snapshot before risky changes.
4. Implement on `work/...`.
5. PR into `development`.
6. Require Development CI compile + permanent signing + package/version/label + applicable architecture/regression preflight.
7. For navigation work, verify the runtime launch target and audit that exact shell class.
8. Merge only after CI passes.
9. Device-test signed Development APK.
10. Promote to `main` only by explicit release action after production checks; preserve production T icon.

## Secret/safety recovery rule
Never expose or commit Torn API keys, Firebase secrets, Android signing material, Apps Script service credentials, Nuke integration keys, or privileged third-party Sendbird credentials. Reconstruct future context from GitHub state and this file rather than inventing credentials or architecture.
