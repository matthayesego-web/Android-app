package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Small shared UI kit for new TornFCA player-facing surfaces. */
public final class TornFcaUi {
    public static final int BG=Color.rgb(6,9,13),PANEL=Color.rgb(14,20,29),PANEL2=Color.rgb(9,14,21),BORDER=Color.rgb(45,55,69),TEXT=Color.rgb(244,246,249),MUTED=Color.rgb(154,164,178),GOLD=Color.rgb(241,194,106),BLUE=Color.rgb(88,166,255),GREEN=Color.rgb(63,185,80),RED=Color.rgb(239,88,82),PURPLE=Color.rgb(158,114,255);
    private TornFcaUi(){}
    public static int dp(Activity a,int v){return Math.round(v*a.getResources().getDisplayMetrics().density);}
    public static GradientDrawable rounded(Activity a,int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(a,radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(a,1),stroke);return d;}
    public static GradientDrawable gradient(Activity a,int first,int second,int stroke,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{first,second});d.setCornerRadius(dp(a,radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(a,1),stroke);return d;}
    public static TextView text(Activity a,String value,float size,int color,boolean bold){TextView t=new TextView(a);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return t;}
    public static Button button(Activity a,String label,int accent){Button b=new Button(a);b.setText(label);b.setAllCaps(false);b.setTextColor(TEXT);b.setTextSize(13.5f);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setBackground(rounded(a,PANEL2,accent,12));if(Build.VERSION.SDK_INT>=21)b.setElevation(dp(a,2));return b;}
    @SuppressWarnings("deprecation") public static ScrollView shell(Activity a){a.getWindow().setStatusBarColor(BG);a.getWindow().setNavigationBarColor(BG);ScrollView s=new ScrollView(a);s.setFillViewport(true);s.setClipToPadding(false);s.setBackgroundColor(BG);int l=dp(a,16),t=dp(a,18),r=dp(a,16),b=dp(a,30);s.setPadding(l,t,r,b);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),b+i.getSystemWindowInsetBottom());return i;});return s;}
    public static LinearLayout root(Activity a,ScrollView s){LinearLayout r=new LinearLayout(a);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));return r;}
    public static void header(Activity a,LinearLayout r,String backLabel,String title,String subtitle){
        LinearLayout actions=new LinearLayout(a);actions.setOrientation(LinearLayout.HORIZONTAL);actions.setGravity(Gravity.CENTER_VERTICAL);
        Button back=button(a,"← "+backLabel,BORDER);back.setOnClickListener(v->a.finish());actions.addView(back,new LinearLayout.LayoutParams(0,dp(a,44),1f));
        if(a instanceof FactionChatActivity){
            Button real=button(a,"Torn Chat",GREEN);real.setOnClickListener(v->a.startActivity(new Intent(a,RealTornChatActivity.class)));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(a,104),dp(a,44));cp.leftMargin=dp(a,8);actions.addView(real,cp);
        }else if(!(a instanceof RealTornChatActivity)){
            Button chat=button(a,"Chat",BLUE);chat.setOnClickListener(v->a.startActivity(new Intent(a,FactionChatActivity.class)));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(a,92),dp(a,44));cp.leftMargin=dp(a,8);actions.addView(chat,cp);
        }
        r.addView(actions,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(a,44)));
        TextView brand=text(a,"TORNFCA • FACTION COMPANION",10,GOLD,true);brand.setLetterSpacing(.11f);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);bp.topMargin=dp(a,17);r.addView(brand,bp);r.addView(text(a,title,30,TEXT,true));TextView sub=text(a,subtitle,13,MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(a,4);sp.bottomMargin=dp(a,16);r.addView(sub,sp);}
    public static LinearLayout card(Activity a,String eyebrow,String title,String body,int accent){LinearLayout c=new LinearLayout(a);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(a,17),dp(a,15),dp(a,17),dp(a,15));c.setBackground(gradient(a,PANEL,PANEL2,accent,18));if(eyebrow!=null&&!eyebrow.isEmpty()){TextView e=text(a,eyebrow.toUpperCase(),9.5f,accent,true);e.setLetterSpacing(.11f);c.addView(e);}TextView h=text(a,title,18.5f,TEXT,true);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);hp.topMargin=dp(a,4);c.addView(h,hp);if(body!=null&&!body.isEmpty()){TextView b=text(a,body,12.8f,MUTED,false);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);bp.topMargin=dp(a,5);c.addView(b,bp);}return c;}
    public static void add(Activity a,LinearLayout root,View view){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(a,10);root.addView(view,p);}
    public static TextView section(Activity a,String value){TextView t=text(a,value.toUpperCase(),10.5f,MUTED,true);t.setLetterSpacing(.12f);return t;}
    public static void addSection(Activity a,LinearLayout r,String value){TextView t=section(a,value);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(a,4);p.bottomMargin=dp(a,8);r.addView(t,p);}
    public static TextView footer(Activity a,String value){TextView t=text(a,value,10.5f,MUTED,false);t.setGravity(Gravity.CENTER);return t;}
}
