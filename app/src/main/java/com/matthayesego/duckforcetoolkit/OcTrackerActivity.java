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

public class OcTrackerActivity extends Activity {
    private static final int TAB_OPEN=0, TAB_PLANNING=1, TAB_COMPLETE=2;
    private static final int BG=Color.rgb(8,12,18), PANEL=Color.rgb(20,27,38), PANEL2=Color.rgb(27,36,49), BORDER=Color.rgb(49,63,81);
    private static final int TEXT=Color.rgb(245,248,252), MUTED=Color.rgb(151,163,179), GOLD=Color.rgb(243,184,52), BLUE=Color.rgb(88,166,255), GOOD=Color.rgb(63,185,80), BAD=Color.rgb(248,81,73);

    private SecureApiKeyStore keyStore;
    private String factionName;
    private boolean factionApi;
    private JSONArray recruiting=new JSONArray(), planning=new JSONArray(), completed=new JSONArray(), members=new JSONArray();
    private int selectedTab=TAB_OPEN;

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        keyStore=new SecureApiKeyStore(this);factionName=getIntent().getStringExtra(FactionOpsActivity.EXTRA_FACTION_NAME);factionApi=getIntent().getBooleanExtra(FactionOpsActivity.EXTRA_FACTION_API,false);if(factionName==null||factionName.trim().isEmpty())factionName="Faction";showLoading();load();
    }

    private boolean hasFactionApi(){return factionApi&&!DeveloperSettings.simulatePublicOnly(this);}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String label,int stroke){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(TEXT);b.setTextSize(13);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL2,stroke,11));return b;}
    private LinearLayout card(String title,String body,int stroke){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(15),dp(13),dp(15),dp(13));c.setBackground(rounded(PANEL,stroke,16));c.addView(text(title,17,TEXT,true));if(body!=null&&!body.isEmpty()){TextView b=text(body,13,MUTED,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(5);c.addView(b,p);}return c;}
    private void addCard(LinearLayout r,LinearLayout c){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(9);r.addView(c,p);}
    private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);s.setPadding(dp(16),dp(16),dp(16),dp(28));return s;}
    private LinearLayout root(ScrollView s){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));return r;}

    private void addHeader(LinearLayout r,String subtitle){Button back=button("← Companion",BORDER);back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(dp(124),dp(44)));TextView title=text("OC Tracker",27,TEXT,true);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp(14);r.addView(title,tp);TextView sub=text(factionName+" • "+subtitle,13,MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(4);sp.bottomMargin=dp(12);r.addView(sub,sp);}
    private void showLoading(){ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Loading organized crimes…");addCard(r,card("Quick OC status","Loading recruiting, planning and recent completed crimes into one fast screen.",BLUE));setContentView(s);}

    private void load(){
        if(!hasFactionApi()){renderError("Faction API Access is required for organized-crime details.");return;}
        String key=keyStore.load();if(key==null||key.trim().isEmpty()){renderError("Reconnect your Torn API key to use the OC tracker.");return;}
        new Thread(()->{try{
            recruiting=TornApiClient.getPagedArray("/faction/crimes?cat=recruiting&sort=DESC&limit=100",key,"crimes",3);
            planning=TornApiClient.getPagedArray("/faction/crimes?cat=planning&sort=DESC&limit=100",key,"crimes",3);
            completed=TornApiClient.getPagedArray("/faction/crimes?cat=completed&sort=DESC&limit=20",key,"crimes",1);
            JSONArray m=TornApiClient.getJson("/faction/members",key).optJSONArray("members");members=m==null?new JSONArray():m;
            runOnUiThread(this::render);
        }catch(Exception e){renderError(e.getMessage()==null?"Unable to load organized crimes.":e.getMessage());}}).start();
    }

    private void renderError(String message){runOnUiThread(()->{ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Data unavailable");addCard(r,card("Unable to load",message,BAD));setContentView(s);});}

    private void render(){
        ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Fast recruiting, planning and completion view");
        addSummary(r);addTabs(r);
        if(selectedTab==TAB_PLANNING)renderPlanning(r);else if(selectedTab==TAB_COMPLETE)renderCompleted(r);else renderOpen(r);
        setContentView(s);
    }

    private void addSummary(LinearLayout r){
        int openSlots=0;for(int i=0;i<recruiting.length();i++)openSlots+=slotStats(recruiting.optJSONObject(i))[1];
        int successes=0;for(int i=0;i<completed.length();i++){JSONObject c=completed.optJSONObject(i);if(c!=null&&c.optString("status","").toLowerCase(Locale.US).contains("success"))successes++;}
        addCard(r,card("OC status",recruiting.length()+" open • "+planning.length()+" planning • "+completed.length()+" recent complete\n"+openSlots+" recruiting slots open • "+successes+" recent successes",BLUE));
    }

    private void addTabs(LinearLayout r){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);String[] labels={"Open "+recruiting.length(),"Planning "+planning.length(),"Complete "+completed.length()};
        for(int i=0;i<labels.length;i++){final int tab=i;Button b=button(labels[i],selectedTab==i?GOLD:BORDER);b.setOnClickListener(v->{selectedTab=tab;render();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(46),1f);if(i>0)p.leftMargin=dp(6);row.addView(b,p);}LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));rp.bottomMargin=dp(12);r.addView(row,rp);
    }

    private void renderOpen(LinearLayout r){
        TextView h=text("OPEN / RECRUITING",12,MUTED,true);h.setLetterSpacing(.08f);r.addView(h);
        if(recruiting.length()==0)addCard(r,card("No recruiting OCs","No organized crimes are currently recruiting.",GOOD));
        for(int i=0;i<recruiting.length();i++){JSONObject c=recruiting.optJSONObject(i);if(c==null)continue;int[] stats=slotStats(c);String body="Difficulty "+c.optInt("difficulty",0)+" • "+stats[0]+" / "+stats[2]+" filled"+(stats[1]>0?" • "+stats[1]+" open":"")+(stats[3]>0?"\nMissing item warnings: "+stats[3]:"");addCard(r,card(c.optString("name","Organized Crime"),body,stats[1]>0?GOLD:GOOD));}
        List<String> unassigned=new ArrayList<>();for(int i=0;i<members.length();i++){JSONObject m=members.optJSONObject(i);if(m!=null&&!m.optBoolean("is_in_oc",false))unassigned.add(m.optString("name","Unknown"));}
        if(!unassigned.isEmpty()){StringBuilder b=new StringBuilder();for(int i=0;i<Math.min(20,unassigned.size());i++){if(i>0)b.append(" • ");b.append(unassigned.get(i));}if(unassigned.size()>20)b.append(" • +").append(unassigned.size()-20).append(" more");addCard(r,card("Members not in an OC",unassigned.size()+" members\n"+b,BORDER));}
    }

    private void renderPlanning(LinearLayout r){
        TextView h=text("PLANNING / READY",12,MUTED,true);h.setLetterSpacing(.08f);r.addView(h);
        if(planning.length()==0)addCard(r,card("No planning OCs","No organized crimes are currently in planning.",BORDER));
        for(int i=0;i<planning.length();i++){JSONObject c=planning.optJSONObject(i);if(c==null)continue;int[] stats=slotStats(c);String body="Difficulty "+c.optInt("difficulty",0)+" • "+stats[0]+" / "+stats[2]+" filled"+(stats[3]>0?"\nMissing item warnings: "+stats[3]:"");long ready=c.optLong("ready_at",0L);if(ready>0)body+="\nReady: "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(ready*1000L));addCard(r,card(c.optString("name","Organized Crime"),body,stats[3]>0?GOLD:GOOD));}
    }

    private void renderCompleted(LinearLayout r){
        TextView h=text("RECENT COMPLETE",12,MUTED,true);h.setLetterSpacing(.08f);r.addView(h);
        if(completed.length()==0)addCard(r,card("No recent completed OCs","No completed organized crimes were returned.",BORDER));
        for(int i=0;i<completed.length();i++){JSONObject c=completed.optJSONObject(i);if(c==null)continue;String status=c.optString("status","Complete");String body=status+" • Difficulty "+c.optInt("difficulty",0);long executed=c.optLong("executed_at",0L);if(executed>0)body+="\nCompleted: "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(executed*1000L));int stroke=status.toLowerCase(Locale.US).contains("success")?GOOD:status.toLowerCase(Locale.US).contains("fail")?BAD:BORDER;addCard(r,card(c.optString("name","Organized Crime"),body,stroke));}
    }

    private int[] slotStats(JSONObject crime){int filled=0,empty=0,total=0,missing=0;if(crime==null)return new int[]{0,0,0,0};JSONArray slots=crime.optJSONArray("slots");if(slots==null)return new int[]{0,0,0,0};total=slots.length();for(int i=0;i<slots.length();i++){JSONObject slot=slots.optJSONObject(i);if(slot==null)continue;if(slot.optJSONObject("user")!=null)filled++;else empty++;JSONObject req=slot.optJSONObject("item_requirement");if(req!=null&&!req.optBoolean("is_available",true))missing++;}return new int[]{filled,empty,total,missing};}
}
