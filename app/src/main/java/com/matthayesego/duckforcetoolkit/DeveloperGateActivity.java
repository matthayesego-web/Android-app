package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.UUID;

/** Hidden developer entry. Access is granted only to backend-authorized, verified Torn player IDs. */
public class DeveloperGateActivity extends Activity {
    private DeveloperSessionStore sessions;

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        sessions=new DeveloperSessionStore(this);
        checkAccess();
    }

    private void checkAccess(){
        DeveloperSessionStore.Session current=sessions.load();
        if(current!=null&&DeveloperBackendClient.isConfigured()){
            showState("Checking developer access…",TornFcaUi.GOLD,null);
            new Thread(()->{
                try{DeveloperBackendClient.developerSession(current.token);runOnUiThread(this::openPanel);}
                catch(Exception e){sessions.clear();runOnUiThread(this::verifyCurrentTornId);}
            },"TornFCA-DeveloperSessionCheck").start();
            return;
        }
        verifyCurrentTornId();
    }

    private void verifyCurrentTornId(){
        String apiKey=new SecureApiKeyStore(this).load();
        if(apiKey==null||apiKey.trim().isEmpty()){
            showState("Reconnect your Torn API key first. Developer access is tied to your verified Torn player ID.",TornFcaUi.RED,this::finish);
            return;
        }
        if(!DeveloperBackendClient.isConfigured()){
            showState("Developer Backend is not configured in this build.",TornFcaUi.RED,this::finish);
            return;
        }
        showState("Verifying your Torn player ID…",TornFcaUi.GOLD,null);
        new Thread(()->{
            try{
                JSONObject response=DeveloperBackendClient.developerIdLogin(apiKey,deviceId());
                saveSession(response);
                runOnUiThread(this::openPanel);
            }catch(Exception e){
                String m=e.getMessage()==null?"This Torn player ID is not authorized for the Developer Console.":e.getMessage();
                runOnUiThread(()->showState(m,TornFcaUi.RED,this::verifyCurrentTornId));
            }
        },"TornFCA-DeveloperIdLogin").start();
    }

    private void saveSession(JSONObject response)throws Exception{
        JSONObject dev=response.optJSONObject("developer");
        String token=response.optString("developer_session","");
        String username=dev==null?"Developer":dev.optString("username","Developer");
        String role=dev==null?"developer":dev.optString("role","developer");
        long expires=response.optLong("expires_at",0L);
        sessions.save(token,username,role,expires);
    }

    private String deviceId(){
        SharedPreferences p=getSharedPreferences("tornfca_developer_device_v1",MODE_PRIVATE);
        String id=p.getString("device_id","");
        if(id==null||id.isBlank()){
            id=UUID.randomUUID().toString();
            p.edit().putString("device_id",id).apply();
        }
        return id;
    }

    private void showState(String message,int color,Runnable retry){
        ScrollView shell=TornFcaUi.shell(this);
        LinearLayout root=TornFcaUi.root(this,shell);
        TornFcaUi.header(this,root,"More","Developer Console","Hidden entry • verified Torn ID access");
        LinearLayout card=TornFcaUi.card(this,"DEVELOPER","Developer access",message,color==TornFcaUi.RED?TornFcaUi.RED:TornFcaUi.GOLD);
        TextView note=TornFcaUi.text(this,"The TornFCA owner ID is always Root. Additional IDs may be authorized by Root from inside the Developer Console.",12,TornFcaUi.MUTED,false);
        note.setGravity(Gravity.CENTER);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);np.topMargin=TornFcaUi.dp(this,10);card.addView(note,np);
        if(retry!=null){
            Button b=TornFcaUi.button(this,color==TornFcaUi.RED?"Try Again":"Continue",TornFcaUi.GOLD);
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,48));bp.topMargin=TornFcaUi.dp(this,12);card.addView(b,bp);b.setOnClickListener(v->retry.run());
        }
        TornFcaUi.add(this,root,card);
        TextView footer=TornFcaUi.footer(this,"No developer username, password or authenticator is required in the current Beta access mode. Hardened MFA infrastructure remains available for later reactivation.\n\nTornFCA v"+TornFcaBrand.VERSION);
        root.addView(footer,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(shell);shell.requestApplyInsets();
    }

    private void openPanel(){
        DeveloperSessionStore.Session session=sessions.load();
        if(session==null){verifyCurrentTornId();return;}
        Intent i=new Intent(this,DeveloperPanelActivity.class);
        String key=new SecureApiKeyStore(this).load();
        FactionScopeCache.Scope scope=key==null?null:FactionScopeCache.load(this,key);
        if(scope!=null){
            i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_ID,scope.factionId);
            i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_NAME,scope.factionName);
            i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_API,scope.factionApiAccess);
            i.putExtra(DeveloperConsoleActivity.EXTRA_POSITION,scope.position);
        }
        i.putExtra(DeveloperPanelActivity.EXTRA_DEVELOPER_ROLE,session.role);
        i.putExtra(DeveloperPanelActivity.EXTRA_DEVELOPER_USERNAME,session.username);
        startActivity(i);finish();
    }
}
