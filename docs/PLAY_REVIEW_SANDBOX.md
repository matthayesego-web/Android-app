# TornFCA — Google Play Review Sandbox Design
Prepared: 2026-08-20
Status: Implemented on `work/v0.10.18-beta-hardening`; not deployed to production

## Decision

Use a dedicated **Play Review Sandbox** instead of providing Google with a real Torn production account/API key.

Google requires review access to be reusable, always valid, and sufficient to inspect restricted functionality.
The sandbox is therefore part of the submitted app and uses deterministic local sample data.

## Entry

On the unauthenticated TornFCA launcher, reviewers can choose:

- **Continue to TornFCA** for the normal live Torn sign-in flow.
- **Google Play Review Access** for the isolated synthetic review environment.

The review environment asks for a reusable testing-only code supplied in Play Console App Access instructions.
The code is not a production secret and cannot grant access to Torn, Firebase, Apps Script, Premium administration,
or developer administration.

## Review personas

### Member
Demonstrates representative synthetic versions of:
- My Day
- Ranked War / chain context
- Organized Crime
- Member Directory
- Training Resources
- Faction Announcements
- Notification Center
- Faction Chat and safety controls
- Banking request flow

### Faction Leader
Adds representative synthetic versions of:
- Leadership Attention
- Faction Activity
- War Prep Leadership
- Banking Management
- Announcement publish/delete controls
- War payout workflow
- Community moderation

## Absolute isolation rules

When Review Sandbox is active:

1. Do not read or save a Torn API key.
2. Do not call Torn APIs.
3. Do not call any TornFCA Apps Script backend.
4. Do not initialize/register Firebase Cloud Messaging for the sandbox identity.
5. Do not send push notifications to real users.
6. Do not refresh Premium entitlement.
7. Do not expose Developer/Admin credentials.
8. Do not write banking, chat, announcements, reports or feedback to production.
9. Actions mutate only synthetic/local review state.
10. Every review screen is visibly marked **PLAY REVIEW SANDBOX**.

## Current implementation

- `PlayReviewStore.java`
  - local-only active flag
  - Member / Leader persona
  - clear/exit support

- `PlayReviewActivity.java`
  - reusable review-code entry
  - persona switcher
  - deterministic synthetic member/war/OC/training/announcement/chat/banking/notification/leadership data
  - simulated write actions that only show a local confirmation message

- `AccessGateActivity.java`
  - resumes an active review session before normal warm-up
  - exposes Review Access only when there is no saved Torn key
  - leaves the authenticated TornFCA path intact

## Play Console instructions draft

"All or some functionality is restricted.

To review without using a real Torn account:
1. Launch TornFCA and accept the displayed Legal & Privacy documents.
2. On the unauthenticated launcher, tap 'Google Play Review Access'.
3. Enter the reusable review code provided below.
4. Choose Member or Faction Leader from the review persona selector.
5. All review-mode data is synthetic and all writes remain inside the review sandbox. No live Torn account,
   faction, Firebase recipient, banking record or user-generated content is affected.
6. Use 'Exit Review Sandbox' to return to the normal TornFCA path."

## Acceptance tests

- Fresh install -> Legal acceptance -> Review Access works without Torn credential.
- Review sandbox remains usable without live Torn/backend connectivity.
- Member persona cannot see Leader-only review cards.
- Leader persona can inspect representative leadership surfaces.
- Synthetic write buttons cannot touch production services.
- No PushDevices row is created by review mode.
- No Torn request is made by review mode.
- Exiting review clears the review session.
- Normal Torn login remains unchanged after sandbox use.

## Release rule

Declare TornFCA as restricted functionality in Play Console and provide the reusable review resource. Reconfirm the
exact submitted AAB exposes all functionality Google needs to evaluate before submission.
