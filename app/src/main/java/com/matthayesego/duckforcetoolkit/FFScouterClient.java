package com.matthayesego.duckforcetoolkit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Small provider boundary around FFScouter. No estimate returned here is treated as exact. */
public final class FFScouterClient {
    private static final String BASE="https://ffscouter.com/api/v1";
    private static final String USER_AGENT="DuckForceCompanion/0.9.2 Android";
    private FFScouterClient(){}

    public static JSONArray getStats(String key,List<Integer> playerIds)throws IOException{
        if(key==null||key.trim().isEmpty())throw new IOException("FFScouter key is not configured.");
        if(playerIds==null||playerIds.isEmpty())return new JSONArray();
        StringBuilder targets=new StringBuilder();
        for(int i=0;i<playerIds.size()&&i<205;i++){if(i>0)targets.append(',');targets.append(playerIds.get(i));}
        String url=BASE+"/get-stats?key="+URLEncoder.encode(key,StandardCharsets.UTF_8.name())+"&targets="+URLEncoder.encode(targets.toString(),StandardCharsets.UTF_8.name());
        String trimmed=get(url).trim();
        try{
            if(trimmed.startsWith("["))return new JSONArray(trimmed);
            JSONObject error=new JSONObject(trimmed);
            throw new IOException(error.optString("error","FFScouter returned an unexpected response."));
        }catch(JSONException e){
            throw new IOException("FFScouter returned an unreadable response.",e);
        }
    }

    private static String get(String value)throws IOException{
        HttpURLConnection connection=(HttpURLConnection)new URL(value).openConnection();
        try{connection.setRequestMethod("GET");connection.setConnectTimeout(12000);connection.setReadTimeout(18000);connection.setRequestProperty("Accept","application/json");connection.setRequestProperty("User-Agent",USER_AGENT);int code=connection.getResponseCode();InputStream in=code>=200&&code<300?connection.getInputStream():connection.getErrorStream();String body=read(in);if(code<200||code>=300){try{JSONObject o=new JSONObject(body);throw new IOException(o.optString("error","FFScouter request failed (HTTP "+code+")."));}catch(JSONException ignored){throw new IOException("FFScouter request failed (HTTP "+code+").");}}return body;}finally{connection.disconnect();}}
    private static String read(InputStream in)throws IOException{if(in==null)return"";try(InputStream input=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=input.read(buffer))!=-1)out.write(buffer,0,n);return out.toString(StandardCharsets.UTF_8.name());}}
}
