package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

/** Free personal training progress. Baselines are local and scoped to the verified player + faction. */
public class TrainingProgressActivity extends Activity {
    private static final String PREFS="tornfca_training_progress_v1";
    private SecureApiKeyStore keyStore;
    private AuthSession session;
    private Snapshot current;
    private JSONObject factionRules;

    @Override protected void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);keyStore=new SecureApiKeyStore(this);showLoading();load();}

    private void showLoading(){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Member Center","My Training Progress","Reading your own Torn training totals…");TornFcaUi.add(this,r,TornFcaUi.card(this,"PERSONAL","Building your progress snapshot","TornFCA compares your current battle stats and Xanax total with a device-local baseline for this player and faction. No other member's private stats are read.",TornFcaUi.GREEN));setContentView(s);s.requestApplyInsets();}

    private void load(){String key=keyStore.load();if(key==null||key.isBlank()){renderError("Reconnect your Torn API key to view training progress.");return;}new Thread(()->{try{AuthSession verified=TornApiClient.cachedSession(key);if(verified==null)verified=TornApiClient.authenticate(key);session=verified;JSONObject battleRoot=TornApiClient.getJson("/user/battlestats",key);JSONObject battle=battleRoot.optJSONObject("battlestats");if(battle==null)throw new Exception("Torn did not return your battle stats. A Limited/Custom/Full key is required for this progress view.");JSONObject personal=TornApiClient.getJson("/user/personalstats?cat=drugs",key).optJSONObject("personalstats");JSONObject drugs=personal==null?null:personal.optJSONObject("drugs");current=Snapshot.from(battle,drugs);if(CommunityBackendClient.isConfigured())try{factionRules=CommunityBackendClient.trainingLibrary(key).optJSONObject("trainingRules");}catch(Exception ignored){}ensureBaseline();runOnUiThread(this::render);}catch(Exception e){renderError(e.getMessage()==null?"Unable to load your training progress.":e.getMessage());}},"TornFCA-TrainingProgress").start();}

    private String prefix(){return"p"+session.playerId+"_f"+session.factionId+"_";}
    private SharedPreferences prefs(){return getSharedPreferences(PREFS,MODE_PRIVATE);}
    private Baseline baseline(){SharedPreferences p=prefs();String x=prefix();long at=p.getLong(x+"at",0L);if(at<=0)return null;return new Baseline(at,p.getLong(x+"total",0),p.getLong(x+"strength",0),p.getLong(x+"defense",0),p.getLong(x+"speed",0),p.getLong(x+"dexterity",0),p.getInt(x+"xanax",0));}
    private void ensureBaseline(){if(baseline()==null)saveBaseline(current);}
    private void saveBaseline(Snapshot s){String x=prefix();prefs().edit().putLong(x+"at",System.currentTimeMillis()).putLong(x+"total",s.total).putLong(x+"strength",s.strength).putLong(x+"defense",s.defense).putLong(x+"speed",s.speed).putLong(x+"dexterity",s.dexterity).putInt(x+"xanax",s.xanax).apply();}

    private void render(){Baseline b=baseline();if(b==null||current==null){renderError("Training baseline is unavailable.");return;}ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Member Center","My Training Progress",session.factionName+" • "+session.playerName+" • private member view");
        long elapsed=Math.max(0L,System.currentTimeMillis()-b.at),days=Math.max(1L,(elapsed+86399999L)/86400000L);long gain=Math.max(0L,current.total-b.total);double pct=b.total>0?100d*gain/b.total:0d;int xanax=Math.max(0,current.xanax-b.xanax);double xanaxPerDay=xanax/(Math.max(1d,elapsed/86400000d));
        TornFcaUi.addSection(this,r,"SINCE YOUR FACTION BASELINE");String tracked=elapsed<3600000L?"Baseline created today":days+" day"+(days==1?"":"s")+" tracked";TornFcaUi.add(this,r,TornFcaUi.card(this,"PROGRESS",tracked,"Total battle-stat gain: "+signed(gain)+" ("+String.format(Locale.US,"%.2f",pct)+"%)\nXanax taken: +"+xanax+" • "+String.format(Locale.US,"%.2f",xanaxPerDay)+" / day\nBaseline: "+DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(b.at)),TornFcaUi.GREEN));
        TornFcaUi.addSection(this,r,"BATTLE STATS");TornFcaUi.add(this,r,TornFcaUi.card(this,"CURRENT TOTAL",number(current.total),"Strength "+number(current.strength)+" ("+signed(current.strength-b.strength)+")\nDefense "+number(current.defense)+" ("+signed(current.defense-b.defense)+")\nSpeed "+number(current.speed)+" ("+signed(current.speed-b.speed)+")\nDexterity "+number(current.dexterity)+" ("+signed(current.dexterity-b.dexterity)+")",TornFcaUi.BLUE));
        TornFcaUi.addSection(this,r,"CURRENT FACTION EXPECTATIONS");if(factionRules==null||factionRules.length()==0)TornFcaUi.add(this,r,TornFcaUi.card(this,"FACTION RULES","No shared targets available","Open Training Center for your faction's published rules and guide library. Progress here remains available even when shared community services are offline.",TornFcaUi.GOLD));else{String stat=factionRules.optString("stat_gain_target","").trim(),xan=factionRules.optString("xanax_target","").trim(),notes=factionRules.optString("notes","").trim();String body="Stat gain target: "+(stat.isBlank()?"Not set":stat)+"\nXanax target: "+(xan.isBlank()?"Not set":xan);if(!notes.isBlank())body+="\n\n"+notes;TornFcaUi.add(this,r,TornFcaUi.card(this,"FACTION RULES","Compare your progress",body,TornFcaUi.GOLD));}
        TornFcaUi.add(this,r,TornFcaUi.card(this,"PRIVACY","Your baseline stays on this device","This screen compares only your own Torn data. It does not send your battle stats to the faction library and does not expose your private stats to ordinary members. Leadership-wide compliance monitoring is a separate future permission-aware feature.",TornFcaUi.PURPLE));
        Button reset=TornFcaUi.button(this,"Reset progress baseline",TornFcaUi.BORDER);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));rp.topMargin=TornFcaUi.dp(this,3);rp.bottomMargin=TornFcaUi.dp(this,12);r.addView(reset,rp);reset.setOnClickListener(v->{saveBaseline(current);Toast.makeText(this,"Training baseline reset to your current stats.",Toast.LENGTH_SHORT).show();render();});
        r.addView(TornFcaUi.footer(this,"A faction change creates a separate baseline automatically; old faction expectations are never reused."));setContentView(s);s.requestApplyInsets();}

    private String number(long v){return NumberFormat.getIntegerInstance(Locale.US).format(v);}
    private String signed(long v){return(v>=0?"+":"")+number(v);}
    private void renderError(String message){runOnUiThread(()->{ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Member Center","My Training Progress","Progress unavailable");TornFcaUi.add(this,r,TornFcaUi.card(this,"CONNECTION","Unable to load",message,TornFcaUi.RED));setContentView(s);s.requestApplyInsets();});}

    private static long value(JSONObject battle,String name){JSONObject o=battle.optJSONObject(name);return o==null?0L:o.optLong("value",0L);}
    private static final class Snapshot{final long total,strength,defense,speed,dexterity;final int xanax;Snapshot(long total,long strength,long defense,long speed,long dexterity,int xanax){this.total=total;this.strength=strength;this.defense=defense;this.speed=speed;this.dexterity=dexterity;this.xanax=xanax;}static Snapshot from(JSONObject battle,JSONObject drugs){return new Snapshot(battle.optLong("total",0L),value(battle,"strength"),value(battle,"defense"),value(battle,"speed"),value(battle,"dexterity"),drugs==null?0:drugs.optInt("xanax",0));}}
    private static final class Baseline{final long at,total,strength,defense,speed,dexterity;final int xanax;Baseline(long at,long total,long strength,long defense,long speed,long dexterity,int xanax){this.at=at;this.total=total;this.strength=strength;this.defense=defense;this.speed=speed;this.dexterity=dexterity;this.xanax=xanax;}}
}
