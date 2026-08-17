package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

/** Free member training resources plus the currently verified faction's private training library. */
public class TrainingCenterActivity extends Activity {
    private SecureApiKeyStore keyStore;
    private AuthSession session;
    private JSONObject library=new JSONObject();
    private String backendMessage="";

    @Override protected void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);keyStore=new SecureApiKeyStore(this);showLoading();load();}

    private void showLoading(){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Member Center","Training Center","Loading training resources and your faction library…");TornFcaUi.add(this,r,TornFcaUi.card(this,"TRAINING","Building your library","Universal TornFCA guides stay available everywhere. Faction rules and custom guides are loaded only for your currently verified faction.",TornFcaUi.GREEN));setContentView(s);s.requestApplyInsets();}

    private void load(){String key=keyStore.load();if(key==null||key.isBlank()){renderError("Reconnect your Torn API key to load faction training resources.");return;}new Thread(()->{try{AuthSession verified=TornApiClient.cachedSession(key);if(verified==null)verified=TornApiClient.authenticate(key);session=verified;if(CommunityBackendClient.isConfigured()){try{library=CommunityBackendClient.trainingLibrary(key);}catch(Exception e){backendMessage=e.getMessage()==null?"Faction training library is temporarily unavailable.":e.getMessage();}}else backendMessage="Faction training library is not connected to the community backend in this build.";runOnUiThread(this::render);}catch(Exception e){renderError(e.getMessage()==null?"Unable to verify your Torn faction.":e.getMessage());}},"TornFCA-TrainingCenter").start();}

    private void render(){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Member Center","Training Center",session.factionName+" • guides, expectations and progression help");
        TornFcaUi.addSection(this,r,"YOUR FACTION EXPECTATIONS");JSONObject rules=library.optJSONObject("trainingRules");if(rules==null||rules.length()==0){String body=backendMessage.isBlank()?"Your faction has not published training targets yet.":backendMessage;TornFcaUi.add(this,r,TornFcaUi.card(this,"FACTION RULES","No published training rules",body,TornFcaUi.GOLD));}else addRules(r,rules);
        if(isLeader(session.position)){Button manage=TornFcaUi.button(this,"Manage faction training",TornFcaUi.GOLD);manage.setOnClickListener(v->startActivity(new Intent(this,TrainingAdminActivity.class)));TornFcaUi.add(this,r,manage);}

        TornFcaUi.addSection(this,r,"FACTION GUIDE LIBRARY");JSONArray guides=library.optJSONArray("guides");if(guides==null||guides.length()==0)TornFcaUi.add(this,r,TornFcaUi.card(this,"FACTION LIBRARY","No custom guides published yet","When your faction leadership publishes guides, they appear here only while you are a verified member of that faction.",TornFcaUi.BLUE));else for(int i=0;i<guides.length();i++){JSONObject g=guides.optJSONObject(i);if(g!=null)addGuide(r,g);}

        TornFcaUi.addSection(this,r,"TORNFCA STARTER GUIDES");
        addBuiltIn(r,"HAPPY JUMP","Happy Jump — starter checklist","A happy jump combines stored energy with temporarily elevated happiness, then spends that energy in the gym before the next happiness reset. Xanax currently adds 250 energy and 75 happiness; an Erotic DVD adds 2,500 happiness. Check the in-game happy timer before boosting above your normal cap, and remember that a drug overdose can wipe energy, nerve and happiness. Your faction's own timing, budget and stat targets should take priority over any generic recipe.",TornFcaUi.PURPLE);
        addBuiltIn(r,"DAILY TRAINING","Xanax & energy discipline","Xanax currently provides 250 energy with a 360–480 minute drug cooldown. Natural energy also refills over time, so capped energy is usually wasted regeneration unless you are deliberately saving it for a faction event. Use your faction's published Xanax target as the expectation rather than assuming every faction wants the same daily count.",TornFcaUi.GREEN);
        addBuiltIn(r,"PROGRESSION","Build consistency before complexity","Regular training, avoiding wasted energy, following your faction's war/chain priorities and tracking progress over time usually matter more than chasing a complicated routine you cannot sustain. Use the faction library for local expectations and ask leadership when a guide conflicts with a current war or training event.",TornFcaUi.BLUE);
        TornFcaUi.add(this,r,TornFcaUi.card(this,"SCOPE","Faction-local by design","Universal TornFCA guides remain in the app. Faction rules and custom guides are keyed to the verified faction ID; they do not travel with your account when you change factions.",TornFcaUi.GOLD));
        r.addView(TornFcaUi.footer(this,"Training guidance is informational; Torn game mechanics and faction policy can change."));setContentView(s);s.requestApplyInsets();}

    private void addRules(LinearLayout r,JSONObject rules){String gain=rules.optString("stat_gain_target","").trim(),xanax=rules.optString("xanax_target","").trim(),notes=rules.optString("notes","").trim(),by=rules.optString("updated_by_name","").trim();long at=rules.optLong("updated_at",0L);StringBuilder b=new StringBuilder();b.append("Stat gain target: ").append(gain.isBlank()?"Not set":gain).append("\nXanax target: ").append(xanax.isBlank()?"Not set":xanax);if(!notes.isBlank())b.append("\n\n").append(notes);if(!by.isBlank())b.append("\n\nUpdated by ").append(by);if(at>0)b.append(" • ").append(DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(at*1000L)));TornFcaUi.add(this,r,TornFcaUi.card(this,"FACTION RULES","Current training expectations",b.toString(),TornFcaUi.GOLD));}
    private void addGuide(LinearLayout r,JSONObject g){String title=g.optString("title","Faction guide"),category=g.optString("category","Guide"),body=g.optString("body","");String author=g.optString("author_name","");long updated=g.optLong("updated_at",0L);String suffix="";if(!author.isBlank())suffix="\n\nPublished by "+author;if(updated>0)suffix+=(suffix.isBlank()?"\n\n":" • ")+DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(updated*1000L));TornFcaUi.add(this,r,TornFcaUi.card(this,category,title,body+suffix,TornFcaUi.BLUE));}
    private void addBuiltIn(LinearLayout r,String eye,String title,String body,int accent){TornFcaUi.add(this,r,TornFcaUi.card(this,eye,title,body,accent));}
    private static boolean isLeader(String position){String n=position==null?"":position.toLowerCase().replace("-","").replace("_","").replace(" ","");return"leader".equals(n)||"coleader".equals(n);}
    private void renderError(String message){runOnUiThread(()->{ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Member Center","Training Center","Training resources unavailable");TornFcaUi.add(this,r,TornFcaUi.card(this,"CONNECTION","Unable to load",message,TornFcaUi.RED));setContentView(s);s.requestApplyInsets();});}
}
