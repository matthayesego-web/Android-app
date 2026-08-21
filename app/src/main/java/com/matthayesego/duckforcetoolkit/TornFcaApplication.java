package com.matthayesego.duckforcetoolkit;

import android.app.Application;

/** Creates notification channels, command-shell lifecycle helpers and optional background integrations. */
public class TornFcaApplication extends Application {
    @Override public void onCreate(){
        super.onCreate();
        NotificationCenter.ensureChannels(this);
        TornFcaCommandRuntime.install(this);
        RemoteFeaturePolicy.refreshAsync(this);
        if(LegalAcceptanceStore.hasAcceptedCurrent(this))PushNotifications.initialize(this);
    }
}
