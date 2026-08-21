# TornFCA v0.10.18 — Beta Hardening

Prepared: 2026-08-20  
Branch: `work/v0.10.18-beta-hardening`

## Android / Play review

- Android versionCode 73 / versionName 0.10.18.
- Production package remains `com.matthayesego.duckforcetoolkit`; parallel sideload beta remains `.beta`.
- Targets/compiles against API 36 for the August 31, 2026 Google Play submission requirement.
- Adds an isolated Google Play Review Sandbox reachable before Torn sign-in on devices with no saved Torn key.
- Requires the reusable testing-only code `TORNFCA-PLAY-REVIEW`.
- Provides synthetic Member and Faction Leader personas with representative member, war, OC, training, announcements, chat, banking, notification, leadership, payout, and moderation surfaces.
- Review mode never creates a Torn AuthSession/API key, never calls Torn or TornFCA Apps Script backends, never registers a Firebase device, and never performs production writes.
- Review mode persists only its active/persona state locally and clearly marks every screen as synthetic.
- Existing authenticated TornFCA routing remains unchanged after choosing `Continue to TornFCA`.
- Adds branch-native compile/syntax validation and a manual signed Play AAB workflow.
- Adds a self-reporting beta build workflow that compiles even when permanent signing secrets are not yet restored, then signs automatically when those secrets exist.

## Premium expansion

Product rule remains: Free gets the complete faction companion; Premium adds depth, convenience and analytics without creating faction authority.

### Premium backend v1.3.0
- Launch conversion aligned to **7 Premium days per Xanax**.
- Adds `applyPremiumSevenDayLaunchPricing()` for existing deployments whose settings were created before the seven-day conversion was finalized.
- Keeps receipt processing deduped/ScriptLock-protected.
- Adds explicit owner-only **Complimentary Premium** with source `COMPLIMENTARY_GRANT`.
- Complimentary time uses the same player-ID entitlement/expiry model, does not require a Xanax receipt and does not increment Xanax totals.

### Member Premium
- Personal Insights now compares the last 7-day combat/war pace with the previous 23-day pace inside the same 30-day scan.
- Adds Premium **Training Goal Pacing** to the existing Free Training Progress screen.
- Free keeps current battle stats, faction-scoped baseline, stat gains, Xanax progress and faction expectations.
- Premium goal state is private/device-local/player+faction scoped and adds target progress, remaining stats, gain/day and pace-based ETA without an extra Torn request.
- Smart alert lead-time selection remains Premium while standard notification categories and 15-minute war reminders remain Free.

### Leadership Premium
- Free leadership keeps the 7-day Activity Tracker and basic Needs Attention list.
- Adds **Premium Activity Trends**, which independently re-verifies current leadership and Premium before scanning a 30-day activity window.
- Activity Trends compares the last 7-day member mention pace with the previous 23 days, surfaces positive/negative momentum and keeps a shortcut back to the Free Activity Tracker.
- Existing Faction Pulse and Member Dossier remain Premium convenience/intelligence views while underlying roster/status/provider access remains available through Free tools.

## Community Backend v1.8.1

- Adds a fresh current-faction member check before every faction-wide FCM broadcast.
- Uses one `/faction/{factionId}/members` request per faction broadcast, not one per device.
- Filters stored PushDevices against the freshly verified member ID set before sending.
- Fails closed for faction push fan-out if membership verification fails while leaving the underlying chat/announcement/banking action intact.
- Refreshes security-sensitive faction and position verification with a unique Torn timestamp parameter.
- Preserves player-targeted cloud push test behavior and existing device-level push deduplication.

## Current validation state

- Apps Script syntax validation has passed with Premium backend v1.3.0.
- Android beta compilation has passed during the v0.10.18 Premium work; the exact feature-frozen snapshot must receive one final green compile before device handoff.
- Permanent signing identity is verified against the private backup, but the four Android signing values still need to be restored as GitHub Actions secrets before CI can output the permanently signed beta APK / Play AAB.

## Deployment boundary

- This branch is beta-only and is not a production deployment.
- Premium Backend v1.3.0 is source-ready but must be deployed and smoke-tested before Complimentary Premium / seven-day conversion is treated as live.
- Community Backend v1.8.1 must be deployed and smoke-tested before production community push/moderation readiness is claimed.
- `main` / live remains untouched.
