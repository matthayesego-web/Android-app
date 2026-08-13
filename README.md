# Duck Force Torn Toolkit — Android

A single Android application containing three previously built Torn tools:

1. **Torn Faction Xanax Auditor v2.0**
2. **Torn Xanax Armory Log v1.2**
3. **Torn Train Payment Calculator v1.0**

## Android app version

**v0.2.0 prototype**

v0.2.0 adds Torn API-key authentication, Duck Force membership verification, encrypted local API-key storage, and Torn-position-based access tiers. Leader and Co-leader positions receive global access plus the Rank Access Control screen. Red/Black positions receive global tool access, Orange receives elevated access, and Green receives member access.

The original HTML tools remain bundled inside the APK as local assets. Each tool gets its own internal WebView origin so its saved browser data is isolated from the other tools. Torn tools automatically receive the API key already connected to the Android app.

The Torn API tools send their requests only to `api.torn.com`. The Android shell proxies those requests locally through the app's WebView client to make the existing browser-based API logic reliable inside Android without exposing an additional server.

See `ACCESS_CONTROL.md` for the permission architecture. A shared Google Apps Script / Google Sheets backend scaffold is included in `backend/AccessBackend.gs` for the upcoming globally editable rank/module access matrix.

## Automatic APK builds

Every push to `main` triggers `.github/workflows/android-build.yml`.

The workflow:

- uses Java 17
- uses Gradle 8.13
- uses the Android SDK provided by GitHub's Ubuntu runner
- builds `:app:assembleDebug`
- uploads `DuckForce-Torn-Toolkit-v0.2.0.apk` as a GitHub Actions artifact

You can also run it manually from **Actions → Build Android APK → Run workflow**.

## Important signing note

The current workflow builds a standard Android **debug APK** for testing and sideloading. A permanent release-signing keystore should be configured with GitHub Secrets before treating builds as production releases, because stable signing is required for seamless in-place updates between release APKs.

## Source tools

The bundled tool assets are stored in:

- `app/src/main/assets/tools/xanax_auditor.html`
- `app/src/main/assets/tools/armory_log.html`
- `app/src/main/assets/tools/train_calculator.html`
