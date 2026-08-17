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

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Premium personal analytics built only from the authenticated player's own Torn activity/history. */
public class PremiumInsightsActivity extends Activity {
    private SecureApiKeyStore keyStore;
    private AuthSession session;

    @Override protected void onCreate(Bundle b){super.onCreate(b);keyStore=new SecureApiKeyStore(this);bootstrap();}
    @Override protected void onResume(){super.onResume();int player=currentPlayerId();if(player>0)PremiumAccess.refresh(this,player);}

    private void bootstrap(){String key=keyStore.load();if(key==null||key.isBlank()){renderError("Reconnect your Torn API key first.");return;}session=TornApiClient.cachedSession(key);if(session!=null){PremiumAccess.refresh(this,session.playerId);if(!PremiumAccess.has(this,session.playerId,PremiumAccess.PERSONAL_INSIGHTS)){renderLocked();return;}showLoading();load(key);return;}showLoading();new Thread(()->{try{session=TornApiClient.authenticate(key);PremiumAccess.refresh(this,session.playerId);runOnUiThread(()->{if(!PremiumAccess.has(this,session.playerId,PremiumAccess.PERSONAL_INSIGHTS))renderLocked();else load(key);});}catch(Exception e){renderError(e.getMessage()==null?"Unable to verify your Torn account.":e.getMessage());}},"TornFCA-PremiumInsightsAuth").start();}

    private void load(String key){new Thread(()->{try{long now=System.currentTimeMillis()/1000L,from=now-30L*24L*60L*60L;JSONArray attacks=TornApiClient.getPagedArray("/user/attacks?filters=outgoing&from="+from+"&to="+now+"&sort=DESC&limit=100",key,"attacks",20);JSONArray wars=TornApiClient.getJson("/faction/"+session.factionId+"/rankedwars?limit=10",key).optJSONArray("rankedwars");if(wars==null)wars=new JSONArray();Stats stats=buildStats(attacks,wars);runOnUiThread(()->render(stats));}catch(Exception e){renderError(e.getMessage()==null?"Unable to build your Premium Insights.":e.getMessage());}},"TornFCA-PremiumInsights").start();}

    private Stats buildStats(JSONArray attacks,JSONArray wars){Stats s=new Stats();s.totalAttacks=attacks==null?0:attacks.length();Set<String> activeDays=new HashSet<>();SimpleDateFormat day=new SimpleDateFormat("yyyy-MM-dd",Locale.US);for(int i=0;attacks!=null&&i<attacks.length();i++){JSONObject a=attacks.optJSONObject(i);if(a==null)continue;if(a.optBoolean("is_ranked_war",false))s.rankedHits++;long ts=firstPositive(a.optLong("ended",0L),a.optLong("started",0L),a.optLong("timestamp",0L));if(ts>0)activeDays.add(day.format(new Date(ts*1000L)));}s.activeDays=activeDays.size();for(int i=0;wars!=null&&i<wars.length();i++){JSONObject w=wars.optJSONObject(i);if(w==null)continue;s.wars++;int winner=w.isNull("winner")?0:w.optInt("winner",0);if(winner==0)s.draws++;else if(winner==session.factionId)s.wins++;else s.losses++;int warId=w.optInt("id",0);JSONObject row=WarPayoutReceiptStore.memberRow(this,warId,session.playerId);if(row!=null){s.receipts++;s.localPayout+=Math.max(0L,row.optLong("net",0L));}}return s;}
    private long firstPositive(long... values){for(long v:values)if(v>0)return v;return 0L;}

