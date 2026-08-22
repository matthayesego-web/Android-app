# TornFCA v0.10.24 Navigation Audit

Status: implementation audit for the Development navigation overhaul. This document maps user-facing features to one primary navigation home and records intentional contextual duplicates. It does not make legacy compatibility Activities public navigation destinations.

## Navigation rules
- Leadership top-level: Home / Faction / War / Leadership / More.
- Normal member top-level: Home / Faction / War / More.
- Training is not a permanent tab; it is personal character/progression under Home.
- Leadership/admin cards do not appear on ordinary Home, Faction, War or character pages.
- One primary home per feature; duplicates are allowed only when the same action is genuinely useful in another live context.
- Target 3-6 persistent cards per page. Larger groups become named sub-pages.
- Developer/test/diagnostic surfaces stay outside normal public navigation unless separately authorized.

## Home
Primary purpose: the signed-in player's own state and progression.

- My Day -> MemberDailyActivity
- Training & Progress -> Home sub-page
  - Training Center -> TrainingCenterActivity
  - My Training Progress -> TrainingProgressActivity
- Notification Inbox -> NotificationInboxActivity

Removed from Home:
- permanent My War Prep duplicate (primary home is War)
- Faction Announcements duplicate (primary home is Faction)
- leadership Needs Attention card (primary home is Leadership)

## Faction
Primary purpose: communication and shared member-facing faction information.

- Faction Chat -> FactionChatActivity
  - Real Torn Chat remains a provider option inside Chat, not another global navigation entry
- Faction Announcements -> WarNoticeActivity
- Faction Overview -> MemberFactionActivity
- Faction Directory -> MemberDirectoryActivity
- Faction Resources -> FactionResourcesActivity
- Faction Tools -> Faction sub-page
  - My Organized Crime -> FeatureRouter TARGET_OC / OcTrackerActivity path
  - Chain Status -> FeatureRouter TARGET_CHAIN
  - Faction Strength Intel -> FeatureRouter TARGET_STRENGTH / FactionStrengthActivity path

Chat intentionally has one normal app-navigation home: Faction.

## War
Primary purpose: live warfare and the member's own readiness.

- Ranked War -> WarCenterActivity
- Chain Status -> FeatureRouter TARGET_CHAIN
- Territories -> TerritoryWarActivity
- My War Prep -> WarPrepActivity

Intentional contextual duplicate:
- Chain also exists in Faction Tools because it is a faction status tool, but War is the live-combat context.

Excluded from War:
- payout administration
- banking
- cache selling
- armory/admin functions

Those belong to Leadership.

## Leadership
Top-level cards:
- Needs Attention -> LeadershipAttentionActivity
- People & Activity -> leadership sub-page
- War & Intel -> leadership sub-page
- Finance & Assets -> leadership sub-page
- Faction Admin -> leadership sub-page

### People & Activity
- Activity Tracker -> FeatureRouter TARGET_ACTIVITY
- Faction Pulse -> FeatureRouter TARGET_PULSE / QuickIntelActivity pulse mode
- Member Dossier -> FeatureRouter TARGET_LOOKUP / MemberDossierActivity path

### War & Intel
- Spy Intel -> SpyIntelActivity
  - official Torn faction `stats` reports only
  - full/partial reports and age are visible
  - statistical estimates are not labeled spies
- Faction Strength Intel -> FeatureRouter TARGET_STRENGTH
  - intentional leadership-context duplicate; estimates remain visibly distinct from official spies
- Ranked War Payout Calculator -> FeatureRouter TARGET_WAR_PAYOUT / WarPayoutActivity

### Finance & Assets
- Banking -> FeatureRouter TARGET_BANKING / BankingCompanionActivity
- RW Cache Market Advisor -> CacheMarketAdvisorActivity
- Armory Auditor -> ToolHostActivity ARMORY

### Faction Admin
- Announcement Management -> WarNoticeActivity with leadership publish permission
- Guide & Training Management -> TrainingAdminActivity
- Reports & Moderation -> CommunityModerationActivity

Removed/retired navigation label:
- `Operations` / `Faction Operations` as a catch-all bucket.

## More
Primary purpose: app control, support and information rather than Torn gameplay.

- Settings -> SettingsActivity
- TornFCA Premium -> PremiumPreviewActivity
- Feedback & Requests -> FeedbackActivity
- Legal & Privacy -> LegalActivity / LegalDocumentActivity
- About TornFCA -> AboutActivity

Feedback is restored to visible normal navigation. Developer Console and debug/test tooling are not added as ordinary More cards by this overhaul.

## Contextual/non-primary surfaces retained
These Activities continue to exist because they are used from deeper flows, notifications, legacy compatibility, detail screens or development controls. Their existence does not mean they need a top-level navigation card.

Examples:
- RealTornChatActivity: provider experiment launched from Faction Chat
- CommunityModerationActivity: Leadership -> Faction Admin
- WarHistoryDetailActivity / TerritoryWarDetailActivity / WarPayoutReceiptActivity: detail flows
- NotificationSettingsActivity: Settings flow
- PremiumInsightsActivity / PremiumActivityTrendsActivity / PremiumAdminActivity: Premium/developer flows
- WarPrepLeadershipActivity: deeper leadership/war-prep flow where invoked
- DeveloperGateActivity / DeveloperPanelActivity / DeveloperAccessActivity / DeveloperBackendActivity / DeveloperConsoleActivity / BetaCommandActivity: authorized development/admin flows, not normal navigation
- PlayReviewActivity: Play review/testing isolation flow
- legacy `TornFcaScreens$...` aliases: compatibility declarations, not duplicate navigation destinations

## Permission review
- Leadership top-level is rendered only when current faction position resolves as leadership.
- Leadership feature launchers pass current faction ID/name/API availability and position to scoped Activities.
- Backend-controlled features such as chat moderation must continue to re-verify authorization server-side; Android visibility is not a security boundary.
- Spy Intel still depends on Torn returning faction reports to the current key.
- Cache Market Advisor performs decision support only; it does not execute trades or sales.

## Density review
Persistent card counts after overhaul:
- Home: 3
- Home / Training & Progress: 2
- Faction: 6
- Faction Tools: 3
- War: 4
- Leadership: 5
- Leadership / People & Activity: 3
- Leadership / War & Intel: 3
- Leadership / Finance & Assets: 3
- Leadership / Faction Admin: 3
- More: 5

All primary pages remain at or below the 3-6 target except the intentionally small personal training sub-page.

## Device test checklist
1. Member account sees exactly Home / Faction / War / More.
2. Leadership account sees Home / Faction / War / Leadership / More.
3. No Training bottom tab appears.
4. No leadership/admin cards appear on member Home/Faction/War pages.
5. Chat appears as one normal Faction destination and nowhere globally.
6. Home -> Training & Progress reaches both training screens and Back returns to Home.
7. Leadership sub-pages return predictably to Leadership.
8. Spy Intel opens from Leadership -> War & Intel.
9. Cache Market Advisor opens from Leadership -> Finance & Assets.
10. Reports & Moderation opens from Leadership -> Faction Admin.
11. Feedback & Requests opens from More.
12. Android Back from a shell sub-page returns to its parent before leaving the app shell.
13. D launcher identity remains on Development package; production T remains untouched.
