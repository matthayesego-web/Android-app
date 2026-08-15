package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class PremiumPreviewActivity extends Activity {
    private static final int BG=Color.rgb(8,12,18), PANEL=Color.rgb(20,27,38), PANEL2=Color.rgb(27,36,49), BORDER=Color.rgb(49,63,81);
    private static final int TEXT=Color.rgb(245,248,252), MUTED=Color.rgb(151,163,179), GOLD=Color.rgb(243,184,52), BLUE=Color.rgb(88,166,255), GOOD=Color.rgb(63,185,80);

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        render();
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(TEXT);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL2,BORDER,11));return b;}
    private LinearLayout card(String title,String body,int stroke){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(14),dp(16),dp(14));c.setBackground(rounded(PANEL,stroke,16));c.addView(text(title,17,TEXT,true));TextView b=text(body,13,MUTED,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(5);c.addView(b,p);return c;}
    private void addCard(LinearLayout root,LinearLayout c){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(9);root.addView(c,p);}

    private void render(){
        ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);s.setPadding(dp(16),dp(16),dp(16),dp(28));
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        Button back=button("← Companion");back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(dp(124),dp(44)));
        TextView title=text("Premium Preview",27,TEXT,true);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp(14);r.addView(title,tp);
        TextView state=text(PremiumEntitlementStore.summary(this),12,GOLD,true);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(4);sp.bottomMargin=dp(14);r.addView(state,sp);

        addCard(r,card("Smart Alerts","Optional Android alerts for war starts, chain danger, OC ready states and personal participation reminders.",BLUE));
        addCard(r,card("Advanced Member Intel","Longer activity history, participation trends, member comparison and faster leadership follow-up.",GOLD));
        addCard(r,card("War Analytics","Zero-hit watch, participation trends, readiness snapshots and deeper completed-war reporting.",GOLD));
        addCard(r,card("History & Export","Extended history, copy-ready reports and export tools beyond the quick live dashboard.",BLUE));
        addCard(r,card("Automatic Unlock Architecture","Future entitlement will be tied to Torn player ID. A backend will verify the in-game payment/receipt first, then the app will cache the verified premium grant. No client-side payment claim will unlock features.",GOOD));
        addCard(r,card("Not For Sale Yet","Premium is only a prepared framework right now. No in-game payment is required or accepted by this build.",BORDER));
        setContentView(s);
    }
}
