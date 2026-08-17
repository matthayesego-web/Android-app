package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class CompanionActivity extends Activity {
    private static final String APP_VERSION = TornFcaBrand.VERSION;
    private static final int BG=Color.rgb(8,12,18),BG2=Color.rgb(12,18,27),PANEL=Color.rgb(20,27,38),PANEL2=Color.rgb(27,36,49),BORDER=Color.rgb(49,63,81),ACCENT=Color.rgb(243,184,52),ACCENT2=Color.rgb(255,216,118),TEXT=Color.rgb(245,248,252),MUTED=Color.rgb(151,163,179),GOOD=Color.rgb(63,185,80),BAD=Color.rgb(248,81,73),BLUE=Color.rgb(88,166,255);
    private enum Screen{LOGIN,HOME,BANKING,LEADERSHIP,DEVELOPER}
    private Screen screen=Screen.LOGIN;private volatile AuthSession session;private SecureApiKeyStore keyStore;

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);keyStore=new SecureApiKeyStore(this);
        String saved=keyStore.load();
        if(saved==null||saved.trim().isEmpty()){showLogin(null);return;}
        FactionScopeCache.Scope cached=FactionScopeCache.load(this,saved);
        if(cached!=null){session=sessionFromScope(cached);showHome();refreshSavedSession(saved);}
        else authenticate(saved,true);
    }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int color,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private GradientDrawable gradient(int start,int end,int stroke,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{start,end});d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private TextView section(String value){TextView t=text(value.toUpperCase(),12,MUTED,true);t.setLetterSpacing(.08f);return t;}
    private Button primary(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(Color.rgb(23,17,7));b.setTextSize(14);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(gradient(ACCENT2,ACCENT,ACCENT,12));return b;}
    private Button secondary(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(TEXT);b.setTextSize(13);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL2,BORDER,11));return b;}
    @SuppressWarnings("deprecation") private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);int l=dp(16),t=dp(16),r=dp(16),b=dp(28);s.setPadding(l,t,r,b);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),b+i.getSystemWindowInsetBottom());return i;});return s;}
    private LinearLayout column(ScrollView s){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);s.addView(c,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));return c;}
    private void spacer(LinearLayout c,int amount){View v=new View(this);c.addView(v,new LinearLayout.LayoutParams(1,dp(amount)));}
    private LinearLayout card(String title,String body,int stroke){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(15),dp(16),dp(15));c.setBackground(rounded(PANEL,stroke,17));c.addView(text(title,18,TEXT,true));if(body!=null&&!body.isEmpty()){TextView b=text(body,13,MUTED,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(5);c.addView(b,p);}return c;}
    private void addCard(LinearLayout p,LinearLayout c){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.bottomMargin=dp(10);p.addView(c,lp);}

    private void showLogin(String error){
        screen=Screen.LOGIN;session=null;ScrollView scroll=shell();LinearLayout c=column(scroll);
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER_HORIZONTAL);hero.setPadding(dp(22),dp(28),dp(22),dp(26));hero.setBackground(gradient(Color.rgb(35,49,69),Color.rgb(17,24,35),ACCENT,24));
        TextView icon=text("🦆",52,TEXT,false);icon.setGravity(Gravity.CENTER);hero.addView(icon);TextView brand=text("TORNFCA",13,ACCENT2,true);brand.setLetterSpacing(.22f);brand.setGravity(Gravity.CENTER);hero.addView(brand);TextView title=text("Faction Companion",30,TEXT,true);title.setGravity(Gravity.CENTER);hero.addView(title);TextView sub=text("Your faction tools, requests and permitted leadership access in one place.",14,MUTED,false);sub.setGravity(Gravity.CENTER);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(7);hero.addView(sub,sp);c.addView(hero);spacer(c,14);
        LinearLayout login=card("Connect your Torn account","Your key verifies your identity and current Torn faction, then follows the storage option you choose at sign-in.",BORDER);EditText key=new EditText(this);key.setHint("16-character Torn API key");key.setHintTextColor(MUTED);key.setTextColor(TEXT);key.setSingleLine(true);key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);key.setPadding(dp(14),0,dp(14),0);key.setBackground(rounded(BG2,BORDER,12));LinearLayout.LayoutParams kp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(54));kp.topMargin=dp(14);login.addView(key,kp);Button connect=primary("Connect to TornFCA");LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50));bp.topMargin=dp(12);login.addView(connect,bp);TextView status=text(error==null?"🔐 Torn identity + faction verification  •  v"+APP_VERSION:error,12,error==null?MUTED:BAD,false);status.setGravity(Gravity.CENTER_HORIZONTAL);LinearLayout.LayoutParams stp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);stp.topMargin=dp(10);login.addView(status,stp);connect.setOnClickListener(v->{String value=key.getText().toString().trim();if(value.isEmpty()){status.setText("Enter an API key first.");status.setTextColor(BAD);return;}connect.setEnabled(false);connect.setText("Checking access…");authenticate(value,false);});c.addView(login);setContentView(scroll);scroll.requestApplyInsets();
    }

    private void showLoading(){ScrollView s=shell();LinearLayout c=column(s);c.addView(card("TornFCA","Verifying your saved Torn account and current faction permissions…",ACCENT));setContentView(s);s.requestApplyInsets();}

    private AuthSession sessionFromScope(FactionScopeCache.Scope cached){
        boolean resolved=AccessPolicy.isLeaderPosition(cached.position);
        return new AuthSession(cached.playerId,cached.playerName,cached.factionId,cached.factionName,cached.position,cached.factionApiAccess,AccessTier.GREEN,new JSONArray(),new JSONArray(),resolved);
    }

    private void refreshSavedSession(String key){
        new Thread(()->{
            try{
                AuthSession fresh=TornApiClient.authenticate(key);
                keyStore.save(key);FactionScopeCache.save(this,key,fresh);session=fresh;
                runOnUiThread(()->{if(screen==Screen.HOME)showHome();});
                resolveBackendPermissionsAsync(key,fresh);
            }catch(Exception ignored){}
        },"TornFCA-SessionRefresh").start();
    }

    private void authenticate(String key,boolean saved){
        if(saved)showLoading();
        new Thread(()->{
            try{
                AuthSession result=TornApiClient.authenticate(key);
                keyStore.save(key);FactionScopeCache.save(this,key,result);session=result;
                runOnUiThread(this::showHome);
                resolveBackendPermissionsAsync(key,result);
            }catch(Exception e){
                if(saved)keyStore.clear();String message=e.getMessage()==null?"Unable to verify Torn account.":e.getMessage();runOnUiThread(()->showLogin(message));
            }
        },"TornFCA-Authenticate").start();
    }

    private void resolveBackendPermissionsAsync(String key,AuthSession base){
        if(base==null||!CompanionBackendClient.isConfigured())return;
        new Thread(()->{
            AuthSession resolved=CompanionBackendClient.resolvePermissions(base,key);
            if(resolved==null)return;session=resolved;
            runOnUiThread(()->{if(screen==Screen.HOME)showHome();});
        },"TornFCA-BackendPermissions").start();
    }

    private void showHome(){
        if(session==null){showLogin("Connect your Torn account to continue.");return;}screen=Screen.HOME;ScrollView scroll=shell();LinearLayout c=column(scroll);
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(18),dp(18),dp(18),dp(18));hero.setBackground(gradient(Color.rgb(34,49,69),Color.rgb(18,27,40),BORDER,20));hero.addView(text("Welcome back, "+session.playerName,22,TEXT,true));TextView meta=text(session.factionName+" • "+session.position+" • "+session.accessLabel(),13,MUTED,false);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);mp.topMargin=dp(5);hero.addView(meta,mp);
        if(AppRoles.isOwner(session)){TextView owner=text("OWNER / DEVELOPER",11,ACCENT2,true);owner.setPadding(dp(9),dp(5),dp(9),dp(5));owner.setBackground(rounded(BG2,ACCENT,11));LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);op.topMargin=dp(10);hero.addView(owner,op);}
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);Button refresh=secondary("↻ Refresh");refresh.setOnClickListener(v->{String key=keyStore.load();if(key!=null)authenticate(key,true);});actions.addView(refresh,new LinearLayout.LayoutParams(0,dp(44),1f));Button logout=secondary("Forget key");logout.setOnClickListener(v->{keyStore.clear();FactionScopeCache.clear(this);TornApiClient.clearMemoryCache();FactionMemberCache.clear();session=null;showLogin(null);});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(44),1f);lp.leftMargin=dp(8);actions.addView(logout,lp);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));ap.topMargin=dp(14);hero.addView(actions,ap);c.addView(hero);
        spacer(c,12);addWarBanner(c);spacer(c,6);c.addView(section("Faction companion"));spacer(c,8);
        LinearLayout banking=card("💰 Banking","Request faction payouts, review history and manage the shared queue when permitted.",BLUE);banking.setClickable(true);banking.setOnClickListener(v->showBanking());addCard(c,banking);
        if(session.hasPermission("Faction API Access"))addExactToolCard(c,"📦 Armory Auditor","Audit any faction armory item, member totals, deposits/restocks and detailed activity.","ARMORY","Faction API Access",BLUE);
        if(session.canManageAccess()){LinearLayout leadership=card("⚙️ Leadership Controls","Exact Torn permissions, listener guidance and faction administration.",ACCENT);leadership.setClickable(true);leadership.setOnClickListener(v->showLeadership());addCard(c,leadership);}
        if(AppRoles.isOwner(session)){spacer(c,4);c.addView(section("My tools"));spacer(c,8);addExactToolCard(c,"🏋️ Company Train Calculator","Private company-training payment calculator.","TRAIN","Owner / Developer",ACCENT);LinearLayout dev=card("🛠 Developer Console","Private tools, owner status and future per-player grants.",ACCENT);dev.setClickable(true);dev.setOnClickListener(v->showDeveloper());addCard(c,dev);}
        TextView footer=text("TornFCA v"+APP_VERSION+" • Torn permission model",11,MUTED,false);footer.setGravity(Gravity.CENTER_HORIZONTAL);c.addView(footer);setContentView(scroll);scroll.requestApplyInsets();
    }

    private void addWarBanner(LinearLayout parent){LinearLayout war=card("WAR STATUS","Checking Torn for ranked war status…",ACCENT);TextView headline=(TextView)war.getChildAt(0);TextView detail=war.getChildCount()>1&&war.getChildAt(1)instanceof TextView?(TextView)war.getChildAt(1):null;war.setClickable(true);war.setOnClickListener(v->openWarNotices());addCard(parent,war);String key=keyStore.load();if(key==null)return;new Thread(()->{try{WarStatus status=WarStatus.from(TornApiClient.getJson("/faction/wars",key),session.factionId);runOnUiThread(()->{long now=System.currentTimeMillis()/1000L;headline.setText(status.headline(now));headline.setTextColor(status.isLive(now)?GOOD:TEXT);if(detail!=null)detail.setText(status.detail(now)+" • Tap for notices");});}catch(Exception e){runOnUiThread(()->{headline.setText("War status unavailable");if(detail!=null)detail.setText("Tap to retry and open faction notices.");});}}).start();}
    private void openWarNotices(){if(session==null)return;Intent i=new Intent(this,WarNoticeActivity.class);i.putExtra(WarNoticeActivity.EXTRA_FACTION_ID,session.factionId);i.putExtra(WarNoticeActivity.EXTRA_FACTION_NAME,session.factionName);i.putExtra(WarNoticeActivity.EXTRA_CAN_PUBLISH,session.canPublishNotices());startActivity(i);}
    private void addExactToolCard(LinearLayout parent,String title,String body,String tool,String permission,int stroke){LinearLayout c=card(title,body,stroke);TextView access=text("Access: "+permission+" • Tap to open",11,stroke,true);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(10);c.addView(access,p);c.setClickable(true);c.setOnClickListener(v->openTool(tool));addCard(parent,c);}
    private void openTool(String tool){Intent i=new Intent(this,ToolHostActivity.class);i.putExtra(ToolHostActivity.EXTRA_TOOL,tool);startActivity(i);}

    private void showBanking(){if(CompanionBackendClient.isConfigured())loadSharedBanking();else showBankingContent(null,null,false);}

    private void loadSharedBanking(){
        screen=Screen.BANKING;ScrollView scroll=shell();LinearLayout c=column(scroll);addBack(c,"Banking");addCard(c,card("Banking Companion — Shared Queue","Loading faction banking requests and reconciling eligible retroactive chat requests…",BLUE));setContentView(scroll);scroll.requestApplyInsets();
        String key=keyStore.load();if(key==null){showLogin("Reconnect your Torn account to use banking.");return;}
        boolean reconcile=session!=null&&session.canManageBankingQueue();
        new Thread(()->{try{JSONObject response=CompanionBackendClient.getBankingRequests(key,reconcile);JSONArray rows=response.optJSONArray("requests");String reconcileError=response.optString("reconcile_error","");runOnUiThread(()->{if(screen==Screen.BANKING)showBankingContent(rows,reconcileError,true);});}catch(Exception e){String message=e.getMessage()==null?"Unable to load shared banking queue.":e.getMessage();runOnUiThread(()->{if(screen==Screen.BANKING)showBankingContent(null,message,true);});}}).start();
    }

    private void showBankingContent(JSONArray sharedRows,String sharedError,boolean backendMode){
        screen=Screen.BANKING;ScrollView scroll=shell();LinearLayout c=column(scroll);addBack(c,"Banking");
        String intro=backendMode?"Requests are shared across the faction. If the backend is temporarily unavailable, submissions are preserved locally on this phone.":"Shared backend deployment is still pending, so payout requests are being preserved locally on this phone.";
        addCard(c,card(backendMode?"Banking Companion — Shared Queue":"Banking Companion — Local Fallback",intro,BLUE));

        EditText amount=new EditText(this);amount.setHint("Amount requested — leave blank for full balance");amount.setHintTextColor(MUTED);amount.setTextColor(TEXT);amount.setSingleLine(true);amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);amount.setPadding(dp(12),0,dp(12),0);amount.setBackground(rounded(BG2,BORDER,11));c.addView(amount,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(52)));spacer(c,8);
        EditText note=new EditText(this);note.setHint("Optional note — e.g. war supplies");note.setHintTextColor(MUTED);note.setTextColor(TEXT);note.setSingleLine(true);note.setPadding(dp(12),0,dp(12),0);note.setBackground(rounded(BG2,BORDER,11));c.addView(note,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(52)));spacer(c,10);
        Button submit=primary(backendMode?"Submit Payout Request":"Save Payout Request Locally");
        submit.setOnClickListener(v->{String amountValue=amount.getText().toString().trim(),noteValue=note.getText().toString().trim();if(!backendMode){BankingDraftStore.add(this,session,amountValue,noteValue);Toast.makeText(this,"Request saved locally.",Toast.LENGTH_SHORT).show();showBankingContent(null,null,false);return;}String key=keyStore.load();if(key==null)return;submit.setEnabled(false);submit.setText("Submitting…");new Thread(()->{try{CompanionBackendClient.submitBankingRequest(key,amountValue,noteValue);runOnUiThread(()->{Toast.makeText(this,"Payout request submitted.",Toast.LENGTH_SHORT).show();loadSharedBanking();});}catch(Exception e){BankingDraftStore.add(this,session,amountValue,noteValue);String message=e.getMessage()==null?"Shared queue unavailable.":e.getMessage();runOnUiThread(()->{Toast.makeText(this,"Saved locally because the shared queue was unavailable.",Toast.LENGTH_LONG).show();showBankingContent(sharedRows,message,true);});}}).start();});
        c.addView(submit,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50)));spacer(c,14);

        if(session.canManageBankingQueue())addCard(c,card("Banking management permission verified","Your Torn rank includes Money Giving / Balance Adjustment, or you are Leader/Co-leader. Shared request controls use this exact permission check.",GOOD));
        if(sharedError!=null&&!sharedError.trim().isEmpty())addCard(c,card("Shared banking warning",sharedError.trim(),BAD));
        addCard(c,card("Retroactive reconciliation","Faction-chat requests can be checked against Torn faction balances. Retroactive requests found with less than $1,000,000 remaining are moved out of the pending queue as likely already handled; app-submitted requests are never auto-cleared.",ACCENT));

        if(backendMode){
            c.addView(section(session.canManageBankingQueue()?"Faction request queue":"Your shared request history"));spacer(c,8);
            if(sharedRows==null)addCard(c,card("Shared queue unavailable","New requests will fall back to local storage until the backend responds again.",BAD));
            else if(sharedRows.length()==0)addCard(c,card("No shared requests yet","New payout requests will appear here.",BORDER));
            else for(int i=0;i<sharedRows.length();i++){JSONObject row=sharedRows.optJSONObject(i);if(row!=null)addSharedBankingRow(c,row);}
        }

        JSONArray localRows=BankingDraftStore.all(this);
        if(!backendMode||localRows.length()>0){c.addView(section(backendMode?"Local fallback requests":"Requests saved on this phone"));spacer(c,8);if(localRows.length()==0)addCard(c,card("No local requests","Create a request above to test the banking workflow.",BORDER));else{for(int i=localRows.length()-1;i>=0;i--){JSONObject row=localRows.optJSONObject(i);if(row==null)continue;String when=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(row.optLong("created",0)));String body=row.optString("amount","FULL BALANCE")+" • "+when;String n=row.optString("note","");if(!n.isEmpty())body+="\n"+n;addCard(c,card("LOCAL FALLBACK",body,BORDER));}Button clear=secondary("Clear Local Fallback Requests");clear.setOnClickListener(v->{BankingDraftStore.clear(this);showBanking();});c.addView(clear,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46)));}}
        setContentView(scroll);scroll.requestApplyInsets();
    }

    private void addSharedBankingRow(LinearLayout parent,JSONObject row){
        String status=row.optString("status","PENDING");String requester=row.optString("requester_name","Member");long requestedAt=row.optLong("requested_at",row.optLong("detected_at",0));String when=requestedAt>0?DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(requestedAt*1000L)):"Unknown time";Object rawAmount=row.opt("requested_amount");String amount=(rawAmount==null||rawAmount==JSONObject.NULL||row.optString("request_mode","").equals("FULL_BALANCE"))?"FULL BALANCE":formatMoney(row.optLong("requested_amount",0));StringBuilder body=new StringBuilder(amount+" • "+when);String source=row.optString("source","");if(!source.isEmpty())body.append("\n").append(source.replace('_',' '));String note=row.optString("note","");if(!note.isEmpty())body.append("\n").append(note);String requestText=row.optString("request_text","");if(!requestText.isEmpty()){if(requestText.length()>180)requestText=requestText.substring(0,180)+"…";body.append("\n\"").append(requestText).append("\"");}
        LinearLayout item=card(status+(session.canManageBankingQueue()?" • "+requester:""),body.toString(),bankingStatusColor(status));
        if(session.canManageBankingQueue()&&(status.equals("PENDING")||status.equals("LIKELY_HANDLED"))){LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);Button first=primary(status.equals("LIKELY_HANDLED")?"Confirm Handled":"Mark Paid");String firstStatus=status.equals("LIKELY_HANDLED")?"HANDLED":"PAID";first.setOnClickListener(v->updateSharedBanking(row.optString("id",""),firstStatus));actions.addView(first,new LinearLayout.LayoutParams(0,dp(44),1f));Button second=secondary(status.equals("LIKELY_HANDLED")?"Reopen":"Handled");String secondStatus=status.equals("LIKELY_HANDLED")?"PENDING":"HANDLED";second.setOnClickListener(v->updateSharedBanking(row.optString("id",""),secondStatus));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(44),1f);sp.leftMargin=dp(8);actions.addView(second,sp);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));ap.topMargin=dp(12);item.addView(actions,ap);}
        addCard(parent,item);
    }

    private void updateSharedBanking(String requestId,String status){if(requestId==null||requestId.isEmpty())return;String key=keyStore.load();if(key==null)return;Toast.makeText(this,"Updating request…",Toast.LENGTH_SHORT).show();new Thread(()->{try{CompanionBackendClient.updateBankingRequest(key,requestId,status);runOnUiThread(this::loadSharedBanking);}catch(Exception e){String message=e.getMessage()==null?"Unable to update request.":e.getMessage();runOnUiThread(()->Toast.makeText(this,message,Toast.LENGTH_LONG).show());}}).start();}
    private int bankingStatusColor(String status){if("PAID".equals(status)||"HANDLED".equals(status))return GOOD;if("CANCELLED".equals(status))return BAD;if("LIKELY_HANDLED".equals(status))return BLUE;return ACCENT;}
    private String formatMoney(long amount){return String.format(Locale.US,"$%,d",amount);}

    private void showLeadership(){
        if(session==null||!session.canManageAccess()){showHome();return;}screen=Screen.LEADERSHIP;ScrollView scroll=shell();LinearLayout c=column(scroll);addBack(c,"Leadership Controls");LinearLayout war=card("War & leadership notices","Live Torn ranked-war countdown plus the faction message board. Notice publishing uses Announcement Changes permission.",ACCENT);war.setClickable(true);war.setOnClickListener(v->openWarNotices());addCard(c,war);addCard(c,card("💬 Faction Chat Listener","The listener recognizes banker/balance/withdrawal requests plus amount-specific requests such as “bank 25m”, retries failed sends and deduplicates successfully queued messages.",BLUE));addCard(c,card("Retroactive banking","Shared banking reconciliation checks retroactive chat requests against faction balances and flags sub-$1M balances as likely already handled.",GOOD));
        if(session.positions!=null&&session.positions.length()>0){c.addView(section("Exact Torn rank permissions"));spacer(c,8);for(int i=0;i<session.positions.length();i++){JSONObject pos=session.positions.optJSONObject(i);if(pos==null)continue;JSONArray abilities=pos.optJSONArray("abilities");StringBuilder body=new StringBuilder();if(abilities!=null)for(int j=0;j<abilities.length();j++){if(j>0)body.append(" • ");body.append(abilities.optString(j));}addCard(c,card(pos.optString("name","Position"),body.length()==0?"No explicit abilities":body.toString(),BORDER));}}
        addCard(c,card("Permission synchronization",CompanionBackendClient.isConfigured()?"Leadership rank permissions, notices and banking are connected to the shared backend.":"Rank permissions, faction notices, banking queue and listener ingestion are coded and ready; shared operation begins when the Companion backend deployment URL is connected.",BORDER));setContentView(scroll);scroll.requestApplyInsets();
    }

    private void showDeveloper(){if(!AppRoles.isOwner(session)){showHome();return;}screen=Screen.DEVELOPER;ScrollView scroll=shell();LinearLayout c=column(scroll);addBack(c,"Developer Console");addCard(c,card("Owner identity active",session.playerName+" ["+session.playerId+"] is recognized as Owner/Developer. The API key is not the developer credential.",ACCENT));addCard(c,card("Permission architecture","Feature access checks Torn ability names directly instead of treating Green/Orange/Red/Black as application roles.",GOOD));addCard(c,card("Shared banking foundation","Backend queue/history, listener ingestion, manager status controls and retroactive balance reconciliation retain local outage fallback.",BLUE));addCard(c,card("Multi-faction boundary","Faction ID is the tenant boundary across permission, notice and banking schemas; player ID remains the user identity.",BLUE));addCard(c,card("Release foundation","TornFCA v"+APP_VERSION+" remains on the permanent-signing track while the Play candidate is still under device validation.",BORDER));setContentView(scroll);scroll.requestApplyInsets();}
    private void addBack(LinearLayout c,String title){Button back=secondary("← Home");back.setOnClickListener(v->showHome());c.addView(back,new LinearLayout.LayoutParams(dp(104),dp(42)));TextView t=text(title,26,TEXT,true);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp(14);tp.bottomMargin=dp(14);c.addView(t,tp);}
    @Override public void onBackPressed(){if(screen!=Screen.HOME&&screen!=Screen.LOGIN)showHome();else super.onBackPressed();}
}
