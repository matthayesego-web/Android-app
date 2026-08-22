package com.matthayesego.duckforcetoolkit;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import org.json.JSONObject;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User-enabled live card for Ranked War chain status.
 *
 * The system chronometer performs the visible second-by-second countdown locally. Torn is queried
 * only every 30 seconds while a chain is active and every 60 seconds while a Ranked War is active
 * without a chain. The service removes itself as soon as no active Ranked War is returned.
 */
public class WarChainLiveService extends Service {
    private static final int NOTIFICATION_ID=591041;
    private static final long ACTIVE_CHAIN_REFRESH_MS=30_000L;
    private static final long IDLE_WAR_REFRESH_MS=60_000L;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final AtomicBoolean refreshing=new AtomicBoolean(false);
    private volatile boolean stopped;
    private long nextRefreshMs;
    private Snapshot snapshot=Snapshot.checking();

    public static void startIfEnabled(Context context){
        if(context==null||!AppSettingsStore.liveChainTracker(context)||!AppSettingsStore.notificationsEnabled(context)||!NotificationCenter.canPost(context))return;
        Intent i=new Intent(context,WarChainLiveService.class);
        try{
            if(Build.VERSION.SDK_INT>=26)context.startForegroundService(i);else context.startService(i);
        }catch(Exception ignored){}
    }

    public static void stop(Context context){
        if(context==null)return;
        try{context.stopService(new Intent(context,WarChainLiveService.class));}catch(Exception ignored){}
    }

    @Override public void onCreate(){
        super.onCreate();
        NotificationCenter.ensureChannels(this);
        startAsForeground(buildNotification(snapshot));
        scheduleRefresh(0L);
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(!AppSettingsStore.liveChainTracker(this)||!AppSettingsStore.notificationsEnabled(this)||!NotificationCenter.canPost(this)){stopTracker();return START_NOT_STICKY;}
        if(nextRefreshMs==0L||System.currentTimeMillis()>=nextRefreshMs)scheduleRefresh(0L);
        return START_STICKY;
    }

    private void startAsForeground(Notification notification){
        try{
            if(Build.VERSION.SDK_INT>=29)startForeground(NOTIFICATION_ID,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            else startForeground(NOTIFICATION_ID,notification);
        }catch(Exception e){
            stopped=true;stopSelf();
        }
    }

    private void scheduleRefresh(long delayMs){
        if(stopped)return;
        handler.removeCallbacks(refreshRunnable);
        nextRefreshMs=System.currentTimeMillis()+Math.max(0L,delayMs);
        handler.postDelayed(refreshRunnable,Math.max(0L,delayMs));
    }

    private final Runnable refreshRunnable=()->{
        if(stopped||!AppSettingsStore.liveChainTracker(this)){stopTracker();return;}
        if(!refreshing.compareAndSet(false,true))return;
        new Thread(()->{
            try{
                String key=new SecureApiKeyStore(this).load();
                if(key==null||key.isBlank()){stopTracker();return;}
                JSONObject warsRoot=TornApiClient.getJson("/faction/wars",key);
                JSONObject ranked=activeRankedWar(warsRoot);
                if(ranked==null){stopTracker();return;}
                int warId=ranked.optInt("war_id",0);
                Snapshot next;
                try{
                    JSONObject chainRoot=TornApiClient.getJson("/faction/chain",key);
                    JSONObject chain=chainRoot.optJSONObject("chain");
                    if(chain==null)next=Snapshot.warIdle(warId);
                    else{
                        int current=chain.optInt("current",0),max=chain.optInt("max",0),timeout=Math.max(0,chain.optInt("timeout",0));
                        double modifier=chain.optDouble("modifier",1.0);
                        next=current>0&&timeout>0?Snapshot.chain(warId,current,max,timeout,modifier):Snapshot.warIdle(warId);
                    }
                }catch(Exception chainError){next=Snapshot.warIdle(warId);}
                snapshot=next;
                publish(next);
                scheduleRefresh(next.chainActive?ACTIVE_CHAIN_REFRESH_MS:IDLE_WAR_REFRESH_MS);
            }catch(Exception ignored){
                // A temporary network/Torn failure should not instantly erase a live card. Keep the
                // last known state and retry on the conservative idle cadence.
                if(!stopped){publish(snapshot.withTemporaryRefreshIssue());scheduleRefresh(IDLE_WAR_REFRESH_MS);}
            }finally{refreshing.set(false);}
        },"TornFCA-WarChainLive").start();
    };

    private JSONObject activeRankedWar(JSONObject root){
        JSONObject wars=root==null?null:root.optJSONObject("wars");
        JSONObject ranked=wars==null?null:wars.optJSONObject("ranked");
        if(ranked==null||ranked.optInt("war_id",0)<=0)return null;
        long now=System.currentTimeMillis()/1000L,start=ranked.optLong("start",0L),end=ranked.optLong("end",0L);
        if(start>0L&&start>now)return null;
        if(end>0L&&end<=now)return null;
        return ranked;
    }

    private void publish(Snapshot value){
        if(stopped)return;
        NotificationManager manager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager!=null&&NotificationCenter.canPost(this))manager.notify(NOTIFICATION_ID,buildNotification(value));
    }

