# TornFCA v0.10.18 — Free / Premium Matrix

Status: pre-1.0 release gate. The filename is retained for continuity, but this document is authoritative for the current v0.10.18 Free/Premium boundary.

## Product rule

TornFCA Free must be a complete, useful faction companion. Premium is a convenience/depth tier: longer history, aggregation, automation, advanced intelligence presentation, private goal pacing, saved workflow and personalization.

TornFCA must not charge merely because information is available from another service or because another standalone Torn tool performs a similar job. Free should get the job done; Premium should save time, remember more and turn existing data into deeper workflow.

In particular, basic FFScouter/TornStats provider data remains usable without TornFCA Premium when the player separately opts in and their provider account/key is entitled to the data.

Real Torn faction permissions always remain the authorization boundary for faction administration. Premium never grants leadership permission and lack of Premium never removes essential leadership permission.

## Free core

| Area | Tool / capability | v0.10.18 tier | Why |
|---|---|---|---|
| Account | Torn sign-in, API validation, encrypted/session key storage | Free | Required to use app |
| Home | Identity/profile hero, My Day, readiness shortcuts | Free | Core daily use |
| War | Current/upcoming Ranked War status and score | Free | Core participation |
| War | Recent Ranked War history and standard War History Detail | Free | Core participation/history |
| War | Territories, walls, assaults and territory details | Free | Core faction participation |
| War | My War Prep checklist | Free | Core readiness |
| War | Opponent FFScouter strength data using player's opted-in provider access | Free | Do not double-charge provider data |
| WarPay | Basic payout calculator and current receipt workflow | Free + Torn leadership permission | Core faction administration |
| WarPay | Cloud receipt persistence currently implemented | Free + Torn leadership permission | Existing workflow; Premium additions should be presets/analytics/exports |
| Activity | Faction Activity Tracker, 7-day view | Free + leadership | Useful basic participation review |
| Attention | Basic leadership attention list | Free + leadership | Essential exception visibility |
| Chain | Current chain status and participation | Free | Core participation |
| OC | Personal OC assignment/readiness | Free | Core participation |
| OC | Existing authorized leadership OC tools | Free + Torn permission | Essential administration |
| Members | Faction overview | Free | Core faction context |
| Members | Directory and basic member status / quick lookup | Free | Core roster access |
| Resources | Faction resources/onboarding/rules | Free | Core participation |
| Training | Training Center and faction guides | Free | Core participation |
| Training | Personal Training Progress/local baseline, current stats and gains | Free | Core personal progress |
| Training | Company Train Calculator embedded utility | Free | Basic calculator/convenience utility |
| Training admin | Publish/manage faction training guides | Free + Torn/leadership permission | Essential faction administration |
| Community | Faction Chat | Free | Core community feature |
| Community | Report/block controls | Free | Safety function |
| Community | Notification Inbox | Free | Core alerts |
| Notifications | War/OC/chain/faction/chat/personal categories | Free | Core alerts |
| Notifications | Standard 15-minute war reminder | Free | Core reminder |
| Notifications | Local/cloud notification diagnostics when available | Free | Setup/support function |
| Intelligence | Faction Strength / FFScouter basic estimates | Free | Provider data is not a TornFCA paywall |
| Intelligence | TornStats basic opted-in provider access where surfaced | Free | Provider data is not a TornFCA paywall |
| Banking | Request/queue/current banking workflow | Free + Torn permission | Essential administration |
| Armory | Current Armory Auditor | Free + Torn permission | Essential/basic audit workflow |
| Notices | Current faction/war notice publishing and management | Free + Torn permission | Essential administration |
| Moderation | TornFCA community moderation | Free + moderation authorization | Safety function must not be paywalled |
| Settings | Legal/privacy, logout, provider consent, API retention | Free | Account/safety controls |

## Premium in v0.10.18

| Premium feature | Enforcement | Free alternative |
|---|---|---|
| Personal Insights | `PremiumAccess.PERSONAL_INSIGHTS` in `PremiumInsightsActivity` | My Day, War history, Training Progress and personal core tools |
| Training Goal Pacing | `PremiumAccess.TRAINING_GOALS` inside `TrainingProgressActivity` | Full current stats, baseline, gains and Xanax progress remain Free |
| Smart alert timing | `PremiumAccess.ADVANCED_ALERTS` in `NotificationSettingsActivity` | Standard categories + 15-minute war reminder |
| Extended Activity Tracker | `PremiumAccess.EXTENDED_ACTIVITY` through `DeveloperSettings.activityDays()` | 7-day Activity Tracker remains free |
| Faction Pulse | `PremiumAccess.FACTION_PULSE` through the Premium route/target guard | Faction Overview, Directory, OC/war status and basic Attention remain free |
| Member Dossier | `PremiumAccess.MEMBER_DOSSIER` through the Premium route/target guard | Directory/basic member status + raw opted-in FFScouter/TornStats tools remain free |

