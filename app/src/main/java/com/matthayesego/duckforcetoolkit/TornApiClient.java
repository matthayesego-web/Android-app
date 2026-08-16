package com.matthayesego.duckforcetoolkit;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared Torn API boundary.
 *
 * TornFCA deliberately stays far below Torn's user-wide request ceiling. Requests are serialized,
 * spaced, cached where safe, and only transient/rate-limit failures are backed off. Permanent
 * invalid-key/access errors are never automatically retried.
 */
public final class TornApiClient {
    private static final String BASE = "https://api.torn.com/v2";
    private static final String USER_AGENT = "TornFCA/0.9.13 Android";
    private static final Pattern API_KEY = Pattern.compile("^[A-Za-z0-9]{16}$");
    private static final Pattern QUERY_KEY = Pattern.compile("([?&])key=[^&]*", Pattern.CASE_INSENSITIVE);

    // Torn documents 100 requests/minute per user across all of that user's keys. TornFCA caps its
    // own Android Torn traffic at 20/minute (one request every 3 seconds), leaving >=80% of the
    // documented ceiling available for TornStats, FFScouter, Torn PDA and other tools.
    private static final long MIN_REQUEST_SPACING_MS = 3000L;
    private static long nextRequestAtMs = 0L;

    private static final ConcurrentHashMap<String, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private TornApiClient() {}

    public static AuthSession authenticate(String key) throws IOException {
        validateKey(key);
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

        // Do not make a known-to-fail /faction/positions request for ordinary members. key/info
        // already tells us whether this key currently has the in-game Faction API Access permission.
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
        validateKey(key);
        String joiner = path.contains("?") ? "&" : "?";
        return getJsonAbsolute(BASE + path + joiner + "key="
                + java.net.URLEncoder.encode(key, StandardCharsets.UTF_8.name()), key);
    }

    /** Serializes Torn requests across the whole app so parallel screens cannot burst the API. */
    public static synchronized JSONObject getJsonAbsolute(String absoluteUrl, String key) throws IOException {
        validateKey(key);
        String urlValue = canonicalizeTornUrl(absoluteUrl, key);

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

                JSONObject json = null;
                try { if (!body.isEmpty()) json = new JSONObject(body); }
                catch (Exception ignored) {}

                if (json != null) {
                    JSONObject error = json.optJSONObject("error");
                    if (error != null) {
                        int tornCode = error.optInt("code", 0);
                        String message = error.optString("error", "Torn API error " + tornCode);
                        if (isTransientTornError(tornCode) && attempt < 2) {
                            sleepQuietly(backoffMs(tornCode, attempt));
                            last = new IOException(message);
                            continue;
                        }
                        throw new PermanentApiException(message);
                    }
                }

                if (code == 429 || code >= 500) {
                    String message = "Torn API request failed (HTTP " + code + ").";
                    if (attempt < 2) {
                        sleepQuietly((attempt + 1L) * 8000L);
                        last = new IOException(message);
                        continue;
                    }
                    throw new IOException(message);
                }
                if (code < 200 || code >= 300) throw new PermanentApiException("Torn API request failed (HTTP " + code + ").");
                if (json == null) throw new IOException("Torn returned an unreadable response.");

                if (ttl > 0L) CACHE.put(cacheKey, new CacheEntry(body, System.currentTimeMillis() + ttl));
                trimCache();
                return json;
            } catch (PermanentApiException e) {
                throw e;
            } catch (IOException e) {
                last = e;
                if (attempt < 2) sleepQuietly((attempt + 1L) * 3000L);
            } finally {
                connection.disconnect();
            }
        }
        throw last == null ? new IOException("Torn API request failed.") : last;
    }

    public static JSONArray getPagedArray(String path, String key, String arrayName, int maxPages) throws IOException {
        validateKey(key);
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

    public static void validateKey(String key) throws IOException {
        if (key == null || !API_KEY.matcher(key.trim()).matches()) {
            throw new IOException("Torn API keys must be exactly 16 letters/numbers.");
        }
    }

    private static String canonicalizeTornUrl(String absoluteUrl, String key) throws IOException {
        if (absoluteUrl == null || !absoluteUrl.startsWith(BASE)) throw new IOException("Refusing non-Torn API URL.");
        String encoded = java.net.URLEncoder.encode(key.trim(), StandardCharsets.UTF_8.name());
        Matcher matcher = QUERY_KEY.matcher(absoluteUrl);
        if (matcher.find()) return matcher.replaceAll("$1key=" + encoded);
        return absoluteUrl + (absoluteUrl.contains("?") ? "&" : "?") + "key=" + encoded;
    }

    private static boolean isTransientTornError(int code) {
        return code == 5 || code == 17;
    }

    private static long backoffMs(int tornCode, int attempt) {
        if (tornCode == 5) return (attempt + 1L) * 10000L;
        return (attempt + 1L) * 6000L;
    }

    private static void waitForRequestSlot() {
        long now = System.currentTimeMillis();
        long wait = Math.max(0L, nextRequestAtMs - now);
        if (wait > 0L) sleepQuietly(wait);
        nextRequestAtMs = Math.max(System.currentTimeMillis(), nextRequestAtMs) + MIN_REQUEST_SPACING_MS;
    }

    private static long ttlFor(String url) {
        if (url.contains("/key/info")) return 10L * 60L * 1000L;
        if (url.contains("/user/basic")) return 10L * 60L * 1000L;
        if (url.contains("/user/profile")) return 10L * 60L * 1000L;
        if (url.contains("/user/faction")) return 5L * 60L * 1000L;
        if (url.contains("/faction/positions")) return 5L * 60L * 1000L;
        if (url.contains("/rankedwarreport")) return 24L * 60L * 60L * 1000L;
        if (url.contains("/rankedwars")) return 2L * 60L * 1000L;
        if (url.contains("/faction/attacks") || url.contains("/user/attacks")) {
            long to = queryLong(url, "to");
            long now = System.currentTimeMillis() / 1000L;
            return to > 0 && to < now - 300L ? 30L * 60L * 1000L : 20L * 1000L;
        }
        if (url.contains("/faction/news")) return 2L * 60L * 1000L;
        if (url.contains("/faction/crimes")) return 30L * 1000L;
        if (url.contains("/user/organizedcrime")) return 30L * 1000L;
        if (url.contains("/faction/balance")) return 15L * 1000L;
        if (url.contains("/faction/wars")) return 20L * 1000L;
        if (url.contains("/faction/chain")) return 15L * 1000L;
        if (url.contains("/members")) return 60L * 1000L;
        return 20L * 1000L;
    }

    private static long queryLong(String url, String name) {
        try {
            int q = url.indexOf('?');
            if (q < 0) return 0L;
            String[] parts = url.substring(q + 1).split("&");
            for (String part : parts) {
                int eq = part.indexOf('=');
                String k = eq < 0 ? part : part.substring(0, eq);
                if (!name.equals(URLDecoder.decode(k, StandardCharsets.UTF_8.name()))) continue;
                String v = eq < 0 ? "" : part.substring(eq + 1);
                return Long.parseLong(URLDecoder.decode(v, StandardCharsets.UTF_8.name()));
            }
        } catch (Exception ignored) {}
        return 0L;
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

    private static final class PermanentApiException extends IOException {
        PermanentApiException(String message){super(message);}
    }
}
