package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Applies the public TornFCA brand and premium presentation without rewriting proven feature logic. */
public final class TornFcaBrand {
    public static final String NAME="TornFCA";
    public static final String LONG_NAME="Torn Faction Companion App";
    public static final String VERSION="0.9.23";

    private static final int[] LEGACY_BRAND_COLORS=new int[]{
            Color.rgb(241,190,86),Color.rgb(241,194,106),Color.rgb(243,184,52),Color.rgb(215,160,68),Color.rgb(242,197,107)
    };
    private static final Map<View,Boolean> ANIMATED=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View,Boolean> OBSERVED=Collections.synchronizedMap(new WeakHashMap<>());

    private TornFcaBrand(){}

    public static void apply(Activity activity,View root){
        if(activity==null||root==null)return;
        FactionTheme theme=FactionTheme.forContext(activity);
        activity.getWindow().setStatusBarColor(theme.accentDark);
        activity.getWindow().setNavigationBarColor(theme.background);
        if(Build.VERSION.SDK_INT>=28)activity.getWindow().setNavigationBarDividerColor(theme.border);
        if(Build.VERSION.SDK_INT>=29){
            activity.getWindow().setStatusBarContrastEnforced(false);
            activity.getWindow().setNavigationBarContrastEnforced(false);
        }
        applyView(activity,root,theme);
        enhanceLeadership(activity,root,theme);
        PremiumActionPolish.apply(activity,root);
        observeLeadership(activity,root);
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
        String label=button.getText()==null?"":button.getText().toString();
        if("Open TornStats API Key FAQ".equals(label)&&context instanceof Activity){
            button.setText("Create / Recover TornStats Account");
            button.setOnClickListener(v->{try{context.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(TornStatsClient.REGISTER_URL)));}catch(Exception ignored){}});
        }
        button.setAllCaps(false);
        button.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        if(Build.VERSION.SDK_INT>=21){
            button.setLetterSpacing(.008f);
            if(button.getElevation()<dp(context,2f))button.setElevation(dp(context,2f));
        }
        if(Build.VERSION.SDK_INT>=23&&button.getForeground()==null)button.setForeground(ripple(context,theme,12f));
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
        if(Build.VERSION.SDK_INT>=23&&group.isClickable()&&group.getForeground()==null)group.setForeground(ripple(context,theme,18f));
        if(Build.VERSION.SDK_INT>=28){
            group.setOutlineAmbientShadowColor(theme.accentDark);
            group.setOutlineSpotShadowColor(Color.BLACK);
        }
    }

    /** Keep critical leadership tools first-class even when the legacy shell no longer exposes their old cards. */
    private static void observeLeadership(Activity activity,View root){
        if(OBSERVED.put(root,Boolean.TRUE)!=null)return;
        root.getViewTreeObserver().addOnGlobalLayoutListener(()->{
            if(activity.isFinishing())return;
            try{enhanceLeadership(activity,root,FactionTheme.forContext(activity));PremiumActionPolish.apply(activity,root);}catch(Exception ignored){}
        });
    }

    private static void enhanceLeadership(Activity activity,View root,FactionTheme theme){
        if(DeveloperPreviewStore.isMemberPreview(activity))return;
        TextView command=findExact(root,"Command center");
        if(command==null)return;
        ViewParent headerParent=command.getParent();
        if(!(headerParent instanceof View))return;
        ViewParent pageParent=((View)headerParent).getParent();
        if(!(pageParent instanceof LinearLayout))return;
        LinearLayout page=(LinearLayout)pageParent;

        if(findExact(page,"Activity Tracker")==null){
            int warIndex=findDirectIndexContaining(page,"WAR & OC");
            int insertAt=warIndex>=0?warIndex:Math.max(0,page.getChildCount()-1);
            TextView section=leadershipEyebrow(activity,"MEMBER OPERATIONS",theme);
            LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=Math.round(dp(activity,5));sp.bottomMargin=Math.round(dp(activity,8));
            page.addView(section,insertAt++,sp);
            LinearLayout row=new LinearLayout(activity);row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout activityCard=leadershipCard(activity,"ACTIVITY","Activity Tracker","Faction-wide participation and activity scan.",theme.accent,()->openFeature(activity,FeatureRouterActivity.TARGET_ACTIVITY));
            LinearLayout pulseCard=leadershipCard(activity,"READINESS","Faction Pulse","Member health, inactivity and availability at a glance.",Color.rgb(76,190,102),()->openFeature(activity,FeatureRouterActivity.TARGET_PULSE));
            LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,Math.round(dp(activity,132)),1f);
            LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,Math.round(dp(activity,132)),1f);right.leftMargin=Math.round(dp(activity,10));
            row.addView(activityCard,left);row.addView(pulseCard,right);
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Math.round(dp(activity,132)));rp.bottomMargin=Math.round(dp(activity,14));
            page.addView(row,insertAt,rp);
        }

        if(findExact(page,"Armory Auditor")==null){
            LinearLayout armory=leadershipCard(activity,"FACTION OPERATIONS","Armory Auditor","Audit faction armory items, member totals, deposits, restocks and detailed activity.",Color.rgb(76,190,102),()->openArmory(activity));
            LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Math.round(dp(activity,118)));ap.bottomMargin=Math.round(dp(activity,12));
            int index=Math.max(0,page.getChildCount()-1);
            page.addView(armory,index,ap);
        }
    }

    private static LinearLayout leadershipCard(Activity activity,String eye,String title,String body,int accent,Runnable action){
        FactionTheme theme=FactionTheme.forContext(activity);
        LinearLayout card=new LinearLayout(activity);card.setOrientation(LinearLayout.VERTICAL);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(Math.round(dp(activity,16)),Math.round(dp(activity,13)),Math.round(dp(activity,16)),Math.round(dp(activity,13)));card.setClickable(true);card.setFocusable(true);
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{theme.surface,Color.rgb(8,13,20)});bg.setCornerRadius(dp(activity,20));bg.setStroke(Math.round(dp(activity,1)),accent);card.setBackground(bg);
        TextView eyebrow=leadershipEyebrow(activity,eye,theme);eyebrow.setTextColor(accent);card.addView(eyebrow);
        TextView heading=new TextView(activity);heading.setText(title);heading.setTextColor(Color.rgb(246,248,251));heading.setTextSize(17);heading.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);hp.topMargin=Math.round(dp(activity,5));card.addView(heading,hp);
        TextView copy=new TextView(activity);copy.setText(body);copy.setTextColor(Color.rgb(145,155,169));copy.setTextSize(11.5f);copy.setMaxLines(2);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);bp.topMargin=Math.round(dp(activity,4));card.addView(copy,bp);
        card.setOnClickListener(v->action.run());polishContainer(activity,card,theme);return card;
    }

    private static TextView leadershipEyebrow(Activity activity,String value,FactionTheme theme){TextView t=new TextView(activity);t.setText(value);t.setTextColor(theme.accent);t.setTextSize(9.5f);t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));if(Build.VERSION.SDK_INT>=21)t.setLetterSpacing(.12f);return t;}
    private static int findDirectIndexContaining(LinearLayout parent,String needle){for(int i=0;i<parent.getChildCount();i++)if(containsText(parent.getChildAt(i),needle))return i;return-1;}
    private static boolean containsText(View view,String needle){if(view instanceof TextView){CharSequence raw=((TextView)view).getText();if(raw!=null&&raw.toString().contains(needle))return true;}if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)if(containsText(g.getChildAt(i),needle))return true;}return false;}
    private static TextView findExact(View view,String exact){if(view instanceof TextView){CharSequence raw=((TextView)view).getText();if(raw!=null&&exact.equals(raw.toString()))return(TextView)view;}if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++){TextView found=findExact(g.getChildAt(i),exact);if(found!=null)return found;}}return null;}
    private static void openFeature(Activity activity,String target){Intent i=new Intent(activity,FeatureRouterActivity.class);i.putExtra(FeatureRouterActivity.EXTRA_TARGET,target);activity.startActivity(i);}
    private static void openArmory(Activity activity){Intent i=new Intent(activity,ToolHostActivity.class);i.putExtra(ToolHostActivity.EXTRA_TOOL,"ARMORY");activity.startActivity(i);}

    private static RippleDrawable ripple(Context context,FactionTheme theme,float radiusDp){
        int rippleColor=Color.argb(44,Color.red(theme.accent),Color.green(theme.accent),Color.blue(theme.accent));
        GradientDrawable mask=new GradientDrawable();mask.setColor(Color.WHITE);mask.setCornerRadius(dp(context,radiusDp));
        return new RippleDrawable(ColorStateList.valueOf(rippleColor),null,mask);
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
                .replace("capped at roughly 12 requests/minute","capped at roughly 30 direct requests/minute")
                .replace("v0.9.6","v0.9.20")
                .replace("v0.9.7","v0.9.20")
                .replace("v0.9.8","v0.9.20")
                .replace("v0.9.9","v0.9.20")
                .replace("v0.9.10","v0.9.20")
                .replace("v0.9.11","v0.9.20")
                .replace("v0.9.12","v0.9.20")
                .replace("v0.9.13","v0.9.20")
                .replace("v0.9.14","v0.9.20")
                .replace("v0.9.15","v0.9.20")
                .replace("v0.9.16","v0.9.20")
                .replace("v0.9.17","v0.9.20")
                .replace("v0.9.18","v0.9.20")
                .replace("v0.9.19","v0.9.20");
        branded=branded.replaceAll("v0\\.9\\.\\d+","v"+VERSION);
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
