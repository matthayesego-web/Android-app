package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/** Completed territory-war report with faction totals and the signed-in member's official contribution. */
public class TerritoryWarDetailActivity extends Activity {
    public static final String EXTRA_WAR_ID="territory_war_id";
    public static final String EXTRA_PLAYER_ID="player_id";
    public static final String EXTRA_PLAYER_NAME="player_name";
    private int warId,factionId,playerId;
    private String playerName;
    private SecureApiKeyStore keyStore;

    @Override protected void onCreate(Bundle b){super.onCreate(b);keyStore=new SecureApiKeyStore(this);warId=getIntent().getIntExtra(EXTRA_WAR_ID,0);factionId=getIntent().getIntExtra(FactionOpsActivity.EXTRA_FACTION_ID,0);playerId=getIntent().getIntExtra(EXTRA_PLAYER_ID,0);playerName=getIntent().getStringExtra(EXTRA_PLAYER_NAME);if(playerName==null||playerName.isBlank())playerName="You";showLoading();load();}

    private void load(){String key=keyStore.load();if(key==null||key.isBlank()){renderError("Reconnect your Torn API key to view this territory-war report.");return;}if(warId<=0){renderError("No territory war ID was supplied.");return;}new Thread(()->{try{if(playerId<=0||factionId<=0){AuthSession s=TornApiClient.cachedSession(key);if(s==null)s=TornApiClient.authenticate(key);if(playerId<=0){playerId=s.playerId;playerName=s.playerName;}if(factionId<=0)factionId=s.factionId;}JSONObject root=TornApiClient.getJson("/faction/"+warId+"/territorywarreport",key);JSONObject report=root.optJSONObject("territorywarreport");if(report==null)throw new IllegalStateException("Torn did not return a territory-war report.");runOnUiThread(()->render(report));}catch(Exception e){renderError(e.getMessage()==null?"Unable to load territory-war details.":e.getMessage());}},"TornFCA-TerritoryReport").start();}

    private void render(JSONObject report){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);String territory=report.optString("territory","?");TornFcaUi.header(this,r,"Territories","Territory "+territory,"Official completed territory-war report • War #"+warId);
        long start=report.optLong("started_at",0),end=report.optLong("ended_at",0);int winner=report.optInt("winner",0);String result=human(report.optString("result","completed"));String when=(start>0?DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(start*1000L)):"Unknown start")+(end>0?" → "+DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(end*1000L)):"");
        TornFcaUi.addSection(this,r,"Result");TornFcaUi.add(this,r,TornFcaUi.card(this,"OFFICIAL REPORT",winner==factionId?"Faction Victory":winner==0?"Completed":"Faction Defeat",result+"\n"+when,winner==factionId?TornFcaUi.GREEN:winner==0?TornFcaUi.GOLD:TornFcaUi.RED));

        JSONArray factions=report.optJSONArray("factions");JSONObject ours=null,other=null;for(int i=0;factions!=null&&i<factions.length();i++){JSONObject f=factions.optJSONObject(i);if(f==null)continue;if(f.optInt("id",0)==factionId)ours=f;else other=f;}
        TornFcaUi.addSection(this,r,"Faction totals");if(ours!=null){String body="Score: "+fmt(ours.optInt("score",0))+" • Joins: "+fmt(ours.optInt("joins",0))+" • Clears: "+fmt(ours.optInt("clears",0))+"\nRole: "+(ours.optBoolean("is_aggressor",false)?"Aggressor":"Defender");if(other!=null)body+="\nOpponent: "+other.optString("name","Opponent")+" • "+fmt(other.optInt("score",0))+" score";TornFcaUi.add(this,r,TornFcaUi.card(this,"YOUR FACTION",ours.optString("name","Faction"),body,TornFcaUi.BLUE));}

        TornFcaUi.addSection(this,r,"My contribution");JSONObject me=findMember(ours,playerId);if(me==null){TornFcaUi.add(this,r,TornFcaUi.card(this,"PERSONAL","No contribution row returned","Torn's completed report does not list "+playerName+" among the faction members who scored, joined or cleared during this territory war.",TornFcaUi.BORDER));}else{String body="Score: "+fmt(me.optInt("score",0))+"\nJoins: "+fmt(me.optInt("joins",0))+" • Clears: "+fmt(me.optInt("clears",0))+" • Level "+me.optInt("level",0);TornFcaUi.add(this,r,TornFcaUi.card(this,"PERSONAL",me.optString("username",playerName),body,TornFcaUi.GREEN));}

        if(ours!=null){TornFcaUi.addSection(this,r,"Faction contributors");JSONArray members=ours.optJSONArray("members");if(members!=null&&members.length()>0){int shown=Math.min(20,members.length());for(int i=0;i<shown;i++){JSONObject m=members.optJSONObject(i);if(m==null)continue;String body="Score "+fmt(m.optInt("score",0))+" • "+m.optInt("joins",0)+" joins • "+m.optInt("clears",0)+" clears";TornFcaUi.add(this,r,TornFcaUi.card(this,"MEMBER",m.optString("username","Member"),body,m.optInt("id",0)==playerId?TornFcaUi.GREEN:TornFcaUi.BORDER));}if(members.length()>shown)TornFcaUi.add(this,r,TornFcaUi.card(this,"MORE",(members.length()-shown)+" additional contributors","The most relevant first 20 report rows are shown to keep this screen easy to scan.",TornFcaUi.BORDER));}}
        setContentView(s);s.requestApplyInsets();}

    private JSONObject findMember(JSONObject faction,int id){if(faction==null||id<=0)return null;JSONArray members=faction.optJSONArray("members");for(int i=0;members!=null&&i<members.length();i++){JSONObject m=members.optJSONObject(i);if(m!=null&&m.optInt("id",0)==id)return m;}return null;}
    private String fmt(long value){return java.text.NumberFormat.getIntegerInstance(Locale.US).format(value);}
    private String human(String raw){if(raw==null||raw.isBlank())return"Completed";String[] parts=raw.replace('_',' ').split(" ");StringBuilder b=new StringBuilder();for(String p:parts){if(p.isBlank())continue;if(b.length()>0)b.append(' ');b.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));}return b.toString();}
    private void showLoading(){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Territories","Territory War Details","Loading official contribution report…");TornFcaUi.add(this,r,TornFcaUi.card(this,"LOADING","Checking Torn's report","Loading faction totals and your personal joins, clears and score.",TornFcaUi.BLUE));setContentView(s);s.requestApplyInsets();}
    private void renderError(String message){runOnUiThread(()->{ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Territories","Territory War Details","Report unavailable");TornFcaUi.add(this,r,TornFcaUi.card(this,"ERROR","Unable to load report",message,TornFcaUi.RED));Button retry=TornFcaUi.button(this,"Retry",TornFcaUi.GOLD);retry.setOnClickListener(v->{showLoading();load();});r.addView(retry,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,48)));setContentView(s);s.requestApplyInsets();});}
}
