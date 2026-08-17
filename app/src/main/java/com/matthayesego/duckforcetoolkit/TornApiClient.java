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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared Torn API boundary.
 *
 * TornFCA deliberately stays below Torn's user-wide request ceiling. A small token bucket allows
 * a login/navigation burst while keeping sustained direct Android traffic at roughly 30/minute.
 * Safe responses are cached and only transient/rate-limit failures are backed off.
 */
public final class TornApiClient {
    private static final String BASE = "https://api.torn.com/v2";
    private static final String USER_AGENT = "TornFCA/" + TornFcaBrand.VERSION + " Android";
    private static final Pattern API_KEY = Pattern.compile("^[A-Za-z0-9]{16}$");
    private static final Pattern QUERY_KEY = Pattern.compile("([?&])key=[^&]*", Pattern.CASE_INSENSITIVE);

    // Three requests may start promptly for an interactive login. Tokens then refill at one every
    // two seconds, preserving the same ~30/minute sustained TornFCA ceiling.
    private static final long TOKEN_REFILL_MS = 2000L;
    private static final double TOKEN_CAPACITY = 3.0;
    private static double requestTokens = TOKEN_CAPACITY;
    private static long tokenRefillAtMs = System.currentTimeMillis();
    private static final long SESSION_CACHE_MS = 30L * 60L * 1000L;

    private static final ConcurrentHashMap<String, CacheEntry> CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, SessionEntry> SESSION_CACHE = new ConcurrentHashMap<>();

    private TornApiClient() {}

