package com.matthayesego.duckforcetoolkit;

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
import java.util.List;

/** Provider boundary around FFScouter. Every request uses the current player's own registered Torn API key. */
public final class FFScouterClient {
    private static final String BASE="https://ffscouter.com/api/v1";
    private static final String USER_AGENT="DuckForceCompanion/0.9.5 Android";
    private FFScouterClient(){}

    public static JSONObject checkKey(String key)throws IOException{
        requireKey(key);
        String url=BASE+"/check-key?key="+URLEncoder.encode(key,StandardCharsets.UTF_8.name());
        return objectResponse(get(url));
    }

    public static JSONObject registerKey(String key)throws IOException{
        requireKey(key);
        JSONObject payload=new JSONObject();
        try{
            payload.put("key",key);
            payload.put("agree_to_data_policy",true);
            payload.put("signup_source","DuckForceCompanion");
        }catch(JSONException e){throw new IOException("Unable to prepare FFScouter registration.",e);}
        return objectResponse(post(BASE+"/register",payload.toString()));
    }

    public static JSONArray getStats(String key,List<Integer> playerIds)throws IOException{
        requireKey(key);
        if(playerIds==null||playerIds.isEmpty())return new JSONArray();
        StringBuilder targets=new StringBuilder();
        for(int i=0;i<playerIds.size()&&i<205;i++){if(i>0)targets.append(',');targets.append(playerIds.get(i));}
        String url=BASE+"/get-stats?key="+URLEncoder.encode(key,StandardCharsets.UTF_8.name())+"&targets="+URLEncoder.encode(targets.toString(),StandardCharsets.UTF_8.name());
        String trimmed=get(url).trim();
        try{
            if(trimmed.startsWith("["))return new JSONArray(trimmed);
            JSONObject error=new JSONObject(trimmed);
            throw new IOException(error.optString("error","FFScouter returned an unexpected response."));
        }catch(JSONException e){throw new IOException("FFScouter returned an unreadable response.",e);}
    }

    private static void requireKey(String key)throws IOException{if(key==null||key.trim().isEmpty())throw new IOException("Your Torn API key is not available.");}
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

    private static void configure(HttpURLConnection c,String method)throws IOException{c.setRequestMethod(method);c.setConnectTimeout(12000);c.setReadTimeout(18000);c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent",USER_AGENT);}
    private static String readResponse(HttpURLConnection c)throws IOException{
        int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();String body=read(in);
        if(code<200||code>=300){try{JSONObject o=new JSONObject(body);throw new IOException(o.optString("error","FFScouter request failed (HTTP "+code+")."));}catch(JSONException ignored){throw new IOException("FFScouter request failed (HTTP "+code+").");}}
        return body;
    }
    private static String read(InputStream in)throws IOException{if(in==null)return"";try(InputStream input=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=input.read(buffer))!=-1)out.write(buffer,0,n);return out.toString(StandardCharsets.UTF_8.name());}}
}
