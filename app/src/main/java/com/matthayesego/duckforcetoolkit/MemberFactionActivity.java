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
import java.util.Date;
import java.util.Locale;

/** Member-safe faction views. These screens use only the signed-in player's data plus public faction status. */
public class MemberFactionActivity extends Activity {
    public static final String EXTRA_MODE = "mode";
    public static final String MODE_OVERVIEW = "OVERVIEW";
    public static final String MODE_OC = "OC";
    public static final String MODE_PARTICIPATION = "PARTICIPATION";

    private static final int BG=Color.rgb(8,12,18), PANEL=Color.rgb(20,27,38), PANEL2=Color.rgb(27,36,49), BORDER=Color.rgb(49,63,81);
    private static final int TEXT=Color.rgb(245,248,252), MUTED=Color.rgb(151,163,179), GOLD=Color.rgb(243,184,52), BLUE=Color.rgb(88,166,255), GOOD=Color.rgb(63,185,80), BAD=Color.rgb(248,81,73);
    private static final String DUCK_FORCE_NAME = "Duck Force";

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
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(TEXT);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL2,BORDER,11));return b;}
    private LinearLayout card(String title,String body,int stroke){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(15),dp(13),dp(15),dp(13));c.setBackground(rounded(PANEL,stroke,16));c.addView(text(title,17,TEXT,true));if(body!=null&&!body.isEmpty()){TextView b=text(body,13,MUTED,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(5);c.addView(b,p);}return c;}
    private void addCard(LinearLayout root,LinearLayout c){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(9);root.addView(c,p);}
    private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);s.setPadding(dp(16),dp(16),dp(16),dp(28));return s;}
    private LinearLayout root(ScrollView s){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));return r;}

    private String title(){if(MODE_OC.equals(mode))return "My OC";if(MODE_PARTICIPATION.equals(mode))return "My Participation";return "My Faction Status";}
    private void addHeader(LinearLayout r,String subtitle){Button back=button("← Companion");back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(dp(124),dp(44)));TextView t=text(title(),27,TEXT,true);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp(14);r.addView(t,tp);TextView s=text(subtitle,13,MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(4);sp.bottomMargin=dp(14);r.addView(s,sp);}
    private void showLoading(){ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Loading only your signed-in faction data…");addCard(r,card("Member-safe view","This screen does not expose faction-wide leadership data.",BLUE));setContentView(s);}

    private void load(){
        String key=keyStore.load();
        if(key==null||key.trim().isEmpty()){renderError("Reconnect your Torn API key to continue.");return;}
        new Thread(()->{
            try{
                AuthSession session=TornApiClient.authenticate(key);
                if(session.factionName==null||!DUCK_FORCE_NAME.equalsIgnoreCase(session.factionName.trim()))throw new Exception("This build is restricted to Duck Force members.");
                if(MODE_OC.equals(mode))loadOc(key,session);
                else if(MODE_PARTICIPATION.equals(mode))loadParticipation(key,session);
                else loadOverview(key,session);
            }catch(Exception e){renderError(e.getMessage()==null?"Unable to load your faction status.":e.getMessage());}
        }).start();
    }

    private void loadOverview(String key,AuthSession session) throws Exception {
        OcSummary oc=readOc(key,session.playerId);
        WarStatus war=WarStatus.from(TornApiClient.getJson("/faction/wars",key),session.factionId);
        JSONObject chainRoot=TornApiClient.getJson("/faction/chain",key);
        JSONObject chain=chainRoot.optJSONObject("chain");
        ParticipationSummary participation=readParticipation(key,war);
        runOnUiThread(()->{
            ScrollView s=shell();LinearLayout r=root(s);addHeader(r,session.factionName+" • "+session.playerName);
            int attention=0;
            if(!oc.assigned)attention++;
            if(war.isLive(System.currentTimeMillis()/1000L)&&participation.known&&participation.hits==0)attention++;
            addCard(r,card(attention==0?"You're clear right now":attention+" item"+(attention==1?"":"s")+" need attention",
                    attention==0?"No immediate member obligations were detected from the data available to v0.8.":"Open the cards below to see what needs action.",attention==0?GOOD:GOLD));
            addCard(r,card("My OC",oc.summary,oc.assigned?GOOD:GOLD));
            addCard(r,card("War participation",participation.summary,participation.known?(participation.hits>0?GOOD:GOLD):BORDER));
            if(chain==null)addCard(r,card("Chain","No active chain returned by Torn.",BORDER));
            else addCard(r,card("Chain","Current: "+chain.optInt("current",0)+" / "+chain.optInt("max",0)+" • timeout "+chain.optInt("timeout",0)+" sec",chain.optInt("current",0)>0?GOOD:GOLD));
            addCard(r,card("While You Were Away","v0.8 starts with live/session-derived status. Durable cross-device history remains deferred to the shared backend release.",BLUE));
            setContentView(s);
        });
    }

    private void loadOc(String key,AuthSession session) throws Exception {
        OcSummary oc=readOc(key,session.playerId);
        runOnUiThread(()->{
            ScrollView s=shell();LinearLayout r=root(s);addHeader(r,session.factionName+" • signed-in member only");
            addCard(r,card(oc.assigned?oc.name:"No current OC",oc.summary,oc.assigned?GOOD:GOLD));
            if(oc.assigned){
                if(!oc.position.isEmpty())addCard(r,card("My slot",oc.position+(oc.checkpointPassRate>=0?" • CPR "+oc.checkpointPassRate+"%":""),BLUE));
                if(oc.readyAt>0)addCard(r,card("Ready time",DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(oc.readyAt*1000L)),BORDER));
                if(oc.itemRequired)addCard(r,card("Required item",oc.itemAvailable?"Required item is available.":"Required item is missing.",oc.itemAvailable?GOOD:BAD));
            }
            setContentView(s);
        });
    }

    private void loadParticipation(String key,AuthSession session) throws Exception {
        WarStatus war=WarStatus.from(TornApiClient.getJson("/faction/wars",key),session.factionId);
        ParticipationSummary p=readParticipation(key,war);
        runOnUiThread(()->{
            ScrollView s=shell();LinearLayout r=root(s);addHeader(r,session.factionName+" • your attacks only");
            addCard(r,card(war.isLive(System.currentTimeMillis()/1000L)?"Current ranked war":"Recent ranked-war activity",p.summary,p.known?(p.hits>0?GOOD:GOLD):BORDER));
            if(war.present)addCard(r,card("War status",war.headline(System.currentTimeMillis()/1000L)+"\n"+war.detail(System.currentTimeMillis()/1000L),BLUE));
            addCard(r,card("Requirement progress","v0.8 shows your verified hit count. Configurable faction hit requirements are intentionally deferred to the automation framework so the app does not invent a requirement.",BORDER));
            setContentView(s);
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
        if(slots!=null)for(int i=0;i<slots.length();i++){
            JSONObject slot=slots.optJSONObject(i);if(slot==null)continue;JSONObject user=slot.optJSONObject("user");if(user==null||user.optInt("id",0)!=playerId)continue;
            position=slot.optString("position","");cpr=slot.optInt("checkpoint_pass_rate",-1);JSONObject req=slot.optJSONObject("item_requirement");if(req!=null){itemRequired=true;itemAvailable=req.optBoolean("is_available",true);}break;
        }
        String summary=status+" • Difficulty "+difficulty+(position.isEmpty()?"":"\nYour slot: "+position)+(cpr>=0?" • CPR "+cpr+"%":"");
        return new OcSummary(true,name,summary,position,cpr,readyAt,itemRequired,itemAvailable);
    }

    private ParticipationSummary readParticipation(String key,WarStatus war){
        long now=System.currentTimeMillis()/1000L;
        long from=war.isLive(now)?war.start:now-(30L*86400L);
        try{
            JSONArray attacks=TornApiClient.getPagedArray("/user/attacks?filters=outgoing&from="+from+"&to="+now+"&sort=DESC&limit=100",key,"attacks",20);
            int hits=0;
            for(int i=0;i<attacks.length();i++){JSONObject a=attacks.optJSONObject(i);if(a!=null&&a.optBoolean("is_ranked_war",false))hits++;}
            String window=war.isLive(now)?"since this war started":"in the last 30 days";
            return new ParticipationSummary(true,hits,hits+" ranked-war hit"+(hits==1?"":"s")+" "+window+".");
        }catch(Exception e){
            return new ParticipationSummary(false,0,"Personal attack history is unavailable with this key. A Limited/Custom/Full key is required for verified personal hit counts.");
        }
    }

    private void renderError(String message){runOnUiThread(()->{ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Data unavailable");addCard(r,card("Unable to load",message,BAD));setContentView(s);});}

    private static final class ParticipationSummary {final boolean known;final int hits;final String summary;ParticipationSummary(boolean known,int hits,String summary){this.known=known;this.hits=hits;this.summary=summary;}}
    private static final class OcSummary {
        final boolean assigned;final String name;final String summary;final String position;final int checkpointPassRate;final long readyAt;final boolean itemRequired;final boolean itemAvailable;
        OcSummary(boolean assigned,String name,String summary,String position,int checkpointPassRate,long readyAt,boolean itemRequired,boolean itemAvailable){this.assigned=assigned;this.name=name;this.summary=summary;this.position=position;this.checkpointPassRate=checkpointPassRate;this.readyAt=readyAt;this.itemRequired=itemRequired;this.itemAvailable=itemAvailable;}
        static OcSummary none(String summary){return new OcSummary(false,"",summary,"",-1,0L,false,true);}
    }
}
