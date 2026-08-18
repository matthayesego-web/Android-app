package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Makes the canonical TornFCA command dashboard gauges represent real device/Torn data. */
public final class BetaGaugeLiveData {
    private static final long LIVE_TTL_MS=60_000L;
    private static final long TRAINING_TTL_MS=5L*60L*1000L;
    private static final String WAR_PREP_PREFS="tornfca_war_prep_v2";
    private static final String TRAINING_PREFS="tornfca_training_progress_v1";
    private static final Map<View,Boolean> OBSERVED=Collections.synchronizedMap(new WeakHashMap<>());
    private static final AtomicBoolean LIVE_IN_FLIGHT=new AtomicBoolean(false);
    private static final AtomicBoolean TRAINING_IN_FLIGHT=new AtomicBoolean(false);

    private static boolean installed;
    private static volatile int playerId,factionId;
    private static volatile int energyCurrent=-1,energyMax=-1;
    private static volatile long warToken;
    private static volatile long liveFetchedAt,liveAttemptAt;
    private static volatile long trainingCurrentTotal=-1L,trainingFetchedAt,trainingAttemptAt;

    private BetaGaugeLiveData(){}

    public static synchronized void install(Application app){
        if(app==null||installed||!TornFcaCommandRuntime.enabled())return;
        installed=true;
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
            @Override public void onActivityCreated(Activity activity,Bundle state){}
            @Override public void onActivityStarted(Activity activity){}
            @Override public void onActivityResumed(Activity activity){if(activity instanceof BetaCommandActivity)attach(activity);}
            @Override public void onActivityPaused(Activity activity){}
            @Override public void onActivityStopped(Activity activity){}
            @Override public void onActivitySaveInstanceState(Activity activity,Bundle state){}
            @Override public void onActivityDestroyed(Activity activity){}
        });
    }

    private static void attach(Activity activity){
        ViewGroup root=activity.findViewById(android.R.id.content);if(root==null)return;
        if(OBSERVED.put(root,Boolean.TRUE)==null){root.getViewTreeObserver().addOnGlobalLayoutListener(()->{if(!activity.isFinishing())refreshVisible(activity,root);});}
        root.post(()->refreshVisible(activity,root));
    }

    private static void refreshVisible(Activity activity,View root){
        Scope scope=scope(activity);if(scope.playerId<=0||scope.factionId<=0)return;
        ensureScope(scope.playerId,scope.factionId);

        LinearLayout today=findMetric(root,"TODAY");
        LinearLayout prep=findMetric(root,"WAR PREP");
        LinearLayout alerts=findMetric(root,"ALERTS");
        LinearLayout progress=findMetric(root,"PROGRESS");
        LinearLayout baseline=findMetric(root,"GUIDES");

        if(today!=null){
            if(energyCurrent>=0&&energyMax>0){int pct=Math.max(0,Math.min(100,Math.round(100f*energyCurrent/energyMax)));applyGauge(activity,today,pct+"%","Energy",energyCurrent+" / "+energyMax,pct/100f,TornFcaCommandUi.GREEN,()->open(activity,MemberDailyActivity.class));}
            else applyGauge(activity,today,"--","Energy","Updating from Torn…",0f,TornFcaCommandUi.GREEN,()->open(activity,MemberDailyActivity.class));
        }

        if(prep!=null){PrepLocal local=prepLocal(activity,scope.playerId,scope.factionId);int remaining=Math.max(0,local.total-local.done);applyGauge(activity,prep,local.done+"/"+local.total,"War prep",local.done==local.total?"Checklist complete":remaining+" steps left",local.total<=0?0f:local.done/(float)local.total,local.done==local.total?TornFcaCommandUi.GREEN:TornFcaCommandUi.GOLD,()->open(activity,WarPrepActivity.class));}

        if(alerts!=null){JSONArray rows=NotificationInboxStore.all(activity);int count=rows==null?0:rows.length();float fill=count<=0?0f:Math.min(1f,count/10f);applyGauge(activity,alerts,String.valueOf(count),"Saved alerts",count==0?"Inbox clear":count+" on this device",fill,count==0?TornFcaCommandUi.GREEN:TornFcaCommandUi.PURPLE,()->open(activity,NotificationInboxActivity.class));}

        if(progress!=null){TrainingLocal local=trainingLocal(activity,scope.playerId,scope.factionId);if(local.baselineTotal>0&&trainingCurrentTotal>=0){long gain=Math.max(0L,trainingCurrentTotal-local.baselineTotal);double pct=100d*gain/local.baselineTotal;String center=pct>=100d?"100%+":"+"+String.format(Locale.US,pct>=10?"%.0f%%":"%.1f%%",pct);applyGauge(activity,progress,center,"Stat gain","+"+compact(gain)+" vs baseline",(float)Math.min(1d,gain/(double)local.baselineTotal),TornFcaCommandUi.PURPLE,()->open(activity,TrainingProgressActivity.class));}else applyGauge(activity,progress,"--","Stat gain",local.at>0?"Updating current stats…":"Create baseline first",0f,TornFcaCommandUi.PURPLE,()->open(activity,TrainingProgressActivity.class));}

        if(baseline!=null){TrainingLocal local=trainingLocal(activity,scope.playerId,scope.factionId);long days=local.at<=0?0:Math.max(1L,(System.currentTimeMillis()-local.at+86399999L)/86400000L);applyGauge(activity,baseline,days<=0?"NEW":days+"d","Baseline age",days<=0?"Open progress to start":Math.min(days,30)+" / 30 day view",days<=0?0f:Math.min(1f,days/30f),TornFcaCommandUi.BLUE,()->open(activity,TrainingProgressActivity.class));}

        boolean needsLive=today!=null||prep!=null||progress!=null;
        long now=System.currentTimeMillis();if(needsLive&&now-liveFetchedAt>LIVE_TTL_MS&&now-liveAttemptAt>LIVE_TTL_MS)scheduleLive(activity,scope);
        if(progress!=null&&now-trainingFetchedAt>TRAINING_TTL_MS&&now-trainingAttemptAt>LIVE_TTL_MS)scheduleTraining(activity,scope);
    }

    private static synchronized void ensureScope(int p,int f){if(playerId==p&&factionId==f)return;playerId=p;factionId=f;energyCurrent=-1;energyMax=-1;warToken=0L;liveFetchedAt=0L;trainingCurrentTotal=-1L;trainingFetchedAt=0L;}

    private static void scheduleLive(Activity activity,Scope scope){
        if(!LIVE_IN_FLIGHT.compareAndSet(false,true))return;liveAttemptAt=System.currentTimeMillis();
        new Thread(()->{try{String key=new SecureApiKeyStore(activity).load();if(key==null||key.isBlank())return;JSONObject root=TornApiClient.getJson("/user?selections=bars",key);JSONObject bars=root.optJSONObject("bars");JSONObject energy=bars==null?null:bars.optJSONObject("energy");if(energy!=null){energyCurrent=energy.optInt("current",0);energyMax=energy.optInt("maximum",0);}try{WarStatus war=WarStatus.from(TornApiClient.getJson("/faction/wars",key),scope.factionId);warToken=war.warId!=0?war.warId:(war.start>0?war.start:0L);}catch(Exception ignored){}liveFetchedAt=System.currentTimeMillis();}catch(Exception ignored){}finally{LIVE_IN_FLIGHT.set(false);activity.runOnUiThread(()->{ViewGroup root=activity.findViewById(android.R.id.content);if(root!=null)refreshVisible(activity,root);});}},"TornFCA-GaugeLive").start();
    }

    private static void scheduleTraining(Activity activity,Scope scope){
        if(!TRAINING_IN_FLIGHT.compareAndSet(false,true))return;trainingAttemptAt=System.currentTimeMillis();
        new Thread(()->{try{String key=new SecureApiKeyStore(activity).load();if(key==null||key.isBlank())return;JSONObject root=TornApiClient.getJson("/user/battlestats",key);JSONObject battle=root.optJSONObject("battlestats");if(battle!=null){trainingCurrentTotal=battle.optLong("total",0L);trainingFetchedAt=System.currentTimeMillis();}}catch(Exception ignored){}finally{TRAINING_IN_FLIGHT.set(false);activity.runOnUiThread(()->{ViewGroup root=activity.findViewById(android.R.id.content);if(root!=null)refreshVisible(activity,root);});}},"TornFCA-GaugeTraining").start();
    }

    private static PrepLocal prepLocal(Activity activity,int p,int f){
        SharedPreferences prefs=activity.getSharedPreferences(WAR_PREP_PREFS,Activity.MODE_PRIVATE);String cycle=warToken==0?"general":"war"+warToken;String prefix="p"+p+"_f"+f+"_"+cycle+"_";int total=Math.max(1,Math.min(8,prefs.getInt(prefix+"item_count",5))),done=0;for(int i=1;i<=total;i++)if(prefs.getBoolean(prefix+"item"+i,false))done++;return new PrepLocal(done,total);
    }

    private static TrainingLocal trainingLocal(Activity activity,int p,int f){SharedPreferences prefs=activity.getSharedPreferences(TRAINING_PREFS,Activity.MODE_PRIVATE);String prefix="p"+p+"_f"+f+"_";return new TrainingLocal(prefs.getLong(prefix+"at",0L),prefs.getLong(prefix+"total",0L));}

    private static Scope scope(Activity activity){String key=new SecureApiKeyStore(activity).load();if(key==null||key.isBlank())return new Scope(0,0);AuthSession hot=TornApiClient.cachedSession(key);if(hot!=null)return new Scope(hot.playerId,hot.factionId);FactionScopeCache.Scope cached=FactionScopeCache.load(activity,key);return cached==null?new Scope(0,0):new Scope(cached.playerId,cached.factionId);}

    private static LinearLayout findMetric(View root,String eyebrow){if(root instanceof LinearLayout){LinearLayout l=(LinearLayout)root;if(l.getChildCount()>=4&&l.getChildAt(0) instanceof TextView&&eyebrow.equals(textOf((TextView)l.getChildAt(0))))return l;}if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++){LinearLayout found=findMetric(g.getChildAt(i),eyebrow);if(found!=null)return found;}}return null;}

    private static String textOf(TextView t){return t.getText()==null?"":t.getText().toString();}

    private static void applyGauge(Activity activity,LinearLayout tile,String center,String title,String detail,float progress,int accent,Runnable action){
        if(tile==null||tile.getChildCount()<4)return;View ring=tile.getChildAt(1);LinearLayout.LayoutParams params=(LinearLayout.LayoutParams)ring.getLayoutParams();if(ring instanceof LiveGaugeView)((LiveGaugeView)ring).update(center,progress,accent);else{LiveGaugeView live=new LiveGaugeView(activity,center,progress,accent);tile.removeViewAt(1);tile.addView(live,1,params);}TextView titleView=(TextView)tile.getChildAt(2),detailView=(TextView)tile.getChildAt(3);if(!title.equals(textOf(titleView)))titleView.setText(title);if(!detail.equals(textOf(detailView)))detailView.setText(detail);tile.setOnClickListener(v->action.run());tile.setContentDescription(title+": "+center+". "+detail+". Tap to open.");
    }

    private static void open(Activity activity,Class<?> target){activity.startActivity(new Intent(activity,target));}
    private static String compact(long value){if(value>=1_000_000_000L)return String.format(Locale.US,"%.1fB",value/1_000_000_000d);if(value>=1_000_000L)return String.format(Locale.US,"%.1fM",value/1_000_000d);if(value>=1_000L)return String.format(Locale.US,"%.1fK",value/1_000d);return NumberFormat.getIntegerInstance(Locale.US).format(value);}

    private static final class Scope{final int playerId,factionId;Scope(int playerId,int factionId){this.playerId=playerId;this.factionId=factionId;}}
    private static final class PrepLocal{final int done,total;PrepLocal(int done,int total){this.done=done;this.total=total;}}
    private static final class TrainingLocal{final long at,baselineTotal;TrainingLocal(long at,long baselineTotal){this.at=at;this.baselineTotal=baselineTotal;}}

    private static final class LiveGaugeView extends View{
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final RectF oval=new RectF();private String center;private float progress;private int accent;
        LiveGaugeView(Activity activity,String center,float progress,int accent){super(activity);this.center=center;this.progress=clamp(progress);this.accent=accent;setLayerType(LAYER_TYPE_SOFTWARE,null);}
        void update(String center,float progress,int accent){float next=clamp(progress);if(this.center.equals(center)&&Math.abs(this.progress-next)<.001f&&this.accent==accent)return;this.center=center;this.progress=next;this.accent=accent;invalidate();}
        private float clamp(float v){return Math.max(0f,Math.min(1f,v));}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),m=Math.min(w,h),pad=m*.13f;oval.set(pad,pad,w-pad,h-pad);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(m*.085f);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(Color.rgb(39,46,62));c.drawArc(oval,-90,360,false,p);if(progress>0f){p.setColor(accent);p.setShadowLayer(m*.07f,0,0,accent);c.drawArc(oval,-90,360f*progress,false,p);p.clearShadowLayer();}p.setStyle(Paint.Style.FILL);p.setColor(TornFcaCommandUi.TEXT);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);float scale=center.length()>6?.13f:center.length()>4?.155f:.20f;p.setTextSize(m*scale);Paint.FontMetrics fm=p.getFontMetrics();c.drawText(center,w/2f,h/2f-(fm.ascent+fm.descent)/2f,p);}
    }
}
