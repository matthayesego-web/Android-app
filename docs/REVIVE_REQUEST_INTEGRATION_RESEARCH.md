# TornFCA Revive Request Integration Research

Status: researched candidate for a future Development build. Not implemented yet.

## Confirmed feasibility
- Torn PDA currently integrates multiple third-party revive providers rather than attempting to perform a revive through the Torn API.
- Its provider list includes Nuke, UHC, WTF, Midnight X, The Wolverines, Combat Ready and Asclepius.
- Nuke request flow first verifies the user is hospitalized via Torn data, then posts player ID/name/faction/location/app source to Nuke's revive-request API.
- Public Torn PDA source currently posts Nuke requests to `https://nuke.family/api/revive-request` and treats HTTP 201 as success.
- Public Torn PDA source currently posts UHC requests to `https://tornuhc.eu/api/request` with player/faction/source details and treats HTTP 200 as success.
- These provider calls are external revive-service APIs, not Torn API write endpoints.

## TornFCA design target
- Add one normal **Request Revive** action in an appropriate personal/war-readiness location; do not put it on every screen.
- Before sending, refresh the signed-in player's Torn status and only allow a normal self-revive request when the player is actually hospitalized.
- Present provider, current advertised price/contract note, and explicit confirmation before the request is sent.
- Identify requests honestly as TornFCA in any provider `source` / `appInfo` field.
- Never send the user's Torn API key, password, Firebase token or unrelated account data to a revive provider.
- Provider adapter interface should allow several services without hard-coding the UI to one faction.
- If a provider supports overseas requests, include only the minimum location field that provider requires.
- Add cooldown/debounce protection so repeated taps cannot spam provider request endpoints.
- Show provider response/error cleanly and offer the provider's forum/Discord fallback when appropriate.
- Payment remains between the player and reviver/provider. TornFCA must show the current provider terms before submitting and must not imply the revive is free.

## Suggested UX
1. User taps **Request Revive**.
2. TornFCA refreshes current player status.
3. If not hospitalized, explain that no request is needed/allowed.
4. If hospitalized, show available enabled providers with price/contract note and service identity.
5. User selects provider and confirms **Request revive**.
6. TornFCA sends one request containing only the provider-required player/faction/location/source fields.
7. Show **Revive requested** or the provider's returned failure reason.

## Provider / policy considerations before release
- Public source proves the endpoints are used by Torn PDA, but TornFCA should still confirm current provider terms/compatibility before shipping a production integration and should be prepared for endpoint/schema changes.
- Provider prices should be remotely configurable or refreshed rather than permanently baked into the APK.
- Keep an emergency fallback that opens the provider's current Discord/forum link if their API is down.
- Do not automatically request a revive in the background; the request should come from an explicit user action.
- Do not repeatedly poll provider systems after requesting unless they publish a supported status API.

## Relevant Torn API capabilities
- Current Torn API exposes user profile/basic/travel data and user/faction revive logs, which can support status checks and later confirmation/history, but it does not expose a public write endpoint to request a third-party revive.

## Evidence reviewed
- Torn PDA `lib/models/profile/revive_services/revive_provider.dart` provider registry.
- Torn PDA `lib/widgets/revive/nuke_revive_button.dart` hospitalized-state check and Nuke request UX.
- Torn PDA `lib/utils/external/nuke_revive.dart` public Nuke POST integration.
- Torn PDA `lib/utils/external/uhc_revive.dart` public UHC POST integration.
- Current Torn forums for Nuke/NITE/UHC revive bots/scripts and on-demand revive services.

## Next step
Prototype a provider-neutral `ReviveRequestActivity` / adapter on a `work/*` branch only after the next Development scope is approved. Start with one or two providers whose current request schemas and commercial terms can be verified, then expand rather than shipping seven fragile integrations at once.
