# Duck Force Companion — Multi-Faction Architecture

## v0.6 position

v0.6 keeps the production sign-in gate restricted to Duck Force, but all new faction-intelligence modules are tenant-scoped by the authenticated Torn faction ID and faction name. This lets the app move to many factions later without rebuilding those feature screens.

## Non-negotiable tenant rules

1. **Faction ID is the tenant key.** Every shared record, cache, permission map, notice, activity snapshot and future configuration row must include `faction_id`.
2. **Never trust a client-selected faction.** The active tenant is derived from `/user/faction` using the authenticated Torn key.
3. **No cross-faction reads.** Backend lookups must always filter by the authenticated faction ID before returning shared data.
4. **Permissions are faction-local.** Rank names and Torn abilities are cached separately for each faction ID. Identical rank names in different factions are unrelated.
5. **API keys are verification credentials, not stored tenant data.** Keys stay encrypted on-device; backend requests may use a key to verify the caller but must not persist it.
6. **Feature configuration is tenant-scoped.** Future feature flags, branding and defaults belong to the faction ID rather than global constants.
7. **Developer controls are device-local.** Owner test switches never change another member's app or another faction's tenant.

## Current v0.6 tenant-safe modules

- Faction Activity Tracker — current authenticated faction only.
- War Participation — current authenticated faction ID used to select the correct side of war reports.
- Chain Command Center — current authenticated faction data only.
- OC Readiness — current authenticated faction data only.
- War/notice architecture already carries faction ID.
- Shared backend schemas introduced earlier already carry faction ID for positions/notices and should retain that rule as they expand.

## Multi-faction release gate

The switch from single-faction to many-faction distribution should happen only after all of the following are true:

- Production authentication no longer contains a Duck Force-only rejection.
- Backend has a tenant registry keyed by numeric faction ID.
- New factions have an explicit onboarding/enrollment path.
- Leader/Co-leader permission synchronization is isolated per faction.
- Every backend action verifies the caller's current faction before reading/writing tenant data.
- Branding can fall back to generic Companion branding when no custom tenant branding exists.
- Faction-specific private tools can be disabled per tenant.
- Cross-faction isolation tests pass with at least two real/test faction identities.
- Stable signing is in place so production versions update the same Android app instead of installing side-by-side packages.

## Developer preview

The v0.6 Developer Console exposes a `Multi-faction architecture preview` marker. It does **not** open the production login gate. It exists so faction-scoped modules and diagnostics can be tested with the future tenant model while the public app remains single-faction.

## Future backend shape

Recommended tenant-level tables/collections:

- `Tenants`: faction ID, display name, enabled state, feature configuration, onboarding metadata.
- `FactionPositions`: faction ID + position name + Torn abilities.
- `Notices`: faction ID + notice data.
- `FeatureConfig`: faction ID + feature key + value.
- `ActivityCache`: faction ID + time window + generated snapshot metadata.
- Other shared modules: always faction ID first, then module-specific keys.

No shared query should be valid without a resolved authenticated faction ID.
