package com.matthayesego.duckforcetoolkit;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Tiny process-local cache for faction member snapshots used by quick-access tools.
 * Keeps tool-to-tool navigation fast without persisting faction data to disk.
 */
public final class FactionMemberCache {
    private static final long TTL_MS = 20_000L;

    private static int factionId;
    private static long storedAtMs;
    private static String payload;

    private FactionMemberCache() {}

    public static synchronized JSONArray load(int requestedFactionId) {
        if (requestedFactionId <= 0 || requestedFactionId != factionId || payload == null) return null;
        if (System.currentTimeMillis() - storedAtMs > TTL_MS) {
            clear();
            return null;
        }
        try {
            return new JSONArray(payload);
        } catch (JSONException ignored) {
            clear();
            return null;
        }
    }

    public static synchronized void save(int requestedFactionId, JSONArray members) {
        if (requestedFactionId <= 0 || members == null) return;
        factionId = requestedFactionId;
        storedAtMs = System.currentTimeMillis();
        payload = members.toString();
    }

    public static synchronized long ageSeconds(int requestedFactionId) {
        if (requestedFactionId <= 0 || requestedFactionId != factionId || payload == null) return -1L;
        return Math.max(0L, (System.currentTimeMillis() - storedAtMs) / 1000L);
    }

    public static synchronized void clear() {
        factionId = 0;
        storedAtMs = 0L;
        payload = null;
    }
}
