package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.content.SharedPreferences;

public final class DeveloperSettings {
    private static final String PREFS = "duckforce_developer_v060";
    private static final String KEY_MULTI_FACTION_PREVIEW = "multi_faction_preview";
    private static final String KEY_VERBOSE = "verbose_diagnostics";
    private static final String KEY_PUBLIC_ONLY = "simulate_public_only";
    private static final String KEY_ACTIVITY_DAYS = "activity_days";

    private DeveloperSettings() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean multiFactionPreview(Context context) {
        return prefs(context).getBoolean(KEY_MULTI_FACTION_PREVIEW, false);
    }

    public static void setMultiFactionPreview(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_MULTI_FACTION_PREVIEW, enabled).apply();
    }

    public static boolean verboseDiagnostics(Context context) {
        return prefs(context).getBoolean(KEY_VERBOSE, false);
    }

    public static void setVerboseDiagnostics(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_VERBOSE, enabled).apply();
    }

    public static boolean simulatePublicOnly(Context context) {
        return prefs(context).getBoolean(KEY_PUBLIC_ONLY, false);
    }

    public static void setSimulatePublicOnly(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_PUBLIC_ONLY, enabled).apply();
    }

    public static int activityDays(Context context) {
        int days = prefs(context).getInt(KEY_ACTIVITY_DAYS, 30);
        return days == 7 || days == 14 || days == 30 ? days : 30;
    }

    public static void setActivityDays(Context context, int days) {
        if (days != 7 && days != 14 && days != 30) days = 30;
        prefs(context).edit().putInt(KEY_ACTIVITY_DAYS, days).apply();
    }

    public static void reset(Context context) {
        prefs(context).edit().clear().apply();
    }
}
