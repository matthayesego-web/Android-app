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

/** Authenticated TornFCA Beta feedback transport. API keys are used only for the current request. */
public final class FeedbackBackendClient {
    private static final String BACKEND_URL=BuildConfig.FEEDBACK_BACKEND_URL==null?"":BuildConfig.FEEDBACK_BACKEND_URL.trim();
    private static final String USER_AGENT="TornFCA/"+TornFcaBrand.VERSION+" Android Feedback";
    private static long nextRequestAtMs=0L;

    private FeedbackBackendClient(){}

    public static boolean isConfigured(){return BACKEND_URL.startsWith("https://")&&!BACKEND_URL.contains("###");}
    public static String configuredUrl(){return BACKEND_URL;}

    public static JSONObject submit(String apiKey,String category,String title,String message,String screen)throws IOException{
        JSONObject body=request("submit",apiKey);
        put(body,"category",category);
        put(body,"title",title);
        put(body,"message",message);
        put(body,"app_version",BuildConfig.VERSION_NAME);
        put(body,"version_code",BuildConfig.VERSION_CODE);
        put(body,"screen",screen);
        put(body,"platform","Android");
        JSONObject response=postChecked(body,"Unable to submit feedback.");
        JSONObject feedback=response.optJSONObject("feedback");
        return feedback==null?new JSONObject():feedback;
    }

    public static JSONArray mine(String apiKey)throws IOException{
        JSONObject response=postChecked(request("mine",apiKey),"Unable to load your feedback history.");
        JSONArray rows=response.optJSONArray("feedback");
        return rows==null?new JSONArray():rows;
    }

    private static JSONObject request(String action,String apiKey){
        JSONObject body=new JSONObject();
        put(body,"action",action);
        put(body,"apiKey",apiKey==null?"":apiKey.trim());
        return body;
    }

    private static void put(JSONObject o,String key,Object value){try{o.put(key,value==null?"":value);}catch(Exception ignored){}}

    private static JSONObject postChecked(JSONObject body,String fallback)throws IOException{
        if(!isConfigured())throw new IOException("TornFCA feedback backend is not configured in this build.");
        String apiKey=body.optString("apiKey","");
        if(apiKey.isEmpty())throw new IOException("Signed-in Torn API key required.");
        TornApiClient.validateKey(apiKey);
        waitForSlot();
        HttpURLConnection c=(HttpURLConnection)new URL(BACKEND_URL).openConnection();
        try{
            c.setRequestMethod("POST");
            c.setConnectTimeout(12000);
            c.setReadTimeout(22000);
            c.setUseCaches(false);
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type","text/plain;charset=UTF-8");
            c.setRequestProperty("Accept","application/json");
            c.setRequestProperty("User-Agent",USER_AGENT);
            byte[] payload=body.toString().getBytes(StandardCharsets.UTF_8);
            try(OutputStream out=c.getOutputStream()){out.write(payload);}
            int code=c.getResponseCode();
            InputStream in=code>=400?c.getErrorStream():c.getInputStream();
            String raw=in==null?"":read(in);
            JSONObject response;
            try{response=new JSONObject(raw);}catch(Exception e){throw new IOException("Feedback backend returned an unreadable response.");}
            if(code<200||code>=300)throw new IOException("Feedback backend HTTP "+code+".");
            if(!response.optBoolean("ok",false))throw new IOException(response.optString("error",fallback));
            return response;
        }finally{c.disconnect();}
    }

    private static synchronized void waitForSlot(){
        long now=System.currentTimeMillis(),wait=Math.max(0L,nextRequestAtMs-now);
        if(wait>0)try{Thread.sleep(wait);}catch(InterruptedException e){Thread.currentThread().interrupt();}
        nextRequestAtMs=System.currentTimeMillis()+2500L;
    }

    private static String read(InputStream input)throws IOException{
        try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}
