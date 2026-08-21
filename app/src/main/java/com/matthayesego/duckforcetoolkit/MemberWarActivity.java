package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
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
import java.util.Date;

/** Member-safe ranked-war view: current personal participation plus completed-war history. */
public class MemberWarActivity extends Activity {
    private static final int BG=Color.rgb(6,9,13),PANEL=Color.rgb(15,20,28),PANEL2=Color.rgb(10,15,22),BORDER=Color.rgb(45,55,69),TEXT=Color.rgb(244,246,249),MUTED=Color.rgb(154,164,178),GOLD=Color.rgb(241,194,106),BLUE=Color.rgb(88,166,255),GREEN=Color.rgb(63,185,80),RED=Color.rgb(248,81,73);
    private SecureApiKeyStore keyStore;
    private AuthSession session;

    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);keyStore=new SecureApiKeyStore(this);showLoading();load();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String value,int accent){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextColor(TEXT);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL2,accent,12));return b;}
    @SuppressWarnings("deprecation") private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);int l=dp(16),t=dp(18),r=dp(16),bt=dp(30);s.setPadding(l,t,r,bt);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),bt+i.getSystemWindowInsetBottom());return i;});return s;}
    private LinearLayout root(ScrollView s){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));return r;}
    private LinearLayout card(int stroke){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(17),dp(15),dp(17),dp(15));c.setBackground(rounded(PANEL,stroke,18));return c;}
    private void add(LinearLayout r,LinearLayout c){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(10);r.addView(c,p);}
    private void section(LinearLayout r,String label){TextView t=text(label,11,MUTED,true);t.setLetterSpacing(.11f);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(5);p.bottomMargin=dp(8);r.addView(t,p);}
    private void header(LinearLayout r,String subtitle){Button back=button("← Companion",BORDER);back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(dp(132),dp(44)));TextView brand=text("FACTION COMPANION • MEMBER",10,GOLD,true);brand.setLetterSpacing(.13f);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);bp.topMargin=dp(16);r.addView(brand,bp);r.addView(text("My War",31,TEXT,true));TextView sub=text(subtitle,13,MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(5);sp.bottomMargin=dp(15);r.addView(sub,sp);}
    private void showLoading(){ScrollView s=shell();LinearLayout r=root(s);header(r,"Loading your ranked-war status and history…");LinearLayout c=card(BLUE);c.addView(text("Checking your current participation…",17,TEXT,true));add(r,c);setContentView(s);s.requestApplyInsets();}

    private void load(){String key=keyStore.load();if(key==null||key.isBlank()){renderError("Reconnect your Torn API key to use My War.");return;}new Thread(()->{try{session=TornApiClient.cachedSession(key);if(session==null)session=TornApiClient.authenticate(key);WarStatus war=WarStatus.from(TornApiClient.getJson("/faction/wars",key),session.factionId);long now=System.currentTimeMillis()/1000L;int hits=0;if(war.isLive(now)){JSONArray attacks=TornApiClient.getPagedArray("/user/attacks?filters=outgoing&from="+war.start+"&to="+now+"&sort=DESC&limit=100",key,"attacks",12);for(int i=0;i<attacks.length();i++){JSONObject a=attacks.optJSONObject(i);if(a!=null&&a.optBoolean("is_ranked_war",false))hits++;}}JSONArray history=TornApiClient.getJson("/faction/"+session.factionId+"/rankedwars?limit=6",key).optJSONArray("rankedwars");if(history==null)history=new JSONArray();int finalHits=hits;JSONArray finalHistory=history;runOnUiThread(()->render(war,finalHits,finalHistory));}catch(Exception e){renderError(e.getMessage()==null?"Unable to load your war status.":e.getMessage());}}).start();}

    private void render(WarStatus war,int hits,JSONArray history){ScrollView s=shell();LinearLayout r=root(s);header(r,session.factionName+" • "+session.playerName);long now=System.currentTimeMillis()/1000L;section(r,"CURRENT WAR");int accent=war.isLive(now)?RED:war.isUpcoming(now)?GOLD:GREEN;LinearLayout current=card(accent);current.addView(text(war.headline(now),20,TEXT,true));String detail=war.detail(now);if(war.isLive(now))detail+="\nYour verified ranked-war hits: "+hits;current.addView(text(detail,13,MUTED,false));if(war.simulated)current.addView(text("Developer simulation — no Torn data is changed.",11.5f,GOLD,true));add(r,current);
        section(r,"RECENT WARS");if(history.length()==0){LinearLayout none=card(BORDER);none.addView(text("No completed ranked wars returned.",16,TEXT,true));add(r,none);}else for(int i=0;i<history.length();i++){JSONObject w=history.optJSONObject(i);if(w==null)continue;add(r,historyCard(w));}
        TextView foot=text("Tap a completed war to see the official result, faction rewards, top contributors and your own participation. A personal payout line appears when a local WarPay receipt exists on this device.",10.5f,MUTED,false);foot.setGravity(Gravity.CENTER);r.addView(foot);setContentView(s);s.requestApplyInsets();}
    private LinearLayout historyCard(JSONObject w){int warId=w.optInt("id",0),winner=w.isNull("winner")?0:w.optInt("winner",0);JSONArray fs=w.optJSONArray("factions");String opponent="Opponent";int ours=0,theirs=0;for(int i=0;fs!=null&&i<fs.length();i++){JSONObject f=fs.optJSONObject(i);if(f==null)continue;if(f.optInt("id",0)==session.factionId)ours=f.optInt("score",0);else{opponent=f.optString("name",opponent);theirs=f.optInt("score",0);}}String result=winner==0?"DRAW":winner==session.factionId?"WIN":"LOSS";int accent="WIN".equals(result)?GREEN:"LOSS".equals(result)?RED:GOLD;LinearLayout c=card(Color.TRANSPARENT);c.addView(text(result+" • vs "+opponent,17,TEXT,true));long end=w.optLong("end",0);String date=end>0?DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(end*1000L)):"Unknown date";c.addView(text("Final "+ours+" – "+theirs+" • "+date+" • War #"+warId,12.5f,MUTED,false));JSONObject receipt=WarPayoutReceiptStore.memberRow(this,warId,session.playerId);if(receipt!=null)c.addView(text("My local payout receipt: "+money(receipt.optLong("net",0)),12,GOLD,true));Button open=button("View My War Details",BLUE);LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));op.topMargin=dp(9);c.addView(open,op);open.setOnClickListener(v->openDetails(warId));return c;}
    private void openDetails(int warId){Intent i=new Intent(this,WarHistoryDetailActivity.class);i.putExtra(WarHistoryDetailActivity.EXTRA_WAR_ID,warId);i.putExtra(WarHistoryDetailActivity.EXTRA_FACTION_ID,session.factionId);i.putExtra(WarHistoryDetailActivity.EXTRA_PLAYER_ID,session.playerId);i.putExtra(WarHistoryDetailActivity.EXTRA_PLAYER_NAME,session.playerName);startActivity(i);}
    private String money(long value){return "$"+java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(value);}
    private void renderError(String message){runOnUiThread(()->{ScrollView s=shell();LinearLayout r=root(s);header(r,"Data unavailable");LinearLayout c=card(RED);c.addView(text("Unable to load My War",19,TEXT,true));c.addView(text(message,13,MUTED,false));Button retry=button("Retry",GOLD);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));rp.topMargin=dp(9);c.addView(retry,rp);retry.setOnClickListener(v->{showLoading();load();});add(r,c);setContentView(s);s.requestApplyInsets();});}
}
