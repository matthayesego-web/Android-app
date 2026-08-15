package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class FactionOpsActivity extends Activity {
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_FACTION_ID = "faction_id";
    public static final String EXTRA_FACTION_NAME = "faction_name";
    public static final String EXTRA_FACTION_API = "faction_api";

    public static final String MODE_ACTIVITY = "ACTIVITY";
    public static final String MODE_WAR = "WAR";
    public static final String MODE_CHAIN = "CHAIN";
    public static final String MODE_OC = "OC";

    private static final int BG=Color.rgb(8,12,18), PANEL=Color.rgb(20,27,38), PANEL2=Color.rgb(27,36,49), BORDER=Color.rgb(49,63,81);
    private static final int TEXT=Color.rgb(245,248,252), MUTED=Color.rgb(151,163,179), GOLD=Color.rgb(243,184,52), BLUE=Color.rgb(88,166,255), GOOD=Color.rgb(63,185,80), BAD=Color.rgb(248,81,73);

    private SecureApiKeyStore keyStore;
    private String mode;
    private int factionId;
    private String factionName;
    private boolean factionApi;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        keyStore = new SecureApiKeyStore(this);
        mode = getIntent().getStringExtra(EXTRA_MODE);
        factionId = getIntent().getIntExtra(EXTRA_FACTION_ID, 0);
        factionName = getIntent().getStringExtra(EXTRA_FACTION_NAME);
        factionApi = getIntent().getBooleanExtra(EXTRA_FACTION_API, false);
        if (mode == null) mode = MODE_ACTIVITY;
        if (factionName == null || factionName.trim().isEmpty()) factionName = "Faction";
        showLoading();
        load();
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(TEXT);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL2,BORDER,11));return b;}
    private LinearLayout card(String title,String body,int stroke){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(15),dp(16),dp(15));c.setBackground(rounded(PANEL,stroke,17));c.addView(text(title,18,TEXT,true));if(body!=null&&!body.isEmpty()){TextView b=text(body,13,MUTED,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(6);c.addView(b,p);}return c;}
    private void addCard(LinearLayout root,LinearLayout card){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(10);root.addView(card,p);}

    @SuppressWarnings("deprecation") private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);int l=dp(16),t=dp(16),r=dp(16),b=dp(28);s.setPadding(l,t,r,b);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),b+i.getSystemWindowInsetBottom());return i;});return s;}
    private LinearLayout root(ScrollView s){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));return r;}

    private String titleForMode(){
        if(MODE_WAR.equals(mode))return "War Participation";
        if(MODE_CHAIN.equals(mode))return "Chain Command Center";
        if(MODE_OC.equals(mode))return "OC Readiness";
        return "30-Day Activity";
    }

    private void addHeader(LinearLayout root,String subtitle){
        Button back=button("← Companion");back.setOnClickListener(v->finish());root.addView(back,new LinearLayout.LayoutParams(dp(124),dp(44)));
        TextView title=text(titleForMode(),27,TEXT,true);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp(14);root.addView(title,tp);
        TextView sub=text(factionName+" • "+subtitle,13,MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(4);sp.bottomMargin=dp(14);root.addView(sub,sp);
        Button refresh=button("↻ Refresh");refresh.setOnClickListener(v->{showLoading();load();});root.addView(refresh,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46)));
        TextView gap=text("",6,MUTED,false);root.addView(gap);
    }

    private void showLoading(){ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Loading live Torn data…");addCard(r,card("Loading", "This screen reads only the authenticated faction scope for faction ID "+factionId+".", GOLD));setContentView(s);s.requestApplyInsets();}

    private void load(){
        String key=keyStore.load();
        if(key==null||key.trim().isEmpty()){renderError("Reconnect your Torn API key to use faction operations.");return;}
        new Thread(()->{try{
            if(MODE_WAR.equals(mode))loadWar(key);
            else if(MODE_CHAIN.equals(mode))loadChain(key);
            else if(MODE_OC.equals(mode))loadOc(key);
            else loadActivity(key);
        }catch(Exception e){renderError(e.getMessage()==null?"Unable to load faction data.":e.getMessage());}}).start();
    }

    private void renderError(String message){runOnUiThread(()->{ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Data unavailable");addCard(r,card("Unable to load",message,BAD));setContentView(s);s.requestApplyInsets();});}

    private void loadActivity(String key) throws Exception {
        if(!factionApi){renderAccessRequired("Faction API Access is required for the faction-news activity scan. War history and chain data remain available without it.");return;}
        int days=DeveloperSettings.activityDays(this);
        long now=System.currentTimeMillis()/1000L, from=now-(days*86400L);
        JSONArray members=TornApiClient.getJson("/faction/members",key).optJSONArray("members");
        if(members==null)members=new JSONArray();
        String cats="main,attack,armoryDeposit,armoryAction,territoryWar,rankedWar,territoryGain,chain,crime,membership";
        JSONArray news;
        try{
            news=TornApiClient.getPagedArray("/faction/news?cat="+cats+"&from="+from+"&to="+now+"&sort=DESC&limit=100",key,"news",20);
        }catch(Exception limitedKey){
            String fallback="main,armoryDeposit,armoryAction,territoryWar,rankedWar,territoryGain,chain,crime,membership";
            news=TornApiClient.getPagedArray("/faction/news?cat="+fallback+"&from="+from+"&to="+now+"&sort=DESC&limit=100",key,"news",20);
        }
        List<MemberCount> counts=new ArrayList<>();
        for(int i=0;i<members.length();i++){
            JSONObject m=members.optJSONObject(i);if(m==null)continue;
            String name=m.optString("name","").trim();if(name.isEmpty())continue;
            counts.add(new MemberCount(m.optInt("id",0),name));
        }
        for(int i=0;i<news.length();i++){
            JSONObject row=news.optJSONObject(i);if(row==null)continue;
            String raw=row.optString("text","");
            String clean=Html.fromHtml(raw,Html.FROM_HTML_MODE_LEGACY).toString();
            for(MemberCount mc:counts)if(mc.pattern.matcher(clean).find())mc.count++;
        }
        Collections.sort(counts,(a,b)->{int c=Integer.compare(b.count,a.count);return c!=0?c:a.name.compareToIgnoreCase(b.name);});
        final JSONArray finalNews=news;final List<MemberCount> finalCounts=counts;
        runOnUiThread(()->{
            ScrollView s=shell();LinearLayout r=root(s);addHeader(r,days+"-day faction-log scan");
            addCard(r,card("Activity coverage",finalNews.length()+" faction-news rows scanned • up to 2,000 rows per refresh\nCounts are log entries that mention each member by name; they are a participation signal, not a Torn-issued score.",BLUE));
            int total=0;for(MemberCount mc:finalCounts)total+=mc.count;
            addCard(r,card("Faction activity mentions",String.format(Locale.US,"%,d member mentions across %,d scanned log rows",total,finalNews.length()),GOOD));
            int rank=1;for(MemberCount mc:finalCounts){String body=String.format(Locale.US,"%,d actions / mentions",mc.count);addCard(r,card(rank+". "+mc.name,body,mc.count==0?BAD:BORDER));rank++;}
            setContentView(s);s.requestApplyInsets();
        });
    }

    private void loadWar(String key) throws Exception {
        JSONObject membersRoot=TornApiClient.getJson("/faction/members",key);
        JSONArray members=membersRoot.optJSONArray("members");if(members==null)members=new JSONArray();
        Map<Integer,String> names=new HashMap<>();for(int i=0;i<members.length();i++){JSONObject m=members.optJSONObject(i);if(m!=null)names.put(m.optInt("id",0),m.optString("name","ID "+m.optInt("id",0)));}
        JSONObject currentRoot=TornApiClient.getJson("/faction/wars",key);JSONObject wars=currentRoot.optJSONObject("wars");JSONObject ranked=wars==null?null:wars.optJSONObject("ranked");
        long now=System.currentTimeMillis()/1000L;
        if(ranked!=null&&ranked.optInt("war_id",0)>0&&ranked.optLong("start",0)<=now&&(ranked.isNull("end")||ranked.optLong("end",0)==0||ranked.optLong("end",0)>now)){
            int warId=ranked.optInt("war_id",0);long start=ranked.optLong("start",0);JSONArray factions=ranked.optJSONArray("factions");
            String scoreText=warScoreText(factions);
            List<MemberCount> participation=new ArrayList<>();
            String detail="Live member participation needs a Limited/Custom/Full key plus Faction API Access.";
            if(factionApi){
                try{
                    JSONArray attacks=TornApiClient.getPagedArray("/faction/attacks?filters=outgoing&from="+start+"&to="+now+"&sort=DESC&limit=100",key,"attacks",20);
                    Map<Integer,Integer> hitCounts=new HashMap<>();
                    for(int i=0;i<attacks.length();i++){
                        JSONObject a=attacks.optJSONObject(i);if(a==null||!a.optBoolean("is_ranked_war",false))continue;
                        JSONObject attacker=a.optJSONObject("attacker");if(attacker==null)continue;int id=attacker.optInt("id",0);if(!names.containsKey(id))continue;hitCounts.put(id,hitCounts.getOrDefault(id,0)+1);
                    }
                    for(Map.Entry<Integer,String> e:names.entrySet()){MemberCount mc=new MemberCount(e.getKey(),e.getValue());mc.count=hitCounts.getOrDefault(e.getKey(),0);participation.add(mc);}
                    Collections.sort(participation,(a,b)->Integer.compare(b.count,a.count));detail="Live ranked-war attacks counted from the war start.";
                }catch(Exception ex){detail="War is live; score is available, but this key could not read detailed faction attacks.";}
            }
            final String finalDetail=detail;final List<MemberCount> finalParticipation=participation;final int finalWarId=warId;final String finalScore=scoreText;
            runOnUiThread(()->{
                ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Live ranked war #"+finalWarId);addCard(r,card("LIVE WAR",finalScore+"\n"+finalDetail,GOLD));
                if(finalParticipation.isEmpty())addCard(r,card("Participation","Member hit counts will appear here when the authenticated key can read faction attacks.",BORDER));
                else{for(MemberCount mc:finalParticipation)addCard(r,card(mc.name,mc.count+" ranked-war attacks",mc.count==0?BAD:BORDER));}
                setContentView(s);s.requestApplyInsets();
            });return;
        }

        JSONArray history=TornApiClient.getJson("/faction/rankedwars?limit=1",key).optJSONArray("rankedwars");
        if(history==null||history.length()==0){renderSimple("No ranked-war history","No completed ranked war was returned for this faction.");return;}
        JSONObject latest=history.optJSONObject(0);int warId=latest==null?0:latest.optInt("id",0);if(warId<=0){renderSimple("War report unavailable","The latest ranked war did not contain a report ID.");return;}
        JSONObject reportRoot=TornApiClient.getJson("/faction/"+warId+"/rankedwarreport",key);JSONObject report=reportRoot.optJSONObject("rankedwarreport");
        if(report==null){renderSimple("War report unavailable","Torn did not return a ranked-war report.");return;}
        JSONArray factions=report.optJSONArray("factions");JSONObject ours=null,other=null;if(factions!=null)for(int i=0;i<factions.length();i++){JSONObject f=factions.optJSONObject(i);if(f==null)continue;if(f.optInt("id",0)==factionId)ours=f;else other=f;}
        if(ours==null){renderSimple("War report unavailable","This faction was not present in the latest ranked-war report.");return;}
        JSONArray warMembers=ours.optJSONArray("members");List<WarMember> rows=new ArrayList<>();if(warMembers!=null)for(int i=0;i<warMembers.length();i++){JSONObject m=warMembers.optJSONObject(i);if(m!=null)rows.add(new WarMember(m.optString("name","Unknown"),m.optInt("attacks",0),m.optDouble("score",0)));}
        Collections.sort(rows,(a,b)->{int c=Integer.compare(b.attacks,a.attacks);return c!=0?c:Double.compare(b.score,a.score);});
        String summary=ours.optString("name",factionName)+" "+ours.optInt("score",0)+" — "+(other==null?"Opponent":other.optString("name","Opponent"))+" "+(other==null?0:other.optInt("score",0));boolean won=report.optInt("winner",0)==factionId;
        runOnUiThread(()->{ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Latest completed ranked war #"+warId);addCard(r,card(won?"WIN":"WAR COMPLETE",summary+"\nFaction attacks: "+ours.optInt("attacks",0),won?GOOD:GOLD));int rankNo=1;for(WarMember m:rows){addCard(r,card(rankNo+". "+m.name,m.attacks+" attacks • "+String.format(Locale.US,"%.2f",m.score)+" score",m.attacks==0?BAD:BORDER));rankNo++;}setContentView(s);s.requestApplyInsets();});
    }

    private void loadChain(String key) throws Exception {
        JSONObject root=TornApiClient.getJson("/faction/chain",key);JSONObject chain=root.optJSONObject("chain");
        JSONArray members=TornApiClient.getJson("/faction/members",key).optJSONArray("members");if(members==null)members=new JSONArray();
        int online=0,available=0,onWall=0;List<String> ready=new ArrayList<>();
        for(int i=0;i<members.length();i++){
            JSONObject m=members.optJSONObject(i);if(m==null)continue;JSONObject last=m.optJSONObject("last_action"),status=m.optJSONObject("status");String lastState=last==null?"":last.optString("status","");String state=status==null?"":status.optString("state","");if("Online".equalsIgnoreCase(lastState))online++;if(m.optBoolean("is_on_wall",false))onWall++;
            boolean blocked="Hospital".equalsIgnoreCase(state)||"Jail".equalsIgnoreCase(state)||"Traveling".equalsIgnoreCase(state)||"Federal".equalsIgnoreCase(state);if(!blocked){available++;if("Online".equalsIgnoreCase(lastState))ready.add(m.optString("name","Unknown")+" • "+state);}
        }
        final JSONObject c=chain;final int fOnline=online,fAvailable=available,fOnWall=onWall;final List<String> fReady=ready;
        runOnUiThread(()->{ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Live chain readiness");if(c==null)addCard(r,card("No active chain","Torn did not return an active chain object.",BORDER));else{String body="Current: "+c.optInt("current",0)+" / "+c.optInt("max",0)+"\nTimeout: "+c.optInt("timeout",0)+" sec\nModifier: "+String.format(Locale.US,"%.2f",c.optDouble("modifier",1.0));long cooldown=c.optLong("cooldown",0);if(cooldown>0)body+="\nCooldown until: "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(cooldown*1000L));addCard(r,card("Current chain",body,c.optInt("current",0)>0?GOOD:GOLD));}
            addCard(r,card("Member readiness",fOnline+" online • "+fAvailable+" not hospital/jail/traveling • "+fOnWall+" on territory wall",BLUE));if(fReady.isEmpty())addCard(r,card("Online & available","No members currently matched the online-and-available filter.",BORDER));else for(String name:fReady)addCard(r,card(name,"Ready-state snapshot",BORDER));setContentView(s);s.requestApplyInsets();});
    }

    private void loadOc(String key) throws Exception {
        if(!factionApi){renderAccessRequired("OC Readiness requires Faction API Access because Torn restricts faction organized-crime details.");return;}
        JSONArray crimes=TornApiClient.getPagedArray("/faction/crimes?cat=available&limit=100",key,"crimes",5);
        JSONArray members=TornApiClient.getJson("/faction/members",key).optJSONArray("members");if(members==null)members=new JSONArray();
        List<String> unassigned=new ArrayList<>();for(int i=0;i<members.length();i++){JSONObject m=members.optJSONObject(i);if(m!=null&&!m.optBoolean("is_in_oc",false))unassigned.add(m.optString("name","Unknown"));}
        final JSONArray fCrimes=crimes;final List<String> fUnassigned=unassigned;
        runOnUiThread(()->{ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Recruiting + planning organized crimes");int emptyTotal=0;for(int i=0;i<fCrimes.length();i++){JSONObject crime=fCrimes.optJSONObject(i);if(crime==null)continue;JSONArray slots=crime.optJSONArray("slots");int filled=0,empty=0,missingItems=0,total=slots==null?0:slots.length();if(slots!=null)for(int j=0;j<slots.length();j++){JSONObject slot=slots.optJSONObject(j);if(slot==null)continue;if(slot.optJSONObject("user")!=null)filled++;else empty++;JSONObject req=slot.optJSONObject("item_requirement");if(req!=null&&!req.optBoolean("is_available",true))missingItems++;}emptyTotal+=empty;String body=crime.optString("status","Unknown")+" • Difficulty "+crime.optInt("difficulty",0)+"\nSlots: "+filled+" / "+total+" filled"+(empty>0?" • "+empty+" open":"")+(missingItems>0?"\nItem warnings: "+missingItems:"");long readyAt=crime.optLong("ready_at",0);if(readyAt>0)body+="\nReady: "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(readyAt*1000L));addCard(r,card(crime.optString("name","Organized Crime"),body,empty>0?GOLD:GOOD));}
            addCard(r,card("OC coverage",fCrimes.length()+" active recruiting/planning crimes • "+emptyTotal+" open slots • "+fUnassigned.size()+" members not currently in an OC",BLUE));if(!fUnassigned.isEmpty()){StringBuilder names=new StringBuilder();for(int i=0;i<Math.min(25,fUnassigned.size());i++){if(i>0)names.append(" • ");names.append(fUnassigned.get(i));}if(fUnassigned.size()>25)names.append(" • +").append(fUnassigned.size()-25).append(" more");addCard(r,card("Members not in an OC",names.toString(),BORDER));}setContentView(s);s.requestApplyInsets();});
    }

    private void renderAccessRequired(String message){runOnUiThread(()->{ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Permission required");addCard(r,card("Faction API Access",message,GOLD));setContentView(s);s.requestApplyInsets();});}
    private void renderSimple(String title,String body){runOnUiThread(()->{ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Faction operations");addCard(r,card(title,body,BORDER));setContentView(s);s.requestApplyInsets();});}

    private String warScoreText(JSONArray factions){if(factions==null)return "Score unavailable";StringBuilder b=new StringBuilder();for(int i=0;i<factions.length();i++){JSONObject f=factions.optJSONObject(i);if(f==null)continue;if(b.length()>0)b.append(" — ");b.append(f.optString("name","Faction")).append(' ').append(f.optInt("score",0));}return b.length()==0?"Score unavailable":b.toString();}

    private static final class MemberCount {final int id;final String name;final Pattern pattern;int count;MemberCount(int id,String name){this.id=id;this.name=name;this.pattern=Pattern.compile("(?i)(?<![A-Za-z0-9_])"+Pattern.quote(name)+"(?![A-Za-z0-9_])");}}
    private static final class WarMember {final String name;final int attacks;final double score;WarMember(String name,int attacks,double score){this.name=name;this.attacks=attacks;this.score=score;}}
}
