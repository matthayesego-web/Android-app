package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

public class SettingsActivity extends Activity {
    @Override protected void onCreate(Bundle b){super.onCreate(b);PushNotifications.syncIfReady(this);render();}
    @Override protected void onResume(){super.onResume();PushNotifications.syncIfReady(this);render();}

    private void render(){
        ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);
        TornFcaUi.header(this,r,"More","Settings","Notifications, security, optional services, legal/privacy choices and account controls are grouped here.");

        TornFcaUi.addSection(this,r,"Notifications & community");
        LinearLayout notifications=TornFcaUi.card(this,"NOTIFICATIONS","Alerts & Notification Inbox","Choose which war, OC, chain, faction, chat and personal alerts you want. Android notification permission stays under your control.",TornFcaUi.BLUE);
        Button manage=TornFcaUi.button(this,"Manage Notifications",TornFcaUi.BLUE);manage.setOnClickListener(v->startActivity(new Intent(this,NotificationSettingsActivity.class)));
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));np.topMargin=TornFcaUi.dp(this,10);notifications.addView(manage,np);TornFcaUi.add(this,r,notifications);

        String chatState=CommunityBackendClient.isConfigured()?"Available":"Not available in this build";
        LinearLayout cloud=TornFcaUi.card(this,"COMMUNITY","Faction Chat & Push Notifications",PushNotifications.status(this)+"\nFaction Chat: "+chatState,PushNotifications.cloudConfigured()?TornFcaUi.GREEN:TornFcaUi.GOLD);
        if(PushNotifications.cloudConfigured()){
            Button sync=TornFcaUi.button(this,"Sync This Device",TornFcaUi.GREEN);
            sync.setOnClickListener(v->{PushNotifications.syncIfReady(this);Toast.makeText(this,"Notification sync requested.",Toast.LENGTH_SHORT).show();});
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));cp.topMargin=TornFcaUi.dp(this,9);cloud.addView(sync,cp);
        }
        TornFcaUi.add(this,r,cloud);

        TornFcaUi.addSection(this,r,"Account & security");
        SecureApiKeyStore store=new SecureApiKeyStore(this);long expiry=store.persistedUntilMillis();
        String mode=expiry>0?"Encrypted on this device until "+DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(expiry))+".":"Session-only. Your API key will not be kept for the next app session.";
        LinearLayout account=TornFcaUi.card(this,"API KEY","Storage on this device",mode,TornFcaUi.GREEN);
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        addRetentionButton(row,"Session",false,30,0);addRetentionButton(row,"7d",true,7,1);addRetentionButton(row,"30d",true,30,2);addRetentionButton(row,"90d",true,90,3);
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,44));rp.topMargin=TornFcaUi.dp(this,10);account.addView(row,rp);TornFcaUi.add(this,r,account);

        TornFcaUi.addSection(this,r,"Optional services & plan");
        String key=store.load();boolean ff=key!=null&&FFScouterClient.hasConsent(this,key),ts=TornStatsClient.hasConsent(this);
        LinearLayout integrations=TornFcaUi.card(this,"OPTIONAL SERVICES","FFScouter & TornStats","FFScouter: "+(ff?"enabled":"disabled")+" • TornStats: "+(ts?"enabled":"disabled")+". TornFCA asks for separate permission before using either service.",TornFcaUi.PURPLE);
        if(ff||ts){
            Button disable=TornFcaUi.button(this,"Disable FFScouter & TornStats",TornFcaUi.RED);
            disable.setOnClickListener(v->{if(key!=null)FFScouterClient.setConsent(this,key,false);TornStatsClient.setConsent(this,false);Toast.makeText(this,"Optional intelligence services disabled.",Toast.LENGTH_SHORT).show();render();});
            LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));ip.topMargin=TornFcaUi.dp(this,10);integrations.addView(disable,ip);
        }
        TornFcaUi.add(this,r,integrations);

        int player=currentPlayerId();String premium=player>0&&PremiumEntitlementStore.hasPremium(this,player)?"Premium active":"Free plan";
        LinearLayout plan=TornFcaUi.card(this,"PLAN","TornFCA "+premium,"Core member and leadership tools stay free. Premium adds extra history, automation, alert options and convenience features.",TornFcaUi.GOLD);
        Button planButton=TornFcaUi.button(this,"View Plan",TornFcaUi.GOLD);planButton.setOnClickListener(v->startActivity(new Intent(this,PremiumPreviewActivity.class)));
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));pp.topMargin=TornFcaUi.dp(this,10);plan.addView(planButton,pp);TornFcaUi.add(this,r,plan);

        TornFcaUi.addSection(this,r,"Legal & privacy");
        String legalState=LegalAcceptanceStore.hasAcceptedCurrent(this)?"The current documents have been acknowledged on this device.":"The current documents have not been acknowledged on this device.";
        LinearLayout legal=TornFcaUi.card(this,"LEGAL","Privacy, Terms & EULA",legalState,TornFcaUi.PURPLE);
        Button review=TornFcaUi.button(this,"Review Legal Documents",TornFcaUi.PURPLE);review.setOnClickListener(v->startActivity(new Intent(this,LegalActivity.class)));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));lp.topMargin=TornFcaUi.dp(this,10);legal.addView(review,lp);TornFcaUi.add(this,r,legal);

        TornFcaUi.add(this,r,TornFcaUi.card(this,"PRIVACY","Data on this device","Notification history and preferences stay on this device. Your Torn API key stays session-only unless you choose encrypted storage. Cloud push remains disabled until the current legal version is acknowledged; when enabled, its device token can be tied to your verified Torn player and faction for notification delivery.",TornFcaUi.GREEN));

        TornFcaUi.addSection(this,r,"Account action");
        Button logout=TornFcaUi.button(this,"Log Out / Change API Key",TornFcaUi.RED);logout.setOnClickListener(v->logout());r.addView(logout,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,50)));
        setContentView(s);s.requestApplyInsets();
    }

    private void addRetentionButton(LinearLayout row,String label,boolean persist,int days,int index){Button b=TornFcaUi.button(this,label,TornFcaUi.BORDER);b.setOnClickListener(v->applyRetention(persist,days));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,TornFcaUi.dp(this,44),1f);if(index>0)p.leftMargin=TornFcaUi.dp(this,6);row.addView(b,p);}
    private void applyRetention(boolean persist,int days){try{SecureApiKeyStore store=new SecureApiKeyStore(this);String key=store.load();if(key==null||key.isBlank()){Toast.makeText(this,"No active API key is available.",Toast.LENGTH_SHORT).show();return;}SecureApiKeyStore.prepareNextSave(persist,days);store.save(key);Toast.makeText(this,persist?("API key encrypted for "+days+" days."):"API key changed to session-only.",Toast.LENGTH_SHORT).show();render();}catch(Exception e){Toast.makeText(this,"Unable to change key storage.",Toast.LENGTH_LONG).show();}}
    private int currentPlayerId(){String key=new SecureApiKeyStore(this).load();if(key==null)return 0;AuthSession hot=TornApiClient.cachedSession(key);if(hot!=null)return hot.playerId;FactionScopeCache.Scope sc=FactionScopeCache.load(this,key);return sc==null?0:sc.playerId;}
    private void logout(){PushNotifications.unregisterAsync(this);new SecureApiKeyStore(this).clear();FactionScopeCache.clear(this);TornApiClient.clearMemoryCache();FactionMemberCache.clear();DeveloperPreviewStore.clear(this);PremiumEntitlementStore.clear(this);NotificationInboxStore.clear(this);Intent i=new Intent(this,AccessGateActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);startActivity(i);finish();}
}
