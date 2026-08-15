package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;

public final class BankingDraftStore {
    private static final String PREFS = "duckforce_banking_v040";
    private static final String KEY = "requests";

    private BankingDraftStore() {}

    public static void add(Context context, AuthSession session, String amount, String note) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONArray rows = new JSONArray(prefs.getString(KEY, "[]"));
            JSONObject row = new JSONObject();
            long created = System.currentTimeMillis();
            row.put("id", UUID.randomUUID().toString());
            row.put("created", created);
            row.put("playerId", session.playerId);
            row.put("playerName", session.playerName);
            row.put("amount", amount == null || amount.trim().isEmpty() ? "FULL BALANCE" : amount.trim());
            row.put("note", note == null ? "" : note.trim());
            row.put("status", "LOCAL FALLBACK");
            rows.put(row);
            prefs.edit().putString(KEY, rows.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static JSONArray all(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            return new JSONArray(prefs.getString(KEY, "[]"));
        } catch (Exception ignored) {
            return new JSONArray();
        }
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

    public static void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply();
    }
}
