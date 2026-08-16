package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Optional TornStats provider. Calls are made only after explicit key-specific user opt-in. */
public final class TornStatsClient {
    private static final String BASE="https://www.tornstats.com/api/v2/";
    public static final String TERMS_URL="https://www.tornstats.com/tos";
    public static final String FAQ_URL="https://www.tornstats.com/faq";
    private static final String PREFS="tornfca_tornstats_consent_v2";
    private static final String USER_AGENT="TornFCA/0.9.13 Android";
    private static final Pattern API_KEY=Pattern.compile("^[A-Za-z0-9]{16}$");
    private static long nextRequestAtMs=0L;
    private static final ConcurrentHashMap<String,CacheEntry> CACHE=new ConcurrentHashMap<>();
    private TornStatsClient(){}

    public static boolean hasConsent(Context c){
        if(c==null)return false;String key=new SecureApiKeyStore(c).load();if(key==null)return false;
        SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        return p.getBoolean("consent",false)&&fingerprint(key).equals(p.getString("key_fingerprint",""));
    }
    public static void setConsent(Context c,boolean consent){
        if(c==null)return;SharedPreferences.Editor e=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit();
        if(consent){String key=new SecureApiKeyStore(c).load();if(key==null){e.clear();}else{e.putBoolean("consent",true).putString("key_fingerprint",fingerprint(key));}}
        else e.clear();e.apply();if(!consent)CACHE.clear();
    }

    public static JSONObject factionRoster(String key)throws IOException{return getCached(path(key,"faction/roster"),120_000L);}
    public static JSONObject userSpy(String key,int playerId)throws IOException{return getCached(path(key,"spy/user/"+playerId),120_000L);}

    private static String path(String key,String suffix)throws IOException{
        if(key==null||!API_KEY.matcher(key.trim()).matches())throw new IOException("TornStats requires the same 16-character Torn API key. A preset Limited Access key is recommended.");
        return BASE+URLEncoder.encode(key,StandardCharsets.UTF_8.name())+"/"+suffix;
    }

    private static JSONObject getCached(String value,long ttl)throws IOException{
        String cacheKey=Integer.toHexString(value.hashCode());CacheEntry cached=CACHE.get(cacheKey);long now=System.currentTimeMillis();
        if(cached!=null&&cached.expiresAt>now)try{return new JSONObject(cached.body);}catch(Exception ignored){CACHE.remove(cacheKey);}
        JSONObject result=get(value);CACHE.put(cacheKey,new CacheEntry(result.toString(),System.currentTimeMillis()+ttl));if(CACHE.size()>80)CACHE.clear();return result;
    }

    private static JSONObject get(String value)throws IOException{
        waitForSlot();
        HttpURLConnection c=(HttpURLConnection)new URL(value).openConnection();
        try{
            c.setRequestMethod("GET");c.setConnectTimeout(12000);c.setReadTimeout(18000);c.setUseCaches(false);
            c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent",USER_AGENT);
            int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();String raw=read(in);
            JSONObject response;try{response=new JSONObject(raw);}catch(Exception e){throw new IOException("TornStats returned an unreadable response.");}
            if(code<200||code>=300)throw new IOException(response.optString("message","TornStats request failed (HTTP "+code+")."));
            if(response.has("status")&&!response.optBoolean("status",false))throw new IOException(response.optString("message","TornStats could not return this data."));
            return response;
        }finally{c.disconnect();}
    }

    /** TornStats documents 100/min; TornFCA intentionally limits itself to 12/min locally. */
    private static synchronized void waitForSlot(){long now=System.currentTimeMillis();long wait=Math.max(0L,nextRequestAtMs-now);if(wait>0)try{Thread.sleep(wait);}catch(InterruptedException e){Thread.currentThread().interrupt();}nextRequestAtMs=System.currentTimeMillis()+5000L;}
    private static String fingerprint(String value){try{MessageDigest d=MessageDigest.getInstance("SHA-256");byte[] bytes=d.digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte v:bytes)b.append(String.format(Locale.US,"%02x",v&0xff));return b.toString();}catch(Exception e){return Integer.toHexString(value.hashCode());}}
    private static String read(InputStream in)throws IOException{if(in==null)return"";try(InputStream input=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=input.read(buffer))!=-1)out.write(buffer,0,n);return out.toString(StandardCharsets.UTF_8.name());}}
    private static final class CacheEntry{final String body;final long expiresAt;CacheEntry(String body,long expiresAt){this.body=body;this.expiresAt=expiresAt;}}
}
