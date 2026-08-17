# Changelog

## v0.9.26 — 2026-08-17

- Completed a readability-only polish pass after the v0.9.25 navigation/legal foundation, without changing feature access or permission boundaries.
- Simplified Member Center descriptions so ordinary members see player-facing language instead of terms such as member-safe, permission-gated, backend configured, or entitlement state.
- Simplified About, Settings, and More descriptions while keeping the same legal, privacy, provider-consent, Premium, and leadership behavior.
- Reduced the API sign-in notice to the essentials: use a Limited Access key, choose session-only or encrypted storage, and note that leadership tools may require in-game Faction API Access.
- Made the compact sign-in notice open Legal & Privacy directly when tapped.
- Strengthened the Community Security Audit so optional-provider opt-out is verified by the actual FFScouter and TornStats consent-disable calls rather than by a specific button label.
- Preserved the compact Home/Faction/War/(Leadership)/More navigation and the task-grouped Member Center rather than adding more bottom-navigation items.
- Bumped Android versionCode to 43 and versionName to 0.9.26.

## v0.9.25 — 2026-08-17

- Reorganized the free Member Center around plain-language workflows: **Start Here**, **Daily & Readiness**, **Growth & Training**, **My Faction**, **Community & Alerts**, and **Optional Upgrade**.
- Kept everyday navigation intentionally compact rather than adding more bottom-navigation tabs; Home/Faction/War remain the primary routes and deeper member tools are organized under Member Center.
- Added versioned first-run legal acknowledgement before Torn API sign-in.
- Added in-app **Privacy Policy**, **Terms & Conditions**, and **End User License Agreement** screens.
- Added persistent access to Legal & Privacy from More, Settings, and About TornFCA.
- Added a device-local legal-version/timestamp record so materially revised legal documents can require renewed acknowledgement later.
- Added a Google Play release/Data safety checklist grounded in TornFCA's current Torn API, device storage, faction community, FCM, FFScouter, and TornStats architecture.
- Added a clearly marked web privacy-policy publication draft; it remains non-production until a public privacy contact is chosen and HTTPS hosting is enabled/tested.
- Expanded the Member Core CI audit to protect member navigation, free-core discoverability, legal routing, local privacy boundaries, and faction isolation.
- Bumped Android versionCode to 42 and versionName to 0.9.25.

## v0.9.24 — 2026-08-17

- Added free **My War Prep** for ordinary faction members.
- Added current/upcoming ranked-war timing plus the signed-in member's own bars, cooldowns, travel, refills, and organized-crime context.
- Added a five-step personal readiness checklist scoped to `player_id + faction_id + war cycle`, preventing checklist state from carrying into another faction or future war.
- Added shortcuts from War Prep to My War, My Day, Faction Resources, and faction community tools.
- Kept faction-specific requirements in faction-authored rules/guides rather than inventing universal energy, Xanax, travel, or war-readiness requirements inside TornFCA.
- Preserved the v0.9.23 faction-resource restore point before introducing War Prep.
- Bumped Android versionCode to 41 and versionName to 0.9.24.

## v0.9.23 — 2026-08-17

- Added **Faction Resources** to the free Member Center for ordinary faction members.
- Added a five-step local onboarding checklist covering faction identity, training expectations, organized crime readiness, war/chain expectations, and faction guides.
- Scoped onboarding progress to both `player_id` and `faction_id`, so joining a different faction starts a separate checklist rather than carrying another faction's onboarding state forward.
- Added quick links from Faction Resources to My Day, Training Center, Faction Directory, and My War.
- Reused the existing freshly verified faction-scoped guide library for onboarding, training, trading, war prep, community rules, and other faction-local member resources; no new backend tenant datastore was introduced.
- Generalized Leader/Co-leader publishing from training-only guides to broader faction member guides while retaining the same server-side faction/leadership authorization and archive controls.
- Kept Faction Resources useful when the shared backend is unavailable: local onboarding and native member shortcuts continue to work while shared faction guides report their unavailable state clearly.
- Bumped Android versionCode to 40 and versionName to 0.9.23.

## v0.9.22 — 2026-08-17

- Added **My Training Progress** to the free Member Center.
- Added private personal battle-stat progress using the signed-in player's own Torn battle stats only.
- Added device-local baselines scoped to both `player_id` and `faction_id`, so progress comparisons do not carry across factions.
- Added total battle-stat gain, per-stat deltas, Xanax-use delta, time tracked, and average Xanax use per day since baseline.
- Added side-by-side display of the current faction's published training expectations when the faction Community backend is available.
- Kept personal battle-stat baselines on the device; they are not written to the faction training library or exposed to ordinary faction members.
- Kept faction-wide training compliance, violation summaries, long-term trend reporting, and automated follow-up outside the free member screen for future permission-aware Faction Pro tooling.
- Added a member-controlled **Reset progress baseline** action.
- Bumped Android versionCode to 39 and versionName to 0.9.22.

## v0.9.21 — 2026-08-17

- Expanded the free member core with a searchable faction directory and member-safe roster cards.
- Added a free Training Center with universal TornFCA starter guides.
- Added faction-scoped training rules so Leader/Co-leader can publish stat-gain expectations, regular Xanax expectations, and notes/exceptions.
- Added a faction-scoped custom training guide library with publishing and archive controls.
- Enforced guide/rule tenant isolation by verified `faction_id`; every training action now performs a fresh Torn faction check so old-faction content is revoked immediately after a faction change.
- Kept advanced member history, trend/compliance analytics, and automated follow-up in the Premium/Faction Pro roadmap rather than paywalling basic member guidance.
- Updated the Google Play/public-release roadmap to make EULA, Terms & Conditions, About/login acknowledgement, Privacy Policy, and Data safety explicit production gates.
- Updated Community backend schema to add `TrainingRules` and `TrainingGuides` while preserving existing chat/push data when setup is rerun.
- Bumped Android versionCode to 38 and versionName to 0.9.21.

