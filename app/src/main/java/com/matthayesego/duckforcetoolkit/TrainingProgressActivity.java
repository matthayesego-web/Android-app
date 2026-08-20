package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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

    private void load(){String key=keyStore.load();if(key==null||key.isBlank()){renderError("Reconnect your Torn API key to view training progress.");return;}new Thread(()->{try{AuthSession verified=TornApiClient.cachedSession(key);if(verified==null)verified=TornApiClient.authenticate(key);session=verified;PremiumAccess.refresh(this,session.playerId);JSONObject battleRoot=TornApiClient.getJson("/user/battlestats",key);JSONObject battle=battleRoot.optJSONObject("battlestats");if(battle==null)throw new Exception("Torn did not return your battle stats. A Limited/Custom/Full key is required for this progress view.");JSONObject personal=TornApiClient.getJson("/user/personalstats?cat=drugs",key).optJSONObject("personalstats");JSONObject drugs=personal==null?null:personal.optJSONObject("drugs");current=Snapshot.from(battle,drugs);if(CommunityBackendClient.isConfigured())try{factionRules=CommunityBackendClient.trainingLibrary(key).optJSONObject("trainingRules");}catch(Exception ignored){}ensureBaseline();runOnUiThread(this::render);}catch(Exception e){renderError(e.getMessage()==null?"Unable to load your training progress.":e.getMessage());}},"TornFCA-TrainingProgress").start();}

    private String prefix(){return"p"+session.playerId+"_f"+session.factionId+"_";}
    private SharedPreferences prefs(){return getSharedPreferences(PREFS,MODE_PRIVATE);}
    private Baseline baseline(){SharedPreferences p=prefs();String x=prefix();long at=p.getLong(x+"at",0L);if(at<=0)return null;return new Baseline(at,p.getLong(x+"total",0),p.getLong(x+"strength",0),p.getLong(x+"defense",0),p.getLong(x+"speed",0),p.getLong(x+"dexterity",0),p.getInt(x+"xanax",0));}
    private void ensureBaseline(){if(baseline()==null)saveBaseline(current);}
    private void saveBaseline(Snapshot s){String x=prefix();prefs().edit().putLong(x+"at",System.currentTimeMillis()).putLong(x+"total",s.total).putLong(x+"strength",s.strength).putLong(x+"defense",s.defense).putLong(x+"speed",s.speed).putLong(x+"dexterity",s.dexterity).putInt(x+"xanax",s.xanax).apply();}
    private long premiumGoal(){return prefs().getLong(prefix()+"premium_goal_total",0L);}
    private void savePremiumGoal(long total){prefs().edit().putLong(prefix()+"premium_goal_total",Math.max(0L,total)).apply();}

    private void render(){Baseline b=baseline();if(b==null||current==null){renderError("Training baseline is unavailable.");return;}ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Member Center","My Training Progress",session.factionName+" • "+session.playerName+" • private member view");
        long elapsed=Math.max(0L,System.currentTimeMillis()-b.at),days=Math.max(1L,(elapsed+86399999L)/86400000L);long gain=Math.max(0L,current.total-b.total);double pct=b.total>0?100d*gain/b.total:0d;int xanax=Math.max(0,current.xanax-b.xanax);double xanaxPerDay=xanax/(Math.max(1d,elapsed/86400000d));
        TornFcaUi.addSection(this,r,"SINCE YOUR FACTION BASELINE");String tracked=elapsed<3600000L?"Baseline created today":days+" day"+(days==1?"":"s")+" tracked";TornFcaUi.add(this,r,TornFcaUi.card(this,"PROGRESS",tracked,"Total battle-stat gain: "+signed(gain)+" ("+String.format(Locale.US,"%.2f",pct)+"%)\nXanax taken: +"+xanax+" • "+String.format(Locale.US,"%.2f",xanaxPerDay)+" / day\nBaseline: "+DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(b.at)),TornFcaUi.GREEN));
        TornFcaUi.addSection(this,r,"BATTLE STATS");TornFcaUi.add(this,r,TornFcaUi.card(this,"CURRENT TOTAL",number(current.total),"Strength "+number(current.strength)+" ("+signed(current.strength-b.strength)+")\nDefense "+number(current.defense)+" ("+signed(current.defense-b.defense)+")\nSpeed "+number(current.speed)+" ("+signed(current.speed-b.speed)+")\nDexterity "+number(current.dexterity)+" ("+signed(current.dexterity-b.dexterity)+")",TornFcaUi.BLUE));
        addPremiumGoal(r,b,elapsed,gain);
        TornFcaUi.addSection(this,r,"CURRENT FACTION EXPECTATIONS");if(factionRules==null||factionRules.length()==0)TornFcaUi.add(this,r,TornFcaUi.card(this,"FACTION RULES","No shared targets available","Open Training Center for your faction's published rules and guide library. Progress here remains available even when shared community services are offline.",TornFcaUi.GOLD));else{String stat=factionRules.optString("stat_gain_target","").trim(),xan=factionRules.optString("xanax_target","").trim(),notes=factionRules.optString("notes","").trim();String body="Stat gain target: "+(stat.isBlank()?"Not set":stat)+"\nXanax target: "+(xan.isBlank()?"Not set":xan);if(!notes.isBlank())body+="\n\n"+notes;TornFcaUi.add(this,r,TornFcaUi.card(this,"FACTION RULES","Compare your progress",body,TornFcaUi.GOLD));}
        TornFcaUi.add(this,r,TornFcaUi.card(this,"PRIVACY","Your baseline stays on this device","This screen compares only your own Torn data. It does not send your battle stats to the faction library and does not expose your private stats to ordinary members. Premium goal targets also stay on this device and are scoped to this player and faction.",TornFcaUi.PURPLE));
        Button reset=TornFcaUi.button(this,"Reset progress baseline",TornFcaUi.BORDER);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));rp.topMargin=TornFcaUi.dp(this,3);rp.bottomMargin=TornFcaUi.dp(this,12);r.addView(reset,rp);reset.setOnClickListener(v->{saveBaseline(current);Toast.makeText(this,"Training baseline reset to your current stats.",Toast.LENGTH_SHORT).show();render();});
        r.addView(TornFcaUi.footer(this,"A faction change creates a separate baseline automatically; old faction expectations and Premium goals are never reused."));setContentView(s);s.requestApplyInsets();}

    private void addPremiumGoal(LinearLayout r,Baseline b,long elapsed,long gain){
        TornFcaUi.addSection(this,r,"PERSONAL GOAL");
        boolean premium=PremiumAccess.has(this,session.playerId,PremiumAccess.TRAINING_GOALS);
        if(!premium){
            LinearLayout locked=TornFcaUi.card(this,"PREMIUM","Training Goal Pacing","Your free training baseline and stat gains stay exactly as they are. Premium adds a private target with remaining stats, progress percentage and a pace-based estimate using the gains TornFCA already loaded.",TornFcaUi.GOLD);
            Button view=TornFcaUi.button(this,"View Premium",TornFcaUi.GOLD);view.setOnClickListener(v->startActivity(new Intent(this,PremiumPreviewActivity.class)));LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,44));vp.topMargin=TornFcaUi.dp(this,9);locked.addView(view,vp);TornFcaUi.add(this,r,locked);return;
        }

        long goal=premiumGoal();
        String title=goal>0?"Goal: "+number(goal):"Set a total battle-stat goal";
        String body;
        if(goal<=0){body="Choose a private total-stat target. TornFCA will use your existing faction baseline to estimate progress and pace without making another Torn API request.";}
        else{
            long remaining=Math.max(0L,goal-current.total);double progress=goal>0?Math.min(100d,100d*current.total/goal):0d;double trackedDays=Math.max(1d,elapsed/86400000d),gainPerDay=gain/trackedDays;
            if(remaining<=0)body="Goal reached. Current total: "+number(current.total)+" • 100% complete.";
            else if(elapsed<3600000L||gainPerDay<=0d)body="Current: "+number(current.total)+" • Remaining: "+number(remaining)+" • "+String.format(Locale.US,"%.2f",progress)+"% complete.\nBuild more baseline history to unlock a pace estimate.";
            else{double etaDays=remaining/gainPerDay;body="Current: "+number(current.total)+" • Remaining: "+number(remaining)+" • "+String.format(Locale.US,"%.2f",progress)+"% complete.\nCurrent pace: "+number(Math.round(gainPerDay))+" stats/day • estimated "+formatEta(etaDays)+" at this pace.";}
        }
        LinearLayout card=TornFcaUi.card(this,"PREMIUM GOAL",title,body,goal>0&&current.total>=goal?TornFcaUi.GREEN:TornFcaUi.GOLD);
        EditText target=new EditText(this);target.setSingleLine(true);target.setInputType(InputType.TYPE_CLASS_NUMBER);target.setHint("Target total battle stats");target.setTextColor(TornFcaUi.TEXT);target.setHintTextColor(TornFcaUi.MUTED);if(goal>0)target.setText(String.valueOf(goal));LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,48));tp.topMargin=TornFcaUi.dp(this,10);card.addView(target,tp);
        Button save=TornFcaUi.button(this,"Save Training Goal",TornFcaUi.GOLD);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,44));sp.topMargin=TornFcaUi.dp(this,8);card.addView(save,sp);save.setOnClickListener(v->{long value=parseLong(target.getText().toString());if(value<=0){Toast.makeText(this,"Enter a valid total battle-stat goal.",Toast.LENGTH_SHORT).show();return;}savePremiumGoal(value);Toast.makeText(this,"Premium training goal saved.",Toast.LENGTH_SHORT).show();render();});
        if(goal>0){Button clear=TornFcaUi.button(this,"Clear Goal",TornFcaUi.BORDER);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,42));cp.topMargin=TornFcaUi.dp(this,7);card.addView(clear,cp);clear.setOnClickListener(v->{savePremiumGoal(0L);Toast.makeText(this,"Training goal cleared.",Toast.LENGTH_SHORT).show();render();});}
        TornFcaUi.add(this,r,card);
    }

    private String formatEta(double days){if(days<1d)return "under 1 day";if(days<60d)return String.format(Locale.US,"%.1f days",days);double months=days/30.4375d;if(months<24d)return String.format(Locale.US,"%.1f months",months);return String.format(Locale.US,"%.1f years",days/365.25d);}
    private long parseLong(String raw){try{return Long.parseLong(raw.replace(",","").trim());}catch(Exception e){return 0L;}}
    private String number(long v){return NumberFormat.getIntegerInstance(Locale.US).format(v);}
    private String signed(long v){return(v>=0?"+":"")+number(v);}
    private void renderError(String message){runOnUiThread(()->{ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Member Center","My Training Progress","Progress unavailable");TornFcaUi.add(this,r,TornFcaUi.card(this,"CONNECTION","Unable to load",message,TornFcaUi.RED));setContentView(s);s.requestApplyInsets();});}

    private static long value(JSONObject battle,String name){JSONObject o=battle.optJSONObject(name);return o==null?0L:o.optLong("value",0L);}
    private static final class Snapshot{final long total,strength,defense,speed,dexterity;final int xanax;Snapshot(long total,long strength,long defense,long speed,long dexterity,int xanax){this.total=total;this.strength=strength;this.defense=defense;this.speed=speed;this.dexterity=dexterity;this.xanax=xanax;}static Snapshot from(JSONObject battle,JSONObject drugs){return new Snapshot(battle.optLong("total",0L),value(battle,"strength"),value(battle,"defense"),value(battle,"speed"),value(battle,"dexterity"),drugs==null?0:drugs.optInt("xanax",0));}}
    private static final class Baseline{final long at,total,strength,defense,speed,dexterity;final int xanax;Baseline(long at,long total,long strength,long defense,long speed,long dexterity,int xanax){this.at=at;this.total=total;this.strength=strength;this.defense=defense;this.speed=speed;this.dexterity=dexterity;this.xanax=xanax;}}
}
