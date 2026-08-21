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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Leadership exception engine: answer "who needs attention and why?" from live faction data. */
public class LeadershipAttentionActivity extends Activity {
    private static final long INACTIVE_SECONDS = 72L * 60L * 60L;
    private static final int BG=Color.rgb(6,9,13), PANEL=Color.rgb(15,20,28), PANEL2=Color.rgb(10,15,22), BORDER=Color.rgb(45,55,69), TEXT=Color.rgb(244,246,249), MUTED=Color.rgb(154,164,178), GOLD=Color.rgb(241,194,106), BLUE=Color.rgb(88,166,255), GOOD=Color.rgb(63,185,80), BAD=Color.rgb(248,81,73);
    private SecureApiKeyStore keyStore;

    @Override protected void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);keyStore=new SecureApiKeyStore(this);showLoading();load();}

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private GradientDrawable gradient(int start,int end,int stroke,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{start,end});d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(TEXT);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL2,BORDER,12));return b;}
    private LinearLayout card(String title,String body,int stroke){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(17),dp(15),dp(17),dp(15));c.setBackground(rounded(PANEL,stroke,18));c.addView(text(title,18,TEXT,true));if(body!=null&&!body.isEmpty()){TextView b=text(body,13,MUTED,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(6);c.addView(b,p);}return c;}
    private void addCard(LinearLayout root,LinearLayout c){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(10);root.addView(c,p);}
    private void section(LinearLayout root,String label){TextView t=text(label,11,MUTED,true);t.setLetterSpacing(.11f);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(5);p.bottomMargin=dp(8);root.addView(t,p);}
    @SuppressWarnings("deprecation") private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);int l=dp(16),t=dp(18),r=dp(16),b=dp(30);s.setPadding(l,t,r,b);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),b+i.getSystemWindowInsetBottom());return i;});return s;}
    private LinearLayout root(ScrollView s){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));return r;}

    private void addHeader(LinearLayout r,String subtitle){Button back=button("← Companion");back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(dp(132),dp(44)));LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER_HORIZONTAL);hero.setPadding(dp(18),dp(19),dp(18),dp(19));hero.setBackground(gradient(Color.rgb(45,38,23),Color.rgb(14,18,24),GOLD,22));TextView brand=text("TORNFCA • LEADERSHIP",10,GOLD,true);brand.setLetterSpacing(.13f);brand.setGravity(Gravity.CENTER);hero.addView(brand);TextView title=text("Leadership Attention",29,TEXT,true);title.setGravity(Gravity.CENTER);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp(6);hero.addView(title,tp);TextView sub=text(subtitle,13,MUTED,false);sub.setGravity(Gravity.CENTER);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(6);hero.addView(sub,sp);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);hp.topMargin=dp(14);hp.bottomMargin=dp(16);r.addView(hero,hp);}

    private void showLoading(){ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Building live exception list…");section(r,"COMMAND SUMMARY");addCard(r,card("Who needs attention and why?","Checking current member status, OC assignment, recent activity and live-war participation when available.",BLUE));setContentView(s);s.requestApplyInsets();}

    private void load(){String key=keyStore.load();if(key==null||key.trim().isEmpty()){renderError("Reconnect your Torn API key first.");return;}new Thread(()->{try{
        AuthSession session=TornApiClient.cachedSession(key);if(session==null)session=TornApiClient.authenticate(key);
        if(DeveloperPreviewStore.isMemberPreview(this)){renderError("Leadership Attention is hidden in Member Preview, matching normal member access.");return;}
        boolean leadership=AppRoles.isOwner(session)||AccessPolicy.isLeaderPosition(session.position)||session.factionApiAccess||session.hasPermission("Faction API Access");
        if(!leadership){renderError("Leadership Attention is restricted to authorized faction positions/permissions.");return;}
        JSONArray members=TornApiClient.getJson("/faction/members",key).optJSONArray("members");if(members==null)members=new JSONArray();FactionMemberCache.save(session.factionId,members);
        long now=System.currentTimeMillis()/1000L;
        WarStatus war=WarStatus.from(TornApiClient.getJson("/faction/wars",key),session.factionId);boolean liveWar=war.isLive(now);
        Map<Integer,Integer> warHits=new HashMap<>();boolean warHitsAvailable=false;
        if(liveWar){try{JSONArray attacks=TornApiClient.getPagedArray("/faction/attacks?filters=outgoing&from="+war.start+"&to="+now+"&sort=DESC&limit=100",key,"attacks",20);for(int i=0;i<attacks.length();i++){JSONObject a=attacks.optJSONObject(i);if(a==null||!a.optBoolean("is_ranked_war",false))continue;JSONObject attacker=a.optJSONObject("attacker");if(attacker==null)continue;int id=attacker.optInt("id",0);if(id>0)warHits.put(id,warHits.getOrDefault(id,0)+1);}warHitsAvailable=true;}catch(Exception ignored){warHitsAvailable=false;}}

        Map<Integer,ExceptionRow> rows=new LinkedHashMap<>();int noOc=0,inactive=0,unavailable=0,zeroHits=0;
        for(int i=0;i<members.length();i++){JSONObject m=members.optJSONObject(i);if(m==null)continue;int id=m.optInt("id",0);String name=m.optString("name","Unknown");if(id<=0)continue;ExceptionRow row=new ExceptionRow(id,name);
            if(!m.optBoolean("is_in_oc",false)){row.reasons.add("No OC assignment");noOc++;}
            JSONObject last=m.optJSONObject("last_action");long ts=last==null?0:last.optLong("timestamp",0);if(ts>0&&now-ts>=INACTIVE_SECONDS){row.reasons.add("Inactive 3d+");inactive++;}
            JSONObject status=m.optJSONObject("status");String state=status==null?"":status.optString("state","");String lower=state.toLowerCase(Locale.US);if(lower.contains("hospital")||lower.contains("jail")||lower.contains("travel")||lower.contains("abroad")){row.reasons.add("Unavailable: "+state);unavailable++;if(liveWar)row.high=true;}
            if(liveWar&&warHitsAvailable&&warHits.getOrDefault(id,0)==0){row.reasons.add("0 ranked-war hits recorded");zeroHits++;row.high=true;}
            if(!row.reasons.isEmpty())rows.put(id,row);
        }
        List<ExceptionRow> list=new ArrayList<>(rows.values());Collections.sort(list,(a,b)->{int c=Boolean.compare(b.high,a.high);if(c!=0)return c;c=Integer.compare(b.reasons.size(),a.reasons.size());return c!=0?c:a.name.compareToIgnoreCase(b.name);});
        final int fNoOc=noOc,fInactive=inactive,fUnavailable=unavailable,fZeroHits=zeroHits;final boolean fLiveWar=liveWar,fWarHitsAvailable=warHitsAvailable;final String faction=session.factionName;runOnUiThread(()->render(faction,list,fNoOc,fInactive,fUnavailable,fZeroHits,fLiveWar,fWarHitsAvailable));
    }catch(Exception e){renderError(e.getMessage()==null?"Unable to build leadership exceptions.":e.getMessage());}}).start();}

    private void render(String faction,List<ExceptionRow> rows,int noOc,int inactive,int unavailable,int zeroHits,boolean liveWar,boolean warHitsAvailable){ScrollView s=shell();LinearLayout r=root(s);addHeader(r,faction+" • live exception dashboard");section(r,"ATTENTION SNAPSHOT");String summary=rows.size()+" members need attention\n"+noOc+" no OC • "+inactive+" inactive 3d+ • "+unavailable+" unavailable"+(liveWar&&warHitsAvailable?" • "+zeroHits+" with 0 war hits":"");addCard(r,card("Attention summary",summary,rows.isEmpty()?GOOD:GOLD));String note="TornFCA v"+TornFcaBrand.VERSION+" rules • inactivity threshold: 72 hours";if(liveWar&&!warHitsAvailable)note+="\nLive war detected, but detailed faction attacks were not available to this key.";addCard(r,card("Rule freshness",note,BLUE));section(r,"MEMBERS NEEDING ATTENTION");if(rows.isEmpty())addCard(r,card("All clear","No member matched the current exception rules.",GOOD));else for(ExceptionRow row:rows){StringBuilder body=new StringBuilder();for(int i=0;i<row.reasons.size();i++){if(i>0)body.append("\n");body.append("• ").append(row.reasons.get(i));}addCard(r,card(row.name,body.toString(),row.high?BAD:BORDER));}setContentView(s);s.requestApplyInsets();}
    private void renderError(String message){runOnUiThread(()->{ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Unable to build attention list");section(r,"ACCESS / DATA");addCard(r,card("Attention dashboard unavailable",message,BAD));setContentView(s);s.requestApplyInsets();});}

    private static final class ExceptionRow{final int playerId;final String name;final List<String> reasons=new ArrayList<>();boolean high;ExceptionRow(int playerId,String name){this.playerId=playerId;this.name=name;}}
}
