package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/** Everyday member launchpad. Leadership is additive; this screen is useful to every faction member. */
public class MemberCenterActivity extends Activity {
    @Override protected void onCreate(Bundle b){super.onCreate(b);PushNotifications.syncIfReady(this);int player=currentPlayerId();if(player>0)PremiumAccess.refresh(this,player);render();}
    @Override protected void onResume(){super.onResume();render();}

    private void render(){
        ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);
        TornFcaUi.header(this,r,"More","Member Center","Start with My Day, then use the section that matches what you are doing. Core faction-member tools stay free.");

        TornFcaUi.addSection(this,r,"Start here");
        addLaunch(r,"TODAY","My Day","One-screen summary of bars, cooldowns, refills, your OC, chain, current war and what may need attention.",TornFcaUi.GREEN,MemberDailyActivity.class,null,"Open My Day");

        TornFcaUi.addSection(this,r,"Daily & readiness");
        addLaunch(r,"WAR PREP","My War Prep","Check your personal readiness and a war-specific checklist before fighting starts.",TornFcaUi.GOLD,WarPrepActivity.class,null,"Open War Prep");
        addLaunch(r,"WAR","My War","See your current ranked-war participation and recent completed-war history.",TornFcaUi.RED,MemberWarActivity.class,null,"Open My War");
        addLaunch(r,"OC","My Organized Crime","See your own OC assignment and readiness. Leadership-wide OC information remains permission-gated.",TornFcaUi.PURPLE,null,FeatureRouterActivity.TARGET_OC,"Open My OC");
        addLaunch(r,"CHAIN","Chain Status","See the current faction chain and your participation context.",TornFcaUi.BLUE,null,FeatureRouterActivity.TARGET_CHAIN,"Open Chain Status");

        TornFcaUi.addSection(this,r,"Growth & training");
        addLaunch(r,"TRAINING","Training Center","Read TornFCA starter guides plus your faction's current training rules and private guide library.",TornFcaUi.PURPLE,TrainingCenterActivity.class,null,"Open Training Center");
        addLaunch(r,"PROGRESS","My Training Progress","Track your own battle-stat and Xanax progress from a private faction-specific baseline.",TornFcaUi.GREEN,TrainingProgressActivity.class,null,"Open My Progress");

        TornFcaUi.addSection(this,r,"My faction");
        addLaunch(r,"RESOURCES","Faction Resources","Start here when joining a faction: onboarding checklist, local rules, guides and useful shortcuts.",TornFcaUi.GOLD,FactionResourcesActivity.class,null,"Open Faction Resources");
        addLaunch(r,"OVERVIEW","Faction Overview","Member-safe faction status and current faction information.",TornFcaUi.GOLD,MemberFactionActivity.class,null,"Open Faction Overview");
        addLaunch(r,"DIRECTORY","Faction Directory","Search the current roster and open member-safe status cards without leadership analytics.",TornFcaUi.BLUE,MemberDirectoryActivity.class,null,"Open Directory");

        TornFcaUi.addSection(this,r,"Community & alerts");
        addLaunch(r,"COMMUNITY","Faction Chat",CommunityBackendClient.isConfigured()?"Chat with authenticated members of your current faction inside TornFCA.":"Faction chat is installed; the shared community connection is not configured in this build.",TornFcaUi.BLUE,FactionChatActivity.class,null,"Open Faction Chat");
        addLaunch(r,"ALERTS","Notification Inbox","Review important TornFCA alerts after they leave Android's notification shade.",TornFcaUi.GOLD,NotificationInboxActivity.class,null,"Open Notification Inbox");

        TornFcaUi.addSection(this,r,"Optional upgrade");
        int player=currentPlayerId();boolean premium=PremiumAccess.has(this,player,PremiumAccess.PERSONAL_INSIGHTS);
        addLaunch(r,premium?"PREMIUM ACTIVE":"PREMIUM","Premium Insights",premium?"Your deeper personal history and analytics are active.":"Optional deeper history, analytics, automation and convenience. The core member tools above remain free.",TornFcaUi.GOLD,PremiumInsightsActivity.class,null,premium?"Open Premium Insights":"Preview Premium");

        setContentView(s);s.requestApplyInsets();
    }

    private void addLaunch(LinearLayout r,String eye,String title,String body,int accent,Class<?> activity,String feature,String action){
        LinearLayout c=TornFcaUi.card(this,eye,title,body,accent);
        Runnable open=()->{if(activity!=null)startActivity(new Intent(this,activity));else{Intent i=new Intent(this,FeatureRouterActivity.class);i.putExtra(FeatureRouterActivity.EXTRA_TARGET,feature);startActivity(i);}};
        c.setClickable(true);c.setFocusable(true);c.setOnClickListener(v->open.run());
        Button b=TornFcaUi.button(this,action,accent);b.setOnClickListener(v->open.run());
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));p.topMargin=TornFcaUi.dp(this,10);c.addView(b,p);TornFcaUi.add(this,r,c);
    }

    private int currentPlayerId(){String key=new SecureApiKeyStore(this).load();if(key==null)return 0;AuthSession hot=TornApiClient.cachedSession(key);if(hot!=null)return hot.playerId;FactionScopeCache.Scope scope=FactionScopeCache.load(this,key);return scope==null?0:scope.playerId;}
}
