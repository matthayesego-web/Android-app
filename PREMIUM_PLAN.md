# TornFCA Premium Plan

## Product principle
TornFCA Free is a complete faction companion. Premium sells convenience, aggregation, longer history, analytics, automation and personalization — not basic faction participation or faction authority.

Torn permissions remain the authorization boundary for faction administration. Premium never grants a user leadership access and essential leadership tools are not removed because a leader is Free.

For the detailed tool-by-tool release boundary, see `docs/PREMIUM_MATRIX_0.10.1.md`.

## v1.0 entitlement model

### FREE
Default for every authenticated Torn player.

### PREMIUM
Player-level entitlement keyed to the verified numeric Torn `player_id`. It follows the player between factions. The backend is authoritative; Android caches only backend-verified state.

A separate faction-wide paid tier is **deferred beyond v1.0**. The older Faction Pro concept is not a v1.0 dependency and must not be implied in current product copy or authorization logic.

## FREE CORE

### Member essentials
- Torn sign-in/API connection and identity.
- My Day.
- My War Prep.
- Ranked War current/upcoming status, score, recent history and standard detail.
- Territories and territory-war details.
- Personal OC and chain status.
- Training Center and personal Training Progress baseline/stat gains.
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

## PREMIUM — v0.10.18 / v1.0 launch set

### Personal Insights
- 30-day personal outgoing activity picture.
- 7-day pace compared with the previous 23 days using the same already-loaded history.
- Ranked-war participation and active-combat-day trends.
- Recent faction-war result summary.
- Personal local WarPay receipt analytics.

### Training Goal Pacing
- Free keeps the complete personal baseline/current stat-gain view.
- Premium can set a private total battle-stat target scoped to player + faction.
- Shows current progress, remaining stats, current gain/day and estimated time-to-goal when enough baseline history exists.
- Goal state remains device-local and creates no additional Torn API request.

### Smart Alerts
- Selectable 15/30/60-minute war reminder lead time.
- Future advanced automation can extend this without removing the Free notification categories.

### Extended Activity Tracker
- Free leadership view: 7 days.
- Premium leadership view: 30-day review.
- The internal Developer test control may still exercise shorter windows for QA, but the customer-facing Premium product is the deeper 30-day history.
- This makes Premium the deeper-history option while preserving a useful Free participation tool.

### Faction Pulse
- One-screen leadership convenience snapshot combining online/available state, hospital/jail/travel, OC gaps and territory-wall status.
- Underlying roster/status information remains available through Free tools.

### Member Dossier
- All-in-one advanced member-intelligence view combining Torn status with separately opted-in FFScouter/TornStats data.
- Free keeps Directory/basic member status and basic provider tools.

## Premium expansion candidates

These are appropriate next additions because they deepen an existing workflow rather than removing Free capability:

- WarPay saved payout presets, rule templates, payout-model comparison, longer receipt history, exports and analytics.
- Extended Armory audit history, saved reports, deltas, repeat-borrower signals and exports.
- Banking reconciliation/resolved-request history, aging and exports.
- Saved scouting targets and quick comparison views.
- Advanced war-to-war participation analytics.
- Leadership Attention custom rules, recurring-problem history and follow-up workflow.
- OC/chain automated exception queues and follow-up workflow.
- Saved dashboard preferences, advanced filters and cosmetic personalization.

These are roadmap candidates, not implemented features unless separately shipped and tested.

## Payment / entitlement behavior

Premium backend **v1.3.0** contains the server-verified entitlement model. The launch conversion is **7 Premium days per Xanax**, required message `TORNFCA`, and stacking enabled.

The backend also supports **Complimentary Premium**. The verified TornFCA owner can grant Premium directly to a numeric Torn player ID without a Xanax payment. Complimentary time uses the same entitlement/expiry model and is recorded with source `COMPLIMENTARY_GRANT`, keeping it distinguishable from Xanax receipts and developer-test grants.

Existing Premium sheets created before the seven-day launch conversion was finalized must run `applyPremiumSevenDayLaunchPricing()` or be updated through Premium Admin so `days_per_xanax=7` before paid scanning is enabled.

Automatic paid entitlement processing remains fail-closed unless deliberately approved/configured:

- setup defaults `MONETIZATION_APPROVED=false`
- the payment scan trigger cannot be installed while false
- the payment scanner refuses to process while false
- owner-only complimentary/developer grants remain available for testing/support without accepting a payment

Production entitlement rules:
- Numeric Torn player ID is the entitlement identity.
- Status reads require the signed-in user's Torn key and can only read that user's entitlement.
- Admin grant/config changes require verified owner identity plus the admin password.
- Client-side payment claims never grant Premium.
- Complimentary grants are server-side owner actions, never local client overrides.
- Expired entitlement falls back to Free.
- Remote `disable_premium`, maintenance and minimum-version controls override entitlement.
- Automatic receipt processing is locked/replay-safe and honors stacking only after the separate monetization gate is deliberately enabled.

## Developer testing

Developer Premium simulation exists to test both matrix states. It defaults OFF, requires a valid short-lived Developer Channel session, is applied only by `PremiumAccess`, and is never written into the backend entitlement cache.

For a stronger end-to-end test, use the deployed backend's owner-only developer or complimentary grant while automatic payment scanning is disabled.

## Release gate

Before v1.0.0 app release readiness:
1. Deploy/redeploy the required Apps Script backends, including Premium backend v1.3.0.
2. Apply/confirm seven-day Premium pricing on the deployed Premium sheet.
3. Configure the backend URLs in the signed candidate.
4. Run the automated Premium Matrix and v0.10.18 validation workflows.
5. Device-test Free member, Free leader, Premium member, Premium leader, expired Premium, remote Premium disable, developer simulation and Complimentary Premium.
6. Verify basic FFScouter/TornStats use is identical for Free and Premium when provider consent/entitlement is the same.
7. Verify no essential leadership route requires Premium.
8. Verify entitlement refresh, expiration, paid source labeling and complimentary source labeling.
9. Complete privacy/data-safety and Google Play release checks.
10. Produce the exact signed Play AAB and signed parallel-install beta APK from the permanent TornFCA signing identity.

## Version policy

- **v0.10.18:** Premium expansion + Play beta hardening line.
- **v1.0.0:** only after live backends, signed build, device smoke test and release gates pass.
