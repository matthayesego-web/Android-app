package com.matthayesego.duckforcetoolkit;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class CompanionBackendClient {
    private static final String BACKEND_URL = "###DUCKFORCE-BACKEND-URL###";
    private static final String USER_AGENT = "DuckForceCompanion/0.4.5 Android";

    private CompanionBackendClient() {}

    public static boolean isConfigured() {
        return BACKEND_URL.startsWith("https://") && !BACKEND_URL.contains("###");
    }

    public static AuthSession resolvePermissions(AuthSession session, String apiKey) {
        if (session == null || !isConfigured()) return session;
        try {
            JSONObject body = new JSONObject();
            body.put("action", "config");
            body.put("apiKey", apiKey);
            JSONObject response = post(body);
            JSONArray permissions = response.optJSONArray("permissions");
            if (permissions != null) return session.withPermissions(permissions);
        } catch (Exception ignored) {}
        return session;
    }

    public static JSONArray getNotices(String apiKey) throws IOException {
        if (!isConfigured()) return new JSONArray();
        try {
            JSONObject body = new JSONObject();
            body.put("action", "notices");
            body.put("apiKey", apiKey);
            JSONObject response = post(body);
            JSONArray notices = response.optJSONArray("notices");
            return notices == null ? new JSONArray() : notices;
        } catch (IOException e) { throw e; }
        catch (Exception e) { throw new IOException(e.getMessage() == null ? "Unable to read faction notices." : e.getMessage()); }
    }

    public static void publishNotice(String apiKey, String title, String message, long expiresAt) throws IOException {
        if (!isConfigured()) throw new IOException("Shared faction backend is not configured yet.");
        try {
            JSONObject body = new JSONObject();
            body.put("action", "post_notice");
            body.put("apiKey", apiKey);
            body.put("title", title == null ? "" : title.trim());
            body.put("message", message == null ? "" : message.trim());
            body.put("expires_at", expiresAt);
            post(body);
        } catch (IOException e) { throw e; }
        catch (Exception e) { throw new IOException(e.getMessage() == null ? "Unable to publish faction notice." : e.getMessage()); }
    }

    private static JSONObject post(JSONObject body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(BACKEND_URL).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "text/plain;charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream out = connection.getOutputStream()) { out.write(payload); }
            int code = connection.getResponseCode();
            InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String raw = stream == null ? "" : readAll(stream);
            JSONObject response;
            try { response = new JSONObject(raw); }
            catch (Exception e) { throw new IOException("Faction backend returned an unreadable response."); }
            if (!response.optBoolean("ok", false)) throw new IOException(response.optString("error", "Faction backend request failed."));
            if (code < 200 || code >= 300) throw new IOException("Faction backend HTTP " + code + ".");
            return response;
        } finally { connection.disconnect(); }
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
