package com.matthayesego.duckforcetoolkit;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared Torn API boundary.
 *
 * TornFCA deliberately stays well below Torn's user-wide request ceiling. Requests are serialized,
 * spaced, cached where safe, and error 5 is backed off/retried rather than hammered.
 */
public final class TornApiClient {
    private static final String BASE = "https://api.torn.com/v2";
    private static final String USER_AGENT = "TornFCA/0.9.11 Android";

    // Torn documents 100 requests/minute per user across all keys. TornFCA caps itself at 40/minute
    // to leave generous headroom for Torn itself and other tools the player may be using.
    private static final long MIN_REQUEST_SPACING_MS = 1500L;
    private static long nextRequestAtMs = 0L;

    private static final ConcurrentHashMap<String, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private TornApiClient() {}

    public static AuthSession authenticate(String key) throws IOException {
        JSONObject keyInfoRoot = getJson("/key/info", key);
        JSONObject keyInfo = keyInfoRoot.optJSONObject("info");
        if (keyInfo == null) throw new IOException("Torn key information was unavailable.");

        JSONObject access = keyInfo.optJSONObject("access");
        int accessLevel = access == null ? 0 : access.optInt("level", 0);
        String accessType = access == null ? "" : access.optString("type", "");
        boolean customKey = accessType.toLowerCase().contains("custom");
        if (!customKey && accessLevel < 3) {
            throw new IOException("TornFCA requires a Limited Access key or higher. Full Access is not required.");
        }

        JSONObject keyUser = keyInfo.optJSONObject("user");
        if (keyUser == null) throw new IOException("Torn key owner information was unavailable.");
        int playerId = keyUser.optInt("id", 0);
        if (playerId <= 0) throw new IOException("Unable to determine the Torn player ID.");

        JSONObject basicRoot = getJson("/user/basic", key);
        JSONObject basic = basicRoot.optJSONObject("profile");
        String playerName = basic == null ? "Unknown" : basic.optString("name", "Unknown");

        JSONObject factionResponse = getJson("/user/faction", key);
        JSONObject faction = factionResponse.optJSONObject("faction");
        if (faction == null) throw new IOException("This Torn account is not currently in a faction.");
        int factionId = faction.optInt("id", 0);
        String factionName = faction.optString("name", "");
        String position = faction.optString("position", "");
        if (factionId <= 0 || factionName.isEmpty()) throw new IOException("Unable to verify the Torn faction.");

        JSONArray positions = new JSONArray();
        JSONArray abilities = new JSONArray();
        boolean permissionsResolved = false;
        boolean factionApiAccess = access != null && access.optBoolean("faction", false);
        AccessTier tier = AccessTier.GREEN;

        // Do not make a known-to-fail /faction/positions request for ordinary members. key/info already
        // tells us whether this key currently has the in-game Faction API Access permission.
        if (factionApiAccess) {
            try {
                JSONObject positionsResponse = getJson("/faction/positions", key);
                JSONArray fetched = positionsResponse.optJSONArray("positions");
                if (fetched != null) {
                    positions = fetched;
                    for (int i = 0; i < fetched.length(); i++) {
                        JSONObject pos = fetched.optJSONObject(i);
                        if (pos != null && position.equalsIgnoreCase(pos.optString("name", ""))) {
                            JSONArray found = pos.optJSONArray("abilities");
                            abilities = found == null ? new JSONArray() : found;
                            permissionsResolved = true;
                            tier = AccessPolicy.tierForAbilities(abilities);
                            break;
                        }
                    }
                }
            } catch (IOException ignored) {
                // A temporary Torn failure must not be converted into "no Faction API Access".
                // key/info remains the source of truth for the boolean permission.
            }
        }

        if (AccessPolicy.isLeaderPosition(position) && !permissionsResolved) permissionsResolved = true;

        return new AuthSession(playerId, playerName, factionId, factionName, position,
                factionApiAccess, tier, positions, abilities, permissionsResolved);
    }

    public static JSONObject getJson(String path, String key) throws IOException {
        String joiner = path.contains("?") ? "&" : "?";
        return getJsonAbsolute(BASE + path + joiner + "key="
                + java.net.URLEncoder.encode(key, StandardCharsets.UTF_8.name()), key);
    }

