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

/** Free personal War Prep. Local state is immediate; shared state is faction+war scoped when Community is live. */
public class WarPrepActivity extends Activity {
    private static final String PREFS="tornfca_war_prep_v2";
    private SecureApiKeyStore keyStore;
    private AuthSession session;
    private WarStatus war=WarStatus.none();
    private JSONObject self=new JSONObject();
    private JSONArray checklistItems=defaultItems();
    private boolean sharedConfigured;

    @Override protected void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);keyStore=new SecureApiKeyStore(this);showLoading();load();}

    private void showLoading(){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"War Center","My War Prep","Checking your personal readiness, faction checklist and current ranked-war timing…");TornFcaUi.add(this,r,TornFcaUi.card(this,"PERSONAL","Building your war-prep snapshot","Every ranked war receives its own checklist state. When the faction Community backend is connected, leadership can see readiness submitted by TornFCA users only.",TornFcaUi.RED));setContentView(s);s.requestApplyInsets();}

    private void load(){String key=keyStore.load();if(key==null||key.isBlank()){renderError("Reconnect your Torn API key to use My War Prep.");return;}new Thread(()->{try{AuthSession verified=TornApiClient.cachedSession(key);if(verified==null)verified=TornApiClient.authenticate(key);session=verified;war=WarStatus.from(TornApiClient.getJson("/faction/wars",key),verified.factionId);self=loadSelf(key);loadShared(key);persistChecklistMetadata();runOnUiThread(this::render);syncSharedAsync();}catch(Exception e){renderError(e.getMessage()==null?"Unable to build your war-prep snapshot.":e.getMessage());}},"TornFCA-WarPrep").start();}

    private JSONObject loadSelf(String key)throws Exception{
        try{return TornApiClient.getJson("/user?selections=bars,cooldowns,travel,refills,organizedcrime",key);}
        catch(Exception combined){JSONObject out=new JSONObject();int loaded=0;loaded+=copy(out,safe("/user/bars",key),"bars");loaded+=copy(out,safe("/user/cooldowns",key),"cooldowns");loaded+=copy(out,safe("/user/travel",key),"travel");loaded+=copy(out,safe("/user/refills",key),"refills");loaded+=copy(out,safe("/user/organizedcrime",key),"organizedCrime");if(loaded==0)throw combined;return out;}
    }
    private JSONObject safe(String path,String key){try{return TornApiClient.getJson(path,key);}catch(Exception ignored){return new JSONObject();}}
    private int copy(JSONObject into,JSONObject from,String key){JSONObject value=from==null?null:from.optJSONObject(key);if(value==null)return 0;try{into.put(key,value);return 1;}catch(Exception ignored){return 0;}}

    private void loadShared(String key){
        sharedConfigured=CommunityBackendClient.isConfigured()&&war.warId>0;
        if(!sharedConfigured)return;
        try{
            JSONObject state=CommunityBackendClient.warPrepState(key,war.warId);if(state==null)return;
            JSONArray items=state.optJSONArray("items");if(items!=null&&items.length()>0)checklistItems=items;
            JSONObject status=state.optJSONObject("status"),completed=status==null?null:status.optJSONObject("completed");
            if(completed!=null){for(int i=0;i<checklistItems.length();i++){JSONObject item=checklistItems.optJSONObject(i);if(item==null)continue;String id=item.optString("id","item"+(i+1));if(completed.optBoolean(id,false))prefs().edit().putBoolean(prefix()+id,true).apply();}}
        }catch(Exception ignored){sharedConfigured=false;}
    }

    private JSONArray defaultItems(){JSONArray a=new JSONArray();addDefault(a,"item1","Reviewed current war mode and timing");addDefault(a,"item2","Checked travel");addDefault(a,"item3","Checked cooldowns & refills");addDefault(a,"item4","Reviewed faction resources");addDefault(a,"item5","Reviewed current instructions");return a;}
    private void addDefault(JSONArray a,String id,String title){JSONObject o=new JSONObject();try{o.put("id",id);o.put("title",title);a.put(o);}catch(Exception ignored){}}
    private String cycle(){long token=war.warId!=0?war.warId:(war.start>0?war.start:0);return token==0?"general":"war"+token;}
    private String prefix(){return"p"+session.playerId+"_f"+session.factionId+"_"+cycle()+"_";}
    private SharedPreferences prefs(){return getSharedPreferences(PREFS,MODE_PRIVATE);}
    private void persistChecklistMetadata(){if(session==null)return;prefs().edit().putInt(prefix()+"item_count",Math.max(1,Math.min(8,checklistItems.length()))).apply();}
    private boolean done(String id){return prefs().getBoolean(prefix()+id,false);}
    private void toggle(String id){prefs().edit().putBoolean(prefix()+id,!done(id)).apply();render();syncSharedAsync();}

    private void render(){long now=System.currentTimeMillis()/1000L;ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"War Center","My War Prep",session.factionName+" • "+session.playerName+" • personal readiness");

        int warAccent=war.isLive(now)?TornFcaUi.RED:war.isUpcoming(now)?TornFcaUi.GOLD:TornFcaUi.BORDER;TornFcaUi.addSection(this,r,"RANKED WAR TIMING");TornFcaUi.add(this,r,TornFcaUi.card(this,war.isLive(now)?"RANKED WAR LIVE":war.isUpcoming(now)?"UPCOMING RANKED WAR":"RANKED WAR STATUS",war.headline(now),war.detail(now),warAccent));

        JSONObject bars=self.optJSONObject("bars"),cooldowns=self.optJSONObject("cooldowns"),travel=self.optJSONObject("travel"),refills=self.optJSONObject("refills"),oc=self.optJSONObject("organizedCrime");
        TornFcaUi.addSection(this,r,"PERSONAL READINESS");
        if(bars!=null){String body=bar(bars,"energy","Energy")+"\n"+bar(bars,"life","Life")+"\n"+bar(bars,"nerve","Nerve")+"\n"+bar(bars,"happy","Happy");TornFcaUi.add(this,r,TornFcaUi.card(this,"BARS","Your current bars",body,TornFcaUi.GREEN));}
        if(cooldowns!=null){int drug=cooldowns.optInt("drug",0),medical=cooldowns.optInt("medical",0),booster=cooldowns.optInt("booster",0);int accent=(drug==0&&medical==0)?TornFcaUi.GREEN:TornFcaUi.GOLD;TornFcaUi.add(this,r,TornFcaUi.card(this,"COOLDOWNS","Current cooldowns","Drug: "+duration(drug)+"\nMedical: "+duration(medical)+"\nBooster: "+duration(booster),accent));}
        if(travel!=null){int left=travel.optInt("time_left",0);String destination=travel.optString("destination","");String body=left>0?"Traveling to "+(destination.isBlank()?"destination":destination)+" • "+duration(left)+" remaining":"Torn does not report you as currently traveling.";TornFcaUi.add(this,r,TornFcaUi.card(this,"TRAVEL",left>0?"Travel in progress":"Travel clear",body,left>0?TornFcaUi.GOLD:TornFcaUi.GREEN));}
        if(refills!=null){TornFcaUi.add(this,r,TornFcaUi.card(this,"REFILLS","Available refills","Energy: "+available(refills.optBoolean("energy",false))+"\nNerve: "+available(refills.optBoolean("nerve",false))+"\nToken: "+available(refills.optBoolean("token",false)),TornFcaUi.BLUE));}
        if(oc!=null&&oc.length()>0){String name=oc.optString("name","Organized Crime"),status=oc.optString("status","Current assignment");TornFcaUi.add(this,r,TornFcaUi.card(this,"MY OC",name,status+"\nCheck My Day/OC before changing your normal routine around faction warfare.",TornFcaUi.PURPLE));}

        TornFcaUi.addSection(this,r,"MY PREP CHECKLIST");int complete=0;
        for(int i=0;i<checklistItems.length();i++){JSONObject item=checklistItems.optJSONObject(i);if(item==null)continue;String id=item.optString("id","item"+(i+1)),title=item.optString("title","Faction War Prep item");if(done(id))complete++;addCheck(r,id,title,"Confirm this requirement for the current ranked-war cycle. Faction leadership can customize these checklist items when shared War Prep is connected.");}
        int total=Math.max(1,checklistItems.length());String sync=sharedConfigured?"Shared with faction leadership for this war. Only TornFCA users who open/sync War Prep appear in leadership status.":"Stored locally on this device. Shared leadership status activates when the Community backend is connected.";TornFcaUi.add(this,r,TornFcaUi.card(this,"PREP STATUS",complete+" / "+total+" complete",(complete==total?"Your checklist for this ranked-war cycle is complete.\n":"")+sync,complete==total?TornFcaUi.GREEN:TornFcaUi.GOLD));

        if(MemberPresentationPolicy.leadershipVisible(this,session.position)){LinearLayout manage=TornFcaUi.card(this,"LEADERSHIP","Faction War Prep Management","Customize this faction's checklist and review readiness submitted by TornFCA users for the current/upcoming war.",TornFcaUi.PURPLE);addLaunch(manage,"Open War Prep Management",WarPrepLeadershipActivity.class,TornFcaUi.PURPLE);TornFcaUi.add(this,r,manage);}

        TornFcaUi.addSection(this,r,"WAR SHORTCUTS");LinearLayout quick=TornFcaUi.card(this,"MEMBER TOOLS","Continue from here","Open the detailed member tools without exposing leadership-only data.",TornFcaUi.BLUE);addLaunch(quick,"Open War Center",WarHubActivity.class,TornFcaUi.RED);addLaunch(quick,"Open My Day",MemberDailyActivity.class,TornFcaUi.GREEN);addLaunch(quick,"Open Faction Resources",FactionResourcesActivity.class,TornFcaUi.GOLD);addLaunch(quick,"Open Faction Chat",FactionChatActivity.class,TornFcaUi.BLUE);TornFcaUi.add(this,r,quick);

        TornFcaUi.add(this,r,TornFcaUi.card(this,"SCOPE","Per-war and per-faction","Checklist state is isolated by player + verified faction + ranked-war cycle. A new ranked war automatically starts a fresh checklist. Faction-specific checklist options never carry into another faction.",TornFcaUi.BORDER));
        r.addView(TornFcaUi.footer(this,"War Prep resets by ranked-war ID. Shared status represents TornFCA users only, not the faction's full roster."));setContentView(s);s.requestApplyInsets();}

    private void syncSharedAsync(){if(!sharedConfigured||session==null||war.warId<=0)return;String key=keyStore.load();if(key==null||key.isBlank())return;JSONObject completed=completedJson();new Thread(()->{try{CommunityBackendClient.saveWarPrepStatus(key,war.warId,completed);}catch(Exception ignored){}},"TornFCA-WarPrepSync").start();}
    private JSONObject completedJson(){JSONObject out=new JSONObject();for(int i=0;i<checklistItems.length();i++){JSONObject item=checklistItems.optJSONObject(i);if(item==null)continue;String id=item.optString("id","item"+(i+1));try{out.put(id,done(id));}catch(Exception ignored){}}return out;}
    private void addCheck(LinearLayout r,String id,String title,String body){boolean complete=done(id);LinearLayout c=TornFcaUi.card(this,complete?"COMPLETE":"TO DO",title,body,complete?TornFcaUi.GREEN:TornFcaUi.BORDER);Button b=TornFcaUi.button(this,complete?"Mark not complete":"Mark complete",complete?TornFcaUi.BORDER:TornFcaUi.GREEN);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,42));p.topMargin=TornFcaUi.dp(this,9);c.addView(b,p);b.setOnClickListener(v->{toggle(id);Toast.makeText(this,complete?"Prep item reopened.":"Prep item completed.",Toast.LENGTH_SHORT).show();});TornFcaUi.add(this,r,c);}
    private void addLaunch(LinearLayout c,String label,Class<?> activity,int accent){Button b=TornFcaUi.button(this,label,accent);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,43));p.topMargin=TornFcaUi.dp(this,8);c.addView(b,p);b.setOnClickListener(v->startActivity(new Intent(this,activity)));}
    private String bar(JSONObject bars,String key,String label){JSONObject b=bars.optJSONObject(key);if(b==null)return label+": unavailable";return label+": "+b.optInt("current",0)+" / "+b.optInt("maximum",0);}
    private String duration(int seconds){return seconds<=0?"Clear":WarStatus.duration(seconds);}
    private String available(boolean value){return value?"Available":"Used / unavailable";}
    private void renderError(String message){runOnUiThread(()->{ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"War Center","My War Prep","Readiness unavailable");TornFcaUi.add(this,r,TornFcaUi.card(this,"CONNECTION","Unable to load",message,TornFcaUi.RED));setContentView(s);s.requestApplyInsets();});}
}
