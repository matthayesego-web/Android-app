# TornFCA — Google Play Data Safety Working Worksheet
Prepared: 2026-08-20
Status: Working answers. Reconfirm against the exact AAB before submitting.

This worksheet is intentionally conservative. Google says the developer is responsible for accounting for data
handled by both the app and included SDKs.

## Security / handling questions

### Is all user data encrypted in transit?
**Planned answer: Yes**
- Android disallows cleartext traffic.
- TornFCA backend/Torn/Firebase calls use HTTPS.

### Can users request deletion of collected data?
**Planned answer: Yes, with policy/process completion required**
- Local data can be removed through app controls / app-data clearing / uninstall depending on the data.
- Hosted data needs a published developer/privacy contact and documented deletion-request procedure.
- Before submission, make the public Privacy Policy URL and deletion contact final.

### Is data collection optional?
Mixed by feature:
- Torn identity/faction data: required for live authenticated TornFCA functionality.
- Persisting the Torn API key beyond the current process: optional.
- Firebase push: functionally optional / user-controlled notification categories, and initialization is delayed
  until legal acknowledgement.
- FFScouter/TornStats: optional opt-in.
- Faction chat, banking, announcements, feedback etc.: collected only when the user uses those features.

## Likely Play data types to declare

### Personal info — Name
**Collected: Yes**
Examples:
- Verified Torn player name stored with shared faction content such as chat, notices, banking/workflow rows.
Purpose:
- App functionality
- Account/faction context
- Moderation/support where relevant
Sharing:
- Intended faction/authorized workflow only; separately assess Google's definition of "shared" because processing
  by service providers is treated differently from disclosure to other third parties.

### Personal info — User IDs
**Collected: Yes**
Examples:
- Torn player ID
- faction ID/scope
Purpose:
- Authentication/authorization
- Faction tenant separation
- Shared workflow ownership
- Notifications
- Fraud/security/abuse prevention

### Messages — Other in-app messages
**Collected: Yes**
Examples:
- Faction chat messages
- report reason / message snapshot for moderation
Purpose:
- App functionality
- Community features
- Safety/moderation

### App activity — Other user-generated content
**Collected: Yes**
Examples:
- Faction announcements
- training guides/rules
- banking notes/request text
- War Prep checklist state
- feedback submissions
Purpose:
- App functionality
- Support
- Faction workflows

### App activity / diagnostics — app version / service heartbeat
**Probably declare: Yes (conservative)**
Current Developer telemetry can record:
- salted one-way player-ID hash
- first/last seen
- TornFCA version
It does not store Torn API key or player name in the aggregate telemetry table.
Purpose:
- Analytics / service operation / version distribution
Before final form, map this to Google's exact current App activity / App info & performance field labels.

### Device or other IDs
**Likely declare: Yes**
Reasons:
- FCM registration token is handled by TornFCA backend.
- Firebase Installations generates a Firebase installation ID (FID).
- Firebase documentation says the Installations SDK automatically collects FID and Firebase user-agent data.
Purpose:
- Notifications
- App/service functionality

### App info & performance — diagnostics/crash logs
**Current app-specific answer: verify before submission**
TornFCA's own Developer telemetry is sanitized app/version/heartbeat data, not necessarily crash logs.
If Google Play Android Vitals is the only crash/ANR source, confirm whether any crash SDK is packaged before
checking crash-log/diagnostic fields.
Current Android dependencies inspected include Firebase Messaging, not Firebase Analytics/Crashlytics.

### Financial info
**Do not automatically classify Torn faction banking as real-world financial data**
TornFCA banking requests are workflows involving Torn's in-game currency, not card/bank/payment credentials.
Before answering the Play form, compare the exact current Google definition of "Other financial info" against
virtual-game currency/workflow data. Avoid both over- and under-declaring.

### Purchase history
**Current expected answer: No**
Unless paid Premium/Play Billing is enabled in the exact submitted build. Current Beta monetization is intended
to remain disabled pending approval.

### Location
**Expected: No**
No Android location permission is declared and no TornFCA feature audited here intentionally collects physical
device location.

### Contacts
**Expected: No**
No Contacts permission is declared.

### Photos / videos / audio / files & docs / calendar
**Expected: No**, unless a future feedback attachment/export feature changes the exact submitted build.
Recheck before submission.

### Advertising data
**Expected: No**
Firebase Analytics is disabled and no advertising SDK has been identified in the audited build.
Confirm Play Console Ads declaration separately.

## Torn API key treatment

The Torn API key is authentication/access credential data and should be described clearly in the Privacy Policy
even if it does not map neatly to a visible Play data-type checkbox.

Current implementation:
- session-only by default
- optional 7/30/90-day encrypted retention
- persistent retention uses AES-GCM with an Android Keystore key
- sent over HTTPS to Torn
- sent request-by-request to TornFCA backends where current identity/permission verification is needed
- backends are designed not to persist client API keys

Do not characterize the API key as "never leaves the device"; that would be inaccurate.

## Firebase SDK data to account for

Current Firebase documentation for Android states that Cloud Messaging automatically collects:
- application version
- Firebase user-agent information

Firebase Messaging transitively uses Firebase Installations, which automatically generates/collects:
- Firebase installation ID (FID)
- Firebase user-agent information

TornFCA additionally obtains an FCM registration token and registers it with the Community Backend together with
verified player/faction scope and notification preferences.

Current code:
- Firebase Analytics collection disabled
- FCM auto-init disabled in manifest
- TornFCA initializes messaging only after current legal acknowledgement

## User-generated-content safeguards already present

- report faction-chat message
- block faction-chat user
- unblock controls
- community conduct terms
- moderation workflow
- faction-scoped chat
- faction-scoped shared records

## Final verification before clicking Submit in Play Console

1. Build the exact release AAB.
2. Inventory its SDK/dependency tree.
3. Re-run manifest permissions/export audit.
4. Confirm monetization state.
5. Confirm all backend fields retained by each enabled feature.
6. Confirm public Privacy Policy matches the submitted build.
7. Confirm deletion-request contact/process.
8. Compare every answer against Google's current Data Safety definitions.
9. Include Firebase's current Play-data-disclosure guidance.
10. Save a dated copy of the submitted Data Safety answers with the release tag.
