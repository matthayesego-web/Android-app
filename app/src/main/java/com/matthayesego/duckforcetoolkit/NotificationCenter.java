package com.matthayesego.duckforcetoolkit;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

/** Native notification boundary shared by local tests now and cloud push when configured. */
public final class NotificationCenter {
    public static final String CH_WAR="tornfca_war",CH_OC_CHAIN="tornfca_oc_chain",CH_FACTION="tornfca_faction",CH_CHAT="tornfca_chat",CH_PERSONAL="tornfca_personal";
    private NotificationCenter(){}
    public static void ensureChannels(Context c){if(Build.VERSION.SDK_INT<26)return;NotificationManager m=c.getSystemService(NotificationManager.class);if(m==null)return;create(m,CH_WAR,"War alerts","Ranked-war start and participation alerts",NotificationManager.IMPORTANCE_HIGH);create(m,CH_OC_CHAIN,"OC & chain","Organized crime and chain reminders",NotificationManager.IMPORTANCE_DEFAULT);create(m,CH_FACTION,"Faction updates","Faction announcements and important updates",NotificationManager.IMPORTANCE_DEFAULT);create(m,CH_CHAT,"Faction chat","New TornFCA faction chat messages",NotificationManager.IMPORTANCE_DEFAULT);create(m,CH_PERSONAL,"Personal reminders","Your TornFCA personal reminders",NotificationManager.IMPORTANCE_DEFAULT);}
    private static void create(NotificationManager m,String id,String name,String description,int importance){NotificationChannel ch=new NotificationChannel(id,name,importance);ch.setDescription(description);m.createNotificationChannel(ch);}
    public static boolean canPost(Context c){NotificationManager m=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(m==null||!m.areNotificationsEnabled())return false;return Build.VERSION.SDK_INT<33||c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;}
    public static void receive(Context c,String type,String title,String body,int factionId){NotificationInboxStore.add(c,type,title,body,factionId);if(!AppSettingsStore.categoryEnabled(c,type)||!canPost(c))return;ensureChannels(c);String channel=channel(type);Intent open=new Intent(c,NotificationInboxActivity.class);open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);PendingIntent pi=PendingIntent.getActivity(c,Math.abs((title+body+System.nanoTime()).hashCode()),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,channel):new Notification.Builder(c);b.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title==null?"TornFCA":title).setContentText(body==null?"":body).setStyle(new Notification.BigTextStyle().bigText(body==null?"":body)).setAutoCancel(true).setContentIntent(pi).setCategory(Notification.CATEGORY_REMINDER);((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).notify((int)(System.currentTimeMillis()&0x7fffffff),b.build());}
    public static void test(Context c){receive(c,"personal","TornFCA notifications are ready","This is a local test. Your category choices and Android notification permission are working.",0);}
    private static String channel(String type){String t=type==null?"":type.toLowerCase(java.util.Locale.US);if("war".equals(t))return CH_WAR;if("oc".equals(t)||"chain".equals(t))return CH_OC_CHAIN;if("faction".equals(t)||"announcement".equals(t))return CH_FACTION;if("chat".equals(t))return CH_CHAT;return CH_PERSONAL;}
}
