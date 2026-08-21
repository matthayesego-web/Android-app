# TornFCA Release Dependency & Data Safety Inventory

Last reviewed: 2026-08-17

Purpose: keep the Google Play Data safety answers grounded in the exact Android dependencies and app behavior rather than package-name guesses from an APK.

## Direct Android dependencies

Current `app/build.gradle.kts` declares:

- `androidx.webkit:webkit:1.14.0`
- Firebase BOM `34.16.0`
- `com.google.firebase:firebase-messaging`

The app does **not** directly declare Firebase Analytics, Crashlytics, Performance Monitoring, Authentication, Firestore, Realtime Database, Remote Config, or Firebase App Distribution.

## Firebase configuration

Current production manifest/application intent:

- Firebase Cloud Messaging remains available for push notifications.
- `firebase_analytics_collection_enabled=false` is explicitly declared.
- `firebase_messaging_auto_init_enabled=false` is explicitly declared.
- `TornFcaApplication` initializes Messaging only when the current legal version has already been acknowledged.
- The first-run legal acceptance flow explicitly initializes Messaging only after acceptance, so the user does not need to restart the app to enable configured push.
- TornFCA does not call `FirebaseAnalytics`, `setAnalyticsCollectionEnabled`, or `setDeliveryMetricsExportToBigQuery`.
- The Community Security Audit fails if these privacy boundaries disappear or an Analytics/BigQuery delivery-metrics integration is introduced without an explicit privacy review.

## What FCM still means for Data safety

Disabling Analytics does **not** mean Firebase/FCM processes no data.

Google's current Firebase Android Data safety documentation states that Firebase Cloud Messaging automatically collects:

- application version; and
- Firebase user-agent information.

FCM also transitively includes Firebase Installations. Google's disclosure documentation states that Firebase Installations automatically generates/collects a per-installation Firebase installation ID (FID) and Firebase user-agent information. Google describes the FID as a per-installation identifier that does not uniquely identify a user or physical device.

The Firebase user agent can include device metadata such as OS version, device name/model/brand/form factor, install source, and the Firebase SDKs/versions present in the app. Google states that this user-agent bundle is used to provide/maintain/improve Firebase and is not linked to a user or device identifier.

TornFCA delays its FCM Messaging initialization until current legal acknowledgement, but once Messaging is enabled the Play Data safety form must still evaluate FCM/Firebase Installations data processing.

## TornFCA-specific push data

TornFCA's own push-registration/community flow may additionally process/store:

- FCM registration token;
- verified Torn player ID;
- verified Torn faction ID;
- role/scope needed for faction-targeted or player-targeted push delivery; and
- notification payload data needed for the intended alert.

The Torn API key is used for verification where required but is not intended to be persisted as part of push registration.

## FCM analytics distinction

Google documents optional notification interaction analytics when the Firebase Analytics SDK is included. TornFCA does not directly include that SDK and explicitly disables Firebase Analytics collection in the manifest.

This does **not** remove FCM's normal operational data described above.

## BigQuery delivery metrics

Google documents FCM delivery-metrics export to BigQuery as usage-dependent and requiring the relevant integration/API call. TornFCA currently has no `setDeliveryMetricsExportToBigQuery` call. The security audit rejects one unless the privacy/Data safety posture is deliberately reviewed first.

## Why analytics-named files may appear in an APK

Do not decide the Data safety form by scanning filenames alone. Firebase/Google libraries can package shared connector/protobuf/resources whose filenames contain words such as `measurement` or `analytics` even when `firebase-analytics` is not a direct dependency.

The release decision should instead use all of the following:

1. exact Gradle dependency graph for the candidate;
2. manifest configuration;
3. source/API invocation search;
4. Google's current SDK disclosure documentation; and
5. actual TornFCA backend behavior.

## Pre-Play verification checklist

Before each production submission:

- [ ] Run `./gradlew :app:dependencies` (or equivalent targeted release dependency report) for the exact candidate.
- [ ] Confirm `firebase-analytics`, Crashlytics and Performance are not present unless intentionally added.
- [ ] Confirm `firebase_analytics_collection_enabled=false` remains in the merged production manifest.
- [ ] Confirm `firebase_messaging_auto_init_enabled=false` remains in the merged production manifest.
- [ ] Confirm first-run Messaging initialization is still gated by `LegalAcceptanceStore.hasAcceptedCurrent` and enabled immediately after legal acceptance.
- [ ] Confirm `setDeliveryMetricsExportToBigQuery` is absent unless intentionally enabled and disclosed.
- [ ] Confirm only the intended Firebase client configuration is compiled into Android; service-account/private sender credentials remain server-side.
- [ ] Review Google's current Firebase Android Data safety disclosure page for the exact SDK versions in use.
- [ ] Reconcile this inventory with `docs/PLAY_RELEASE_CHECKLIST.md`, the in-app Privacy Policy, and the public privacy-policy page.

## Official references to re-check at release time

- Firebase Android Play data disclosure: `https://firebase.google.com/docs/android/play-data-disclosure`
- Firebase privacy/security information: `https://firebase.google.com/support/privacy/`
- Firebase Cloud Messaging Android setup/auto-init: `https://firebase.google.com/docs/cloud-messaging/android/get-started`

This file is an engineering inventory, not legal advice and not a substitute for the final Play Console declarations.
