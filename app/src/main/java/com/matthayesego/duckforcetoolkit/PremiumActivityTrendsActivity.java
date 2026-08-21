package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Premium leadership analytics layered on top of the Free 7-day Activity Tracker. */
public class PremiumActivityTrendsActivity extends Activity {
    private static final long DAY=86400L;
    private SecureApiKeyStore keyStore;
    private AuthSession session;

    @Override protected void onCreate(Bundle b){super.onCreate(b);keyStore=new SecureApiKeyStore(this);showLoading();load();}

    private void showLoading(){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Leadership","Premium Activity Trends","Comparing the last 7 days with the previous 23…");TornFcaUi.add(this,r,TornFcaUi.card(this,"ANALYZING","Building participation momentum","TornFCA is re-verifying current leadership and scanning the same faction-log categories used by Activity Tracker across a 30-day Premium window.",TornFcaUi.GOLD));setContentView(s);s.requestApplyInsets();}

    private void load(){
        String key=keyStore.load();
        if(key==null||key.isBlank()){renderError("Reconnect your Torn API key first.");return;}
        new Thread(()->{try{
            AuthSession verified=TornApiClient.authenticateFreshFaction(key);session=verified;
            if(DeveloperPreviewStore.isMemberPreview(this)||!AccessPolicy.isLeaderPosition(verified.position)){renderError("Premium Activity Trends is available only to currently verified faction leadership. Premium never grants leadership authority.");return;}
            PremiumAccess.refresh(this,verified.playerId);
            if(!PremiumAccess.has(this,verified.playerId,PremiumAccess.EXTENDED_ACTIVITY)){renderLocked();return;}

            long now=System.currentTimeMillis()/1000L,from=now-30L*DAY,recentCutoff=now-7L*DAY;
            JSONArray members=TornApiClient.getJson("/faction/members",key).optJSONArray("members");if(members==null)members=new JSONArray();
            String primary="main,attack,armoryDeposit,armoryAction,territoryWar,rankedWar,territoryGain,chain,crime,membership";
            JSONArray news;
            try{news=TornApiClient.getPagedArray("/faction/news?cat="+primary+"&from="+from+"&to="+now+"&sort=DESC&limit=100",key,"news",20);}
            catch(Exception limited){String fallback="main,armoryDeposit,armoryAction,territoryWar,rankedWar,territoryGain,chain,crime,membership";news=TornApiClient.getPagedArray("/faction/news?cat="+fallback+"&from="+from+"&to="+now+"&sort=DESC&limit=100",key,"news",20);}
            try{appendAll(news,TornApiClient.getPagedArray("/faction/news?cat=depositFunds,giveFunds&from="+from+"&to="+now+"&sort=DESC&limit=100",key,"news",20));}catch(Exception ignored){}

            List<Row> rows=new ArrayList<>();
            for(int i=0;i<members.length();i++){JSONObject m=members.optJSONObject(i);if(m==null)continue;String name=m.optString("name","").trim();if(name.isEmpty())continue;rows.add(new Row(m.optInt("id",0),name));}
            int recentRows=0;
            for(int i=0;i<news.length();i++){
                JSONObject item=news.optJSONObject(i);if(item==null)continue;
                long ts=item.optLong("timestamp",0L);boolean recent=ts>0&&ts>=recentCutoff;if(recent)recentRows++;
                String clean=Html.fromHtml(item.optString("text",""),Html.FROM_HTML_MODE_LEGACY).toString();
                for(Row row:rows)if(row.pattern.matcher(clean).find()){row.total30++;if(recent)row.recent7++;}
            }
            for(Row row:rows)row.finish();
            final JSONArray finalNews=news;final int fRecentRows=recentRows;runOnUiThread(()->render(rows,finalNews.length(),fRecentRows));
        }catch(Exception e){renderError(e.getMessage()==null?"Unable to build Premium Activity Trends.":e.getMessage());}},"TornFCA-PremiumActivityTrends").start();
    }

    private void render(List<Row> rows,int scannedRows,int recentRows){
        ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Leadership","Premium Activity Trends",session.factionName+" • 30-day participation momentum");
        int mentions30=0,mentions7=0,active30=0,active7=0;for(Row row:rows){mentions30+=row.total30;mentions7+=row.recent7;if(row.total30>0)active30++;if(row.recent7>0)active7++;}
        double priorPerDay=Math.max(0,mentions30-mentions7)/23d,recentPerDay=mentions7/7d;String pace=paceSummary(recentPerDay,priorPerDay);
        TornFcaUi.add(this,r,TornFcaUi.card(this,"30-DAY REVIEW","Faction participation picture",String.format(Locale.US,"%,d member mentions • %d/%d members active\n%,d faction-news rows scanned",mentions30,active30,rows.size(),scannedRows),TornFcaUi.GOLD));
        TornFcaUi.add(this,r,TornFcaUi.card(this,"7-DAY MOMENTUM","Recent participation pace",String.format(Locale.US,"%,d recent member mentions • %d/%d members active\n%s",mentions7,active7,rows.size(),pace),TornFcaUi.BLUE));

        List<Row> risers=new ArrayList<>(rows);Collections.sort(risers,(a,b)->Double.compare(b.deltaPerDay,a.deltaPerDay));
        TornFcaUi.addSection(this,r,"BIGGEST POSITIVE MOMENTUM");int shown=0;for(Row row:risers){if(shown>=5)break;if(row.deltaPerDay<=0&&row.recent7<=0)continue;TornFcaUi.add(this,r,trendCard(row));shown++;}if(shown==0)TornFcaUi.add(this,r,TornFcaUi.card(this,"NO RISERS YET","No positive participation shift detected","Recent activity is flat or the 30-day scan does not yet have enough dated log rows to show a positive change.",TornFcaUi.BORDER));

        List<Row> declines=new ArrayList<>(rows);Collections.sort(declines,(a,b)->Double.compare(a.deltaPerDay,b.deltaPerDay));
        TornFcaUi.addSection(this,r,"NEEDS A LOOK");shown=0;for(Row row:declines){if(shown>=5)break;if(row.deltaPerDay>=0||row.prior23<=0)continue;TornFcaUi.add(this,r,trendCard(row));shown++;}if(shown==0)TornFcaUi.add(this,r,TornFcaUi.card(this,"NO DECLINES FLAGGED","No clear drop in participation pace","This is a trend signal only; use the Free Needs Attention and Activity Tracker tools for current operational follow-up.",TornFcaUi.GREEN));

        Collections.sort(rows,(a,b)->{int c=Integer.compare(b.recent7,a.recent7);if(c!=0)return c;return Integer.compare(b.total30,a.total30);});
        TornFcaUi.addSection(this,r,"ALL MEMBERS — RECENT VS 30 DAYS");int rank=1;for(Row row:rows){String body=row.recent7+" last 7d • "+row.prior23+" previous 23d • "+row.total30+" total\n"+row.trendLabel();TornFcaUi.add(this,r,TornFcaUi.card(this,"#"+rank,row.name,body,row.deltaPerDay>0?TornFcaUi.GREEN:row.deltaPerDay<0?TornFcaUi.GOLD:TornFcaUi.BORDER));rank++;}
        TornFcaUi.add(this,r,TornFcaUi.card(this,"HOW TO READ THIS","Momentum, not a disciplinary score","The tracker counts faction-log entries that mention each member by name. Premium normalizes recent 7-day mentions per day against the previous 23-day pace. Treat it as a participation signal and review context before acting.",TornFcaUi.PURPLE));
        Button free=TornFcaUi.button(this,"Open Free Activity Tracker",TornFcaUi.BLUE);free.setOnClickListener(v->{Intent i=new Intent(this,FeatureRouterActivity.class);i.putExtra(FeatureRouterActivity.EXTRA_TARGET,FeatureRouterActivity.TARGET_ACTIVITY);startActivity(i);});r.addView(free,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,48)));
        setContentView(s);s.requestApplyInsets();
    }

    private LinearLayout trendCard(Row row){int accent=row.deltaPerDay>0?TornFcaUi.GREEN:row.deltaPerDay<0?TornFcaUi.GOLD:TornFcaUi.BORDER;return TornFcaUi.card(this,row.deltaPerDay>0?"RISING":"DECLINING",row.name,row.recent7+" last 7d • "+row.prior23+" previous 23d\n"+row.trendLabel(),accent);}
    private String paceSummary(double recent,double prior){if(prior<=0d)return recent>0d?String.format(Locale.US,"Recent faction pace %.1f mentions/day; previous-window pace was near zero.",recent):"No participation pace available yet.";double change=100d*(recent-prior)/prior;if(Math.abs(change)<5d)return String.format(Locale.US,"Recent faction pace %.1f/day is roughly level with the previous %.1f/day.",recent,prior);return String.format(Locale.US,"Recent faction pace %.1f/day is %.1f%% %s than the previous %.1f/day.",recent,Math.abs(change),change>0?"higher":"lower",prior);}
    private void appendAll(JSONArray target,JSONArray source){if(source==null)return;for(int i=0;i<source.length();i++)target.put(source.opt(i));}

    private void renderLocked(){runOnUiThread(()->{ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Leadership","Premium Activity Trends","The Free 7-day Activity Tracker remains available.");TornFcaUi.add(this,r,TornFcaUi.card(this,"PREMIUM","30-day momentum analysis","Premium adds the deeper 30-day window and recent-vs-prior pace comparison. It does not replace the Free leadership Activity Tracker.",TornFcaUi.GOLD));Button plan=TornFcaUi.button(this,"View Premium",TornFcaUi.GOLD);plan.setOnClickListener(v->startActivity(new Intent(this,PremiumPreviewActivity.class)));r.addView(plan,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,48)));setContentView(s);s.requestApplyInsets();});}
    private void renderError(String message){runOnUiThread(()->{ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Leadership","Premium Activity Trends","Unable to build the trend review");TornFcaUi.add(this,r,TornFcaUi.card(this,"DATA / ACCESS","Activity Trends unavailable",message,TornFcaUi.RED));setContentView(s);s.requestApplyInsets();});}

    private static final class Row{
        final int id;final String name;final Pattern pattern;int total30,recent7,prior23;double recentPerDay,priorPerDay,deltaPerDay,percentChange;
        Row(int id,String name){this.id=id;this.name=name;this.pattern=Pattern.compile("(?i)(?<![A-Za-z0-9_])"+Pattern.quote(name)+"(?![A-Za-z0-9_])");}
        void finish(){prior23=Math.max(0,total30-recent7);recentPerDay=recent7/7d;priorPerDay=prior23/23d;deltaPerDay=recentPerDay-priorPerDay;percentChange=priorPerDay>0d?100d*deltaPerDay/priorPerDay:(recentPerDay>0?100d:0d);}
        String trendLabel(){if(total30==0)return "No faction-log mentions in this 30-day scan.";if(priorPerDay<=0d)return recent7>0?"New/recent activity compared with a quiet prior window.":"No recent activity; earlier comparison pace is unavailable.";if(Math.abs(percentChange)<5d)return "Participation pace is roughly level.";return String.format(Locale.US,"Recent pace is %.1f%% %s.",Math.abs(percentChange),percentChange>0?"higher":"lower");}
    }
}
