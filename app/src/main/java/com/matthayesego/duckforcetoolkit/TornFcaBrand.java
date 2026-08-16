package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Applies the public TornFCA brand and premium presentation without rewriting proven feature logic. */
public final class TornFcaBrand {
    public static final String NAME="TornFCA";
    public static final String LONG_NAME="Torn Faction Companion App";
    public static final String VERSION="0.9.18";

    private static final int[] LEGACY_BRAND_COLORS=new int[]{
            Color.rgb(241,190,86),Color.rgb(241,194,106),Color.rgb(243,184,52),Color.rgb(215,160,68),Color.rgb(242,197,107)
    };
    private static final Map<View,Boolean> ANIMATED=Collections.synchronizedMap(new WeakHashMap<>());

    private TornFcaBrand(){}

    public static void apply(Activity activity,View root){
        if(activity==null||root==null)return;
        FactionTheme theme=FactionTheme.forContext(activity);
        activity.getWindow().setStatusBarColor(theme.accentDark);
        activity.getWindow().setNavigationBarColor(theme.background);
        if(Build.VERSION.SDK_INT>=29){
            activity.getWindow().setStatusBarContrastEnforced(false);
            activity.getWindow().setNavigationBarContrastEnforced(false);
        }
        applyView(activity,root,theme);
        animateOnce(activity,root);
    }

