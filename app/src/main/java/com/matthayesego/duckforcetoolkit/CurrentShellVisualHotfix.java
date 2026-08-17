package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.app.Application;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Torn FCA Beta visual system.
 *
 * The overhaul is deliberately runtime-gated to the .beta application ID so the normal TornFCA
 * package stays visually frozen at the tested v0.9.31 fallback while this side-by-side build is
 * evaluated. Routing, API behavior, access checks and persisted feature logic are not changed here.
 */
public final class CurrentShellVisualHotfix {
    private static final String NAV_PREFIX="current-nav:";
    private static final String ACTION_TAG="tornfca-premium-action-label";

    private static final int BG=Color.rgb(5,8,13);
    private static final int BG_2=Color.rgb(8,12,19);
    private static final int SURFACE=Color.rgb(13,19,28);
    private static final int SURFACE_2=Color.rgb(9,14,22);
    private static final int SURFACE_3=Color.rgb(20,29,41);
    private static final int BORDER=Color.rgb(39,51,68);
    private static final int BORDER_SOFT=Color.rgb(29,39,53);
    private static final int TEXT=Color.rgb(245,247,250);
    private static final int MUTED=Color.rgb(145,157,175);
    private static final int STEEL=Color.rgb(104,121,145);
    private static final int GOLD=Color.rgb(232,183,92);
    private static final int GOLD_BRIGHT=Color.rgb(247,211,143);
    private static final int GOLD_DARK=Color.rgb(92,65,27);
    private static final int BLUE=Color.rgb(91,145,198);
    private static final int GREEN=Color.rgb(83,181,119);
    private static final int RED=Color.rgb(215,102,102);
    private static final int PURPLE=Color.rgb(150,119,226);

    private static final Map<View,Boolean> OBSERVED=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View,Boolean> STYLED=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View,Boolean> NAV_STATE=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View,Boolean> NAV_BARS=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View,Boolean> ANIMATED=Collections.synchronizedMap(new WeakHashMap<>());
    private static boolean installed;

    private CurrentShellVisualHotfix(){}

    public static synchronized void install(Application application){
        if(application==null||installed||!isBetaBuild())return;
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

    private static boolean isBetaBuild(){
        return BuildConfig.APPLICATION_ID!=null&&BuildConfig.APPLICATION_ID.endsWith(".beta");
    }

    @SuppressWarnings("deprecation")
    private static void prepareWindow(Activity activity){
        if(activity==null)return;
        Window w=activity.getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        if(android.os.Build.VERSION.SDK_INT>=28)w.setNavigationBarDividerColor(BORDER_SOFT);
        int flags=w.getDecorView().getSystemUiVisibility();
        flags&=~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        flags&=~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        w.getDecorView().setSystemUiVisibility(flags);
    }

    private static void attach(Activity activity){
        if(activity==null||!isBetaBuild())return;
        prepareWindow(activity);
        View root=activity.findViewById(android.R.id.content);
        if(root==null)return;
        polish(activity,root);
        animateRoot(activity,root);
        if(OBSERVED.put(root,Boolean.TRUE)!=null)return;
        root.getViewTreeObserver().addOnGlobalLayoutListener(()->{
            if(activity.isFinishing())return;
            try{polish(activity,root);}catch(Exception ignored){}
        });
    }

    private static void polish(Activity activity,View view){
        if(view==null)return;

        if(view instanceof TextView)sanitizeText((TextView)view);
        if(view instanceof LinearLayout)removeDuplicateAction((LinearLayout)view);
        if(view instanceof TextView&&isCurrentNav((TextView)view)){
            polishCurrentNavItem(activity,(TextView)view);
        }else if(STYLED.put(view,Boolean.TRUE)==null){
            if(view instanceof EditText)styleEditText(activity,(EditText)view);
            else if(view instanceof CompoundButton)styleCompoundButton(activity,(CompoundButton)view);
            else if(view instanceof Button)styleButton(activity,(Button)view);
            else if(view instanceof TextView)styleText(activity,(TextView)view);
            else if(view instanceof ImageView)styleImage(activity,(ImageView)view);
            else if(view instanceof ProgressBar)styleProgress((ProgressBar)view);
            else if(view instanceof ScrollView)styleScroll((ScrollView)view);

            if(view instanceof LinearLayout)styleLayout(activity,(LinearLayout)view);
        }

        if(view instanceof ViewGroup){
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)polish(activity,group.getChildAt(i));
        }
    }

