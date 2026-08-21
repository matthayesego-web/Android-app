package com.matthayesego.duckforcetoolkit;

/**
 * Shared governor for authenticated TornFCA backend calls.
 *
 * Backend requests often verify the caller against Torn server-side, so they are not "free" from
 * the player's Torn request budget. A four-call burst keeps startup/navigation responsive, while
 * sustained backend request starts settle near 12/minute. Combined with the direct Torn client
 * governor this keeps TornFCA near its conservative per-player request target without forcing a
 * 10-second delay in front of ordinary screen taps.
 */
public final class BackendRequestGovernor {
    private static final long REFILL_MS = 5000L;
    private static final double CAPACITY = 4.0;
    private static double tokens = CAPACITY;
    private static long refillAtMs = System.currentTimeMillis();

    private BackendRequestGovernor() {}

    public static void acquire() {
        synchronized (BackendRequestGovernor.class) {
            while (true) {
                long now = System.currentTimeMillis();
                long elapsed = Math.max(0L, now - refillAtMs);
                if (elapsed > 0L) {
                    tokens = Math.min(CAPACITY, tokens + (double) elapsed / (double) REFILL_MS);
                    refillAtMs = now;
                }
                if (tokens >= 1.0) {
                    tokens -= 1.0;
                    return;
                }
                long wait = Math.max(20L, (long) Math.ceil((1.0 - tokens) * REFILL_MS));
                try {
                    BackendRequestGovernor.class.wait(wait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
