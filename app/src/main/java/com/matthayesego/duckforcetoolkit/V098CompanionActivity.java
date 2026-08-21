package com.matthayesego.duckforcetoolkit;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

/** v0.9.8 shell layer over the premium navigation architecture. */
public class V098CompanionActivity extends V095CompanionActivity {
    @Override public void setContentView(View view){
        super.setContentView(view);
        ViewGroup host=findViewById(android.R.id.content);
        if(host==null)return;
        refreshCurrentUi(host);
        host.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
            @Override public void onGlobalLayout(){refreshCurrentUi(host);}
        });
    }
    private int dp98(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void refreshCurrentUi(ViewGroup host){stamp(host);ensureWarPayout(host);}
    private void stamp(View view){
        if(view instanceof TextView){TextView t=(TextView)view;CharSequence raw=t.getText();if(raw!=null){String s=raw.toString().replace("v0.9.7","v0.9.8");if(!s.equals(raw.toString()))t.setText(s);}}
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)stamp(g.getChildAt(i));}
    }
    private void ensureWarPayout(View root){
        if(findExact(root,"WAR PAYOUT")!=null)return;
        TextView section=findExact(root,"WAR & OC");
        if(section==null||!(section.getParent() instanceof LinearLayout))return;
        LinearLayout page=(LinearLayout)section.getParent();
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp98(18),dp98(14),dp98(18),dp98(14));GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(12,18,26),Color.rgb(8,13,20)});bg.setCornerRadius(dp98(20));bg.setStroke(dp98(1),Color.rgb(36,47,61));card.setBackground(bg);card.setClickable(true);card.setFocusable(true);
        TextView eye=new TextView(this);eye.setText("WAR PAYOUT");eye.setTextColor(Color.rgb(241,190,86));eye.setTextSize(9.5f);eye.setTypeface(Typeface.DEFAULT,Typeface.BOLD);eye.setLetterSpacing(.12f);card.addView(eye);
        TextView title=new TextView(this);title.setText("Leadership Payout Calculator");title.setTextColor(Color.rgb(246,248,251));title.setTextSize(19);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp98(5);card.addView(title,tp);
        TextView body=new TextView(this);body.setText("Choose a completed war, load official member participation, and calculate a reviewable payout.");body.setTextColor(Color.rgb(145,155,169));body.setTextSize(12.5f);body.setMaxLines(2);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);bp.topMargin=dp98(4);card.addView(body,bp);
        card.setOnClickListener(v->{Intent i=new Intent(this,FeatureRouterActivity.class);i.putExtra(FeatureRouterActivity.EXTRA_TARGET,FeatureRouterActivity.TARGET_WAR_PAYOUT);startActivity(i);});
        int index=Math.min(page.indexOfChild(section)+2,page.getChildCount());LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp98(118));cp.bottomMargin=dp98(12);page.addView(card,index,cp);
    }
    private TextView findExact(View view,String exact){if(view instanceof TextView){CharSequence raw=((TextView)view).getText();if(raw!=null&&exact.equals(raw.toString()))return(TextView)view;}if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++){TextView f=findExact(g.getChildAt(i),exact);if(f!=null)return f;}}return null;}
}
