# TornFCA — Google Play / Public Release Roadmap

Current development line: v0.9.x community beta preparation
Production target: v1.0.0 after closed-beta validation

## Member-first beta objective
Before public release, TornFCA must be clearly useful to an ordinary faction member without payment or leadership permissions. The free core should include:
- My Day / readiness summary.
- Current war, chain and personal OC context.
- Personal ranked-war participation.
- Member-safe faction overview and roster/directory.
- Universal training resources.
- Faction-scoped training rules and custom guide library.
- Faction community/chat and important notification history where the community backend is enabled.

Leadership tools remain additive and permission-aware. Premium should add convenience, history, automation and analytics rather than removing the basic information members need to participate.

## v0.9.x — Community beta hardening
- Continue the member-facing navigation and UI overhaul.
- Expand member-safe faction information and self-service tools.
- Add faction-local training expectations and guide publishing.
- Verify tenant isolation for chat, training content, notifications and any future shared faction data.
- Continue multi-faction testing; no Duck Force-only production assumptions.
- Maintain restore branches before high-risk changes.
- Keep `main` untouched until a tested release is explicitly approved for promotion.

## Legal / policy work required before production
- Final privacy policy and in-app privacy disclosure.
- End User License Agreement (EULA).
- Terms & Conditions / Terms of Service.
- Add EULA and Terms links to the About page.
- Add appropriate EULA / Terms acknowledgement to the login/onboarding flow before public production release.
- Decide whether acceptance must be explicit (checkbox/button + versioned acceptance record) based on the final legal text and account/data model.
- Complete Google Play Data safety answers from the final data inventory.
- Clearly describe Torn API-key handling, optional third-party provider connections, community backend data, push notifications and any paid entitlements.
- Accessibility, phone/tablet layout, icon and crash/error-state review.

## Billing / entitlement work before monetization
- Production entitlement backend keyed by Torn `player_id` and `faction_id`.
- Google Play Billing integration for Android digital entitlements.
- Server-side purchase-token verification.
- Restore purchase / refresh entitlement flow.
- Subscription/product descriptions, cancellation and support information.
- Developer/test entitlement simulation must remain unavailable in public builds outside the hidden protected developer tooling.

## Signing and release
- `applicationId`: `com.matthayesego.duckforcetoolkit` unless an explicitly approved migration occurs before production.
- targetSdk / compileSdk remain API 36 or later as required by Google Play at release time.
- Produce signed Android App Bundle (.aab) for Play testing.
- Use Google Play App Signing with a separate upload key.
- Preserve the permanent direct-distribution signing identity and backup outside source control.
- Run internal and closed Play testing before production rollout.
- Direct APK distribution may remain available for approved testers where appropriate.

## v1.0.0 production gate
Do not promote to production until all of the following are true:
1. Member-first free core is stable across multiple factions.
2. Leadership permissions and faction isolation have been re-audited.
3. Community backend security checks pass.
4. EULA, Terms, Privacy Policy and Data safety are complete.
5. Billing/entitlements are production-safe if monetization is enabled at launch.
6. Signed release/AAB update path is tested.
7. Closed-beta feedback has no unresolved release-blocking issues.
