# TornFCA Premium Plan

## Product principle
TornFCA must remain useful to a normal faction member without payment. Premium should sell convenience, automation, history, analytics and shared leadership workflow — not access to the basic information a member needs to participate in their faction.

## Entitlement model
TornFCA should support three entitlement levels:

### FREE
Bound to the authenticated Torn `player_id` and current `faction_id`.

### PREMIUM PLAYER
A player-level entitlement for personal convenience features. It follows the Torn player between factions.

### FACTION PRO
A faction-level entitlement keyed by `faction_id`. Leadership purchases/manages the faction entitlement and the enabled Faction Pro tools become available to authorized members according to their real Torn faction permissions.

Entitlements must ultimately be verified by the TornFCA backend. Local toggles are acceptable only for developer/test preview and must never be the production authorization boundary.

---

# FREE CORE — keep free

## Home / identity
- Torn login/API connection
- Faction-adaptive TornFCA theme
- Current faction and position
- Player avatar/header
- Basic Home attention summary

## War
- Current/upcoming ranked-war status and score
- Recent ranked-war history and W/L results
- Personal war participation
- Opposing-faction FFScouter lookup using the player's own FFScouter entitlement
- Basic WarPay calculator for authorized leadership
- Current WarPay manual Torn payment handoff

## Faction
- Faction roster/directory
- Basic member status
- Personal faction status
- Basic Member Dossier
- Basic notices/resources

## Chain / OC
- Current chain status
- Personal OC assignment/readiness
- Basic participation/obligation visibility

## Leadership essentials
- Permission-aware Leadership hub
- Basic attention list
- Basic WarPay
- Existing essential faction operations that are required for ordinary faction participation

## FFScouter rule
TornFCA must not charge a second time for basic access to FFScouter data that the player is already entitled to receive from FFScouter. TornFCA Premium may add workflow, saved analysis and automation around FFScouter, while provider-level premium data remains controlled by FFScouter's own entitlement.

---

# PREMIUM PLAYER

Personal convenience features that do not require the whole faction to subscribe.

- Smart war-start / chain / OC reminders
- Configurable push notifications
- Advanced "While You Were Away" digest
- Longer personal participation history
- Personal performance trends
- Saved scouting targets and quick views
- Saved dashboard preferences
- Advanced personal filters/search
- Optional cosmetic personalization beyond the automatic faction theme

---

# FACTION PRO

High-value shared leadership tooling.

## WarPay Pro
- Saved payout presets per faction
- Saved slider profiles
- Rule/penalty templates
- Penalty reason library
- Approval/review workflow before payout
- Persistent payment queue state
- Payout history and audit trail
- CSV/copy/export packages
- Compare payout models before committing
- War-to-war payout analytics

## War operations
- War readiness dashboard
- Participation exception monitoring
- Advanced opponent comparison views
- Long-term war history analytics
- Member performance trends
- Automated pre-war / during-war attention queues

## Member intelligence
- Advanced Member Dossier
- Participation/OC/chain history
- Inactivity and availability trends
- Configurable faction requirements
- Requirement violation summaries

## OC / Chain automation
- OC exception monitoring
- Missing-assignment / readiness alerts
- Chain participation monitoring
- Automated leadership follow-up queues

## Armory / Banking workflow
- Extended armory audit history
- Saved audit reports
- Banking request reconciliation/history
- Leadership exports and audit trails

## Shared faction customization
- Leadership-selected faction accent/theme override
- Optional faction branding/header configuration
- Shared dashboard configuration
- Shared rule presets

---

# Closed beta premium strategy

Before monetization is live, closed-beta testers should be able to exercise Premium features without real charges through a developer/test entitlement mode. The UI should still visibly identify which features are Free, Premium Player, and Faction Pro so we can test the product boundary.

Before production monetization:
1. Build the TornFCA entitlement backend keyed by `player_id` and `faction_id`.
2. Integrate Google Play Billing for Android digital entitlements.
3. Verify purchase tokens server-side before granting production entitlements.
4. Add restore-purchase and entitlement refresh paths.
5. Add clear subscription/product descriptions and cancellation/support information.
6. Keep developer entitlement simulation unavailable in public production builds except behind the hidden password-protected developer console.

# Recommended rollout

- **v0.9.10:** Remove beta code, rebuild WarPay, finalize Premium plan.
- **v0.9.11:** Add entitlement framework + developer Premium/Faction Pro preview states.
- **v0.9.12:** Implement first Premium Player and Faction Pro feature gates.
- **v0.9.13:** Play Billing/backend integration and closed-beta readiness.
- **v1.0.0:** Production candidate only after multi-faction testing, privacy/data-safety review, billing verification and store review readiness.
