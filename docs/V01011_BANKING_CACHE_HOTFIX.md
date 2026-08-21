# TornFCA Beta v0.10.11 — Banking + cache hotfix

- Fixes the Banking Companion runtime crash seen after v0.10.10 warm-start changes.
- Banking renders a single safe cached snapshot first, then refreshes queue and balances independently.
- Malformed/stale banking rows are skipped instead of crashing the Activity.
- Startup warmup runs once per Android process; backgrounding/reopening the running app does not repeat the full warm cycle.
- Banking warm cache is accepted for up to 45 minutes while the process lives; live queue refresh remains available.
- Persisted notice fallback is limited to one hour.
- Main/live is intentionally unchanged.
