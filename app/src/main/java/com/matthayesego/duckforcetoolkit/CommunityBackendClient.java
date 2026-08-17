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

/** Tenant-scoped TornFCA community transport for faction chat, moderation, training library and FCM device registration. */
public final class CommunityBackendClient {
    private static final String BACKEND_URL=BuildConfig.COMMUNITY_BACKEND_URL==null?"":BuildConfig.COMMUNITY_BACKEND_URL.trim();
    private static final String USER_AGENT="TornFCA/"+TornFcaBrand.VERSION+" Android Community";
    private static long nextRequestAtMs=0L;
    private CommunityBackendClient(){}
    public static boolean isConfigured(){return BACKEND_URL.startsWith("https://")&&!BACKEND_URL.contains("###");}
    public static String configuredUrl(){return BACKEND_URL;}
    public static JSONObject config(String key)throws IOException{return postChecked(request("config",key),"Unable to read community configuration.");}
    public static JSONArray getChatMessages(String key,String channel)throws IOException{JSONObject b=request("chat_list",key);put(b,"channel",channel);JSONObject r=postChecked(b,"Unable to load faction chat.");JSONArray a=r.optJSONArray("messages");return a==null?new JSONArray():a;}
    public static JSONObject sendChatMessage(String key,String channel,String message)throws IOException{JSONObject b=request("chat_send",key);put(b,"channel",channel);put(b,"message",message);return postChecked(b,"Unable to send faction chat message.");}
    public static JSONObject reportChatMessage(String key,String messageId,String reason)throws IOException{JSONObject b=request("chat_report",key);put(b,"messageId",messageId);put(b,"reason",reason);return postChecked(b,"Unable to report this message.");}
    public static JSONObject trainingLibrary(String key)throws IOException{return postChecked(request("training_library",key),"Unable to load faction training library.");}
    public static JSONObject saveTrainingRules(String key,String statGainTarget,String xanaxTarget,String notes)throws IOException{JSONObject b=request("training_rules_save",key);put(b,"statGainTarget",statGainTarget);put(b,"xanaxTarget",xanaxTarget);put(b,"notes",notes);return postChecked(b,"Unable to save faction training rules.");}
    public static JSONObject saveTrainingGuide(String key,String id,String title,String category,String body)throws IOException{JSONObject b=request("training_guide_save",key);put(b,"id",id);put(b,"title",title);put(b,"category",category);put(b,"body",body);return postChecked(b,"Unable to save faction training guide.");}
    public static JSONObject archiveTrainingGuide(String key,String id)throws IOException{JSONObject b=request("training_guide_archive",key);put(b,"id",id);return postChecked(b,"Unable to archive faction training guide.");}
    public static void registerPushToken(String key,String token,String prefsJson)throws IOException{JSONObject b=request("push_register",key);put(b,"token",token);put(b,"preferences",prefsJson);put(b,"platform","android");postChecked(b,"Unable to register this device for push notifications.");}
    public static void unregisterPushToken(String key,String token)throws IOException{JSONObject b=request("push_unregister",key);put(b,"token",token);postChecked(b,"Unable to unregister this device from push notifications.");}
    public static void sendPushTest(String key)throws IOException{postChecked(request("push_test",key),"Unable to send a cloud push test.");}
    public static void publishAnnouncement(String key,String title,String message)throws IOException{JSONObject b=request("announcement_push",key);put(b,"title",title);put(b,"message",message);postChecked(b,"Unable to send announcement push.");}
    private static JSONObject request(String action,String key){JSONObject b=new JSONObject();put(b,"action",action);put(b,"apiKey",key==null?"":key);return b;}
    private static void put(JSONObject o,String k,Object v){try{o.put(k,v==null?"":v);}catch(Exception ignored){}}
    private static JSONObject postChecked(JSONObject body,String fallback)throws IOException{if(!isConfigured())throw new IOException("TornFCA community backend is not configured yet.");String key=body.optString("apiKey","");if(!key.isEmpty())TornApiClient.validateKey(key);waitForSlot();HttpURLConnection c=(HttpURLConnection)new URL(BACKEND_URL).openConnection();try{c.setRequestMethod("POST");c.setConnectTimeout(12000);c.setReadTimeout(22000);c.setUseCaches(false);c.setDoOutput(true);c.setRequestProperty("Content-Type","text/plain;charset=UTF-8");c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent",USER_AGENT);byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);try(OutputStream out=c.getOutputStream()){out.write(bytes);}int code=c.getResponseCode();InputStream in=code>=400?c.getErrorStream():c.getInputStream();String raw=in==null?"":read(in);JSONObject r;try{r=new JSONObject(raw);}catch(Exception e){throw new IOException("Community backend returned an unreadable response.");}if(code<200||code>=300)throw new IOException("Community backend HTTP "+code+".");if(!r.optBoolean("ok",false))throw new IOException(r.optString("error",fallback));return r;}finally{c.disconnect();}}
    private static synchronized void waitForSlot(){long now=System.currentTimeMillis();long wait=Math.max(0L,nextRequestAtMs-now);if(wait>0)try{Thread.sleep(wait);}catch(InterruptedException e){Thread.currentThread().interrupt();}nextRequestAtMs=System.currentTimeMillis()+2500L;}
    private static String read(InputStream input)throws IOException{try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString(StandardCharsets.UTF_8.name());}}
}
