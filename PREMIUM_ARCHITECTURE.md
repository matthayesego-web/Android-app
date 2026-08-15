# Premium entitlement architecture (prep)

Status: **framework only — no payments accepted and no production unlocks yet.**

## Goal
Allow selected features to unlock automatically after a verified Torn in-game payment/item transfer, while keeping the app fast and preventing client-side spoofing.

## Trust model
- Entitlements are keyed by **numeric Torn player ID**, never player name.
- The Android client never unlocks premium merely because it sees or claims a payment.
- A backend verifies the receipt/payment against the designated seller profile and records a durable entitlement.
- The app fetches the verified entitlement after normal Torn authentication and caches the result locally for fast UI decisions.
- A receipt/transaction identifier must be single-use to prevent replay.

## Proposed entitlement record
- `player_id`
- `faction_id` (optional scope; useful once multi-faction launches)
- `product_id`
- `tier` (`FREE`, `PREMIUM`)
- `granted_at`
- `expires_at` (`0` for lifetime)
- `source_type` (item / cash / manual grant / promo)
- `source_receipt_id`
- `verified_at`
- `revoked_at`

## Proposed premium features
1. Smart Android alerts: war start, chain danger, OC ready, participation reminders.
2. Advanced member intel: longer participation history, trends, comparisons.
3. War analytics: zero-hit watch, readiness, richer completed-war analysis.
4. History and export: longer retention and copy/export reports.
5. Optional leadership automation: premium alerts and exception lists without replacing Torn permissions.

## Developer testing
`DeveloperSettings.simulatePremium()` exists only for owner testing. It must never be treated as a real payment or backend entitlement.

## Release gate before payments go live
- Decide accepted Torn item/payment products and prices.
- Implement server-side receipt watcher/verifier.
- Add replay protection and manual revoke/grant tools.
- Add app entitlement refresh on login + periodic refresh.
- Confirm Terms / Torn API usage and rate limits before enabling automatic receipt polling.
