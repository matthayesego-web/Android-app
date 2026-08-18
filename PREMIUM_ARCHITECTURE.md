# TornFCA Premium entitlement architecture

Status: **source implementation complete; production deployment/smoke test pending; automatic monetization intentionally disabled by default.**

## Goal
Provide a single player-level Premium convenience tier while keeping TornFCA Free fully useful and keeping real Torn faction permissions independent from payment state.

## Trust model
- Entitlements are keyed by numeric Torn `player_id`, never player name.
- Android never grants Premium because it sees or claims a payment.
- Entitlement status requires the signed-in Torn API key and only reads the verified caller's own entitlement.
- Android caches only backend-verified entitlement for responsive/offline display.
- Owner admin changes require both verified owner Torn identity and the Premium admin password.
- Payment/log identifiers are deduplicated and payment processing is ScriptLock-protected.
- Automatic payment processing remains fail-closed until an explicit monetization approval switch is enabled.

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
FFScouter and TornStats remain separate optional providers with separate consent/entitlement rules. Basic provider data is not a TornFCA Premium entitlement. Premium can add convenience/aggregation around provider data without double-charging for access to the provider itself.

## Developer testing
`DeveloperSettings.simulatePremium()` defaults OFF. `PremiumAccess` accepts simulation only when `playerId == BuildConfig.DEVELOPER_PLAYER_ID`.

The deployed backend also supports owner-only manual grants. These are the preferred way to test real backend-verified Premium behavior before monetization is enabled.

## Backend source — v1.2.0
`backend/TornFcaPremiumBackend.gs` is the separate Premium service.

Current configurable defaults:
- 15 Premium days per Xanax
- required message `TORNFCA`
- stacking enabled

These values describe the prepared payment model, not permission to start charging users.

### Monetization fail-closed switch

`setupTornFcaPremiumBackend()` initializes `MONETIZATION_APPROVED=false` when missing.

While false:
- Premium status/manual owner grants work for testing.
- `installPremiumScanTrigger()` refuses to install the automatic scanner.
- `scanPremiumPayments()` refuses to process payments.

The flag should only be changed to true after the operator has handled Torn's applicable charging approval requirement and selected a distribution/payment path that is permitted for the release channel.

After that separate release gate is satisfied:
1. set `MONETIZATION_APPROVED=true`,
2. configure the server-only minimum-access `OWNER_API_KEY`,
3. install exactly one scan trigger,
4. run replay/stacking/expiry/payment smoke tests before accepting production payments.

## Receipt safety
- Scanner runs under ScriptLock.
- Torn log IDs are deduplicated in the payment table.
- Entitlement source includes `XANAX_LOG_4103:<logId>`.
- If a run extends the entitlement but fails before recording the payment row, retry sees the same receipt source and does not extend again.
- Stacking=true extends current unexpired time; stacking=false starts the new grant from now.

## Release gate
- Deploy Premium backend v1.2.0 and configure its URL.
- Keep `MONETIZATION_APPROVED=false` during ordinary v0.10.1 testing.
- Set the admin password through the one-time bootstrap property; confirm plaintext deletion.
- Verify status cannot query another player's entitlement.
- Verify owner manual grant, expiration and remote disable.
- Verify payment scanner refuses while monetization is unapproved.
- Run `.github/workflows/tornfca-premium-matrix-audit.yml`.
- Device-test Free member, Free leader, Premium member, Premium leader, expired Premium, remote disable and owner simulation.
- Confirm all essential leadership operations remain permission-gated rather than Premium-gated.
- Treat real automatic payment activation as a separate approval/release gate, not a prerequisite for testing the 1.0 app architecture.
