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
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Member-first daily readiness screen built from the signed-in player's own Torn data. */
public class MemberDailyActivity extends Activity {
    private static final int BG=Color.rgb(6,9,13),PANEL=Color.rgb(15,20,28),PANEL2=Color.rgb(10,15,22),BORDER=Color.rgb(45,55,69),TEXT=Color.rgb(244,246,249),MUTED=Color.rgb(154,164,178),GOLD=Color.rgb(241,194,106),BLUE=Color.rgb(88,166,255),GREEN=Color.rgb(63,185,80),RED=Color.rgb(248,81,73);
    private SecureApiKeyStore keyStore;

    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);keyStore=new SecureApiKeyStore(this);showLoading();load();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String value,int accent){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextColor(TEXT);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL2,accent,12));return b;}
    @SuppressWarnings("deprecation") private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);int l=dp(16),t=dp(18),r=dp(16),bt=dp(30);s.setPadding(l,t,r,bt);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),bt+i.getSystemWindowInsetBottom());return i;});return s;}
    private LinearLayout root(ScrollView s){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));return r;}
    private LinearLayout card(int stroke){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(17),dp(15),dp(17),dp(15));c.setBackground(rounded(PANEL,stroke,18));return c;}
    private void add(LinearLayout r,LinearLayout c){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(10);r.addView(c,p);}
    private void section(LinearLayout r,String label){TextView t=text(label,11,MUTED,true);t.setLetterSpacing(.11f);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(5);p.bottomMargin=dp(8);r.addView(t,p);}
    private void header(LinearLayout r,String subtitle){Button back=button("← Companion",BORDER);back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(dp(132),dp(44)));TextView brand=text("FACTION COMPANION • MEMBER",10,GOLD,true);brand.setLetterSpacing(.13f);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);bp.topMargin=dp(16);r.addView(brand,bp);r.addView(text("My Day",31,TEXT,true));TextView sub=text(subtitle,13,MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(5);sp.bottomMargin=dp(15);r.addView(sub,sp);}
    private void showLoading(){ScrollView s=shell();LinearLayout r=root(s);header(r,"Building your personal readiness snapshot…");LinearLayout c=card(BLUE);c.addView(text("Checking readiness and faction benefits…",17,TEXT,true));c.addView(text("Your personal data, faction standing and public/current faction state are loaded together to keep this screen fast.",12.5f,MUTED,false));add(r,c);setContentView(s);s.requestApplyInsets();}

    private void load(){
        String key=keyStore.load();if(key==null||key.isBlank()){renderError("Reconnect your Torn API key to use My Day.");return;}
        new Thread(()->{
            ExecutorService pool=null;
            try{
                AuthSession session=TornApiClient.cachedSession(key);if(session==null)session=TornApiClient.authenticate(key);
                final AuthSession verified=session;
                pool=Executors.newFixedThreadPool(3);
                Future<JSONObject> selfFuture=pool.submit(()->TornApiClient.getJson("/user?selections=bars,cooldowns,travel,refills,organizedcrime,perks,factionbalance",key));
                Future<JSONObject> warFuture=pool.submit(()->{try{return TornApiClient.getJson("/faction/wars",key);}catch(Exception ignored){return new JSONObject();}});
                Future<JSONObject> membersFuture=pool.submit(()->{try{return TornApiClient.getJson("/faction/"+verified.factionId+"/members",key);}catch(Exception ignored){return new JSONObject();}});
                JSONObject self=selfFuture.get();
                WarStatus war=WarStatus.from(warFuture.get(),verified.factionId);
                JSONObject membersRoot=membersFuture.get();
                Standing standing=findStanding(membersRoot.optJSONArray("members"),verified.playerId,verified.position);

                JSONObject bars=self.optJSONObject("bars");JSONObject chain=bars==null?null:bars.optJSONObject("chain");
                long now=System.currentTimeMillis()/1000L;long chainStart=chain==null?0:chain.optLong("start",0);long warStart=war.isLive(now)?war.start:0;int chainHits=0,warHits=0;
                if(chainStart>0||warStart>0){long from=chainStart>0&&warStart>0?Math.min(chainStart,warStart):Math.max(chainStart,warStart);JSONArray attacks=TornApiClient.getPagedArray("/user/attacks?filters=outgoing&from="+from+"&to="+now+"&sort=DESC&limit=100",key,"attacks",8);for(int i=0;i<attacks.length();i++){JSONObject a=attacks.optJSONObject(i);if(a==null)continue;long started=a.optLong("started",a.optLong("timestamp_started",0));if(warStart>0&&started>=warStart&&a.optBoolean("is_ranked_war",false))warHits++;if(chainStart>0&&started>=chainStart&&a.optInt("chain",0)>0&&a.optDouble("respect_gain",0d)>0&&!a.optBoolean("is_interrupted",false))chainHits++;}}
                AuthSession finalSession=verified;int finalChainHits=chainHits,finalWarHits=warHits;Standing finalStanding=standing;
                runOnUiThread(()->render(finalSession,self,war,chain,finalChainHits,finalWarHits,finalStanding));
            }catch(Exception e){renderError(e.getMessage()==null?"Unable to build your daily snapshot.":e.getMessage());}
            finally{if(pool!=null)pool.shutdownNow();}
        },"TornFCA-MyDay").start();
    }

    private void render(AuthSession session,JSONObject self,WarStatus war,JSONObject chain,int chainHits,int warHits,Standing standing){
        ScrollView s=shell();LinearLayout r=root(s);header(r,session.factionName+" • "+session.playerName);
        JSONObject bars=self.optJSONObject("bars"),cooldowns=self.optJSONObject("cooldowns"),travel=self.optJSONObject("travel"),refills=self.optJSONObject("refills"),perks=self.optJSONObject("perks"),balance=self.optJSONObject("factionBalance");OcSummary oc=readOc(self.optJSONObject("organizedCrime"),session.playerId);long now=System.currentTimeMillis()/1000L;

        section(r,"WHAT SHOULD I DO NEXT?");String action;int actionColor=GREEN;if(war.isLive(now)&&warHits==0){action="War is live — you have no verified ranked-war hits yet. Open My War when you're ready to contribute.";actionColor=GOLD;}else if(chain!=null&&chain.optInt("current",0)>0&&chain.optInt("timeout",0)<=120){action="The faction chain is active and its timer is getting low. If you're able to hit, this is the priority.";actionColor=RED;}else if(oc.assigned&&oc.itemRequired&&!oc.itemAvailable){action="Your organized crime needs an item that Torn reports as unavailable. Check your OC before it is ready.";actionColor=GOLD;}else if(barFull(bars,"energy")){action="Your energy is full. This is a good time to train or use it before regeneration is wasted.";actionColor=GREEN;}else if(chain!=null&&chain.optInt("current",0)>0){action="The faction chain is active. Your current-chain contribution is "+chainHits+" verified hit"+(chainHits==1?"":"s")+".";actionColor=BLUE;}else if(cooldowns!=null&&cooldowns.optInt("drug",1)==0){action="Drug cooldown is clear. No urgent faction obligation is showing right now.";actionColor=GREEN;}else action="No urgent faction obligation is showing right now. Keep an eye on your OC, energy and upcoming war status.";LinearLayout next=card(actionColor);next.addView(text(action,17,TEXT,true));add(r,next);

        section(r,"PERSONAL READINESS");if(bars!=null){LinearLayout b=card(BLUE);b.addView(text("Bars",18,TEXT,true));b.addView(text(barLine(bars,"energy","Energy")+"\n"+barLine(bars,"nerve","Nerve")+"\n"+barLine(bars,"life","Life")+"\n"+barLine(bars,"happy","Happy"),13,MUTED,false));add(r,b);}if(cooldowns!=null){LinearLayout c=card(BORDER);c.addView(text("Cooldowns",18,TEXT,true));c.addView(text("Drug: "+duration(cooldowns.optInt("drug",0))+" • Medical: "+duration(cooldowns.optInt("medical",0))+" • Booster: "+duration(cooldowns.optInt("booster",0)),13,MUTED,false));add(r,c);}if(refills!=null){LinearLayout f=card(BORDER);f.addView(text("Refills",18,TEXT,true));f.addView(text("Energy: "+available(refills.optBoolean("energy",false))+" • Nerve: "+available(refills.optBoolean("nerve",false))+" • Token: "+available(refills.optBoolean("token",false)),13,MUTED,false));add(r,f);}
        if(travel!=null&&travel.optInt("time_left",0)>0){section(r,"TRAVEL");LinearLayout t=card(GOLD);t.addView(text("Traveling to "+travel.optString("destination","destination"),18,TEXT,true));t.addView(text("Method: "+travel.optString("method","Unknown")+" • ETA in "+duration(travel.optInt("time_left",0))+(travel.optLong("arrival_at",0)>0?"\nArrival: "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(travel.optLong("arrival_at",0)*1000L)):""),13,MUTED,false));add(r,t);}

        section(r,"MY FACTION TOOLS");
        LinearLayout standingCard=card(BLUE);standingCard.addView(text("My Faction Standing",18,TEXT,true));standingCard.addView(text(standing.summary(session.position),13,MUTED,false));add(r,standingCard);
        LinearLayout balanceCard=card(GREEN);balanceCard.addView(text("My Faction Balance",18,TEXT,true));if(balance==null)balanceCard.addView(text("Torn did not return a personal faction balance for this account.",13,MUTED,false));else balanceCard.addView(text("Money: "+money(balance.optLong("money",0L))+"\nPoints: "+NumberFormat.getIntegerInstance(Locale.US).format(balance.optLong("points",0L))+"\nOnly your own faction balance is shown here.",13,MUTED,false));add(r,balanceCard);
        JSONArray factionPerks=perks==null?null:perks.optJSONArray("faction");LinearLayout perkCard=card(GOLD);perkCard.addView(text("Faction Perks",18,TEXT,true));if(factionPerks==null||factionPerks.length()==0)perkCard.addView(text("No faction perks were returned by Torn.",13,MUTED,false));else{StringBuilder list=new StringBuilder();for(int i=0;i<factionPerks.length();i++){if(i>0)list.append("\n");list.append("• ").append(factionPerks.optString(i,"Perk"));}perkCard.addView(text(list.toString(),12.5f,MUTED,false));}add(r,perkCard);

        section(r,"FACTION READINESS");LinearLayout ocCard=card(oc.assigned?(oc.itemRequired&&!oc.itemAvailable?RED:GREEN):GOLD);ocCard.addView(text(oc.assigned?"My OC — "+oc.name:"My OC",18,TEXT,true));ocCard.addView(text(oc.summary,13,MUTED,false));add(r,ocCard);LinearLayout chainCard=card(chain!=null&&chain.optInt("current",0)>0?GREEN:BORDER);chainCard.addView(text("My Chain",18,TEXT,true));chainCard.addView(text(chain==null||chain.optInt("current",0)<=0?"No active faction chain returned by Torn.":chain.optInt("current",0)+" / "+chain.optInt("max",0)+" • timer "+duration(chain.optInt("timeout",0))+"\nYour verified contribution this chain: "+chainHits+" hit"+(chainHits==1?"":"s"),13,MUTED,false));add(r,chainCard);LinearLayout warCard=card(war.isLive(now)?RED:war.isUpcoming(now)?GOLD:BORDER);warCard.addView(text("My War",18,TEXT,true));String warBody=war.headline(now)+"\n"+war.detail(now);if(war.isLive(now))warBody+="\nYour verified hits this war: "+warHits;warCard.addView(text(warBody,13,MUTED,false));add(r,warCard);

        TextView foot=text("My Day is a read-only snapshot from your Torn API key. TornFCA does not perform game actions for you.",10.5f,MUTED,false);foot.setGravity(Gravity.CENTER);r.addView(foot);setContentView(s);s.requestApplyInsets();
    }

    private Standing findStanding(JSONArray members,int playerId,String fallbackPosition){if(members!=null)for(int i=0;i<members.length();i++){JSONObject m=members.optJSONObject(i);if(m!=null&&m.optInt("id",0)==playerId)return Standing.from(m,fallbackPosition);}return Standing.unavailable(fallbackPosition);}
    private boolean barFull(JSONObject bars,String name){JSONObject b=bars==null?null:bars.optJSONObject(name);return b!=null&&b.optInt("maximum",0)>0&&b.optInt("current",0)>=b.optInt("maximum",0);}
    private String barLine(JSONObject bars,String key,String label){JSONObject b=bars.optJSONObject(key);if(b==null)return label+": unavailable";String out=label+": "+b.optInt("current",0)+" / "+b.optInt("maximum",0);int full=b.optInt("full_time",0);if(full>0&&b.optInt("current",0)<b.optInt("maximum",0))out+=" • full in "+duration(full);return out;}
    private String available(boolean value){return value?"Available":"Used";}
    private String duration(long seconds){long s=Math.max(0,seconds);long d=s/86400;s%=86400;long h=s/3600;s%=3600;long m=s/60;if(d>0)return d+"d "+h+"h";if(h>0)return h+"h "+m+"m";return m+"m";}
    private String money(long value){return "$"+NumberFormat.getIntegerInstance(Locale.US).format(Math.max(0L,value));}
    private OcSummary readOc(JSONObject crime,int playerId){if(crime==null||crime.has("error"))return OcSummary.none("You are not currently assigned to an organized crime.");String name=crime.optString("name","Organized Crime"),status=crime.optString("status","Unknown");int difficulty=crime.optInt("difficulty",0);String position="";int cpr=-1;boolean itemRequired=false,itemAvailable=true;JSONArray slots=crime.optJSONArray("slots");for(int i=0;slots!=null&&i<slots.length();i++){JSONObject slot=slots.optJSONObject(i);if(slot==null)continue;JSONObject user=slot.optJSONObject("user");if(user==null||user.optInt("id",0)!=playerId)continue;position=slot.optString("position","");cpr=slot.optInt("checkpoint_pass_rate",-1);JSONObject req=slot.optJSONObject("item_requirement");if(req!=null){itemRequired=true;itemAvailable=req.optBoolean("is_available",true);}break;}String summary=status+" • Difficulty "+difficulty+(position.isEmpty()?"":"\nYour slot: "+position)+(cpr>=0?" • CPR "+cpr+"%":"")+(itemRequired?"\nRequired item: "+(itemAvailable?"ready":"missing"):"");return new OcSummary(true,name,summary,itemRequired,itemAvailable);}
    private void renderError(String message){runOnUiThread(()->{ScrollView s=shell();LinearLayout r=root(s);header(r,"Data unavailable");LinearLayout c=card(RED);c.addView(text("Unable to build My Day",19,TEXT,true));c.addView(text(message,13,MUTED,false));Button retry=button("Retry",GOLD);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));rp.topMargin=dp(9);c.addView(retry,rp);retry.setOnClickListener(v->{showLoading();load();});add(r,c);setContentView(s);s.requestApplyInsets();});}

    private static final class Standing{
        final boolean found;final String position,status,lastAction,revive;final int level,days;final boolean inOc,onWall,revivable,earlyDischarge;
        Standing(boolean found,String position,String status,String lastAction,String revive,int level,int days,boolean inOc,boolean onWall,boolean revivable,boolean earlyDischarge){this.found=found;this.position=position;this.status=status;this.lastAction=lastAction;this.revive=revive;this.level=level;this.days=days;this.inOc=inOc;this.onWall=onWall;this.revivable=revivable;this.earlyDischarge=earlyDischarge;}
        static Standing from(JSONObject m,String fallbackPosition){JSONObject st=m.optJSONObject("status"),last=m.optJSONObject("last_action");String status=st==null?"Unknown":st.optString("description",st.optString("state","Unknown"));String lastAction=last==null?"Unknown":last.optString("relative","Unknown");String pos=m.optString("position",fallbackPosition==null?"Member":fallbackPosition);return new Standing(true,pos,status,lastAction,m.optString("revive_setting","Unknown"),m.optInt("level",0),m.optInt("days_in_faction",0),m.optBoolean("is_in_oc",false),m.optBoolean("is_on_wall",false),m.optBoolean("is_revivable",false),m.optBoolean("has_early_discharge",false));}
        static Standing unavailable(String fallbackPosition){return new Standing(false,fallbackPosition==null?"Member":fallbackPosition,"Unavailable","Unavailable","Unknown",0,0,false,false,false,false);}
        String summary(String fallbackPosition){if(!found)return"Position: "+(position==null||position.isBlank()?fallbackPosition:position)+"\nDetailed standing is temporarily unavailable; the rest of My Day can still be used.";StringBuilder b=new StringBuilder();b.append("Position: ").append(position).append(" • Level ").append(level).append("\n").append(days).append(" day").append(days==1?"":"s").append(" in faction • ").append(inOc?"In an OC":"Not in an OC");if(onWall)b.append(" • Defending wall");b.append("\nStatus: ").append(status).append(" • Last action: ").append(lastAction);if(!"Unknown".equalsIgnoreCase(revive))b.append("\nRevive setting: ").append(revive);if(earlyDischarge)b.append(" • Early discharge available");else if(revivable)b.append(" • Revivable");return b.toString();}
    }
    private static final class OcSummary{final boolean assigned;final String name,summary;final boolean itemRequired,itemAvailable;OcSummary(boolean assigned,String name,String summary,boolean itemRequired,boolean itemAvailable){this.assigned=assigned;this.name=name;this.summary=summary;this.itemRequired=itemRequired;this.itemAvailable=itemAvailable;}static OcSummary none(String summary){return new OcSummary(false,"",summary,false,true);}}
}
