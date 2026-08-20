# TornFCA v0.10.18 — Beta Hardening

Prepared: 2026-08-20
Branch: `work/v0.10.18-beta-hardening`

## Android

- Bumps Android beta to versionCode 73 / versionName 0.10.18.
- Adds an isolated Google Play Review Sandbox reachable before Torn sign-in on devices with no saved Torn key.
- Requires the reusable testing-only code `TORNFCA-PLAY-REVIEW`.
- Provides synthetic Member and Faction Leader personas with representative member, war, OC, training, announcements, chat, banking, notification, leadership, payout, and moderation surfaces.
- Review mode never creates a Torn AuthSession/API key, never calls Torn or TornFCA Apps Script backends, never registers a Firebase device, and never performs production writes.
- Review mode persists only its active/persona state locally and clearly marks every screen as synthetic.
- Existing authenticated TornFCA routing remains unchanged after choosing `Continue to TornFCA`.

## Community Backend v1.8.1

- Adds a fresh current-faction member check before every faction-wide FCM broadcast.
- Uses one `/faction/{factionId}/members` request per faction broadcast, not one per device.
- Filters stored PushDevices against the freshly verified member ID set before sending.
- Fails closed for faction push fan-out if membership verification fails while leaving the underlying chat/announcement/banking action intact.
- Refreshes security-sensitive faction and position verification with a unique Torn timestamp parameter.
- Preserves player-targeted cloud push test behavior and existing device-level push deduplication.

## Deployment boundary

- This branch is beta-only and is not a production deployment.
- Community Backend v1.8.1 must be deployed to the Community Apps Script only after beta regression testing.
- `main` / live remains untouched.
