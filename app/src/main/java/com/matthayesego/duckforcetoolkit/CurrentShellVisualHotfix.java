package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.app.Application;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Small runtime visual hotfix for the v0.9.31 persistent shell.
 *
 * It deliberately leaves routing and feature logic untouched. The pass only restores icon-first
 * bottom navigation and removes the duplicate CTA label created when TornFcaCurrentActivity's
 * explicit action hint and PremiumActionPolish's generated action label are both present.
 */
public final class CurrentShellVisualHotfix {
    private static final String NAV_PREFIX="current-nav:";
    private static final String ACTION_TAG="tornfca-premium-action-label";
    private static final int GOLD=Color.rgb(241,194,106);
    private static final int MUTED=Color.rgb(164,174,188);

    private static final Map<View,Boolean> OBSERVED=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View,Boolean> NAV_STATE=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View,Boolean> NAV_BARS=Collections.synchronizedMap(new WeakHashMap<>());
    private static boolean installed;

    private CurrentShellVisualHotfix(){}

    public static synchronized void install(Application application){
        if(application==null||installed)return;
        installed=true;
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
            @Override public void onActivityCreated(Activity activity,Bundle state){}
            @Override public void onActivityStarted(Activity activity){}
            @Override public void onActivityResumed(Activity activity){
                if(activity instanceof TornFcaCurrentActivity)attach(activity);
            }
            @Override public void onActivityPaused(Activity activity){}
            @Override public void onActivityStopped(Activity activity){}
            @Override public void onActivitySaveInstanceState(Activity activity,Bundle state){}
            @Override public void onActivityDestroyed(Activity activity){}
        });
    }

    private static void attach(Activity activity){
        View root=activity.findViewById(android.R.id.content);
        if(root==null)return;
        polish(activity,root);
        if(OBSERVED.put(root,Boolean.TRUE)!=null)return;
        root.getViewTreeObserver().addOnGlobalLayoutListener(()->{
            if(activity.isFinishing())return;
            try{polish(activity,root);}catch(Exception ignored){}
        });
    }

    private static void polish(Activity activity,View view){
        if(view instanceof TextView)polishNavItem(activity,(TextView)view);
        if(view instanceof LinearLayout)removeDuplicateAction((LinearLayout)view);
        if(view instanceof ViewGroup){
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)polish(activity,group.getChildAt(i));
        }
    }

    private static void polishNavItem(Activity activity,TextView item){
        Object rawTag=item.getTag();
        if(!(rawTag instanceof String))return;
        String tag=(String)rawTag;
        if(!tag.startsWith(NAV_PREFIX))return;

        String label=tag.substring(NAV_PREFIX.length());
        boolean selected=item.getBackground()!=null;
        Boolean previous=NAV_STATE.put(item,selected);
        if(previous!=null&&previous==selected)return;

        int icon=iconFor(label);
        Drawable drawable=icon==0?null:activity.getDrawable(icon);
        if(drawable!=null){
            drawable=drawable.mutate();
            drawable.setTint(selected?GOLD:MUTED);
        }
        item.setCompoundDrawablesWithIntrinsicBounds(null,drawable,null,null);
        item.setCompoundDrawablePadding(dp(activity,3));
        item.setGravity(Gravity.CENTER);
        item.setSingleLine(true);
        item.setTextSize(selected?9.2f:1f);
        item.setText(selected?label:"");
        item.setTextColor(selected?GOLD:MUTED);
        item.setContentDescription(selected?label+", selected":label);
        item.setPadding(dp(activity,3),dp(activity,5),dp(activity,3),dp(activity,4));
        ViewGroup.LayoutParams raw=item.getLayoutParams();
        if(raw!=null&&raw.height!=dp(activity,64)){
            raw.height=dp(activity,64);
            item.setLayoutParams(raw);
        }
        item.setBackground(selected?selectedBackground(activity):null);

        if(item.getParent() instanceof LinearLayout)polishNavBar(activity,(LinearLayout)item.getParent());
    }

    private static void polishNavBar(Activity activity,LinearLayout nav){
        if(NAV_BARS.put(nav,Boolean.TRUE)!=null)return;
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(11,17,25),Color.rgb(5,9,14)});
        bg.setCornerRadius(dp(activity,20));
        bg.setStroke(dp(activity,1),Color.rgb(55,68,84));
        nav.setBackground(bg);
        nav.setElevation(dp(activity,8));
    }

    private static GradientDrawable selectedBackground(Activity activity){
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(48,35,17),Color.rgb(13,17,22)});
        bg.setCornerRadius(dp(activity,15));
        bg.setStroke(dp(activity,1),GOLD);
        return bg;
    }

    private static int iconFor(String label){
        switch(label){
            case "Home":return R.drawable.ic_nav_home;
            case "Faction":return R.drawable.ic_nav_faction;
            case "War":return R.drawable.ic_nav_war;
            case "Training":return R.drawable.ic_nav_training;
            case "Leadership":return R.drawable.ic_nav_leadership;
            case "More":return R.drawable.ic_nav_more;
            default:return 0;
        }
    }

    private static void removeDuplicateAction(LinearLayout card){
        if(!card.isClickable()||card.getChildCount()<2)return;
        TextView tagged=null;
        for(int i=0;i<card.getChildCount();i++){
            View child=card.getChildAt(i);
            if(child instanceof TextView&&ACTION_TAG.equals(child.getTag())){
                tagged=(TextView)child;
                break;
            }
        }
        if(tagged==null)return;
        for(int i=card.getChildCount()-1;i>=0;i--){
            View child=card.getChildAt(i);
            if(child==tagged||!(child instanceof TextView)||child instanceof android.widget.Button)continue;
            CharSequence raw=((TextView)child).getText();
            if(raw!=null&&raw.toString().contains("→"))card.removeViewAt(i);
        }
    }

    private static int dp(Activity activity,int value){
        return Math.round(value*activity.getResources().getDisplayMetrics().density);
    }
}
