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

/** Tenant-scoped TornFCA community transport for chat, moderation, training, War Prep and FCM registration. */
public final class CommunityBackendClient {
    private static final String BACKEND_URL=BuildConfig.COMMUNITY_BACKEND_URL==null?"":BuildConfig.COMMUNITY_BACKEND_URL.trim();
    private static final String USER_AGENT="TornFCA/"+TornFcaBrand.VERSION+" Android Community";

    public static final class ChatSnapshot {
        public final int playerId;
        public final String playerName;
        public final int factionId;
        public final String factionName;
        public final String position;
        public final JSONArray messages;

        ChatSnapshot(JSONObject response){
            JSONObject user=response==null?null:response.optJSONObject("user");
            playerId=user==null?0:user.optInt("id",0);
            playerName=user==null?"Member":user.optString("name","Member");
            factionId=user==null?0:user.optInt("faction_id",0);
            factionName=user==null?"Faction":user.optString("faction_name","Faction");
            position=user==null?"":user.optString("position","");
            JSONArray rows=response==null?null:response.optJSONArray("messages");
            messages=rows==null?new JSONArray():rows;
        }
    }

    private CommunityBackendClient(){}
    public static boolean isConfigured(){return BACKEND_URL.startsWith("https://")&&!BACKEND_URL.contains("###");}
    public static String configuredUrl(){return BACKEND_URL;}
    public static JSONObject config(String key)throws IOException{return postChecked(request("config",key),"Unable to read community configuration.");}

    /** Chat reads use their own lightweight lane. The backend applies a short faction-membership cache for these reads. */
    public static ChatSnapshot getChatSnapshot(String key,String channel)throws IOException{
        JSONObject b=request("chat_list",key);put(b,"channel",channel);
        return new ChatSnapshot(postChecked(b,"Unable to load faction chat.",false));
    }
    public static JSONArray getChatMessages(String key,String channel)throws IOException{return getChatSnapshot(key,channel).messages;}

