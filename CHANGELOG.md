# Changelog

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
