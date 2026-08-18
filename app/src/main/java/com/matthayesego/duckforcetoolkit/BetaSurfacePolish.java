package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/** Command-surface styling for secondary screens under the canonical TornFCA command shell. */
public final class BetaSurfacePolish {
    private static final String RAIL_TAG="tornfca-beta-context-rail";
    private static final int BG=Color.rgb(3,6,10),PANEL=Color.rgb(9,15,23),PANEL_2=Color.rgb(13,21,32),PANEL_3=Color.rgb(19,29,43);
    private static final int LINE=Color.rgb(42,55,74),TEXT=Color.rgb(246,247,250),MUTED=Color.rgb(149,160,179),STEEL=Color.rgb(112,128,151);
    private static final int GOLD=Color.rgb(238,185,83),PURPLE=Color.rgb(147,89,246),GREEN=Color.rgb(78,190,129),RED=Color.rgb(226,91,100),BLUE=Color.rgb(84,151,222);

    private static final Map<View,Boolean> OBSERVED=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View,Boolean> STYLED=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View,Boolean> ANIMATED=Collections.synchronizedMap(new WeakHashMap<>());
    private static boolean installed;

    private BetaSurfacePolish(){}

    public static synchronized void install(Application application){
        if(application==null||installed||!isBeta())return;
        installed=true;
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
            @Override public void onActivityCreated(Activity activity,Bundle state){prepareWindow(activity);}
            @Override public void onActivityStarted(Activity activity){}
            @Override public void onActivityResumed(Activity activity){attach(activity);}
            @Override public void onActivityPaused(Activity activity){}
            @Override public void onActivityStopped(Activity activity){}
            @Override public void onActivitySaveInstanceState(Activity activity,Bundle state){}
            @Override public void onActivityDestroyed(Activity activity){}
        });
    }

    public static void apply(Activity activity,View root){
        if(activity==null||root==null||!isBeta()||skip(activity))return;
        prepareWindow(activity);
        decorateContextRail(activity,root);
        polishTree(activity,root);
        animateOnce(activity,root);
    }

    /** Historical name retained to avoid churn; now means canonical command runtime is enabled. */
    private static boolean isBeta(){return TornFcaCommandRuntime.enabled();}
    private static boolean skip(Activity activity){return activity instanceof BetaCommandActivity||activity instanceof AccessGateActivity;}

    private static void attach(Activity activity){
        if(activity==null||!isBeta()||skip(activity))return;
        View root=activity.findViewById(android.R.id.content);if(root==null)return;
        apply(activity,root);
        if(OBSERVED.put(root,Boolean.TRUE)!=null)return;
        root.getViewTreeObserver().addOnGlobalLayoutListener(()->{
            if(activity.isFinishing())return;
            try{decorateContextRail(activity,root);polishTree(activity,root);}catch(Exception ignored){}
        });
    }

    @SuppressWarnings("deprecation") private static void prepareWindow(Activity activity){
        if(activity==null||!isBeta())return;
        activity.getWindow().setStatusBarColor(BG);
        activity.getWindow().setNavigationBarColor(BG);
        if(android.os.Build.VERSION.SDK_INT>=28)activity.getWindow().setNavigationBarDividerColor(LINE);
        int flags=activity.getWindow().getDecorView().getSystemUiVisibility();
        flags&=~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;flags&=~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        activity.getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private static void decorateContextRail(Activity activity,View root){
        ScrollView scroll=findScroll(root);if(scroll==null||scroll.getChildCount()==0||!(scroll.getChildAt(0) instanceof LinearLayout))return;
        LinearLayout page=(LinearLayout)scroll.getChildAt(0);
        for(int i=0;i<page.getChildCount();i++)if(RAIL_TAG.equals(page.getChildAt(i).getTag()))return;
        int accent=accentFor(activity);
        LinearLayout rail=new LinearLayout(activity);rail.setTag(RAIL_TAG);rail.setOrientation(LinearLayout.HORIZONTAL);rail.setGravity(Gravity.CENTER_VERTICAL);rail.setPadding(dp(activity,11),dp(activity,9),dp(activity,9),dp(activity,9));rail.setBackground(gradient(activity,blend(PANEL_2,accent,.09f),PANEL,accent,14));
        View bar=new View(activity);bar.setBackground(solid(activity,accent,2,Color.TRANSPARENT,0));rail.addView(bar,new LinearLayout.LayoutParams(dp(activity,3),dp(activity,22)));
        TextView label=text(activity,contextLabel(activity),9.5f,accent,true);label.setLetterSpacing(.12f);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);lp.leftMargin=dp(activity,9);rail.addView(label,lp);
        TextView console=text(activity,"Console  ›",10.5f,TEXT,true);console.setGravity(Gravity.CENTER);console.setPadding(dp(activity,10),dp(activity,6),dp(activity,10),dp(activity,6));console.setBackground(solid(activity,Color.argb(120,Color.red(PANEL_3),Color.green(PANEL_3),Color.blue(PANEL_3)),9,accent,1));console.setClickable(true);console.setFocusable(true);console.setForeground(ripple(activity,accent,9));console.setOnClickListener(v->openConsole(activity));rail.addView(console,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);rp.bottomMargin=dp(activity,12);page.addView(rail,0,rp);
    }

    private static ScrollView findScroll(View view){
        if(view instanceof ScrollView)return(ScrollView)view;
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++){ScrollView s=findScroll(g.getChildAt(i));if(s!=null)return s;}}
        return null;
    }

    private static void openConsole(Activity activity){
        Intent i=TornFcaCommandRuntime.homeIntent(activity,sectionFor(activity));i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);activity.startActivity(i);
    }

    private static String sectionFor(Activity activity){
        String n=activity.getClass().getName().toLowerCase(Locale.US);
        if(n.contains("training"))return"Training";
        if(n.contains("settings")||n.contains("legal")||n.contains("about")||n.contains("premium")||n.contains("notification"))return"More";
        if(n.contains("war")||n.contains("territory")||n.contains("bank")||n.contains("leadership")||n.contains("factionops")||n.contains("quickintel"))return"Operations";
        if(n.contains("member")||n.contains("faction")||n.contains("oc")||n.contains("chat")||n.contains("resource")||n.contains("dossier"))return"Members";
        return"Home";
    }

    private static String contextLabel(Activity activity){
        String section=sectionFor(activity),suffix=TornFcaCommandRuntime.isBetaBuild()?"  •  BETA":"";
        if("Training".equals(section))return"TRAINING LAB"+suffix;
        if("Operations".equals(section))return"OPERATIONS COMMAND"+suffix;
        if("Members".equals(section))return"MEMBER NETWORK"+suffix;
        if("More".equals(section))return"APP CONTROL"+suffix;
        return"COMMAND TOOL"+suffix;
    }

    private static void polishTree(Activity activity,View view){
        if(view==null)return;
        if(STYLED.put(view,Boolean.TRUE)==null){
            if(view instanceof Button)styleButton(activity,(Button)view);
            else if(view instanceof TextView)styleText(activity,(TextView)view);
            else if(view instanceof ScrollView)styleScroll((ScrollView)view);
            if(view instanceof LinearLayout)styleLayout(activity,(LinearLayout)view);
        }
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)polishTree(activity,g.getChildAt(i));}
    }

    private static void styleScroll(ScrollView scroll){scroll.setBackgroundColor(BG);scroll.setFillViewport(true);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);scroll.setClipToPadding(false);}

    private static void styleText(Activity activity,TextView t){
        String value=value(t);float sp=t.getTextSize()/activity.getResources().getDisplayMetrics().scaledDensity;int base=accentFor(activity);
        if(t.isClickable()){
            int accent=semanticAccent(value,base);t.setTextColor(accent);t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return;
        }
        if(sp>=24f){t.setTextColor(TEXT);t.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));t.setLetterSpacing(-.012f);return;}
        if(looksLikeEyebrow(value)){
            int accent=semanticAccent(value,base);t.setTextColor(accent);t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));t.setLetterSpacing(.12f);return;
        }
        if(!isSemanticColor(t.getCurrentTextColor())){
            if(sp<=13.5f)t.setTextColor(MUTED);else t.setTextColor(TEXT);
        }
        if(sp>=16f&&t.getTypeface()!=null&&t.getTypeface().isBold())t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
    }

    private static void styleButton(Activity activity,Button b){
        int accent=semanticAccent(value(b),accentFor(activity));b.setAllCaps(false);b.setTextColor(TEXT);b.setTextSize(13f);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setGravity(Gravity.CENTER);b.setMinHeight(dp(activity,46));b.setPadding(dp(activity,14),0,dp(activity,14),0);b.setBackground(gradient(activity,blend(PANEL_3,accent,.13f),PANEL,accent,13));b.setForeground(ripple(activity,accent,13));b.setElevation(dp(activity,2));
    }

    private static void styleLayout(Activity activity,LinearLayout layout){
        if(RAIL_TAG.equals(layout.getTag())||layout.getChildCount()==0)return;
        Drawable bg=layout.getBackground();if(bg==null&&!layout.isClickable())return;
        String words=flatten(layout);int accent=semanticAccent(words,accentFor(activity));int stroke=layout.isClickable()?accent:LINE;
        layout.setBackground(gradient(activity,blend(PANEL_2,accent,layout.isClickable()?.08f:.03f),PANEL,stroke,18));
        if(layout.isClickable()){layout.setForeground(ripple(activity,accent,18));layout.setElevation(dp(activity,3));}
        else layout.setElevation(dp(activity,1));
    }

    private static int accentFor(Activity activity){
        String n=activity.getClass().getName().toLowerCase(Locale.US);
        if(n.contains("training"))return PURPLE;
        if(n.contains("war")||n.contains("territory"))return RED;
        if(n.contains("bank"))return BLUE;
        if(n.contains("settings")||n.contains("legal")||n.contains("about")||n.contains("notification"))return BLUE;
        if(n.contains("member")||n.contains("faction")||n.contains("oc")||n.contains("chat"))return PURPLE;
        if(n.contains("leadership")||n.contains("developer"))return GOLD;
        return GOLD;
    }

    private static int semanticAccent(String value,int fallback){
        String v=value==null?"":value.toUpperCase(Locale.US);
        if(v.contains("LOSS")||v.contains("ERROR")||v.contains("UNAVAILABLE")||v.contains("IN PROGRESS")||v.contains("RANKED WAR"))return RED;
        if(v.contains("WIN")||v.contains("READY")||v.contains("CURRENT STATUS")||v.contains("SUCCESS"))return GREEN;
        if(v.contains("BANK")||v.contains("INTEL")||v.contains("DETAIL"))return BLUE;
        if(v.contains("TRAIN")||v.contains("MEMBER")||v.contains("DOSSIER")||v.contains("OC"))return PURPLE;
        if(v.contains("LEADERSHIP")||v.contains("PAYOUT")||v.contains("PREMIUM"))return GOLD;
        return fallback;
    }

    private static boolean isSemanticColor(int c){return near(c,GOLD)||near(c,PURPLE)||near(c,GREEN)||near(c,RED)||near(c,BLUE);}
    private static boolean near(int a,int b){return Math.abs(Color.red(a)-Color.red(b))<24&&Math.abs(Color.green(a)-Color.green(b))<24&&Math.abs(Color.blue(a)-Color.blue(b))<24;}
    private static boolean looksLikeEyebrow(String value){if(value==null||value.length()<2||value.length()>70)return false;boolean letter=false;for(int i=0;i<value.length();i++){char c=value.charAt(i);if(Character.isLetter(c)){letter=true;if(Character.isLowerCase(c))return false;}}return letter;}
    private static String value(TextView t){CharSequence raw=t.getText();return raw==null?"":raw.toString().trim();}
    private static String flatten(View view){StringBuilder b=new StringBuilder();flatten(view,b);return b.toString();}
    private static void flatten(View view,StringBuilder b){if(view instanceof TextView){String v=value((TextView)view);if(!v.isBlank())b.append(v).append(' ');}if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)flatten(g.getChildAt(i),b);}}

    private static GradientDrawable gradient(Activity a,int first,int second,int stroke,int radius){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{first,second});g.setCornerRadius(dp(a,radius));if(stroke!=Color.TRANSPARENT)g.setStroke(dp(a,1),stroke);return g;}
    private static GradientDrawable solid(Activity a,int fill,int radius,int stroke,int width){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(a,radius));if(stroke!=Color.TRANSPARENT&&width>0)g.setStroke(dp(a,width),stroke);return g;}
    private static RippleDrawable ripple(Activity a,int accent,int radius){return new RippleDrawable(ColorStateList.valueOf(Color.argb(58,Color.red(accent),Color.green(accent),Color.blue(accent))),null,solid(a,Color.WHITE,radius,Color.TRANSPARENT,0));}
    private static int blend(int base,int accent,float amount){float a=Math.max(0f,Math.min(1f,amount));return Color.rgb(Math.round(Color.red(base)*(1f-a)+Color.red(accent)*a),Math.round(Color.green(base)*(1f-a)+Color.green(accent)*a),Math.round(Color.blue(base)*(1f-a)+Color.blue(accent)*a));}
    private static TextView text(Activity a,String value,float sp,int color,boolean bold){TextView t=new TextView(a);t.setText(value);t.setTextSize(sp);t.setTextColor(color);t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));t.setIncludeFontPadding(false);t.setSingleLine(true);t.setEllipsize(TextUtils.TruncateAt.END);return t;}
    private static int dp(Activity a,int v){return Math.round(v*a.getResources().getDisplayMetrics().density);}

    private static void animateOnce(Activity activity,View root){if(ANIMATED.put(root,Boolean.TRUE)!=null)return;root.setAlpha(.94f);root.setTranslationY(dp(activity,4));root.animate().alpha(1f).translationY(0f).setDuration(170L).setInterpolator(new DecelerateInterpolator()).start();}
}
