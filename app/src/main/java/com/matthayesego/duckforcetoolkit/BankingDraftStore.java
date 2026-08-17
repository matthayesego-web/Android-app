package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Local outage fallback for banking requests. Every new row is tenant-bound by faction_id. */
public final class BankingDraftStore {
    private static final String PREFS = "duckforce_banking_v040";
    private static final String KEY = "requests";
    private static final AtomicBoolean SYNCING = new AtomicBoolean(false);

    private BankingDraftStore() {}

    public static void add(Context context, AuthSession session, String amount, String note) {
        if(context==null||session==null)return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONArray rows = new JSONArray(prefs.getString(KEY, "[]"));
            JSONObject row = new JSONObject();
            long created = System.currentTimeMillis();
            row.put("id", UUID.randomUUID().toString());
            row.put("created", created);
            row.put("playerId", session.playerId);
            row.put("playerName", session.playerName);
            row.put("factionId", session.factionId);
            row.put("factionName", session.factionName);
            row.put("amount", amount == null || amount.trim().isEmpty() ? "FULL BALANCE" : amount.trim());
            row.put("note", note == null ? "" : note.trim());
            row.put("status", "LOCAL FALLBACK");
            rows.put(row);
            prefs.edit().putString(KEY, rows.toString()).apply();
        } catch (Exception ignored) {}
    }

    /** Returns only drafts that belong to the signed-in player + current faction tenant. */
    public static JSONArray all(Context context, AuthSession session) {
        JSONArray rows = read(context);
        JSONArray scoped = scopedRows(rows, session);
        scheduleSync(context.getApplicationContext(), scoped);
        return scoped;
    }

    private static JSONArray read(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            return new JSONArray(prefs.getString(KEY, "[]"));
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    private static JSONArray scopedRows(JSONArray rows,AuthSession session){
        JSONArray out=new JSONArray();if(rows==null||session==null)return out;
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.optJSONObject(i);if(row==null)continue;
            if(row.optInt("playerId",0)!=session.playerId)continue;
            int storedFaction=row.optInt("factionId",0);
            if(storedFaction==session.factionId&&storedFaction>0){out.put(row);continue;}
            // Rows created before TornFCA became multi-faction had no faction_id and could only
            // have been created by the historical Duck Force-restricted build. Preserve access to
            // those drafts only while the same player is in Duck Force, but never auto-sync them.
            if(storedFaction<=0&&"Duck Force".equalsIgnoreCase(session.factionName))out.put(row);
        }
        return out;
    }

    private static void scheduleSync(Context context, JSONArray snapshot) {
        if (!CompanionBackendClient.isConfigured() || snapshot == null || snapshot.length() == 0 || !SYNCING.compareAndSet(false, true)) return;
        new Thread(() -> {
            try {
                String key = new SecureApiKeyStore(context).load();
                if (key == null || key.trim().isEmpty()) return;
                AuthSession current = TornApiClient.cachedSession(key);if(current==null)current=TornApiClient.authenticate(key);
                for (int i = 0; i < snapshot.length(); i++) {
                    JSONObject row = snapshot.optJSONObject(i);
                    if (row == null || row.optInt("playerId", 0) != current.playerId) continue;
                    int rowFaction=row.optInt("factionId",0);
                    if(rowFaction<=0||rowFaction!=current.factionId)continue; // legacy/unscoped rows never auto-sync
                    String amount = row.optString("amount", "FULL BALANCE");
                    if ("FULL BALANCE".equalsIgnoreCase(amount)) amount = "";
                    try {
                        CompanionBackendClient.submitBankingRequest(key, amount, row.optString("note", ""));
                        remove(context, row);
                    } catch (Exception ignored) {
                        break;
                    }
                }
            } catch (Exception ignored) {
            } finally {
                SYNCING.set(false);
            }
        }, "TornFCA-BankingFallbackSync").start();
    }

    public static String fingerprint(JSONObject row) {
        if (row == null) return "";
        String id = row.optString("id", "").trim();
        if (!id.isEmpty()) return "local:" + id;
        return "local-legacy:" + row.optLong("created", 0) + ":" + row.optInt("playerId", 0);
    }

    public static void remove(Context context, JSONObject target) {
        if (target == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONArray rows = new JSONArray(prefs.getString(KEY, "[]"));
            JSONArray keep = new JSONArray();
            String targetFingerprint = fingerprint(target);
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row == null || !targetFingerprint.equals(fingerprint(row))) keep.put(rows.opt(i));
            }
            prefs.edit().putString(KEY, keep.toString()).apply();
        } catch (Exception ignored) {}
    }

    /** Clears only the current player/current faction tenant's visible drafts. */
    public static void clear(Context context,AuthSession session) {
        if(context==null||session==null)return;
        try{
            SharedPreferences prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
            JSONArray rows=new JSONArray(prefs.getString(KEY,"[]"));JSONArray keep=new JSONArray();
            for(int i=0;i<rows.length();i++){
                JSONObject row=rows.optJSONObject(i);if(row==null){keep.put(rows.opt(i));continue;}
                boolean samePlayer=row.optInt("playerId",0)==session.playerId;int storedFaction=row.optInt("factionId",0);
                boolean sameTenant=samePlayer&&storedFaction==session.factionId&&storedFaction>0;
                boolean duckLegacy=samePlayer&&storedFaction<=0&&"Duck Force".equalsIgnoreCase(session.factionName);
                if(!sameTenant&&!duckLegacy)keep.put(row);
            }
            prefs.edit().putString(KEY,keep.toString()).apply();
        }catch(Exception ignored){}
    }
}