    private void render(Stats s){ScrollView scroll=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,scroll);TornFcaUi.header(this,r,"Member Center","Premium Insights",session.playerName+" • personal 30-day faction activity view");TornFcaUi.add(this,r,TornFcaUi.card(this,"30-DAY COMBAT","Your recent activity",s.totalAttacks+" outgoing attacks scanned • "+s.rankedHits+" ranked-war hits"+(s.activeDays>0?" • "+s.activeDays+" active combat days":"")+".\nScan is capped at 2,000 recent outgoing attacks to protect API responsiveness.",TornFcaUi.GOLD));String warText=s.wars+" recent faction wars • "+s.wins+" wins • "+s.losses+" losses"+(s.draws>0?" • "+s.draws+" draws":"");TornFcaUi.add(this,r,TornFcaUi.card(this,"WAR HISTORY","Recent faction results",warText,TornFcaUi.RED));String payout=s.receipts==0?"No personal WarPay receipts from these recent wars are stored on this device.":s.receipts+" local personal receipt"+(s.receipts==1?"":"s")+" • "+money(s.localPayout)+" recorded payout";TornFcaUi.add(this,r,TornFcaUi.card(this,"LOCAL RECEIPTS","Your recorded WarPay",payout,TornFcaUi.GREEN));String insight;if(s.rankedHits==0)insight="No ranked-war hits were found in the scanned 30-day activity window. Your next completed war will begin building this trend automatically.";else if(s.activeDays>0)insight="You averaged "+oneDecimal((double)s.rankedHits/(double)s.activeDays)+" ranked-war hits per active combat day across the scanned window.";else insight="Torn returned "+s.rankedHits+" ranked-war hits in the scanned window. Day-level timestamps were not available for a daily average.";TornFcaUi.add(this,r,TornFcaUi.card(this,"PERSONAL TREND","What the numbers say",insight,TornFcaUi.BLUE));TornFcaUi.add(this,r,TornFcaUi.card(this,"PRIVACY","Personal by design","Premium Insights uses your authenticated user activity and your faction's recent war results. It does not expose another member's private account data.",TornFcaUi.BORDER));Button refresh=TornFcaUi.button(this,"Refresh Insights",TornFcaUi.GOLD);refresh.setOnClickListener(v->{showLoading();load(keyStore.load());});r.addView(refresh,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,48)));setContentView(scroll);scroll.requestApplyInsets();}

    private void renderLocked(){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Member Center","Premium Insights","Personal analytics are a TornFCA Premium feature.");TornFcaUi.add(this,r,TornFcaUi.card(this,"PREMIUM","Personal Insights locked","Free members keep My Day, My War/history, personal OC, chain, faction overview, basic notifications and faction chat. Premium adds deeper personal trends and convenience features.",TornFcaUi.GOLD));Button plan=TornFcaUi.button(this,"View Free vs Premium",TornFcaUi.GOLD);plan.setOnClickListener(v->startActivity(new Intent(this,PremiumPreviewActivity.class)));r.addView(plan,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,48)));setContentView(s);s.requestApplyInsets();}
    private void showLoading(){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Member Center","Premium Insights","Building your personal 30-day activity picture…");TornFcaUi.add(this,r,TornFcaUi.card(this,"ANALYZING","Scanning your history","Loading your own outgoing attacks, ranked-war participation and recent faction war results through the shared TornFCA API throttle.",TornFcaUi.GOLD));setContentView(s);s.requestApplyInsets();}
    private void renderError(String message){runOnUiThread(()->{ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Member Center","Premium Insights","Unable to build your personal analytics");TornFcaUi.add(this,r,TornFcaUi.card(this,"DATA UNAVAILABLE","Premium Insights could not load",message,TornFcaUi.RED));Button retry=TornFcaUi.button(this,"Retry",TornFcaUi.GOLD);retry.setOnClickListener(v->bootstrap());r.addView(retry,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,48)));setContentView(s);s.requestApplyInsets();});}
    private int currentPlayerId(){if(session!=null)return session.playerId;String key=keyStore==null?null:keyStore.load();if(key==null)return 0;AuthSession hot=TornApiClient.cachedSession(key);if(hot!=null)return hot.playerId;FactionScopeCache.Scope scope=FactionScopeCache.load(this,key);return scope==null?0:scope.playerId;}
    private String money(long value){return "$"+NumberFormat.getIntegerInstance(Locale.US).format(value);}
    private String oneDecimal(double value){return String.format(Locale.US,"%.1f",value);}
    private static final class Stats{int totalAttacks,rankedHits,activeDays,wars,wins,losses,draws,receipts;long localPayout;}
}
