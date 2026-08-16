package com.matthayesego.duckforcetoolkit;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Public TornFCA shell. Existing v0.9 functionality remains underneath this brand/theme layer. */
public class TornFcaActivity extends V098CompanionActivity {
    @Override public void setContentView(View view){
        super.setContentView(view);
        ViewGroup root=findViewById(android.R.id.content);
        if(root!=null){
            TornFcaBrand.apply(this,root);
            addApiRequirementNotice(root);
            retargetBankingCards(root);
        }
        primeProviderConsent();
    }

    @Override protected void onResume(){
        super.onResume();
        ViewGroup root=findViewById(android.R.id.content);
        if(root!=null){
            TornFcaBrand.apply(this,root);
            addApiRequirementNotice(root);
            retargetBankingCards(root);
        }
        primeProviderConsent();
        refreshPremiumEntitlement();
    }

    @Override public void startActivity(Intent intent){
        super.startActivity(TornFcaBrand.retarget(this,intent));
    }

    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}

    /** Adds Torn's key-use disclosure directly beside the API-key input. */
    private void addApiRequirementNotice(View root){
        TextView title=findText(root,"Connect your Torn account");
        if(title==null||!(title.getParent() instanceof LinearLayout))return;
        LinearLayout card=(LinearLayout)title.getParent();
        for(int i=0;i<card.getChildCount();i++)if("tornfca-api-requirement".equals(card.getChildAt(i).getTag()))return;

        FactionTheme theme=FactionTheme.forContext(this);
        TextView notice=new TextView(this);
        notice.setTag("tornfca-api-requirement");
        notice.setText("API KEY REQUIREMENT\nUse one 16-character Limited Access Torn API key for TornFCA. Full Access is NOT required. Leadership-only faction data additionally depends on your in-game Faction API Access permission.\n\nStorage: the key is AES-GCM encrypted on this device using Android Keystore. TornFCA sends it to Torn's official API for requested features. Shared notices/banking may temporarily send it over HTTPS to the TornFCA faction backend to verify identity/permissions; that backend does not store the key. FFScouter and TornStats receive the key only after separate explicit opt-in, under their own terms/data policies.");
        notice.setTextSize(12f);
        notice.setTextColor(Color.rgb(224,232,241));
        notice.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        notice.setLineSpacing(0f,1.12f);
        notice.setPadding(dp(13),dp(12),dp(13),dp(12));
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(Color.rgb(8,18,25));
        bg.setCornerRadius(dp(13));
        bg.setStroke(dp(1),theme.accent);
        notice.setBackground(bg);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin=dp(12);p.bottomMargin=dp(3);
        card.addView(notice,Math.min(2,card.getChildCount()),p);
    }

    private void retargetBankingCards(View root){
        if(root instanceof TextView){
            TextView t=(TextView)root;
            if("Banking".equals(t.getText()==null?"":t.getText().toString())){
                View card=findClickableAncestor(t);
                if(card!=null){card.setOnClickListener(v->{Intent i=new Intent(this,FeatureRouterActivity.class);i.putExtra(FeatureRouterActivity.EXTRA_TARGET,FeatureRouterActivity.TARGET_BANKING);startActivity(i);});}
            }
        }
        if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++)retargetBankingCards(g.getChildAt(i));}
    }

    private View findClickableAncestor(View start){View v=start;while(v!=null){if(v.isClickable())return v;if(!(v.getParent() instanceof View))break;v=(View)v.getParent();}return null;}

    /** Loads only local consent state so provider clients can enforce opt-in at their network boundary. */
    private void primeProviderConsent(){String key=new SecureApiKeyStore(this).load();if(key!=null&&!key.isBlank())FFScouterClient.hasConsent(this,key);}

    private void refreshPremiumEntitlement(){
        String key=new SecureApiKeyStore(this).load();if(key==null||key.isBlank()||!PremiumBackendClient.isConfigured())return;
        FactionScopeCache.Scope scope=FactionScopeCache.load(this,key);if(scope==null||scope.playerId<=0)return;
        PremiumBackendClient.refreshAsync(this,scope.playerId);
    }

    private TextView findText(View view,String needle){
        if(view instanceof TextView){TextView t=(TextView)view;if(needle.equals(t.getText()==null?"":t.getText().toString()))return t;}
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++){TextView found=findText(g.getChildAt(i),needle);if(found!=null)return found;}}
        return null;
    }
}
