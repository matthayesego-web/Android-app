# Duck Force Companion v0.8.0 Blueprint

## Product direction
Duck Force Companion is the faction operating layer: one place to simplify faction life, reduce manual leadership work, and keep members informed without duplicating Torn PDA.

## Navigation
### Home — everyone
One action-first screen answering: **What matters to me right now?**
- My OC summary and readiness
- My ranked-war participation / requirement progress
- Current chain status and whether I should act
- Priority faction notice
- Personal faction obligations / goals
- While You Were Away summary

### Faction — everyone
Shared faction-facing tools with member-safe data scopes.
- Overview: faction status, chain, current war, announcements
- My Participation: personal 7/30-day activity and requirement progress
- My OC: only the member's assigned OC details
- Intel: searchable member directory; public/safe information only
- Tools: member-facing requests and calculators that reduce faction friction

### Leadership — authorized positions/permissions only
One consolidated command workspace instead of separate home tiles.
- Members: Activity Tracker + Faction Pulse + Member Lookup combined
- War: War Participation / Command Center
- OC: full Open / Planning / Complete management
- Operations: Banking, Armory, notices, permissions/access
- Developer remains owner-only and outside normal leadership authorization

## Existing v0.7 feature mapping
| v0.7 feature | v0.8 destination |
|---|---|
| Faction Activity Tracker | Leadership > Members; personal slice in Faction > My Participation |
| Faction Pulse | Leadership > Members summary |
| Member Quick Lookup | Faction > Intel (safe scope) / Leadership > Members (full scope) |
| War Participation | Home personal card / Faction overview / Leadership > War |
| Chain Command Center | Home + Faction Overview; leadership details in Leadership > War/Command |
| OC Tracker | Home personal OC / Faction > My OC / Leadership > OC |
| Banking | Leadership > Operations; member request entry remains member-accessible where allowed |
| Armory Auditor | Leadership > Operations; future member request flow under Faction > Tools |
| Notices | Home priority notice + Faction Overview; management under Leadership > Operations |
| Premium Preview | Remove standalone home tiles; premium enhances existing screens |

## v0.8 implementation rules
1. Reduce navigation before adding more utilities.
2. Members never receive faction-wide private/leadership data merely because a screen exists.
3. Leadership is permission-gated from verified Torn session data, not mutable display names.
4. Owner/developer access remains separate from leadership access.
5. Shared records remain `faction_id` scoped even while production is Duck Force-only.
6. Existing banking behavior is not rewritten in v0.8; only navigation/entry points may move.
7. Premium never blocks core faction participation. It adds convenience/analytics later.

## New v0.8 concepts
### My Obligations
A normalized list of what the faction expects from the current member, designed to later support configurable leadership goals.

Initial signals can include:
- war participation requirement
- assigned OC / OC readiness
- current chain action signal
- priority notice acknowledgement/status where available

### While You Were Away
A compact digest of meaningful faction changes since the member last opened the app. v0.8 can begin with locally-derived/session-based events; durable cross-device history belongs to the later shared backend.

## UX target
Normal member: **Home | Faction**
Leadership member: **Home | Faction | Leadership**
Developer: same leadership/member UI plus an owner-only Developer entry.

Avoid exposing six separate intelligence tiles on Home. Home should prioritize status and actions, not tool discovery.

## v0.8 test gates
- Existing v0.7 login still works.
- Member session never sees Leadership navigation without qualifying permissions.
- Leadership session sees Leadership and retains current tools.
- Developer Console remains owner-only.
- OC member view exposes only the signed-in member's relevant OC information.
- Banking and Armory current flows are not broken by navigation changes.
- Production package remains `com.matthayesego.duckforcetoolkit`.
- Internal debug package remains separate and is never distributed as another user app.
- Compile debug + release.
- Emulator smoke: login + member Home/Faction + leadership preview + no fatal exception.

## Deferred from v0.8
- FFScouter production integration
- automatic banking reconciliation changes
- war payout automation
- armory request automation
- global multi-faction onboarding
- website

Those are prepared in the v0.9 framework or later after v0.8 real-world testing.