    public static JSONObject sendChatMessage(String key,String channel,String message)throws IOException{JSONObject b=request("chat_send",key);put(b,"channel",channel);put(b,"message",message);return postChecked(b,"Unable to send faction chat message.");}
    public static JSONObject reportChatMessage(String key,String messageId,String reason)throws IOException{JSONObject b=request("chat_report",key);put(b,"messageId",messageId);put(b,"reason",reason);return postChecked(b,"Unable to report this message.");}
    public static JSONArray moderationReports(String key)throws IOException{JSONObject r=postChecked(request("moderation_list",key),"Unable to load moderation reports.");JSONArray a=r.optJSONArray("reports");return a==null?new JSONArray():a;}
    public static JSONObject resolveModerationReport(String key,String reportId,String resolution)throws IOException{JSONObject b=request("moderation_resolve",key);put(b,"reportId",reportId);put(b,"resolution",resolution);return postChecked(b,"Unable to resolve moderation report.");}
    public static JSONObject trainingLibrary(String key)throws IOException{return postChecked(request("training_library",key),"Unable to load faction training library.");}
    public static JSONObject saveTrainingRules(String key,String statGainTarget,String xanaxTarget,String notes)throws IOException{JSONObject b=request("training_rules_save",key);put(b,"statGainTarget",statGainTarget);put(b,"xanaxTarget",xanaxTarget);put(b,"notes",notes);return postChecked(b,"Unable to save faction training rules.");}
    public static JSONObject saveTrainingGuide(String key,String id,String title,String category,String body)throws IOException{JSONObject b=request("training_guide_save",key);put(b,"id",id);put(b,"title",title);put(b,"category",category);put(b,"body",body);return postChecked(b,"Unable to save faction training guide.");}
    public static JSONObject archiveTrainingGuide(String key,String id)throws IOException{JSONObject b=request("training_guide_archive",key);put(b,"id",id);return postChecked(b,"Unable to archive faction training guide.");}
    public static JSONObject warPrepState(String key,int warId)throws IOException{JSONObject b=request("warprep_state",key);put(b,"warId",warId);return postChecked(b,"Unable to load shared War Prep.").optJSONObject("warPrep");}
    public static JSONObject saveWarPrepStatus(String key,int warId,JSONObject completed)throws IOException{JSONObject b=request("warprep_status_save",key);put(b,"warId",warId);put(b,"completed",completed==null?new JSONObject():completed);return postChecked(b,"Unable to sync War Prep status.").optJSONObject("warPrep");}
    public static JSONObject warPrepLeadership(String key,int warId)throws IOException{JSONObject b=request("warprep_leadership",key);put(b,"warId",warId);return postChecked(b,"Unable to load faction War Prep status.").optJSONObject("warPrep");}
    public static JSONObject saveWarPrepConfig(String key,JSONArray items)throws IOException{JSONObject b=request("warprep_config_save",key);put(b,"items",items==null?new JSONArray():items);return postChecked(b,"Unable to save faction War Prep options.").optJSONObject("warPrep");}
    public static void registerPushToken(String key,String token,String prefsJson)throws IOException{JSONObject b=request("push_register",key);put(b,"token",token);put(b,"preferences",prefsJson);put(b,"platform","android");postChecked(b,"Unable to register this device for push notifications.");}
    public static void unregisterPushToken(String key,String token)throws IOException{JSONObject b=request("push_unregister",key);put(b,"token",token);postChecked(b,"Unable to unregister this device from push notifications.");}
    public static void sendPushTest(String key)throws IOException{postChecked(request("push_test",key),"Unable to send a cloud push test.");}
    public static void publishAnnouncement(String key,String title,String message)throws IOException{JSONObject b=request("announcement_push",key);put(b,"title",title);put(b,"message",message);postChecked(b,"Unable to send announcement push.");}
    public static JSONObject publishBankingRequest(String key,String summary,String note)throws IOException{JSONObject b=request("banking_request_push",key);put(b,"summary",summary);put(b,"note",note);return postChecked(b,"Unable to alert faction leadership about this banking request.");}

    private static JSONObject request(String action,String key){JSONObject b=new JSONObject();put(b,"action",action);put(b,"apiKey",key==null?"":key);return b;}
    private static void put(JSONObject o,String k,Object v){try{o.put(k,v==null?"":v);}catch(Exception ignored){}}
    private static JSONObject postChecked(JSONObject body,String fallback)throws IOException{return postChecked(body,fallback,true);}
    private static JSONObject postChecked(JSONObject body,String fallback,boolean governed)throws IOException{
        if(!isConfigured())throw new IOException("TornFCA community backend is not configured yet.");
        String key=body.optString("apiKey","");if(!key.isEmpty())TornApiClient.validateKey(key);
        if(governed)BackendRequestGovernor.acquire();
        HttpURLConnection c=(HttpURLConnection)new URL(BACKEND_URL).openConnection();
        try{
            c.setRequestMethod("POST");c.setConnectTimeout(10000);c.setReadTimeout(20000);c.setUseCaches(false);c.setDoOutput(true);
            c.setRequestProperty("Content-Type","text/plain;charset=UTF-8");c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent",USER_AGENT);
            byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);try(OutputStream out=c.getOutputStream()){out.write(bytes);}
            int code=c.getResponseCode();InputStream in=code>=400?c.getErrorStream():c.getInputStream();String raw=in==null?"":read(in);JSONObject r;
            try{r=new JSONObject(raw);}catch(Exception e){throw new IOException("Community backend returned an unreadable response.");}
            if(code<200||code>=300)throw new IOException("Community backend HTTP "+code+".");
            if(!r.optBoolean("ok",false))throw new IOException(r.optString("error",fallback));
            return r;
        }finally{c.disconnect();}
    }
    private static String read(InputStream input)throws IOException{try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString(StandardCharsets.UTF_8.name());}}
}
