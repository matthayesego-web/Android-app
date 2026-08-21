# TornFCA Developer Access

The hidden developer panel remains accessible through the triple-tap footer gesture plus a developer password.

## Security model
- The normal beta/public access-code wall has been removed.
- The developer panel remains hidden from normal navigation.
- Developer access does not require owner/player identity; anyone intentionally given the password can enter.
- The plaintext developer password is not stored in source control.
- The Android build receives only a SHA-256 password hash through `BuildConfig.DEVELOPER_ACCESS_SHA256`.

## Setting the password before a build
The build checks for the hash in this order:
1. Gradle property `TORNFCA_DEV_PASSWORD_SHA256`
2. Environment variable `TORNFCA_DEV_PASSWORD_SHA256`
3. Current protected fallback hash for development builds

GitHub Actions is prepared to read repository secret `TORNFCA_DEV_PASSWORD_SHA256` during the Android build. Once that secret is configured, changing the developer password does not require editing `DeveloperGateActivity`.

## Production recommendation
Before the Play closed beta, set `TORNFCA_DEV_PASSWORD_SHA256` as a protected GitHub Actions secret using the SHA-256 hash of the chosen master developer password. Do not commit the plaintext password or put it in release notes, screenshots, issues, or source comments.
