package com.matthayesego.duckforcetoolkit;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

/** Tracks the one-time Android notification permission onboarding prompt. */
public final class NotificationPermissionOnboarding {
    private static final String PREFS="tornfca_notification_permission_v1";
    private static final String ASKED="asked";

    private NotificationPermissionOnboarding(){}

    public static boolean shouldRequest(Context context){
        if(context==null||Build.VERSION.SDK_INT<33)return false;
        if(context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)return false;
        return !context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean(ASKED,false);
    }

    public static void markRequested(Context context){
        if(context!=null)context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean(ASKED,true).apply();
    }
}
