# Duck Force Companion v0.6.0 — Implementation Status

## Implemented on `v0.6-activity-war-platform`

### Faction Activity Tracker
- Default 30-day lookback; developer can switch to 7 or 14 days.
- Paginated faction-news scanning with configurable 5/10/20-page cap per category batch.
- Scans the full faction-news category set when the authenticated key permits it.
- Exact member-name mention counting across faction logs.
- Full member ranking.
- Active-member and zero-mention counts.
- Bottom-five summary for quick leadership follow-up.

### War Participation
- Live ranked-war score.
- Live member ranked-war attack counts when the current key can read faction attacks.
- Public latest-completed ranked-war report fallback.
- Per-member completed-war attack/score ranking.

### Chain Command Center
- Current/max chain.
- Timeout, modifier and cooldown.
- Online member count.
- Available-member count excluding hospital/jail/travel states.
- Territory-wall count.
- Online + available member list.

### OC Readiness
- Recruiting/planning organized-crime list.
- Filled/open slots.
- Difficulty and ready time.
- Missing-item warnings.
- Members not currently assigned to an OC.

### Developer Console
- Owner-only authenticated routing.
- Current faction/tenant ID and permission state.
- Direct launcher for all four v0.6 modules.
- Per-feature enable/disable switches.
- Public-only permission simulation.
- 7/14/30-day activity lookback control.
- 5/10/20-page activity API load control.
- Concise/verbose read-only Torn endpoint diagnostics.
- Multi-faction architecture preview marker.
- Reset controls that do not touch banking data.

### Multi-faction preparation
- New modules resolve the authenticated Torn user/faction before launch.
- New modules are scoped by numeric faction ID and faction name.
- Production sign-in remains Duck Force-only until the explicit multi-faction release gate.
- Multi-faction isolation rules are documented in `MULTI_FACTION_ARCHITECTURE.md`.

## Explicitly unchanged
- Banking behavior and banking backend logic.
- Existing Armory Auditor.
- Existing Company Train Calculator.
- Existing war notice/message-board behavior.

## Validation status
- Feature branch remains separate from `main`.
- GitHub Actions compile validation is currently blocked before runner startup by the repository account's Actions billing/spending-limit status.
- The Actions failure is infrastructure-related; no v0.6 compiler result has been produced yet.
- Do not merge v0.6 to `main` until a complete Android build passes.

## Release/update requirement
- User-facing v0.6 must update the existing production application ID `com.matthayesego.duckforcetoolkit`.
- Internal debug builds may use an internal suffix but must not be distributed as a second user app.
- Stable production signing must be confirmed before distributing v0.6 so Android accepts it as an update to the existing installed app.
