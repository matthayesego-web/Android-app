# TornFCA v0.9.21 — Member-First Overhaul

## Purpose
The beta-preparation priority is expanding what a regular faction member receives from TornFCA while preserving leadership/admin depth and keeping the Premium boundary healthy.

## Free member core added / targeted
- Searchable faction roster/directory with current member-safe status.
- Training Center available to every authenticated faction member.
- Universal TornFCA starter guides that remain available regardless of faction.
- Faction-scoped training expectations: leadership can publish plain-language stat-gain and Xanax-use targets plus notes/exceptions.
- Faction-scoped custom training guide library.
- Training content follows `faction_id`, not the player account: leaving a faction removes access to that faction's private library; joining a new faction exposes only the new faction's library.

## Training library security model
The existing Community Backend is the source of truth for faction-local content.
- Every request supplies the user's Torn API key only for live identity verification.
- The backend derives the current `player_id`, `faction_id`, faction name and position from Torn.
- Reads are filtered exclusively to the verified current `faction_id`.
- Training-rule and guide writes are initially restricted server-side to Leader/Co-leader.
- The Android UI also hides management controls from ordinary members, but UI hiding is never the authorization boundary.
- Guide records contain faction ID, title/category/body, author identity, update time and active/archive state.
- The backend does not store Torn API keys.

## Training product boundary
### Free
- Built-in training education.
- Current faction rules/expectations.
- Current faction custom guide library.
- Ordinary member access to guidance needed to participate and progress.

### Premium Player
- Personal longer-term progress history and trends.
- Advanced personal reminders and configurable training notifications.
- Personal analytics around consistency and historical activity.

### Faction Pro
- Automated compliance/exception monitoring against faction training rules.
- Long-term member training trends.
- Requirement violation summaries and leadership follow-up queues.
- Advanced shared analytics/reporting around faction progression.

The basic rules and guide library must not be paywalled.

## Built-in guide policy
Built-in game-mechanic values should be conservative, sourced from current official/authoritative Torn documentation where practical, and written so faction-specific guidance can override generic advice. Avoid hard-coding a universal daily Xanax requirement; factions set their own expectation.

## Release/legal note
Before public Google Play production release, TornFCA must add:
- EULA.
- Terms & Conditions / Terms of Service.
- Links from About.
- Appropriate login/onboarding acknowledgement/acceptance flow.
- Final Privacy Policy and Google Play Data safety disclosures.

Legal work is a production gate but should not stall current closed-beta member feature development.

## Next member-first candidates
- Better personal progression snapshot using only the signed-in player's data.
- Faction notices/resources library using the same tenant-scoped backend pattern.
- Member onboarding checklist customized by faction.
- Optional faction mentorship/contact directory.
- Clearer in-app explanation of Free vs Premium Player vs Faction Pro without degrading the free experience.
