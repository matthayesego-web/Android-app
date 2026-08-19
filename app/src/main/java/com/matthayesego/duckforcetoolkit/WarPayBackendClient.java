package com.matthayesego.duckforcetoolkit;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Faction-scoped cross-device persistence for leadership WarPay receipts. */
public final class WarPayBackendClient {
    private static final String BACKEND_URL=BuildConfig.WARPAY_BACKEND_URL==null?"":BuildConfig.WARPAY_BACKEND_URL.trim();
    private static final String USER_AGENT="TornFCA/"+TornFcaBrand.VERSION+" Android WarPay";

    private WarPayBackendClient(){}
    public static boolean isConfigured(){return BACKEND_URL.startsWith("https://")&&!BACKEND_URL.contains("###");}

    public static JSONArray list(String apiKey)throws IOException{
        JSONObject response=post(request("list",apiKey));JSONArray rows=response.optJSONArray("receipts");return rows==null?new JSONArray():rows;
    }

    public static JSONObject get(String apiKey,int warId)throws IOException{
        JSONObject body=request("get",apiKey);put(body,"war_id",warId);JSONObject response=post(body);return response.optJSONObject("receipt");
    }

    public static JSONObject save(String apiKey,JSONObject receipt)throws IOException{
        if(receipt==null)throw new IOException("WarPay receipt required.");
        JSONObject body=request("save",apiKey);try{body.put("receipt",receipt);}catch(Exception e){throw new IOException("Unable to prepare WarPay receipt.");}
        JSONObject response=post(body);JSONObject saved=response.optJSONObject("receipt");return saved==null?receipt:saved;
    }

    private static JSONObject request(String action,String apiKey){JSONObject o=new JSONObject();put(o,"action",action);put(o,"apiKey",apiKey==null?"":apiKey);return o;}
    private static void put(JSONObject o,String key,Object value){try{o.put(key,value);}catch(Exception ignored){}}

    private static JSONObject post(JSONObject body)throws IOException{
        if(!isConfigured())throw new IOException("WarPay backend is not configured in this build.");
        String apiKey=body.optString("apiKey","");if(apiKey.isEmpty())throw new IOException("Signed-in Torn API key required.");
        TornApiClient.validateKey(apiKey);BackendRequestGovernor.acquire();
        HttpURLConnection c=(HttpURLConnection)new URL(BACKEND_URL).openConnection();
        try{
            c.setRequestMethod("POST");c.setConnectTimeout(10000);c.setReadTimeout(20000);c.setUseCaches(false);c.setDoOutput(true);
            c.setRequestProperty("Content-Type","text/plain;charset=UTF-8");c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent",USER_AGENT);
            byte[] payload=body.toString().getBytes(StandardCharsets.UTF_8);try(OutputStream out=c.getOutputStream()){out.write(payload);}
            int code=c.getResponseCode();InputStream in=code>=400?c.getErrorStream():c.getInputStream();String raw=in==null?"":read(in);JSONObject response;
            try{response=new JSONObject(raw);}catch(Exception e){throw new IOException("WarPay backend returned an unreadable response.");}
            if(code<200||code>=300)throw new IOException("WarPay backend HTTP "+code+".");
            if(!response.optBoolean("ok",false))throw new IOException(response.optString("error","WarPay backend request failed."));
            return response;
        }finally{c.disconnect();}
    }

    private static String read(InputStream input)throws IOException{try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString(StandardCharsets.UTF_8.name());}}
}
