# TornFCA v0.9.13 — Pre-Beta API / Key / Privacy Audit

Status target: release-blocking audit for the final private test APK before beta.

## One-key compatibility

Recommended key: one standard 16-character **Limited Access** Torn API key.

- TornFCA: Limited Access is sufficient for every direct Torn endpoint currently used. Full Access is not required.
- FFScouter core scouting: the same Limited Access Torn key is compatible. FFScouter remains a separate opt-in provider and registration is never automatic.
- TornStats: TornStats states that at least Limited Access is required. The same Limited key is therefore the recommended cross-provider key.
- Faction-wide private data: some Torn faction endpoints additionally require the player's in-game **Faction API Access** position permission. This is not a higher API-key access level.

Custom keys may work when they contain every required selection, but preset Limited Access is the supported one-key compatibility target because it is the common denominator across TornFCA, FFScouter and TornStats.

## Torn request safety

Torn documents a 100 requests/minute ceiling per user across all of that user's keys, and warns that repeated invalid-key calls can temporarily block an IP.

TornFCA controls:

- Android direct Torn calls are serialized through `TornApiClient`.
- Minimum device-side spacing is 5 seconds: theoretical maximum 12 Torn requests/minute from the Android client.
- Shared faction-backend actions are client-throttled to 1 request/10 seconds (6/min/device) because the backend may re-verify identity against Torn.
- Identical/stable endpoints are cached; completed ranked-war reports are cached for 24 hours and historical attack windows for 30 minutes.
- Live attack windows use short cache lifetimes so war participation is not frozen for 30 minutes.
- Torn error 5 (rate limit) and 17 (backend error) receive bounded backoff/retry.
- Permanent invalid/access errors are not automatically retried.
- API key format is rejected locally before any Torn request unless it is exactly 16 alphanumeric characters.
- Legacy launcher activities that could bypass the shared native Torn throttle are no longer declared as Android activities.
- Embedded Armory requests are routed through `TornApiClient`; the real key never enters the WebView JavaScript DOM.
- Automatic remote Torn profile/avatar web fetching was removed.

The premium entitlement monitor is separate from player keys and is designed for one Torn log request/minute using its protected owner key.

## FFScouter

Published FFScouter API limits currently used by TornFCA:

- get-stats: 20/min/IP
- check-key: 10/min/IP
- register: 3/min/IP

TornFCA stays below those limits with local spacing and read caching. `get-stats` is batched (up to FFScouter's documented 205-target limit).

FFScouter network access is protected by a key-specific consent gate at the provider client boundary. No check-key, stats or registration request is allowed before explicit opt-in. Registration has an additional acknowledgement step and links to FFScouter's homepage/Data Policy/Terms as required by FFScouter.

## TornStats

TornStats states that at least a Limited Access Torn API key is required and documents a 100 calls/minute API limit.

TornFCA:

- requires explicit key-specific opt-in before TornStats calls,
- links to TornStats Terms and API-key FAQ before enablement,
- locally limits TornStats calls to 12/minute,
- caches roster/spy responses for two minutes,
- stops provider calls when disabled.

TornStats is a separate service and may store the key/data according to its own Terms; TornFCA discloses this before opt-in.

## Key handling

- API key encrypted at rest with AES-GCM using Android Keystore.
- No plaintext API key stored in normal preferences.
- Android backup disabled.
- Cleartext HTTP disabled.
- Only Internet permission requested.
- Non-launcher activities are not exported.
- FFScouter and TornStats consent is bound to the current API key fingerprint; replacing the Torn key requires fresh provider consent.
- TornFCA's faction backend may receive the key transiently over HTTPS for authentication/shared features, but backend code does not persist the key.

## Banking listener

The optional faction-chat banking userscript:

- makes no Torn API calls,
- only scans while Torn is visible/focused, faction chat appears open, and the player has interacted recently,
- submits only messages matching explicit banking request patterns,
- deduplicates matched messages locally and on the backend.

## Release gates

The v0.9.13 APK must not be distributed until:

1. GitHub Actions builds beta and release variants successfully.
2. CI static checks confirm the current launcher/shell, v0.9.13 metadata, consent gates, throttle boundaries, no legacy launchable shells, `allowBackup=false`, and no WebView real-key injection.
3. Release APK is zipaligned and signed with the permanent TornFCA signing key.
4. APK v2/v3 signatures verify.
5. Signing certificate SHA-256 matches the permanent release certificate.
6. OS package remains `com.matthayesego.duckforcetoolkit`, versionCode 28, versionName 0.9.13.

`main` remains untouched until the private test build is explicitly approved.
