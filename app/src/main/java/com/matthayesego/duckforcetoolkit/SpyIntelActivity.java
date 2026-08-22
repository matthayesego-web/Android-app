package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Leadership view of official Torn faction stat reports (spies). */
public class SpyIntelActivity extends Activity {
    private static final int BG=Color.rgb(5,8,12),PANEL=Color.rgb(12,18,26),PANEL2=Color.rgb(8,13,20),BORDER=Color.rgb(36,47,61),TEXT=Color.rgb(246,248,251),MUTED=Color.rgb(145,155,169),GOLD=Color.rgb(241,190,86),BLUE=Color.rgb(82,153,235),GREEN=Color.rgb(76,190,102),RED=Color.rgb(239,88,82),PURPLE=Color.rgb(170,115,238);
    private SecureApiKeyStore keyStore;
    private String key,position="Member",factionName="Faction";
    private int factionId;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        keyStore=new SecureApiKeyStore(this);key=keyStore.load();
        position=getIntent().getStringExtra(DeveloperConsoleActivity.EXTRA_POSITION);if(position==null)position="Member";
        factionId=getIntent().getIntExtra(FactionOpsActivity.EXTRA_FACTION_ID,0);String f=getIntent().getStringExtra(FactionOpsActivity.EXTRA_FACTION_NAME);if(f!=null&&!f.isBlank())factionName=f;
        if(!AccessPolicy.isLeaderPosition(position)){renderError("Spy Intel is restricted to faction leadership.");return;}
        if(key==null||key.isBlank()){renderError("Reconnect your Torn API key to read faction spy reports.");return;}
        showLoading("Loading official faction stat reports…");loadRecent();
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private GradientDrawable gradient(int a,int b,int stroke,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private TextView eye(String value,int color){TextView t=text(value,9.5f,color,true);t.setLetterSpacing(.12f);return t;}
    private Button button(String value,int accent){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextColor(TEXT);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL2,accent,13));return b;}
    private LinearLayout card(int accent){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(16),dp(18),dp(16));c.setBackground(gradient(PANEL,PANEL2,accent==Color.TRANSPARENT?BORDER:accent,20));return c;}
    private void add(LinearLayout root,LinearLayout c){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(11);root.addView(c,p);}
    @SuppressWarnings("deprecation") private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setClipToPadding(false);s.setBackgroundColor(BG);int l=dp(18),t=dp(14),r=dp(18),bb=dp(28);s.setPadding(l,t,r,bb);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),bb+i.getSystemWindowInsetBottom());return i;});return s;}
    private LinearLayout root(ScrollView s){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));return r;}
    private void header(LinearLayout r,String sub){Button back=button("← Leadership",BORDER);back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(dp(150),dp(44)));TextView e=eye("LEADERSHIP • OFFICIAL TORN INTEL",PURPLE);LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);ep.topMargin=dp(18);r.addView(e,ep);r.addView(text("Spy Intel",30,TEXT,true));TextView st=text(factionName+" • "+sub,13,MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(5);sp.bottomMargin=dp(16);r.addView(st,sp);}
    private void finish(ScrollView s){setContentView(s);s.requestApplyInsets();}
    private void showLoading(String msg){ScrollView s=shell();LinearLayout r=root(s);header(r,"Loading");LinearLayout c=card(BORDER);c.addView(eye("READING REPORTS",GOLD));c.addView(text(msg,18,TEXT,true));add(r,c);finish(s);}
    private void renderError(String msg){runOnUiThread(()->{ScrollView s=shell();LinearLayout r=root(s);header(r,"Unavailable");LinearLayout c=card(RED);c.addView(text("Unable to load Spy Intel",18,TEXT,true));c.addView(text(msg,13,MUTED,false));add(r,c);finish(s);});}

    private void loadRecent(){new Thread(()->{try{JSONObject root=TornApiClient.getJson("/faction/reports?cat=stats&limit=100&sort=DESC",key);JSONArray reports=root.optJSONArray("reports");runOnUiThread(()->render(reports==null?new JSONArray():reports,0));}catch(Exception e){renderError(e.getMessage()==null?"Faction stat reports were unavailable.":e.getMessage());}},"TornFCA-SpyIntel").start();}
    private void loadTarget(int target){showLoading("Finding the newest spy for player #"+target+"…");new Thread(()->{try{JSONObject root=TornApiClient.getJson("/faction/reports?cat=stats&target="+target+"&limit=100&sort=DESC",key);JSONArray reports=root.optJSONArray("reports");runOnUiThread(()->render(reports==null?new JSONArray():reports,target));}catch(Exception e){renderError(e.getMessage()==null?"No spy report was available for that player.":e.getMessage());}},"TornFCA-SpyTarget").start();}

    private void render(JSONArray reports,int requestedTarget){
        ScrollView s=shell();LinearLayout r=root(s);header(r,"Official faction stat reports");
        LinearLayout search=card(BLUE);search.addView(eye("PLAYER LOOKUP",BLUE));search.addView(text("Search by Torn player ID. TornFCA shows only official stats reports your faction can access; estimates are never labeled as spies.",12.5f,MUTED,false));
        EditText target=new EditText(this);target.setHint("Torn player ID");target.setHintTextColor(MUTED);target.setTextColor(TEXT);target.setSingleLine(true);target.setInputType(InputType.TYPE_CLASS_NUMBER);target.setPadding(dp(13),0,dp(13),0);target.setBackground(rounded(PANEL2,BORDER,11));if(requestedTarget>0)target.setText(String.valueOf(requestedTarget));LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50));tp.topMargin=dp(10);search.addView(target,tp);
        Button go=button("Find Latest Spy",BLUE);LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));gp.topMargin=dp(8);search.addView(go,gp);go.setOnClickListener(v->{try{int id=Integer.parseInt(target.getText().toString().trim());if(id>0)loadTarget(id);}catch(Exception ignored){target.setError("Enter a Torn player ID");}});add(r,search);

        if(requestedTarget>0){Button all=button("Show Recent Faction Spies",BORDER);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));ap.bottomMargin=dp(12);r.addView(all,ap);all.setOnClickListener(v->{showLoading("Loading recent faction spies…");loadRecent();});}
        if(reports.length()==0){LinearLayout none=card(BORDER);none.addView(text(requestedTarget>0?"No spy found":"No stat reports returned",18,TEXT,true));none.addView(text("This requires a Limited Access Torn key and access to your faction's reports. A missing report does not mean the player has no battle stats; it means Torn did not return a faction spy report here.",12.5f,MUTED,false));add(r,none);finish(s);return;}

        Set<Integer> seen=new LinkedHashSet<>();int shown=0;
        for(int i=0;i<reports.length();i++){
            JSONObject row=reports.optJSONObject(i);if(row==null||!"stats".equalsIgnoreCase(row.optString("type","")))continue;int id=row.optInt("target_id",0);if(id<=0)continue;if(requestedTarget==0&&!seen.add(id))continue;
            add(r,spyCard(row));shown++;if(requestedTarget>0&&shown>=8)break;if(requestedTarget==0&&shown>=25)break;
        }
        if(shown==0){LinearLayout none=card(BORDER);none.addView(text("No usable stat reports",17,TEXT,true));add(r,none);}
        TextView foot=text("Official Torn faction reports • values may be partial • newest report shown per player",10.5f,MUTED,false);foot.setGravity(Gravity.CENTER);r.addView(foot);finish(s);
    }

    private LinearLayout spyCard(JSONObject row){
        int id=row.optInt("target_id",0);long ts=row.optLong("timestamp",0);JSONObject rep=row.optJSONObject("report");if(rep==null)rep=new JSONObject();
        Long str=nullableLong(rep,"strength"),spd=nullableLong(rep,"speed"),def=nullableLong(rep,"defense"),dex=nullableLong(rep,"dexterity"),total=nullableLong(rep,"total");int known=0;if(str!=null)known++;if(spd!=null)known++;if(def!=null)known++;if(dex!=null)known++;
        boolean full=known==4;int accent=full?GREEN:GOLD;LinearLayout c=card(accent);c.addView(eye((full?"FULL SPY":"PARTIAL SPY")+" • PLAYER #"+id,accent));c.addView(text(total==null?"Known total unavailable":fmt(total)+" total",21,TEXT,true));
        String body="STR "+fmtNullable(str)+"\nDEF "+fmtNullable(def)+"\nSPD "+fmtNullable(spd)+"\nDEX "+fmtNullable(dex)+"\nReport age: "+age(ts)+" • "+formatTime(ts)+"\nReporter #"+row.optInt("reporter_id",0);c.addView(text(body,13,MUTED,false));
        Button profile=button("Open Torn Profile",BLUE);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));pp.topMargin=dp(10);c.addView(profile,pp);profile.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.torn.com/profiles.php?XID="+id)));}catch(Exception ignored){}});return c;
    }
    private Long nullableLong(JSONObject o,String k){if(o==null||o.isNull(k)||!o.has(k))return null;try{return o.getLong(k);}catch(Exception e){return null;}}
    private String fmtNullable(Long v){return v==null?"Unknown":fmt(v);}
    private String fmt(long v){return NumberFormat.getIntegerInstance(Locale.US).format(v);}
    private String formatTime(long ts){if(ts<=0)return"Unknown time";return new SimpleDateFormat("MMM d, yyyy HH:mm",Locale.US).format(new Date(ts*1000L));}
    private String age(long ts){if(ts<=0)return"unknown";long sec=Math.max(0,System.currentTimeMillis()/1000L-ts);if(sec<3600)return Math.max(1,sec/60)+"m";if(sec<86400)return sec/3600+"h";return sec/86400+"d";}
}
