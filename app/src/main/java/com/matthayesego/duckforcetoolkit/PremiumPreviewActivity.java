package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/** Player-facing plan screen. Entitlements are backend-verified when the premium backend is configured. */
public class PremiumPreviewActivity extends Activity {
    @Override protected void onCreate(Bundle b){super.onCreate(b);render();refresh(false);}
    @Override protected void onResume(){super.onResume();render();}

    private void refresh(boolean force){int player=currentPlayerId();if(player<=0)return;PremiumBackendClient.refreshAsync(this,player,force,()->{if(!isFinishing())render();});}

    private void render(){
        ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);
        TornFcaUi.header(this,r,"More","TornFCA Premium","Your plan, expiration and Premium activation details in one place.");
        int player=currentPlayerId();boolean premium=PremiumAccess.has(this,player,PremiumAccess.PERSONAL_INSIGHTS),verifiedPremium=PremiumEntitlementStore.hasPremium(this,player),leader=currentLeader();
        String state=premium?(verifiedPremium?"PREMIUM":"PREMIUM PREVIEW"):"FREE";
        String detail;
        if(verifiedPremium)detail=PremiumEntitlementStore.expirySummary(this,player)+"\n"+PremiumEntitlementStore.sourceLabel(this,player);
        else if(premium)detail="Developer Premium preview is active on this device. This does not create or extend a paid entitlement.";
        else detail="Complete free core active. Premium is optional and adds deeper history, pacing and convenience.";
        LinearLayout plan=TornFcaUi.card(this,"ACCOUNT TIER",state,detail,premium?TornFcaUi.GOLD:TornFcaUi.BLUE);
        Button refresh=TornFcaUi.button(this,"Refresh Premium Status",premium?TornFcaUi.GOLD:TornFcaUi.BLUE);refresh.setOnClickListener(v->refresh(true));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,44));rp.topMargin=TornFcaUi.dp(this,9);plan.addView(refresh,rp);TornFcaUi.add(this,r,plan);

        addActivationCard(r,premium,verifiedPremium);

        TornFcaUi.addSection(this,r,"Free — complete core");
        TornFcaUi.add(this,r,TornFcaUi.card(this,"MEMBER CORE","Faction life stays free","My Day • War Prep • Ranked War and Territories • personal OC • chain • Training Center and baseline progress • faction overview, directory and resources • faction chat • Notification Inbox and standard alerts.",TornFcaUi.GREEN));
        TornFcaUi.add(this,r,TornFcaUi.card(this,"LEADERSHIP CORE","Authority is never sold","Authorized leaders keep the 7-day Activity Tracker, basic attention list, WarPay calculator and current receipts, banking workflow, current Armory Auditor, training management and essential faction administration without Premium.",TornFcaUi.GREEN));
        TornFcaUi.add(this,r,TornFcaUi.card(this,"PROVIDER DATA","No double charge","Basic FFScouter and TornStats access stays free inside TornFCA when you separately opt in and your provider account/key is entitled to that data. Premium sells TornFCA workflow around it—not access to someone else's service.",TornFcaUi.BLUE));

        TornFcaUi.addSection(this,r,"Premium for every player");
        LinearLayout insights=TornFcaUi.card(this,"PERSONAL INSIGHTS","7-day pace + 30-day picture","See recent outgoing activity, ranked-war participation, active combat days, recent faction results and your local WarPay receipt totals together. Premium compares your last 7 days with the previous 23-day pace so the history tells you something useful.",TornFcaUi.GOLD);Button open=TornFcaUi.button(this,premium?"Open Premium Insights":"Preview Premium Insights",TornFcaUi.GOLD);open.setOnClickListener(v->startActivity(new Intent(this,PremiumInsightsActivity.class)));LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));op.topMargin=TornFcaUi.dp(this,9);insights.addView(open,op);TornFcaUi.add(this,r,insights);

        LinearLayout goals=TornFcaUi.card(this,"TRAINING GOAL PACING","Turn stat gains into a target","Set a private total battle-stat goal inside My Training Progress. Premium shows current progress, stats remaining, your gain/day from the existing baseline and an estimated time-to-goal—without adding another Torn API request.",TornFcaUi.GOLD);Button training=TornFcaUi.button(this,"Open Training Progress",TornFcaUi.GOLD);training.setOnClickListener(v->startActivity(new Intent(this,TrainingProgressActivity.class)));LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));gp.topMargin=TornFcaUi.dp(this,9);goals.addView(training,gp);TornFcaUi.add(this,r,goals);

        TornFcaUi.add(this,r,TornFcaUi.card(this,"SMART ALERTS","Choose when TornFCA gets your attention","Free keeps standard notification categories and the normal 15-minute war reminder. Premium lets you select 15/30/60-minute war lead times so alerts fit how you actually prepare.",TornFcaUi.GOLD));

        TornFcaUi.addSection(this,r,"Premium when you lead");
        LinearLayout activity=TornFcaUi.card(this,"ACTIVITY TRENDS","30-day participation momentum","Free leadership keeps the useful 7-day Activity Tracker. Premium adds a 30-day leadership analysis that compares each member's recent 7-day pace with the previous 23 days and surfaces improving or declining participation.",TornFcaUi.GOLD);if(leader){Button trends=TornFcaUi.button(this,premium?"Open Activity Trends":"Preview Activity Trends",TornFcaUi.GOLD);trends.setOnClickListener(v->startActivity(new Intent(this,PremiumActivityTrendsActivity.class)));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));ap.topMargin=TornFcaUi.dp(this,9);activity.addView(trends,ap);}TornFcaUi.add(this,r,activity);
        TornFcaUi.add(this,r,TornFcaUi.card(this,"FACTION PULSE","One-screen command snapshot","Premium combines current availability, online state, hospital/jail/travel, OC gaps and territory-wall status into a fast leadership snapshot. The underlying roster/status information still exists in free tools.",TornFcaUi.GOLD));
        TornFcaUi.add(this,r,TornFcaUi.card(this,"MEMBER DOSSIER","All-in-one member intelligence","Premium combines Torn member status with separately opted-in FFScouter and TornStats intelligence in one dossier. Free keeps the Directory, basic status and raw provider tools.",TornFcaUi.GOLD));

        TornFcaUi.addSection(this,r,"Premium roadmap — adds, never removals");
        TornFcaUi.add(this,r,TornFcaUi.card(this,"NEXT DEPTH","More history, presets & reports","Premium is designed to grow with saved WarPay models, longer audit/banking history, exports, saved scouting views, advanced comparisons and personalization. Those additions must sit on top of the existing free calculators, audits and faction administration rather than replacing them.",TornFcaUi.BLUE));
        TornFcaUi.add(this,r,TornFcaUi.card(this,"ENTITLEMENT","Follows your Torn account","Premium is verified server-side against your numeric Torn player ID, so it follows you between factions. Complimentary Premium uses the same entitlement system as normal Premium; faction permissions remain completely separate.",TornFcaUi.BLUE));
        setContentView(s);s.requestApplyInsets();
    }

    private void addActivationCard(LinearLayout root,boolean premium,boolean verifiedPremium){
        int days=PremiumBackendClient.daysPerXanax(this),recipient=PremiumBackendClient.paymentPlayerId(this);String required=PremiumBackendClient.requiredMessage(this);boolean open=PremiumBackendClient.activationsOpen(this),offerVerified=PremiumBackendClient.offerVerified(this);
        String eyebrow=open?(premium?"EXTEND PREMIUM":"UNLOCK PREMIUM"):"PREMIUM ACTIVATION";
        String title="1 Xanax = "+days+" Premium days";
        StringBuilder body=new StringBuilder();
        if(open)body.append(premium?"To extend your Premium time":"To activate Premium").append(", send Xanax in Torn to player ID ").append(recipient).append(" and include \"").append(required).append("\" in the item-send message. TornFCA verifies the transfer server-side and updates the entitlement tied to your numeric Torn player ID.");
        else if(offerVerified)body.append("The Premium server currently reports paid activations as closed. When activations are opened, send Xanax in Torn to player ID ").append(recipient).append(" with \"").append(required).append("\" in the item-send message. The launch rate is ").append(days).append(" days per Xanax.");
        else body.append("Launch instructions: send Xanax in Torn to player ID ").append(recipient).append(" with \"").append(required).append("\" in the item-send message. Launch rate: ").append(days).append(" Premium days per Xanax. The app will confirm whether activations are open after the Premium backend is updated.");
        if(verifiedPremium)body.append("\nCurrent entitlement: ").append(PremiumEntitlementStore.expirySummary(this,currentPlayerId())).append(".");
        TornFcaUi.add(this,root,TornFcaUi.card(this,eyebrow,title,body.toString(),open?TornFcaUi.GOLD:TornFcaUi.BLUE));
    }

    private int currentPlayerId(){return PremiumAccess.currentPlayerId(this);}
    private boolean currentLeader(){if(DeveloperPreviewStore.isMemberPreview(this))return false;String key=new SecureApiKeyStore(this).load();if(key==null||key.isBlank())return false;AuthSession hot=TornApiClient.cachedSession(key);if(hot!=null)return AccessPolicy.isLeaderPosition(hot.position);FactionScopeCache.Scope scope=FactionScopeCache.load(this,key);return scope!=null&&AccessPolicy.isLeaderPosition(scope.position);}
}
