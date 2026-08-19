package com.matthayesego.duckforcetoolkit;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Transport for TornFCA's developer control plane and public product policy. */
public final class DeveloperBackendClient {
    private static final String BACKEND_URL=BuildConfig.DEVELOPER_BACKEND_URL==null?"":BuildConfig.DEVELOPER_BACKEND_URL.trim();
    private static final String USER_AGENT="TornFCA/"+TornFcaBrand.VERSION+" Android Developer";

    private DeveloperBackendClient(){}

    public static boolean isConfigured(){return BACKEND_URL.startsWith("https://")&&!BACKEND_URL.contains("###");}
    public static String configuredUrl(){return BACKEND_URL;}

    /** Non-secret app policy. Any verified Torn user may read it. */
    public static JSONObject publicConfig(String apiKey)throws IOException{return postChecked(request("public_config",apiKey),true,"Unable to read TornFCA remote policy.");}

    /** Root control-plane calls remain Torn-owner verified until cross-service delegation is explicitly enabled. */
    public static JSONObject status(String apiKey)throws IOException{return postChecked(request("status",apiKey),true,"Unable to read developer backend status.");}
    public static JSONObject readConfig(String apiKey)throws IOException{return postChecked(request("config_read",apiKey),true,"Unable to read developer configuration.");}
    public static JSONObject writeConfig(String apiKey,String developerPassword,JSONObject config)throws IOException{JSONObject body=request("config_write",apiKey);put(body,"admin_password",developerPassword==null?"":developerPassword);try{body.put("config",config==null?new JSONObject():config);}catch(Exception ignored){}return postChecked(body,true,"Unable to update developer configuration.");}
    public static JSONObject readAudit(String apiKey,String developerPassword)throws IOException{JSONObject body=request("audit_list",apiKey);put(body,"admin_password",developerPassword==null?"":developerPassword);return postChecked(body,true,"Unable to read developer audit history.");}

    /** Beta gate: backend verifies the current Torn player ID and returns Root or Developer access. */
    public static JSONObject developerIdLogin(String apiKey,String deviceId)throws IOException{JSONObject body=request("developer_id_login",apiKey);put(body,"device_id",deviceId);return postChecked(body,true,"This Torn player ID is not authorized for the Developer Console.");}
    public static JSONObject idAccessList(String sessionToken)throws IOException{JSONObject body=plain("developer_id_access_list");put(body,"developer_session",sessionToken);return postChecked(body,false,"Unable to read authorized Torn IDs.");}
    public static JSONObject addIdAccess(String sessionToken,long playerId,String playerName)throws IOException{JSONObject body=plain("developer_id_access_add");put(body,"developer_session",sessionToken);put(body,"player_id",playerId);put(body,"player_name",playerName);return postChecked(body,false,"Unable to authorize that Torn ID.");}
    public static JSONObject removeIdAccess(String sessionToken,long playerId)throws IOException{JSONObject body=plain("developer_id_access_remove");put(body,"developer_session",sessionToken);put(body,"player_id",playerId);return postChecked(body,false,"Unable to remove that Torn ID.");}

