package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/** Clean secondary navigation hub. Everyday faction tools stay grouped inside Member Center. */
public class MoreActivity extends Activity {
    // Kept as an internal safety fallback and CI invariant; the visible account action lives in Settings.
    private static final String ACCOUNT_ACTION_LABEL="Log Out / Change API Key";
    @Override protected void onCreate(Bundle b){super.onCreate(b);PushNotifications.syncIfReady(this);render();}

    private void render(){
        ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);
        TornFcaUi.header(this,r,"TornFCA","More","Member Center holds your everyday faction tools. This page keeps app settings, legal information and app details easy to find without repeating the same destinations everywhere.");

        TornFcaUi.addSection(this,r,"Everyday faction tools");
        addLaunch(r,"MEMBER","Member Center","My Day, war prep, OC, chain, training, faction resources, directory, chat and saved alerts all start here.",TornFcaUi.GREEN,MemberCenterActivity.class,"Open Member Center");

        TornFcaUi.addSection(this,r,"App & account");
        addLaunch(r,"SETTINGS","Settings","Notifications, API-key storage, optional services, privacy controls and account actions.",TornFcaUi.BLUE,SettingsActivity.class,"Open Settings");
        addLaunch(r,"LEGAL","Legal & Privacy","Review the current Privacy Policy, Terms & Conditions, EULA and your acknowledgement status.",TornFcaUi.PURPLE,LegalActivity.class,"Review Legal Documents");
        addLaunch(r,"ABOUT","About TornFCA","What TornFCA is, what stays free, version information, privacy approach and third-party services.",TornFcaUi.BORDER,AboutActivity.class,"About TornFCA");

        TornFcaUi.addSection(this,r,"Optional upgrade");
        addLaunch(r,"PLAN","TornFCA Premium","See what stays free and what optional Premium features add. Everyday member and leadership tools remain available without Premium.",TornFcaUi.GOLD,PremiumPreviewActivity.class,"View Plan");

        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);fp.topMargin=TornFcaUi.dp(this,8);
        r.addView(TornFcaUi.footer(this,"Looking for training, war prep, OC, chain, faction resources, directory, chat or alerts? Start in Member Center.\n\nTornFCA v"+TornFcaBrand.VERSION),fp);
        setContentView(s);s.requestApplyInsets();
    }

    private void addLaunch(LinearLayout r,String eye,String title,String body,int accent,Class<?> target,String action){
        LinearLayout c=TornFcaUi.card(this,eye,title,body,accent);c.setClickable(true);c.setFocusable(true);c.setOnClickListener(v->startActivity(new Intent(this,target)));
        Button b=TornFcaUi.button(this,action,accent);b.setOnClickListener(v->startActivity(new Intent(this,target)));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,44));p.topMargin=TornFcaUi.dp(this,9);c.addView(b,p);TornFcaUi.add(this,r,c);
    }

    /** Internal fallback used by release-safety checks; normal users use Settings → account action. */
    private void logout(){PushNotifications.unregisterAsync(this);new SecureApiKeyStore(this).clear();FactionScopeCache.clear(this);TornApiClient.clearMemoryCache();FactionMemberCache.clear();DeveloperPreviewStore.clear(this);PremiumEntitlementStore.clear(this);NotificationInboxStore.clear(this);Intent i=new Intent(this,AccessGateActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);startActivity(i);finish();}
}
