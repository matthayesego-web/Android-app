# TornFCA Community + Push Deployment

TornFCA v0.9.21 contains the Android client for app-native faction chat, notification preferences/inbox, Firebase Cloud Messaging (FCM), and the faction-scoped Training Center. The client remains usable when cloud services are not configured; universal training guides remain available even when faction-shared content is offline.

## Security model

- Never commit a Torn API key, Firebase service-account private key, signing key, or backend credential to this repository.
- Android may contain Firebase client identifiers (App ID, API key, project ID, sender ID). They identify the Firebase app; they are not the server send credential.
- FCM send authority stays server-side in Google Apps Script Script Properties.
- Torn API keys sent to the Community backend are used only to verify the current request. `TornFcaCommunityBackend.gs` does not persist them to Sheets, Properties, or Cache; only a SHA-256 key fingerprint is used for the short identity cache key.
- Chat rows and push-device registrations are scoped by verified `faction_id` and `player_id`.
- Training rules and custom guides are stored with `faction_id` and are returned only for the faction currently verified through Torn.
- Every `training_*` action bypasses the short identity cache and rechecks Torn immediately, so changing/leaving factions revokes the old faction library without a stale-cache grace period.
- Training-rule and guide writes are re-authorized server-side and currently require Leader/Co-leader.
- Leadership chat is Leader/Co-leader only and is intentionally not pushed to generic faction devices.

## 1. Firebase Android client

Create a Firebase project and register the Android package:

`com.matthayesego.duckforcetoolkit`

Record these client values for the build environment:

- `TORNFCA_FIREBASE_APP_ID`
- `TORNFCA_FIREBASE_API_KEY`
- `TORNFCA_FIREBASE_PROJECT_ID`
- `TORNFCA_FIREBASE_SENDER_ID`

Do not add a Firebase service-account JSON/private key to Android or GitHub source.

## 2. TornFCA Community backend

Create or use the dedicated Google Sheet for the Community backend. In that Sheet's bound Apps Script project, use the current contents of:

`backend/TornFcaCommunityBackend.gs`

Run:

`setupTornFcaCommunityBackend()`

The setup is safe to rerun on an existing beta backend: it keeps existing rows and creates the missing sheets/headers needed by the current schema. The current schema uses:

- `ChatMessages`
- `PushDevices`
- `TrainingRules`
- `TrainingGuides`

After replacing the Apps Script source, create/update the Web App deployment so Android reaches the new code. Save the deployed Web App URL as:

`TORNFCA_COMMUNITY_BACKEND_URL`

The backend performs its own Torn identity/faction verification for every authenticated action. GitHub source changes do **not** automatically redeploy a bound Apps Script project, so this deployment step is required before faction rules/custom guides work end-to-end in a cloud-enabled APK.

## 3. Firebase server send authority

In the Community Apps Script project, store the FCM HTTP v1 service-account values in Script Properties only:

- `FIREBASE_PROJECT_ID`
- `FIREBASE_CLIENT_EMAIL`
- `FIREBASE_PRIVATE_KEY`

The service account must be authorized to send Firebase Cloud Messaging messages for that Firebase project. The private key remains server-side.

## 4. GitHub build configuration

For a cloud-enabled candidate, configure repository secrets for the four Firebase client identifiers and `TORNFCA_COMMUNITY_BACKEND_URL`. Existing optional backend secrets remain separate:

- `TORNFCA_FACTION_BACKEND_URL`
- `TORNFCA_PREMIUM_BACKEND_URL`

Use the manual **TornFCA Cloud Candidate** workflow after those values are configured. Standard branch CI intentionally remains capable of producing a cloud-dormant build with empty optional backend values.

## 5. Live verification checklist

1. Install the signed release-package APK and sign in with a faction member.
2. Settings → Notifications: grant Android notification permission and enable desired categories.
3. Settings → Community & Push should report Firebase + Community backend configured and an FCM device token available.
4. Use **Send Cloud Push Test** and confirm Android notification delivery and Notification Inbox persistence.
5. Open Member Center → Faction Chat with two members in the same faction. Verify General/War/OC messages sync and chat push reaches the other device.
6. Open Member Center → Training Center. Confirm universal guides render and the verified faction's rules/custom guides load.
7. As Leader/Co-leader, publish training rules and a custom guide; confirm an ordinary member in the same faction can read them but cannot edit them.
8. Verify a different-faction account cannot read the first faction's chat, training rules, or custom guides.
9. Change/leave factions and immediately retry Training Center; the old faction library must not be returned.
10. Verify a regular member cannot open/read Leadership chat.
11. Verify Leadership chat content does not fan-out as a generic faction push.
12. Publish a faction notice and confirm the notice still succeeds even if push delivery is temporarily unavailable.
13. Log out. A stale faction-scoped FCM message must be dropped while no faction is authenticated.

## Premium

Premium entitlement remains tied to Torn `player_id` through the existing Premium backend. Leadership permissions are not a Premium entitlement. Basic training rules and the current faction guide library remain free member features. Premium/Faction Pro may add history, analytics, automation and compliance monitoring around them rather than paywalling basic training information.
