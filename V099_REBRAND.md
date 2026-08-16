# TornFCA v0.9.9 — Rebrand Architecture

## Public product
**TornFCA** = Torn Faction Companion App.

TornFCA is a multi-faction operating layer. It is not a Duck Force-specific product and is not intended to replace Torn or Torn PDA.

## Branding model
- TornFCA owns the neutral premium platform identity, launcher icon, access screen and base dark UI.
- The authenticated faction owns the in-app personality: faction name, player role and a restrained faction accent.
- The current theme engine derives a stable premium accent from `faction_id` + faction name.
- `FactionTheme.saveFactionAccent()` is the future override boundary for shared faction-admin theme configuration.
- Status semantics stay consistent across factions: green = healthy/ready, red = urgent/problem, blue = information. Faction accent is used for brand/navigation/priority emphasis.

## Architecture
- `TornFcaActivity` is the public shell over the proven v0.9 feature layer.
- `TornFcaScreens` wraps existing feature activities so rebranding/theming is applied without rewriting working Banking, Armory, War, OC, FFScouter or developer logic.
- `TornFcaBrand` handles brand cleanup, legacy activity routing and faction accent application.
- `FactionTheme` is faction-scoped and ready for a future shared backend override keyed by `faction_id`.

## Release identity
v0.9.9 keeps the current sideload `applicationId` so it updates the existing tester installation instead of creating another duplicate app. A final Play Store application ID must be chosen once before the first Google Play upload; after that it must remain stable.

## Next before public closed beta
1. Finalize War Payout weighting/penalties and payout handoff.
2. Implement premium entitlement/backend boundaries.
3. Finalize Play Store application ID, AAB signing/upload flow, privacy/data-safety materials and store assets.
4. Run multi-faction testing to validate theme contrast and permission boundaries.