    private static void applyView(Context context,View view,FactionTheme theme){
        if(view instanceof TextView){
            TextView t=(TextView)view;
            CharSequence raw=t.getText();
            if(raw!=null){String value=rebrand(raw.toString());if(!value.equals(raw.toString()))t.setText(value);}
            if(isLegacyBrandColor(t.getCurrentTextColor()))t.setTextColor(theme.accent);
            CharSequence hint=t.getHint();
            if(hint!=null){String value=rebrand(hint.toString());if(!value.equals(hint.toString()))t.setHint(value);}
            polishText(context,t);
        }
        if(view instanceof Button)polishButton(context,(Button)view,theme);
        if(view instanceof ImageView){replaceLegacyArtwork(context,(ImageView)view);polishImage(context,(ImageView)view,theme);}
        CharSequence description=view.getContentDescription();
        if(description!=null&&"Duck Force".contentEquals(description))view.setContentDescription(NAME);
        if(view instanceof ViewGroup){
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)applyView(context,group.getChildAt(i),theme);
            Object tag=view.getTag();
            if(tag instanceof String&&((String)tag).startsWith("nav:")&&view.getBackground()!=null&&selectedNav(group,theme.accent)){
                Drawable bg=view.getBackground().mutate();bg.setTint(theme.accentDark);view.setBackground(bg);
            }
            polishContainer(context,group,theme);
        }
    }

    private static void polishText(Context context,TextView text){
        float sp=text.getTextSize()/context.getResources().getDisplayMetrics().scaledDensity;
        CharSequence raw=text.getText();String value=raw==null?"":raw.toString().trim();
        if(text instanceof Button)return;
        if(sp>=24f){
            text.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
            if(Build.VERSION.SDK_INT>=21)text.setLetterSpacing(-0.012f);
            text.setLineSpacing(0f,1.02f);
        }else if(sp>=16f&&text.getTypeface()!=null&&text.getTypeface().isBold()){
            text.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        }else if(sp<=11.5f&&looksLikeEyebrow(value)){
            text.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
            if(Build.VERSION.SDK_INT>=21)text.setLetterSpacing(.12f);
        }
    }

    private static void polishButton(Context context,Button button,FactionTheme theme){
        button.setAllCaps(false);
        button.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        if(Build.VERSION.SDK_INT>=21){
            button.setLetterSpacing(.008f);
            if(button.getElevation()<dp(context,2f))button.setElevation(dp(context,2f));
        }
        if(Build.VERSION.SDK_INT>=28){
            button.setOutlineAmbientShadowColor(theme.accentDark);
            button.setOutlineSpotShadowColor(Color.BLACK);
        }
        if(button.getMinHeight()<dp(context,42f))button.setMinHeight(Math.round(dp(context,42f)));
    }

    private static void polishImage(Context context,ImageView image,FactionTheme theme){
        if(!"tornfca-profile-avatar".equals(image.getTag()))return;
        if(Build.VERSION.SDK_INT>=21&&image.getElevation()<dp(context,5f))image.setElevation(dp(context,5f));
        if(Build.VERSION.SDK_INT>=28){
            image.setOutlineAmbientShadowColor(theme.accentDark);
            image.setOutlineSpotShadowColor(Color.BLACK);
        }
    }

    private static void polishContainer(Context context,ViewGroup group,FactionTheme theme){
        if(group instanceof ScrollView||group.getBackground()==null)return;
        boolean cardLike=group.getBackground() instanceof GradientDrawable;
        if(!cardLike)return;
        if(Build.VERSION.SDK_INT>=21){
            float desired=group.isClickable()?dp(context,3.5f):dp(context,1.5f);
            if(group.getElevation()<desired)group.setElevation(desired);
        }
        if(Build.VERSION.SDK_INT>=28){
            group.setOutlineAmbientShadowColor(theme.accentDark);
            group.setOutlineSpotShadowColor(Color.BLACK);
        }
    }

    private static void animateOnce(Context context,View root){
        if(ANIMATED.put(root,Boolean.TRUE)!=null)return;
        root.setAlpha(.94f);root.setTranslationY(dp(context,5f));
        root.animate().alpha(1f).translationY(0f).setDuration(180L).setInterpolator(new DecelerateInterpolator()).start();
    }

    private static float dp(Context context,float value){return value*context.getResources().getDisplayMetrics().density;}
    private static boolean looksLikeEyebrow(String value){if(value==null||value.length()<2||value.length()>55)return false;boolean letter=false;for(int i=0;i<value.length();i++){char c=value.charAt(i);if(Character.isLetter(c)){letter=true;if(Character.isLowerCase(c))return false;}}return letter;}
    private static boolean selectedNav(ViewGroup group,int accent){for(int i=0;i<group.getChildCount();i++)if(group.getChildAt(i)instanceof TextView&&((TextView)group.getChildAt(i)).getCurrentTextColor()==accent)return true;return false;}

    private static void replaceLegacyArtwork(Context context,ImageView image){
        if("tornfca-profile-avatar".equals(image.getTag()))return;
        try{Drawable current=image.getDrawable();Drawable legacy=context.getDrawable(R.drawable.duckforce_noir_art);if(current!=null&&legacy!=null&&current.getConstantState()!=null&&legacy.getConstantState()!=null&&current.getConstantState().equals(legacy.getConstantState())){image.setImageResource(R.drawable.tornfca_mark);int pad=Math.round(8*context.getResources().getDisplayMetrics().density);image.setPadding(pad,pad,pad,pad);}}catch(Exception ignored){}
    }

    private static boolean isLegacyBrandColor(int color){for(int c:LEGACY_BRAND_COLORS)if(color==c)return true;return false;}

    public static String rebrand(String value){
        if(value==null)return"";
        if("DUCK FORCE".equals(value))return"FACTION COMPANION";
        if("Companion".equals(value))return"TornFCA";
        if("My Obligations".equals(value))return"My Day";
        if("What needs my action right now".equals(value))return"Bars, cooldowns, OC, chain and war readiness";
        String branded=value
                .replace("Sign in to Duck Force","Sign in to TornFCA")
                .replace("Connect to Duck Force","Connect to TornFCA")
                .replace("Duck Force only  •  ","")
                .replace("Duck Force only • ","")
                .replace("Duck Force Companion","TornFCA")
                .replace("DUCK FORCE COMPANION","TORNFCA")
                .replace("Duck Force Beta","TornFCA Beta")
                .replace("Duck Force beta","TornFCA beta")
                .replace("Unlock Duck Force Beta","Unlock TornFCA Beta")
                .replace("Duck Force membership","faction membership")
                .replace("Duck Force estimates","faction estimates")
                .replace("Loading Duck Force estimates","Loading faction estimates")
                .replace("Duck Force War Center","Faction War Center")
                .replace("Duck Force payout","faction payout")
                .replace("Duck Force can remain the first tenant","the current faction can remain the first tenant")
                .replace("DUCK FORCE •","TORNFCA •")
                .replace("Leadership-focused member lookup with current Torn status and opted-in battle intelligence.","Search members with Torn status, FFScouter estimates and TornStats spy intelligence.")
                .replace("v0.9.6","v0.9.18")
                .replace("v0.9.7","v0.9.18")
                .replace("v0.9.8","v0.9.18")
                .replace("v0.9.9","v0.9.18")
                .replace("v0.9.10","v0.9.18")
                .replace("v0.9.11","v0.9.18")
                .replace("v0.9.12","v0.9.18")
                .replace("v0.9.13","v0.9.18")
                .replace("v0.9.14","v0.9.18")
                .replace("v0.9.15","v0.9.18")
                .replace("v0.9.16","v0.9.18")
                .replace("v0.9.17","v0.9.18");
        if(branded.contains("Encrypted on this device"))branded=branded.replaceAll("v0\\.9\\.\\d+","v"+VERSION);
        return branded;
    }

    /** Retargets existing feature intents through thin TornFCA wrappers so every screen receives branding/theme. */
    public static Intent retarget(Context context,Intent source){
        if(source==null||source.getComponent()==null)return source;
        String c=source.getComponent().getClassName();Class<?> target=null;
        if(c.equals(MoreActivity.class.getName()))target=TornFcaScreens.More.class;
        else if(c.equals(AboutActivity.class.getName()))target=TornFcaScreens.About.class;
        else if(c.equals(FeatureRouterActivity.class.getName()))target=TornFcaScreens.FeatureRouter.class;
        else if(c.equals(WarCenterActivity.class.getName()))target=TornFcaScreens.WarCenter.class;
        else if(c.equals(WarPayoutActivity.class.getName()))target=TornFcaScreens.WarPayout.class;
        else if(c.equals(BankingCompanionActivity.class.getName()))target=TornFcaScreens.BankingCompanion.class;
        else if(c.equals(MemberDossierActivity.class.getName()))target=TornFcaScreens.MemberDossier.class;
        else if(c.equals(DeveloperGateActivity.class.getName()))target=TornFcaScreens.DeveloperGate.class;
        else if(c.equals(DeveloperPanelActivity.class.getName()))target=TornFcaScreens.DeveloperPanel.class;
        else if(c.equals(PremiumAdminActivity.class.getName()))target=TornFcaScreens.PremiumAdmin.class;
        else if(c.equals(LeadershipAttentionActivity.class.getName()))target=TornFcaScreens.LeadershipAttention.class;
        else if(c.equals(FactionStrengthActivity.class.getName()))target=TornFcaScreens.FactionStrength.class;
        else if(c.equals(MemberFactionActivity.class.getName())){
            String mode=source.getStringExtra(MemberFactionActivity.EXTRA_MODE);
            if(MemberFactionActivity.MODE_OVERVIEW.equals(mode))target=MemberDailyActivity.class;
            else if(MemberFactionActivity.MODE_PARTICIPATION.equals(mode))target=MemberWarActivity.class;
            else target=TornFcaScreens.MemberFaction.class;
        }
        else if(c.equals(WarNoticeActivity.class.getName()))target=TornFcaScreens.WarNotice.class;
        else if(c.equals(FactionOpsActivity.class.getName()))target=TornFcaScreens.FactionOps.class;
        else if(c.equals(OcTrackerActivity.class.getName()))target=TornFcaScreens.OcTracker.class;
        else if(c.equals(QuickIntelActivity.class.getName()))target=TornFcaScreens.QuickIntel.class;
        else if(c.equals(PremiumPreviewActivity.class.getName()))target=TornFcaScreens.PremiumPreview.class;
        else if(c.equals(DeveloperConsoleActivity.class.getName()))target=TornFcaScreens.DeveloperConsole.class;
        else if(c.equals(V098CompanionActivity.class.getName())||c.equals(V095CompanionActivity.class.getName()))target=TornFcaActivity.class;
        if(target!=null)source.setClass(context,target);
        return source;
    }
}
