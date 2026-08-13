package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

public final class BankingDraftStore {
    private static final String PREFS = "duckforce_banking_v040";
    private static final String KEY = "requests";

    private BankingDraftStore() {}

    public static void add(Context context, AuthSession session, String amount, String note) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONArray rows = new JSONArray(prefs.getString(KEY, "[]"));
            JSONObject row = new JSONObject();
            row.put("created", System.currentTimeMillis());
            row.put("playerId", session.playerId);
            row.put("playerName", session.playerName);
            row.put("amount", amount == null || amount.trim().isEmpty() ? "FULL BALANCE" : amount.trim());
            row.put("note", note == null ? "" : note.trim());
            row.put("status", "LOCAL TEST");
            rows.put(row);
            prefs.edit().putString(KEY, rows.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public static JSONArray all(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            return new JSONArray(prefs.getString(KEY, "[]"));
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    public static void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply();
    }
}
