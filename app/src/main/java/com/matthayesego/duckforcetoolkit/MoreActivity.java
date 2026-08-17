package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/** Clean secondary navigation hub for member tools, settings, plan and app information. */
public class MoreActivity extends Activity {
    @Override protected void onCreate(Bundle b){super.onCreate(b);render();}
    private void render(){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Companion","More","Member tools, settings, notifications and app information.");addLaunch(r,"MEMBER","Member Center","My Day, My War, personal OC, chain status and notification history.",TornFcaUi.GREEN,MemberCenterActivity.class,"Open Member Center");addLaunch(r,"SETTINGS","Settings","Notification categories, API-key retention, integrations, privacy and account controls.",TornFcaUi.BLUE,SettingsActivity.class,"Open Settings");addLaunch(r,"ALERTS","Notification Inbox",NotificationInboxStore.count(this)+" saved notification"+(NotificationInboxStore.count(this)==1?"":"s")+" on this device.",TornFcaUi.GOLD,NotificationInboxActivity.class,"Open Inbox");addLaunch(r,"PLAN","TornFCA Premium","See what stays free, what Premium adds and your current entitlement state.",TornFcaUi.GOLD,PremiumPreviewActivity.class,"View Plan");addLaunch(r,"ABOUT","About TornFCA","Version, purpose, privacy summary and third-party service information.",TornFcaUi.BORDER,AboutActivity.class,"About TornFCA");LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);fp.topMargin=TornFcaUi.dp(this,6);r.addView(TornFcaUi.footer(this,"TornFCA v"+TornFcaBrand.VERSION),fp);setContentView(s);s.requestApplyInsets();}
    private void addLaunch(LinearLayout r,String eye,String title,String body,int accent,Class<?> target,String action){LinearLayout c=TornFcaUi.card(this,eye,title,body,accent);c.setClickable(true);c.setFocusable(true);c.setOnClickListener(v->startActivity(new Intent(this,target)));Button b=TornFcaUi.button(this,action,accent);b.setOnClickListener(v->startActivity(new Intent(this,target)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,44));p.topMargin=TornFcaUi.dp(this,9);c.addView(b,p);TornFcaUi.add(this,r,c);}
}
