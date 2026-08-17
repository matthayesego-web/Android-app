package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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

/** Free faction-scoped onboarding and resource library for ordinary members. */
public class FactionResourcesActivity extends Activity {
    private static final String PREFS="tornfca_onboarding_v1";
    private SecureApiKeyStore keyStore;
    private AuthSession session;
    private JSONObject library=new JSONObject();
    private String backendMessage="";

    @Override protected void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);keyStore=new SecureApiKeyStore(this);showLoading();load();}

    private void showLoading(){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Member Center","Faction Resources","Loading your current faction's onboarding and guide library…");TornFcaUi.add(this,r,TornFcaUi.card(this,"FACTION LOCAL","Loading resources","Your checklist and library are scoped to the faction Torn currently verifies for this account.",TornFcaUi.BLUE));setContentView(s);s.requestApplyInsets();}

    private void load(){String key=keyStore.load();if(key==null||key.isBlank()){renderError("Reconnect your Torn API key to open faction resources.");return;}new Thread(()->{try{AuthSession verified=TornApiClient.cachedSession(key);if(verified==null)verified=TornApiClient.authenticate(key);session=verified;if(CommunityBackendClient.isConfigured()){try{library=CommunityBackendClient.trainingLibrary(key);}catch(Exception e){backendMessage=e.getMessage()==null?"Faction library is temporarily unavailable.":e.getMessage();}}else backendMessage="Faction shared resources are not connected in this build.";runOnUiThread(this::render);}catch(Exception e){renderError(e.getMessage()==null?"Unable to verify your current faction.":e.getMessage());}},"TornFCA-FactionResources").start();}

    private String prefix(){return"p"+session.playerId+"_f"+session.factionId+"_";}
    private SharedPreferences prefs(){return getSharedPreferences(PREFS,MODE_PRIVATE);}
    private boolean done(String id){return prefs().getBoolean(prefix()+id,false);}
    private void toggle(String id){prefs().edit().putBoolean(prefix()+id,!done(id)).apply();render();}

    private void render(){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Member Center","Faction Resources",session.factionName+" • onboarding, local guides and quick links");

        TornFcaUi.addSection(this,r,"NEW MEMBER CHECKLIST");
        addChecklist(r,"profile","1. Know your faction","Review your current faction, position and member-safe roster so you know where you fit.");
        addChecklist(r,"training","2. Read training expectations","Open Training Center and review the stat-gain/Xanax expectations and any exceptions your faction has published.");
        addChecklist(r,"oc","3. Check your organized crime","Confirm your current OC assignment, slot, readiness time and required item status.");
        addChecklist(r,"war","4. Know war & chain expectations","Review My Day, My War and Chain Status before a faction event so you know what needs your attention.");
        addChecklist(r,"guides","5. Read faction guides","Use the library below for any faction-specific onboarding, training, trading, war-prep or community instructions.");
        int complete=0;String[] ids={"profile","training","oc","war","guides"};for(String id:ids)if(done(id))complete++;
        TornFcaUi.add(this,r,TornFcaUi.card(this,"ONBOARDING",complete+" / 5 complete",complete==5?"Your local onboarding checklist is complete. You can reset individual items any time by tapping them again.":"This checklist is stored only on this device and is separate for each player + faction combination.",complete==5?TornFcaUi.GREEN:TornFcaUi.GOLD));

        TornFcaUi.addSection(this,r,"QUICK LINKS");LinearLayout quick=TornFcaUi.card(this,"MEMBER TOOLS","Jump to the essentials","Use these shortcuts while working through your faction's onboarding or guides.",TornFcaUi.BLUE);addLaunch(quick,"Open My Day",MemberDailyActivity.class,TornFcaUi.GREEN);addLaunch(quick,"Open Training Center",TrainingCenterActivity.class,TornFcaUi.PURPLE);addLaunch(quick,"Open Faction Directory",MemberDirectoryActivity.class,TornFcaUi.BLUE);addLaunch(quick,"Open My War",MemberWarActivity.class,TornFcaUi.RED);TornFcaUi.add(this,r,quick);

        TornFcaUi.addSection(this,r,"FACTION GUIDE LIBRARY");JSONArray guides=library.optJSONArray("guides");if(guides==null||guides.length()==0){String body=backendMessage.isBlank()?"Your faction has not published any custom guides yet.":backendMessage+"\n\nYour local onboarding checklist and TornFCA member tools remain available.";TornFcaUi.add(this,r,TornFcaUi.card(this,"FACTION LIBRARY","No shared guides available",body,TornFcaUi.GOLD));}else for(int i=0;i<guides.length();i++){JSONObject g=guides.optJSONObject(i);if(g!=null)addGuide(r,g);}

        if(isLeader(session.position)){LinearLayout manage=TornFcaUi.card(this,"LEADERSHIP","Manage faction guides","Publish onboarding, training, trading, war-prep or other faction-specific guides through the existing faction guide manager.",TornFcaUi.GOLD);Button b=TornFcaUi.button(this,"Open Guide Management",TornFcaUi.GOLD);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));p.topMargin=TornFcaUi.dp(this,10);manage.addView(b,p);b.setOnClickListener(v->startActivity(new Intent(this,TrainingAdminActivity.class)));TornFcaUi.add(this,r,manage);}

        TornFcaUi.add(this,r,TornFcaUi.card(this,"TENANT SCOPE","Resources belong to the faction","The shared guide library is loaded only after Torn verifies your current faction. If you leave or change factions, the old faction library is no longer returned and your new faction receives its own separate onboarding checklist.",TornFcaUi.GREEN));
        r.addView(TornFcaUi.footer(this,"TornFCA v"+TornFcaBrand.VERSION+" • faction-local resources"));setContentView(s);s.requestApplyInsets();}

    private void addChecklist(LinearLayout r,String id,String title,String body){boolean complete=done(id);LinearLayout c=TornFcaUi.card(this,complete?"COMPLETE":"TO DO",title,body,complete?TornFcaUi.GREEN:TornFcaUi.BORDER);Button b=TornFcaUi.button(this,complete?"Mark not complete":"Mark complete",complete?TornFcaUi.BORDER:TornFcaUi.GREEN);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,42));p.topMargin=TornFcaUi.dp(this,9);c.addView(b,p);b.setOnClickListener(v->{toggle(id);Toast.makeText(this,complete?"Checklist item reopened.":"Checklist item completed.",Toast.LENGTH_SHORT).show();});TornFcaUi.add(this,r,c);}
    private void addLaunch(LinearLayout c,String label,Class<?> activity,int accent){Button b=TornFcaUi.button(this,label,accent);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,43));p.topMargin=TornFcaUi.dp(this,8);c.addView(b,p);b.setOnClickListener(v->startActivity(new Intent(this,activity)));}
    private void addGuide(LinearLayout r,JSONObject g){String title=g.optString("title","Faction guide"),category=g.optString("category","Guide"),body=g.optString("body","").trim(),author=g.optString("author_name","").trim();long updated=g.optLong("updated_at",0L);StringBuilder copy=new StringBuilder(body);if(!author.isBlank())copy.append("\n\nPublished by ").append(author);if(updated>0)copy.append(author.isBlank()?"\n\n":" • ").append(DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(updated*1000L)));TornFcaUi.add(this,r,TornFcaUi.card(this,category,title,copy.toString(),TornFcaUi.BLUE));}
    private static boolean isLeader(String position){String n=position==null?"":position.toLowerCase().replace("-","").replace("_","").replace(" ","");return"leader".equals(n)||"coleader".equals(n);}
    private void renderError(String message){runOnUiThread(()->{ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Member Center","Faction Resources","Resources unavailable");TornFcaUi.add(this,r,TornFcaUi.card(this,"CONNECTION","Unable to load",message,TornFcaUi.RED));setContentView(s);s.requestApplyInsets();});}
}
