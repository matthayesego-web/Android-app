package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class AboutActivity extends Activity {
    private static final int BG=Color.rgb(6,9,13),PANEL=Color.rgb(14,20,29),PANEL2=Color.rgb(9,14,21),BORDER=Color.rgb(45,55,69),TEXT=Color.rgb(244,246,249),MUTED=Color.rgb(154,164,178),GOLD=Color.rgb(241,194,106),BLUE=Color.rgb(88,166,255),GREEN=Color.rgb(63,185,80);
    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);render();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String label,int stroke){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(TEXT);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL2,stroke,12));return b;}
    private LinearLayout card(String title,String body,int stroke){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(17),dp(15),dp(17),dp(15));c.setBackground(rounded(PANEL,stroke,18));c.addView(text(title,18,TEXT,true));TextView b=text(body,13,MUTED,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(6);c.addView(b,p);return c;}
    private void add(LinearLayout r,LinearLayout c){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(10);r.addView(c,p);}
    @SuppressWarnings("deprecation") private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);int l=dp(16),t=dp(18),r=dp(16),b=dp(30);s.setPadding(l,t,r,b);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),b+i.getSystemWindowInsetBottom());return i;});return s;}
    private void render(){
        ScrollView s=shell();LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));Button back=button("← More",BORDER);back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(dp(110),dp(44)));
        TextView brand=text("TORNFCA • FACTION COMPANION",10,GOLD,true);brand.setLetterSpacing(.11f);brand.setGravity(Gravity.CENTER);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);bp.topMargin=dp(18);r.addView(brand,bp);TextView title=text("About",30,TEXT,true);title.setGravity(Gravity.CENTER);r.addView(title);TextView version=text("TornFCA v0.9.13 • pre-release audit build",13,MUTED,false);version.setGravity(Gravity.CENTER);LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);vp.topMargin=dp(4);vp.bottomMargin=dp(18);r.addView(version,vp);
        add(r,card("What TornFCA is","TornFCA is a multi-faction companion and operating layer designed to reduce faction administration, organize member information, improve war/OC/banking workflows and surface what needs attention. It is not intended to replace Torn or Torn PDA.",GOLD));
        add(r,card("One-key access model","The recommended key is one standard 16-character Limited Access Torn API key. That same key is compatible with TornFCA, FFScouter core scouting and TornStats. Full Access is not required. Private faction-wide features additionally require Torn's in-game Faction API Access permission on the player's faction position; that is a faction permission, not a stronger API-key level.",GREEN));
        add(r,card("Torn API use & request safety","The key is AES-GCM encrypted on-device with Android Keystore. Direct Torn calls are serialized, cached and capped at roughly 12 requests/minute from the Android app. Permanent invalid/access errors are not retried; only transient/rate-limit failures receive bounded backoff. Embedded tools use the same native request boundary and never receive the real key in JavaScript.",GREEN));
        add(r,card("Shared TornFCA backend","Shared notices, banking and permission resolution may temporarily transmit the player's key over HTTPS to the configured TornFCA faction backend so it can verify identity and faction permissions against Torn. The backend does not persist the API key. Shared application records are scoped by faction_id and player_id.",GOLD));
        add(r,card("Optional intelligence providers","FFScouter and TornStats are separate third-party services. TornFCA does not contact either provider until the player explicitly opts in. Their terms/data-policy links are shown before enablement. Provider requests are locally throttled/cached, and disabling a provider stops TornFCA from sending the key to it. Disabling TornFCA access does not itself delete a key already registered/stored by that provider; use the provider's own controls for that.",BLUE));
        add(r,card("Banking Companion privacy","The optional Torn/TornPDA banking userscript makes no Torn API calls. It operates only during an active Torn session while faction chat is open and recently interacted with, and sends only messages matching banking-request patterns plus the minimum requester/amount/deduplication metadata. It is not designed to upload ordinary faction conversation.",BLUE));
        add(r,card("Premium entitlement data","Premium status is keyed to Torn player_id and verified by a protected backend. The Android app does not claim payments. The backend's optional item-receipt monitor uses a dedicated owner key and is designed for one Torn log request per minute.",GOLD));
        add(r,card("Android security","Only Internet permission is requested. Cleartext traffic is disabled, application backup is disabled, non-launcher activities are not exported, and saved Torn keys remain encrypted locally rather than being written in plaintext preferences.",GREEN));
        Button ff=button("Open FFScouter Terms / Data Policy",BLUE);ff.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(FFScouterClient.HOMEPAGE))));LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48));fp.topMargin=dp(2);r.addView(ff,fp);
        Button ts=button("Open TornStats Terms",BLUE);ts.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(TornStatsClient.TERMS_URL))));LinearLayout.LayoutParams tsp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48));tsp.topMargin=dp(8);r.addView(ts,tsp);
        TextView legal=text("TornFCA is an independent community project. Torn, FFScouter and TornStats are separate services and are not represented as being owned or operated by TornFCA.",11,MUTED,false);legal.setGravity(Gravity.CENTER);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.topMargin=dp(12);r.addView(legal,lp);setContentView(s);s.requestApplyInsets();
    }
}
