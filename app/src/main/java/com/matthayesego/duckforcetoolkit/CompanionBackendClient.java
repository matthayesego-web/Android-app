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
    private static final String BACKEND_URL = BuildConfig.FACTION_BACKEND_URL == null ? "" : BuildConfig.FACTION_BACKEND_URL.trim();
    private static final String USER_AGENT = "TornFCA/" + TornFcaBrand.VERSION + " Android";

    private CompanionBackendClient() {}

    public static boolean isConfigured() { return BACKEND_URL.startsWith("https://") && !BACKEND_URL.contains("###"); }
    public static String configuredUrl(){ return BACKEND_URL; }

    public static AuthSession resolvePermissions(AuthSession session, String apiKey) {
        if (session == null || !isConfigured()) return session;
        try {JSONObject response = post(request("config", apiKey));JSONArray permissions = response.optJSONArray("permissions");if (permissions != null) return session.withPermissions(permissions);} catch (Exception ignored) {}
        return session;
    }

    public static JSONArray getNotices(String apiKey) throws IOException {if (!isConfigured()) return new JSONArray();JSONObject response = postChecked(request("notices", apiKey), "Unable to read faction notices.");JSONArray notices = response.optJSONArray("notices");return notices == null ? new JSONArray() : notices;}
    public static void publishNotice(String apiKey, String title, String message, long expiresAt) throws IOException {
        if (!isConfigured()) throw new IOException("Shared faction backend is not configured yet.");
        JSONObject body = request("post_notice", apiKey);
        try { body.put("title", title == null ? "" : title.trim());body.put("message", message == null ? "" : message.trim());body.put("expires_at", expiresAt); }
        catch (Exception e) { throw new IOException("Unable to prepare faction notice."); }
        postChecked(body, "Unable to publish faction notice.");
        // The notice itself is authoritative. Cloud delivery is intentionally decoupled so a slow
        // Firebase/community path cannot make the leadership UI look like publishing failed.
        if(CommunityBackendClient.isConfigured()){
            final String pushTitle=title,pushMessage=message,pushKey=apiKey;
            new Thread(()->{try{CommunityBackendClient.publishAnnouncement(pushKey,pushTitle,pushMessage);}catch(Exception ignored){}},"TornFCA-NoticePush").start();
        }
    }
    public static JSONObject getBankingRequests(String apiKey, boolean reconcile) throws IOException {if (!isConfigured()) throw new IOException("Shared faction backend is not configured yet.");JSONObject body = request("banking_list", apiKey);try { body.put("reconcile", reconcile); }catch (Exception ignored) {}return postChecked(body, "Unable to load banking requests.");}
    public static JSONObject submitBankingRequest(String apiKey, String amount, String note) throws IOException {if (!isConfigured()) throw new IOException("Shared faction backend is not configured yet.");JSONObject body = request("banking_submit", apiKey);try {body.put("requested_amount", amount == null ? "" : amount.trim());body.put("note", note == null ? "" : note.trim());}catch (Exception e) {throw new IOException("Unable to prepare banking request.");}return postChecked(body, "Unable to submit banking request.");}
    public static JSONObject updateBankingRequest(String apiKey, String requestId, String status) throws IOException {if (!isConfigured()) throw new IOException("Shared faction backend is not configured yet.");JSONObject body = request("banking_update", apiKey);try {body.put("request_id", requestId == null ? "" : requestId.trim());body.put("status", status == null ? "" : status.trim());}catch (Exception e) {throw new IOException("Unable to prepare banking update.");}return postChecked(body, "Unable to update banking request.");}
    public static JSONObject reconcileBanking(String apiKey) throws IOException {if (!isConfigured()) throw new IOException("Shared faction backend is not configured yet.");return postChecked(request("banking_reconcile", apiKey), "Unable to reconcile banking requests.");}

    private static JSONObject request(String action, String apiKey) {JSONObject body = new JSONObject();try {body.put("action", action);body.put("apiKey", apiKey == null ? "" : apiKey);}catch (Exception ignored) {}return body;}
    private static JSONObject postChecked(JSONObject body, String fallback) throws IOException {try { return post(body); }catch (IOException e) { throw e; }catch (Exception e) { throw new IOException(e.getMessage() == null ? fallback : e.getMessage()); }}

    private static JSONObject post(JSONObject body) throws IOException {
        if(!isConfigured()) throw new IOException("Shared faction backend is not configured yet.");
        String apiKey=body.optString("apiKey","");if(!apiKey.isEmpty())TornApiClient.validateKey(apiKey);
        BackendRequestGovernor.acquire();
        HttpURLConnection connection = (HttpURLConnection) new URL(BACKEND_URL).openConnection();
        try {
            connection.setRequestMethod("POST");connection.setConnectTimeout(10000);connection.setReadTimeout(22000);connection.setUseCaches(false);connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "text/plain;charset=UTF-8");connection.setRequestProperty("Accept", "application/json");connection.setRequestProperty("User-Agent", USER_AGENT);
            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);try (OutputStream out = connection.getOutputStream()) { out.write(payload); }
            int code = connection.getResponseCode();InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();String raw = stream == null ? "" : readAll(stream);
            JSONObject response;try { response = new JSONObject(raw); }catch (Exception e) { throw new IOException("Faction backend returned an unreadable response."); }
            if (!response.optBoolean("ok", false)) throw new IOException(response.optString("error", "Faction backend request failed."));
            if (code < 200 || code >= 300) throw new IOException("Faction backend HTTP " + code + ".");
            return response;
        } finally {connection.disconnect();}
    }

    private static String readAll(InputStream input) throws IOException {try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {byte[] buffer=new byte[4096];int n;while ((n = input.read(buffer)) >= 0) out.write(buffer, 0, n);return out.toString(StandardCharsets.UTF_8.name());}}
}
