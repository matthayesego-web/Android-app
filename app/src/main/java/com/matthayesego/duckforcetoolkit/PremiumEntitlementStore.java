package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Local cache for a backend-verified premium entitlement.
 *
 * This class deliberately does not accept client-side payment claims. A future backend must
 * verify Torn receipt/payment activity and then return the entitlement tied to the numeric
 * Torn player ID. The app only caches that verified result for quick access/offline display.
 */
public final class PremiumEntitlementStore {
    public static final String TIER_FREE = "FREE";
    public static final String TIER_PREMIUM = "PREMIUM";

    private static final String PREFS = "duckforce_premium_entitlement_v1";
    private static final String KEY_PLAYER_ID = "player_id";
    private static final String KEY_TIER = "tier";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_VERIFIED_AT = "verified_at";
    private static final String KEY_EXPIRES_AT = "expires_at";

    private PremiumEntitlementStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean hasPremium(Context context, int playerId) {
        if (DeveloperSettings.simulatePremium(context)) return true;
        SharedPreferences p = prefs(context);
        if (playerId <= 0 || p.getInt(KEY_PLAYER_ID, 0) != playerId) return false;
        if (!TIER_PREMIUM.equals(p.getString(KEY_TIER, TIER_FREE))) return false;
        long expiresAt = p.getLong(KEY_EXPIRES_AT, 0L);
        long now = System.currentTimeMillis() / 1000L;
        return expiresAt <= 0L || expiresAt > now;
    }

    public static void saveVerified(Context context, int playerId, String tier, String source,
                                    long verifiedAt, long expiresAt) {
        if (playerId <= 0) return;
        String safeTier = TIER_PREMIUM.equalsIgnoreCase(tier) ? TIER_PREMIUM : TIER_FREE;
        prefs(context).edit()
                .putInt(KEY_PLAYER_ID, playerId)
                .putString(KEY_TIER, safeTier)
                .putString(KEY_SOURCE, source == null ? "backend" : source)
                .putLong(KEY_VERIFIED_AT, verifiedAt)
                .putLong(KEY_EXPIRES_AT, expiresAt)
                .apply();
    }

    public static String summary(Context context) {
        if (DeveloperSettings.simulatePremium(context)) return "PREMIUM • developer simulation";
        SharedPreferences p = prefs(context);
        int playerId = p.getInt(KEY_PLAYER_ID, 0);
        if (playerId <= 0) return "FREE • no verified entitlement cached";
        String tier = p.getString(KEY_TIER, TIER_FREE);
        String source = p.getString(KEY_SOURCE, "backend");
        return tier + " • player " + playerId + " • " + source;
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }
}
