package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Leadership member dossier: Torn snapshot + batched FFScouter + opt-in TornStats intelligence. */
public class MemberDossierActivity extends Activity {
    private static final int BG=Color.rgb(5,8,12),PANEL=Color.rgb(12,18,26),PANEL2=Color.rgb(8,13,20),BORDER=Color.rgb(36,47,61),TEXT=Color.rgb(246,248,251),MUTED=Color.rgb(145,155,169),GOLD=Color.rgb(241,190,86),BLUE=Color.rgb(82,153,235),GREEN=Color.rgb(76,190,102),RED=Color.rgb(239,88,82);
    private SecureApiKeyStore keyStore;
    private int factionId;
    private String factionName="Faction";
    private JSONArray members=new JSONArray();
    private final Map<Integer,JSONObject> ffById=new HashMap<>(),tsRosterById=new HashMap<>(),tsSpyById=new HashMap<>();
    private String ffStatus="FFScouter not loaded",tsStatus="TornStats not connected";
    private JSONObject selectedMember;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);keyStore=new SecureApiKeyStore(this);
        factionId=getIntent().getIntExtra(FactionOpsActivity.EXTRA_FACTION_ID,0);String fn=getIntent().getStringExtra(FactionOpsActivity.EXTRA_FACTION_NAME);if(fn!=null&&!fn.isBlank())factionName=fn;
        JSONArray cached=FactionMemberCache.load(factionId);if(cached!=null){members=cached;showLoading("Loading faction battle intelligence…");loadProviders();}else{showLoading("Loading faction roster…");loadRoster();}
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private GradientDrawable gradient(int a,int b,int stroke,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private TextView eyebrow(String value,int color){TextView t=text(value,9.5f,color,true);t.setLetterSpacing(.11f);return t;}
    private Button button(String value,int accent){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextColor(TEXT);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL2,accent,12));return b;}
    private LinearLayout card(int accent){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(16),dp(18),dp(16));c.setBackground(gradient(PANEL,PANEL2,accent==Color.TRANSPARENT?BORDER:accent,19));return c;}
    private void add(LinearLayout r,LinearLayout c){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(11);r.addView(c,p);}
    @SuppressWarnings("deprecation") private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setClipToPadding(false);s.setBackgroundColor(BG);int l=dp(18),t=dp(14),r=dp(18),bt=dp(28);s.setPadding(l,t,r,bt);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),bt+i.getSystemWindowInsetBottom());return i;});return s;}
    private LinearLayout root(ScrollView s){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));return r;}
    private void header(LinearLayout r){Button back=button("← Leadership",BORDER);back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(dp(145),dp(44)));TextView e=eyebrow("LEADERSHIP • MEMBER INTELLIGENCE",BLUE);LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);ep.topMargin=dp(18);r.addView(e,ep);r.addView(text("Member Dossier",30,TEXT,true));TextView sub=text(factionName+" • current faction status + battle intelligence",13,MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(5);sp.bottomMargin=dp(16);r.addView(sub,sp);}

    private void showLoading(String msg){ScrollView s=shell();LinearLayout r=root(s);header(r);LinearLayout c=card(BORDER);c.addView(eyebrow("LOADING",GOLD));c.addView(text(msg,18,TEXT,true));add(r,c);setContentView(s);s.requestApplyInsets();}

    private void loadRoster(){String key=keyStore.load();if(key==null){renderError("Reconnect your Torn key first.");return;}new Thread(()->{try{JSONArray data=TornApiClient.getJson("/faction/members",key).optJSONArray("members");members=data==null?new JSONArray():data;FactionMemberCache.save(factionId,members);loadProvidersInternal(key);runOnUiThread(this::renderSearch);}catch(Exception e){renderError(e.getMessage()==null?"Unable to load faction members.":e.getMessage());}}).start();}
    private void loadProviders(){String key=keyStore.load();if(key==null){renderError("Reconnect your Torn key first.");return;}new Thread(()->{loadProvidersInternal(key);runOnUiThread(this::renderSearch);}).start();}
    private void loadProvidersInternal(String key){loadFF(key);if(TornStatsClient.hasConsent(this))loadTornStatsRoster(key);}

    private void loadFF(String key){
        ffById.clear();List<Integer> ids=new ArrayList<>();for(int i=0;i<members.length();i++){JSONObject m=members.optJSONObject(i);if(m!=null&&m.optInt("id",0)>0)ids.add(m.optInt("id",0));}
        try{JSONArray rows=FFScouterClient.getStats(key,ids);for(int i=0;i<rows.length();i++){JSONObject o=rows.optJSONObject(i);if(o==null)continue;int id=o.optInt("player_id",o.optInt("id",0));if(id>0)ffById.put(id,o);}ffStatus=ffById.size()+" FFScouter estimates loaded in one batch";}catch(Exception e){ffStatus=e.getMessage()==null?"FFScouter unavailable":e.getMessage();}
    }

    private void loadTornStatsRoster(String key){
        tsRosterById.clear();try{JSONObject root=TornStatsClient.factionRoster(key);JSONObject roster=root.optJSONObject("members");if(roster!=null){for(String id:roster.keySet()){JSONObject o=roster.optJSONObject(id);if(o!=null)try{tsRosterById.put(Integer.parseInt(id),o);}catch(Exception ignored){}}}tsStatus=tsRosterById.size()+" TornStats roster records loaded";}catch(Exception e){tsStatus=e.getMessage()==null?"TornStats unavailable":e.getMessage();}
    }

    private void renderSearch(){
        ScrollView s=shell();LinearLayout r=root(s);header(r);
        LinearLayout providers=card(BLUE);providers.addView(eyebrow("INTELLIGENCE SOURCES",BLUE));providers.addView(text(ffStatus,12.5f,MUTED,false));providers.addView(text(TornStatsClient.hasConsent(this)?tsStatus:"TornStats disabled — explicit opt-in required",12.5f,TornStatsClient.hasConsent(this)?MUTED:GOLD,false));
        if(!TornStatsClient.hasConsent(this)){Button connect=button("Enable TornStats Provider",GOLD);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));cp.topMargin=dp(10);providers.addView(connect,cp);connect.setOnClickListener(v->showTornStatsConsent());}else{Button disconnect=button("Disconnect TornStats",BORDER);LinearLayout.LayoutParams dpv=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(42));dpv.topMargin=dp(8);providers.addView(disconnect,dpv);disconnect.setOnClickListener(v->{TornStatsClient.setConsent(this,false);tsRosterById.clear();tsSpyById.clear();tsStatus="TornStats not connected";renderSearch();});}
        add(r,providers);

        EditText search=new EditText(this);search.setHint("Search faction member name or Torn ID");search.setHintTextColor(MUTED);search.setTextColor(TEXT);search.setSingleLine(true);search.setInputType(InputType.TYPE_CLASS_TEXT);search.setPadding(dp(14),0,dp(14),0);search.setBackground(rounded(PANEL2,BLUE,12));r.addView(search,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(52)));Button go=button("Open Member Dossier",BLUE);LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48));gp.topMargin=dp(8);gp.bottomMargin=dp(12);r.addView(go,gp);go.setOnClickListener(v->findAndOpen(search.getText().toString()));
        LinearLayout guide=card(BORDER);guide.addView(text("What this dossier tells leadership",17,TEXT,true));guide.addView(text("Current Torn status, position, last action, OC/wall state, FFScouter battle-stat estimate and Fair Fight, plus TornStats shared/verified totals when the provider is enabled. Detailed TornStats spy data is loaded only for the member you choose.",12.5f,MUTED,false));add(r,guide);
        setContentView(s);s.requestApplyInsets();
    }

    private void showTornStatsConsent(){
        ScrollView s=shell();LinearLayout r=root(s);header(r);LinearLayout c=card(GOLD);c.addView(eyebrow("THIRD-PARTY PROVIDER CONSENT",GOLD));c.addView(text("Connect TornStats?",21,TEXT,true));c.addView(text("If enabled, TornFCA will send your currently saved Torn API key directly to TornStats only when requesting TornStats data. TornStats is a separate service with its own data handling. TornFCA does not send the key to our backend for this integration. You can disconnect TornStats at any time.",13,MUTED,false));Button yes=button("I Agree — Enable TornStats",GREEN);LinearLayout.LayoutParams yp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48));yp.topMargin=dp(13);c.addView(yes,yp);Button no=button("Cancel",BORDER);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));np.topMargin=dp(8);c.addView(no,np);yes.setOnClickListener(v->{TornStatsClient.setConsent(this,true);showLoading("Loading TornStats faction roster…");String key=keyStore.load();new Thread(()->{loadTornStatsRoster(key);runOnUiThread(this::renderSearch);}).start();});no.setOnClickListener(v->renderSearch());add(r,c);setContentView(s);s.requestApplyInsets();
    }

    private void findAndOpen(String raw){String q=raw==null?"":raw.trim();if(q.isEmpty()){Toast.makeText(this,"Enter a member name or Torn ID.",Toast.LENGTH_SHORT).show();return;}JSONObject exact=null,partial=null;for(int i=0;i<members.length();i++){JSONObject m=members.optJSONObject(i);if(m==null)continue;String name=m.optString("name","");int id=m.optInt("id",0);if(q.equals(String.valueOf(id))||name.equalsIgnoreCase(q)){exact=m;break;}if(partial==null&&name.toLowerCase(Locale.US).contains(q.toLowerCase(Locale.US)))partial=m;}selectedMember=exact!=null?exact:partial;if(selectedMember==null){Toast.makeText(this,"No current faction member matched that search.",Toast.LENGTH_SHORT).show();return;}renderDossier();}

    private void renderDossier(){
        if(selectedMember==null){renderSearch();return;}int id=selectedMember.optInt("id",0);String name=selectedMember.optString("name","Member");
        ScrollView s=shell();LinearLayout r=root(s);header(r);Button another=button("← Search Another Member",BORDER);another.setOnClickListener(v->{selectedMember=null;renderSearch();});LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));ap.bottomMargin=dp(11);r.addView(another,ap);

        JSONObject last=selectedMember.optJSONObject("last_action"),status=selectedMember.optJSONObject("status");String state=status==null?"Unknown":status.optString("state","Unknown");String detail=status==null?"":status.optString("description",status.optString("details",""));String lastState=last==null?"Unknown":last.optString("status","Unknown");String relative=last==null?"":last.optString("relative","");
        LinearLayout identity=card("Online".equalsIgnoreCase(lastState)?GREEN:BORDER);identity.addView(eyebrow("TORN PROFILE • "+id,"Online".equalsIgnoreCase(lastState)?GREEN:BLUE));identity.addView(text(name,25,TEXT,true));identity.addView(text(selectedMember.optString("position","Member")+" • "+state+(detail.isBlank()?"":" — "+detail),13,MUTED,false));identity.addView(text("Last action: "+lastState+(relative.isBlank()?"":" • "+relative)+"\nOC: "+(selectedMember.optBoolean("is_in_oc",false)?"Assigned":"Not assigned")+" • Territory wall: "+(selectedMember.optBoolean("is_on_wall",false)?"Yes":"No"),12.5f,MUTED,false));add(r,identity);

        JSONObject tsSpy=tsSpyById.get(id),tsRoster=tsRosterById.get(id),ff=ffById.get(id);LinearLayout battle=card(GOLD);battle.addView(eyebrow("BATTLE INTELLIGENCE",GOLD));
        JSONObject spy=extractSpy(tsSpy);long exactTotal=spy==null?0L:spy.optLong("total",0L);long sharedTotal=tsRoster==null?0L:tsRoster.optLong("total",0L);long estimate=ff==null?0L:Math.round(ff.optDouble("bs_estimate",ff.optDouble("battle_stats",0d)));
        if(exactTotal>0){battle.addView(text("TornStats shared/spy total: "+number(exactTotal),19,GREEN,true));battle.addView(text(spy.optString("type","TornStats")+freshness(spy.optString("difference","")),12,MUTED,false));}
        else if(sharedTotal>0){battle.addView(text("TornStats roster total: "+number(sharedTotal),19,GREEN,true));battle.addView(text("Shared/verified: "+(tsRoster.optInt("verified",0)==1?"Yes":"No"),12,MUTED,false));}
        else if(estimate>0){battle.addView(text("FFScouter estimate: "+number(estimate),19,BLUE,true));battle.addView(text("Fair Fight: "+format(ff.optDouble("fair_fight",0d))+ffFreshness(ff),12,MUTED,false));}
        else battle.addView(text("No battle-stat intelligence is currently available for this member.",14,MUTED,false));
        if(ff!=null&&estimate>0&&(exactTotal>0||sharedTotal>0))battle.addView(text("FFScouter estimate: "+number(estimate)+" • Fair Fight "+format(ff.optDouble("fair_fight",0d)),12,BLUE,false));
        add(r,battle);

        LinearLayout sources=card(BLUE);sources.addView(text("Provider detail",17,TEXT,true));sources.addView(text("FFScouter: "+(ff==null?ffStatus:"estimate available")+"\nTornStats: "+(TornStatsClient.hasConsent(this)?(tsRoster==null?"no shared roster total for this member":"roster data available"):"not connected"),12,MUTED,false));
        if(TornStatsClient.hasConsent(this)){Button detailButton=button(tsSpy==null?"Load Detailed TornStats Data":"Refresh Detailed TornStats Data",BLUE);LinearLayout.LayoutParams dbp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));dbp.topMargin=dp(10);sources.addView(detailButton,dbp);detailButton.setOnClickListener(v->loadDetailedTornStats(id));}
        add(r,sources);

        if(tsSpy!=null){JSONObject detailed=extractSpy(tsSpy);if(detailed!=null&&detailed.optBoolean("status",true)){LinearLayout d=card(GREEN);d.addView(eyebrow("TORNSTATS DETAIL",GREEN));d.addView(text("Strength "+number(detailed.optLong("strength",0))+"\nDefense "+number(detailed.optLong("defense",0))+"\nSpeed "+number(detailed.optLong("speed",0))+"\nDexterity "+number(detailed.optLong("dexterity",0))+"\nTotal "+number(detailed.optLong("total",0)),13,TEXT,false));if(detailed.has("fair_fight_bonus"))d.addView(text("Fair Fight bonus: "+format(detailed.optDouble("fair_fight_bonus",0d))+freshness(detailed.optString("difference","")),12,MUTED,false));add(r,d);}}
        TextView foot=text("Exact/shared provider values are labeled separately from FFScouter estimates. TornFCA does not silently combine an estimate into an exact stat value.",10.5f,MUTED,false);foot.setGravity(Gravity.CENTER);r.addView(foot);setContentView(s);s.requestApplyInsets();
    }

    private void loadDetailedTornStats(int id){String key=keyStore.load();if(key==null)return;showLoading("Loading TornStats detail for the selected member…");new Thread(()->{try{JSONObject spy=TornStatsClient.userSpy(key,id);tsSpyById.put(id,spy);runOnUiThread(this::renderDossier);}catch(Exception e){String m=e.getMessage()==null?"TornStats detail unavailable.":e.getMessage();runOnUiThread(()->{Toast.makeText(this,m,Toast.LENGTH_LONG).show();renderDossier();});}}).start();}

    private JSONObject extractSpy(JSONObject root){if(root==null)return null;JSONObject compare=root.optJSONObject("compare");if(compare!=null){JSONObject spy=compare.optJSONObject("spy");if(spy!=null)return spy;}JSONObject spy=root.optJSONObject("spy");return spy;}
    private static String freshness(String value){return value==null||value.isBlank()?"":" • "+value;}
    private static String ffFreshness(JSONObject ff){if(ff==null)return"";Object raw=ff.opt("last_updated");if(raw instanceof JSONObject){JSONObject o=(JSONObject)raw;String rel=o.optString("relative",o.optString("human",""));if(!rel.isBlank())return" • "+rel;}String rel=ff.optString("last_updated_human","");return rel.isBlank()?"":" • "+rel;}
    private static String format(double v){return String.format(Locale.US,"%.2f",v);}
    private static String number(long v){return NumberFormat.getIntegerInstance(Locale.US).format(v);}
    private void renderError(String message){runOnUiThread(()->{ScrollView s=shell();LinearLayout r=root(s);header(r);LinearLayout c=card(RED);c.addView(eyebrow("DOSSIER UNAVAILABLE",RED));c.addView(text(message,13,TEXT,false));Button retry=button("Retry",GOLD);retry.setOnClickListener(v->loadRoster());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48));p.topMargin=dp(10);c.addView(retry,p);add(r,c);setContentView(s);s.requestApplyInsets();});}
}
