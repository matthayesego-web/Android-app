package com.matthayesego.duckforcetoolkit;

import android.app.Application;

/** Creates local notification channels immediately and enables optional cloud push after legal acknowledgement. */
public class TornFcaApplication extends Application {
    @Override public void onCreate(){
        super.onCreate();
        NotificationCenter.ensureChannels(this);
        if(LegalAcceptanceStore.hasAcceptedCurrent(this))PushNotifications.initialize(this);
    }
}
