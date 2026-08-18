package com.matthayesego.duckforcetoolkit;

import android.app.Application;

/** Creates local notification channels immediately and starts optional background integrations. */
public class TornFcaApplication extends Application {
    @Override public void onCreate(){
        super.onCreate();
        NotificationCenter.ensureChannels(this);
        RemoteFeaturePolicy.refreshAsync(this);
        if(LegalAcceptanceStore.hasAcceptedCurrent(this))PushNotifications.initialize(this);
    }
}
