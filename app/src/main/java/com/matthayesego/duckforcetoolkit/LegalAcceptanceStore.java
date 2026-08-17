package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.content.SharedPreferences;

/** Device-local record that the current TornFCA legal documents and data-use acknowledgement were accepted. */
public final class LegalAcceptanceStore {
    public static final String LEGAL_VERSION="2026-08-17-v3";
    private static final String PREFS="tornfca_legal";
    private static final String KEY_VERSION="accepted_version";
    private static final String KEY_AT="accepted_at";

    private LegalAcceptanceStore(){}

    public static boolean hasAcceptedCurrent(Context context){
        return LEGAL_VERSION.equals(prefs(context).getString(KEY_VERSION,""));
    }

    public static long acceptedAt(Context context){
        return hasAcceptedCurrent(context)?prefs(context).getLong(KEY_AT,0L):0L;
    }

    public static void acceptCurrent(Context context){
        prefs(context).edit().putString(KEY_VERSION,LEGAL_VERSION).putLong(KEY_AT,System.currentTimeMillis()).apply();
    }

    /** Used when a materially changed legal version must be shown again. */
    public static void clear(Context context){prefs(context).edit().clear().apply();}

    private static SharedPreferences prefs(Context context){return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
}
