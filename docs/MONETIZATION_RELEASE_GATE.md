# TornFCA Monetization Release Gate

Status: **automatic paid entitlement processing is intentionally disabled by default.**

This gate is separate from ordinary backend/app readiness. TornFCA can deploy and test backend-verified Premium with owner manual grants while real payment processing remains off.

## Why this gate exists

### Torn API policy

Torn's current official API documentation asks API-tool creators to contact Torn if they want to advertise, accept voluntary real-money donations, or charge users for usage. The same documentation requires clear API-key usage disclosure where users provide their key when data/key is stored or shared.

Official reference:
- `https://www.torn.com/api.html`

Relevant current sections:
- Acceptable usage / charging contact requirement
- API Terms of Service Guidelines
- Data Storage / Data Sharing / Purpose of Use / Key Storage & Sharing / Key Access Level disclosure
- optional-service ToS linking

### Google Play distribution

For a Play-distributed Android build, Google's current payments policy generally requires Google Play Billing for payments that unlock digital in-app functionality unless a stated exception/eligible alternative-billing program applies.

Official references:
- `https://support.google.com/googleplay/android-developer/answer/9858738`
- `https://developer.android.com/google/play/billing/backend`

A direct APK distribution and a Google Play distribution may therefore have different payment constraints. Do not assume an in-game Torn item transfer is an allowed Google Play payment method for a Play-distributed Premium unlock.

## Technical fail-closed controls

Premium backend v1.2.0 uses Script Property:

- `MONETIZATION_APPROVED=false` by default

While false:
- normal Premium status works
- owner-only manual grants work for testing
- `installPremiumScanTrigger()` throws and refuses to install the scanner
- `scanPremiumPayments()` throws and refuses to process payments
- the backend GET health response exposes only the non-secret approval state for deployment diagnostics

The app can therefore complete the Free/Premium matrix and backend smoke tests without accepting payment.

## What is allowed before approval activation

- Deploy Premium backend v1.2.0.
- Configure the admin password securely.
- Build/sign/test v0.10.1 against the backend.
- Use owner-only manual grants for Free → Premium → expiry tests.
- Test remote `disable_premium`.
- Test all Premium feature gates.
- Test entitlement refresh and offline cached display behavior.

Do not install the automatic payment scanner solely to prove the backend works.

## Gate to enable real automatic payments

Before changing `MONETIZATION_APPROVED` to true:

1. Contact/obtain the necessary Torn approval or written guidance for charging users of this API-based tool.
2. Decide distribution channel(s): direct APK, Google Play, or both.
3. For each distribution channel, confirm the intended payment mechanism is permitted there.
4. Finalize user-facing Premium product/pricing/refund/support language.
5. Configure the minimum-access server-only `OWNER_API_KEY` required by the Torn payment-log scanner.
6. Set `MONETIZATION_APPROVED=true` deliberately in Apps Script Script Properties.
7. Run `installPremiumScanTrigger()` once.
8. Confirm exactly one one-minute scanner trigger exists.
9. Test receipt dedupe/replay safety.
10. Test stacking enabled and disabled behavior.
11. Test expired/renewed Premium.
12. Test malformed/wrong-message/non-Xanax logs are ignored/recorded without granting.
13. Confirm the deployed backend version still matches the audited source before accepting real payments.

## Release-channel decision

The **app itself does not need paid scanning enabled to reach v1.0.0 technical readiness**. A valid launch option is:

- v1.0.0 with Free + Premium architecture deployed,
- Premium entitlement test/admin paths operational,
- automatic monetization still disabled,
- enable production Premium sales later only after this policy/payment gate is closed.

This avoids making monetization approval a reason to weaken or rush the technical release.
