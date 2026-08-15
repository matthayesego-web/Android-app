package com.matthayesego.duckforcetoolkit;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class TornApiClient {
    private static final String BASE = "https://api.torn.com/v2";
    private static final String USER_AGENT = "DuckForceCompanion/0.6.0 Android";

    private TornApiClient() {}

    public static AuthSession authenticate(String key) throws IOException {
        JSONObject keyInfoRoot = getJson("/key/info", key);
        JSONObject keyInfo = keyInfoRoot.optJSONObject("info");
        if (keyInfo == null) throw new IOException("Torn key information was unavailable.");
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
        boolean factionApiAccess = false;
        AccessTier tier = AccessTier.GREEN;

        try {
            JSONObject positionsResponse = getJson("/faction/positions", key);
            JSONArray fetched = positionsResponse.optJSONArray("positions");
            if (fetched != null) {
                positions = fetched;
                factionApiAccess = true;
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
            // Members without Faction API Access cannot read /faction/positions.
            // The shared backend can resolve their position against a leader-synced cache.
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

    public static JSONObject getJsonAbsolute(String absoluteUrl, String key) throws IOException {
        String urlValue = absoluteUrl;
        if (!urlValue.startsWith(BASE)) throw new IOException("Refusing non-Torn pagination URL.");
        if (!urlValue.contains("key=")) {
            String joiner = urlValue.contains("?") ? "&" : "?";
            urlValue += joiner + "key=" + java.net.URLEncoder.encode(key, StandardCharsets.UTF_8.name());
        }
        URL url = new URL(urlValue);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
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
            if (error != null) throw new IOException(error.optString("error", "Torn API error " + error.optInt("code")));
            if (code < 200 || code >= 300) throw new IOException("Torn API request failed (HTTP " + code + ").");
            return json;
        } finally { connection.disconnect(); }
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

    private static String readAll(InputStream input) throws IOException {
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) >= 0) out.write(buffer, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}
