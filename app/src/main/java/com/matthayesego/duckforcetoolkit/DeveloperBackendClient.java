package com.matthayesego.duckforcetoolkit;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Transport for TornFCA's developer control plane and authenticated public product policy. */
public final class DeveloperBackendClient {
    private static final String BACKEND_URL=BuildConfig.DEVELOPER_BACKEND_URL==null?"":BuildConfig.DEVELOPER_BACKEND_URL.trim();
    private static final String USER_AGENT="TornFCA/"+TornFcaBrand.VERSION+" Android Developer";
    private static long nextRequestAtMs=0L;

    private DeveloperBackendClient(){}

    public static boolean isConfigured(){return BACKEND_URL.startsWith("https://")&&!BACKEND_URL.contains("###");}
    public static String configuredUrl(){return BACKEND_URL;}

    /** Non-secret app policy. Any verified Torn user may read it. */
    public static JSONObject publicConfig(String apiKey)throws IOException{
        return postChecked(request("public_config",apiKey),"Unable to read TornFCA remote policy.");
    }

    public static JSONObject status(String apiKey)throws IOException{
        return postChecked(request("status",apiKey),"Unable to read developer backend status.");
    }

    public static JSONObject readConfig(String apiKey)throws IOException{
        return postChecked(request("config_read",apiKey),"Unable to read developer configuration.");
    }

    public static JSONObject writeConfig(String apiKey,String developerPassword,JSONObject config)throws IOException{
        JSONObject body=request("config_write",apiKey);
        put(body,"admin_password",developerPassword==null?"":developerPassword);
        try{body.put("config",config==null?new JSONObject():config);}catch(Exception ignored){}
        return postChecked(body,"Unable to update developer configuration.");
    }

    public static JSONObject readAudit(String apiKey,String developerPassword)throws IOException{
        JSONObject body=request("audit_list",apiKey);
        put(body,"admin_password",developerPassword==null?"":developerPassword);
        return postChecked(body,"Unable to read developer audit history.");
    }

    private static JSONObject request(String action,String apiKey){
        JSONObject body=new JSONObject();
        put(body,"action",action);
        put(body,"apiKey",apiKey==null?"":apiKey);
        return body;
    }

    private static void put(JSONObject o,String key,Object value){try{o.put(key,value==null?"":value);}catch(Exception ignored){}}

    private static JSONObject postChecked(JSONObject body,String fallback)throws IOException{
        if(!isConfigured())throw new IOException("TornFCA developer backend is not configured in this build.");
        String apiKey=body.optString("apiKey","");
        if(apiKey.isEmpty())throw new IOException("Developer verification requires the signed-in Torn API key.");
        TornApiClient.validateKey(apiKey);
        waitForSlot();
        HttpURLConnection c=(HttpURLConnection)new URL(BACKEND_URL).openConnection();
        try{
            c.setRequestMethod("POST");c.setConnectTimeout(12000);c.setReadTimeout(22000);c.setUseCaches(false);c.setDoOutput(true);
            c.setRequestProperty("Content-Type","text/plain;charset=UTF-8");c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent",USER_AGENT);
            byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);try(OutputStream out=c.getOutputStream()){out.write(bytes);}
            int code=c.getResponseCode();InputStream in=code>=400?c.getErrorStream():c.getInputStream();String raw=in==null?"":read(in);
            JSONObject result;try{result=new JSONObject(raw);}catch(Exception e){throw new IOException("Developer backend returned an unreadable response.");}
            if(code<200||code>=300)throw new IOException("Developer backend HTTP "+code+".");
            if(!result.optBoolean("ok",false))throw new IOException(result.optString("error",fallback));
            return result;
        }finally{c.disconnect();}
    }

    private static synchronized void waitForSlot(){long now=System.currentTimeMillis();long wait=Math.max(0L,nextRequestAtMs-now);if(wait>0)try{Thread.sleep(wait);}catch(InterruptedException e){Thread.currentThread().interrupt();}nextRequestAtMs=System.currentTimeMillis()+3000L;}
    private static String read(InputStream input)throws IOException{try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString(StandardCharsets.UTF_8.name());}}
}
