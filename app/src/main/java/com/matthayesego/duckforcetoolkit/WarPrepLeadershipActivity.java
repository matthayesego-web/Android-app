package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

/** Faction-scoped War Prep configuration and TornFCA-user readiness review for leadership. */
public class WarPrepLeadershipActivity extends Activity {
    private SecureApiKeyStore keyStore;
    private AuthSession session;
    private WarStatus war=WarStatus.none();
    private JSONObject shared;
    private JSONArray items=new JSONArray();

    @Override protected void onCreate(Bundle b){super.onCreate(b);keyStore=new SecureApiKeyStore(this);showLoading();load();}

    private void showLoading(){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Operations","War Prep Management","Verifying current leadership and loading this faction's War Prep environment…");TornFcaUi.add(this,r,TornFcaUi.card(this,"FACTION SCOPED","Loading War Prep","Every request is re-verified against your current Torn faction before any checklist or member status is returned.",TornFcaUi.PURPLE));setContentView(s);s.requestApplyInsets();}

    private void load(){String key=keyStore.load();if(key==null||key.isBlank()){renderError("Reconnect your Torn API key first.");return;}new Thread(()->{try{session=TornApiClient.authenticateFreshFaction(key);if(!AccessPolicy.isLeaderPosition(session.position)){renderError("Faction Leader or Co-leader access is required.");return;}war=WarStatus.from(TornApiClient.getJson("/faction/wars",key),session.factionId);if(CommunityBackendClient.isConfigured()&&war.warId>0)shared=CommunityBackendClient.warPrepLeadership(key,war.warId);if(shared!=null&&shared.optJSONArray("items")!=null)items=shared.optJSONArray("items");else items=defaultItems();runOnUiThread(this::render);}catch(Exception e){renderError(e.getMessage()==null?"Unable to load faction War Prep.":e.getMessage());}},"TornFCA-WarPrepLeadership").start();}

    private JSONArray defaultItems(){JSONArray a=new JSONArray();add(a,"item1","Reviewed current war mode and timing");add(a,"item2","Checked travel");add(a,"item3","Checked cooldowns & refills");add(a,"item4","Reviewed faction resources");add(a,"item5","Reviewed current instructions");return a;}
    private void add(JSONArray a,String id,String title){JSONObject o=new JSONObject();try{o.put("id",id);o.put("title",title);a.put(o);}catch(Exception ignored){}}

    private void render(){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Operations","War Prep Management",session.factionName+" • "+(war.warId>0?"ranked war #"+war.warId:"no current/upcoming ranked war"));
        TornFcaUi.add(this,r,TornFcaUi.card(this,"TENANT ISOLATION","This faction only","Checklist configuration and readiness rows are keyed to the freshly verified faction ID. A member in another faction cannot read or write this environment.",TornFcaUi.GREEN));
        TornFcaUi.addSection(this,r,"TORNFCA USER READINESS");
        JSONArray members=shared==null?null:shared.optJSONArray("members");int seen=members==null?0:members.length();TornFcaUi.add(this,r,TornFcaUi.card(this,"APP USERS THIS WAR",String.valueOf(seen),"This count includes only faction members who opened/synced War Prep in TornFCA for this ranked war. It is not the faction's full roster and does not mark non-users as incomplete.",TornFcaUi.BLUE));
        if(war.warId<=0)TornFcaUi.add(this,r,TornFcaUi.card(this,"NO WAR CYCLE","No shared readiness to review","Shared completion rows are deliberately tied to a ranked-war ID so every new war starts clean.",TornFcaUi.GOLD));
        else if(!CommunityBackendClient.isConfigured())TornFcaUi.add(this,r,TornFcaUi.card(this,"BACKEND PENDING","Shared readiness unavailable","Local member checklists still work. Deploy Community Backend v1.7.0 to enable cross-device leadership status and faction customization.",TornFcaUi.GOLD));
        else if(seen==0)TornFcaUi.add(this,r,TornFcaUi.card(this,"NO APP USERS YET","No War Prep submissions","Members appear here after they open My War Prep in TornFCA during this war cycle.",TornFcaUi.BORDER));
        else for(int i=0;i<seen;i++){JSONObject m=members.optJSONObject(i);if(m==null)continue;JSONObject completed=m.optJSONObject("completed");int count=0;for(int j=0;j<items.length();j++){JSONObject item=items.optJSONObject(j);if(item!=null&&completed!=null&&completed.optBoolean(item.optString("id","item"+(j+1)),false))count++;}TornFcaUi.add(this,r,TornFcaUi.card(this,"TORNFCA USER",m.optString("player_name","Member"),count+" / "+items.length()+" checklist items complete",count==items.length()&&items.length()>0?TornFcaUi.GREEN:TornFcaUi.BORDER));}
        TornFcaUi.addSection(this,r,"FACTION CHECKLIST");TornFcaUi.add(this,r,TornFcaUi.card(this,"CUSTOM OPTIONS","One checklist per faction","These options persist for your faction, while every member's completion state resets automatically when Torn reports a new ranked-war ID. Maximum 8 items.",TornFcaUi.PURPLE));
        LinearLayout editorCard=TornFcaUi.card(this,"EDIT CHECKLIST","One item per line","Keep each requirement short and clear. Changes apply only to this verified faction.",TornFcaUi.BLUE);EditText editor=new EditText(this);editor.setTextColor(TornFcaUi.TEXT);editor.setHintTextColor(TornFcaUi.MUTED);editor.setText(itemsText());editor.setGravity(Gravity.TOP);editor.setMinLines(6);editor.setPadding(TornFcaUi.dp(this,12),TornFcaUi.dp(this,10),TornFcaUi.dp(this,12),TornFcaUi.dp(this,10));editor.setBackground(TornFcaUi.rounded(this,TornFcaUi.BG,TornFcaUi.BORDER,12));LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,190));ep.topMargin=TornFcaUi.dp(this,10);editorCard.addView(editor,ep);Button save=TornFcaUi.button(this,"Save Faction Checklist",TornFcaUi.PURPLE);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));sp.topMargin=TornFcaUi.dp(this,10);editorCard.addView(save,sp);save.setOnClickListener(v->saveConfig(editor,save));TornFcaUi.add(this,r,editorCard);
        Button refresh=TornFcaUi.button(this,"Refresh War Prep Status",TornFcaUi.BLUE);refresh.setOnClickListener(v->{showLoading();load();});r.addView(refresh,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46)));r.addView(TornFcaUi.footer(this,"Leadership view = TornFCA users only • verified faction + ranked-war ID isolation"));setContentView(s);s.requestApplyInsets();}

    private String itemsText(){StringBuilder b=new StringBuilder();for(int i=0;i<items.length();i++){JSONObject item=items.optJSONObject(i);if(item==null)continue;if(b.length()>0)b.append('\n');b.append(item.optString("title",""));}return b.toString();}
    private void saveConfig(EditText editor,Button button){String raw=editor.getText().toString();String[] lines=raw.split("\\r?\\n");JSONArray out=new JSONArray();for(String line:lines){String v=line.trim();if(v.isEmpty())continue;if(out.length()>=8)break;out.put(v);}if(out.length()==0){Toast.makeText(this,"Add at least one checklist item.",Toast.LENGTH_SHORT).show();return;}if(!CommunityBackendClient.isConfigured()){Toast.makeText(this,"Community backend must be deployed before faction customization can sync.",Toast.LENGTH_LONG).show();return;}String key=keyStore.load();button.setEnabled(false);new Thread(()->{try{JSONObject saved=CommunityBackendClient.saveWarPrepConfig(key,out);JSONArray next=saved==null?null:saved.optJSONArray("items");if(next!=null)items=next;runOnUiThread(()->{Toast.makeText(this,"Faction War Prep checklist saved.",Toast.LENGTH_SHORT).show();button.setEnabled(true);showLoading();load();});}catch(Exception e){runOnUiThread(()->{Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();button.setEnabled(true);});}},"TornFCA-WarPrepConfig").start();}
    private void renderError(String message){runOnUiThread(()->{ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Operations","War Prep Management","Unavailable");TornFcaUi.add(this,r,TornFcaUi.card(this,"ACCESS","Unable to load",message,TornFcaUi.RED));setContentView(s);s.requestApplyInsets();});}
}
