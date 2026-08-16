package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
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

/** Product planning screen only. No premium sale/unlock is active in the beta. */
public class PremiumPreviewActivity extends Activity {
    private static final int BG=Color.rgb(6,9,13),PANEL=Color.rgb(14,20,29),PANEL2=Color.rgb(9,14,21),BORDER=Color.rgb(45,55,69),TEXT=Color.rgb(244,246,249),MUTED=Color.rgb(154,164,178),GOLD=Color.rgb(241,194,106),BLUE=Color.rgb(88,166,255),GOOD=Color.rgb(63,185,80);
    @Override protected void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);render();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(TEXT);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL2,BORDER,11));return b;}
    private LinearLayout card(String eyebrow,String title,String body,int stroke){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(14),dp(16),dp(14));c.setBackground(rounded(PANEL,stroke,16));TextView e=text(eyebrow,9,stroke,true);e.setLetterSpacing(.10f);c.addView(e);TextView h=text(title,17,TEXT,true);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);hp.topMargin=dp(4);c.addView(h,hp);TextView b=text(body,13,MUTED,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(5);c.addView(b,p);return c;}
    private void addCard(LinearLayout root,LinearLayout c){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(9);root.addView(c,p);}
    @SuppressWarnings("deprecation") private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);int l=dp(16),t=dp(18),r=dp(16),b=dp(30);s.setPadding(l,t,r,b);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),b+i.getSystemWindowInsetBottom());return i;});return s;}
    private void render(){ScrollView s=shell();LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));Button back=button("← More");back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(dp(110),dp(44)));TextView title=text("Premium Roadmap",29,TEXT,true);title.setGravity(Gravity.CENTER);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp(16);r.addView(title,tp);TextView state=text("v0.9.5 planning only • nothing is for sale yet",12,GOLD,true);state.setGravity(Gravity.CENTER);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(4);sp.bottomMargin=dp(14);r.addView(state,sp);
        addCard(r,card("FREE CORE","Faction essentials","Home attention summary, personal OC/war participation, chain status, faction directory, notices, banking requests, basic leadership workflows, Member-safe tools and basic FFScouter integration should remain free. A faction member should not have to pay just to participate properly.",GOOD));
        addCard(r,card("PREMIUM CANDIDATE","Automation & alerts","Smart push alerts, configurable reminders, recurring exception monitoring, advanced While You Were Away digests and automated follow-up queues.",GOLD));
        addCard(r,card("PREMIUM CANDIDATE","Advanced analytics","Longer history, participation trends, completed-war analytics, comparison views, readiness scoring and deeper leadership reporting.",GOLD));
        addCard(r,card("PREMIUM CANDIDATE","Exports & workflow power","Copy-ready reports, CSV/export packages, payout analytics, advanced audit history and higher-level workflow automation.",BLUE));
        addCard(r,card("KEEP PROVIDER FAIR","FFScouter","A player's basic FFScouter connection should not be double-paywalled by Duck Force Companion. If FFScouter itself marks data/features as premium, the app should respect that player's FFScouter entitlement. Our premium can add workflow and analysis around the provider instead.",BLUE));
        addCard(r,card("FUTURE ENTITLEMENT","Player-based unlocks","If Duck Force Companion premium launches later, entitlement should bind to Torn player_id and be verified by the shared backend rather than trusting a local toggle or payment claim.",GOOD));
        addCard(r,card("NOT FOR SALE YET","1.0 comes first","Premium remains a prepared framework. Public monetization should wait until the core app, permissions, privacy disclosures and store release are stable.",BORDER));setContentView(s);s.requestApplyInsets();}
}
