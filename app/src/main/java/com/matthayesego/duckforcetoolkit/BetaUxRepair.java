package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/** Small command-shell UX repairs shared by Beta and the production candidate. */
public final class BetaUxRepair {
    private static final String RAIL_TAG="tornfca-beta-context-rail";
    private static final String RAIL_COMPACT="tornfca-beta-context-rail-compact";
    private static final String ANNOUNCEMENT_ACTION_TAG="tornfca-command-announcements";
    private static final String WAR_PREP_ADMIN_ACTION_TAG="tornfca-command-warprep-admin";
    private static final long REFILL_TTL_MS=60_000L;
    private static final Map<View,Boolean> OBSERVED=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Activity,Long> REFILL_AT=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Activity,Boolean> REFILL_IN_FLIGHT=Collections.synchronizedMap(new WeakHashMap<>());
    private static boolean installed;

    private BetaUxRepair(){}

    public static synchronized void install(Application app){
        if(app==null||installed||!isBeta())return;
        installed=true;
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
            @Override public void onActivityCreated(Activity activity,Bundle state){}
            @Override public void onActivityStarted(Activity activity){}
            @Override public void onActivityResumed(Activity activity){attach(activity);}
            @Override public void onActivityPaused(Activity activity){}
            @Override public void onActivityStopped(Activity activity){}
            @Override public void onActivitySaveInstanceState(Activity activity,Bundle state){}
            @Override public void onActivityDestroyed(Activity activity){}
        });
    }

    /** Historical name retained to avoid churn; now means the canonical command runtime is enabled. */
    private static boolean isBeta(){return TornFcaCommandRuntime.enabled();}

    private static void attach(Activity activity){
        if(activity==null||!isBeta())return;
        View root=activity.findViewById(android.R.id.content);if(root==null)return;
        repair(activity,root);
        if(OBSERVED.put(root,Boolean.TRUE)!=null)return;
        root.getViewTreeObserver().addOnGlobalLayoutListener(()->{if(!activity.isFinishing())repair(activity,root);});
    }

    private static void repair(Activity activity,View root){
        if(activity instanceof BetaCommandActivity){repairCommand(activity,root);return;}
        compactReturnRail(activity,root);
        if(activity instanceof WarPrepActivity)repairRefills(activity,root);
    }

    private static void repairCommand(Activity activity,View root){
        replaceMoreIcons(activity,root);
        repairOperationsFeatured(activity,root);
        ensureLeadershipShortcuts(activity,root);
    }

    private static void replaceMoreIcons(Activity activity,View root){
        if(root instanceof LinearLayout){
            LinearLayout row=(LinearLayout)root;String tag=String.valueOf(row.getTag());
            if("command-nav:More".equals(tag)){setDirectIcon(activity,row,R.drawable.ic_beta_more,selectedNavColor(row));}
            else if(row.isClickable()){
                String words=flatten(row).toLowerCase(Locale.US);
                if(words.contains("legal & privacy"))setDirectIcon(activity,row,R.drawable.ic_beta_privacy,TornFcaCommandUi.PURPLE);
                else if(words.contains("about torn fca"))setDirectIcon(activity,row,R.drawable.ic_beta_info,TornFcaCommandUi.BLUE);
                else if(words.contains("torn fca premium"))setDirectIcon(activity,row,R.drawable.ic_beta_premium,TornFcaCommandUi.GOLD);
                else if(words.contains("notification inbox")||words.startsWith("alerts"))setDirectIcon(activity,row,R.drawable.ic_beta_notifications,TornFcaCommandUi.BLUE);
                else if(words.contains("settings")||words.contains("preferences"))setDirectIcon(activity,row,R.drawable.ic_beta_settings,TornFcaCommandUi.GOLD);
            }
        }
        if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++)replaceMoreIcons(activity,g.getChildAt(i));}
    }

    /** Operations' large hero is now the faction notice surface instead of a third Ranked War shortcut. */
    private static void repairOperationsFeatured(Activity activity,View root){
        if(!containsText(root,"Operations Command"))return;
        TextView title=findExactText(root,"Ranked War Command");if(title==null)return;
        ViewParent parent=title.getParent();if(!(parent instanceof LinearLayout))return;LinearLayout copy=(LinearLayout)parent;
        if(copy.getChildCount()<4)return;
        View badge=copy.getChildAt(0),body=copy.getChildAt(2),cta=copy.getChildAt(3);
        if(badge instanceof TextView)((TextView)badge).setText("FACTION NOTICE");
        title.setText("Faction Announcements");
        if(body instanceof TextView){((TextView)body).setText("Faction notices, war instructions and updates in one place.");((TextView)body).setMaxLines(4);}
        if(cta instanceof TextView){((TextView)cta).setText("Open Announcements   →");cta.setOnClickListener(v->openAnnouncements(activity));}
        FrameLayout frame=ancestorFrame(copy);if(frame!=null){ViewGroup.LayoutParams lp=frame.getLayoutParams();if(lp!=null){lp.height=ViewGroup.LayoutParams.WRAP_CONTENT;frame.setLayoutParams(lp);}frame.setMinimumHeight(TornFcaCommandUi.dp(activity,230));frame.setOnClickListener(v->openAnnouncements(activity));}
    }

    private static FrameLayout ancestorFrame(View view){ViewParent p=view.getParent();while(p instanceof View){if(p instanceof FrameLayout)return(FrameLayout)p;p=p.getParent();}return null;}

    /** Make the two faction-wide leadership workflows discoverable without adding another main navigation tab. */
    private static void ensureLeadershipShortcuts(Activity activity,View root){
        SessionInfo info=sessionInfo(activity);if(!info.leader||!containsText(root,"Leadership Command"))return;
        LinearLayout panel=findLeadershipPanel(root);if(panel==null)return;
        if(findTag(panel,ANNOUNCEMENT_ACTION_TAG)==null){
            LinearLayout row=TornFcaCommandUi.actionRow(activity,R.drawable.ic_nav_more,"Faction Announcements","Post and review faction notices and push updates","Announce",TornFcaCommandUi.GOLD,()->openAnnouncements(activity));row.setTag(ANNOUNCEMENT_ACTION_TAG);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.topMargin=TornFcaCommandUi.dp(activity,7);panel.addView(row,lp);
        }
        if(findTag(panel,WAR_PREP_ADMIN_ACTION_TAG)==null){
            LinearLayout row=TornFcaCommandUi.actionRow(activity,R.drawable.ic_nav_training,"War Prep Management","Customize your faction checklist and review TornFCA member readiness","Manage",TornFcaCommandUi.GREEN,()->activity.startActivity(new Intent(activity,WarPrepLeadershipActivity.class)));row.setTag(WAR_PREP_ADMIN_ACTION_TAG);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.topMargin=TornFcaCommandUi.dp(activity,7);panel.addView(row,lp);
        }
    }

    private static LinearLayout findLeadershipPanel(View root){
        TextView title=findExactText(root,"Leadership Command");if(title==null)return null;ViewParent p=title.getParent();
        while(p instanceof View){if(p instanceof LinearLayout){LinearLayout row=(LinearLayout)p;if(containsText(row,"Leadership Command")&&containsText(row,"Activity Tracker"))return row;}p=p.getParent();}
        return null;
    }

    private static void openAnnouncements(Activity activity){
        SessionInfo info=sessionInfo(activity);Intent i=new Intent(activity,WarNoticeActivity.class);i.putExtra(WarNoticeActivity.EXTRA_FACTION_ID,info.factionId);i.putExtra(WarNoticeActivity.EXTRA_FACTION_NAME,info.factionName);i.putExtra(WarNoticeActivity.EXTRA_CAN_PUBLISH,info.leader);activity.startActivity(i);
    }

    private static int selectedNavColor(LinearLayout row){for(int i=0;i<row.getChildCount();i++)if(row.getChildAt(i) instanceof TextView)return((TextView)row.getChildAt(i)).getCurrentTextColor();return TornFcaCommandUi.STEEL;}
    private static void setDirectIcon(Activity activity,LinearLayout row,int drawableId,int tint){for(int i=0;i<row.getChildCount();i++)if(row.getChildAt(i) instanceof ImageView){ImageView iv=(ImageView)row.getChildAt(i);Drawable d=activity.getDrawable(drawableId);if(d!=null){d=d.mutate();d.setTint(tint);iv.setImageDrawable(d);}return;}}

    private static SessionInfo sessionInfo(Activity activity){String key=new SecureApiKeyStore(activity).load();if(key==null||key.isBlank())return new SessionInfo(0,0,"Faction",false);AuthSession hot=TornApiClient.cachedSession(key);if(hot!=null)return new SessionInfo(hot.playerId,hot.factionId,hot.factionName,MemberPresentationPolicy.leadershipVisible(activity,hot.position));FactionScopeCache.Scope scope=FactionScopeCache.load(activity,key);return scope==null?new SessionInfo(0,0,"Faction",false):new SessionInfo(scope.playerId,scope.factionId,scope.factionName,MemberPresentationPolicy.leadershipVisible(activity,scope.position));}

    private static void compactReturnRail(Activity activity,View root){View found=findTag(root,RAIL_TAG);if(!(found instanceof LinearLayout))return;LinearLayout rail=(LinearLayout)found;if(!RAIL_COMPACT.equals(String.valueOf(rail.getContentDescription()))){String section=sectionFor(activity);rail.removeAllViews();rail.setContentDescription(RAIL_COMPACT);rail.setOrientation(LinearLayout.HORIZONTAL);rail.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);rail.setPadding(0,0,0,0);rail.setBackground(null);rail.setElevation(0f);TextView back=TornFcaCommandUi.text(activity,"←  "+section,10.8f,TornFcaCommandUi.MUTED,true);back.setPadding(TornFcaCommandUi.dp(activity,10),TornFcaCommandUi.dp(activity,6),TornFcaCommandUi.dp(activity,10),TornFcaCommandUi.dp(activity,6));back.setBackground(TornFcaCommandUi.solid(activity,Color.argb(95,18,26,39),9,TornFcaCommandUi.LINE_SOFT,1));back.setClickable(true);back.setFocusable(true);back.setForeground(TornFcaCommandUi.ripple(activity,TornFcaCommandUi.GOLD,9));back.setOnClickListener(v->{Intent i=TornFcaCommandRuntime.homeIntent(activity,section);i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);activity.startActivity(i);});rail.addView(back,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));}removeLegacyHeaderBackButton(rail);}
    private static void removeLegacyHeaderBackButton(LinearLayout rail){ViewParent parent=rail.getParent();if(!(parent instanceof LinearLayout))return;LinearLayout page=(LinearLayout)parent;for(int i=page.getChildCount()-1;i>=0;i--){View child=page.getChildAt(i);if(child==rail||!(child instanceof Button))continue;CharSequence raw=((Button)child).getText();if(raw!=null&&raw.toString().trim().startsWith("←"))page.removeViewAt(i);}}
    private static String sectionFor(Activity activity){String n=activity.getClass().getName().toLowerCase(Locale.US);if(n.contains("training"))return"Training";if(n.contains("settings")||n.contains("legal")||n.contains("about")||n.contains("premium")||n.contains("notification")||n.contains("developer"))return"More";if(n.contains("war")||n.contains("territory")||n.contains("bank")||n.contains("leadership")||n.contains("factionops")||n.contains("quickintel"))return"Operations";if(n.contains("member")||n.contains("faction")||n.contains("oc")||n.contains("chat")||n.contains("resource")||n.contains("dossier"))return"Members";return"Home";}

    private static void repairRefills(Activity activity,View root){TextView title=findExactText(root,"Available refills");if(title==null)title=findExactText(root,"Daily & special refills");if(title==null||!(title.getParent() instanceof LinearLayout))return;LinearLayout card=(LinearLayout)title.getParent();TextView body=findBody(card);if(body==null)return;if("Available refills".contentEquals(title.getText())){title.setText("Daily & special refills");String provisional=body.getText()==null?"":body.getText().toString();provisional=provisional.replace("Used / unavailable","__AVAILABLE__").replace("Available","Used today").replace("__AVAILABLE__","Available");if(!provisional.contains("Special refills:"))provisional+="\nSpecial refills: Checking…";body.setText(provisional);}long now=System.currentTimeMillis();Long last=REFILL_AT.get(activity);if(last!=null&&now-last<REFILL_TTL_MS)return;if(Boolean.TRUE.equals(REFILL_IN_FLIGHT.get(activity)))return;REFILL_IN_FLIGHT.put(activity,Boolean.TRUE);REFILL_AT.put(activity,now);new Thread(()->{try{String key=new SecureApiKeyStore(activity).load();if(key==null||key.isBlank())throw new Exception("No API key");JSONObject response=TornApiClient.getJson("/user/refills",key);JSONObject refills=response.optJSONObject("refills");if(refills==null&&response.has("energy"))refills=response;if(refills==null)throw new Exception("Refills unavailable");final JSONObject value=refills;activity.runOnUiThread(()->applyRefills(activity,value));}catch(Exception ignored){activity.runOnUiThread(()->applyRefillUnavailable(activity));}finally{REFILL_IN_FLIGHT.remove(activity);}},"TornFCA-RefillRepair").start();}
    private static void applyRefills(Activity activity,JSONObject refills){View root=activity.findViewById(android.R.id.content);TextView title=findExactText(root,"Daily & special refills");if(title==null||!(title.getParent() instanceof LinearLayout))return;TextView body=findBody((LinearLayout)title.getParent());if(body==null)return;String text="Energy point refill: "+refillState(refills,"energy")+"\nNerve point refill: "+refillState(refills,"nerve")+"\nToken refill: "+refillState(refills,"token")+"\nSpecial refills: "+(refills.has("special_count")?String.valueOf(Math.max(0,refills.optInt("special_count",0))):"Not reported");body.setText(text);}
    private static String refillState(JSONObject refills,String key){if(!refills.has(key))return"Not reported";return refills.optBoolean(key,false)?"Used today":"Available";}
    private static void applyRefillUnavailable(Activity activity){View root=activity.findViewById(android.R.id.content);TextView title=findExactText(root,"Daily & special refills");if(title==null||!(title.getParent() instanceof LinearLayout))return;TextView body=findBody((LinearLayout)title.getParent());if(body!=null&&body.getText()!=null&&body.getText().toString().contains("Checking…"))body.setText(body.getText().toString().replace("Special refills: Checking…","Special refills: Not reported"));}

    private static TextView findBody(LinearLayout card){for(int i=0;i<card.getChildCount();i++)if(card.getChildAt(i) instanceof TextView){TextView t=(TextView)card.getChildAt(i);String s=t.getText()==null?"":t.getText().toString();if(s.contains("Energy:")||s.contains("Energy point refill:"))return t;}return null;}
    private static TextView findExactText(View view,String text){if(view instanceof TextView&&text.contentEquals(((TextView)view).getText()))return(TextView)view;if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++){TextView t=findExactText(g.getChildAt(i),text);if(t!=null)return t;}}return null;}
    private static View findTag(View view,String tag){if(view!=null&&tag.equals(String.valueOf(view.getTag())))return view;if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++){View f=findTag(g.getChildAt(i),tag);if(f!=null)return f;}}return null;}
    private static boolean containsText(View view,String needle){if(view instanceof TextView){String s=((TextView)view).getText()==null?"":((TextView)view).getText().toString();if(s.contains(needle))return true;}if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)if(containsText(g.getChildAt(i),needle))return true;}return false;}
    private static String flatten(View view){StringBuilder out=new StringBuilder();flatten(view,out);return out.toString();}
    private static void flatten(View view,StringBuilder out){if(view instanceof TextView){CharSequence t=((TextView)view).getText();if(t!=null)out.append(t).append(' ');}if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)flatten(g.getChildAt(i),out);}}
    private static final class SessionInfo{final int playerId,factionId;final String factionName;final boolean leader;SessionInfo(int p,int f,String n,boolean l){playerId=p;factionId=f;factionName=n==null||n.isBlank()?"Faction":n;leader=l;}}
}