    private static void sanitizeText(TextView text){
        CharSequence raw=text.getText();
        if(raw!=null){
            String cleaned=sanitize(raw.toString());
            if(!cleaned.equals(raw.toString()))text.setText(cleaned);
        }
        CharSequence hint=text.getHint();
        if(hint!=null){
            String cleaned=sanitize(hint.toString());
            if(!cleaned.equals(hint.toString()))text.setHint(cleaned);
        }
        CharSequence description=text.getContentDescription();
        if(description!=null){
            String cleaned=sanitize(description.toString());
            if(!cleaned.equals(description.toString()))text.setContentDescription(cleaned);
        }
    }

    private static String sanitize(String value){
        if(value==null||value.isEmpty())return value==null?"":value;
        String out=value
                .replace("Duck Force Companion","Torn FCA")
                .replace("DUCK FORCE COMPANION","TORN FCA")
                .replace("Duck Force","Faction")
                .replace("DUCK FORCE","FACTION")
                .replace("duck force","faction")
                .replace("DuckForce","Faction")
                .replace("DUCKFORCE","FACTION")
                .replace("Torn Faction Companion App","Torn FCA")
                .replace("TORNFCA","TORN FCA")
                .replace("TornFCA","Torn FCA");
        if(out.startsWith("TORN FCA • v"))out="TORN FCA BETA • v"+out.substring("TORN FCA • v".length());
        if(out.startsWith("Torn FCA • v"))out="Torn FCA Beta • v"+out.substring("Torn FCA • v".length());
        return out;
    }

    private static void styleText(Activity activity,TextView text){
        String value=value(text);
        float sp=text.getTextSize()/activity.getResources().getDisplayMetrics().scaledDensity;

        if(ACTION_TAG.equals(text.getTag())||value.contains("→")){
            int accent=semanticAccent(parentText(text));
            text.setTextColor(accent);
            text.setTextSize(11.5f);
            text.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
            text.setLetterSpacing(.01f);
            text.setPadding(0,dp(activity,3),0,0);
            return;
        }

        if(looksLikeEyebrow(value)){
            text.setTextColor(GOLD_BRIGHT);
            text.setTextSize(Math.max(10.2f,Math.min(sp,11f)));
            text.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
            text.setLetterSpacing(.14f);
            text.setLineSpacing(0f,1.0f);
            return;
        }

        if(sp>=24f){
            text.setTextColor(TEXT);
            text.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
            text.setLetterSpacing(-.012f);
            text.setLineSpacing(0f,1.01f);
            return;
        }

        if(sp>=17f){
            text.setTextColor(TEXT);
            if(text.getTypeface()!=null&&text.getTypeface().isBold())text.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
            text.setLineSpacing(0f,1.04f);
            return;
        }

        if(sp<=13.5f&&!isAccentColor(text.getCurrentTextColor())){
            text.setTextColor(MUTED);
            text.setLineSpacing(0f,1.12f);
        }else if(!isAccentColor(text.getCurrentTextColor())){
            text.setTextColor(TEXT);
        }
    }

