# TornFCA Community + Push Deployment

TornFCA v0.9.20 contains the Android client for app-native faction chat, notification preferences/inbox, Firebase Cloud Messaging (FCM) delivery, and tenant-scoped push registration. The client remains fully usable when cloud services are not configured.

## Security model

- Never commit a Torn API key, Firebase service-account private key, signing key, or backend credential to this repository.
- Android may contain Firebase **client identifiers** (App ID, API key, project ID, sender ID). They identify the Firebase app; they are not the server send credential.
- FCM send authority stays server-side in Google Apps Script Script Properties.
- Torn API keys sent to the Community backend are used only to verify the current request. `TornFcaCommunityBackend.gs` does not persist them to Sheets, Properties, or Cache; only a SHA-256 key fingerprint is used for the short identity cache key.
- Every chat row and push-device registration is scoped by verified `faction_id` and `player_id`.
- Leadership chat is Leader/Co-leader only and is intentionally **not pushed** to generic faction devices. Users read it only through the authenticated Leadership channel.

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

Create a dedicated Google Sheet for the Community backend. In that Sheet's bound Apps Script project, use the contents of:

`backend/TornFcaCommunityBackend.gs`

Run once:

`setupTornFcaCommunityBackend()`

This records the Sheet ID in Script Properties and creates:

- `ChatMessages`
- `PushDevices`

Deploy the script as a Web App that can receive Android POST requests. The backend performs its own Torn identity/faction verification on every authenticated action.

Save the deployed Web App URL as:

`TORNFCA_COMMUNITY_BACKEND_URL`

## 3. Firebase server send authority

In the Community Apps Script project, store the FCM HTTP v1 service-account values in **Script Properties only**:

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
4. Use **Send Cloud Push Test** and confirm both Android notification delivery and Notification Inbox persistence.
5. Open Member Center → Faction Chat with two members in the same faction. Verify General/War/OC messages sync and chat push reaches the other device.
6. Verify a different-faction account cannot read the first faction's messages.
7. Verify a regular member cannot open/read Leadership chat.
8. Verify Leadership chat content does not fan-out as a generic faction push.
9. Publish a faction notice and confirm the notice still succeeds even if push delivery is temporarily unavailable.
10. Log out. A stale faction-scoped FCM message must be dropped while no faction is authenticated.

## Premium

Premium entitlement remains tied to Torn `player_id` through the existing Premium backend. Leadership permissions are not a Premium entitlement. Premium client features may be previewed with the Developer Console simulation until the payment/entitlement backend is activated.
