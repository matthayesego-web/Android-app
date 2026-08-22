# TornFCA — Nuke Revive Integration Partner Handoff

Status: planning / partner approval required before implementation.

## What TornFCA needs from Nuke

Nuke's current public developer documentation describes a server-to-server Revive Request API at `POST https://nuke.family/api/revive-request`.

Before production integration, obtain explicit approval for TornFCA and ask Nuke to provide/confirm:

1. A dedicated TornFCA `X-Revive-Key` for server-to-server requests.
2. Any required `app_info` prefix, preferably `TornFCA`.
3. Confirmation that TornFCA may use the documented revive-request endpoint from its backend.
4. Any TornFCA-specific rate-limit expectations or abuse-handling requirements.
5. Confirmation of the current required/recommended request fields and response behavior.
6. Confirmation that `#test` requests are available for validation and who should confirm test-channel delivery.
7. A support/developer contact for API changes, key rotation, outages, or integration questions.
8. Whether TornFCA may display the Nuke name/logo and current revive-service terms in-app.
9. How Nuke wants pricing/contract information represented. The revive API itself does not imply a price.
10. Whether there are special overseas/travel rules TornFCA should display before submission.

## Security architecture

- The Nuke key MUST live server-side only. Never embed it in the Android APK, client Java/Kotlin, Firebase Remote Config, or a public repository.
- Android calls a TornFCA backend revive-request action.
- TornFCA backend re-verifies the requesting Torn identity/status using the user's API key for the request only; the key is not persisted.
- Backend should confirm the player is hospitalized/revivable before forwarding the request.
- Backend forwards only the minimum Nuke-supported fields.
- Suggested fields: `torn_player_id`, `app_info`, `torn_player_country` when known, `requested_by`, optional fallback `torn_player_name`, optional `faction_id`.
- Never send the user's Torn API key, login credentials, FCM token, device identifiers, or unrelated faction data to Nuke.
- Add request throttling/debouncing in TornFCA so repeated taps cannot spam Nuke.

## Current documented response handling

- `201` = accepted/logged, not a guaranteed completed revive.
- `401` = missing/invalid/deactivated integration key.
- `422` = validation error.
- `429` = rate limit; honor `Retry-After`.
- `403` = server/IP banned; contact Nuke.
- Nuke currently documents no completion callback/webhook, so TornFCA must not claim the revive occurred merely because the request was accepted.

## Test plan

- Use the Nuke-issued TornFCA key.
- Use `TornFCA #test` (or the exact required prefix + `#test`) for initial requests.
- Verify accepted test request with Nuke contact before enabling live submissions.
- Then device-test one hospitalized self-request in Development.
- Verify duplicate-tap protection and all documented error states.

## UX target

- Contextual **Request Revive** card appears when the user is hospitalized.
- Refresh Torn status immediately before submit.
- Show provider name and service/payment notice before confirmation.
- One explicit user press submits the request.
- Show `Request accepted` rather than `Revive complete` after HTTP 201.
- Offer Nuke Discord/forum fallback if the request service is unavailable.

## Partner message summary

TornFCA should ask Nuke for a dedicated integration key, permission to use their documented server-to-server revive-request API, the required app-info prefix, testing contact/process, branding/service-term guidance, and a developer contact for future changes.