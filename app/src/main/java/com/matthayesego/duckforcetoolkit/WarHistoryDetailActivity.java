package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Public-report-backed completed ranked-war detail, safe for ordinary faction members. */
public class WarHistoryDetailActivity extends Activity {
    public static final String EXTRA_WAR_ID="war_id";
    public static final String EXTRA_FACTION_ID="faction_id";
    public static final String EXTRA_PLAYER_ID="player_id";
    public static final String EXTRA_PLAYER_NAME="player_name";

    private static final int BG=Color.rgb(5,8,12),PANEL=Color.rgb(12,18,26),PANEL2=Color.rgb(8,13,20),BORDER=Color.rgb(36,47,61),TEXT=Color.rgb(246,248,251),MUTED=Color.rgb(145,155,169),GOLD=Color.rgb(241,190,86),BLUE=Color.rgb(82,153,235),GREEN=Color.rgb(76,190,102),RED=Color.rgb(239,88,82);
    private SecureApiKeyStore keyStore;
    private int warId,factionId,playerId;
    private String playerName;

    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);keyStore=new SecureApiKeyStore(this);warId=getIntent().getIntExtra(EXTRA_WAR_ID,0);factionId=getIntent().getIntExtra(EXTRA_FACTION_ID,0);playerId=getIntent().getIntExtra(EXTRA_PLAYER_ID,0);playerName=getIntent().getStringExtra(EXTRA_PLAYER_NAME);if(playerName==null)playerName="You";showLoading();load();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private TextView eyebrow(String value,int color){TextView t=text(value,9.5f,color,true);t.setLetterSpacing(.12f);return t;}
    private Button button(String value,int accent){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextColor(TEXT);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL2,accent,13));return b;}
    @SuppressWarnings("deprecation") private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);int l=dp(18),t=dp(16),r=dp(18),bt=dp(28);s.setPadding(l,t,r,bt);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),bt+i.getSystemWindowInsetBottom());return i;});return s;}
    private LinearLayout root(ScrollView s){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));return r;}
    private LinearLayout card(int stroke){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(17),dp(15),dp(17),dp(15));c.setBackground(rounded(PANEL,stroke,18));return c;}
    private void add(LinearLayout r,LinearLayout c){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(10);r.addView(c,p);}
    private void header(LinearLayout r){Button back=button("← War History",BORDER);back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(dp(150),dp(44)));TextView e=eyebrow("RANKED WAR • COMPLETED",GOLD);LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);ep.topMargin=dp(16);r.addView(e,ep);r.addView(text("War #"+warId,30,TEXT,true));}
    private void showLoading(){ScrollView s=shell();LinearLayout r=root(s);header(r);LinearLayout c=card(BORDER);c.addView(text("Loading completed war report…",18,TEXT,true));c.addView(text("TornFCA is reading Torn's official ranked-war report. No faction data is changed.",12.5f,MUTED,false));add(r,c);setContentView(s);s.requestApplyInsets();}

    private void load(){String key=keyStore.load();if(key==null||key.isBlank()){renderError("Reconnect your Torn API key to view war history.");return;}new Thread(()->{try{AuthSession cached=TornApiClient.cachedSession(key);if(cached!=null){if(factionId<=0)factionId=cached.factionId;if(playerId<=0)playerId=cached.playerId;if(playerName==null||"You".equals(playerName))playerName=cached.playerName;}JSONObject root=TornApiClient.getJson("/faction/"+warId+"/rankedwarreport",key);JSONObject report=root.optJSONObject("rankedwarreport");if(report==null)throw new Exception("Torn returned an empty ranked-war report.");runOnUiThread(()->render(report));}catch(Exception e){renderError(e.getMessage()==null?"Unable to load this war report.":e.getMessage());}}).start();}

    private void render(JSONObject report){ScrollView s=shell();LinearLayout r=root(s);header(r);JSONArray factions=report.optJSONArray("factions");JSONObject ours=findFaction(factions,factionId);JSONObject opponent=findOpponent(factions,factionId);int winner=report.optInt("winner",0);String result=winner==0?"DRAW":winner==factionId?"WIN":"LOSS";int accent="WIN".equals(result)?GREEN:"LOSS".equals(result)?RED:GOLD;String opponentName=opponent==null?"Opponent":opponent.optString("name","Opponent");int ourScore=ours==null?0:ours.optInt("score",0),theirScore=opponent==null?0:opponent.optInt("score",0);LinearLayout summary=card(accent);summary.addView(eyebrow(result+" • "+(report.optBoolean("forfeit",false)?"FORFEIT":"FINAL"),accent));summary.addView(text((ours==null?"Your faction":ours.optString("name","Your faction"))+" "+ourScore+" – "+theirScore+" "+opponentName,21,TEXT,true));long start=report.optLong("start",0),end=report.optLong("end",0);String dates=(start>0?DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(start*1000L)):"Unknown start")+" → "+(end>0?DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(end*1000L)):"Unknown end");String duration=start>0&&end>start?" • "+WarStatus.duration(end-start):"";summary.addView(text(dates+duration,12.5f,MUTED,false));add(r,summary);
        if(ours!=null){JSONObject rank=ours.optJSONObject("rank"),rewards=ours.optJSONObject("rewards");String rankText=rank==null?"Rank data unavailable":"Rank: "+rank.optString("before","?")+" → "+rank.optString("after","?");String rewardText=rewards==null?"Rewards unavailable":"Rewards: "+NumberFormat.getIntegerInstance(Locale.US).format(rewards.optInt("respect",0))+" respect • "+NumberFormat.getIntegerInstance(Locale.US).format(rewards.optInt("points",0))+" points";JSONArray items=rewards==null?null:rewards.optJSONArray("items");if(items!=null&&items.length()>0){StringBuilder itemText=new StringBuilder();for(int i=0;i<items.length();i++){JSONObject item=items.optJSONObject(i);if(item==null)continue;if(itemText.length()>0)itemText.append(", ");itemText.append(item.optInt("quantity",0)).append("× ").append(item.optString("name","item"));}if(itemText.length()>0)rewardText+="\nItems: "+itemText;}LinearLayout faction=card(BLUE);faction.addView(eyebrow("YOUR FACTION",BLUE));faction.addView(text(ours.optInt("attacks",0)+" war attacks • "+count(ours.optJSONArray("members"))+" participants",17,TEXT,true));faction.addView(text(rankText+"\n"+rewardText,12.5f,MUTED,false));add(r,faction);}
        JSONObject me=findMember(ours==null?null:ours.optJSONArray("members"),playerId);if(me!=null){LinearLayout personal=card(GREEN);personal.addView(eyebrow("MY WAR",GREEN));personal.addView(text(me.optInt("attacks",0)+" ranked-war attacks",20,TEXT,true));personal.addView(text("War score: "+String.format(Locale.US,"%.2f",me.optDouble("score",0d))+" • Level "+me.optInt("level",0),12.5f,MUTED,false));JSONObject receiptRow=WarPayoutReceiptStore.memberRow(this,warId,playerId);if(receiptRow!=null){personal.addView(text("Local payout receipt: "+money(receiptRow.optLong("net",0))+(receiptRow.optLong("penalty",0)>0?" • penalty -"+money(receiptRow.optLong("penalty",0)):""),12.5f,GOLD,true));}add(r,personal);}else if(playerId>0){LinearLayout none=card(BORDER);none.addView(eyebrow("MY WAR",MUTED));none.addView(text(playerName+" does not appear in the official participant list for this ranked war.",13,MUTED,false));add(r,none);}
        if(ours!=null)addTop(r,"TOP FACTION CONTRIBUTORS",ours.optJSONArray("members"),BLUE);if(opponent!=null)addTop(r,"TOP OPPONENT CONTRIBUTORS",opponent.optJSONArray("members"),GOLD);TextView foot=text("Completed-war details come from Torn's official ranked-war report. Personal payout data appears only when a receipt exists locally on this device.",10.5f,MUTED,false);foot.setGravity(Gravity.CENTER);LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);fp.topMargin=dp(8);r.addView(foot,fp);setContentView(s);s.requestApplyInsets();}

    private void addTop(LinearLayout root,String title,JSONArray members,int accent){List<JSONObject> rows=new ArrayList<>();for(int i=0;members!=null&&i<members.length();i++){JSONObject m=members.optJSONObject(i);if(m!=null)rows.add(m);}Collections.sort(rows,(a,b)->Integer.compare(b.optInt("attacks",0),a.optInt("attacks",0)));LinearLayout c=card(Color.TRANSPARENT);c.addView(eyebrow(title,accent));int max=Math.min(5,rows.size());for(int i=0;i<max;i++){JSONObject m=rows.get(i);c.addView(text((i+1)+". "+m.optString("name","Member")+" — "+m.optInt("attacks",0)+" attacks • "+String.format(Locale.US,"%.2f",m.optDouble("score",0d))+" score",12.5f,i==0?TEXT:MUTED,i==0));}if(max==0)c.addView(text("No participant rows returned.",12.5f,MUTED,false));add(root,c);}
    private JSONObject findFaction(JSONArray fs,int id){for(int i=0;fs!=null&&i<fs.length();i++){JSONObject f=fs.optJSONObject(i);if(f!=null&&f.optInt("id",0)==id)return f;}return null;}
    private JSONObject findOpponent(JSONArray fs,int id){for(int i=0;fs!=null&&i<fs.length();i++){JSONObject f=fs.optJSONObject(i);if(f!=null&&f.optInt("id",0)!=id)return f;}return null;}
    private JSONObject findMember(JSONArray members,int id){for(int i=0;members!=null&&i<members.length();i++){JSONObject m=members.optJSONObject(i);if(m!=null&&m.optInt("id",0)==id)return m;}return null;}
    private int count(JSONArray rows){return rows==null?0:rows.length();}
    private String money(long value){return "$"+NumberFormat.getIntegerInstance(Locale.US).format(value);}
    private void renderError(String message){runOnUiThread(()->{ScrollView s=shell();LinearLayout r=root(s);header(r);LinearLayout c=card(RED);c.addView(text("War report unavailable",20,TEXT,true));c.addView(text(message,13,MUTED,false));Button retry=button("Retry",GOLD);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));rp.topMargin=dp(10);c.addView(retry,rp);retry.setOnClickListener(v->{showLoading();load();});add(r,c);setContentView(s);s.requestApplyInsets();});}
}
