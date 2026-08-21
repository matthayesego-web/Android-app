package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.content.SharedPreferences;

/** Local-only UI preview state. Never changes Torn identity or backend authorization. */
public final class DeveloperPreviewStore {
    private static final String PREFS = "duckforce_developer_preview";
    private static final String KEY_MEMBER_PREVIEW = "member_preview";

    private DeveloperPreviewStore() {}

    public static boolean isMemberPreview(Context context) {
        return prefs(context).getBoolean(KEY_MEMBER_PREVIEW, false);
    }

    public static void setMemberPreview(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_MEMBER_PREVIEW, enabled).apply();
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
