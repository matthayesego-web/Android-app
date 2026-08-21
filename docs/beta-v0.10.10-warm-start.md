# TornFCA Beta v0.10.10 — Warm-start UX candidate

## Performance
- Adds a branded startup warmup gate with early completion and a 12-second safety ceiling.
- Pre-wakes all six Apps Script deployments with unauthenticated health GETs.
- Warms current Torn session, faction notices, banking queue, ranked-war state and community state before Home where available.
- Replaces long per-client UI sleeps with a shared burst-friendly backend request governor.
- Banking and faction notices use stale-while-refresh behavior instead of blocking every screen open on cold network data.

## Banking
- Cached queue renders before network refresh.
- Faction balance lookup no longer blocks the queue from rendering.
- Blank/full-balance requests try to resolve the requesting player's current faction balance at submission time.
- Banker controls include a prominent Torn faction-banking handoff even when balance resolution is unavailable.
- Submits and status changes update the local queue optimistically, with reconciliation in the background.
- Banking notification routing now opens Banking Companion directly.
- Community banking-alert client plumbing is staged for cloud-push activation with the Community backend/Firebase pass.

## Faction notices
- Active faction notices are cached per faction and displayed immediately on the Notices screen.
- The latest undismissed active notice appears as a readable in-app banner across authenticated TornFCA screens.
- The banner survives screen re-renders and opens the full Faction Notices screen when tapped.
- Publishing no longer waits on best-effort community push delivery.
- Leadership authorization is re-verified at the sensitive publish action rather than delaying initial screen display.

## Safety
- Main/live is unchanged.
- Pre-change restore point: `restore/v0.10.9-pre-warmup-ux-2026-08-19`.
- API keys remain local/request-scoped and are not written to shared caches or backend storage.