    private static void styleButton(Activity activity,Button button){
        String label=value(button);
        int accent=buttonAccent(label);
        button.setAllCaps(false);
        button.setTextColor(TEXT);
        button.setTextSize(13.2f);
        button.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(activity,46));
        button.setPadding(dp(activity,16),0,dp(activity,16),0);
        button.setLetterSpacing(.008f);
        button.setBackground(outlineGradient(activity,accent,14));
        button.setElevation(dp(activity,label.startsWith("←")?1:3));
        button.setForeground(ripple(activity,accent,14));
        if(label.startsWith("←"))button.setTextColor(Color.rgb(190,200,214));
    }

    private static void styleEditText(Activity activity,EditText edit){
        edit.setTextColor(TEXT);
        edit.setHintTextColor(Color.rgb(101,115,135));
        edit.setTextSize(14f);
        edit.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        edit.setPadding(dp(activity,14),dp(activity,11),dp(activity,14),dp(activity,11));
        edit.setBackground(outlineGradient(activity,GOLD_DARK,14));
    }

    private static void styleCompoundButton(Activity activity,CompoundButton control){
        control.setTextColor(TEXT);
        control.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        control.setButtonTintList(ColorStateList.valueOf(GOLD));
        control.setPadding(control.getPaddingLeft(),dp(activity,4),control.getPaddingRight(),dp(activity,4));
    }

    private static void styleProgress(ProgressBar progress){
        progress.setProgressTintList(ColorStateList.valueOf(GOLD));
        progress.setIndeterminateTintList(ColorStateList.valueOf(GOLD));
    }

    private static void styleScroll(ScrollView scroll){
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setClipToPadding(false);
    }

    private static void styleImage(Activity activity,ImageView image){
        boolean profile="tornfca-profile-avatar".equals(image.getTag());
        boolean legacy=false;
        try{
            Drawable current=image.getDrawable();
            Drawable old=activity.getDrawable(R.drawable.duckforce_noir_art);
            legacy=current!=null&&old!=null&&current.getConstantState()!=null&&old.getConstantState()!=null
                    &&current.getConstantState().equals(old.getConstantState());
        }catch(Exception ignored){}
        if(profile||legacy)image.setImageResource(R.drawable.tornfca_beta_crest);
        if(profile){
            GradientDrawable halo=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{SURFACE_3,SURFACE});
            halo.setShape(GradientDrawable.OVAL);
            halo.setStroke(dp(activity,2),GOLD);
            image.setBackground(halo);
            image.setPadding(dp(activity,8),dp(activity,8),dp(activity,8),dp(activity,8));
            image.setElevation(dp(activity,7));
        }
    }

    private static void styleLayout(Activity activity,LinearLayout layout){
        Object rawTag=layout.getTag();
        if(rawTag instanceof String&&((String)rawTag).startsWith("nav:")){
            styleLegacyNav(activity,layout);
            return;
        }
        if(isHeroHeader(layout)){
            styleHeroHeader(activity,layout);
            return;
        }
        if(looksLikeCard(layout))styleCard(activity,layout);
        else if(layout.getBackground() instanceof GradientDrawable&&layout.getChildCount()>0)stylePanel(activity,layout);
    }

    private static boolean isHeroHeader(LinearLayout layout){
        for(int i=0;i<layout.getChildCount();i++){
            View child=layout.getChildAt(i);
            if(child instanceof ImageView&&"tornfca-profile-avatar".equals(child.getTag()))return true;
        }
        return false;
    }

    private static void styleHeroHeader(Activity activity,LinearLayout header){
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(22,29,39),Color.rgb(8,12,18)});
        bg.setCornerRadius(dp(activity,24));
        bg.setStroke(dp(activity,1),Color.argb(190,Color.red(GOLD),Color.green(GOLD),Color.blue(GOLD)));
        header.setBackground(bg);
        header.setPadding(dp(activity,16),dp(activity,15),dp(activity,16),dp(activity,15));
        header.setElevation(dp(activity,6));
        ViewGroup.LayoutParams raw=header.getLayoutParams();
        if(raw instanceof ViewGroup.MarginLayoutParams){
            ViewGroup.MarginLayoutParams lp=(ViewGroup.MarginLayoutParams)raw;
            lp.bottomMargin=Math.max(lp.bottomMargin,dp(activity,17));
            header.setLayoutParams(lp);
        }
    }

    private static boolean looksLikeCard(LinearLayout layout){
        if(!layout.isClickable()||layout.getChildCount()<1)return false;
        Object tag=layout.getTag();
        if(tag instanceof String&&(((String)tag).startsWith("nav:")||((String)tag).startsWith(NAV_PREFIX)))return false;
        return containsText(layout);
    }

    private static void styleCard(Activity activity,LinearLayout card){
        int accent=semanticAccent(flattenText(card));
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{SURFACE,SURFACE_2});
        bg.setCornerRadius(dp(activity,22));
        bg.setStroke(dp(activity,1),Color.argb(178,Color.red(accent),Color.green(accent),Color.blue(accent)));
        card.setBackground(bg);
        card.setPadding(dp(activity,18),dp(activity,16),dp(activity,18),dp(activity,16));
        card.setMinimumHeight(dp(activity,106));
        card.setElevation(dp(activity,4));
        card.setForeground(ripple(activity,accent,22));
        card.setClipToOutline(false);
    }

    private static void stylePanel(Activity activity,LinearLayout panel){
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{SURFACE_2,BG_2});
        bg.setCornerRadius(dp(activity,20));
        bg.setStroke(dp(activity,1),BORDER_SOFT);
        panel.setBackground(bg);
        panel.setElevation(dp(activity,1));
    }

    private static boolean isCurrentNav(TextView item){
        Object raw=item.getTag();
        return raw instanceof String&&((String)raw).startsWith(NAV_PREFIX);
    }

    private static void polishCurrentNavItem(Activity activity,TextView item){
        String tag=(String)item.getTag();
        String label=tag.substring(NAV_PREFIX.length());
        boolean selected=item.getBackground()!=null;
        Boolean previous=NAV_STATE.put(item,selected);
        if(previous!=null&&previous==selected)return;

        int icon=iconFor(label);
        Drawable drawable=icon==0?null:activity.getDrawable(icon);
        if(drawable!=null){
            drawable=drawable.mutate();
            drawable.setTint(selected?GOLD_BRIGHT:STEEL);
        }
        item.setCompoundDrawablesWithIntrinsicBounds(null,drawable,null,null);
        item.setCompoundDrawablePadding(dp(activity,4));
        item.setGravity(Gravity.CENTER);
        item.setSingleLine(true);
        item.setText(selected?label:"");
        item.setTextSize(selected?9.4f:1f);
        item.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        item.setTextColor(selected?GOLD_BRIGHT:STEEL);
        item.setContentDescription(selected?label+", selected":label);
        item.setPadding(dp(activity,4),dp(activity,7),dp(activity,4),dp(activity,5));
        ViewGroup.LayoutParams raw=item.getLayoutParams();
        if(raw!=null&&raw.height!=dp(activity,68)){
            raw.height=dp(activity,68);
            item.setLayoutParams(raw);
        }
        item.setBackground(selected?selectedNavBackground(activity):null);

        if(item.getParent() instanceof LinearLayout)polishNavBar(activity,(LinearLayout)item.getParent());
    }

    private static void polishNavBar(Activity activity,LinearLayout nav){
        if(NAV_BARS.put(nav,Boolean.TRUE)!=null)return;
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(14,20,29),Color.rgb(6,10,16)});
        bg.setCornerRadius(dp(activity,24));
        bg.setStroke(dp(activity,1),Color.rgb(47,60,78));
        nav.setBackground(bg);
        nav.setPadding(dp(activity,7),dp(activity,7),dp(activity,7),Math.max(nav.getPaddingBottom(),dp(activity,7)));
        nav.setElevation(dp(activity,12));
        ViewGroup.LayoutParams raw=nav.getLayoutParams();
        if(raw instanceof ViewGroup.MarginLayoutParams){
            ViewGroup.MarginLayoutParams lp=(ViewGroup.MarginLayoutParams)raw;
            lp.leftMargin=dp(activity,10);
            lp.rightMargin=dp(activity,10);
            lp.topMargin=dp(activity,4);
            lp.bottomMargin=Math.max(lp.bottomMargin,dp(activity,8));
            nav.setLayoutParams(lp);
        }
    }

    private static void styleLegacyNav(Activity activity,LinearLayout item){
        boolean selected=item.getBackground()!=null;
        int tint=selected?GOLD_BRIGHT:STEEL;
        for(int i=0;i<item.getChildCount();i++){
            View child=item.getChildAt(i);
            if(child instanceof ImageView)((ImageView)child).setColorFilter(tint);
            if(child instanceof TextView)((TextView)child).setTextColor(tint);
        }
        item.setBackground(selected?selectedNavBackground(activity):null);
    }

    private static GradientDrawable selectedNavBackground(Activity activity){
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(53,39,18),Color.rgb(18,20,24)});
        bg.setCornerRadius(dp(activity,17));
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
            if(child instanceof TextView&&ACTION_TAG.equals(child.getTag())){tagged=(TextView)child;break;}
        }
        if(tagged==null)return;
        for(int i=card.getChildCount()-1;i>=0;i--){
            View child=card.getChildAt(i);
            if(child==tagged||!(child instanceof TextView)||child instanceof Button)continue;
            CharSequence raw=((TextView)child).getText();
            if(raw!=null&&raw.toString().contains("→"))card.removeViewAt(i);
        }
    }

    private static void animateRoot(Activity activity,View root){
        if(ANIMATED.put(root,Boolean.TRUE)!=null)return;
        root.setAlpha(.93f);
        root.setTranslationY(dp(activity,7));
        root.animate().alpha(1f).translationY(0f).setDuration(230L).start();
    }

    private static GradientDrawable outlineGradient(Activity activity,int accent,int radius){
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{SURFACE_3,SURFACE_2});
        bg.setCornerRadius(dp(activity,radius));
        bg.setStroke(dp(activity,1),accent);
        return bg;
    }

    private static RippleDrawable ripple(Activity activity,int accent,int radius){
        int rippleColor=Color.argb(45,Color.red(accent),Color.green(accent),Color.blue(accent));
        GradientDrawable mask=new GradientDrawable();
        mask.setColor(Color.WHITE);
        mask.setCornerRadius(dp(activity,radius));
        return new RippleDrawable(ColorStateList.valueOf(rippleColor),null,mask);
    }

    private static int buttonAccent(String label){
        String key=label==null?"":label.toLowerCase(Locale.US);
        if(key.startsWith("←"))return BORDER;
        if(key.contains("delete")||key.contains("remove")||key.contains("clear")||key.contains("reset")||key.contains("logout")||key.contains("revoke"))return RED;
        if(key.contains("save")||key.contains("accept")||key.contains("continue")||key.contains("connect")||key.contains("enable"))return GOLD;
        return Color.rgb(80,105,137);
    }

    private static int semanticAccent(String text){
        String key=text==null?"":text.toLowerCase(Locale.US);
        if(key.contains("war")||key.contains("attack")||key.contains("territor"))return RED;
        if(key.contains("training")||key.contains("progress")||key.contains("guide"))return PURPLE;
        if(key.contains("chain")||key.contains("ready")||key.contains("success")||key.contains("armory"))return GREEN;
        if(key.contains("member")||key.contains("faction")||key.contains("intel")||key.contains("bank")||key.contains("settings"))return BLUE;
        if(key.contains("leadership")||key.contains("priority")||key.contains("premium")||key.contains("today")||key.contains("my day"))return GOLD;
        return GOLD;
    }

    private static boolean looksLikeEyebrow(String value){
        if(value==null||value.length()<2||value.length()>64)return false;
        boolean letter=false;
        for(int i=0;i<value.length();i++){
            char c=value.charAt(i);
            if(Character.isLetter(c)){
                letter=true;
                if(Character.isLowerCase(c))return false;
            }
        }
        return letter;
    }

    private static boolean isAccentColor(int color){
        return color==GOLD||color==GOLD_BRIGHT||color==BLUE||color==GREEN||color==RED||color==PURPLE
                ||color==TornFcaUi.GOLD||color==TornFcaUi.BLUE||color==TornFcaUi.GREEN||color==TornFcaUi.RED||color==TornFcaUi.PURPLE;
    }

    private static boolean containsText(ViewGroup group){
        for(int i=0;i<group.getChildCount();i++){
            View child=group.getChildAt(i);
            if(child instanceof TextView&&!value((TextView)child).isEmpty())return true;
            if(child instanceof ViewGroup&&containsText((ViewGroup)child))return true;
        }
        return false;
    }

    private static String flattenText(ViewGroup group){
        StringBuilder out=new StringBuilder();
        collectText(group,out);
        return out.toString();
    }

    private static void collectText(View view,StringBuilder out){
        if(view instanceof TextView){
            String value=value((TextView)view);
            if(!value.isEmpty())out.append(' ').append(value);
        }
        if(view instanceof ViewGroup){
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)collectText(group.getChildAt(i),out);
        }
    }

    private static String parentText(View view){
        if(view.getParent() instanceof ViewGroup)return flattenText((ViewGroup)view.getParent());
        return value(view instanceof TextView?(TextView)view:null);
    }

    private static String value(TextView text){
        if(text==null||text.getText()==null)return "";
        return text.getText().toString().trim();
    }

    private static int dp(Activity activity,int value){
        return Math.round(value*activity.getResources().getDisplayMetrics().density);
    }
}
