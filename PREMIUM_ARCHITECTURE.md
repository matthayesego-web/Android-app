# TornFCA Premium entitlement architecture

Status: **source implementation complete; production deployment/smoke test pending.**

## Goal
Provide a single player-level Premium convenience tier while keeping TornFCA Free fully useful and keeping real Torn faction permissions independent from payment state.

## Trust model
- Entitlements are keyed by numeric Torn `player_id`, never player name.
- Android never grants Premium because it sees or claims a payment.
- The Premium backend verifies payment activity and maintains durable entitlement state.
- Entitlement status requests require the signed-in Torn API key and can only read the verified caller's own entitlement.
- Android caches only backend-verified entitlement for responsive/offline display.
- Owner admin changes require both verified owner Torn identity and the Premium admin password.
- Payment/log identifiers are deduplicated server-side.

## Client layers

### `PremiumEntitlementStore`
Backend-verified cache only. It does not know about developer simulation.

### `PremiumAccess`
The only user-facing Premium decision layer. It applies, in order:
1. remote Premium disable,
2. maintenance mode,
3. minimum-version block,
4. verified player identity,
5. owner-only developer simulation, or
6. backend-verified cached entitlement.

Developer simulation therefore cannot mutate or masquerade as production entitlement.

## v1 Premium features
- `PERSONAL_INSIGHTS`
- `ADVANCED_ALERTS`
- `EXTENDED_ACTIVITY`
- `FACTION_PULSE`
- `MEMBER_DOSSIER`
- future `PERSONALIZATION`

See `docs/PREMIUM_MATRIX_0.10.1.md` for the authoritative Free/Premium classification.

## Provider boundary
FFScouter and TornStats remain separate optional providers with separate consent/entitlement rules. Basic provider data is not a TornFCA Premium entitlement. TornFCA Premium can add convenience and aggregation around provider data without double-charging for access to the provider itself.

## Developer testing
`DeveloperSettings.simulatePremium()` defaults OFF. `PremiumAccess` accepts simulation only when `playerId == BuildConfig.DEVELOPER_PLAYER_ID`. Public users cannot self-grant Premium by changing ordinary app state.

## Backend source
`backend/TornFcaPremiumBackend.gs` is the separate Premium service. Current source defaults to a configurable Torn in-game payment model of 15 Premium days per Xanax with stacking enabled. Production monetization is not live until this service is deployed/redeployed and the signed candidate is built with its HTTPS deployment URL.

## Release gate
- Deploy Premium backend and configure URL.
- Configure owner payment-scanner key and admin password securely in Apps Script properties.
- Install/test payment scan trigger.
- Verify status cannot query another player's entitlement.
- Verify payment dedupe and expiration.
- Verify admin config/grant requires owner identity + password.
- Run `.github/workflows/tornfca-premium-matrix-audit.yml`.
- Device-test Free member, Free leader, Premium member, Premium leader, expired Premium, remote disable and owner simulation.
- Confirm all essential leadership operations remain permission-gated rather than Premium-gated.