## v0.4.3 — 2026-08-14

- Restored the Duck Force noir artwork after the v0.4.2 image-rendering regression.
- Replaced the login emoji/compound-drawable substitution with a real native ImageView.
- Rewired the launcher icon to the packaged noir duck artwork directly, with a simple adaptive inset for modern Android launchers.
- Removed the fragile icon resource chain that could fall back to Android's generic robot placeholder.
- Kept the compact professional v0.4.2 login layout and dark/gold companion styling.
- Bumped Android versionCode to 8 and versionName to 0.4.3.
- Continues using the permanent Duck Force release certificate established in v0.4.0.

## v0.4.2 — 2026-08-14

- Switched the visual direction to the user-provided detailed noir Duck Force artwork.
- Redesigned the login screen into a more compact professional welcome/sign-in layout.
- Reduced oversized borders and dead space on the login screen.
- Refined the API-key field, typography, spacing and gold primary action.
- Bumped Android versionCode to 7 and versionName to 0.4.2.

## v0.4.1 — 2026-08-13

- Replaced the launcher presentation with a professional noir Duck Force adaptive icon designed to fill Android circle/squircle masks cleanly.
- Added a dedicated v0.4.1 presentation layer while preserving the working v0.4.0 companion behaviour.
- Replaced the toy-like duck emoji splash mark with the Duck Force noir badge artwork.
- Tightened typography, menu labels, elevation and spacing for a cleaner professional companion look.
- Reduced emoji-heavy tool naming in the faction-facing menu and strengthened visual separation of companion, leadership and private tools.
- Refined the dark charcoal / muted-gold color system used by Android resources.
- Bumped Android versionCode to 6 and versionName to 0.4.1.
- Continued using the permanent Duck Force release certificate established in v0.4.0 so v0.4.1 installs as a normal update.

## v0.4.0 — 2026-08-13

- Reframed the app as the Duck Force Faction Companion rather than a generic utility launcher.
- Added a new companion-first launcher/home screen with faction-facing and private sections.
- Added a Banking Companion prototype with local payout requests, full-balance requests, notes, request history, Red/Black/global queue-visibility messaging, and the $1,000,000 retroactive low-balance rule.
- Added an Owner / Developer role tied to Torn player ID 3987363 rather than to an API-key value.
- Moved Company Train Calculator into the Owner-only My Tools area.
- Added a Developer Console foundation for private tools, future individual grants, delegated developers and beta features.
- Kept Leader/Co-leader leadership controls and faction-chat listener guidance separate from ordinary member features.
- Added a dedicated native tool host for the existing Armory Log, Faction Xanax Auditor and Company Train Calculator.
- Redesigned the launcher artwork as a full noir detective-duck badge and added Android adaptive-icon resources.
- Bumped Android versionCode to 5 and versionName to 0.4.0.
- Added CI generation of both debug and unsigned release APKs so release artifacts can be signed with the permanent Duck Force release certificate.
- Began the release-signing foundation intended for all direct-distribution builds from v0.4.0 onward.
- Play Store readiness remains targeted for v0.7.0.

## v0.3.0 — 2026-08-13

- Added a new polished native ToolkitActivity and made it the Android launcher.
- Rebuilt the API-key entry screen as a Duck Force branded login/splash experience.
- Reworked the main menu into clean role-aware cards with a compact account/access header.
- Added a Leader/Co-leader Leadership Control Center.
- Added an in-app Chat Listener details/install guide.
- Bundled Duck Force Banking Chat Listener v0.3.0 as an Android asset.
- Chat listener recognizes banker, balance-check, withdrawal/cash-out, and amount-specific requests such as “bank 25m”.
- Chat listener is designed to scan both currently loaded faction-chat history and newly loaded messages, with local deduplication.
- Documented the $1,000,000 default low-balance / likely-already-paid reconciliation rule.
- Corrected the Train Payment Calculator branding: “train” now clearly means company employee training, with a 🏋️ icon instead of a railway train.
- Shared banking queue deployment/export remains the next integration step; listener source is already bundled in the project.

## v0.2.0 — 2026-08-12

- Added API-key-first Torn authentication and automatic saved-session verification.
- Encrypts the user's saved Torn API key with Android Keystore.
- Restricted the prototype to verified Duck Force members.
- Added Torn faction-position permission tiers: Green, Orange, Red, and Black.
- Added Leader/Co-leader global access and a Rank Access Control screen.
- Red and Black positions receive global tool access; Orange receives elevated access; Green receives member access.
- Existing Torn web tools automatically receive the app's saved API key.
- Added a shared Google Apps Script / Google Sheets access-backend scaffold for future per-rank and per-user overrides.
- Duck Force numeric faction ID remains a temporary placeholder until the first verified login reveals it.

## v0.1.1 — 2026-08-12

- Fixed Android status-bar and active-call overlay collisions on modern edge-to-edge devices.
- Added dynamic top, bottom, left, and right system-bar/cutout insets to the home screen and tool screens.
- Updated the Android app version to 0.1.1.

## v0.1.0 — 2026-08-12

- Created the first combined Android prototype.
- Added native launcher/home screen for all three Torn utilities.
- Bundled Torn Faction Xanax Auditor v2.0.
- Bundled Torn Xanax Armory Log v1.2.
- Bundled Torn Train Payment Calculator v1.0.
- Isolated each tool's local browser storage.
- Added a Torn API WebView proxy for reliable in-app requests.
- Added GitHub Actions automatic APK builds.