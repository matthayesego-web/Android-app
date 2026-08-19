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

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Member-safe faction views. These screens use only the signed-in player's data plus public faction status. */
public class MemberFactionActivity extends Activity {
    public static final String EXTRA_MODE = "mode";
    public static final String MODE_OVERVIEW = "OVERVIEW";
    public static final String MODE_OC = "OC";
    public static final String MODE_PARTICIPATION = "PARTICIPATION";

    private static final long WAR_CACHE_MS=2L*60L*1000L;
    private static final int BG=Color.rgb(6,9,13), PANEL=Color.rgb(15,20,28), PANEL2=Color.rgb(10,15,22), BORDER=Color.rgb(45,55,69);
    private static final int TEXT=Color.rgb(244,246,249), MUTED=Color.rgb(154,164,178), GOLD=Color.rgb(241,194,106), BLUE=Color.rgb(88,166,255), GOOD=Color.rgb(63,185,80), BAD=Color.rgb(248,81,73);

    private SecureApiKeyStore keyStore;
    private String mode;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        keyStore = new SecureApiKeyStore(this);
        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (mode == null) mode = MODE_OVERVIEW;
        showLoading();
        load();
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private GradientDrawable gradient(int start,int end,int stroke,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{start,end});d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(TEXT);b.setTextSize(13);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL2,BORDER,12));return b;}

    private LinearLayout card(String title,String body,int stroke){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(17),dp(15),dp(17),dp(15));c.setBackground(rounded(PANEL,stroke,18));
        TextView h=text(title,18,TEXT,true);c.addView(h);
        if(body!=null&&!body.isEmpty()){TextView b=text(body,13.5f,MUTED,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(6);c.addView(b,p);}
        return c;
    }

    private void addCard(LinearLayout root,LinearLayout c){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(10);root.addView(c,p);}
    private void section(LinearLayout root,String label){TextView t=text(label,11,MUTED,true);t.setLetterSpacing(.11f);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(5);p.bottomMargin=dp(8);root.addView(t,p);}

    @SuppressWarnings("deprecation") private ScrollView shell(){
        ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);
        int l=dp(16),t=dp(18),r=dp(16),b=dp(30);s.setPadding(l,t,r,b);
        s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),b+i.getSystemWindowInsetBottom());return i;});
        return s;
    }

    private LinearLayout root(ScrollView s){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));return r;}

    private String title(){if(MODE_OC.equals(mode))return "My OC";if(MODE_PARTICIPATION.equals(mode))return "My Participation";return "My Faction Status";}

    private void addHeader(LinearLayout r,String subtitle){
        Button back=button("← Companion");back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(dp(132),dp(44)));

        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER_HORIZONTAL);hero.setPadding(dp(18),dp(20),dp(18),dp(20));hero.setBackground(gradient(Color.rgb(29,43,61),Color.rgb(13,19,27),BORDER,22));
        TextView brand=text("TORNFCA • MEMBER VIEW",10,GOLD,true);brand.setLetterSpacing(.13f);brand.setGravity(Gravity.CENTER);hero.addView(brand);
        TextView h=text(title(),30,TEXT,true);h.setGravity(Gravity.CENTER);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);hp.topMargin=dp(6);hero.addView(h,hp);
        TextView sub=text(subtitle,13,MUTED,false);sub.setGravity(Gravity.CENTER);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(6);hero.addView(sub,sp);
        LinearLayout.LayoutParams heroParams=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);heroParams.topMargin=dp(14);heroParams.bottomMargin=dp(16);r.addView(hero,heroParams);
    }

    private void addFooter(LinearLayout r){TextView f=text("TornFCA v"+TornFcaBrand.VERSION+" • member-safe data scope",11,MUTED,false);f.setGravity(Gravity.CENTER);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(8);p.bottomMargin=dp(4);r.addView(f,p);}

    private void showLoading(){ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Loading only your signed-in faction data…");section(r,"SECURE MEMBER SCOPE");addCard(r,card("Loading your status","Faction-wide leadership records are not exposed on this screen.",BLUE));addFooter(r);setContentView(s);s.requestApplyInsets();}

    private void load(){
        String key=keyStore.load();
        if(key==null||key.trim().isEmpty()){renderError("Reconnect your Torn API key to continue.");return;}
        new Thread(()->{
            try{
                AuthSession session=TornApiClient.cachedSession(key);if(session==null)session=TornApiClient.authenticate(key);
                if(MODE_OC.equals(mode))loadOc(key,session);
                else if(MODE_PARTICIPATION.equals(mode))loadParticipation(key,session);
                else loadOverview(key,session);
            }catch(Exception e){renderError(e.getMessage()==null?"Unable to load your faction status.":e.getMessage());}
        },"TornFCA-MemberFaction").start();
    }

    private JSONObject loadWarRoot(String key,int factionId)throws Exception{
        JSONObject warm=StartupWarmCache.war(factionId,WAR_CACHE_MS);
        if(warm!=null)return warm;
        JSONObject live=TornApiClient.getJson("/faction/wars",key);
        StartupWarmCache.putWar(factionId,live);
        return live;
    }

    private void loadOverview(String key,AuthSession session) throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(4);
        try{
            Future<OcSummary> ocFuture=pool.submit(()->readOc(key,session.playerId));
            Future<JSONObject> warFuture=pool.submit(()->loadWarRoot(key,session.factionId));
            Future<JSONObject> chainFuture=pool.submit(()->TornApiClient.getJson("/faction/chain",key));
            WarStatus war=WarStatus.from(warFuture.get(),session.factionId);
            Future<ParticipationSummary> participationFuture=pool.submit(()->readParticipation(key,war));
            OcSummary oc=ocFuture.get();
            JSONObject chainRoot=chainFuture.get();
            JSONObject chain=chainRoot.optJSONObject("chain");
            ParticipationSummary participation=participationFuture.get();
            runOnUiThread(()->{
                ScrollView s=shell();LinearLayout r=root(s);addHeader(r,session.factionName+" • "+session.playerName);
                long warAge=StartupWarmCache.warAgeMs(session.factionId);if(warAge>=0){section(r,"DATA STATUS");addCard(r,card("Faction status",DataFreshness.ageText(warAge),BORDER));}
                int attention=0;if(!oc.assigned)attention++;if(war.isLive(System.currentTimeMillis()/1000L)&&participation.known&&participation.hits==0)attention++;
                section(r,"TODAY");
                addCard(r,card(attention==0?"✓ You're clear right now":"! "+attention+" item"+(attention==1?"":"s")+" need attention",attention==0?"No immediate member obligations were detected from the data currently available.":"Review the highlighted cards below to see what needs action.",attention==0?GOOD:GOLD));
                section(r,"ACTION & READINESS");
                addCard(r,card("My OC",oc.summary,oc.assigned?GOOD:GOLD));
                addCard(r,card("War participation",participation.summary,participation.known?(participation.hits>0?GOOD:GOLD):BORDER));
                if(chain==null)addCard(r,card("Chain","No active chain returned by Torn.",BORDER));
                else addCard(r,card("Chain","Current: "+chain.optInt("current",0)+" / "+chain.optInt("max",0)+"\nTimeout: "+chain.optInt("timeout",0)+" sec",chain.optInt("current",0)>0?GOOD:GOLD));
                section(r,"DIGEST");
                addCard(r,card("While You Were Away","Live status is generated from current Torn data. Persistent cross-device history will be added with the shared backend rather than guessed locally.",BLUE));
                addFooter(r);setContentView(s);s.requestApplyInsets();
            });
        }finally{pool.shutdownNow();}
    }

    private void loadOc(String key,AuthSession session) throws Exception {
        OcSummary oc=readOc(key,session.playerId);
        runOnUiThread(()->{
            ScrollView s=shell();LinearLayout r=root(s);addHeader(r,session.factionName+" • signed-in member only");
            section(r,"MY ASSIGNMENT");addCard(r,card(oc.assigned?oc.name:"No current OC",oc.summary,oc.assigned?GOOD:GOLD));
            if(oc.assigned){section(r,"READINESS");if(!oc.position.isEmpty())addCard(r,card("My slot",oc.position+(oc.checkpointPassRate>=0?"\nCheckpoint pass rate: "+oc.checkpointPassRate+"%":""),BLUE));if(oc.readyAt>0)addCard(r,card("Ready time",DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(oc.readyAt*1000L)),BORDER));if(oc.itemRequired)addCard(r,card("Required item",oc.itemAvailable?"Required item is available.":"Required item is missing.",oc.itemAvailable?GOOD:BAD));}
            addFooter(r);setContentView(s);s.requestApplyInsets();
        });
    }

    private void loadParticipation(String key,AuthSession session) throws Exception {
        WarStatus war=WarStatus.from(loadWarRoot(key,session.factionId),session.factionId);
        ParticipationSummary p=readParticipation(key,war);
        runOnUiThread(()->{
            ScrollView s=shell();LinearLayout r=root(s);addHeader(r,session.factionName+" • your attacks only");
            section(r,"WAR STATUS");addCard(r,card(war.isLive(System.currentTimeMillis()/1000L)?"Current ranked war":"Recent ranked-war activity",p.summary,p.known?(p.hits>0?GOOD:GOLD):BORDER));
            if(war.present)addCard(r,card("Current war",war.headline(System.currentTimeMillis()/1000L)+"\n"+war.detail(System.currentTimeMillis()/1000L),BLUE));
            section(r,"REQUIREMENT PROGRESS");addCard(r,card("Verified participation","TornFCA reports your verified hit count. Configurable faction requirements will come from the obligation engine so the app never invents a target.",BORDER));
            addFooter(r);setContentView(s);s.requestApplyInsets();
        });
    }

    private OcSummary readOc(String key,int playerId) throws Exception {
        JSONObject root=TornApiClient.getJson("/user/organizedcrime",key);
        JSONObject crime=root.optJSONObject("organizedCrime");
        if(crime==null)return OcSummary.none("You are not currently assigned to an organized crime.");
        if(crime.has("error")&&crime.has("code"))return OcSummary.none(crime.optString("error","No current organized crime."));
        String name=crime.optString("name","Organized Crime");String status=crime.optString("status","Unknown");int difficulty=crime.optInt("difficulty",0);long readyAt=crime.optLong("ready_at",0L);
        String position="";int cpr=-1;boolean itemRequired=false,itemAvailable=true;
        JSONArray slots=crime.optJSONArray("slots");
        if(slots!=null)for(int i=0;i<slots.length();i++){JSONObject slot=slots.optJSONObject(i);if(slot==null)continue;JSONObject user=slot.optJSONObject("user");if(user==null||user.optInt("id",0)!=playerId)continue;position=slot.optString("position","");cpr=slot.optInt("checkpoint_pass_rate",-1);JSONObject req=slot.optJSONObject("item_requirement");if(req!=null){itemRequired=true;itemAvailable=req.optBoolean("is_available",true);}break;}
        String summary=status+" • Difficulty "+difficulty+(position.isEmpty()?"":"\nYour slot: "+position)+(cpr>=0?" • CPR "+cpr+"%":"");
        return new OcSummary(true,name,summary,position,cpr,readyAt,itemRequired,itemAvailable);
    }

    private ParticipationSummary readParticipation(String key,WarStatus war){
        long now=System.currentTimeMillis()/1000L;long from=war.isLive(now)?war.start:now-(30L*86400L);
        try{JSONArray attacks=TornApiClient.getPagedArray("/user/attacks?filters=outgoing&from="+from+"&to="+now+"&sort=DESC&limit=100",key,"attacks",20);int hits=0;for(int i=0;i<attacks.length();i++){JSONObject a=attacks.optJSONObject(i);if(a!=null&&a.optBoolean("is_ranked_war",false))hits++;}String window=war.isLive(now)?"since this war started":"in the last 30 days";return new ParticipationSummary(true,hits,hits+" ranked-war hit"+(hits==1?"":"s")+" "+window+".");}
        catch(Exception e){return new ParticipationSummary(false,0,"Personal attack history is unavailable with this key. A Limited/Custom/Full key is required for verified personal hit counts.");}
    }

    private void renderError(String message){runOnUiThread(()->{ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Data unavailable");section(r,"CONNECTION");addCard(r,card("Unable to load",message,BAD));Button retry=button("Retry");retry.setOnClickListener(v->{showLoading();load();});LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));rp.bottomMargin=dp(10);r.addView(retry,rp);addFooter(r);setContentView(s);s.requestApplyInsets();});}

    private static final class ParticipationSummary {final boolean known;final int hits;final String summary;ParticipationSummary(boolean known,int hits,String summary){this.known=known;this.hits=hits;this.summary=summary;}}
    private static final class OcSummary {
        final boolean assigned;final String name;final String summary;final String position;final int checkpointPassRate;final long readyAt;final boolean itemRequired;final boolean itemAvailable;
        OcSummary(boolean assigned,String name,String summary,String position,int checkpointPassRate,long readyAt,boolean itemRequired,boolean itemAvailable){this.assigned=assigned;this.name=name;this.summary=summary;this.position=position;this.checkpointPassRate=checkpointPassRate;this.readyAt=readyAt;this.itemRequired=itemRequired;this.itemAvailable=itemAvailable;}
        static OcSummary none(String summary){return new OcSummary(false,"",summary,"",-1,0L,false,true);}
    }
}
