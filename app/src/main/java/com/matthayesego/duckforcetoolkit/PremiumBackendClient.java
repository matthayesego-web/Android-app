package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Reads server-verified TornFCA premium entitlements. The client never claims a payment itself. */
public final class PremiumBackendClient {
    private static final String URL_VALUE=BuildConfig.PREMIUM_BACKEND_URL==null?"":BuildConfig.PREMIUM_BACKEND_URL.trim();
    private static final String PREFS="tornfca_premium_sync";
    private static final long REFRESH_MS=5L*60L*1000L;
    private PremiumBackendClient(){}

    public static boolean isConfigured(){return URL_VALUE.startsWith("https://")&&!URL_VALUE.contains("###");}

    public static void refreshAsync(Context context,int playerId){
        if(context==null||playerId<=0||!isConfigured())return;
        Context app=context.getApplicationContext();
        SharedPreferences p=app.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        long last=p.getLong("last_"+playerId,0L);long now=System.currentTimeMillis();
        if(now-last<REFRESH_MS)return;
        p.edit().putLong("last_"+playerId,now).apply();
        new Thread(()->{try{
            JSONObject response=status(playerId);if(!response.optBoolean("ok",false))return;
            JSONObject entitlement=response.optJSONObject("entitlement");if(entitlement==null)return;
            String tier=entitlement.optString("tier",PremiumEntitlementStore.TIER_FREE);
            long verifiedAt=entitlement.optLong("verified_at",System.currentTimeMillis()/1000L);
            long expiresAt=entitlement.optLong("expires_at",0L);
            String source=entitlement.optString("source","premium-backend");
            PremiumEntitlementStore.saveVerified(app,playerId,tier,source,verifiedAt,expiresAt);
        }catch(Exception ignored){}
        }).start();
    }

    public static JSONObject status(int playerId)throws Exception{JSONObject request=new JSONObject();request.put("action","status");request.put("player_id",playerId);return post(request);}

    public static JSONObject updateConfig(String developerPassword,int daysPerXanax,String requiredMessage)throws Exception{
        JSONObject request=new JSONObject();request.put("action","admin_config");request.put("admin_password",developerPassword==null?"":developerPassword);request.put("days_per_xanax",daysPerXanax);request.put("required_message",requiredMessage==null?"":requiredMessage);return checked(post(request));
    }

    public static JSONObject grant(String developerPassword,int playerId,int days)throws Exception{
        JSONObject request=new JSONObject();request.put("action","admin_grant");request.put("admin_password",developerPassword==null?"":developerPassword);request.put("player_id",playerId);request.put("days",days);return checked(post(request));
    }

    private static JSONObject checked(JSONObject response)throws Exception{if(!response.optBoolean("ok",false))throw new Exception(response.optString("error","Premium backend request failed."));return response;}

    private static JSONObject post(JSONObject body)throws Exception{
        if(!isConfigured())throw new Exception("Premium backend is not configured in this build.");
        HttpURLConnection c=(HttpURLConnection)new URL(URL_VALUE).openConnection();
        try{
            c.setRequestMethod("POST");c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setUseCaches(false);c.setDoOutput(true);
            c.setRequestProperty("Content-Type","text/plain;charset=UTF-8");c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","TornFCA/0.9.12 Android");
            byte[] payload=body.toString().getBytes(StandardCharsets.UTF_8);try(OutputStream out=c.getOutputStream()){out.write(payload);}int code=c.getResponseCode();InputStream in=code>=400?c.getErrorStream():c.getInputStream();String raw=in==null?"":readAll(in);if(code<200||code>=300)throw new Exception("Premium backend HTTP "+code);return new JSONObject(raw);
        }finally{c.disconnect();}
    }

    private static String readAll(InputStream input)throws Exception{try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))>=0)out.write(b,0,n);return out.toString(StandardCharsets.UTF_8.name());}}
}
