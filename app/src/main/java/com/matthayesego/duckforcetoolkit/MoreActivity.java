package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

/** Secondary navigation hub kept intentionally small so the main dashboard stays focused. */
public class MoreActivity extends Activity {
    private static final int BG=Color.rgb(6,9,13),PANEL=Color.rgb(14,20,29),PANEL2=Color.rgb(9,14,21),BORDER=Color.rgb(45,55,69),TEXT=Color.rgb(244,246,249),MUTED=Color.rgb(154,164,178),GOLD=Color.rgb(241,194,106),BLUE=Color.rgb(88,166,255),GREEN=Color.rgb(63,185,80),RED=Color.rgb(239,88,82);
    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);render();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private GradientDrawable gradient(int a,int b,int stroke,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String label,int stroke){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(TEXT);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL2,stroke,12));return b;}
    private LinearLayout card(String eyebrow,String title,String body,int stroke){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(17),dp(15),dp(17),dp(15));c.setBackground(rounded(PANEL,stroke,18));TextView e=text(eyebrow,9,stroke,true);e.setLetterSpacing(.10f);c.addView(e);TextView h=text(title,19,TEXT,true);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);hp.topMargin=dp(4);c.addView(h,hp);TextView b=text(body,13,MUTED,false);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);bp.topMargin=dp(5);c.addView(b,bp);return c;}
    private void add(LinearLayout r,LinearLayout c){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(10);r.addView(c,p);}
    @SuppressWarnings("deprecation") private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);int l=dp(16),t=dp(18),r=dp(16),b=dp(30);s.setPadding(l,t,r,b);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),b+i.getSystemWindowInsetBottom());return i;});return s;}

    private void render(){
        ScrollView s=shell();LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        Button back=button("← Companion",BORDER);back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(dp(132),dp(44)));
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER_HORIZONTAL);hero.setPadding(dp(18),dp(18),dp(18),dp(18));hero.setBackground(gradient(Color.rgb(27,38,53),Color.rgb(11,16,23),BORDER,22));TextView brand=text("TORNFCA",10,GOLD,true);brand.setLetterSpacing(.16f);hero.addView(brand);TextView title=text("More",30,TEXT,true);hero.addView(title);TextView sub=text("Settings, account access and app information",13,MUTED,false);sub.setGravity(Gravity.CENTER);hero.addView(sub);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);hp.topMargin=dp(14);hp.bottomMargin=dp(14);r.addView(hero,hp);

        SecureApiKeyStore store=new SecureApiKeyStore(this);long expiry=store.persistedUntilMillis();
        String keyMode=expiry>0L?"Encrypted on this device until "+DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(expiry))+". It will be removed automatically after that time.":"Session-only. This key is not being retained for a future app session.";
        LinearLayout account=card("ACCOUNT & SECURITY","API key session",keyMode,GREEN);
        Button logout=button("Log Out / Change API Key",RED);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48));lp.topMargin=dp(12);account.addView(logout,lp);logout.setOnClickListener(v->logout());add(r,account);

        LinearLayout premium=card("PREMIUM ROADMAP","Free vs Premium","Core faction work stays free. Premium is reserved for automation, deeper analytics, long history, exports and convenience—not basic faction participation or access.",GOLD);premium.setClickable(true);premium.setOnClickListener(v->startActivity(new Intent(this,PremiumPreviewActivity.class)));add(r,premium);
        LinearLayout about=card("APP INFORMATION","About TornFCA","Purpose, version, data sources, privacy/security boundaries, FFScouter attribution and release status.",BLUE);about.setClickable(true);about.setOnClickListener(v->startActivity(new Intent(this,AboutActivity.class)));add(r,about);
        LinearLayout safety=card("SECURITY","Your data","Torn keys are session-only unless you explicitly choose 7, 30 or 90 day encrypted device retention at login. Shared faction systems use faction_id as their tenant boundary and player_id as identity.",GREEN);add(r,safety);
        TextView footer=text("TornFCA v"+TornFcaBrand.VERSION+" • pre-release",11,MUTED,false);footer.setGravity(Gravity.CENTER);LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);fp.topMargin=dp(8);r.addView(footer,fp);setContentView(s);s.requestApplyInsets();
    }

    private void logout(){
        new SecureApiKeyStore(this).clear();FactionScopeCache.clear(this);TornApiClient.clearMemoryCache();DeveloperPreviewStore.clear(this);
        Intent i=new Intent(this,AccessGateActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);startActivity(i);finish();
    }
}
