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
    @Override protected void onCreate(Bundle b){super.onCreate(b);int player=currentPlayerId();if(player>0)PremiumAccess.refresh(this,player);render();}
    @Override protected void onResume(){super.onResume();render();}
    private void render(){
        ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);
        TornFcaUi.header(this,r,"More","TornFCA Premium","Free stays useful. Premium is the convenience layer that puts deeper history and all-in-one intelligence in one place.");
        int player=currentPlayerId();boolean premium=PremiumAccess.has(this,player,PremiumAccess.PERSONAL_INSIGHTS);String state=premium?"PREMIUM ACTIVE":"FREE";String detail=premium?PremiumEntitlementStore.summary(this):(PremiumBackendClient.isConfigured()?"Your account currently has the complete free core.":"You have the complete free core. Premium billing is not enabled in this beta build.");
        TornFcaUi.add(this,r,TornFcaUi.card(this,"YOUR PLAN",state,detail,premium?TornFcaUi.GOLD:TornFcaUi.GREEN));

        TornFcaUi.addSection(this,r,"Free — complete core");
        TornFcaUi.add(this,r,TornFcaUi.card(this,"MEMBER CORE","Included for every member","My Day • War Prep • Ranked War and Territories • personal OC • chain • Training Center and personal progress • faction overview, directory and resources • faction chat • Notification Inbox and standard alerts.",TornFcaUi.GREEN));
        TornFcaUi.add(this,r,TornFcaUi.card(this,"LEADERSHIP CORE","Permissions, not a paywall","Authorized leaders keep the 7-day Activity Tracker, basic attention list, WarPay calculator, banking workflow, Armory Auditor, training management and other essential faction administration without Premium.",TornFcaUi.GREEN));
        TornFcaUi.add(this,r,TornFcaUi.card(this,"PROVIDER DATA","No double charge","Basic FFScouter and TornStats access stays free inside TornFCA when you separately opt in and your provider account/key is entitled to that data. TornFCA Premium sells workflow and convenience around it—not the provider data itself.",TornFcaUi.BLUE));

        TornFcaUi.addSection(this,r,"Premium — convenience & depth");
        LinearLayout insights=TornFcaUi.card(this,"PERSONAL INSIGHTS","Your 30-day trends","Personal outgoing activity, ranked-war participation, recent faction results and local WarPay receipt analytics in one view.",TornFcaUi.GOLD);Button open=TornFcaUi.button(this,premium?"Open Premium Insights":"Preview Premium Insights",TornFcaUi.GOLD);open.setOnClickListener(v->startActivity(new Intent(this,PremiumInsightsActivity.class)));LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));op.topMargin=TornFcaUi.dp(this,9);insights.addView(open,op);TornFcaUi.add(this,r,insights);
        TornFcaUi.add(this,r,TornFcaUi.card(this,"EXTENDED ACTIVITY","30-day faction review","Free leadership keeps the useful 7-day faction-log view. Premium extends the same tracker to a 30-day history for deeper participation review without making the core tracker paid-only.",TornFcaUi.GOLD));
        TornFcaUi.add(this,r,TornFcaUi.card(this,"FACTION PULSE","One-screen leadership snapshot","Premium combines current availability, online state, hospital/jail/travel, OC gaps and territory-wall status into a fast command snapshot. The underlying roster/status data remains available through free tools.",TornFcaUi.GOLD));
        TornFcaUi.add(this,r,TornFcaUi.card(this,"MEMBER DOSSIER","All-in-one member intelligence","Premium combines Torn member status with separately opted-in FFScouter and TornStats intelligence in one dossier. Free members and leaders keep the Directory, basic status and raw provider tools.",TornFcaUi.GOLD));
        TornFcaUi.add(this,r,TornFcaUi.card(this,"SMART ALERTS","More control over your attention","Free members keep the standard notification categories and 15-minute war reminder. Premium adds selectable 15/30/60-minute war lead times and future automation.",TornFcaUi.GOLD));
        TornFcaUi.add(this,r,TornFcaUi.card(this,"FUTURE CONVENIENCE","Presets, exports & saved views","Future Premium work can add saved scouting views, extended WarPay/armory history, report exports, presets and dashboard personalization while leaving the basic calculators, audits and faction administration free.",TornFcaUi.GOLD));
        TornFcaUi.add(this,r,TornFcaUi.card(this,"ENTITLEMENT","Account-based","Premium follows your Torn player ID and is verified server-side. Developer simulation is owner-only, defaults off and is never stored as a real entitlement.",TornFcaUi.BLUE));
        setContentView(s);s.requestApplyInsets();
    }
    private int currentPlayerId(){return PremiumAccess.currentPlayerId(this);}
}
