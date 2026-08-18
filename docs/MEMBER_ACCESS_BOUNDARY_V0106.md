# TornFCA v0.10.6 Member Access Boundary

Standard Member Preview is a presentation-only reducer. It never changes the real Torn identity or grants backend authority.

## Member-visible
- Faction notices: read only.
- Notification inbox and personal notification preferences.
- Training Center and published faction guides/rules: read only.
- Personal Training Progress.
- My War Prep and personal checklist/status sync.
- Faction overview, directory, resources, chat, personal OC, war/territory/chain tools.
- Banking: submit and view own requests only.

## Leadership/admin hidden or blocked in Standard Member Preview
- Notice publishing / faction announcement push.
- Activity Tracker leadership view.
- Leadership Attention.
- Faction Pulse.
- Member Dossier leadership route.
- WarPay management.
- War Prep Management and faction readiness roster.
- Training Management / guide and rule publishing.
- Banking payout queue/management controls.
- Faction-wide OC command data.
- Leadership chat channel.
- Armory Auditor leadership workspace.
- Community moderation and other admin surfaces unless separately entered through the hidden developer channel for testing.

## Server boundaries verified in source
- Faction backend re-verifies Torn faction identity on every authenticated request; notice publishing and banking management are permission-gated.
- Community backend re-verifies current faction membership/position on every request; training writes and War Prep leadership/config writes require Leader/Co-leader, leadership chat is Leader/Co-leader only, moderation has its own server policy.
- Standard Member Preview does not weaken any of those server checks.