    public static AuthSession authenticate(String key) throws IOException {
        validateKey(key);

        // key/info and public identity are independent. Running them together removes the former
        // artificial two-second login waterfall while the token bucket still bounds total traffic.
        FutureTask<JSONObject> keyInfoTask = new FutureTask<>(() -> getJson("/key/info", key));
        FutureTask<JSONObject> identityTask = new FutureTask<>(() -> getJson("/user?selections=profile,faction", key));
        Thread keyThread = new Thread(keyInfoTask, "TornFCA-KeyInfo");
        Thread identityThread = new Thread(identityTask, "TornFCA-Identity");
        keyThread.start();identityThread.start();

        JSONObject keyInfoRoot = await(keyInfoTask);
        JSONObject identity = await(identityTask);
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

        JSONObject profile = identity.optJSONObject("profile");
        String playerName = profile == null ? "Unknown" : profile.optString("name", "Unknown");

        JSONObject faction = identity.optJSONObject("faction");
        if (faction == null) throw new IOException("This Torn account is not currently in a faction.");
        int factionId = faction.optInt("id", 0);
        String factionName = faction.optString("name", "");
        String position = faction.optString("position", "");
        if (factionId <= 0 || factionName.isEmpty()) throw new IOException("Unable to verify the Torn faction.");

        // Faction identity is safe to alias. Do not alias /user/profile here: the generic user
        // selection and dedicated profile endpoint have changed independently in Torn API history,
        // and the avatar loader deliberately reads the dedicated endpoint.
        try {
            JSONObject factionAlias = new JSONObject();factionAlias.put("faction", faction);
            cachePath("/user/faction", key, factionAlias, 10L * 60L * 1000L);
        } catch (Exception ignored) {}

        JSONArray positions = new JSONArray();
        JSONArray abilities = new JSONArray();
        boolean permissionsResolved = false;
        boolean factionApiAccess = access != null && access.optBoolean("faction", false);
        AccessTier tier = AccessTier.GREEN;

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
                // key/info remains a useful hint. Individual restricted endpoints are authoritative
                // when a Leadership tool actually requests their data.
            }
        }

        if (AccessPolicy.isLeaderPosition(position) && !permissionsResolved) permissionsResolved = true;

        AuthSession session = new AuthSession(playerId, playerName, factionId, factionName, position,
                factionApiAccess, tier, positions, abilities, permissionsResolved);
        SESSION_CACHE.put(sessionKey(key), new SessionEntry(session, System.currentTimeMillis() + SESSION_CACHE_MS));
        return session;
    }

    private static JSONObject await(FutureTask<JSONObject> task) throws IOException {
        try {return task.get();}
        catch (InterruptedException e) {Thread.currentThread().interrupt();throw new IOException("Torn login verification was interrupted.");}
        catch (ExecutionException e) {Throwable cause=e.getCause();if(cause instanceof IOException)throw(IOException)cause;throw new IOException(cause==null?"Torn login verification failed.":cause.getMessage());}
    }

    /** Returns the already verified in-process session so feature navigation does not re-authenticate. */
    public static AuthSession cachedSession(String key) {
        if (key == null || key.trim().isEmpty()) return null;
        SessionEntry entry = SESSION_CACHE.get(sessionKey(key));
        if (entry == null) return null;
        if (entry.expiresAtMs <= System.currentTimeMillis()) {
            SESSION_CACHE.remove(sessionKey(key));
            return null;
        }
        return entry.session;
    }

    public static JSONObject getJson(String path, String key) throws IOException {
        validateKey(key);
        String joiner = path.contains("?") ? "&" : "?";
        return getJsonAbsolute(BASE + path + joiner + "key="
                + java.net.URLEncoder.encode(key, StandardCharsets.UTF_8.name()), key);
    }

    /** Requests may overlap in flight; the token bucket bounds aggregate request starts. */
    public static JSONObject getJsonAbsolute(String absoluteUrl, String key) throws IOException {
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

    public static void clearMemoryCache(){ CACHE.clear();SESSION_CACHE.clear(); }

    public static void validateKey(String key) throws IOException {
        if (key == null || !API_KEY.matcher(key.trim()).matches()) {
            throw new IOException("Torn API keys must be exactly 16 letters/numbers.");
        }
    }

    private static String canonicalizeTornUrl(String absoluteUrl, String key) throws IOException {
        if (absoluteUrl == null) throw new IOException("Refusing non-Torn API URL.");
        URL parsed;
        try{parsed=new URL(absoluteUrl);}catch(Exception e){throw new IOException("Refusing invalid Torn API URL.");}
        if(!"https".equalsIgnoreCase(parsed.getProtocol())||!"api.torn.com".equalsIgnoreCase(parsed.getHost())||!parsed.getPath().startsWith("/v2"))throw new IOException("Refusing non-Torn API URL.");
        String encoded = java.net.URLEncoder.encode(key.trim(), StandardCharsets.UTF_8.name());
        Matcher matcher = QUERY_KEY.matcher(absoluteUrl);
        if (matcher.find()) return matcher.replaceAll("$1key=" + encoded);
        return absoluteUrl + (absoluteUrl.contains("?") ? "&" : "?") + "key=" + encoded;
    }

    private static boolean isTransientTornError(int code) { return code == 5 || code == 17; }
    private static long backoffMs(int tornCode, int attempt) { if (tornCode == 5) return (attempt + 1L) * 10000L;return (attempt + 1L) * 6000L; }

    private static synchronized void waitForRequestSlot() {
        while (true) {
            long now=System.currentTimeMillis();
            long elapsed=Math.max(0L,now-tokenRefillAtMs);
            if(elapsed>0L){requestTokens=Math.min(TOKEN_CAPACITY,requestTokens+(double)elapsed/(double)TOKEN_REFILL_MS);tokenRefillAtMs=now;}
            if(requestTokens>=1.0){requestTokens-=1.0;return;}
            long wait=Math.max(25L,(long)Math.ceil((1.0-requestTokens)*TOKEN_REFILL_MS));
            sleepQuietly(wait);
        }
    }

    private static long ttlFor(String url) {
        if (url.contains("/key/info")) return 10L * 60L * 1000L;
        if (url.contains("/user?selections=profile,faction")) return 10L * 60L * 1000L;
        if (url.contains("/user/basic")) return 10L * 60L * 1000L;
        if (url.contains("/user/profile")) return 10L * 60L * 1000L;
        if (url.contains("/user/faction")) return 10L * 60L * 1000L;
        if (url.contains("/faction/positions")) return 10L * 60L * 1000L;
        if (url.contains("/rankedwarreport")) return 24L * 60L * 60L * 1000L;
        if (url.contains("/rankedwars")) return 2L * 60L * 1000L;
        if (url.contains("/faction/attacks") || url.contains("/user/attacks")) {
            long to = queryLong(url, "to");
            long current = System.currentTimeMillis() / 1000L;
            return to > 0 && to < current - 300L ? 30L * 60L * 1000L : 20L * 1000L;
        }
        if (url.contains("/faction/news")) return 2L * 60L * 1000L;
        if (url.contains("/faction/crimes")) return 30L * 1000L;
        if (url.contains("/user/organizedcrime")) return 30L * 1000L;
        if (url.contains("/faction/balance")) return 20L * 1000L;
        if (url.contains("/faction/wars")) return 45L * 1000L;
        if (url.contains("/faction/chain")) return 30L * 1000L;
        if (url.contains("/members")) return 90L * 1000L;
        return 20L * 1000L;
    }

    private static void cachePath(String path, String key, JSONObject json, long ttl) {
        try {
            String joiner = path.contains("?") ? "&" : "?";
            String url = canonicalizeTornUrl(BASE + path + joiner + "key="
                    + java.net.URLEncoder.encode(key, StandardCharsets.UTF_8.name()), key);
            CACHE.put(cacheKey(url, key), new CacheEntry(json.toString(), System.currentTimeMillis() + ttl));
        } catch (Exception ignored) {}
    }

    private static long queryLong(String url, String name) {
        try {
            int q = url.indexOf('?');if (q < 0) return 0L;
            String[] parts = url.substring(q + 1).split("&");
            for (String part : parts) {
                int eq = part.indexOf('=');String k = eq < 0 ? part : part.substring(0, eq);
                if (!name.equals(URLDecoder.decode(k, StandardCharsets.UTF_8.name()))) continue;
                String v = eq < 0 ? "" : part.substring(eq + 1);return Long.parseLong(URLDecoder.decode(v, StandardCharsets.UTF_8.name()));
            }
        } catch (Exception ignored) {}
        return 0L;
    }

    private static String keyFingerprint(String key){
        if(key==null)return"";
        try{java.security.MessageDigest d=java.security.MessageDigest.getInstance("SHA-256");byte[] bytes=d.digest(key.trim().getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte v:bytes)b.append(String.format(java.util.Locale.US,"%02x",v&0xff));return b.toString();}
        catch(Exception e){return Integer.toHexString(key.trim().hashCode());}
    }
    private static String sessionKey(String key){return keyFingerprint(key);}
    private static String cacheKey(String url, String key) {String sanitized = url.replaceAll("([?&])key=[^&]*", "$1key=*");return keyFingerprint(key) + "|" + sanitized;}
    private static void trimCache() {if (CACHE.size() <= 160) return;long now = System.currentTimeMillis();CACHE.entrySet().removeIf(e -> e.getValue().expiresAtMs <= now);if (CACHE.size() > 200) CACHE.clear();}
    private static void sleepQuietly(long ms) {try { Thread.sleep(Math.max(0L, ms)); }catch (InterruptedException e) { Thread.currentThread().interrupt(); }}
    private static String readAll(InputStream input) throws IOException {try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {byte[] buffer=new byte[4096];int n;while ((n = input.read(buffer)) >= 0) out.write(buffer, 0, n);return out.toString(StandardCharsets.UTF_8.name());}}
    private static final class CacheEntry {final String body;final long expiresAtMs;CacheEntry(String body,long expiresAtMs){this.body=body;this.expiresAtMs=expiresAtMs;}}
    private static final class SessionEntry {final AuthSession session;final long expiresAtMs;SessionEntry(AuthSession session,long expiresAtMs){this.session=session;this.expiresAtMs=expiresAtMs;}}
    private static final class PermanentApiException extends IOException {PermanentApiException(String message){super(message);}}
}
