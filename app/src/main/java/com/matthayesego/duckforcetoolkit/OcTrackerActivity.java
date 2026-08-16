package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class OcTrackerActivity extends Activity {
    private static final int TAB_OPEN=0, TAB_PLANNING=1, TAB_COMPLETE=2;
    private static final int BG=Color.rgb(8,12,18), PANEL=Color.rgb(20,27,38), PANEL2=Color.rgb(27,36,49), BORDER=Color.rgb(49,63,81);
    private static final int TEXT=Color.rgb(245,248,252), MUTED=Color.rgb(151,163,179), GOLD=Color.rgb(243,184,52), BLUE=Color.rgb(88,166,255), GOOD=Color.rgb(63,185,80), BAD=Color.rgb(248,81,73);
    private SecureApiKeyStore keyStore;
    private int factionId;
    private String factionName,position;
    private boolean factionApi,leadershipMode;
    private JSONArray recruiting=new JSONArray(), planning=new JSONArray(), completed=new JSONArray(), members=new JSONArray(), personalAvailable=new JSONArray();
    private JSONObject personalCurrent;
    private int selectedTab=TAB_OPEN;

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        keyStore=new SecureApiKeyStore(this);
        factionId=getIntent().getIntExtra(FactionOpsActivity.EXTRA_FACTION_ID,0);
        factionName=getIntent().getStringExtra(FactionOpsActivity.EXTRA_FACTION_NAME);
        factionApi=getIntent().getBooleanExtra(FactionOpsActivity.EXTRA_FACTION_API,false);
        position=getIntent().getStringExtra(DeveloperConsoleActivity.EXTRA_POSITION);
        if(factionName==null||factionName.trim().isEmpty())factionName="Faction";
        if(position==null||position.trim().isEmpty())position="Member";
        leadershipMode=AccessPolicy.isLeaderPosition(position)&&!DeveloperPreviewStore.isMemberPreview(this);
        showLoading();load();
    }

    private boolean hasFactionApi(){return factionApi&&!DeveloperSettings.simulatePublicOnly(this);}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String label,int stroke){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(TEXT);b.setTextSize(13);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL2,stroke,11));return b;}
    private LinearLayout card(String title,String body,int stroke){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(15),dp(13),dp(15),dp(13));c.setBackground(rounded(PANEL,stroke,16));c.addView(text(title,17,TEXT,true));if(body!=null&&!body.isEmpty()){TextView b=text(body,13,MUTED,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(5);c.addView(b,p);}return c;}
    private void addCard(LinearLayout r,LinearLayout c){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(9);r.addView(c,p);}
    @SuppressWarnings("deprecation") private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);int l=dp(16),t=dp(18),r=dp(16),b=dp(30);s.setPadding(l,t,r,b);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),b+i.getSystemWindowInsetBottom());return i;});return s;}
    private LinearLayout root(ScrollView s){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));return r;}
    private void finishView(ScrollView s){setContentView(s);s.requestApplyInsets();}
    private void addHeader(LinearLayout r,String subtitle){Button back=button("← Companion",BORDER);back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(dp(124),dp(44)));TextView title=text("OC Tracker",27,TEXT,true);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp(14);r.addView(title,tp);TextView sub=text(factionName+" • "+subtitle,13,MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(4);sp.bottomMargin=dp(12);r.addView(sub,sp);}
    private void showLoading(){ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Loading organized crimes…");addCard(r,card("Quick OC status",leadershipMode?"Loading the faction-wide OC command view.":"Loading your personal organized-crime information.",BLUE));finishView(s);}

    private void load(){
        String key=keyStore.load();
        if(key==null||key.trim().isEmpty()){renderError("Reconnect your Torn API key to use the OC tracker.");return;}
        // Leadership launches must attempt Torn's faction endpoint directly. The endpoint is the
        // authoritative permission check; a stale cached factionApiAccess flag must never hide OC command data.
        if(leadershipMode||hasFactionApi())loadFaction(key);else loadPersonal(key);
    }

    private void loadFaction(String key){
        new Thread(()->{ExecutorService pool=Executors.newFixedThreadPool(4);try{
            JSONArray cachedMembers=FactionMemberCache.load(factionId);
            Future<JSONArray> recruitingFuture=pool.submit(()->TornApiClient.getPagedArray("/faction/crimes?cat=recruiting&sort=DESC&limit=100",key,"crimes",3));
            Future<JSONArray> planningFuture=pool.submit(()->TornApiClient.getPagedArray("/faction/crimes?cat=planning&sort=DESC&limit=100",key,"crimes",3));
            Future<JSONArray> completedFuture=pool.submit(()->TornApiClient.getPagedArray("/faction/crimes?cat=completed&sort=DESC&limit=20",key,"crimes",1));
            Future<JSONArray> membersFuture=cachedMembers==null?pool.submit(()->{JSONArray m=TornApiClient.getJson("/faction/members",key).optJSONArray("members");return m==null?new JSONArray():m;}):null;
            recruiting=recruitingFuture.get();planning=planningFuture.get();completed=completedFuture.get();members=cachedMembers!=null?cachedMembers:membersFuture.get();
            if(cachedMembers==null)FactionMemberCache.save(factionId,members);
            runOnUiThread(this::renderFaction);
        }catch(Exception e){
            Throwable cause=e.getCause()==null?e:e.getCause();String message=cause.getMessage()==null?"Unable to load organized crimes.":cause.getMessage();
            if(leadershipMode)message="Leadership OC command requires Torn Faction API Access. Torn rejected the faction-wide OC request: "+message;
            renderError(message);
        }finally{pool.shutdownNow();}}).start();
    }

    private void loadPersonal(String key){
        new Thread(()->{ExecutorService pool=Executors.newFixedThreadPool(2);try{
            Future<JSONObject> currentFuture=pool.submit(()->TornApiClient.getJson("/user/organizedcrime",key));
            Future<JSONObject> availableFuture=pool.submit(()->TornApiClient.getJson("/user/organizedcrimes",key));
            JSONObject currentRoot=currentFuture.get();JSONObject availableRoot=availableFuture.get();
            Object raw=currentRoot.opt("organizedCrime");
            personalCurrent=raw instanceof JSONObject?(JSONObject)raw:null;
            if(personalCurrent!=null&&personalCurrent.has("error"))personalCurrent=null;
            JSONArray a=availableRoot.optJSONArray("organizedcrimes");personalAvailable=a==null?new JSONArray():a;
            runOnUiThread(this::renderPersonal);
        }catch(Exception e){Throwable cause=e.getCause()==null?e:e.getCause();renderError(cause.getMessage()==null?"Unable to load your organized-crime details.":cause.getMessage());}finally{pool.shutdownNow();}}).start();
    }

    private void renderError(String message){runOnUiThread(()->{ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Data unavailable");addCard(r,card("Unable to load",message,BAD));finishView(s);});}

    private void renderPersonal(){
        ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Personal OC view");
        addCard(r,card("Personal access active","Your Torn key can show your own current OC and recruiting slots. The full faction-wide OC roster remains inside Leadership for authorized users.",BLUE));
        if(personalCurrent==null){
            addCard(r,card("My current OC","You are not currently assigned to an organized crime.",GOLD));
        }else{
            String status=personalCurrent.optString("status","Unknown");
            String body="Status: "+status+" • Difficulty "+personalCurrent.optInt("difficulty",0);
            long ready=personalCurrent.optLong("ready_at",0L);if(ready>0)body+="\nReady: "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(ready*1000L));
            JSONObject mySlot=findMySlot(personalCurrent);
            if(mySlot!=null){String slotLabel=slotLabel(mySlot);int cpr=mySlot.optInt("checkpoint_pass_rate",0);body+="\nSlot: "+slotLabel+" • CPR "+cpr+"%";JSONObject req=mySlot.optJSONObject("item_requirement");if(req!=null)body+=" • Item "+(req.optBoolean("is_available",false)?"ready":"missing");}
            addCard(r,card(personalCurrent.optString("name","My current OC"),body,status.toLowerCase(Locale.US).contains("planning")?GOOD:GOLD));
        }

        TextView h=text("AVAILABLE RECRUITING SLOTS",12,MUTED,true);h.setLetterSpacing(.08f);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);hp.topMargin=dp(5);hp.bottomMargin=dp(7);r.addView(h,hp);
        if(personalAvailable.length()==0){addCard(r,card("No open slots for you","Torn did not return any recruiting OC slots currently available to your player.",BORDER));}
        for(int i=0;i<personalAvailable.length();i++){
            JSONObject crime=personalAvailable.optJSONObject(i);if(crime==null)continue;
            JSONArray slots=crime.optJSONArray("slots");StringBuilder body=new StringBuilder("Difficulty ").append(crime.optInt("difficulty",0));
            int shown=0;for(int j=0;slots!=null&&j<slots.length();j++){JSONObject slot=slots.optJSONObject(j);if(slot==null||slot.optJSONObject("user")!=null)continue;body.append("\n").append(slotLabel(slot)).append(" • CPR ").append(slot.optInt("checkpoint_pass_rate",0)).append('%');JSONObject req=slot.optJSONObject("item_requirement");if(req!=null)body.append(" • item ").append(req.optBoolean("is_available",false)?"ready":"missing");shown++;}
            addCard(r,card(crime.optString("name","Recruiting OC"),body.toString(),shown>0?GOOD:BORDER));
        }
        finishView(s);
    }

    private String slotLabel(JSONObject slot){
        JSONObject info=slot==null?null:slot.optJSONObject("position_info");
        if(info!=null){String label=info.optString("label","");if(!label.isEmpty())return label;String name=info.optString("name","");int number=info.optInt("number",0);if(!name.isEmpty())return number>0?name+" #"+number:name;}
        String legacy=slot==null?"":slot.optString("position","");return legacy.isEmpty()?"Open slot":legacy;
    }

    private JSONObject findMySlot(JSONObject crime){
        int playerId=0;String key=keyStore.load();if(key!=null){AuthSession session=TornApiClient.cachedSession(key);if(session!=null)playerId=session.playerId;}
        JSONArray slots=crime==null?null:crime.optJSONArray("slots");
        JSONObject fallback=null;
        for(int i=0;slots!=null&&i<slots.length();i++){JSONObject slot=slots.optJSONObject(i);if(slot==null)continue;JSONObject user=slot.optJSONObject("user");if(user==null)continue;if(fallback==null)fallback=slot;if(playerId>0&&user.optInt("id",0)==playerId)return slot;}
        return fallback;
    }

    private void renderFaction(){ScrollView s=shell();LinearLayout r=root(s);addHeader(r,leadershipMode?"Leadership OC command view":"Faction OC view");addSummary(r);addTabs(r);if(selectedTab==TAB_PLANNING)renderPlanning(r);else if(selectedTab==TAB_COMPLETE)renderCompleted(r);else renderOpen(r);finishView(s);}

    private void addSummary(LinearLayout r){
        int openSlots=0,filledSlots=0,totalSlots=0,missingItems=0;
        for(int i=0;i<recruiting.length();i++){int[] s=slotStats(recruiting.optJSONObject(i));filledSlots+=s[0];openSlots+=s[1];totalSlots+=s[2];missingItems+=s[3];}
        for(int i=0;i<planning.length();i++){int[] s=slotStats(planning.optJSONObject(i));filledSlots+=s[0];openSlots+=s[1];totalSlots+=s[2];missingItems+=s[3];}
        int successes=0;for(int i=0;i<completed.length();i++){JSONObject c=completed.optJSONObject(i);if(c!=null&&c.optString("status","").toLowerCase(Locale.US).contains("success"))successes++;}
        int assigned=0;for(int i=0;i<members.length();i++){JSONObject m=members.optJSONObject(i);if(m!=null&&m.optBoolean("is_in_oc",false))assigned++;}int unassigned=Math.max(0,members.length()-assigned);
        String body=(recruiting.length()+planning.length())+" active OCs • "+recruiting.length()+" recruiting • "+planning.length()+" planning\n"
                +filledSlots+" / "+totalSlots+" active slots filled • "+openSlots+" open • "+missingItems+" missing-item warnings\n"
                +assigned+" members assigned • "+unassigned+" not currently in an OC\n"
                +completed.length()+" recent complete • "+successes+" recent successes";
        addCard(r,card(leadershipMode?"OC command snapshot":"OC status",body,BLUE));
    }

    private void addTabs(LinearLayout r){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);String[] labels={"Open "+recruiting.length(),"Planning "+planning.length(),"Complete "+completed.length()};for(int i=0;i<labels.length;i++){final int tab=i;Button b=button(labels[i],selectedTab==i?GOLD:BORDER);b.setOnClickListener(v->{selectedTab=tab;renderFaction();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(46),1f);if(i>0)p.leftMargin=dp(6);row.addView(b,p);}LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));rp.bottomMargin=dp(12);r.addView(row,rp);}
    private void renderOpen(LinearLayout r){TextView h=text("OPEN / RECRUITING",12,MUTED,true);h.setLetterSpacing(.08f);r.addView(h);if(recruiting.length()==0)addCard(r,card("No recruiting OCs","No organized crimes are currently recruiting.",GOOD));for(int i=0;i<recruiting.length();i++){JSONObject c=recruiting.optJSONObject(i);if(c==null)continue;int[] stats=slotStats(c);String body="Difficulty "+c.optInt("difficulty",0)+" • "+stats[0]+" / "+stats[2]+" filled"+(stats[1]>0?" • "+stats[1]+" open":"")+(stats[3]>0?"\nMissing item warnings: "+stats[3]:"");addCard(r,card(c.optString("name","Organized Crime"),body,stats[1]>0?GOLD:GOOD));}List<String> unassigned=new ArrayList<>();for(int i=0;i<members.length();i++){JSONObject m=members.optJSONObject(i);if(m!=null&&!m.optBoolean("is_in_oc",false))unassigned.add(m.optString("name","Unknown"));}if(!unassigned.isEmpty()){StringBuilder b=new StringBuilder();for(int i=0;i<Math.min(20,unassigned.size());i++){if(i>0)b.append(" • ");b.append(unassigned.get(i));}if(unassigned.size()>20)b.append(" • +").append(unassigned.size()-20).append(" more");addCard(r,card("Members not in an OC",unassigned.size()+" members\n"+b,BORDER));}}
    private void renderPlanning(LinearLayout r){TextView h=text("PLANNING / READY",12,MUTED,true);h.setLetterSpacing(.08f);r.addView(h);if(planning.length()==0)addCard(r,card("No planning OCs","No organized crimes are currently in planning.",BORDER));for(int i=0;i<planning.length();i++){JSONObject c=planning.optJSONObject(i);if(c==null)continue;int[] stats=slotStats(c);String body="Difficulty "+c.optInt("difficulty",0)+" • "+stats[0]+" / "+stats[2]+" filled"+(stats[3]>0?"\nMissing item warnings: "+stats[3]:"");long ready=c.optLong("ready_at",0L);if(ready>0)body+="\nReady: "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(ready*1000L));addCard(r,card(c.optString("name","Organized Crime"),body,stats[3]>0?GOLD:GOOD));}}
    private void renderCompleted(LinearLayout r){TextView h=text("RECENT COMPLETE",12,MUTED,true);h.setLetterSpacing(.08f);r.addView(h);if(completed.length()==0)addCard(r,card("No recent completed OCs","No completed organized crimes were returned.",BORDER));for(int i=0;i<completed.length();i++){JSONObject c=completed.optJSONObject(i);if(c==null)continue;String status=c.optString("status","Complete");String body=status+" • Difficulty "+c.optInt("difficulty",0);long executed=c.optLong("executed_at",0L);if(executed>0)body+="\nCompleted: "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(executed*1000L));int stroke=status.toLowerCase(Locale.US).contains("success")?GOOD:status.toLowerCase(Locale.US).contains("fail")?BAD:BORDER;addCard(r,card(c.optString("name","Organized Crime"),body,stroke));}}
    private int[] slotStats(JSONObject crime){int filled=0,empty=0,total=0,missing=0;if(crime==null)return new int[]{0,0,0,0};JSONArray slots=crime.optJSONArray("slots");if(slots==null)return new int[]{0,0,0,0};total=slots.length();for(int i=0;i<slots.length();i++){JSONObject slot=slots.optJSONObject(i);if(slot==null)continue;if(slot.optJSONObject("user")!=null)filled++;else empty++;JSONObject req=slot.optJSONObject("item_requirement");if(req!=null&&!req.optBoolean("is_available",true))missing++;}return new int[]{filled,empty,total,missing};}
}
