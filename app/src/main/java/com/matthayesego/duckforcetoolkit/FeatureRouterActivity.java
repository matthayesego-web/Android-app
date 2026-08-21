package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FeatureRouterActivity extends Activity {
    public static final String EXTRA_TARGET = "target";
    public static final String TARGET_ACTIVITY = "ACTIVITY";
    public static final String TARGET_WAR = "WAR";
    public static final String TARGET_WAR_PAYOUT = "WAR_PAYOUT";
    public static final String TARGET_BANKING = "BANKING";
    public static final String TARGET_CHAIN = "CHAIN";
    public static final String TARGET_OC = "OC";
    public static final String TARGET_PULSE = "PULSE";
    public static final String TARGET_LOOKUP = "LOOKUP";
    public static final String TARGET_STRENGTH = "STRENGTH";
    public static final String TARGET_DEVELOPER = "DEVELOPER";

    private static final int BG=Color.rgb(8,12,18),PANEL=Color.rgb(20,27,38),BORDER=Color.rgb(49,63,81),TEXT=Color.rgb(245,248,252),MUTED=Color.rgb(151,163,179),GOLD=Color.rgb(243,184,52),BAD=Color.rgb(248,81,73);
    private SecureApiKeyStore keyStore;

    @Override protected void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);keyStore=new SecureApiKeyStore(this);DeveloperSettings.activityMaxPages(this);RemoteFeaturePolicy.refreshAsync(this);route();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    @SuppressWarnings("deprecation") private void showStatus(String message,int stroke){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);int l=dp(24),t=dp(24),r=dp(24),b=dp(24);root.setPadding(l,t,r,b);root.setBackgroundColor(BG);root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),b+i.getSystemWindowInsetBottom());return i;});LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(18),dp(18),dp(18),dp(18));card.setBackground(rounded(PANEL,stroke,18));TextView title=new TextView(this);title.setText("TornFCA");title.setTextColor(TEXT);title.setTextSize(20);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);card.addView(title);TextView body=new TextView(this);body.setText(message);body.setTextColor(MUTED);body.setTextSize(13);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);bp.topMargin=dp(7);card.addView(body,bp);root.addView(card,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));setContentView(root);root.requestApplyInsets();}

    private void route(){
        final String target=getIntent().getStringExtra(EXTRA_TARGET);final String key=keyStore.load();
        if(!isKnownTarget(target)){showStatus("This TornFCA feature route is unavailable.",BAD);return;}
        if(key==null||key.trim().isEmpty()){showStatus("No active Torn API key is available. Return to TornFCA and sign in again.",BAD);return;}
        if(!TARGET_DEVELOPER.equals(target)&&blockedByPolicy(target))return;

        final boolean freshFaction=requiresLeadership(target);
        if(!TARGET_DEVELOPER.equals(target)&&!freshFaction){
            FactionScopeCache.Scope cached=FactionScopeCache.load(this,key);
            if(cached!=null){launchFeature(target,cached.factionId,cached.factionName,cached.position,cached.factionApiAccess);return;}
            AuthSession hot=TornApiClient.cachedSession(key);
            if(hot!=null){FactionScopeCache.save(this,key,hot);launchFeature(target,hot.factionId,hot.factionName,hot.position,hot.factionApiAccess);return;}
        }
        showStatus(freshFaction?"Re-verifying current faction leadership…":"Verifying faction scope…",GOLD);
        new Thread(()->{try{AuthSession session=freshFaction?TornApiClient.authenticateFreshFaction(key):TornApiClient.authenticate(key);FactionScopeCache.save(this,key,session);runOnUiThread(()->launchVerified(target,session));}catch(Exception e){String message=e.getMessage()==null?"Unable to verify faction scope.":e.getMessage();runOnUiThread(()->showStatus(message,BAD));}}).start();
    }

    private boolean blockedByPolicy(String target){
        if(RemoteFeaturePolicy.versionBlocked(this)){showStatus("This TornFCA build is below the minimum supported version. Update TornFCA before using this tool.",BAD);return true;}
        if(RemoteFeaturePolicy.maintenanceMode(this)){String message=RemoteFeaturePolicy.betaMessage(this);showStatus(message.isEmpty()?"TornFCA shared tools are temporarily in maintenance mode.":message,BAD);return true;}
        String feature=policyFeature(target);
        if(feature!=null&&!DeveloperSettings.featureEnabled(this,feature)){showStatus("This TornFCA tool is disabled in the current developer test configuration.",BAD);return true;}
        if(feature!=null&&RemoteFeaturePolicy.featureDisabled(this,feature)){String message=RemoteFeaturePolicy.betaMessage(this);showStatus(message.isEmpty()?"This TornFCA tool is temporarily unavailable.":message,BAD);return true;}
        return false;
    }

    private String policyFeature(String target){
        if(TARGET_ACTIVITY.equals(target))return DeveloperSettings.FEATURE_ACTIVITY;
        if(TARGET_WAR.equals(target)||TARGET_WAR_PAYOUT.equals(target))return DeveloperSettings.FEATURE_WAR;
        if(TARGET_CHAIN.equals(target))return DeveloperSettings.FEATURE_CHAIN;
        if(TARGET_OC.equals(target))return DeveloperSettings.FEATURE_OC;
        if(TARGET_PULSE.equals(target))return DeveloperSettings.FEATURE_PULSE;
        if(TARGET_LOOKUP.equals(target))return DeveloperSettings.FEATURE_LOOKUP;
        return null;
    }

    private String premiumFeature(String target){
        if(TARGET_PULSE.equals(target))return PremiumAccess.FACTION_PULSE;
        if(TARGET_LOOKUP.equals(target))return PremiumAccess.MEMBER_DOSSIER;
        return null;
    }

    private void launchVerified(String target,AuthSession session){if(TARGET_DEVELOPER.equals(target)){Intent i=new Intent(this,DeveloperGateActivity.class);putScope(i,session.factionId,session.factionName,session.position,session.factionApiAccess);startActivity(i);finish();return;}launchFeature(target,session.factionId,session.factionName,session.position,session.factionApiAccess);}
    private void launchFeature(String target,int factionId,String factionName,String position,boolean factionApiAccess){
        String effectivePosition=DeveloperPreviewStore.isMemberPreview(this)?"Member":position;
        if(requiresLeadership(target)&&!AccessPolicy.isLeaderPosition(effectivePosition)){showStatus("This tool is restricted to faction leadership. Member Preview uses the same access boundary as a normal member.",BAD);return;}
        String premium=premiumFeature(target);
        if(premium!=null){int playerId=PremiumAccess.currentPlayerId(this);PremiumAccess.refresh(this,playerId);if(!PremiumAccess.has(this,playerId,premium)){showStatus("This advanced convenience tool is included with TornFCA Premium. Free members keep the core faction, war, participation, WarPay, banking and provider-data tools.",GOLD);return;}}
        if(TARGET_WAR_PAYOUT.equals(target))WarPayoutReceiptStore.refreshFromBackendAsync(this);
        Intent i;if(TARGET_WAR.equals(target)){i=new Intent(this,WarHubActivity.class);}else if(TARGET_WAR_PAYOUT.equals(target)){i=new Intent(this,WarPayoutActivity.class);}else if(TARGET_BANKING.equals(target)){i=new Intent(this,BankingCompanionActivity.class);}else if(TARGET_LOOKUP.equals(target)){i=new Intent(this,MemberDossierActivity.class);}else if(TARGET_STRENGTH.equals(target)){i=new Intent(this,FactionStrengthActivity.class);}else if(TARGET_OC.equals(target)){i=new Intent(this,OcTrackerActivity.class);}else if(TARGET_PULSE.equals(target)){i=new Intent(this,QuickIntelActivity.class);i.putExtra(QuickIntelActivity.EXTRA_MODE,QuickIntelActivity.MODE_PULSE);}else{String mode;if(TARGET_CHAIN.equals(target))mode=FactionOpsActivity.MODE_CHAIN;else mode=FactionOpsActivity.MODE_ACTIVITY;i=new Intent(this,FactionOpsActivity.class);i.putExtra(FactionOpsActivity.EXTRA_MODE,mode);}putScope(i,factionId,factionName,effectivePosition,factionApiAccess);startActivity(i);finish();}
    private boolean requiresLeadership(String target){return TARGET_ACTIVITY.equals(target)||TARGET_PULSE.equals(target)||TARGET_LOOKUP.equals(target)||TARGET_WAR_PAYOUT.equals(target);}
    private boolean isKnownTarget(String target){return TARGET_ACTIVITY.equals(target)||TARGET_WAR.equals(target)||TARGET_WAR_PAYOUT.equals(target)||TARGET_BANKING.equals(target)||TARGET_CHAIN.equals(target)||TARGET_OC.equals(target)||TARGET_PULSE.equals(target)||TARGET_LOOKUP.equals(target)||TARGET_STRENGTH.equals(target)||TARGET_DEVELOPER.equals(target);}
    private void putScope(Intent i,int factionId,String factionName,String position,boolean factionApiAccess){i.putExtra(FactionOpsActivity.EXTRA_FACTION_ID,factionId);i.putExtra(FactionOpsActivity.EXTRA_FACTION_NAME,factionName);i.putExtra(FactionOpsActivity.EXTRA_FACTION_API,factionApiAccess);i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_ID,factionId);i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_NAME,factionName);i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_API,factionApiAccess);i.putExtra(DeveloperConsoleActivity.EXTRA_POSITION,position);}
}
