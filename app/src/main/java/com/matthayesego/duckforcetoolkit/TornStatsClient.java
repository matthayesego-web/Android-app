package com.matthayesego.duckforcetoolkit;

import android.content.Context;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Optional TornStats provider. Calls are made only after explicit user opt-in. */
public final class TornStatsClient {
    private static final String BASE="https://www.tornstats.com/api/v2/";
    private static final String PREFS="tornfca_tornstats_consent_v1";
    private static final String USER_AGENT="TornFCA/0.9.12 Android";
    private TornStatsClient(){}

    public static boolean hasConsent(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean("consent",false);}
    public static void setConsent(Context c,boolean consent){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean("consent",consent).apply();}

    public static JSONObject factionRoster(String key)throws IOException{return get(path(key,"faction/roster"));}
    public static JSONObject userSpy(String key,int playerId)throws IOException{return get(path(key,"spy/user/"+playerId));}

    private static String path(String key,String suffix)throws IOException{
        if(key==null||key.trim().isEmpty())throw new IOException("Your Torn API key is unavailable.");
        return BASE+URLEncoder.encode(key,StandardCharsets.UTF_8.name())+"/"+suffix;
    }

    private static JSONObject get(String value)throws IOException{
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

    private static String read(InputStream in)throws IOException{if(in==null)return"";try(InputStream input=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=input.read(buffer))!=-1)out.write(buffer,0,n);return out.toString(StandardCharsets.UTF_8.name());}}
}
