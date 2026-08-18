package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Canonical command-shell runtime for the v0.10.1 -> v1.0 line.
 * The side-by-side Beta keeps Beta branding/package identity, while production uses the same proven
 * command-center layout with normal TornFCA branding and without the Beta marker.
 */
public final class TornFcaCommandRuntime {
    private static final Map<View,Boolean> OBSERVED=Collections.synchronizedMap(new WeakHashMap<>());
    private static boolean installed;

    private TornFcaCommandRuntime() {}

    /** v0.10.1 makes the command-center the canonical visible shell for both build variants. */
    public static boolean enabled() { return true; }

    public static boolean isBetaBuild() {
        return BuildConfig.APPLICATION_ID != null && BuildConfig.APPLICATION_ID.endsWith(".beta");
    }

    public static synchronized void install(Application app) {
        if (app == null || !enabled()) return;
        BetaSurfacePolish.install(app);
        BetaGaugeLiveData.install(app);
        BetaUxRepair.install(app);
        if(installed)return;installed=true;
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
            @Override public void onActivityCreated(Activity activity,Bundle state){}
            @Override public void onActivityStarted(Activity activity){}
            @Override public void onActivityResumed(Activity activity){if(activity instanceof BetaCommandActivity)attachBrandNormalizer(activity);}
            @Override public void onActivityPaused(Activity activity){}
            @Override public void onActivityStopped(Activity activity){}
            @Override public void onActivitySaveInstanceState(Activity activity,Bundle state){}
            @Override public void onActivityDestroyed(Activity activity){}
        });
    }

    public static Intent homeIntent(Context context, String section) {
        Intent i = new Intent(context, BetaCommandActivity.class);
        if (section != null && !section.trim().isEmpty()) i.putExtra(BetaCommandActivity.EXTRA_SECTION, section.trim());
        return i;
    }

    public static String topBrand() {return isBetaBuild() ? "TORN FCA BETA" : "TORN FCA";}
    public static String versionBadge() {return isBetaBuild() ? "BETA   •   v" + TornFcaBrand.VERSION : "v" + TornFcaBrand.VERSION;}
    public static String footerPrefix() {return isBetaBuild() ? "Torn FCA Beta v" : "Torn FCA v";}
    public static String footer(String factionName) {
        String faction = factionName == null || factionName.trim().isEmpty() ? "Faction" : factionName.trim();
        return footerPrefix() + TornFcaBrand.VERSION + "  •  " + faction;
    }

    private static void attachBrandNormalizer(Activity activity){
        View root=activity.findViewById(android.R.id.content);if(root==null)return;normalizeCommandBranding(activity,root);
        if(OBSERVED.put(root,Boolean.TRUE)!=null)return;
        root.getViewTreeObserver().addOnGlobalLayoutListener(()->{if(!activity.isFinishing())normalizeCommandBranding(activity,root);});
    }

    private static void normalizeCommandBranding(Activity activity,View view){
        if(isBetaBuild()||view==null)return;
        if(view instanceof TextView){
            TextView t=(TextView)view;String raw=t.getText()==null?"":t.getText().toString();
            if("TORN FCA BETA".equals(raw))t.setText(topBrand());
            else if(raw.startsWith("BETA   •   v"))t.setText(versionBadge());
            else if(raw.startsWith("Torn FCA Beta v"))t.setText(footerPrefix()+raw.substring("Torn FCA Beta v".length()));
        }
        if(view instanceof ImageView)replaceBetaCrest(activity,(ImageView)view);
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)normalizeCommandBranding(activity,g.getChildAt(i));}
    }

    private static void replaceBetaCrest(Activity activity,ImageView image){
        try{
            Drawable current=image.getDrawable(),beta=activity.getDrawable(R.drawable.tornfca_beta_crest);
            if(current!=null&&beta!=null&&current.getConstantState()!=null&&beta.getConstantState()!=null&&current.getConstantState().equals(beta.getConstantState()))image.setImageResource(R.drawable.tornfca_mark);
        }catch(Exception ignored){}
    }
}
