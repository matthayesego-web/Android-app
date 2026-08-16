package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * App-private receipt persistence for leadership WarPay calculations.
 * Android backup is disabled for TornFCA, so these records remain on this device until app data is cleared.
 * Cross-device/faction-wide receipt sync belongs in the shared backend once it is deployed.
 */
public final class WarPayoutReceiptStore {
    private static final String PREFS = "tornfca_warpay_receipts_v1";
    private static final String PREFIX = "war_";

    private WarPayoutReceiptStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void save(Context context, JSONObject receipt) {
        if (context == null || receipt == null) return;
        int warId = receipt.optInt("war_id", 0);
        if (warId <= 0) return;
        prefs(context).edit().putString(PREFIX + warId, receipt.toString()).apply();
    }

    public static JSONObject load(Context context, int warId) {
        if (context == null || warId <= 0) return null;
        String raw = prefs(context).getString(PREFIX + warId, null);
        if (raw == null || raw.isBlank()) return null;
        try { return new JSONObject(raw); }
        catch (Exception ignored) { return null; }
    }

    public static boolean has(Context context, int warId) {
        return load(context, warId) != null;
    }

    public static JSONObject memberRow(Context context, int warId, int playerId) {
        JSONObject receipt = load(context, warId);
        if (receipt == null || playerId <= 0) return null;
        JSONArray rows = receipt.optJSONArray("rows");
        for (int i = 0; rows != null && i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row != null && row.optInt("player_id", 0) == playerId) return row;
        }
        return null;
    }

    public static String text(JSONObject receipt) {
        if (receipt == null) return "Receipt unavailable.";
        StringBuilder out = new StringBuilder();
        int warId = receipt.optInt("war_id", 0);
        out.append("TornFCA WarPay Receipt — War #").append(warId).append('\n');
        out.append("Pool: ").append(money(receipt.optLong("pool", 0))).append('\n');
        out.append("Paid: ").append(money(receipt.optLong("total_paid", 0))).append('\n');
        out.append("Penalties retained: ").append(money(receipt.optLong("total_penalty", 0))).append('\n');
        out.append("Members: ").append(receipt.optInt("member_count", 0)).append("\n\n");
        JSONArray rows = receipt.optJSONArray("rows");
        for (int i = 0; rows != null && i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i); if (row == null) continue;
            out.append(i + 1).append(". ").append(row.optString("name", "Member"))
                    .append(" [").append(row.optInt("player_id", 0)).append("] — ")
                    .append(money(row.optLong("net", 0)));
            long penalty = row.optLong("penalty", 0);
            if (penalty > 0) {
                out.append(" (gross ").append(money(row.optLong("gross", 0)))
                        .append(", penalty -").append(money(penalty));
                String reason = row.optString("reason", "");
                if (!reason.isBlank()) out.append(", ").append(reason);
                out.append(')');
            }
            out.append('\n');
        }
        return out.toString().trim();
    }

    private static String money(long value) {
        return "$" + java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(value);
    }
}
