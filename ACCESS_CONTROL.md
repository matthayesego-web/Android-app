# Duck Force Toolkit access-control architecture

## Status

v0.2.0 introduces the authentication and Torn-permission foundation. Shared rank-by-rank app overrides are scaffolded in `backend/AccessBackend.gs` and require a one-time Google Apps Script deployment before they can be edited globally from the Android app.

## Torn is the identity authority

On sign-in, the Android app uses the member's own Torn API key to read:

- `/key/info` — validates the key owner and whether the key has faction API access.
- `/user/basic` — player ID/name.
- `/user/faction` — faction ID/name and faction position.
- `/faction/positions` — when the user's key has Faction API Access, returns all faction positions and each position's `abilities[]`.

The Android app never embeds a shared faction master key.

## Permission colors

Torn's official faction position UI groups abilities into four bands. The API does not return the color itself, so the app derives the color from the returned ability names:

- **Green / Member**: armory usage/loaning/retrieving, refills, Faction API Access, etc.
- **Orange / Elevated**: Organised Crimes, Item/Money/Points Giving, Forum Management, Application Management.
- **Red / Leadership**: Kick Members, Balance Adjustment, War Management, Upgrade Management.
- **Black / Leadership**: Newsletter Sending, Announcement Changes, Description Changes.

Duck Force Toolkit policy:

- Green = member tools.
- Orange = elevated tools.
- Red = global tool access.
- Black = global tool access.
- Torn position `Leader` or `Co-leader` = global tool access plus permission to manage the app's shared access matrix.

Only actual Leader/Co-leader positions may change Duck Force Toolkit access rules. A custom position with Red/Black abilities can see all tools, but cannot edit the access matrix.

## v0.2.0 default tool gates

- Train Payment Calculator: Green+
- Xanax Armory Log: Orange+
- Faction Xanax Auditor: Orange+

These defaults are deliberately data-driven and are intended to be replaced/overridden by the shared rank access matrix.

## Shared override backend

Rank access cannot be stored only on one phone because changes must apply to every member. `backend/AccessBackend.gs` provides a zero-cost Google Sheets / Apps Script backend.

It stores:

- `RankAccess`: per Torn rank, per app/module allow/deny.
- `UserOverrides`: optional player-specific allow/deny.
- `Settings`: Duck Force faction ID/name and schema version.

For every write, the backend re-checks the requesting API key against Torn and accepts the change only when the current faction position is Leader or Co-leader. API keys are not written to the sheet.

## Duck Force numeric faction ID

The source currently enforces the exact faction name `Duck Force` and has `DUCK_FORCE_FACTION_ID = 0` as a temporary placeholder. After the first authenticated Duck Force login, the home screen displays the numeric faction ID. Replace `0` with that ID in both Android/backend configuration for a permanent ID lock.

## Next implementation step

After deploying `backend/AccessBackend.gs`, add the Apps Script web-app URL to the Android app and turn the Leader/Co-leader Rank Access Control screen from read-only rank inspection into editable per-rank module toggles.
