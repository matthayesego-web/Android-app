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
- Accepted `development`: v0.10.22, versionCode 77, versionName suffix `-development`.
- Development package: `com.matthayesego.duckforcetoolkit.beta`, visible app name **TornFCA Development**.
- v0.10.22 validated PR: #23, merged into `development` after CI success.
- Validated v0.10.22 APK SHA-256: `a8d2952de935c65ea23a0249b3c7f098b5fc8d1e83a79f5239413fcb3d6b9c2e`.
- v0.10.22 validated source head before merge: `6c19f7222fcd7f0b86ba0b8b70faebcc6493ab3c`.
- v0.10.22 merge commit: `0fdbdad0b8951bd6fa60e523129d16c207ba5012`.
- Pre-v0.10.21 restore point: `restore/development-pre-v0.10.21-real-chat-2026-08-21`.
- Pre-v0.10.22 restore point: `restore/development-pre-v0.10.22-chat-login-admin-fixes-2026-08-21`.

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

## v0.10.22 accepted Development corrections
### Chat placement
- v0.10.21 incorrectly injected a Chat button into nearly every shared TornFCA header. User explicitly rejected this.
- **Do not put Chat on every screen.** Chat has one normal, obvious faction/community entry point.
- `TornFcaUi.header()` no longer injects Chat globally.
- The single normal Chat entry is a prominent top-level **Faction Chat** card directly on the main **Faction** page.
- The Faction Tools submenu no longer contains a duplicate Chat row; it contains OC, Chain and Strength Intel only.
- Inside the Chat destination itself, the `Torn Chat` button is allowed because it switches chat provider rather than creating another normal navigation entry.
- Future navigation changes must preserve the one-place rule unless explicitly redesigned by the user.

### Torn website authentication
- Torn API-key authentication and Torn website-session authentication are different systems.
- A Torn API key **cannot** be used to log the embedded Torn website/WebView in.
- Google explicitly blocks OAuth sign-in inside embedded Android WebViews; this is why a Google-authenticated Torn account cannot simply use a Google button inside `RealTornChatActivity`.
- Torn PDA has documented the same limitation. Practical current fallback: use the Torn email/password web login; if the account was created/used through Google and the password is unknown, use Torn's normal Recover account flow to set/reset a Torn password.
- v0.10.22 states this clearly in the Real Torn Chat UI rather than implying Google sign-in or API-key login should work.
- Continue researching a legitimate native Google/Torn session bridge, but do not spoof Google OAuth, copy browser cookies, or claim external Chrome login will authenticate Android WebView unless a verified Torn-supported exchange exists.

### Premium Admin authorization
- User explicitly requested removal of the extra Developer Password field.
- Premium admin mutations are owner-only by Torn identity. The client API key is validated and the backend verifies the caller Torn player ID against the configured TornFCA owner ID.
- v0.10.22 removes the Developer Password UI and stops sending `admin_password` from Android.
- Canonical `backend/TornFcaPremiumBackend.gs` v1.4.0 removes the second password check for `admin_config` / `admin_grant`; verified Torn owner identity remains mandatory server-side.
- Backend deployment is separate from committing Android/GitHub source. Do not claim the live Apps Script endpoint is upgraded until the v1.4.0 web-app deployment is actually published and verified.
- v0.10.22 CI explicitly checks that global Chat injection and client admin password handling do not regress, validates Premium backend syntax, compiles the Android Development build, permanently signs it and verifies package/version/label.

## Queued Internal Beta follow-up changes
These are planning notes only. Do not treat them as implemented until they are built, validated and device-tested on a `work/*` branch and accepted into `development`.

