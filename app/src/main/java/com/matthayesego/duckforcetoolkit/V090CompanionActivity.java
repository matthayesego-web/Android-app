package com.matthayesego.duckforcetoolkit;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** v0.9 shell overlay: keeps the tested v0.8 navigation and adds automation entry points. */
public class V090CompanionActivity extends PolishedCompanionActivity {
    private static final int PANEL=Color.rgb(15,20,28), BORDER=Color.rgb(45,55,69), TEXT=Color.rgb(244,246,249), MUTED=Color.rgb(154,164,178), GOLD=Color.rgb(241,194,106);

    @Override public void setContentView(View view) {
        super.setContentView(view);
        stampVersion(view);
        attachAutomationObserver(view);
    }

    private int dp090(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded090(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp090(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp090(1),stroke);return d;}

    private void stampVersion(View view){
        if(view instanceof TextView){TextView t=(TextView)view;CharSequence raw=t.getText();if(raw!=null){String v=raw.toString().replace("v0.8.0","v0.9.0");if(!v.equals(raw.toString()))t.setText(v);}}
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)stampVersion(g.getChildAt(i));}
    }

    private void attachAutomationObserver(View root){
        if(root==null||"v090-observed".equals(root.getTag()))return;
        root.setTag("v090-observed");
        root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
            @Override public void onGlobalLayout(){stampVersion(root);injectAttentionCard(root);}
        });
        injectAttentionCard(root);
    }

    private void injectAttentionCard(View root){
        if(!containsText090(root,"Leadership"))return;
        if(containsText090(root,"Leadership Attention"))return;
        TextView section=findText090(root,"WHAT NEEDS MY ATTENTION?");
        if(section==null||!(section.getParent() instanceof LinearLayout))return;
        LinearLayout parent=(LinearLayout)section.getParent();
        int index=parent.indexOfChild(section)+1;

        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp090(14),dp090(12),dp090(14),dp090(12));card.setBackground(rounded090(PANEL,GOLD,15));card.setClickable(true);card.setFocusable(true);
        TextView title=new TextView(this);title.setText("Leadership Attention");title.setTextColor(TEXT);title.setTextSize(16);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);card.addView(title);
        TextView body=new TextView(this);body.setText("Who needs attention and why — live exceptions from faction data");body.setTextColor(MUTED);body.setTextSize(12);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);bp.topMargin=dp090(4);card.addView(body,bp);
        card.setOnClickListener(v->startActivity(new Intent(this,LeadershipAttentionActivity.class)));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp090(88));p.bottomMargin=dp090(9);parent.addView(card,Math.min(index,parent.getChildCount()),p);
    }

    private boolean containsText090(View view,String needle){
        if(view instanceof TextView){CharSequence raw=((TextView)view).getText();if(raw!=null&&raw.toString().contains(needle))return true;}
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)if(containsText090(g.getChildAt(i),needle))return true;}
        return false;
    }

    private TextView findText090(View view,String exact){
        if(view instanceof TextView){CharSequence raw=((TextView)view).getText();if(raw!=null&&exact.equals(raw.toString()))return (TextView)view;}
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++){TextView found=findText090(g.getChildAt(i),exact);if(found!=null)return found;}}
        return null;
    }
}
