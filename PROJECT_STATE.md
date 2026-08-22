# TornFCA Project State

This is the durable recovery note for TornFCA. Read this first after any lost chat/session. Update it whenever branch policy, release state, architecture, major features, deployment assumptions, restore points, or next work changes.

## Branch policy
- `main` = Google Play / production source. Do not develop directly here.
- `development` = accepted next-version development source.
- `work/*` = temporary implementation/validation branches created from `development`.
- `restore/*` = recovery snapshots before risky changes.
- Production launcher identity = **T**. Development launcher identity = **D**.
- Never merge to `main` merely to produce a test APK.

## Current versions
- Production `main`: v0.10.19, versionCode 74. This remains the Google Play line.
- Development `development`: v0.10.21, versionCode 76, versionName suffix `-development`.
- Development package: `com.matthayesego.duckforcetoolkit.beta`, visible app name **TornFCA Development**.
- v0.10.21 validated PR: #22, merged into `development` after CI success.
- Validated v0.10.21 APK SHA-256: `f1ea9d953b53db798cb3e0400ff7751d8b5b88ce4b3095e9128f1cc0fbf568ab`.
- Pre-v0.10.21 restore point: `restore/development-pre-v0.10.21-real-chat-2026-08-21`.

## Core architecture
- Android native app, Java/Kotlin Gradle project, compile/target SDK 36, min SDK 24.
- Torn API key is stored through `SecureApiKeyStore`; never commit or expose user keys.
- Torn API v2 supplies authenticated player/faction data.
- Separate TornFCA backend endpoints exist for faction, community, premium, developer, WarPay and feedback.
- Firebase/FCM supplies cloud push when build secrets are configured.
- Android signing material stays in GitHub secrets; Development CI permanently signs and verifies test APKs.
- Play Review mode remains synthetic/isolated from Torn and production backend writes.

## Accepted through v0.10.20
- Automatic Android notification permission onboarding after authenticated startup; denial is remembered.
- Automatic push initialization/sync.
- Notification deep links route to Chat, War, Chain/OC, Banking, announcements or moderation.
- Native TornFCA community chat with General / War / OC / Leadership channels.
- Chat safety actions behind tapping another member message: View Torn Profile / Report / Block.
- Leaders have Reports & Moderation entry in Chat; backend still independently enforces moderation permissions.
- Opt-in War Chain Live Tracker foreground service: ~30 s Torn refresh during active chain, ~60 s during ranked war with no active chain, local chronometer for second-level timeout, Android 16 promoted-ongoing request when eligible.

## v0.10.21 accepted Development changes
### Real Torn faction chat experiment
- `RealTornChatActivity` is implemented and compiled/signed successfully.
- Entry path: native TornFCA Chat -> **Torn Chat**.
- Hosts Torn's own `https://www.torn.com/` page in a dedicated Android WebView.
- First use can require the user's normal Torn web login. TornFCA does not receive/bridge the Torn password.
- Torn owns authentication, chat transport, messages and Send behavior.
- TornFCA detects the visible faction chat DOM, primarily using `div[id^="faction-"]` plus defensive `#chatRoot` / chat-box fallbacks, and expands that actual Torn chat box to the full WebView.
- The user sends through Torn's real visible textarea/send control; TornFCA does not call Sendbird with an unowned credential.
- The WebView only allows in-WebView navigation to HTTPS Torn hosts; external links are handed to the system browser.
- When the activity leaves foreground, TornFCA stops the load and blanks the WebView to `about:blank`; on return it reloads Torn. This is deliberate so the experiment is not a hidden/unfocused Torn page or WebSocket collector.
- Native TornFCA community chat remains the fallback and is not removed.
- Device test plan: `docs/TORN_CHAT_V01021_TEST_PLAN.md`.
- Research trail: `docs/TORN_CHAT_INTEGRATION_RESEARCH.md`.

### Development D icon
- `main` continues to use the production T launcher.
- Development build uses `@mipmap/ic_launcher_development` with dedicated D artwork.
- CI checks the D-icon configuration and fails Development validation if it is missing.
- Before future Play promotion, production T identity must be preserved/restored for the release build.

## Real Torn chat research boundaries
- Torn Chat 3.0 is Sendbird-backed.
- Torn PDA public source proves native Sendbird faction chat is technically possible, but its privileged Sendbird API/server credential is not ours and must never be copied/extracted/shipped.
- Continue researching whether Torn exposes a legitimate first-party user-scoped Sendbird/session-token exchange.
- Public current Torn userscripts confirm active-page DOM approaches using faction chat selectors and Torn's actual input/send UI.
- Do not implement hidden WebView scraping, hidden WebSocket interception, or background alerts derived from an unfocused Torn page.

## Moderation plan
- Reports are faction-scoped.
- Normal members can report/block; blocking remains device/faction local.
- Leaders/Co-leaders should review faction reports when the backend moderation policy enables them.
- Owner/developer retains owner/global override where backend policy allows.
- Never trust only Android UI for permissions; backend must re-verify Torn identity/faction/position/abilities.
- Remaining task: verify/enable deployed Community backend leader moderation policy before production promotion.

## Release discipline
1. Read this file and the chat research doc before starting work.
2. Compare `main...development`.
3. Create a `restore/...` snapshot from `development` before risky changes.
4. Implement on `work/...`.
5. Open PR into `development`.
6. Require Development CI compile + permanent signing + package/version/label checks + branch identity preflight.
7. Merge only after CI passes.
8. Device-test the signed APK.
9. Promote to `main` only as an explicit release action after production checks, including production T icon.

## Immediate next work
- Device-test v0.10.21 Real Torn Chat: login, chat detection/focus, incoming messages, explicit send, background/reload behavior and DOM-change failure state.
- If DOM focus works but layout is imperfect, tune selectors/CSS from screenshots/device observations rather than guessing.
- If real Torn chat is stable, decide whether it becomes primary member chat while keeping native Leadership/TornFCA chat where useful.
- Continue legitimate user-scoped Sendbird bootstrap research in parallel.
- Verify Community backend leader moderation policy.
- Continue War Chain Live Tracker device tuning.

## Secret/safety recovery rule
Never expose or commit Torn API keys, Firebase secrets, Android signing material, Apps Script service credentials, or privileged third-party Sendbird credentials. If a future session lacks context, reconstruct from GitHub state and this file rather than inventing credentials or architecture.
