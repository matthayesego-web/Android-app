# TornFCA Real Torn Chat Integration Research

Status: active research, August 2026. This document intentionally contains no Torn, Sendbird, Firebase, or third-party production secrets.

## What is confirmed

- Torn Chat 3.0 still uses Sendbird as its realtime backend.
- Torn administrators publicly referenced Sendbird and a user `sessionToken` during Chat 3.0 incidents in October 2025.
- Torn PDA's public source contains a native Sendbird client. Its controller connects with the Torn player ID and a Sendbird session/access token, handles messages, and can send into a faction channel named `faction-<factionId>`.
- Torn PDA's public `.env_example` deliberately leaves its Sendbird app ID / privileged token blank. Those production credentials are not TornFCA credentials and must never be copied or extracted.
- Public Torn userscripts demonstrate a second route: operate on Torn's already-authenticated faction chat DOM while the Torn page is actively being viewed. Current scripts identify faction chat with selectors such as `div[id^="faction-"]`, observe messages with `MutationObserver`, and some user-triggered scripts fill Torn's own chat textarea and activate Torn's own send control.

## Route A — Native Sendbird (preferred if user-scoped auth can be obtained legitimately)

Target architecture:

1. Verify the Torn user and current faction.
2. Obtain a user-scoped Sendbird session token through a Torn-supported/user-session bootstrap path.
3. Connect native Sendbird using the player's Torn ID.
4. Open `faction-<factionId>`.
5. Receive and render messages natively.
6. Send only when the user explicitly presses Send.
7. Register push only through a legitimate user-scoped mechanism.

### Blocker

We have not yet identified a public Torn endpoint that exchanges an authenticated Torn browser/API identity for a Sendbird session token. Torn's own browser necessarily obtains/refreshes a `sessionToken`, and Torn staff have explicitly referenced regenerating it, so tracing the browser bootstrap remains worthwhile.

### Hard boundary

Do not extract, copy, publish, or ship Torn PDA's Sendbird Platform/API token. A privileged Sendbird API token is a server credential, not a player credential. TornFCA must not depend on another application's secret.

## Route B — Foreground Torn WebView / DOM-backed faction chat

This is the strongest no-special-credential fallback discovered so far.

Concept:

- TornFCA opens an authenticated `torn.com` WebView dedicated to faction chat.
- The Torn page itself remains the active/visible source of truth.
- Injected UI code can identify the visible faction chat, enlarge/reflow it, remove irrelevant surrounding chrome, and make chat effectively full-screen.
- Incoming messages are observed from the live chat DOM while that WebView is visible.
- A user pressing Send can write into Torn's actual chat input and trigger Torn's own send control as that one user action.
- We should prefer keeping the enhanced conversation inside the same active Torn WebView rather than exporting scraped WebSocket/page data to a hidden native/background service.

### Why this route is credible

Public Torn userscripts already do these things without owning Sendbird credentials:

- Watch current faction-chat messages using `MutationObserver`.
- Locate the faction chat with `div[id^="faction-"]`.
- Fill Torn's real textarea / autocomplete input.
- Trigger Torn's real send control following a user action.

CSS class names in Torn Chat 3.0 are hashed and change between builds, so production code must avoid relying on a single exact hashed class. Prefer stable IDs, element roles, textarea/input semantics, and layered selector fallbacks.

### Web-login limitation discovered during v0.10.21 device test

- A Torn API key authenticates Torn API requests but does not create a logged-in `torn.com` browser session.
- Google OAuth intentionally blocks embedded Android WebViews, so a Google-authenticated Torn account cannot simply use a Google button inside `RealTornChatActivity`.
- The practical WebView fallback is Torn's own email/password login; a user who normally uses Google can use Torn's Recover account flow to set/reset a Torn password.
- Torn PDA's current public dependencies include native Google sign-in support, but its build documentation says native authentication provider/login implementation files are intentionally excluded from the public repository and replaced by example/stub files for outside builds. This strongly suggests its smooth Google sign-in path depends on a private/native auth integration, not an API-key-to-WebView-session trick we can copy from public source.
- Continue researching a legitimate native Torn identity/session exchange. Do not spoof Google OAuth, steal browser cookies, or assume external Chrome login shares authentication with Android WebView.

## Torn scripting-rule constraint

Torn's January 2026 scripting clarification says scripts/applications may use data from an API or from a page the user manually loaded and is actively viewing. They must not scrape unfocused/hidden pages or use page/WebSocket data from those pages to generate external alerts/aggregation.

Implications for TornFCA:

- Visible Torn-backed chat WebView: viable research/implementation path.
- Hidden WebView kept alive solely to collect chat: do not implement.
- Background WebSocket scraping: do not implement.
- User-triggered one-message send through the visible Torn chat: viable research path.
- Background push derived by scraping the Torn page: do not implement.

## Route C — TornFCA community backend

Keep the existing TornFCA faction/community chat and private Leadership Chat as fallback / app-native services. The UI should stay provider-agnostic so the Faction tab can switch to real Torn chat later without replacing the Leadership implementation.

## Next research targets

1. Inspect current Torn Chat 3.0 bootstrap/network flow for the user-scoped Sendbird `sessionToken` lifecycle.
2. Determine whether the token comes from a Torn first-party HTTP endpoint that is safe to invoke only inside an authenticated, foreground Torn WebView.
3. Determine whether Torn exposes an officially reusable client app ID while keeping privileged token creation server-side.
4. Continue device-testing the foreground-only Torn faction-chat WebView with Torn email/password authentication.
5. Build selectors around stable DOM semantics and test Desktop/mobile Torn Chat 3.0 changes.
6. Preserve explicit user action for sends.
7. Investigate legitimate native Google/Torn authentication or session handoff without copying Torn PDA private auth code/credentials.

## Sources / evidence reviewed

- Torn forum: Updated Rules Page — Scripting & Scraping, Jan. 26 2026.
- Torn forum: Chat 3.0 incidents where staff identifies Sendbird and discusses regenerating `sessionToken`, Oct. 2025.
- Torn PDA public repository: `sendbird_controller.dart`, `.env_example`, architecture docs, faction-chat send path, current auth dependencies, and build documentation describing private/stubbed native auth files.
- Google OAuth documentation: embedded WebViews are not a supported OAuth user-agent path.
- Torn PDA forum discussions: Google login inside native WebView limitation and Torn password/recovery workaround.
- GreasyFork: Torn Chat Banking Helper (live faction-chat DOM observer).
- GreasyFork: Better Faction Chat (DOM enhancement of Torn's current faction chat).
- GreasyFork: Torn DIBS Button + Hospital Timer + TornPDA Support (user-triggered real faction-chat input/send interaction).

No source reviewed so far provides a legitimate public Sendbird privileged API token for TornFCA, and none is required for the foreground WebView fallback.