    /** Hardened username/password/TOTP APIs are retained for later reactivation. */
    public static JSONObject developerLogin(String username,String password,String otp,String deviceId)throws IOException{JSONObject body=plain("developer_login");put(body,"username",username);put(body,"password",password);put(body,"otp",otp);put(body,"device_id",deviceId);return postChecked(body,false,"Developer authentication failed.");}
    public static JSONObject developerSession(String sessionToken)throws IOException{JSONObject body=plain("developer_session");put(body,"developer_session",sessionToken);return postChecked(body,false,"Developer session is no longer valid.");}
    public static JSONObject developerLogout(String sessionToken)throws IOException{JSONObject body=plain("developer_logout");put(body,"developer_session",sessionToken);return postChecked(body,false,"Unable to close developer session.");}
    public static JSONObject enrollmentBegin(String inviteCode,String password,String deviceId)throws IOException{JSONObject body=plain("developer_enroll_begin");put(body,"invite_code",inviteCode);put(body,"password",password);put(body,"device_id",deviceId);return postChecked(body,false,"Unable to begin developer enrollment.");}
    public static JSONObject enrollmentComplete(String enrollmentToken,String otp,String deviceId)throws IOException{JSONObject body=plain("developer_enroll_complete");put(body,"enrollment_token",enrollmentToken);put(body,"otp",otp);put(body,"device_id",deviceId);return postChecked(body,false,"Unable to complete developer enrollment.");}
    public static JSONObject accessList(String sessionToken)throws IOException{JSONObject body=plain("developer_access_list");put(body,"developer_session",sessionToken);return postChecked(body,false,"Unable to read developer access list.");}
    public static JSONObject createInvite(String sessionToken,String username,String displayName,String role)throws IOException{JSONObject body=plain("developer_invite_create");put(body,"developer_session",sessionToken);put(body,"username",username);put(body,"display_name",displayName);put(body,"role",role);return postChecked(body,false,"Unable to create developer invitation.");}
    public static JSONObject revokeAccess(String sessionToken,String developerId)throws IOException{JSONObject body=plain("developer_access_revoke");put(body,"developer_session",sessionToken);put(body,"developer_id",developerId);return postChecked(body,false,"Unable to revoke developer access.");}
    public static JSONObject resetEnrollment(String sessionToken,String developerId)throws IOException{JSONObject body=plain("developer_reset_enrollment");put(body,"developer_session",sessionToken);put(body,"developer_id",developerId);return postChecked(body,false,"Unable to reset developer enrollment.");}

    private static JSONObject request(String action,String apiKey){JSONObject body=plain(action);put(body,"apiKey",apiKey==null?"":apiKey);return body;}
    private static JSONObject plain(String action){JSONObject body=new JSONObject();put(body,"action",action);put(body,"version_code",BuildConfig.VERSION_CODE);put(body,"version_name",BuildConfig.VERSION_NAME);return body;}
    private static void put(JSONObject o,String key,Object value){try{o.put(key,value==null?"":value);}catch(Exception ignored){}}

    private static JSONObject postChecked(JSONObject body,boolean requireTornKey,String fallback)throws IOException{
        if(!isConfigured())throw new IOException("TornFCA developer backend is not configured in this build.");
        if(requireTornKey){String apiKey=body.optString("apiKey","");if(apiKey.isEmpty())throw new IOException("Reconnect your Torn API key before opening the developer console.");TornApiClient.validateKey(apiKey);}
        BackendRequestGovernor.acquire();JSONObject result=execute(body);
        if(!result.optBoolean("ok",false))throw new IOException(result.optString("error",fallback));
        return result;
    }

    /** Explicit redirect handling is required for Google Apps Script ContentService POST responses on some Android builds. */
    private static JSONObject execute(JSONObject body)throws IOException{
        URL url=new URL(BACKEND_URL);
        String method="POST";
        byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);
        for(int redirect=0;redirect<6;redirect++){
            HttpURLConnection c=(HttpURLConnection)url.openConnection();
            try{
                c.setInstanceFollowRedirects(false);
                c.setRequestMethod(method);
                c.setConnectTimeout(10000);
                c.setReadTimeout(18000);
                c.setUseCaches(false);
                c.setRequestProperty("Accept","application/json");
                c.setRequestProperty("User-Agent",USER_AGENT);
                if("POST".equals(method)){
                    c.setDoOutput(true);
                    c.setRequestProperty("Content-Type","text/plain;charset=UTF-8");
                    try(OutputStream out=c.getOutputStream()){out.write(bytes);}
                }
                int code=c.getResponseCode();
                if(code==301||code==302||code==303||code==307||code==308){
                    String location=c.getHeaderField("Location");
                    if(location==null||location.trim().isEmpty())throw new IOException("Developer backend redirect was missing its destination.");
                    url=new URL(url,location);
                    method=(code==307||code==308)?"POST":"GET";
                    continue;
                }
                InputStream in=code>=400?c.getErrorStream():c.getInputStream();
                String raw=in==null?"":read(in);
                JSONObject result;
                try{result=new JSONObject(raw);}catch(Exception e){throw new IOException("Developer backend returned an unreadable response.");}
                if(code<200||code>=300)throw new IOException(result.optString("error","Developer backend HTTP "+code+"."));
                return result;
            }finally{c.disconnect();}
        }
        throw new IOException("Developer backend redirected too many times.");
    }

    private static String read(InputStream input)throws IOException{try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString(StandardCharsets.UTF_8.name());}}
}
