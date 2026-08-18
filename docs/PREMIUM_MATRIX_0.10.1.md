# TornFCA v0.10.1 — Free / Premium Matrix

Status: pre-1.0 release gate. This document is authoritative for the v0.10.1 monetization boundary.

## Product rule

TornFCA Free must be a complete, useful faction companion. Premium is a convenience/depth tier: longer history, aggregation, automation, advanced intelligence presentation, saved workflow and personalization.

TornFCA must not charge merely because information is available from another service. In particular, basic FFScouter/TornStats provider data remains usable without TornFCA Premium when the player separately opts in and their provider account/key is entitled to the data.

Real Torn faction permissions always remain the authorization boundary for faction administration. Premium never grants leadership permission and lack of Premium never removes essential leadership permission.

## Free core

| Area | Tool / capability | v0.10.1 tier | Why |
|---|---|---|---|
| Account | Torn sign-in, API validation, encrypted/session key storage | Free | Required to use app |
| Home | Identity/profile hero, My Day, readiness shortcuts | Free | Core daily use |
| War | Current/upcoming Ranked War status and score | Free | Core participation |
| War | Recent Ranked War history and standard details | Free | Core participation/history |
| War | Territories, walls, assaults and territory details | Free | Core faction participation |
| War | My War Prep checklist | Free | Core readiness |
| War | Opponent FFScouter strength data using player's opted-in provider access | Free | Do not double-charge provider data |
| WarPay | Basic payout calculator and current receipt workflow | Free + Torn leadership permission | Competitive/basic faction administration |
| WarPay | Cloud receipt persistence currently implemented | Free + Torn leadership permission | Existing core workflow; Premium additions should be presets/analytics/exports |
| Activity | Faction Activity Tracker, 7-day view | Free + leadership | Useful basic participation review |
| Attention | Basic leadership attention list | Free + leadership | Essential exception visibility |
| Chain | Current chain status and participation | Free | Core participation |
| OC | Personal OC assignment/readiness | Free | Core participation |
| OC | Existing authorized leadership OC tools | Free + Torn permission | Essential administration |
| Members | Faction overview | Free | Core faction context |
| Members | Directory and basic member status | Free | Core roster access |
| Resources | Faction resources/onboarding/rules | Free | Core participation |
| Training | Training Center and faction guides | Free | Core participation |
| Training | Personal Training Progress/local baseline | Free | Core personal progress |
| Training admin | Publish/manage faction training guides | Free + Torn/leadership permission | Essential faction administration |
| Community | Faction Chat | Free | Core community feature |
| Community | Notification Inbox | Free | Core alerts |
| Notifications | War/OC/chain/faction/chat/personal categories | Free | Core alerts |
| Notifications | Standard 15-minute war reminder | Free | Core reminder |
| Intelligence | Faction Strength / FFScouter basic estimates | Free | Provider data is not a TornFCA paywall |
| Intelligence | TornStats basic opted-in provider access where surfaced | Free | Provider data is not a TornFCA paywall |
| Banking | Request/queue/current banking workflow | Free + Torn permission | Essential administration |
| Armory | Current Armory Auditor | Free + Torn permission | Essential/basic audit workflow |
| Notices | Current faction notice publishing/management | Free + Torn permission | Essential administration |
| Moderation | TornFCA community moderation | Free + moderation authorization | Safety function must not be paywalled |
| Settings | Legal/privacy, logout, provider consent, API retention | Free | Account/safety controls |

## Premium in v0.10.1

| Premium feature | Enforcement | Free alternative |
|---|---|---|
| Personal Insights | `PremiumAccess.PERSONAL_INSIGHTS` in `PremiumInsightsActivity` | My Day, War history, Training Progress and personal core tools |
| Smart alert timing | `PremiumAccess.ADVANCED_ALERTS` in `NotificationSettingsActivity` | Standard categories + 15-minute war reminder |
| Extended Activity Tracker | `PremiumAccess.EXTENDED_ACTIVITY` through `DeveloperSettings.activityDays()` | 7-day Activity Tracker remains free |
| Faction Pulse | `PremiumAccess.FACTION_PULSE` in `FeatureRouterActivity` | Faction Overview, Directory, OC/war status and basic Attention remain free |
| Member Dossier | `PremiumAccess.MEMBER_DOSSIER` in `FeatureRouterActivity` | Directory/basic member status + raw opted-in FFScouter/TornStats tools remain free |

## Premium candidates after 1.0

These are appropriate Premium additions because they are convenience/depth rather than core access. They are NOT considered implemented merely because they are listed here.

- Saved scouting targets and saved quick views.
- Extended WarPay history/analytics, payout presets, rule templates, comparison models, exports and audit packages.
- Extended Armory audit history, saved reports and exports.
- Banking reconciliation history and exports beyond the current queue/workflow.
- Advanced war-to-war analytics and configurable participation trends.
- Advanced OC/chain exception automation and follow-up queues.
- Dashboard personalization, saved filters and cosmetic options.
- Longer personal/faction retention where Torn/API availability permits it.

## Explicitly not Premium

- Torn faction leadership permissions.
- Safety/moderation controls.
- Basic FFScouter or TornStats access/values.
- Basic WarPay calculator.
- Current banking requests/queue.
- Current Armory Auditor.
- Faction chat.
- Core war/territory/chain/OC participation.
- Legal, privacy or security controls.

## Entitlement safety

- Production entitlement is backend-verified and keyed to numeric Torn player ID.
- `PremiumEntitlementStore` caches only backend-verified state and never grants developer simulation.
- Owner developer simulation defaults OFF and is accepted only by `PremiumAccess` when the verified player ID equals `BuildConfig.DEVELOPER_PLAYER_ID`.
- Remote `disable_premium`, maintenance mode and minimum-version policy always override both real entitlement and owner simulation.
- Expired cached entitlement is treated as Free.

## Release matrix tests

Before 1.0, verify all of the following:

1. Free member: every Free member tool opens; every Premium tool is blocked or previews correctly.
2. Free leader: essential leadership tools remain available according to Torn permissions; Pulse/Dossier remain Premium; Activity is limited to 7 days.
3. Premium member: Personal Insights and smart alert timing unlock; no leadership privilege is gained.
4. Premium leader: extended Activity, Pulse and Dossier unlock in addition to real Torn-authorized leadership tools.
5. Expired Premium: all Premium conveniences lock without breaking Free tools.
6. Remote `disable_premium`: all Premium conveniences fail closed while Free tools continue.
7. Maintenance/min-version: existing remote policy behavior remains authoritative.
8. Owner simulation: works only for verified owner ID; defaults OFF; does not modify cached entitlement.
9. Provider opt-in: FFScouter/TornStats basic access works for Free and Premium equally when provider consent/entitlement allows it.
10. Member Preview: never gains leadership access from Premium.

## Version policy

v0.10.1 is the pre-1.0 monetization/release-hardening line. v1.0.0 is reserved for the fully deployed, signed, device-tested production candidate after live backend smoke tests.
