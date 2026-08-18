# TornFCA Premium Plan

## Product principle
TornFCA Free is a complete faction companion. Premium sells convenience, aggregation, longer history, analytics, automation and personalization — not basic faction participation or faction authority.

Torn permissions remain the authorization boundary for faction administration. Premium never grants a user leadership access and essential leadership tools are not removed because a leader is Free.

For the detailed tool-by-tool release boundary, see `docs/PREMIUM_MATRIX_0.10.1.md`.

## v1.0 entitlement model

### FREE
Default for every authenticated Torn player.

### PREMIUM
Player-level entitlement keyed to the verified numeric Torn `player_id`. It follows the player between factions. The backend is authoritative; the Android app caches only backend-verified state.

A separate faction-wide paid tier is **deferred beyond v1.0**. The older Faction Pro concept is not a v1.0 dependency and must not be implied in current product copy or authorization logic.

## FREE CORE

### Member essentials
- Torn sign-in/API connection and identity.
- My Day.
- My War Prep.
- Ranked War current/upcoming status, score, recent history and standard detail.
- Territories and territory-war details.
- Personal OC and chain status.
- Training Center and personal Training Progress.
- Faction Overview, Directory and Resources/onboarding.
- Faction Chat and Notification Inbox.
- Standard notification categories and standard 15-minute war reminder.

### Leadership essentials
When the signed-in Torn account has the required faction permission:
- 7-day Activity Tracker.
- Basic Leadership Attention list.
- Basic WarPay calculator/current receipt workflow.
- Banking request/queue workflow.
- Current Armory Auditor.
- Training management and notice publishing.
- Existing OC/war administration.
- TornFCA community moderation when authorized.

### FFScouter / TornStats rule
Basic provider access is not a TornFCA Premium product. If a player separately opts in and their provider account/key is entitled to the data, TornFCA may surface that provider data in Free.

Premium can sell saved views, aggregation, history, comparisons and workflow around provider data, but it must not charge a second time merely to reveal raw provider data the player can already access.

## PREMIUM — v0.10.1 / v1.0 launch set

### Personal Insights
- 30-day personal outgoing activity trends.
- Ranked-war participation trends.
- Recent faction-war result summary.
- Personal local WarPay receipt analytics.

### Smart Alerts
- Selectable 15/30/60-minute war reminder lead time.
- Future advanced automation can extend this without removing the Free notification categories.

### Extended Activity Tracker
- Free leadership view: 7 days.
- Premium leadership view: selectable 14/30-day review.
- This makes Premium the deeper-history option while preserving a useful Free participation tool.

### Faction Pulse
- One-screen leadership convenience snapshot combining online/available state, hospital/jail/travel, OC gaps and territory-wall status.
- Underlying roster/status information remains available through Free tools.

### Member Dossier
- All-in-one advanced member-intelligence view combining Torn status with separately opted-in FFScouter/TornStats data.
- Free keeps Directory/basic member status and basic provider tools.

## Good Premium candidates after v1.0

- Saved scouting targets and quick views.
- Saved dashboard preferences and advanced filters.
- WarPay presets, rule templates, payout-model comparison, longer history, exports and analytics.
- Extended Armory audit history, saved reports and exports.
- Banking reconciliation history and exports.
- Advanced war-to-war participation analytics.
- OC/chain automated exception queues and follow-up workflow.
- Cosmetic personalization and saved layouts.

These are roadmap candidates, not implemented features unless separately shipped and tested.

## Payment / entitlement behavior

Current Premium backend source supports a configurable Torn in-game item-payment model and defaults to 15 Premium days per Xanax with stacking enabled. The payment backend must be deployed and live-tested before production monetization is considered active.

Production rules:
- Numeric Torn player ID is the entitlement identity.
- Status reads require the signed-in user's Torn key and can only read that user's entitlement.
- Admin grant/config changes require verified owner identity plus the admin password.
- Client-side payment claims never grant Premium.
- Expired entitlement falls back to Free.
- Remote `disable_premium`, maintenance and minimum-version controls override entitlement.

## Developer testing

Owner-only Premium simulation exists to test both matrix states. It defaults OFF, is accepted only for the verified developer player ID, is applied by `PremiumAccess`, and is never written into the backend entitlement cache.

## Release gate

Before v1.0.0:
1. Deploy/redeploy all five Apps Script backends.
2. Configure the backend URLs in the signed candidate.
3. Run the automated Premium Matrix audit.
4. Device-test Free member, Free leader, Premium member, Premium leader, expired Premium, remote Premium disable and owner simulation.
5. Verify basic FFScouter/TornStats use is identical for Free and Premium when provider consent/entitlement is the same.
6. Verify no essential leadership route requires Premium.
7. Verify backend entitlement refresh, expiration, revoke/grant and payment dedupe.
8. Complete the final privacy/data-safety and release checklist.

## Version policy

- **v0.10.1:** monetization matrix + pre-1.0 hardening.
- **v1.0.0:** only after live backends, signed build, device smoke test and release gates pass.
