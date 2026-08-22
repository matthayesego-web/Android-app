# TornFCA — Navigation / Information Architecture Simplification Plan

Status: planning. Perform this audit before allowing major new features to create more top-level cards.

## Goal

Make TornFCA understandable at a glance. Every feature should have one obvious home. Leadership-only/admin tools should not leak into normal member pages just because the signed-in user is a leader. Contextual actions should appear only when relevant.

## Proposed top-level navigation

Target a maximum of five bottom destinations for leadership users and four for ordinary members:

1. **Home**
2. **Faction**
3. **War**
4. **Leadership** — visible only to verified leadership
5. **More**

Remove **Training** as a permanent bottom tab. Training becomes a clear second-level destination from Home / My Character rather than consuming permanent navigation space.

## Home

Purpose: "What matters to me right now?"

Keep this page short and dynamic rather than a catalog of all features.

Suggested sections:
- My Day / current bars, cooldowns, OC and readiness
- My Character / Training & Progress
- Current contextual action(s), only when relevant:
  - Request Revive when hospitalized
  - active chain/war status
  - current OC readiness/problem
  - urgent notification/announcement
- Notification inbox / recent alerts

Do not add leadership/admin controls here.

## Faction

Purpose: shared member-facing faction information and communication.

Primary destinations:
- Faction Chat
- Announcements
- Faction Overview
- Directory
- Resources / Rules / Guides
- Faction Tools subpage for lower-frequency member utilities such as OC, Chain status and Strength Intel

One normal Chat entry only.

## War

Purpose: everything a member needs while preparing for or participating in warfare.

Primary destinations:
- Ranked War
- Chain
- Territories
- My War Prep

Contextual elements:
- hospital/revive action when hospitalized during a war
- live chain tracker state
- relevant opponent intel that the member is permitted to see

Do not mix payouts, banking or admin publishing into the member War page.

## Leadership

Purpose: all leadership-only work. Leadership features should live here rather than appearing on Home, Training or unrelated member pages.

### People & Activity
- Needs Attention
- Activity Tracker
- Faction Pulse
- Member Dossier

### War & Intel
- Spy Intel
- opponent intelligence
- leadership war review
- Ranked War payout review/calculator

### Finance & Assets
- Banking
- RW Cache Market Advisor
- Armory Auditor

### Faction Admin
- announcement publishing/management
- training/guide publishing
- chat moderation / report handling
- other faction configuration that is genuinely leadership-only

Retire the vague overloaded **Operations** bucket after features are reassigned to these clearer homes.

## More

Purpose: app-level controls, not faction gameplay.

Suggested destinations:
- Settings
- Premium
- Feedback & Requests
- Legal & Privacy
- About
- Developer Console only when explicitly authorized / Development build as appropriate

No war/faction operational tools here.

## Page-density rules

- Prefer 3–6 primary cards per page.
- If a page needs more than ~6 persistent actions, split it into named subpages.
- One feature should have one normal navigation home; duplicates are allowed only when context makes the action immediately relevant (example: Request Revive on Home and War only while hospitalized).
- Avoid giant permanent buttons for rare actions.
- Use tap-on-item / overflow menus for secondary actions such as report/block.
- Avoid showing leadership cards on member pages simply because the current user has leadership permissions.
- Advanced and rarely used functions belong one level deeper.

## Contextual UI rule

Features that matter only in a specific state should not permanently consume screen space.

Examples:
- Revive request: appears only when hospitalized/revivable.
- Chain live card: appears only while applicable.
- War shortcuts: become prominent during active war.
- Moderation badge: leadership only, and emphasized only when reports are pending.
- Premium admin controls: only inside authorized admin/developer area.

## Full-app audit process

Before the next major feature release:

1. Inventory every Activity/screen and every visible navigation entry.
2. Assign each feature exactly one primary home from the map above.
3. Identify duplicate entry points and remove unnecessary copies.
4. Identify member vs leadership vs developer permissions for every screen.
5. Identify state-specific features that should become contextual rather than permanent.
6. Review every page for card count, duplicated headers, oversized actions and confusing labels.
7. Rename broad buckets (especially Operations) into task-based categories.
8. Verify back behavior and breadcrumb/subpage titles are predictable.
9. Device-test the full navigation as ordinary member, leader/co-leader and authorized developer.
10. Only after the navigation shell is accepted should new features such as Spy Intel, Cache Market Advisor and Revive Providers be placed into it.

## Immediate recommendation

Treat the next Development cycle primarily as an information-architecture cleanup rather than simply adding more cards. Establish the new homes first, then add new features into the correct subpages.