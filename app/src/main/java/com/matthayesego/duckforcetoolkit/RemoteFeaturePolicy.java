package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

/** Small fail-open cache for non-secret product policy supplied by the developer backend. */
public final class RemoteFeaturePolicy {
    private static final String PREFS="tornfca_remote_policy_v1";
    private static final String KEY_JSON="config";
    private static final String KEY_FETCHED="fetched_at";
    private static final long TTL_MS=10L*60L*1000L;
    private static final AtomicBoolean IN_FLIGHT=new AtomicBoolean(false);

    private RemoteFeaturePolicy(){}

    public static void refreshAsync(Context context){
        if(context==null||!DeveloperBackendClient.isConfigured())return;
        Context app=context.getApplicationContext();
        SharedPreferences p=app.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        long now=System.currentTimeMillis();
        if(now-p.getLong(KEY_FETCHED,0L)<TTL_MS||!IN_FLIGHT.compareAndSet(false,true))return;
        new Thread(()->{
            try{
                String apiKey=new SecureApiKeyStore(app).load();
                if(apiKey==null||apiKey.trim().isEmpty())return;
                JSONObject response=DeveloperBackendClient.publicConfig(apiKey);
                JSONObject config=response.optJSONObject("config");
                if(config!=null)applyVerifiedConfig(app,config);
            }catch(Exception ignored){
                long previous=p.getLong(KEY_FETCHED,0L);
                if(previous<=0L)p.edit().putLong(KEY_FETCHED,System.currentTimeMillis()-TTL_MS+60_000L).apply();
            }finally{IN_FLIGHT.set(false);}
        },"TornFCA-RemotePolicy").start();
    }

    /** Store policy only after it has already been returned by an authenticated backend request. */
    public static void applyVerifiedConfig(Context context,JSONObject config){
        if(context==null||config==null)return;
        context.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
                .putString(KEY_JSON,config.toString()).putLong(KEY_FETCHED,System.currentTimeMillis()).apply();
    }

    public static boolean maintenanceMode(Context context){return config(context).optBoolean("maintenance_mode",false);}
    public static int minimumVersionCode(Context context){return Math.max(0,config(context).optInt("minimum_version_code",0));}
    public static String betaMessage(Context context){return config(context).optString("beta_message","").trim();}

    public static boolean featureDisabled(Context context,String feature){
        String key;
        if(DeveloperSettings.FEATURE_ACTIVITY.equals(feature))key="disable_activity";
        else if(DeveloperSettings.FEATURE_WAR.equals(feature))key="disable_war";
        else if(DeveloperSettings.FEATURE_CHAIN.equals(feature))key="disable_chain";
        else if(DeveloperSettings.FEATURE_OC.equals(feature))key="disable_oc";
        else if(DeveloperSettings.FEATURE_PULSE.equals(feature))key="disable_pulse";
        else if(DeveloperSettings.FEATURE_LOOKUP.equals(feature))key="disable_lookup";
        else if(DeveloperSettings.FEATURE_PREMIUM_PREVIEW.equals(feature)||"premium".equals(feature))key="disable_premium";
        else return false;
        return config(context).optBoolean(key,false);
    }

    public static boolean versionBlocked(Context context){int minimum=minimumVersionCode(context);return minimum>0&&BuildConfig.VERSION_CODE<minimum;}

    private static JSONObject config(Context context){
        if(context==null)return new JSONObject();
        try{return new JSONObject(context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY_JSON,"{}"));}
        catch(Exception ignored){return new JSONObject();}
    }
}
