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

/** v0.9.x shell overlay: automation, hidden developer entry and safe member-preview presentation. */
public class V090CompanionActivity extends PolishedCompanionActivity {
    private static final int PANEL=Color.rgb(15,20,28), BORDER=Color.rgb(45,55,69), TEXT=Color.rgb(244,246,249), MUTED=Color.rgb(154,164,178), GOLD=Color.rgb(241,194,106), BLUE=Color.rgb(88,166,255);
    private int footerTapCount=0;
    private long lastFooterTap=0L;

    @Override public void setContentView(View view) {
        super.setContentView(view);
        stampVersion(view);
        attachAutomationObserver(view);
    }

    private int dp090(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded090(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp090(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp090(1),stroke);return d;}

    private void stampVersion(View view){
        if(view instanceof TextView){TextView t=(TextView)view;CharSequence raw=t.getText();if(raw!=null){String v=raw.toString().replace("v0.8.0","v0.9.1").replace("v0.9.0","v0.9.1");if(!v.equals(raw.toString()))t.setText(v);}}
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)stampVersion(g.getChildAt(i));}
    }

    private void attachAutomationObserver(View root){
        if(root==null||"v091-observed".equals(root.getTag()))return;
        root.setTag("v091-observed");
        root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
            @Override public void onGlobalLayout(){enhance(root);}
        });
        enhance(root);
    }

    private void enhance(View root){
        stampVersion(root);
        polishHomeBrand(root);
        retargetDeveloperConsole(root);
        applyMemberPreview(root);
        attachFooterDeveloperTrigger(root);
        injectAttentionCard(root);
    }

    private void polishHomeBrand(View root){
        TextView welcome=findTextContaining090(root,"Welcome back,");
        if(welcome==null||!(welcome.getParent() instanceof LinearLayout))return;
        LinearLayout hero=(LinearLayout)welcome.getParent();
        if(containsExactText090(hero,"DUCK FORCE • FACTION OS"))return;
        TextView brand=new TextView(this);brand.setText("DUCK FORCE • FACTION OS");brand.setTextColor(GOLD);brand.setTextSize(10);brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD);brand.setLetterSpacing(.12f);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp090(7);hero.addView(brand,0,p);
    }

    private void injectAttentionCard(View root){
        if(DeveloperPreviewStore.isMemberPreview(this))return;
        if(!containsExactText090(root,"Leadership"))return;
        if(containsText090(root,"Leadership Attention"))return;
        TextView section=findText090(root,"WHAT NEEDS MY ATTENTION?");
        if(section==null||!(section.getParent() instanceof LinearLayout))return;
        LinearLayout parent=(LinearLayout)section.getParent();
        int index=parent.indexOfChild(section)+1;

        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp090(16),dp090(12),dp090(16),dp090(12));card.setBackground(rounded090(PANEL,GOLD,17));card.setClickable(true);card.setFocusable(true);
        TextView eyebrow=new TextView(this);eyebrow.setText("COMMAND PRIORITY");eyebrow.setTextColor(GOLD);eyebrow.setTextSize(10);eyebrow.setTypeface(Typeface.DEFAULT,Typeface.BOLD);eyebrow.setLetterSpacing(.10f);card.addView(eyebrow);
        TextView title=new TextView(this);title.setText("Leadership Attention  →");title.setTextColor(TEXT);title.setTextSize(17);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp090(4);card.addView(title,tp);
        TextView body=new TextView(this);body.setText("Who needs attention and why — live exceptions from faction data");body.setTextColor(MUTED);body.setTextSize(12);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);bp.topMargin=dp090(3);card.addView(body,bp);
        card.setOnClickListener(v->startActivity(new Intent(this,LeadershipAttentionActivity.class)));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp090(104));p.bottomMargin=dp090(10);parent.addView(card,Math.min(index,parent.getChildCount()),p);
    }

    private void retargetDeveloperConsole(View root){
        TextView title=findText090(root,"DEVELOPER CONSOLE");
        if(title==null||!(title.getParent() instanceof View))return;
        View card=(View)title.getParent();card.setClickable(true);card.setOnClickListener(v->openDeveloperGate());
    }

    private void attachFooterDeveloperTrigger(View root){
        TextView footer=findTextContaining090(root,"Duck Force Companion v0.9.1");
        if(footer==null)return;
        footer.setClickable(true);footer.setFocusable(true);footer.setPadding(footer.getPaddingLeft(),dp090(12),footer.getPaddingRight(),dp090(14));
        if(DeveloperPreviewStore.isMemberPreview(this)&&!footer.getText().toString().contains("MEMBER PREVIEW"))footer.setText("Duck Force Companion v0.9.1 • MEMBER PREVIEW");
        footer.setOnClickListener(v->{long now=System.currentTimeMillis();if(now-lastFooterTap>1500L)footerTapCount=0;lastFooterTap=now;footerTapCount++;if(footerTapCount>=3){footerTapCount=0;openDeveloperGate();}});
    }

    private void openDeveloperGate(){startActivity(new Intent(this,DeveloperGateActivity.class));}

    private void applyMemberPreview(View root){
        if(!DeveloperPreviewStore.isMemberPreview(this))return;
        hideExactTextView(root,"Leadership");
        hideExactTextView(root,"OWNER / DEVELOPER");
        hideCard(root,"Leadership Attention  →");
        hideCard(root,"Leadership Attention");
        hideCard(root,"LEADERSHIP CONTROLS");
        hideCard(root,"ARMORY AUDITOR");
        hideCard(root,"DEVELOPER CONSOLE");
        hideCard(root,"COMPANY TRAINING CALCULATOR");
        TextView meta=findTextContaining090(root,"Leadership permissions");
        if(meta!=null){String raw=meta.getText().toString();int cut=raw.indexOf(" • ");String faction=cut>0?raw.substring(0,cut):"Duck Force";meta.setText(faction+" • Member Preview • member-safe permissions");}
    }

    private void hideExactTextView(View root,String exact){
        if(root instanceof TextView){CharSequence raw=((TextView)root).getText();if(raw!=null&&exact.equals(raw.toString()))root.setVisibility(View.GONE);}
        if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++)hideExactTextView(g.getChildAt(i),exact);}
    }

    private void hideCard(View root,String title){TextView t=findText090(root,title);if(t!=null&&t.getParent() instanceof View)((View)t.getParent()).setVisibility(View.GONE);}

    private boolean containsText090(View view,String needle){
        if(view instanceof TextView){CharSequence raw=((TextView)view).getText();if(raw!=null&&raw.toString().contains(needle))return true;}
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)if(containsText090(g.getChildAt(i),needle))return true;}
        return false;
    }

    private boolean containsExactText090(View view,String exact){return findText090(view,exact)!=null;}

    private TextView findText090(View view,String exact){
        if(view instanceof TextView){CharSequence raw=((TextView)view).getText();if(raw!=null&&exact.equals(raw.toString()))return (TextView)view;}
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++){TextView found=findText090(g.getChildAt(i),exact);if(found!=null)return found;}}
        return null;
    }

    private TextView findTextContaining090(View view,String needle){
        if(view instanceof TextView){CharSequence raw=((TextView)view).getText();if(raw!=null&&raw.toString().contains(needle))return (TextView)view;}
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++){TextView found=findTextContaining090(g.getChildAt(i),needle);if(found!=null)return found;}}
        return null;
    }
}