    /** Serializes Torn requests across the whole app so parallel screens cannot burst the API. */
    public static synchronized JSONObject getJsonAbsolute(String absoluteUrl, String key) throws IOException {
        String urlValue = absoluteUrl;
        if (!urlValue.startsWith(BASE)) throw new IOException("Refusing non-Torn pagination URL.");
        if (!urlValue.contains("key=")) {
            String joiner = urlValue.contains("?") ? "&" : "?";
            urlValue += joiner + "key=" + java.net.URLEncoder.encode(key, StandardCharsets.UTF_8.name());
        }

        long ttl = ttlFor(urlValue);
        String cacheKey = cacheKey(urlValue, key);
        CacheEntry cached = CACHE.get(cacheKey);
        long now = System.currentTimeMillis();
        if (cached != null && cached.expiresAtMs > now) {
            try { return new JSONObject(cached.body); }
            catch (Exception ignored) { CACHE.remove(cacheKey); }
        }

        IOException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            waitForRequestSlot();
            HttpURLConnection connection = (HttpURLConnection) new URL(urlValue).openConnection();
            try {
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setUseCaches(false);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("User-Agent", USER_AGENT);
                int code = connection.getResponseCode();
                InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
                String body = stream == null ? "" : readAll(stream);
                JSONObject json;
                try { json = new JSONObject(body); }
                catch (Exception e) { throw new IOException("Torn returned an unreadable response."); }

                JSONObject error = json.optJSONObject("error");
                if (error != null) {
                    int tornCode = error.optInt("code", 0);
                    String message = error.optString("error", "Torn API error " + tornCode);
                    if (tornCode == 5 && attempt < 2) {
                        sleepQuietly((attempt + 1L) * 8000L);
                        last = new IOException("Torn rate limit reached; retrying automatically.");
                        continue;
                    }
                    throw new IOException(tornCode == 5 ? "Torn rate limit reached. Please wait a moment and retry." : message);
                }
                if (code < 200 || code >= 300) throw new IOException("Torn API request failed (HTTP " + code + ").");

                if (ttl > 0L) CACHE.put(cacheKey, new CacheEntry(body, System.currentTimeMillis() + ttl));
                trimCache();
                return json;
            } catch (IOException e) {
                last = e;
            } finally { connection.disconnect(); }
        }
        throw last == null ? new IOException("Torn API request failed.") : last;
    }

    public static JSONArray getPagedArray(String path, String key, String arrayName, int maxPages) throws IOException {
        JSONArray out = new JSONArray();
        String next = BASE + path + (path.contains("?") ? "&" : "?") + "key="
                + java.net.URLEncoder.encode(key, StandardCharsets.UTF_8.name());
        int pages = 0;
        int pageLimit = Math.max(1, maxPages);
        if (path.startsWith("/faction/news")) pageLimit = Math.min(pageLimit, DeveloperSettings.runtimeActivityMaxPages());
        while (next != null && !next.isEmpty() && pages < pageLimit) {
            JSONObject root = getJsonAbsolute(next, key);
            JSONArray rows = root.optJSONArray(arrayName);
            if (rows != null) for (int i = 0; i < rows.length(); i++) out.put(rows.opt(i));
            JSONObject metadata = root.optJSONObject("_metadata");
            JSONObject links = metadata == null ? null : metadata.optJSONObject("links");
            next = links == null || links.isNull("next") ? null : links.optString("next", null);
            pages++;
        }
        return out;
    }

    public static void clearMemoryCache(){ CACHE.clear(); }

    private static void waitForRequestSlot() {
        long now = System.currentTimeMillis();
        long wait = Math.max(0L, nextRequestAtMs - now);
        if (wait > 0L) sleepQuietly(wait);
        nextRequestAtMs = Math.max(System.currentTimeMillis(), nextRequestAtMs) + MIN_REQUEST_SPACING_MS;
    }

    private static long ttlFor(String url) {
        if (url.contains("/key/info")) return 10L * 60L * 1000L;
        if (url.contains("/user/basic")) return 10L * 60L * 1000L;
        if (url.contains("/user/faction")) return 5L * 60L * 1000L;
        if (url.contains("/faction/positions")) return 5L * 60L * 1000L;
        if (url.contains("/rankedwarreport")) return 24L * 60L * 60L * 1000L;
        if (url.contains("/rankedwars")) return 2L * 60L * 1000L;
        if (url.contains("/faction/attacks")) return 30L * 60L * 1000L;
        if (url.contains("/faction/wars")) return 20L * 1000L;
        if (url.contains("/faction/chain")) return 15L * 1000L;
        if (url.contains("/members")) return 60L * 1000L;
        return 10L * 1000L;
    }

    private static String cacheKey(String url, String key) {
        String sanitized = url.replaceAll("([?&])key=[^&]*", "$1key=*");
        return Integer.toHexString(key == null ? 0 : key.hashCode()) + "|" + sanitized;
    }

    private static void trimCache() {
        if (CACHE.size() <= 160) return;
        long now = System.currentTimeMillis();
        CACHE.entrySet().removeIf(e -> e.getValue().expiresAtMs <= now);
        if (CACHE.size() > 200) CACHE.clear();
    }

    private static void sleepQuietly(long ms) {
        try { Thread.sleep(Math.max(0L, ms)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static String readAll(InputStream input) throws IOException {
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) >= 0) out.write(buffer, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static final class CacheEntry {
        final String body;
        final long expiresAtMs;
        CacheEntry(String body,long expiresAtMs){this.body=body;this.expiresAtMs=expiresAtMs;}
    }
}
