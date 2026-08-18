package com.matthayesego.duckforcetoolkit;

import android.content.Context;

/** Central client-side display/use gate for server-verified TornFCA Premium features. */
public final class PremiumAccess {
    public static final String PERSONAL_INSIGHTS="personal_insights";
    public static final String ADVANCED_ALERTS="advanced_alerts";
    public static final String EXTENDED_ACTIVITY="extended_activity";
    public static final String FACTION_PULSE="faction_pulse";
    public static final String MEMBER_DOSSIER="member_dossier";
    public static final String PERSONALIZATION="personalization";

    private PremiumAccess(){}

    public static boolean has(Context context,int playerId,String feature){
        if(context==null
                ||RemoteFeaturePolicy.featureDisabled(context,"premium")
                ||RemoteFeaturePolicy.maintenanceMode(context)
                ||RemoteFeaturePolicy.versionBlocked(context)
                ||playerId<=0)return false;

        // Premium simulation never mutates the entitlement cache. It is accepted only while an
        // authenticated short-lived Developer Channel session exists on this device, allowing a
        // delegated Developer to exercise both sides of the matrix without creating real Premium.
        DeveloperSessionStore.Session developer=new DeveloperSessionStore(context).load();
        if(developer!=null&&DeveloperSettings.simulatePremium(context))return true;
        return PremiumEntitlementStore.hasPremium(context,playerId);
    }

    public static boolean active(Context context){int playerId=currentPlayerId(context);return has(context,playerId,PERSONAL_INSIGHTS);}

    public static int currentPlayerId(Context context){
        if(context==null)return 0;
        String key=new SecureApiKeyStore(context).load();if(key==null||key.isBlank())return 0;
        AuthSession hot=TornApiClient.cachedSession(key);if(hot!=null)return hot.playerId;
        FactionScopeCache.Scope scope=FactionScopeCache.load(context,key);return scope==null?0:scope.playerId;
    }

    public static void refresh(Context context,int playerId){if(context!=null&&playerId>0)PremiumBackendClient.refreshAsync(context,playerId);}
}
