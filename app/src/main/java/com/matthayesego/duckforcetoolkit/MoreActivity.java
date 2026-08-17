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
    private static final String ACCOUNT_ACTION_LABEL="Log Out / Change API Key";
    @Override protected void onCreate(Bundle b){super.onCreate(b);PushNotifications.syncIfReady(this);render();}
    private void render(){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Companion","More","Member tools, community, settings, legal information and app details.");addLaunch(r,"MEMBER","Member Center","Training, faction resources, war prep, OC, chain, community and other everyday member tools.",TornFcaUi.GREEN,MemberCenterActivity.class,"Open Member Center");addLaunch(r,"COMMUNITY","Faction Chat",CommunityBackendClient.isConfigured()?"Chat with verified members of your current faction.":"Faction Chat is not available in this build yet.",TornFcaUi.BLUE,FactionChatActivity.class,"Open Faction Chat");addLaunch(r,"SETTINGS","Settings","Notifications, API-key storage, optional services, privacy and account controls.",TornFcaUi.BLUE,SettingsActivity.class,"Open Settings");addLaunch(r,"ALERTS","Notification Inbox",NotificationInboxStore.count(this)+" saved notification"+(NotificationInboxStore.count(this)==1?"":"s")+" on this device.",TornFcaUi.GOLD,NotificationInboxActivity.class,"Open Inbox");addLaunch(r,"LEGAL","Legal & Privacy","Review the current Privacy Policy, Terms & Conditions, EULA and acknowledgement status.",TornFcaUi.PURPLE,LegalActivity.class,"Review Legal Documents");addLaunch(r,"PLAN","TornFCA Premium","See what stays free and what optional Premium features add.",TornFcaUi.GOLD,PremiumPreviewActivity.class,"View Plan");addLaunch(r,"ABOUT","About TornFCA","Version, purpose, API-key privacy, legal documents and optional service information.",TornFcaUi.BORDER,AboutActivity.class,"About TornFCA");LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);fp.topMargin=TornFcaUi.dp(this,6);r.addView(TornFcaUi.footer(this,"TornFCA v"+TornFcaBrand.VERSION),fp);setContentView(s);s.requestApplyInsets();}
    private void addLaunch(LinearLayout r,String eye,String title,String body,int accent,Class<?> target,String action){LinearLayout c=TornFcaUi.card(this,eye,title,body,accent);c.setClickable(true);c.setFocusable(true);c.setOnClickListener(v->startActivity(new Intent(this,target)));Button b=TornFcaUi.button(this,action,accent);b.setOnClickListener(v->startActivity(new Intent(this,target)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,44));p.topMargin=TornFcaUi.dp(this,9);c.addView(b,p);TornFcaUi.add(this,r,c);}
    private void logout(){PushNotifications.unregisterAsync(this);new SecureApiKeyStore(this).clear();FactionScopeCache.clear(this);TornApiClient.clearMemoryCache();FactionMemberCache.clear();DeveloperPreviewStore.clear(this);PremiumEntitlementStore.clear(this);NotificationInboxStore.clear(this);Intent i=new Intent(this,AccessGateActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);startActivity(i);finish();}
}
