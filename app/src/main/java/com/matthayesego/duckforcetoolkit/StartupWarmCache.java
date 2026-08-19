package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Small process cache populated by the startup warmup and high-value screens.
 * Banking/war/chat data remains process-local. Notices may survive a process restart for up to one
 * hour so the UI has a safe fallback while a cold-start refresh is running.
 */
public final class StartupWarmCache {
    private static final String PREFS = "tornfca_warm_cache_v1";
    private static final long NOTICE_PERSIST_MS = 60L * 60L * 1000L;

    private static AuthSession session;
    private static int sessionFactionId;
    private static JSONObject banking;
    private static long bankingAtMs;
    private static int bankingFactionId;
    private static int bankingPlayerId;
    private static JSONObject war;
    private static long warAtMs;
    private static int warFactionId;
    private static JSONArray notices;
    private static long noticesAtMs;
    private static int noticesFactionId;
    private static final Map<String, JSONArray> chatByKey = new HashMap<>();
    private static final Map<String, Long> chatAtByKey = new HashMap<>();

    private StartupWarmCache() {}

    public static synchronized void clearMemory() {
        session = null; sessionFactionId = 0;
        banking = null; bankingAtMs = 0L; bankingFactionId = 0; bankingPlayerId = 0;
        war = null; warAtMs = 0L; warFactionId = 0;
        notices = null; noticesAtMs = 0L; noticesFactionId = 0;
        chatByKey.clear(); chatAtByKey.clear();
    }

    public static synchronized void putSession(AuthSession value) { session = value; sessionFactionId = value == null ? 0 : value.factionId; }
    public static synchronized AuthSession session(int factionId) { return session != null && sessionFactionId == factionId ? session : null; }

    public static synchronized void putBanking(int factionId, int playerId, JSONObject value) {
        if (value == null || factionId <= 0 || playerId <= 0) return;
        banking = copy(value); bankingAtMs = System.currentTimeMillis(); bankingFactionId = factionId; bankingPlayerId = playerId;
    }

    public static synchronized JSONObject banking(int factionId, int playerId, long maxAgeMs) {
        if (banking == null || bankingFactionId != factionId || bankingPlayerId != playerId) return null;
        if (maxAgeMs > 0L && System.currentTimeMillis() - bankingAtMs > maxAgeMs) return null;
        return copy(banking);
    }

    public static synchronized long bankingAgeMs(int factionId, int playerId) {
        if (banking == null || bankingFactionId != factionId || bankingPlayerId != playerId || bankingAtMs <= 0L) return -1L;
        return Math.max(0L, System.currentTimeMillis() - bankingAtMs);
    }

    public static synchronized void putWar(int factionId, JSONObject value) {
        if (value == null || factionId <= 0) return;
        war = copy(value); warAtMs = System.currentTimeMillis(); warFactionId = factionId;
    }

    public static synchronized JSONObject war(int factionId, long maxAgeMs) {
        if (war == null || warFactionId != factionId) return null;
        if (maxAgeMs > 0L && System.currentTimeMillis() - warAtMs > maxAgeMs) return null;
        return copy(war);
    }

    public static synchronized long warAgeMs(int factionId) {
        if (war == null || warFactionId != factionId || warAtMs <= 0L) return -1L;
        return Math.max(0L, System.currentTimeMillis() - warAtMs);
    }

    public static void putNotices(Context context, int factionId, JSONArray value) {
        if (value == null || factionId <= 0) return;
        JSONArray safe = copy(value); long now = System.currentTimeMillis();
        synchronized (StartupWarmCache.class) { notices = safe; noticesAtMs = now; noticesFactionId = factionId; }
        if (context != null) {
            context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putInt("notice_faction_id", factionId)
                    .putLong("notice_at", now)
                    .putString("notices_json", safe.toString())
                    .apply();
        }
        FactionAnnouncementOverlay.refreshVisible();
    }

