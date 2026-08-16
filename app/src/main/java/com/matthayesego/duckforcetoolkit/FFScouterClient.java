package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Provider boundary around FFScouter. Every request uses the current player's own opted-in Torn API key. */
public final class FFScouterClient {
    private static final String BASE="https://ffscouter.com/api/v1";
    public static final String HOMEPAGE="https://ffscouter.com/";
    public static final String API_DOCS="https://ffscouter.com/api-docs";
    private static final String PREFS="tornfca_ffscouter_consent_v2";
    private static final String USER_AGENT="TornFCA/0.9.13 Android";
    private static final Pattern API_KEY=Pattern.compile("^[A-Za-z0-9]{16}$");

    // FFScouter currently documents 20/min for get-stats, 10/min for check-key and 3/min for
    // registration. TornFCA stays slightly below each published ceiling and caches read requests.
    private static long nextStatsAtMs=0L,nextCheckAtMs=0L,nextRegisterAtMs=0L;
    private static volatile String approvedFingerprint="";
    private static final ConcurrentHashMap<String,CacheEntry> CACHE=new ConcurrentHashMap<>();

    private FFScouterClient(){}

    /** Reads local consent only; it never contacts FFScouter. Also primes the process-level safety gate. */
    public static boolean hasConsent(Context context,String key){
        if(context==null||key==null)return false;
        SharedPreferences p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        String fp=fingerprint(key);boolean enabled=p.getBoolean("enabled",false)&&fp.equals(p.getString("key_fingerprint",""));
        approvedFingerprint=enabled?fp:"";return enabled;
    }
    public static void setConsent(Context context,String key,boolean enabled){
        if(context==null)return;
        SharedPreferences.Editor e=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit();
        if(enabled&&key!=null){String fp=fingerprint(key);e.putBoolean("enabled",true).putString("key_fingerprint",fp);approvedFingerprint=fp;}
        else{e.clear();approvedFingerprint="";}
        e.apply();
        if(!enabled)CACHE.clear();
    }

    public static JSONObject checkKey(String key)throws IOException{
        requireAuthorizedKey(key);
        String cacheKey="check|"+fingerprint(key);JSONObject cached=cacheObject(cacheKey);if(cached!=null)return cached;
        waitForCheckSlot();
        String url=BASE+"/check-key?key="+URLEncoder.encode(key,StandardCharsets.UTF_8.name());
        JSONObject result=objectResponse(get(url));cache(cacheKey,result.toString(),60_000L);return result;
    }

    public static JSONObject registerKey(String key)throws IOException{
        requireAuthorizedKey(key);waitForRegisterSlot();
        JSONObject payload=new JSONObject();
        try{
            payload.put("key",key);
            payload.put("agree_to_data_policy",true);
            payload.put("signup_source","TornFCA");
        }catch(JSONException e){throw new IOException("Unable to prepare FFScouter registration.",e);}
        JSONObject result=objectResponse(post(BASE+"/register",payload.toString()));CACHE.clear();return result;
    }

    public static JSONArray getStats(String key,List<Integer> playerIds)throws IOException{
        requireAuthorizedKey(key);
        if(playerIds==null||playerIds.isEmpty())return new JSONArray();
        StringBuilder targets=new StringBuilder();
        for(int i=0;i<playerIds.size()&&i<205;i++){if(i>0)targets.append(',');targets.append(playerIds.get(i));}
        String cacheKey="stats|"+fingerprint(key)+"|"+Integer.toHexString(targets.toString().hashCode());String cached=cacheBody(cacheKey);if(cached!=null)try{return new JSONArray(cached);}catch(Exception ignored){CACHE.remove(cacheKey);}
        waitForStatsSlot();
        String url=BASE+"/get-stats?key="+URLEncoder.encode(key,StandardCharsets.UTF_8.name())+"&targets="+URLEncoder.encode(targets.toString(),StandardCharsets.UTF_8.name());
        String trimmed=get(url).trim();
        try{
            if(trimmed.startsWith("[")){JSONArray result=new JSONArray(trimmed);cache(cacheKey,result.toString(),60_000L);return result;}
            JSONObject error=new JSONObject(trimmed);
            throw new IOException(error.optString("error","FFScouter returned an unexpected response."));
        }catch(JSONException e){throw new IOException("FFScouter returned an unreadable response.",e);}
    }

