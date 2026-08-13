# Duck Force Companion — Release Signing

Permanent direct-distribution signing begins with v0.4.0.

## Public release certificate
- Subject: CN=Duck Force Companion, OU=Duck Force, O=Duck Force, L=Peterborough, ST=Ontario, C=CA
- Algorithm: RSA 4096-bit
- Valid through: 2053-12-29
- SHA-256: C3:EC:C3:5A:64:F8:E7:EC:ED:DA:1C:A4:6D:5F:E0:84:1B:89:0E:ED:9C:78:CC:B6:71:6D:61:A9:23:7F:03:22
- SHA-1: 9A:57:97:E4:CB:CE:2B:AF:CE:5E:A2:8F:FF:C9:11:09:52:E3:BB:98

## Rules
- Never commit the keystore or its password to this repository.
- Keep multiple secure offline backups of the permanent keystore.
- Direct-distribution APKs from v0.4.0 onward should use this same signer so Android can install them as updates.
- The earlier debug-signed prototypes use a different signer and normally require one uninstall before installing the first permanent-signed release.

## Future GitHub Actions secrets
When automatic release signing is enabled, use encrypted repository secrets such as:
- ANDROID_KEYSTORE_BASE64
- ANDROID_KEYSTORE_PASSWORD
- ANDROID_KEY_ALIAS
- ANDROID_KEY_PASSWORD

The workflow should reconstruct the keystore only inside the ephemeral runner, sign the release artifact, verify it, then delete the temporary keystore.

## Google Play target
For the v0.7.0 Play Store release, use Google Play App Signing and maintain a separate upload key. Decide during Play enrollment whether the permanent Duck Force release key will be supplied as the Play app-signing identity or kept solely for direct distribution.
