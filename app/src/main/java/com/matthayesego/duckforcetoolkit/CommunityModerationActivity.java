package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

/** Hidden owner-only review queue for TornFCA community reports. Server authorization is still enforced independently. */
public class CommunityModerationActivity extends Activity {
    private String key;

    @Override protected void onCreate(Bundle b){super.onCreate(b);key=new SecureApiKeyStore(this).load();renderLoading();load();}

    private void load(){
        if(key==null||key.isBlank()){renderError("Reconnect your Torn API key before opening moderation.");return;}
        new Thread(()->{try{JSONArray reports=CommunityBackendClient.moderationReports(key);runOnUiThread(()->render(reports));}catch(Exception e){renderError(e.getMessage()==null?"Unable to load moderation reports.":e.getMessage());}},"TornFCA-ModerationLoad").start();
    }

    private void render(JSONArray reports){
        ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);
        TornFcaUi.header(this,r,"Developer Panel","Community Moderation","Central TornFCA review queue • "+reports.length()+" open report"+(reports.length()==1?"":"s"));
        if(reports.length()==0){TornFcaUi.add(this,r,TornFcaUi.card(this,"ALL CLEAR","No open reports","Faction chat reports that still need TornFCA review will appear here.",TornFcaUi.GREEN));}
        for(int i=0;i<reports.length();i++){
            JSONObject row=reports.optJSONObject(i);if(row==null)continue;
            String reportId=row.optString("id","");String author=row.optString("author_name","Member");String reporter=row.optString("reporter_name","Member");String channel=row.optString("channel","general");String reason=row.optString("reason","No reason supplied");String snapshot=row.optString("message_snapshot","");long created=row.optLong("created_at",0L);int factionId=row.optInt("faction_id",0);
            String when=created>0?DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(created*1000L)):"Unknown time";
            String body="Faction #"+factionId+" • "+channel+" • "+when+"\nReported by: "+reporter+"\nReason: "+reason+"\n\nMessage from "+author+":\n"+snapshot;
            LinearLayout card=TornFcaUi.card(this,"OPEN REPORT",author,body,TornFcaUi.GOLD);
            LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
            Button dismiss=TornFcaUi.button(this,"Dismiss",TornFcaUi.BORDER);dismiss.setOnClickListener(v->confirmResolve(reportId,"dismiss"));
            Button remove=TornFcaUi.button(this,"Remove Message",TornFcaUi.RED);remove.setOnClickListener(v->confirmResolve(reportId,"remove_message"));
            LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,TornFcaUi.dp(this,44),1f),b=new LinearLayout.LayoutParams(0,TornFcaUi.dp(this,44),1f);b.leftMargin=TornFcaUi.dp(this,7);actions.addView(dismiss,a);actions.addView(remove,b);
            LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,44));ap.topMargin=TornFcaUi.dp(this,10);card.addView(actions,ap);TornFcaUi.add(this,r,card);
        }
        Button refresh=TornFcaUi.button(this,"Refresh Reports",TornFcaUi.BLUE);refresh.setOnClickListener(v->{renderLoading();load();});r.addView(refresh,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,48)));
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);fp.topMargin=TornFcaUi.dp(this,12);r.addView(TornFcaUi.footer(this,"Removing a message redacts it from faction chat and closes open reports for that same message. Dismissal closes only the selected report. Every resolution records the verified moderator identity and timestamp."),fp);
        setContentView(s);s.requestApplyInsets();
    }

    private void confirmResolve(String reportId,String action){
        boolean remove="remove_message".equals(action);new AlertDialog.Builder(this).setTitle(remove?"Remove reported message?":"Dismiss this report?").setMessage(remove?"The message will be replaced with a moderation notice and all open reports for that message will be resolved.":"The report will be closed without removing the message.").setNegativeButton("Cancel",null).setPositiveButton(remove?"Remove":"Dismiss",(d,w)->resolve(reportId,action)).show();
    }

    private void resolve(String reportId,String action){new Thread(()->{try{CommunityBackendClient.resolveModerationReport(key,reportId,action);runOnUiThread(()->{Toast.makeText(this,"Moderation action saved.",Toast.LENGTH_SHORT).show();renderLoading();load();});}catch(Exception e){String m=e.getMessage()==null?"Moderation action failed.":e.getMessage();runOnUiThread(()->Toast.makeText(this,m,Toast.LENGTH_LONG).show());}},"TornFCA-ModerationResolve").start();}

    private void renderLoading(){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Developer Panel","Community Moderation","Loading open reports…");TornFcaUi.add(this,r,TornFcaUi.card(this,"LOADING","Checking report queue","Verifying the developer account and loading unresolved community reports.",TornFcaUi.BLUE));setContentView(s);s.requestApplyInsets();}
    private void renderError(String message){runOnUiThread(()->{ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Developer Panel","Community Moderation","Unable to open review queue");TornFcaUi.add(this,r,TornFcaUi.card(this,"MODERATION UNAVAILABLE","Could not load reports",message,TornFcaUi.RED));Button retry=TornFcaUi.button(this,"Retry",TornFcaUi.GOLD);retry.setOnClickListener(v->{renderLoading();load();});r.addView(retry,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,48)));setContentView(s);s.requestApplyInsets();});}
}
