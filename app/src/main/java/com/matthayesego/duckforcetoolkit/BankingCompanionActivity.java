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
import android.view.View;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Central faction banking request queue with cache-first rendering and Torn payment handoff. */
public class BankingCompanionActivity extends Activity {
    private static final int BG=Color.rgb(5,8,12),PANEL=Color.rgb(12,18,26),PANEL2=Color.rgb(8,13,20),BORDER=Color.rgb(36,47,61),TEXT=Color.rgb(246,248,251),MUTED=Color.rgb(145,155,169),GOLD=Color.rgb(241,190,86),BLUE=Color.rgb(82,153,235),GREEN=Color.rgb(76,190,102),RED=Color.rgb(239,88,82);
    private static final long QUEUE_CACHE_MS=5L*60L*1000L;
    private SecureApiKeyStore keyStore;
    private int playerId,factionId;
    private String playerName="Member",factionName="Faction",position="Member";
    private boolean factionApiAccess=false,canManage=false,refreshing=false,balancesLoading=false;
    private JSONArray requests=new JSONArray();
    private final Map<Integer,Long> balances=new HashMap<>();
    private String balanceError="";

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        keyStore=new SecureApiKeyStore(this);
        factionId=getIntent().getIntExtra(FactionOpsActivity.EXTRA_FACTION_ID,0);
        String fn=getIntent().getStringExtra(FactionOpsActivity.EXTRA_FACTION_NAME);if(fn!=null&&!fn.isBlank())factionName=fn;
        String pos=getIntent().getStringExtra(DeveloperConsoleActivity.EXTRA_POSITION);if(pos!=null&&!pos.isBlank())position=pos;
        factionApiAccess=getIntent().getBooleanExtra(FactionOpsActivity.EXTRA_FACTION_API,false);
        loadQueue();
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private GradientDrawable gradient(int a,int b,int stroke,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private TextView eyebrow(String value,int color){TextView t=text(value,9.5f,color,true);t.setLetterSpacing(.11f);return t;}
    private Button button(String value,int accent){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextColor(TEXT);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL2,accent,12));return b;}
    private EditText field(String hint,boolean number){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(MUTED);e.setTextColor(TEXT);e.setSingleLine(true);e.setPadding(dp(13),0,dp(13),0);e.setBackground(rounded(PANEL2,BORDER,11));if(number)e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);return e;}
    @SuppressWarnings("deprecation") private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setClipToPadding(false);s.setBackgroundColor(BG);int l=dp(18),t=dp(14),r=dp(18),bt=dp(90);s.setPadding(l,t,r,bt);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),bt+i.getSystemWindowInsetBottom());return i;});return s;}
    private LinearLayout root(ScrollView s){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));return r;}
    private LinearLayout card(int accent){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(16),dp(18),dp(16));c.setBackground(gradient(PANEL,PANEL2,accent==Color.TRANSPARENT?BORDER:accent,19));return c;}
    private void add(LinearLayout r,View v){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(11);r.addView(v,p);}

    private void header(LinearLayout r){Button back=button("← Faction",BORDER);back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(dp(125),dp(44)));TextView e=eyebrow("FACTION OPERATIONS • BANKING",BLUE);LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);ep.topMargin=dp(18);r.addView(e,ep);r.addView(text("Banking Companion",30,TEXT,true));TextView sub=text("Request money, see the shared queue instantly, and jump straight into Torn when a payout needs action.",13,MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(5);sp.bottomMargin=dp(16);r.addView(sub,sp);}

    private void showLoading(String message){ScrollView s=shell();LinearLayout r=root(s);header(r);LinearLayout c=card(BORDER);c.addView(eyebrow("LOADING",GOLD));TextView m=text(message,18,TEXT,true);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(6);c.addView(m,p);add(r,c);setContentView(s);s.requestApplyInsets();}

    private void resolveScope(String key){
        AuthSession hot=TornApiClient.cachedSession(key);
        if(hot!=null){playerId=hot.playerId;playerName=hot.playerName;factionId=hot.factionId;factionName=hot.factionName;position=hot.position;factionApiAccess=hot.factionApiAccess;return;}
        FactionScopeCache.Scope s=FactionScopeCache.load(this,key);
        if(s!=null){playerId=s.playerId;playerName=s.playerName;factionId=s.factionId;factionName=s.factionName;position=s.position;factionApiAccess=s.factionApiAccess;}
    }

    private void loadQueue(){
        if(!CompanionBackendClient.isConfigured()){renderUnavailable("The shared faction backend is not configured in this build yet.");return;}
        String key=keyStore.load();if(key==null||key.isBlank()){renderUnavailable("Reconnect your Torn API key first.");return;}
        resolveScope(key);
        JSONObject warm=playerId>0&&factionId>0?StartupWarmCache.banking(factionId,playerId,QUEUE_CACHE_MS):null;
        if(warm!=null){applyResponse(warm);render();loadBalancesAsync(key);refreshLive(key,false);return;}
        showLoading("Loading the shared banking queue…");
        refreshLive(key,true);
    }

    private void refreshLive(String key,boolean firstLoad){
        if(refreshing)return;refreshing=true;if(!firstLoad)render();
        new Thread(()->{try{
            if(playerId<=0||factionId<=0){
                AuthSession verified=TornApiClient.cachedSession(key);if(verified==null)verified=TornApiClient.authenticate(key);
                FactionScopeCache.save(this,key,verified);StartupWarmCache.putSession(verified);
                playerId=verified.playerId;playerName=verified.playerName;factionId=verified.factionId;factionName=verified.factionName;position=verified.position;factionApiAccess=verified.factionApiAccess;
            }
            JSONObject response=CompanionBackendClient.getBankingRequests(key,false);
            JSONObject user=response.optJSONObject("user");
            int cacheFaction=user!=null?user.optInt("faction_id",factionId):factionId;
            int cachePlayer=user!=null?user.optInt("id",playerId):playerId;
            if(cacheFaction>0&&cachePlayer>0)StartupWarmCache.putBanking(cacheFaction,cachePlayer,response);
            runOnUiThread(()->{refreshing=false;applyResponse(response);render();loadBalancesAsync(key);});
        }catch(Exception e){String m=e.getMessage()==null?"Unable to load the banking queue.":e.getMessage();runOnUiThread(()->{refreshing=false;if(firstLoad)renderUnavailable(m);else{render();Toast.makeText(this,"Queue refresh failed: "+m,Toast.LENGTH_LONG).show();}});}}).start();
    }

    private void applyResponse(JSONObject response){
        if(response==null)return;
        JSONObject user=response.optJSONObject("user");if(user!=null){int uid=user.optInt("id",0),fid=user.optInt("faction_id",0);if(uid>0)playerId=uid;if(fid>0)factionId=fid;String un=user.optString("name","");if(!un.isBlank())playerName=un;String fn=user.optString("faction_name","");if(!fn.isBlank())factionName=fn;String pos=user.optString("position","");if(!pos.isBlank())position=pos;}
        boolean memberPreview=MemberPresentationPolicy.memberPreview(this);
        boolean backendCanManage=response.optBoolean("can_manage",false);
        JSONArray rows=response.optJSONArray("requests");rows=rows==null?new JSONArray():rows;
        canManage=!memberPreview&&backendCanManage;
        requests=memberPreview?memberRequestsOnly(rows,playerId):rows;
    }

    private JSONArray memberRequestsOnly(JSONArray rows,int id){JSONArray out=new JSONArray();if(id<=0)return out;for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row!=null&&row.optInt("requester_id",0)==id)out.put(row);}return out;}

    private void loadBalancesAsync(String key){
        if(!canManage||balancesLoading)return;
        if(!factionApiAccess){if(balanceError.isEmpty()){balanceError="Detailed faction balances are not available to this key. Amount requests still work, and full-balance requests can be resolved by the requesting member at submission time.";render();}return;}
        balancesLoading=true;
        new Thread(()->{String error="";Map<Integer,Long> fresh=new HashMap<>();try{
            JSONObject data=TornApiClient.getJson("/faction/balance?cat=current",key);
            JSONObject balance=data.optJSONObject("balance");JSONArray members=balance==null?null:balance.optJSONArray("members");
            for(int i=0;members!=null&&i<members.length();i++){JSONObject m=members.optJSONObject(i);if(m==null)continue;fresh.put(m.optInt("id",0),Math.max(0L,m.optLong("money",0L)));}
        }catch(Exception e){error=e.getMessage()==null?"Current faction balances could not be loaded.":e.getMessage();}
            String finalError=error;runOnUiThread(()->{balancesLoading=false;balances.clear();balances.putAll(fresh);balanceError=finalError;render();});
        },"TornFCA-BankingBalances").start();
    }

    private void render(){
        ScrollView s=shell();LinearLayout r=root(s);header(r);
        addRequestCard(r);
        LinearLayout status=card(canManage?GREEN:BLUE);status.addView(eyebrow(canManage?"BANKER ACCESS":"MEMBER VIEW",canManage?GREEN:BLUE));status.addView(text(canManage?"Shared queue + payout controls":"Your banking requests",19,TEXT,true));status.addView(text(canManage?"The queue opens from warmed data first. Current balances refresh separately so they never hold this screen hostage.":"Submit a request here. Authorized faction bankers receive it in the shared queue.",12.5f,MUTED,false));if(!balanceError.isEmpty())status.addView(text(balanceError,11.5f,RED,false));
        if(canManage){Button torn=button("Open Torn Faction Banking",BLUE);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));tp.topMargin=dp(10);status.addView(torn,tp);torn.setOnClickListener(v->openTornBanking());}
        Button refresh=button(refreshing?"Refreshing Queue…":"Refresh Queue",BORDER);refresh.setEnabled(!refreshing);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));rp.topMargin=dp(8);status.addView(refresh,rp);refresh.setOnClickListener(v->{String key=keyStore.load();if(key!=null)refreshLive(key,false);});add(r,status);

        if(requests.length()==0){LinearLayout none=card(BORDER);none.addView(text("No banking requests yet.",17,TEXT,true));none.addView(text("Requests you submit in TornFCA will appear here.",12,MUTED,false));add(r,none);}else{
            for(int i=0;i<requests.length();i++){JSONObject row=requests.optJSONObject(i);if(row!=null)add(r,requestCard(row));}
        }
        TextView foot=text("TornFCA never moves faction money through the API. The actual transfer is completed by an authorized faction user inside Torn.",10.5f,MUTED,false);foot.setGravity(Gravity.CENTER);r.addView(foot);
        setContentView(s);s.requestApplyInsets();
    }

    private void addRequestCard(LinearLayout r){
        LinearLayout c=card(GOLD);c.addView(eyebrow("NEW REQUEST",GOLD));c.addView(text("Request a faction payout",19,TEXT,true));c.addView(text("Leave the amount blank to request your full current faction balance. TornFCA will try to resolve that balance at submission time so leadership receives an actionable amount.",12,MUTED,false));
        EditText amount=field("Amount — blank = full balance",true);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50));ap.topMargin=dp(11);c.addView(amount,ap);
        EditText note=field("Optional note",false);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48));np.topMargin=dp(8);c.addView(note,np);
        Button submit=button("Submit Banking Request",GOLD);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48));bp.topMargin=dp(9);c.addView(submit,bp);
        submit.setOnClickListener(v->{String key=keyStore.load();if(key==null)return;String raw=amount.getText().toString().trim(),noteValue=note.getText().toString().trim();submit.setEnabled(false);submit.setText(raw.isEmpty()?"Resolving full balance…":"Submitting…");new Thread(()->{try{
            long resolved=0L;boolean requestedFull=raw.isEmpty();String sendAmount=raw;
            if(requestedFull){resolved=resolveOwnFactionBalance(key);if(resolved>0L)sendAmount=String.valueOf(resolved);}
            JSONObject response=CompanionBackendClient.submitBankingRequest(key,sendAmount,noteValue);JSONObject request=response.optJSONObject("request");long finalResolved=resolved;runOnUiThread(()->{
                if(request!=null)addRequestLocally(request);
                Toast.makeText(this,requestedFull&&finalResolved>0L?"Full-balance request queued for "+money(finalResolved)+".":"Banking request queued.",Toast.LENGTH_SHORT).show();
                render();
            });
            String summary=requestedFull?(resolved>0L?"Full balance request: "+money(resolved):"Full balance requested"):("Requested "+raw);
            if(CommunityBackendClient.isConfigured())new Thread(()->{try{CommunityBackendClient.publishBankingRequest(key,summary,noteValue);}catch(Exception ignored){}},"TornFCA-BankingAlert").start();
            new Thread(()->{try{JSONObject fresh=CompanionBackendClient.getBankingRequests(key,false);if(factionId>0&&playerId>0)StartupWarmCache.putBanking(factionId,playerId,fresh);runOnUiThread(()->{applyResponse(fresh);render();loadBalancesAsync(key);});}catch(Exception ignored){}},"TornFCA-BankingRefresh").start();
        }catch(Exception e){String m=e.getMessage()==null?"Unable to submit banking request.":e.getMessage();runOnUiThread(()->{submit.setEnabled(true);submit.setText("Submit Banking Request");Toast.makeText(this,m,Toast.LENGTH_LONG).show();});}}).start();});
        add(r,c);
    }

    private long resolveOwnFactionBalance(String key){
        try{
            JSONObject root=TornApiClient.getJson("/user?selections=factionbalance",key);
            JSONObject b=root.optJSONObject("factionBalance");if(b==null)b=root.optJSONObject("factionbalance");if(b==null)b=root.optJSONObject("faction_balance");
            return b==null?0L:Math.max(0L,b.optLong("money",0L));
        }catch(Exception ignored){return 0L;}
    }

    private void addRequestLocally(JSONObject request){
        if(request==null)return;JSONArray next=new JSONArray();next.put(request);String id=request.optString("id","");for(int i=0;i<requests.length();i++){JSONObject row=requests.optJSONObject(i);if(row==null||(!id.isEmpty()&&id.equals(row.optString("id",""))))continue;next.put(row);}requests=next;cacheCurrentQueue();
    }

    private void cacheCurrentQueue(){
        if(factionId<=0||playerId<=0)return;try{JSONObject root=new JSONObject();root.put("ok",true);root.put("can_manage",canManage);root.put("requests",requests);JSONObject user=new JSONObject();user.put("id",playerId);user.put("name",playerName);user.put("faction_id",factionId);user.put("faction_name",factionName);user.put("position",position);root.put("user",user);StartupWarmCache.putBanking(factionId,playerId,root);}catch(Exception ignored){}
    }

    private LinearLayout requestCard(JSONObject row){
        int requesterId=row.optInt("requester_id",0);String name=row.optString("requester_name","Member");String status=row.optString("status","PENDING").toUpperCase(Locale.US);String source=row.optString("source","TORNFCA");boolean amountMode=!row.isNull("requested_amount");long requested=amountMode?Math.max(0L,row.optLong("requested_amount",0L)):0L;Long current=balances.get(requesterId);Long due=amountMode?requested:current;
        int accent="PAID".equals(status)||"HANDLED".equals(status)?GREEN:"CANCELLED".equals(status)?MUTED:"PENDING".equals(status)?GOLD:BLUE;
        LinearLayout c=card(Color.TRANSPARENT);c.addView(eyebrow(status+" • "+source.replace('_',' '),accent));c.addView(text(name,18,TEXT,true));
        String moneyLine=amountMode?"Requested "+money(requested):"Requested FULL BALANCE";if(current!=null)moneyLine+=" • current balance "+money(current);c.addView(text(moneyLine,12.5f,MUTED,false));String note=row.optString("note","");String chat=row.optString("request_text","");if(!note.isBlank())c.addView(text(note,11.5f,TEXT,false));else if(!chat.isBlank())c.addView(text(chat,11.5f,TEXT,false));
        if(canManage&&"PENDING".equals(status)){
            if(due!=null&&due>0){Button pay=button("Open "+money(due)+" Payout in Torn",BLUE);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(47));pp.topMargin=dp(10);c.addView(pay,pp);long finalDue=due;pay.setOnClickListener(v->openTornPayment(requesterId,finalDue));}
            else{TextView unresolved=text("The exact full-balance amount is not readable yet. Open Torn banking to review the member balance directly.",11.5f,RED,false);LinearLayout.LayoutParams up=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);up.topMargin=dp(9);c.addView(unresolved,up);Button open=button("Open Faction Banking in Torn",BLUE);LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(45));op.topMargin=dp(9);c.addView(open,op);open.setOnClickListener(v->openTornBanking());}
            LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);Button paid=button("Confirm Paid",GREEN);Button cancel=button("Cancel",RED);actions.addView(paid,new LinearLayout.LayoutParams(0,dp(44),1f));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(44),1f);cp.leftMargin=dp(8);actions.addView(cancel,cp);LinearLayout.LayoutParams xp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));xp.topMargin=dp(8);c.addView(actions,xp);String id=row.optString("id","");paid.setOnClickListener(v->updateStatus(id,"PAID"));cancel.setOnClickListener(v->updateStatus(id,"CANCELLED"));
        }
        return c;
    }

    private void updateStatus(String requestId,String status){String key=keyStore.load();if(key==null)return;new Thread(()->{try{JSONObject response=CompanionBackendClient.updateBankingRequest(key,requestId,status);JSONObject updated=response.optJSONObject("request");runOnUiThread(()->{if(updated!=null)replaceRequestLocally(updated);Toast.makeText(this,"Banking request updated.",Toast.LENGTH_SHORT).show();render();});}catch(Exception e){String m=e.getMessage()==null?"Unable to update banking request.":e.getMessage();runOnUiThread(()->Toast.makeText(this,m,Toast.LENGTH_LONG).show());}}).start();}

    private void replaceRequestLocally(JSONObject updated){String id=updated.optString("id","");JSONArray next=new JSONArray();for(int i=0;i<requests.length();i++){JSONObject row=requests.optJSONObject(i);if(row!=null&&id.equals(row.optString("id","")))next.put(updated);else if(row!=null)next.put(row);}requests=next;cacheCurrentQueue();}

    private void openTornPayment(int id,long amount){String url="https://www.torn.com/factions.php?step=your#/tab=controls&option=give-to-user&giveMoneyTo="+id+"&money="+amount;openUrl(url,"Unable to open Torn payout controls.");}
    private void openTornBanking(){openUrl("https://www.torn.com/factions.php?step=your#/tab=controls","Unable to open Torn faction controls.");}
    private void openUrl(String url,String error){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){Toast.makeText(this,error,Toast.LENGTH_SHORT).show();}}
    private static String money(long v){return "$"+NumberFormat.getIntegerInstance(Locale.US).format(v);}

    private void renderUnavailable(String message){ScrollView s=shell();LinearLayout r=root(s);header(r);LinearLayout c=card(RED);c.addView(eyebrow("BANKING UNAVAILABLE",RED));c.addView(text(message,13,TEXT,false));Button retry=button("Retry",GOLD);retry.setOnClickListener(v->loadQueue());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48));p.topMargin=dp(11);c.addView(retry,p);add(r,c);setContentView(s);s.requestApplyInsets();}
}
