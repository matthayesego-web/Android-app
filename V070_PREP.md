# v0.7.0 private prep

Branch: `v0.7-dashboard-premium-prep`

## Prepared changes
- Compact two-column Quick Access dashboard.
- Existing faction tools retained: Activity, War, Chain.
- OC moved to dedicated three-part tracker: Open / Planning / Complete.
- New Faction Pulse quick snapshot.
- New Member Quick Lookup.
- Premium Preview section with Smart Alerts + Advanced Intel concepts.
- Backend-safe premium entitlement cache keyed to numeric Torn player ID.
- Developer premium simulation + expanded feature switches/launchers.
- Build and signing workflows read `versionName` automatically instead of hard-coding a release number.
- Production package remains `com.matthayesego.duckforcetoolkit`.
- Planned version bump: `0.7.0`, versionCode `13`.

## Do not merge yet
This branch was prepared while the repository was private to avoid Actions usage. It has **not** been compiled or emulator-tested yet.

## Public-build checklist for next session
1. Make repository public temporarily.
2. Enable/trigger branch compilation.
3. Fix any compiler errors before touching `main`.
4. Run visual smoke test on the internal package.
5. Test OC Open / Planning / Complete with a real faction key.
6. Test Faction Pulse and Member Lookup.
7. Confirm premium preview is locked/non-commercial.
8. Fast-forward/merge only after green checks.
9. Sign release with the permanent signing key.
10. Return repository to private when finished.