### Training Goal Pacing boundary

The Premium layer may save a private target and calculate progress/pace from the personal battle-stat data already loaded by the Free Training Progress screen. It must not hide current stats, baseline gains, Xanax tracking or faction expectations from Free users. Goal state is device-local and scoped to player + faction.

## Not a customer tier

The following are privileged operator/developer surfaces, not Premium benefits and not Free-user product features:

- Developer Gate / Developer Panel / Advanced Developer Console.
- Developer Backend Control Plane.
- Premium administration/grant/configuration screen.
- Owner-only recovery/global moderation controls.

Paying for Premium never unlocks these surfaces.

## Premium expansion candidates

These are appropriate Premium additions because they are convenience/depth rather than core access. They are NOT considered implemented merely because they are listed here.

- Saved scouting targets and saved quick views.
- Extended WarPay history/analytics, payout presets, rule templates, comparison models, exports and audit packages.
- Extended Armory audit history, saved reports, deltas and exports.
- Banking reconciliation/resolved-request history, aging and exports beyond the current queue/workflow.
- Advanced war-to-war analytics and configurable participation trends.
- Leadership Attention custom rules, recurring-problem history and follow-up queues.
- Advanced OC/chain exception automation and follow-up queues.
- Dashboard personalization, saved filters and cosmetic options.
- Longer personal/faction retention where Torn/API availability permits it.

## Explicitly not Premium

- Torn faction leadership permissions.
- Safety/moderation controls.
- Basic FFScouter or TornStats access/values.
- Basic WarPay calculator and current receipt persistence.
- Current banking requests/queue.
- Current Armory Auditor.
- Company Train Calculator.
- Faction chat.
- Core war/territory/chain/OC participation.
- Standard war history/detail.
- Current personal training stats/baseline/gains.
- Legal, privacy or security controls.

## Entitlement safety

- Production entitlement is backend-verified and keyed to numeric Torn player ID.
- `PremiumEntitlementStore` caches only backend-verified state and never grants developer simulation.
- Developer simulation defaults OFF and is accepted only while a valid short-lived Developer Channel session exists; it does not modify cached entitlement.
- Remote `disable_premium`, maintenance mode and minimum-version policy always override real entitlement and simulation.
- Expired cached entitlement is treated as Free.
- Premium-only destination activities must defend themselves in addition to menu/router gating where applicable.
- Complimentary Premium is an owner-only backend mutation with source `COMPLIMENTARY_GRANT`; it is not a local override and does not grant faction authority.

## Release matrix tests

Before 1.0, verify all of the following:

1. Free member: every Free member tool opens; every Premium enhancement is blocked/previews correctly without removing Free data.
2. Free leader: essential leadership tools remain available according to Torn permissions; Pulse/Dossier remain Premium; Activity is limited to 7 days.
3. Premium member: Personal Insights, Training Goal Pacing and smart alert timing unlock; no leadership privilege is gained.
4. Premium leader: extended Activity, Pulse and Dossier unlock in addition to the member Premium benefits and real Torn-authorized leadership tools.
5. Expired Premium: all Premium conveniences lock without breaking Free tools or deleting the underlying Free baseline.
6. Remote `disable_premium`: all Premium conveniences fail closed while Free tools continue.
7. Maintenance/min-version: existing remote policy behavior remains authoritative.
8. Developer simulation: requires a valid Developer Channel session, defaults OFF and does not modify cached entitlement.
9. Complimentary grant: owner can grant time to a numeric player ID; recipient reads normal Premium; source is `COMPLIMENTARY_GRANT`; no Xanax total is incremented.
10. Provider opt-in: FFScouter/TornStats basic access works for Free and Premium equally when provider consent/entitlement allows it.
11. Member Preview: never gains leadership access from Premium.
12. Direct/internal launch defense: Premium-only target screens reject a Free launch even if the router is bypassed accidentally.
13. Operator isolation: Premium never unlocks Developer/Premium Admin surfaces.
14. Training goal scope: goals do not cross player/faction boundaries and do not create an extra Torn API request.

## Version policy

v0.10.18 is the Premium-expansion / Play-beta-hardening line. v1.0.0 is reserved for the fully deployed, signed, device-tested production candidate after live backend smoke tests.
