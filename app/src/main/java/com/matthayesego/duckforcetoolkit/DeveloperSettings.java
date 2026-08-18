package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.content.SharedPreferences;

public final class DeveloperSettings {
    public static final String FEATURE_ACTIVITY = "activity";
    public static final String FEATURE_WAR = "war";
    public static final String FEATURE_CHAIN = "chain";
    public static final String FEATURE_OC = "oc";
    public static final String FEATURE_PULSE = "pulse";
    public static final String FEATURE_LOOKUP = "lookup";
    public static final String FEATURE_PREMIUM_PREVIEW = "premium_preview";

    private static final String PREFS = "duckforce_developer_v060";
    private static final String KEY_MULTI_FACTION_PREVIEW = "multi_faction_preview";
    private static final String KEY_VERBOSE = "verbose_diagnostics";
    private static final String KEY_PUBLIC_ONLY = "simulate_public_only";
    private static final String KEY_PREMIUM_SIM = "simulate_premium";
    private static final String KEY_WAR_SIM = "simulate_ranked_war";
    private static final String KEY_ACTIVITY_DAYS = "activity_days";
    private static final String KEY_ACTIVITY_PAGES = "activity_pages";
    private static final String KEY_FEATURE_PREFIX = "feature_";
    private static volatile int runtimeActivityPages = 20;
    private static volatile boolean runtimeWarSimulation = false;

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

    public static boolean simulatePremium(Context context) {
        // Production-safe default: no device receives Premium merely for installing a beta.
        // PremiumAccess separately restricts this simulation flag to the verified owner ID.
        return prefs(context).getBoolean(KEY_PREMIUM_SIM, false);
    }

    public static void setSimulatePremium(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_PREMIUM_SIM, enabled).apply();
    }

    public static boolean simulateWar(Context context) {
        runtimeWarSimulation = prefs(context).getBoolean(KEY_WAR_SIM, false);
        return runtimeWarSimulation;
    }

    public static void setSimulateWar(Context context, boolean enabled) {
        runtimeWarSimulation = enabled;
        prefs(context).edit().putBoolean(KEY_WAR_SIM, enabled).apply();
    }

    public static boolean runtimeSimulateWar() {
        return runtimeWarSimulation;
    }

    public static int activityDays(Context context) {
        int days = prefs(context).getInt(KEY_ACTIVITY_DAYS, 30);
        int requested = days == 7 || days == 14 || days == 30 ? days : 30;
        int playerId = PremiumAccess.currentPlayerId(context);
        return PremiumAccess.has(context, playerId, PremiumAccess.EXTENDED_ACTIVITY) ? requested : 7;
    }

    public static void setActivityDays(Context context, int days) {
        if (days != 7 && days != 14 && days != 30) days = 30;
        prefs(context).edit().putInt(KEY_ACTIVITY_DAYS, days).apply();
    }

    public static int activityMaxPages(Context context) {
        int pages = prefs(context).getInt(KEY_ACTIVITY_PAGES, 20);
        runtimeActivityPages = pages == 5 || pages == 10 || pages == 20 ? pages : 20;
        return runtimeActivityPages;
    }

    public static void setActivityMaxPages(Context context, int pages) {
        if (pages != 5 && pages != 10 && pages != 20) pages = 20;
        runtimeActivityPages = pages;
        prefs(context).edit().putInt(KEY_ACTIVITY_PAGES, pages).apply();
    }

    public static int runtimeActivityMaxPages() {
        return runtimeActivityPages;
    }

    public static boolean featureEnabled(Context context, String feature) {
        return prefs(context).getBoolean(KEY_FEATURE_PREFIX + feature, true);
    }

    public static void setFeatureEnabled(Context context, String feature, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_FEATURE_PREFIX + feature, enabled).apply();
    }

    public static void reset(Context context) {
        runtimeActivityPages = 20;
        runtimeWarSimulation = false;
        prefs(context).edit().clear().apply();
    }
}