### Restore the in-app Feedback entry
- The feedback feature itself already exists: `FeedbackActivity` and `FeedbackBackendClient` support Bug, Feature Request, UI/UX, Performance, Access/Permissions and Other submissions plus the signed-in user's recent submission history.
- The current command-center **More** page lost the visible Feedback route during navigation consolidation.
- Restore a clear **Feedback & Requests / Send Feedback** entry on the current More/App Control page so Internal/Closed Beta users can submit feedback directly into the live feedback backend instead of contacting the owner manually.
- This should be a navigation repair only; do not replace or duplicate the existing feedback backend/form unnecessarily.

### Premium Admin should use authorized Developer Console access
- v0.10.22 correctly removed the extra Premium Developer Password, but it currently restricts Premium mutations to the single owner Torn ID.
- Desired next behavior: a user who has already been explicitly authorized into the TornFCA Developer Console may use the Premium Admin controls without a second password.
- Do not trust Android UI alone. Premium backend mutations must validate a real short-lived Developer Console session/token server-side (or an equivalent backend-to-backend authorization mechanism) before allowing delegated Premium administration.
- Owner/root access remains valid. Revoking a developer from the Developer Console should also remove their ability to perform Premium admin mutations without requiring another app update.

### Complete Premium entitlement administration
- Add a current-status lookup for a target Torn player ID before changing entitlement state.
- Keep **Grant Complimentary Premium** and support extending an existing entitlement by a chosen number of days.
- Add **Remove / Revoke Premium** for manually granted/promotional access when needed.
- Revocation should not silently delete history. Prefer an expired/revoked entitlement state plus an auditable event/source such as `ADMIN_REVOKE`.
- Add a confirmation before destructive revoke/removal.
- Record who performed grant/extend/revoke, target player ID, amount/duration, timestamp and action in an admin audit trail.
- Paid Xanax-derived entitlement processing must remain separate from these manual administrative grants/revocations and preserve existing idempotency/deduplication protections.

### Premium visual identity polish
- Keep the existing FREE / PREMIUM account indicator.
- Make verified Premium slightly more pronounced with a restrained gold treatment, preferably a thin gold border/accent on the account/status hero or tier badge.
- Avoid large glows, animation or a major layout redesign.
- Complimentary Premium should receive the normal verified Premium visual treatment. Developer Preview may remain visually distinct from server-verified Premium.

### Closed Beta / production UI cleanup
- Before Closed Beta/live, inventory and remove or hide developer-only/test-only controls that normal users do not need, including test-push/debug utilities and temporary diagnostics.
- Prefer keeping useful troubleshooting tools available only in Development/internal builds rather than exposing them in the public Play release.
- Continue small UI cleanup for duplicate/redundant headers, repeated back controls, temporary labels and extra buttons that no longer add value.
- Do not perform a broad redesign while stability testing is underway; treat these as targeted polish changes.

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
- Device-test v0.10.22: confirm Chat appears once on the main Faction page and no longer appears on unrelated screens.
- Publish/verify Premium backend v1.4.0 before testing owner Premium mutations without a password.
- Restore a visible Feedback & Requests route on the current More/App Control page so beta users can submit into the existing feedback backend.
- Design delegated Premium Admin authorization around the existing Developer Console session/access system, then add target status + grant/extend/revoke + audit support.
- Retest Real Torn Chat using a Torn website password / Recover account path and then test faction-chat DOM focus, incoming messages and explicit send.
- Continue researching legitimate native Google/Torn website authentication or user-scoped Sendbird bootstrap.
- If real Torn chat is stable, decide whether it becomes primary member chat while keeping native Leadership/TornFCA chat where useful.
- Verify Community backend leader moderation policy.
- Continue War Chain Live Tracker device tuning.
- Before Closed Beta/live, perform targeted public-build cleanup of test/debug controls and small redundant UI elements, and add the restrained verified-Premium gold accent.

## Secret/safety recovery rule
Never expose or commit Torn API keys, Firebase secrets, Android signing material, Apps Script service credentials, or privileged third-party Sendbird credentials. If a future session lacks context, reconstruct from GitHub state and this file rather than inventing credentials or architecture.
