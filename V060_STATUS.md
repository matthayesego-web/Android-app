# Duck Force Companion v0.6.0 — Implementation Status

## Implemented on `v0.6-activity-war-platform`

### Faction Activity Tracker
- Default 30-day lookback; developer can switch to 7 or 14 days.
- Paginated faction-news scanning with configurable 5/10/20-page cap per category batch.
- Scans the full current faction-news category set when the authenticated key permits it, including the fund categories as a second batch.
- Exact member-name mention counting across faction logs.
- Full member ranking plus active-member and zero-mention counts.
- Bottom-five leadership summary ordered from lowest activity upward.
- One-tap copy for the bottom five and the complete numbered ranking.

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
- Current faction/tenant ID and real/effective permission state.
- Direct launchers for all four v0.6 modules.
- Per-feature enable/disable switches that control Home visibility.
- Public-only permission simulation.
- 7/14/30-day activity lookback control.
- 5/10/20-page activity API load control enforced by the paginator.
- Concise/verbose read-only Torn endpoint diagnostics.
- Multi-faction architecture preview marker.
- Faction-scope cache age/status display.
- `Force Fresh Faction Verification` control to clear only the short-lived tenant cache.
- Reset controls clear v0.6 developer settings/cache without touching banking data.

### Performance / routing
- v0.6 modules route through an authenticated faction-scope layer instead of hard-coded feature data.
- A five-minute scope cache reduces repeated Torn identity/faction lookups when moving between v0.6 tools.
- The cache stores only verified faction/user metadata plus a one-way SHA-256 fingerprint of the API key; the API key itself remains in the encrypted Android key store.
- Developer Console authentication always fresh-verifies the owner before opening.

### Multi-faction preparation
- New modules are scoped by authenticated numeric faction ID and faction name.
- Production sign-in remains Duck Force-only until the explicit multi-faction release gate.
- Multi-faction isolation rules are documented in `MULTI_FACTION_ARCHITECTURE.md`.
- Future shared tenant/backend records are required to use faction ID as the primary isolation key.

### Update-only release preparation
- v0.6 metadata is versionCode 12 / versionName 0.6.0.
- Production application ID remains `com.matthayesego.duckforcetoolkit`.
- Internal debug builds use `.internal` only for private validation and are not intended for user distribution.
- `release-sign.yml` is prepared to reconstruct the permanent keystore from encrypted GitHub secrets, sign v1/v2/v3, verify the certificate/package, and produce a production update APK with the original application ID.

## Explicitly unchanged
- Banking behavior and banking backend logic.
- Existing Armory Auditor.
- Existing Company Train Calculator.
- Existing war notice/message-board behavior.

## Validation status
- GitHub Actions compile validation passed successfully after the repository was temporarily made public so the standard hosted runner could start.
- Successful workflow run: `31859105090` (rerun attempt on v0.6 head `58ae193dc65448794fc4dfbda5146d1b53b02efd`).
- `:app:assembleDebug` and `:app:assembleRelease` both completed successfully.
- APK naming and artifact upload steps completed successfully.
- Artifact `DuckForce-Companion-v0.6.0-internal-builds` was produced with digest `sha256:403178ee8cc5d1b47dfb4d7a868d613a18383d8b4748912faf9deec4e38931b3`.
- A fresh branch diff confirms no banking source files are modified.
- Temporary feature-branch CI triggering has been removed; normal Android builds return to `main` only.
- v0.6 is validated for promotion to `main`; production distribution still requires the permanent signing workflow to succeed.

## Release/update requirement
- User-facing v0.6 must update the existing production application ID `com.matthayesego.duckforcetoolkit`.
- Internal debug packages must not be distributed as a second user app.
- Stable production signing must be confirmed before distributing v0.6 so Android accepts it as an update to the existing installed app.
