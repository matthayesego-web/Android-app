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
    @Override protected void onCreate(Bundle b){super.onCreate(b);PushNotifications.syncIfReady(this);render();}
    private void render(){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"More","Member Center","Your daily faction tools, personal participation and faction community in one place.");addLaunch(r,"TODAY","My Day","Bars, cooldowns, refills, personal OC, chain, current war, faction standing and what to do next.",TornFcaUi.GREEN,MemberDailyActivity.class,null);addLaunch(r,"WAR","My War","Current personal ranked-war participation plus your recent completed-war history.",TornFcaUi.RED,MemberWarActivity.class,null);addLaunch(r,"OC","My Organized Crime","Your personal OC assignment/readiness view. Leadership-wide OC data remains permission-gated.",TornFcaUi.PURPLE,null,FeatureRouterActivity.TARGET_OC);addLaunch(r,"CHAIN","Chain Status","Current faction chain status and participation context.",TornFcaUi.BLUE,null,FeatureRouterActivity.TARGET_CHAIN);addLaunch(r,"COMMUNITY","Faction Chat",CommunityBackendClient.isConfigured()?"Chat with authenticated members of your faction inside TornFCA.":"Faction chat client is installed; community backend connection is pending.",TornFcaUi.BLUE,FactionChatActivity.class,null);addLaunch(r,"ALERTS","Notification Inbox","Keep important TornFCA alerts available after they leave Android's notification shade.",TornFcaUi.GOLD,NotificationInboxActivity.class,null);setContentView(s);s.requestApplyInsets();}
    private void addLaunch(LinearLayout r,String eye,String title,String body,int accent,Class<?> activity,String feature){LinearLayout c=TornFcaUi.card(this,eye,title,body,accent);Button b=TornFcaUi.button(this,"Open "+title,accent);b.setOnClickListener(v->{if(activity!=null)startActivity(new Intent(this,activity));else{Intent i=new Intent(this,FeatureRouterActivity.class);i.putExtra(FeatureRouterActivity.EXTRA_TARGET,feature);startActivity(i);}});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));p.topMargin=TornFcaUi.dp(this,10);c.addView(b,p);TornFcaUi.add(this,r,c);}
}
