package com.matthayesego.duckforcetoolkit;

import android.app.Application;

/** Initializes notification channels and optional cloud messaging before any Activity/Service. */
public class TornFcaApplication extends Application {
    @Override public void onCreate(){
        super.onCreate();
        NotificationCenter.ensureChannels(this);
        PushNotifications.initialize(this);
    }
}