    public static JSONArray notices(Context context, int factionId, long maxAgeMs) {
        synchronized (StartupWarmCache.class) {
            if (notices != null && noticesFactionId == factionId
                    && (maxAgeMs <= 0L || System.currentTimeMillis() - noticesAtMs <= maxAgeMs)) return copy(notices);
        }
        if (context == null || factionId <= 0) return null;
        SharedPreferences p = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (p.getInt("notice_faction_id", 0) != factionId) return null;
        long at = p.getLong("notice_at", 0L);
        long allowed = maxAgeMs > 0L ? Math.min(maxAgeMs, NOTICE_PERSIST_MS) : NOTICE_PERSIST_MS;
        if (at <= 0L || System.currentTimeMillis() - at > allowed) return null;
        try {
            JSONArray loaded = new JSONArray(p.getString("notices_json", "[]"));
            synchronized (StartupWarmCache.class) { notices = copy(loaded); noticesAtMs = at; noticesFactionId = factionId; }
            return loaded;
        } catch (Exception ignored) { return null; }
    }

    public static long noticesAgeMs(Context context, int factionId) {
        synchronized (StartupWarmCache.class) {
            if (notices != null && noticesFactionId == factionId && noticesAtMs > 0L)
                return Math.max(0L, System.currentTimeMillis() - noticesAtMs);
        }
        if (context == null || factionId <= 0) return -1L;
        SharedPreferences p = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (p.getInt("notice_faction_id", 0) != factionId) return -1L;
        long at = p.getLong("notice_at", 0L);
        if (at <= 0L || System.currentTimeMillis() - at > NOTICE_PERSIST_MS) return -1L;
        return Math.max(0L, System.currentTimeMillis() - at);
    }

    public static synchronized void putChat(int factionId, String channel, JSONArray value) {
        if (factionId <= 0 || value == null) return;
        String key = chatKey(factionId, channel);
        chatByKey.put(key, copy(value));
        chatAtByKey.put(key, System.currentTimeMillis());
    }

    public static synchronized JSONArray chat(int factionId, String channel, long maxAgeMs) {
        String key = chatKey(factionId, channel);
        JSONArray value = chatByKey.get(key);
        Long at = chatAtByKey.get(key);
        if (value == null || at == null) return null;
        if (maxAgeMs > 0L && System.currentTimeMillis() - at > maxAgeMs) return null;
        return copy(value);
    }

    public static synchronized long chatAgeMs(int factionId, String channel) {
        Long at = chatAtByKey.get(chatKey(factionId, channel));
        return at == null || at <= 0L ? -1L : Math.max(0L, System.currentTimeMillis() - at);
    }

    public static JSONObject latestVisibleNotice(Context context, int factionId) {
        JSONArray rows = notices(context, factionId, NOTICE_PERSIST_MS); if (rows == null) return null;
        long now = System.currentTimeMillis() / 1000L;
        String dismissed = context == null ? "" : context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("dismissed_notice_id", "");
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i); if (row == null) continue;
            long expires = row.optLong("expires_at", 0L); if (expires > 0L && expires <= now) continue;
            String id = row.optString("id", ""); if (!id.isEmpty() && id.equals(dismissed)) continue;
            return copy(row);
        }
        return null;
    }

    public static void dismissNotice(Context context, String id) {
        if (context == null || id == null || id.isBlank()) return;
        context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("dismissed_notice_id", id).apply();
        FactionAnnouncementOverlay.refreshVisible();
    }

    private static String chatKey(int factionId, String channel) {
        String safe = channel == null || channel.isBlank() ? "general" : channel.trim().toLowerCase(java.util.Locale.US);
        return factionId + ":" + safe;
    }

    private static JSONObject copy(JSONObject value) { if (value == null) return null; try { return new JSONObject(value.toString()); } catch (Exception e) { return null; } }
    private static JSONArray copy(JSONArray value) { if (value == null) return null; try { return new JSONArray(value.toString()); } catch (Exception e) { return null; } }
}
