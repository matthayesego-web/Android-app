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
        TornFcaUi.header(this,r,"More","Member Center","Start with My Day, then choose the section that matches what you want to do. Core faction-member tools stay free.");

        TornFcaUi.addSection(this,r,"Start here");
        addLaunch(r,"TODAY","My Day","See your bars, cooldowns, refills, OC, chain, current war and anything that may need your attention.",TornFcaUi.GREEN,MemberDailyActivity.class,null,"Open My Day");

        TornFcaUi.addSection(this,r,"Daily & readiness");
        addLaunch(r,"WAR PREP","My War Prep","Check your personal readiness and work through a checklist for the current or upcoming war.",TornFcaUi.GOLD,WarPrepActivity.class,null,"Open War Prep");
        addLaunch(r,"WAR","My War","See your current ranked-war participation and your recent completed wars.",TornFcaUi.RED,MemberWarActivity.class,null,"Open My War");
        addLaunch(r,"OC","My Organized Crime","See your own OC assignment and readiness. Faction-wide OC details are shown only to leaders who have access.",TornFcaUi.PURPLE,null,FeatureRouterActivity.TARGET_OC,"Open My OC");
        addLaunch(r,"CHAIN","Chain Status","See the current faction chain and your participation.",TornFcaUi.BLUE,null,FeatureRouterActivity.TARGET_CHAIN,"Open Chain Status");

        TornFcaUi.addSection(this,r,"Growth & training");
        addLaunch(r,"TRAINING","Training Center","Read TornFCA starter guides plus training rules and guides published by your current faction.",TornFcaUi.PURPLE,TrainingCenterActivity.class,null,"Open Training Center");
        addLaunch(r,"PROGRESS","My Training Progress","Track your own battle stats and Xanax use from a baseline saved only on this device for this faction.",TornFcaUi.GREEN,TrainingProgressActivity.class,null,"Open My Progress");

        TornFcaUi.addSection(this,r,"My faction");
        addLaunch(r,"RESOURCES","Faction Resources","New here? Find your faction's onboarding checklist, rules, guides and useful shortcuts.",TornFcaUi.GOLD,FactionResourcesActivity.class,null,"Open Faction Resources");
        addLaunch(r,"OVERVIEW","Faction Overview","See your faction's current status and information available to members.",TornFcaUi.GOLD,MemberFactionActivity.class,null,"Open Faction Overview");
        addLaunch(r,"DIRECTORY","Faction Directory","Search your faction roster and open basic member status cards.",TornFcaUi.BLUE,MemberDirectoryActivity.class,null,"Open Directory");

        TornFcaUi.addSection(this,r,"Community & alerts");
        addLaunch(r,"COMMUNITY","Faction Chat",CommunityBackendClient.isConfigured()?"Chat with verified members of your current faction inside TornFCA.":"Faction Chat is not available in this build yet.",TornFcaUi.BLUE,FactionChatActivity.class,null,"Open Faction Chat");
        addLaunch(r,"ALERTS","Notification Inbox","Review important TornFCA alerts after they leave Android's notification shade.",TornFcaUi.GOLD,NotificationInboxActivity.class,null,"Open Notification Inbox");

        TornFcaUi.addSection(this,r,"Optional upgrade");
        int player=currentPlayerId();boolean premium=PremiumAccess.has(this,player,PremiumAccess.PERSONAL_INSIGHTS);
        addLaunch(r,premium?"PREMIUM ACTIVE":"PREMIUM","Premium Insights",premium?"Your extra history and personal analytics are active.":"Optional extra history, analytics, automation and convenience. The core member tools above remain free.",TornFcaUi.GOLD,PremiumInsightsActivity.class,null,premium?"Open Premium Insights":"Preview Premium");

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
