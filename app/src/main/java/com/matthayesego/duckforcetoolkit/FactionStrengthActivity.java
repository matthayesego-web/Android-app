package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Faction strength provider. Uses the signed-in player's own Torn key only after explicit FFScouter opt-in. */
public class FactionStrengthActivity extends Activity {
    public static final String EXTRA_SCOUTING_TARGET="scouting_target";
    private static final int BG=Color.rgb(6,9,13),PANEL=Color.rgb(14,20,29),PANEL2=Color.rgb(9,14,21),BORDER=Color.rgb(45,55,69),TEXT=Color.rgb(244,246,249),MUTED=Color.rgb(154,164,178),GOLD=Color.rgb(241,194,106),BLUE=Color.rgb(88,166,255),GREEN=Color.rgb(63,185,80),RED=Color.rgb(248,81,73);
    private SecureApiKeyStore tornStore;private int factionId;private String factionName;private boolean ffPremium=false;private String entitlement="none";private boolean scoutingTarget=false;

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        tornStore=new SecureApiKeyStore(this);factionId=getIntent().getIntExtra(FactionOpsActivity.EXTRA_FACTION_ID,0);factionName=getIntent().getStringExtra(FactionOpsActivity.EXTRA_FACTION_NAME);scoutingTarget=getIntent().getBooleanExtra(EXTRA_SCOUTING_TARGET,false);if(factionName==null||factionName.trim().isEmpty())factionName="Faction";
        String key=tornStore.load();if(key==null||key.trim().isEmpty()){renderError("Reconnect your Torn API key before using FFScouter.");return;}
        if(!FFScouterClient.hasConsent(this,key))renderProviderConsent();else checkRegistration();
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private GradientDrawable gradient(int a,int b,int stroke,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String label,int stroke){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(TEXT);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL2,stroke,12));return b;}
    private LinearLayout card(String title,String body,int stroke){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(17),dp(15),dp(17),dp(15));c.setBackground(rounded(PANEL,stroke,18));c.addView(text(title,18,TEXT,true));if(body!=null&&!body.isEmpty()){TextView b=text(body,13,MUTED,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(6);c.addView(b,p);}return c;}
    private void addCard(LinearLayout root,LinearLayout c){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(10);root.addView(c,p);}
    @SuppressWarnings("deprecation") private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);int l=dp(16),t=dp(18),r=dp(16),b=dp(30);s.setPadding(l,t,r,b);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),b+i.getSystemWindowInsetBottom());return i;});return s;}
    private LinearLayout root(ScrollView s){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));return r;}
    private void header(LinearLayout r,String subtitle){Button back=button("← War / Faction",BORDER);back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(dp(144),dp(44)));LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER_HORIZONTAL);hero.setPadding(dp(18),dp(18),dp(18),dp(18));hero.setBackground(gradient(Color.rgb(20,37,55),Color.rgb(14,18,24),BLUE,22));TextView brand=text(scoutingTarget?"WAR CENTER • FFSCOUTER":"TORNFCA • FFSCOUTER INTEL",10,BLUE,true);brand.setLetterSpacing(.13f);hero.addView(brand);TextView title=text(scoutingTarget?"Opponent Strength Intel":"Faction Strength Intel",29,TEXT,true);title.setGravity(Gravity.CENTER);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp(5);hero.addView(title,tp);TextView sub=text(subtitle,13,MUTED,false);sub.setGravity(Gravity.CENTER);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(5);hero.addView(sub,sp);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);hp.topMargin=dp(14);hp.bottomMargin=dp(14);r.addView(hero,hp);}

    /** No FFScouter network request occurs before this explicit, key-specific opt-in. */
    private void renderProviderConsent(){
        ScrollView s=shell();LinearLayout r=root(s);header(r,"FFScouter is an optional third-party provider");
        addCard(r,card("One key across TornFCA + FFScouter","TornFCA can use the same 16-character Limited Access Torn API key you already connected. Full Access is not required for FFScouter core scouting. If enabled, your saved key is sent directly to FFScouter over HTTPS for FFScouter requests; it is not routed through the TornFCA faction backend.",BLUE));
        addCard(r,card("Read before enabling","FFScouter is a separate service with its own Data Policy and Terms. Its homepage explains what it stores, how it uses the key, and how to remove the key. TornFCA will not contact FFScouter until you opt in below.",GOLD));
        Button terms=button("Open FFScouter Terms / Data Policy",BLUE);terms.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(FFScouterClient.HOMEPAGE))));r.addView(terms,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48)));
        CheckBox agree=new CheckBox(this);agree.setText("I have read FFScouter's Data Policy and Terms and want TornFCA to use my current Torn API key with FFScouter.");agree.setTextColor(TEXT);agree.setButtonTintList(android.content.res.ColorStateList.valueOf(GOLD));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);ap.topMargin=dp(12);r.addView(agree,ap);
        TextView status=text("This consent is tied to the current API key. Replacing the key requires a new opt-in.",12,MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(8);r.addView(status,sp);
        Button enable=button("Enable FFScouter",GREEN);LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50));ep.topMargin=dp(10);r.addView(enable,ep);
        enable.setOnClickListener(v->{if(!agree.isChecked()){status.setText("Read and accept FFScouter's Data Policy and Terms first.");status.setTextColor(RED);return;}String key=tornStore.load();if(key==null){status.setText("Your Torn key is no longer available.");status.setTextColor(RED);return;}FFScouterClient.setConsent(this,key,true);checkRegistration();});
        setContentView(s);s.requestApplyInsets();
    }

    private void checkRegistration(){
        String key=tornStore.load();if(key==null||key.trim().isEmpty()){renderError("Reconnect your Torn API key before using FFScouter.");return;}
        if(!FFScouterClient.hasConsent(this,key)){renderProviderConsent();return;}
        showLoading("Checking your FFScouter registration…");new Thread(()->{try{JSONObject status=FFScouterClient.checkKey(key);boolean registered=status.optBoolean("is_registered",false);ffPremium=status.optBoolean("is_premium",false);entitlement=status.optString("premium_entitlement_source","none");if(registered)load(key);else runOnUiThread(()->renderSetup(null));}catch(Exception e){String m=e.getMessage()==null?"Unable to check FFScouter registration.":e.getMessage();runOnUiThread(()->renderSetup(m));}}).start();
    }

    private void renderSetup(String error){
        ScrollView s=shell();LinearLayout r=root(s);header(r,"Your key is not yet registered with FFScouter");
        addCard(r,card("Your key, your entitlement","TornFCA uses the same player-owned Torn API key already encrypted on this device. No leader/shared FFScouter key is used.",BLUE));
        addCard(r,card("One-time FFScouter registration","FFScouter requires each key to be registered before its stats API can be queried. Registration contacts Torn to validate the key and occurs only after the separate confirmation below.",GOLD));
        Button terms=button("Read FFScouter Terms / Data Policy",BLUE);terms.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(FFScouterClient.HOMEPAGE))));r.addView(terms,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48)));
        CheckBox agree=new CheckBox(this);agree.setText("I have read the FFScouter terms and data policy and agree to register my current key.");agree.setTextColor(TEXT);agree.setButtonTintList(android.content.res.ColorStateList.valueOf(GOLD));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);ap.topMargin=dp(12);r.addView(agree,ap);
        Button register=button("Register My Key with FFScouter",GOLD);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50));rp.topMargin=dp(10);r.addView(register,rp);
        Button retry=button("Check Registration Again",BORDER);LinearLayout.LayoutParams ryp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));ryp.topMargin=dp(8);r.addView(retry,ryp);
        TextView status=text(error==null?"A preset Limited Access Torn key is the recommended one-key choice for TornFCA, FFScouter and TornStats. Full Access is not required.":error,12,error==null?MUTED:RED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(10);r.addView(status,sp);
        retry.setOnClickListener(v->checkRegistration());register.setOnClickListener(v->{if(!agree.isChecked()){status.setText("Please read and accept the FFScouter terms/data policy first.");status.setTextColor(RED);return;}String key=tornStore.load();if(key==null){status.setText("Your Torn key is no longer available.");status.setTextColor(RED);return;}register.setEnabled(false);register.setText("Registering…");new Thread(()->{try{FFScouterClient.registerKey(key);JSONObject check=FFScouterClient.checkKey(key);ffPremium=check.optBoolean("is_premium",false);entitlement=check.optString("premium_entitlement_source","none");runOnUiThread(()->{showLoading("Registration complete. Loading estimates…");load(key);});}catch(Exception e){String m=e.getMessage()==null?"FFScouter registration failed.":e.getMessage();runOnUiThread(()->{register.setEnabled(true);register.setText("Register My Key with FFScouter");status.setText(m);status.setTextColor(RED);});}}).start();});setContentView(s);s.requestApplyInsets();
    }

    private void showLoading(String message){ScrollView s=shell();LinearLayout r=root(s);header(r,message);addCard(r,card("FFScouter provider","Using your own opted-in registered key. Battle-stat values remain estimates and retain their FFScouter source/freshness context.",BLUE));setContentView(s);s.requestApplyInsets();}

    private void load(String ffKey){
        String tornKey=tornStore.load();if(tornKey==null){renderError("Reconnect your Torn API key to load the faction roster.");return;}if(!FFScouterClient.hasConsent(this,tornKey)){renderProviderConsent();return;}
        new Thread(()->{try{JSONArray roster=FactionMemberCache.load(factionId);if(roster==null){String path=factionId>0?"/faction/"+factionId+"/members":"/faction/members";roster=TornApiClient.getJson(path,tornKey).optJSONArray("members");if(roster==null)roster=new JSONArray();if(factionId>0)FactionMemberCache.save(factionId,roster);}Map<Integer,String> names=new HashMap<>();List<Integer> ids=new ArrayList<>();for(int i=0;i<roster.length();i++){JSONObject m=roster.optJSONObject(i);if(m==null)continue;int id=m.optInt("id",0);if(id>0){ids.add(id);names.put(id,m.optString("name","Unknown"));}}JSONArray stats=FFScouterClient.getStats(ffKey,ids);List<Row> rows=new ArrayList<>();long total=0;int known=0;long newest=0;for(int i=0;i<stats.length();i++){JSONObject o=stats.optJSONObject(i);if(o==null)continue;int id=o.optInt("player_id",0);long estimate=o.isNull("bs_estimate")?0:o.optLong("bs_estimate",0);String human=o.optString("bs_estimate_human",estimate>0?human(estimate):"Unknown");double ff=o.isNull("fair_fight")?-1:o.optDouble("fair_fight",-1);long updated=o.isNull("last_updated")?0:o.optLong("last_updated",0);String source=o.optString("source","bss");if(estimate>0){known++;total+=estimate;}if(updated>newest)newest=updated;rows.add(new Row(id,names.getOrDefault(id,"Player "+id),estimate,human,ff,updated,source));}for(int id:ids){boolean found=false;for(Row row:rows)if(row.id==id){found=true;break;}if(!found)rows.add(new Row(id,names.getOrDefault(id,"Player "+id),0,"Unknown",-1,0,"bss"));}Collections.sort(rows,(a,b)->Long.compare(b.estimate,a.estimate));final long fTotal=total,fNewest=newest;final int fKnown=known,fCount=ids.size();runOnUiThread(()->render(rows,fTotal,fKnown,fCount,fNewest));}catch(Exception e){String m=e.getMessage()==null?"Unable to load FFScouter estimates.":e.getMessage();runOnUiThread(()->renderError(m));}}).start();
    }

    private void render(List<Row> rows,long total,int known,int count,long newest){
        ScrollView s=shell();LinearLayout r=root(s);header(r,factionName+(scoutingTarget?" • opposing faction":" • player-owned FFScouter connection"));String freshness=newest>0?DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(newest*1000L)):"Unknown";String access=ffPremium?"Premium ("+entitlement.replace('_',' ')+")":"Standard";
        addCard(r,card(scoutingTarget?"Opponent estimate coverage":"Faction estimate coverage",known+" / "+count+" members have an estimate\nKnown estimated total: "+human(total)+"\nNewest source update: "+freshness+"\nYour FFScouter access: "+access,GOLD));addCard(r,card("Estimate safety","These are scouting estimates, not exact Torn battle stats. Source and freshness are shown on every member. Unknown estimates stay unknown instead of being guessed.",BLUE));
        Button refresh=button("Refresh FFScouter",BLUE);refresh.setOnClickListener(v->{showLoading("Refreshing FFScouter estimates…");load(tornStore.load());});r.addView(refresh,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46)));
        Button account=button("Open FFScouter",BORDER);LinearLayout.LayoutParams kp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));kp.topMargin=dp(8);r.addView(account,kp);account.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(FFScouterClient.HOMEPAGE))));
        Button disable=button("Disable FFScouter in TornFCA",BORDER);LinearLayout.LayoutParams dip=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));dip.topMargin=dp(8);dip.bottomMargin=dp(14);r.addView(disable,dip);disable.setOnClickListener(v->{String key=tornStore.load();FFScouterClient.setConsent(this,key,false);ffPremium=false;entitlement="none";renderProviderConsent();});
        TextView label=text("MEMBER ESTIMATES",11,MUTED,true);label.setLetterSpacing(.11f);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.bottomMargin=dp(8);r.addView(label,lp);
        for(Row row:rows){String body=row.estimate>0?"Estimated battle stats: "+row.human+(row.ff>=0?"\nFair Fight: "+String.format(Locale.US,"%.2f",row.ff):"")+"\nSource: "+sourceLabel(row.source)+(row.updated>0?" • Updated "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(row.updated*1000L)):" • Freshness unknown"):"No current FFScouter battle-stat estimate.\nSource: FFScouter • confidence unknown";addCard(r,card(row.name,body,row.estimate>0?GREEN:BORDER));}
        TextView footer=text("Provider: FFScouter • TornFCA v0.9.13",11,MUTED,false);footer.setGravity(Gravity.CENTER);r.addView(footer);setContentView(s);s.requestApplyInsets();
    }
    private void renderError(String message){ScrollView s=shell();LinearLayout r=root(s);header(r,"FFScouter provider unavailable");addCard(r,card("Could not load strength intel",message,RED));Button retry=button("Check FFScouter Connection",GOLD);retry.setOnClickListener(v->{String key=tornStore.load();if(key!=null&&FFScouterClient.hasConsent(this,key))checkRegistration();else renderProviderConsent();});r.addView(retry,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48)));setContentView(s);s.requestApplyInsets();}
    private static String sourceLabel(String source){if("premium".equalsIgnoreCase(source))return"FFScouter premium spy";if("spies".equalsIgnoreCase(source))return"Faction spy via FFScouter";return"FFScouter public BSS";}
    private static String human(long v){if(v>=1_000_000_000_000L)return String.format(Locale.US,"%.2ft",v/1_000_000_000_000d);if(v>=1_000_000_000L)return String.format(Locale.US,"%.2fb",v/1_000_000_000d);if(v>=1_000_000L)return String.format(Locale.US,"%.2fm",v/1_000_000d);if(v>=1_000L)return String.format(Locale.US,"%.1fk",v/1_000d);return Long.toString(v);}
    private static final class Row{final int id;final String name;final long estimate;final String human;final double ff;final long updated;final String source;Row(int id,String name,long estimate,String human,double ff,long updated,String source){this.id=id;this.name=name;this.estimate=estimate;this.human=human;this.ff=ff;this.updated=updated;this.source=source;}}
}
