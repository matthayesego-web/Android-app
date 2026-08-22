package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

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
    private static final String USER_AGENT="TornFCA/"+TornFcaBrand.VERSION+" Android";
    private static final int DEFAULT_DAYS_PER_XANAX=7;
    private static final String DEFAULT_REQUIRED_MESSAGE="TORNFCA";
    private static final int DEFAULT_PAYMENT_PLAYER_ID=3987363;
    private PremiumBackendClient(){}

    public static boolean isConfigured(){return URL_VALUE.startsWith("https://")&&!URL_VALUE.contains("###");}

    public static void refreshAsync(Context context,int playerId){refreshAsync(context,playerId,false,null);}
    public static void refreshAsync(Context context,int playerId,Runnable callback){refreshAsync(context,playerId,false,callback);}
    public static void refreshAsync(Context context,int playerId,boolean force,Runnable callback){
        if(context==null||playerId<=0||!isConfigured()){post(callback);return;}
        Context app=context.getApplicationContext();
        SecureApiKeyStore keyStore=new SecureApiKeyStore(app);String apiKey=keyStore.load();
        if(apiKey==null||apiKey.trim().isEmpty()){post(callback);return;}
        SharedPreferences p=app.getSharedPreferences(PREFS,Context.MODE_PRIVATE);long last=p.getLong("last_"+playerId,0L);long now=System.currentTimeMillis();
        if(!force&&now-last<REFRESH_MS){post(callback);return;}
        new Thread(()->{try{
            JSONObject response=status(apiKey,playerId);JSONObject entitlement=response.optJSONObject("entitlement");
            if(entitlement!=null){
                String tier=entitlement.optString("tier",PremiumEntitlementStore.TIER_FREE);
                long verifiedAt=entitlement.optLong("verified_at",System.currentTimeMillis()/1000L);
                long expiresAt=entitlement.optLong("expires_at",0L);
                String source=entitlement.optString("source","premium-backend");
                PremiumEntitlementStore.saveVerified(app,playerId,tier,source,verifiedAt,expiresAt);
            }
            saveOffer(app,response.optJSONObject("offer"));p.edit().putLong("last_"+playerId,System.currentTimeMillis()).apply();
        }catch(Exception ignored){}finally{post(callback);}},"TornFCA-PremiumRefresh").start();
    }

    public static int daysPerXanax(Context context){return prefs(context).getInt("offer_days_per_xanax",DEFAULT_DAYS_PER_XANAX);}
    public static String requiredMessage(Context context){return prefs(context).getString("offer_required_message",DEFAULT_REQUIRED_MESSAGE);}
    public static int paymentPlayerId(Context context){return prefs(context).getInt("offer_payment_player_id",DEFAULT_PAYMENT_PLAYER_ID);}
    public static boolean activationsOpen(Context context){return prefs(context).getBoolean("offer_activations_open",false);}
    public static boolean offerVerified(Context context){return prefs(context).getBoolean("offer_verified",false);}

    public static JSONObject status(String apiKey,int playerId)throws Exception{
        JSONObject request=request("status",apiKey);request.put("player_id",playerId);return checked(post(request));
    }

    /** Retained only for binary/source compatibility; authenticated status now requires the signed-in Torn key. */
    @Deprecated public static JSONObject status(int playerId)throws Exception{throw new Exception("Authenticated premium status requires the signed-in Torn API key.");}

    /** Owner-only mutation. The backend verifies the Torn identity represented by apiKey on every request. */
    public static JSONObject updateConfig(String apiKey,int daysPerXanax,String requiredMessage)throws Exception{
        JSONObject request=request("admin_config",apiKey);request.put("days_per_xanax",daysPerXanax);request.put("required_message",requiredMessage==null?"":requiredMessage);return checked(post(request));
    }

    /** Compatibility overload: the obsolete password value is deliberately ignored and never sent. */
    @Deprecated public static JSONObject updateConfig(String apiKey,String ignoredPassword,int daysPerXanax,String requiredMessage)throws Exception{return updateConfig(apiKey,daysPerXanax,requiredMessage);}

    /** Owner-only grant. The backend independently verifies the signed-in Torn player before applying it. */
    public static JSONObject grant(String apiKey,int playerId,int days)throws Exception{
        JSONObject request=request("admin_grant",apiKey);request.put("player_id",playerId);request.put("days",days);request.put("grant_type","developer");return checked(post(request));
    }

    public static JSONObject grantComplimentary(String apiKey,int playerId,int days)throws Exception{
        JSONObject request=request("admin_grant",apiKey);request.put("player_id",playerId);request.put("days",days);request.put("grant_type","complimentary");return checked(post(request));
    }

    /** Compatibility overloads: password is obsolete and never transmitted. */
    @Deprecated public static JSONObject grant(String apiKey,String ignoredPassword,int playerId,int days)throws Exception{return grant(apiKey,playerId,days);}
    @Deprecated public static JSONObject grantComplimentary(String apiKey,String ignoredPassword,int playerId,int days)throws Exception{return grantComplimentary(apiKey,playerId,days);}

    private static SharedPreferences prefs(Context context){return context.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    private static void saveOffer(Context context,JSONObject offer){
        if(offer==null)return;SharedPreferences.Editor e=prefs(context).edit();
        e.putBoolean("offer_verified",true)
                .putInt("offer_days_per_xanax",Math.max(1,offer.optInt("days_per_xanax",DEFAULT_DAYS_PER_XANAX)))
                .putString("offer_required_message",offer.optString("required_message",DEFAULT_REQUIRED_MESSAGE))
                .putInt("offer_payment_player_id",offer.optInt("payment_player_id",DEFAULT_PAYMENT_PLAYER_ID))
                .putBoolean("offer_activations_open",offer.optBoolean("activations_open",false)).apply();
    }
    private static void post(Runnable callback){if(callback==null)return;new Handler(Looper.getMainLooper()).post(callback);}

    private static JSONObject request(String action,String apiKey)throws Exception{
        String key=apiKey==null?"":apiKey.trim();if(key.isEmpty())throw new Exception("Signed-in Torn API key required.");
        TornApiClient.validateKey(key);JSONObject request=new JSONObject();request.put("action",action);request.put("apiKey",key);return request;
    }

    private static JSONObject checked(JSONObject response)throws Exception{if(!response.optBoolean("ok",false))throw new Exception(response.optString("error","Premium backend request failed."));return response;}

    private static JSONObject post(JSONObject body)throws Exception{
        if(!isConfigured())throw new Exception("Premium backend is not configured in this build.");
        HttpURLConnection c=(HttpURLConnection)new URL(URL_VALUE).openConnection();
        try{
            c.setRequestMethod("POST");c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setUseCaches(false);c.setDoOutput(true);
            c.setRequestProperty("Content-Type","text/plain;charset=UTF-8");c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent",USER_AGENT);
            byte[] payload=body.toString().getBytes(StandardCharsets.UTF_8);try(OutputStream out=c.getOutputStream()){out.write(payload);}int code=c.getResponseCode();InputStream in=code>=400?c.getErrorStream():c.getInputStream();String raw=in==null?"":readAll(in);if(code<200||code>=300)throw new Exception("Premium backend HTTP "+code);JSONObject response;try{response=new JSONObject(raw);}catch(Exception e){throw new Exception("Premium backend returned an unreadable response.");}return response;
        }finally{c.disconnect();}
    }

    private static String readAll(InputStream input)throws Exception{try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))>=0)out.write(b,0,n);return out.toString(StandardCharsets.UTF_8.name());}}
}
