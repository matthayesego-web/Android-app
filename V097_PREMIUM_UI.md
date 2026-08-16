# Duck Force Companion v0.9.7 — Premium UI Pass

This beta release is the first full premium visual shell for Duck Force Companion.

## Presentation
- Removes duplicate top Home / Faction / Leadership tabs.
- Uses bottom navigation as the primary navigation layer.
- Bottom navigation: Home, Faction, War, conditional Leadership, More.
- Leadership appears only when the authenticated faction role/permissions expose leadership features.
- Replaces text-symbol navigation with vector icons.
- Uses a calmer dark luxury palette with restrained semantic accents.

## Home
Home is intentionally a command snapshot rather than a tool directory:
- authenticated Torn avatar/header (with Duck Force fallback)
- current/upcoming ranked-war state
- chain status shortcut
- personal obligations shortcut
- leadership attention when authorized
- compact faction digest

## Faction
Faction contains member-safe faction tools:
- Faction Strength Intel / FFScouter
- My Status
- My OC
- My Participation
- Banking when available

## Leadership
Leadership is exception-first:
- Leadership Attention
- Member Dossier entry point
- War participation
- OC management
- Banking
- Armory Auditor
- Leadership Controls

## Avatar
The header reads the signed-in player's image from Torn API v2 `/user/profile`. If no profile image is returned or the image cannot be loaded, the Duck Force artwork remains as a fallback.

## Developer access
Developer tools remain invisible in normal navigation. Triple-tap the version footer and enter the developer password to open the hidden developer channel. Member Preview remains visual/local only.

## Release policy
- versionCode 22
- versionName 0.9.7
- production application ID remains `com.matthayesego.duckforcetoolkit`
- main remains untouched until a tested release is approved
