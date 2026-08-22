package com.matthayesego.duckforcetoolkit;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

/** Native notification boundary shared by local tests and cloud push. */
public final class NotificationCenter {
    public static final String CH_WAR="tornfca_war",CH_OC_CHAIN="tornfca_oc_chain",CH_FACTION="tornfca_faction",CH_BANKING="tornfca_banking",CH_CHAT="tornfca_chat",CH_PERSONAL="tornfca_personal";
    private static final String DEDUPE_PREFS="tornfca_notification_dedupe_v1",DEDUPE_SIGNATURE="signature",DEDUPE_AT="at";
    private static final long DEDUPE_WINDOW_MS=5000L;
    private NotificationCenter(){}
    public static void ensureChannels(Context c){if(Build.VERSION.SDK_INT<26)return;NotificationManager m=c.getSystemService(NotificationManager.class);if(m==null)return;create(m,CH_WAR,"War alerts","Ranked-war start and participation alerts",NotificationManager.IMPORTANCE_HIGH);create(m,CH_OC_CHAIN,"OC & chain","Organized crime and chain reminders",NotificationManager.IMPORTANCE_DEFAULT);create(m,CH_FACTION,"Faction updates","Faction announcements and important updates",NotificationManager.IMPORTANCE_DEFAULT);create(m,CH_BANKING,"Banking requests","New faction banking requests that need leadership attention",NotificationManager.IMPORTANCE_HIGH);create(m,CH_CHAT,"Faction chat","New TornFCA faction chat messages",NotificationManager.IMPORTANCE_DEFAULT);create(m,CH_PERSONAL,"Personal reminders","Your TornFCA personal reminders",NotificationManager.IMPORTANCE_DEFAULT);}
    @TargetApi(26) private static void create(NotificationManager m,String id,String name,String description,int importance){NotificationChannel ch=new NotificationChannel(id,name,importance);ch.setDescription(description);m.createNotificationChannel(ch);}
    public static boolean canPost(Context c){NotificationManager m=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(m==null||!m.areNotificationsEnabled())return false;return Build.VERSION.SDK_INT<33||c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;}
    public static void receive(Context c,String type,String title,String body,int factionId){if(isImmediateDuplicate(c,type,title,body,factionId))return;NotificationInboxStore.add(c,type,title,body,factionId);if(!AppSettingsStore.categoryEnabled(c,type)||!canPost(c))return;ensureChannels(c);String channel=channel(type);Intent open=targetIntent(c,type,factionId);open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);PendingIntent pi=PendingIntent.getActivity(c,Math.abs(((title==null?"":title)+(body==null?"":body)+System.nanoTime()).hashCode()),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,channel):new Notification.Builder(c);b.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title==null?"TornFCA":title).setContentText(body==null?"":body).setStyle(new Notification.BigTextStyle().bigText(body==null?"":body)).setAutoCancel(true).setContentIntent(pi).setCategory("chat".equalsIgnoreCase(type)?Notification.CATEGORY_MESSAGE:Notification.CATEGORY_REMINDER);if(("banking".equalsIgnoreCase(type)||"war".equalsIgnoreCase(type))&&Build.VERSION.SDK_INT<26)b.setPriority(Notification.PRIORITY_HIGH);((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).notify((int)(System.currentTimeMillis()&0x7fffffff),b.build());}
    private static synchronized boolean isImmediateDuplicate(Context c,String type,String title,String body,int factionId){if(c==null)return false;String signature=factionId+"|"+(type==null?"":type)+"|"+(title==null?"":title)+"|"+(body==null?"":body);long now=System.currentTimeMillis();SharedPreferences p=c.getSharedPreferences(DEDUPE_PREFS,Context.MODE_PRIVATE);String previous=p.getString(DEDUPE_SIGNATURE,"");long at=p.getLong(DEDUPE_AT,0L);if(signature.equals(previous)&&now-at>=0L&&now-at<DEDUPE_WINDOW_MS)return true;p.edit().putString(DEDUPE_SIGNATURE,signature).putLong(DEDUPE_AT,now).apply();return false;}
    public static void test(Context c){receive(c,"personal","TornFCA notifications are ready","This is a local test. Your category choices and Android notification permission are working.",0);}
    private static Intent targetIntent(Context c,String type,int suppliedFactionId){String t=type==null?"":type.toLowerCase(java.util.Locale.US);String key=new SecureApiKeyStore(c).load();AuthSession hot=key==null?null:TornApiClient.cachedSession(key);FactionScopeCache.Scope scope=hot==null&&key!=null?FactionScopeCache.load(c,key):null;int factionId=suppliedFactionId>0?suppliedFactionId:hot!=null?hot.factionId:scope==null?0:scope.factionId;String factionName=hot!=null?hot.factionName:scope==null?"Faction":scope.factionName;String position=hot!=null?hot.position:scope==null?"":scope.position;boolean factionApi=hot!=null?hot.factionApiAccess:scope!=null&&scope.factionApiAccess;
        if("chat".equals(t))return new Intent(c,FactionChatActivity.class);
        if("moderation".equals(t)||"report".equals(t))return new Intent(c,CommunityModerationActivity.class);
        if("banking".equals(t)){Intent i=new Intent(c,BankingCompanionActivity.class);i.putExtra(FactionOpsActivity.EXTRA_FACTION_ID,factionId);i.putExtra(FactionOpsActivity.EXTRA_FACTION_NAME,factionName);i.putExtra(FactionOpsActivity.EXTRA_FACTION_API,factionApi);i.putExtra(DeveloperConsoleActivity.EXTRA_POSITION,position);return i;}
        if("war".equals(t)){Intent i=new Intent(c,WarHubActivity.class);i.putExtra(FactionOpsActivity.EXTRA_FACTION_ID,factionId);i.putExtra(FactionOpsActivity.EXTRA_FACTION_NAME,factionName);i.putExtra(FactionOpsActivity.EXTRA_FACTION_API,factionApi);return i;}
        if("chain".equals(t)||"oc".equals(t)){Intent i=new Intent(c,FeatureRouterActivity.class);i.putExtra(FeatureRouterActivity.EXTRA_TARGET,"chain".equals(t)?FeatureRouterActivity.TARGET_CHAIN:FeatureRouterActivity.TARGET_OC);return i;}
        if("faction".equals(t)||"announcement".equals(t)){Intent i=new Intent(c,WarNoticeActivity.class);i.putExtra(WarNoticeActivity.EXTRA_FACTION_ID,factionId);i.putExtra(WarNoticeActivity.EXTRA_FACTION_NAME,factionName);i.putExtra(WarNoticeActivity.EXTRA_CAN_PUBLISH,AccessPolicy.isLeaderPosition(position));return i;}
        return new Intent(c,NotificationInboxActivity.class);
    }
    private static String channel(String type){String t=type==null?"":type.toLowerCase(java.util.Locale.US);if("war".equals(t))return CH_WAR;if("oc".equals(t)||"chain".equals(t))return CH_OC_CHAIN;if("banking".equals(t))return CH_BANKING;if("faction".equals(t)||"announcement".equals(t)||"moderation".equals(t)||"report".equals(t))return CH_FACTION;if("chat".equals(t))return CH_CHAT;return CH_PERSONAL;}
}
