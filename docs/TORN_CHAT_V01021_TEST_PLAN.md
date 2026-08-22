# TornFCA v0.10.21 Real Torn Chat Test Plan

Purpose: validate the foreground-only Torn-backed faction chat experiment on a real Android device before any production promotion.

## Expected path
1. Install `TornFCA Development` beside the Play Store app. Development launcher must show **D**; production launcher remains **T**.
2. Open TornFCA Development -> Chat -> **Torn Chat**.
3. First use may show Torn's normal login page. Sign in directly to Torn inside the WebView. TornFCA does not receive or bridge the password.
4. Open Torn's faction chat if it is not already open.
5. TornFCA should automatically expand the actual Torn faction chat to the full WebView. The status line should read `LIVE`.
6. Read messages and send a test message using Torn's actual textarea/send control.
7. Confirm the same sent message appears in normal Torn chat outside TornFCA.
8. Background TornFCA Development, then return. The activity intentionally blanks the Torn WebView while backgrounded and reloads the Torn page on return.

## Pass criteria
- No Sendbird privileged credential is stored or requested.
- Torn login works and cookies persist across normal activity recreation/app reopen.
- The faction chat is identified from Torn DOM (`div[id^="faction-"]` first, defensive fallbacks second).
- Real incoming messages are visible while the Torn page is foreground-active.
- Explicit user sends go through Torn's own UI and appear to other Torn clients.
- No hidden/background Torn page or WebSocket remains alive after the activity leaves foreground.
- Back returns to native TornFCA chat, which remains fully functional as fallback.
- External non-Torn links leave the WebView for the system browser.

## Failure notes to capture
- Screenshot of Torn page/chat state.
- Whether status says LOGIN / OPENING / OPEN_FACTION_CHAT / LIVE.
- Whether Torn chat was collapsed or open before Focus.
- Whether message reading worked but sending failed, or vice versa.
- Device/Android version.

## Current DOM evidence
Recent public scripts (reviewed Aug. 2026) still use:
- `div[id^="faction-"]` to locate faction chat.
- `.tt-chat-autocomplete` for a Torn chat input inside faction chat.
- `.iconWrapper___tyRRU` as one current send-control class, but TornFCA v0.10.21 deliberately does not depend on that exact hashed send class because the user sends through Torn's visible UI.
- Older/maximize scripts identify `#chatRoot`, `div[class^=chat-box_]`, `div[class^=chat-box-content_]`, `div[class^=viewport_]` and textarea elements. v0.10.21 uses layered contains/fallback selectors rather than requiring one historical class hash.
