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

import org.json.JSONObject;

/** Free personal war-prep dashboard using only the signed-in member's Torn data plus current faction war state. */
public class WarPrepActivity extends Activity {
    private static final String PREFS="tornfca_war_prep_v1";
    private SecureApiKeyStore keyStore;
    private AuthSession session;
    private WarStatus war=WarStatus.none();
    private JSONObject self=new JSONObject();

    @Override protected void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);keyStore=new SecureApiKeyStore(this);showLoading();load();}

    private void showLoading(){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Member Center","My War Prep","Checking your personal readiness and current ranked-war timing…");TornFcaUi.add(this,r,TornFcaUi.card(this,"MEMBER SAFE","Building your war-prep snapshot","This screen uses only your own Torn bars, cooldowns, travel, refills and OC context plus the current faction war state.",TornFcaUi.RED));setContentView(s);s.requestApplyInsets();}

    private void load(){String key=keyStore.load();if(key==null||key.isBlank()){renderError("Reconnect your Torn API key to use My War Prep.");return;}new Thread(()->{try{AuthSession verified=TornApiClient.cachedSession(key);if(verified==null)verified=TornApiClient.authenticate(key);session=verified;war=WarStatus.from(TornApiClient.getJson("/faction/wars",key),verified.factionId);self=loadSelf(key);runOnUiThread(this::render);}catch(Exception e){renderError(e.getMessage()==null?"Unable to build your war-prep snapshot.":e.getMessage());}},"TornFCA-WarPrep").start();}

    private JSONObject loadSelf(String key)throws Exception{
        try{return TornApiClient.getJson("/user?selections=bars,cooldowns,travel,refills,organizedcrime",key);}
        catch(Exception combined){JSONObject out=new JSONObject();int loaded=0;loaded+=copy(out,safe("/user/bars",key),"bars");loaded+=copy(out,safe("/user/cooldowns",key),"cooldowns");loaded+=copy(out,safe("/user/travel",key),"travel");loaded+=copy(out,safe("/user/refills",key),"refills");loaded+=copy(out,safe("/user/organizedcrime",key),"organizedCrime");if(loaded==0)throw combined;return out;}
    }
    private JSONObject safe(String path,String key){try{return TornApiClient.getJson(path,key);}catch(Exception ignored){return new JSONObject();}}
    private int copy(JSONObject into,JSONObject from,String key){JSONObject value=from==null?null:from.optJSONObject(key);if(value==null)return 0;try{into.put(key,value);return 1;}catch(Exception ignored){return 0;}}

    private String cycle(){long token=war.warId!=0?war.warId:(war.start>0?war.start:0);return token==0?"general":"war"+token;}
    private String prefix(){return"p"+session.playerId+"_f"+session.factionId+"_"+cycle()+"_";}
    private SharedPreferences prefs(){return getSharedPreferences(PREFS,MODE_PRIVATE);}
    private boolean done(String id){return prefs().getBoolean(prefix()+id,false);}
    private void toggle(String id){prefs().edit().putBoolean(prefix()+id,!done(id)).apply();render();}

    private void render(){long now=System.currentTimeMillis()/1000L;ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Member Center","My War Prep",session.factionName+" • "+session.playerName+" • personal readiness");

        int warAccent=war.isLive(now)?TornFcaUi.RED:war.isUpcoming(now)?TornFcaUi.GOLD:TornFcaUi.BORDER;TornFcaUi.addSection(this,r,"WAR TIMING");TornFcaUi.add(this,r,TornFcaUi.card(this,war.isLive(now)?"WAR LIVE":war.isUpcoming(now)?"UPCOMING WAR":"WAR STATUS",war.headline(now),war.detail(now),warAccent));

        JSONObject bars=self.optJSONObject("bars"),cooldowns=self.optJSONObject("cooldowns"),travel=self.optJSONObject("travel"),refills=self.optJSONObject("refills"),oc=self.optJSONObject("organizedCrime");
        TornFcaUi.addSection(this,r,"PERSONAL READINESS");
        if(bars!=null){String body=bar(bars,"energy","Energy")+"\n"+bar(bars,"life","Life")+"\n"+bar(bars,"nerve","Nerve")+"\n"+bar(bars,"happy","Happy");TornFcaUi.add(this,r,TornFcaUi.card(this,"BARS","Your current bars",body,TornFcaUi.GREEN));}
        if(cooldowns!=null){int drug=cooldowns.optInt("drug",0),medical=cooldowns.optInt("medical",0),booster=cooldowns.optInt("booster",0);int accent=(drug==0&&medical==0)?TornFcaUi.GREEN:TornFcaUi.GOLD;TornFcaUi.add(this,r,TornFcaUi.card(this,"COOLDOWNS","Current cooldowns","Drug: "+duration(drug)+"\nMedical: "+duration(medical)+"\nBooster: "+duration(booster),accent));}
        if(travel!=null){int left=travel.optInt("time_left",0);String destination=travel.optString("destination","");String body=left>0?"Traveling to "+(destination.isBlank()?"destination":destination)+" • "+duration(left)+" remaining":"Torn does not report you as currently traveling.";TornFcaUi.add(this,r,TornFcaUi.card(this,"TRAVEL",left>0?"Travel in progress":"Travel clear",body,left>0?TornFcaUi.GOLD:TornFcaUi.GREEN));}
        if(refills!=null){TornFcaUi.add(this,r,TornFcaUi.card(this,"REFILLS","Available refills","Energy: "+available(refills.optBoolean("energy",false))+"\nNerve: "+available(refills.optBoolean("nerve",false))+"\nToken: "+available(refills.optBoolean("token",false)),TornFcaUi.BLUE));}
        if(oc!=null&&oc.length()>0){String name=oc.optString("name","Organized Crime"),status=oc.optString("status","Current assignment");TornFcaUi.add(this,r,TornFcaUi.card(this,"MY OC",name,status+"\nCheck My Day/OC before changing your normal routine around a war.",TornFcaUi.PURPLE));}

        TornFcaUi.addSection(this,r,"MY PREP CHECKLIST");addCheck(r,"timing","Reviewed war timing","I checked whether the war is upcoming/live and know when I need to be available.");addCheck(r,"travel","Checked travel","I checked my travel state and faction instructions so I am not unexpectedly unavailable.");addCheck(r,"cooldowns","Checked cooldowns & refills","I reviewed my personal cooldowns, bars and refill availability before the event.");addCheck(r,"resources","Reviewed faction resources","I checked my faction's War Prep/onboarding guides for its specific expectations.");addCheck(r,"instructions","Reviewed current instructions","I checked faction announcements/chat/leadership instructions instead of assuming a universal war routine.");
        String[] ids={"timing","travel","cooldowns","resources","instructions"};int complete=0;for(String id:ids)if(done(id))complete++;TornFcaUi.add(this,r,TornFcaUi.card(this,"PREP STATUS",complete+" / 5 complete",complete==5?"Your personal checklist for this war cycle is complete.":"These are confirmation steps, not proof of faction compliance. Your faction's live instructions always take priority.",complete==5?TornFcaUi.GREEN:TornFcaUi.GOLD));

        TornFcaUi.addSection(this,r,"WAR SHORTCUTS");LinearLayout quick=TornFcaUi.card(this,"MEMBER TOOLS","Continue from here","Open the detailed member tools without exposing leadership-only data.",TornFcaUi.BLUE);addLaunch(quick,"Open My War",MemberWarActivity.class,TornFcaUi.RED);addLaunch(quick,"Open My Day",MemberDailyActivity.class,TornFcaUi.GREEN);addLaunch(quick,"Open Faction Resources",FactionResourcesActivity.class,TornFcaUi.GOLD);addLaunch(quick,"Open Faction Chat",FactionChatActivity.class,TornFcaUi.BLUE);TornFcaUi.add(this,r,quick);

        TornFcaUi.add(this,r,TornFcaUi.card(this,"SCOPE","No universal war requirements invented","TornFCA reports your current data and gives you a checklist, but it does not assume how much energy, medical cooldown, Xanax, travel or equipment your faction requires. Put faction-specific requirements in Faction Resources and current leadership notices.",TornFcaUi.BORDER));
        r.addView(TornFcaUi.footer(this,"Checklist state is stored locally for this player + faction + war cycle."));setContentView(s);s.requestApplyInsets();}

    private void addCheck(LinearLayout r,String id,String title,String body){boolean complete=done(id);LinearLayout c=TornFcaUi.card(this,complete?"COMPLETE":"TO DO",title,body,complete?TornFcaUi.GREEN:TornFcaUi.BORDER);Button b=TornFcaUi.button(this,complete?"Mark not complete":"Mark complete",complete?TornFcaUi.BORDER:TornFcaUi.GREEN);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,42));p.topMargin=TornFcaUi.dp(this,9);c.addView(b,p);b.setOnClickListener(v->{toggle(id);Toast.makeText(this,complete?"Prep item reopened.":"Prep item completed.",Toast.LENGTH_SHORT).show();});TornFcaUi.add(this,r,c);}
    private void addLaunch(LinearLayout c,String label,Class<?> activity,int accent){Button b=TornFcaUi.button(this,label,accent);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,43));p.topMargin=TornFcaUi.dp(this,8);c.addView(b,p);b.setOnClickListener(v->startActivity(new Intent(this,activity)));}
    private String bar(JSONObject bars,String key,String label){JSONObject b=bars.optJSONObject(key);if(b==null)return label+": unavailable";return label+": "+b.optInt("current",0)+" / "+b.optInt("maximum",0);}
    private String duration(int seconds){return seconds<=0?"Clear":WarStatus.duration(seconds);}
    private String available(boolean value){return value?"Available":"Used / unavailable";}
    private void renderError(String message){runOnUiThread(()->{ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Member Center","My War Prep","Readiness unavailable");TornFcaUi.add(this,r,TornFcaUi.card(this,"CONNECTION","Unable to load",message,TornFcaUi.RED));setContentView(s);s.requestApplyInsets();});}
}