    private Notification buildNotification(Snapshot value){
        Intent open=new Intent(this,FeatureRouterActivity.class);open.putExtra(FeatureRouterActivity.EXTRA_TARGET,FeatureRouterActivity.TARGET_CHAIN);open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi=PendingIntent.getActivity(this,NOTIFICATION_ID,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,NotificationCenter.CH_OC_CHAIN):new Notification.Builder(this);
        String title=value.warId>0?"Ranked War #"+value.warId+" • Chain":"Ranked War Chain";
        String text;
        if(value.checking)text="Checking for an active Ranked War…";
        else if(value.chainActive)text="Chain "+value.current+" / "+value.max+" • "+String.format(Locale.US,"%.2fx",value.modifier);
        else text="War active • no chain running";
        if(value.temporaryIssue)text+=" • refreshing";
        b.setSmallIcon(android.R.drawable.ic_popup_sync)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text+(value.chainActive?"\nTornFCA refreshes the chain snapshot every 30 seconds; Android keeps the timeout countdown live locally.":"")))
                .setContentIntent(pi)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setShowWhen(false);
        if(value.chainActive&&value.deadlineMs>System.currentTimeMillis()){
            b.setWhen(value.deadlineMs).setShowWhen(true).setUsesChronometer(true);
            if(Build.VERSION.SDK_INT>=24)b.setChronometerCountDown(true);
        }
        Notification n=b.build();
        if(Build.VERSION.SDK_INT>=36){
            // API 36 Live Update request. Extras keep this source compatible with SDK 36 even on
            // devices where the promoted-ongoing Builder convenience method is an SDK extension.
            n.extras.putBoolean("android.requestPromotedOngoing",true);
            if(!value.chainActive)n.extras.putCharSequence("android.shortCriticalText","War");
        }
        return n;
    }

    private void stopTracker(){
        if(stopped)return;stopped=true;handler.removeCallbacksAndMessages(null);
        try{if(Build.VERSION.SDK_INT>=24)stopForeground(STOP_FOREGROUND_REMOVE);else stopForeground(true);}catch(Exception ignored){}
        stopSelf();
    }

    @Override public void onDestroy(){stopped=true;handler.removeCallbacksAndMessages(null);super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}

    @Override public void onTimeout(int startId,int fgsType){
        // Android 15+ caps dataSync foreground-service time. Fail closed rather than trying to
        // bypass the platform quota; opening TornFCA during the war can start a fresh eligible run.
        stopTracker();
    }

    private static final class Snapshot {
        final int warId,current,max;
        final double modifier;
        final long deadlineMs;
        final boolean chainActive,checking,temporaryIssue;
        Snapshot(int warId,int current,int max,double modifier,long deadlineMs,boolean chainActive,boolean checking,boolean temporaryIssue){this.warId=warId;this.current=current;this.max=max;this.modifier=modifier;this.deadlineMs=deadlineMs;this.chainActive=chainActive;this.checking=checking;this.temporaryIssue=temporaryIssue;}
        static Snapshot checking(){return new Snapshot(0,0,0,1.0,0L,false,true,false);}
        static Snapshot warIdle(int warId){return new Snapshot(warId,0,0,1.0,0L,false,false,false);}
        static Snapshot chain(int warId,int current,int max,int timeout,double modifier){return new Snapshot(warId,current,max,modifier,System.currentTimeMillis()+timeout*1000L,true,false,false);}
        Snapshot withTemporaryRefreshIssue(){return new Snapshot(warId,current,max,modifier,deadlineMs,chainActive,checking,true);}
    }
}
