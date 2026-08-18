package com.matthayesego.duckforcetoolkit;

import android.content.Context;

/** Central client-side display gate for server-verified TornFCA Premium features. */
public final class PremiumAccess {
    public static final String PERSONAL_INSIGHTS="personal_insights";
    public static final String ADVANCED_ALERTS="advanced_alerts";
    public static final String PERSONALIZATION="personalization";
    private PremiumAccess(){}
    public static boolean has(Context context,int playerId,String feature){
        return context!=null
                &&!RemoteFeaturePolicy.featureDisabled(context,"premium")
                &&!RemoteFeaturePolicy.maintenanceMode(context)
                &&!RemoteFeaturePolicy.versionBlocked(context)
                &&playerId>0
                &&PremiumEntitlementStore.hasPremium(context,playerId);
    }
    public static void refresh(Context context,int playerId){if(context!=null&&playerId>0)PremiumBackendClient.refreshAsync(context,playerId);}
}
