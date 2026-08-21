package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

/** Local/offline cache backed by faction-scoped WarPay cloud persistence when configured. */
public final class WarPayoutReceiptStore {
    private static final String PREFS = "tornfca_warpay_receipts_v1";
    private static final String PREFIX = "war_";
    private static final AtomicBoolean REFRESHING = new AtomicBoolean(false);

    private WarPayoutReceiptStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Save locally immediately, then upload best-effort without blocking the payout workflow. */
    public static void save(Context context, JSONObject receipt) {
        if (context == null || receipt == null) return;
        saveLocal(context, receipt);
        scheduleUpload(context.getApplicationContext(), receipt);
    }

    private static void saveLocal(Context context, JSONObject receipt) {
        int warId = receipt.optInt("war_id", 0);
        if (warId <= 0) return;
        prefs(context).edit().putString(PREFIX + warId, receipt.toString()).apply();
    }

    /** Refresh the offline cache from the current verified faction. Non-leaders simply receive no update. */
    public static void refreshFromBackendAsync(Context context) {
        if (context == null || !WarPayBackendClient.isConfigured() || !REFRESHING.compareAndSet(false, true)) return;
        Context app = context.getApplicationContext();
        new Thread(() -> {
            try {
                String key = new SecureApiKeyStore(app).load();
                if (key == null || key.trim().isEmpty()) return;
                mergeFromBackend(app, WarPayBackendClient.list(key));
            } catch (Exception ignored) {
                // Local receipts remain usable when the backend is unavailable or the user lacks leadership access.
            } finally {
                REFRESHING.set(false);
            }
        }, "TornFCA-WarPayRefresh").start();
    }

    /** Merge faction-scoped server receipts without replacing a newer local calculation. */
    public static void mergeFromBackend(Context context, JSONArray receipts) {
        if (context == null || receipts == null) return;
        for (int i = 0; i < receipts.length(); i++) {
            JSONObject remote = receipts.optJSONObject(i);
            if (remote == null) continue;
            int warId = remote.optInt("war_id", 0);
            if (warId <= 0) continue;
            JSONObject local = load(context, warId);
            long remoteAt = remote.optLong("created_at", 0L);
            long localAt = local == null ? 0L : local.optLong("created_at", 0L);
            if (local == null || remoteAt >= localAt) saveLocal(context, remote);
        }
    }

    private static void scheduleUpload(Context context, JSONObject receipt) {
        if (!WarPayBackendClient.isConfigured()) return;
        final String copy = receipt.toString();
        new Thread(() -> {
            try {
                String key = new SecureApiKeyStore(context).load();
                if (key == null || key.trim().isEmpty()) return;
                WarPayBackendClient.save(key, new JSONObject(copy));
            } catch (Exception ignored) {
                // Offline-first: never roll back the local receipt when cloud persistence fails.
            }
        }, "TornFCA-WarPayUpload").start();
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
