# TornFCA access-control architecture

Status: **v0.10.1 pre-1.0 architecture**.

## Identity and tenant authority

Torn is the identity/faction authority. TornFCA does not embed a shared faction master key.

The signed-in player's own Torn API key is used to resolve:

- numeric player ID/name,
- current faction ID/name,
- current faction position,
- faction position abilities when the key can read them.

Shared TornFCA backends are multi-faction. Shared rows are scoped by the verified numeric `faction_id`; faction names are display metadata, not the tenant key.

`backend/AccessBackend.gs` re-reads the current Torn faction membership/position on every authenticated backend request. Only stable basic player identity may be cached briefly using a SHA-256 API-key fingerprint. API keys are never persisted by the backend.

## Authorization rules

Two independent questions are always kept separate:

1. **Does Torn authorize this faction action?**
2. **Does TornFCA Premium unlock an optional convenience feature?**

Premium never creates faction authority.

Examples:

- Notice publishing: current `Announcement Changes` ability or Leader/Co-leader.
- Banking queue management/reconciliation: current `Money Giving` or `Balance Adjustment` ability, or Leader/Co-leader.
- WarPay cloud receipts: current Leader/Co-leader, matching the current Android WarPay boundary.
- Rank/user access-matrix editing: current Leader/Co-leader.
- Community moderation: separate configurable capability policy; owner recovery access is always retained. No faction-specific rank names are hardcoded.

## Shared rank/user overrides

`backend/AccessBackend.gs` keeps optional faction-scoped app access overrides:

- `RankAccess`: `faction_id + rank_name + tool_id`
- `UserOverrides`: `faction_id + user_id + tool_id`

These rows cannot leak between factions. Existing legacy single-faction sheets are migrated by `setupTornFcaFactionBackend()` by adding the tenant column.

## Premium boundary

The authoritative Free/Premium classification is `docs/PREMIUM_MATRIX_0.10.1.md`.

Essential faction participation, safety and ordinary administration remain Free. Premium is the convenience/depth layer and does not override Torn permissions.

## Developer authority

Developer/operator controls are separate from faction authority:

- verified TornFCA developer player ID: `BuildConfig.DEVELOPER_PLAYER_ID`
- local developer password gate
- remote Developer Control Plane owner verification
- remote mutating actions also require the developer-admin password hash

Developer Premium simulation defaults OFF and is accepted only for the verified developer player ID through `PremiumAccess`. It is never written into the production entitlement cache.

## Deployment requirement

Source code alone is not live authorization. The five Apps Script backends must be deployed/redeployed and the signed candidate must be built with their HTTPS `/exec` URLs before v1.0 can be considered production-ready.

See `docs/V1_BACKEND_GO_LIVE.md` for the deployment/smoke-test sequence.
