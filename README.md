# Duck Force Torn Toolkit — Android

A single Android application containing three previously built Torn tools:

1. **Torn Faction Xanax Auditor v2.0**
2. **Torn Xanax Armory Log v1.2**
3. **Torn Train Payment Calculator v1.0**

## Android app version

**v0.1.0 prototype**

The original HTML tools are bundled inside the APK as local assets. Each tool gets its own internal WebView origin so its saved browser data is isolated from the other tools.

The Torn API tools still send their requests only to `api.torn.com`. The Android shell proxies those requests locally through the app's WebView client to make the existing browser-based API logic reliable inside Android without exposing an additional server.

## Automatic APK builds

Every push to `main` triggers `.github/workflows/android-build.yml`.

The workflow:

- uses Java 17
- uses Gradle 8.13
- installs Android API 36 / Build Tools 35.0.0
- builds `:app:assembleDebug`
- uploads `DuckForce-Torn-Toolkit-v0.1.0.apk` as a GitHub Actions artifact

You can also run it manually from **Actions → Build Android APK → Run workflow**.

## Important signing note

The current workflow builds a standard Android **debug APK** for testing and sideloading. A permanent release-signing keystore should be configured with GitHub Secrets before treating builds as production releases, because stable signing is required for seamless in-place updates between release APKs.

## Source tools

The bundled tool assets are stored in:

- `app/src/main/assets/tools/xanax_auditor.html`
- `app/src/main/assets/tools/armory_log.html`
- `app/src/main/assets/tools/train_calculator.html`
