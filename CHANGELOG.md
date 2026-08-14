# Changelog

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
