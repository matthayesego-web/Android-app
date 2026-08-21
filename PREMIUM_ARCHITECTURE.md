# TornFCA Premium entitlement architecture

Status: **v0.10.18 source implementation active on the beta-hardening branch; Premium backend v1.3.0 deployment/smoke test pending.**

## Goal
Provide a single player-level Premium convenience tier while keeping TornFCA Free fully useful and keeping real Torn faction permissions independent from payment state.

## Trust model
- Entitlements are keyed by numeric Torn `player_id`, never player name.
- Android never grants Premium because it sees or claims a payment.
- Entitlement status requires the signed-in Torn API key and only reads the verified caller's own entitlement.
- Android caches only backend-verified entitlement for responsive/offline display.
- Owner admin changes require both verified owner Torn identity and the Premium admin password.
- Complimentary Premium uses the same server entitlement and expiry model as paid Premium; the Android client cannot create it locally.
- Payment/log identifiers are deduplicated and payment processing is ScriptLock-protected.
- Remote Premium disable, maintenance and minimum-version policy remain authoritative over entitlement display/use.

## Client layers

### `PremiumEntitlementStore`
Backend-verified cache only. It does not know about developer simulation and never creates Premium entitlement.

### `PremiumAccess`
The only user-facing Premium decision layer. It applies:
1. remote Premium disable,
2. maintenance mode,
3. minimum-version block,
4. verified player identity,
5. short-lived Developer Channel simulation when explicitly enabled for QA, or
6. backend-verified cached entitlement.

Developer simulation is therefore a QA presentation path, not stored entitlement. It requires a current `DeveloperSessionStore` session and defaults OFF.

## v0.10.18 Premium feature gates
- `PERSONAL_INSIGHTS`
- `TRAINING_GOALS`
- `ADVANCED_ALERTS`
- `EXTENDED_ACTIVITY`
- `FACTION_PULSE`
- `MEMBER_DOSSIER`
- future `PERSONALIZATION`

The feature string identifies the product capability being checked; production Premium entitlement remains a single player-level tier rather than a collection of separately purchased feature flags.

See `docs/PREMIUM_MATRIX_0.10.1.md` for the Free/Premium classification and `PREMIUM_PLAN.md` for the current product set.

## Provider boundary
FFScouter and TornStats remain separate optional providers with separate consent/entitlement rules. Basic provider data is not a TornFCA Premium entitlement. Premium can add convenience/aggregation around provider data without double-charging for access to the provider itself.

## Complimentary Premium
The owner-facing Premium Admin screen can grant Premium to a numeric Torn player ID without a Xanax receipt.

End-to-end path:
1. Android requires the signed-in TornFCA owner identity plus the Premium admin password.
2. `PremiumBackendClient.grantComplimentary()` sends `action=admin_grant` and `grant_type=complimentary`.
3. Backend re-verifies owner identity and admin password.
4. Backend extends the player's existing entitlement under source `COMPLIMENTARY_GRANT`.
5. The recipient's normal entitlement refresh sees the same Premium tier/expiry behavior as any other grant.

Complimentary grants stack from the current unexpired entitlement just like owner support/test grants. They never increment `total_xanax` and are distinguishable from receipt sources such as `XANAX_LOG_4103:<logId>`.

## Developer testing
`DeveloperSettings.simulatePremium()` defaults OFF. `PremiumAccess` accepts simulation only while a valid short-lived Developer Channel session exists on that device.

For end-to-end entitlement testing, use backend developer or complimentary grants. Those exercise the real backend-verified entitlement cache without requiring the payment scanner.

## Backend source — v1.3.0
`backend/TornFcaPremiumBackend.gs` is the separate Premium service.

Current launch configuration:
- **7 Premium days per Xanax**
- required message `TORNFCA`
- stacking enabled
- owner-only complimentary grants supported

Existing Premium sheets created before the seven-day launch conversion was finalized do not have their existing settings silently overwritten by setup. Run `applyPremiumSevenDayLaunchPricing()` once or update the value through Premium Admin and verify `days_per_xanax=7` before paid scanning.

### Automatic payment safety

`setupTornFcaPremiumBackend()` initializes `MONETIZATION_APPROVED=false` when missing.

While false:
- Premium status works.
- owner developer/complimentary grants work.
- `installPremiumScanTrigger()` refuses to install the automatic scanner.
- `scanPremiumPayments()` refuses to process payments.

When automatic receipt processing is deliberately enabled for the chosen distribution path:
1. configure the server-only minimum-access `OWNER_API_KEY`,
2. confirm deployed `days_per_xanax=7`,
3. set `MONETIZATION_APPROVED=true`,
4. install exactly one scan trigger,
5. run replay/stacking/expiry/payment smoke tests before production use.

## Receipt safety
- Scanner runs under ScriptLock.
- Torn log IDs are deduplicated in the payment table.
- Entitlement source includes `XANAX_LOG_4103:<logId>`.
- If a run extends the entitlement but fails before recording the payment row, retry sees the same receipt source and does not extend again.
- Stacking=true extends current unexpired time; stacking=false starts the new grant from now.
- Complimentary grants never masquerade as payment receipts.

## Release gate
- Deploy Premium backend v1.3.0 and configure its URL.
- Apply/verify seven-day launch pricing on the deployed PremiumSettings sheet.
- Set the admin password through the one-time bootstrap property; confirm plaintext deletion.
- Verify status cannot query another player's entitlement.
- Verify developer grant, Complimentary Premium, expiration and remote disable.
- Verify a complimentary grant returns source `COMPLIMENTARY_GRANT` and does not change Xanax totals.
- Verify payment scanner fail-closed behavior in any build/config where automatic scanning is disabled.
- Run `.github/workflows/tornfca-premium-matrix-audit.yml` and `.github/workflows/tornfca-v01018-validation.yml`.
- Device-test Free member, Free leader, Premium member, Premium leader, expired Premium, remote disable, developer simulation and Complimentary Premium.
- Confirm all essential leadership operations remain permission-gated rather than Premium-gated.
- Produce the exact signed beta APK/Play AAB using the permanent TornFCA signing identity before release testing.
