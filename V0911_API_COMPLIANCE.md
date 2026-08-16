# TornFCA v0.9.11 — API Safety & Compliance Review

## Key requirement shown before login
- Normal TornFCA access: **Limited Access or higher**.
- Full Access is **not** required.
- A Custom key may be accepted when it contains every selection TornFCA actually needs.
- Leadership-only faction data additionally depends on the user's in-game **Faction API Access** permission.
- Key storage: encrypted locally on the Android device.
- Optional third-party providers must remain explicit opt-in integrations.

## API request policy
Torn documents a user-wide ceiling of 100 API requests/minute across that user's keys. TornFCA deliberately caps its own Torn request boundary at **40 requests/minute maximum** (1 request every 1.5 seconds), leaving headroom for Torn and other tools.

TornFCA v0.9.11 also:
- serializes Torn API requests across the app so parallel screens cannot create bursts;
- caches `/key/info`, profile/faction scope and faction positions;
- uses short caches for live war/chain/member state;
- heavily caches immutable completed ranked-war reports and historical attack pages;
- automatically backs off and retries Torn API error 5 rather than repeatedly hammering the endpoint;
- uses `/key/info` as the Faction API Access source of truth and avoids known-to-fail `/faction/positions` calls for users who do not have that permission;
- never retries invalid/unauthorized requests in a tight loop.

## Scripting-rule review
Current TornFCA design is intended to stay inside Torn's scripting policy:
- automatic Torn data retrieval uses the official Torn API only;
- no background scraping of Torn pages;
- no CAPTCHA bypass;
- no automated in-game clicks/actions;
- WarPay only opens a Torn page after an explicit user tap and Torn still requires the authorized user to confirm the transfer;
- provider integrations are separate services and must remain disclosed/opt-in.

## API Terms disclosure
TornFCA should keep a visible key-use disclosure wherever the user provides their key. Because the key is stored locally, the disclosure states local encrypted storage, purpose, minimum access level and third-party opt-in behavior.

Before Play beta, add a full Privacy / API Data Use page covering:
- Data Storage
- Data Sharing
- Purpose of Use
- Key Storage & Sharing
- Required Key Access Level / selections

## Xanax Premium — DO NOT ENABLE IN PLAY BUILD YET
### Technical design
True automatic entitlement activation must be server-side; relying on the developer's phone is not reliable when the app is closed.

Proposed monitor:
1. Dedicated owner Torn **Custom key** containing only the `user -> log` selection and the smallest appropriate incoming-item log category/type.
2. Server checks the owner's filtered incoming-item logs approximately once per minute.
3. Only Xanax receipts are processed.
4. Deduplicate by Torn log/event ID so the same transfer can never extend Premium twice.
5. Resolve sender `player_id`, quantity and timestamp.
6. Apply the developer-configured quantity -> entitlement-duration rule.
7. Store entitlement centrally by Torn `player_id` with audit record: source log ID, quantity, start, expiry and any manual adjustment.
8. Connected apps read only their own entitlement from the TornFCA backend.

This design costs roughly one narrowly-filtered Torn API request per minute for the shared payment monitor, not one request per TornFCA user.

### Compliance blocker
Do not enable Xanax-for-Premium until both are resolved:
- **Torn:** obtain staff confirmation that charging Torn assets for access to TornFCA Premium is acceptable under current RMT/API rules.
- **Google Play:** a Play-distributed app unlocking paid digital features generally falls under Google Play's Payments policy and must use Play Billing unless an applicable alternative-billing program applies.

For closed beta, Premium/Faction Pro should therefore use developer-granted/test entitlements, not Xanax payments.