    private static void requireAuthorizedKey(String key)throws IOException{
        requireKey(key);String fp=fingerprint(key);
        if(!fp.equals(approvedFingerprint))throw new IOException("FFScouter is disabled in TornFCA. Enable it from Faction Strength Intel after reviewing FFScouter's Data Policy and Terms.");
    }
    private static void requireKey(String key)throws IOException{if(key==null||!API_KEY.matcher(key.trim()).matches())throw new IOException("FFScouter requires the same 16-character alphanumeric Torn API key.");}
    private static JSONObject objectResponse(String body)throws IOException{try{return new JSONObject(body.trim());}catch(JSONException e){throw new IOException("FFScouter returned an unreadable response.",e);}}

    private static String get(String value)throws IOException{
        HttpURLConnection c=(HttpURLConnection)new URL(value).openConnection();
        try{configure(c,"GET");return readResponse(c);}finally{c.disconnect();}
    }

    private static String post(String value,String json)throws IOException{
        HttpURLConnection c=(HttpURLConnection)new URL(value).openConnection();
        try{
            configure(c,"POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");
            byte[] bytes=json.getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(bytes.length);
            try(OutputStream out=c.getOutputStream()){out.write(bytes);}
            return readResponse(c);
        }finally{c.disconnect();}
    }

    private static void configure(HttpURLConnection c,String method)throws IOException{c.setRequestMethod(method);c.setConnectTimeout(12000);c.setReadTimeout(18000);c.setUseCaches(false);c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent",USER_AGENT);}
    private static String readResponse(HttpURLConnection c)throws IOException{
        int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();String body=read(in);
        if(code<200||code>=300){try{JSONObject o=new JSONObject(body);throw new IOException(o.optString("error","FFScouter request failed (HTTP "+code+")."));}catch(JSONException ignored){throw new IOException("FFScouter request failed (HTTP "+code+").");}}
        return body;
    }

    private static synchronized void waitForStatsSlot(){waitUntil(nextStatsAtMs);nextStatsAtMs=System.currentTimeMillis()+3200L;}
    private static synchronized void waitForCheckSlot(){waitUntil(nextCheckAtMs);nextCheckAtMs=System.currentTimeMillis()+6500L;}
    private static synchronized void waitForRegisterSlot(){waitUntil(nextRegisterAtMs);nextRegisterAtMs=System.currentTimeMillis()+21000L;}
    private static void waitUntil(long at){long wait=Math.max(0L,at-System.currentTimeMillis());if(wait>0)try{Thread.sleep(wait);}catch(InterruptedException e){Thread.currentThread().interrupt();}}

    private static void cache(String key,String body,long ttl){CACHE.put(key,new CacheEntry(body,System.currentTimeMillis()+ttl));if(CACHE.size()>80)CACHE.clear();}
    private static String cacheBody(String key){CacheEntry e=CACHE.get(key);if(e==null)return null;if(e.expiresAt<System.currentTimeMillis()){CACHE.remove(key);return null;}return e.body;}
    private static JSONObject cacheObject(String key){String body=cacheBody(key);if(body==null)return null;try{return new JSONObject(body);}catch(Exception e){CACHE.remove(key);return null;}}

    private static String fingerprint(String value){try{MessageDigest d=MessageDigest.getInstance("SHA-256");byte[] bytes=d.digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte v:bytes)b.append(String.format(java.util.Locale.US,"%02x",v&0xff));return b.toString();}catch(Exception e){return Integer.toHexString(value.hashCode());}}
    private static String read(InputStream in)throws IOException{if(in==null)return"";try(InputStream input=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=input.read(buffer))!=-1)out.write(buffer,0,n);return out.toString(StandardCharsets.UTF_8.name());}}
    private static final class CacheEntry{final String body;final long expiresAt;CacheEntry(String body,long expiresAt){this.body=body;this.expiresAt=expiresAt;}}
}
