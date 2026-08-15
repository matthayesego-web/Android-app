package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FeatureRouterActivity extends Activity {
    public static final String EXTRA_TARGET = "target";
    public static final String TARGET_ACTIVITY = "ACTIVITY";
    public static final String TARGET_WAR = "WAR";
    public static final String TARGET_CHAIN = "CHAIN";
    public static final String TARGET_OC = "OC";
    public static final String TARGET_DEVELOPER = "DEVELOPER";

    private static final int BG=Color.rgb(8,12,18),PANEL=Color.rgb(20,27,38),BORDER=Color.rgb(49,63,81),TEXT=Color.rgb(245,248,252),MUTED=Color.rgb(151,163,179),GOLD=Color.rgb(243,184,52),BAD=Color.rgb(248,81,73);
    private SecureApiKeyStore keyStore;

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        keyStore=new SecureApiKeyStore(this);
        DeveloperSettings.activityMaxPages(this);
        showStatus("Verifying faction scope…",GOLD);route();
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private void showStatus(String message,int stroke){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(dp(24),dp(24),dp(24),dp(24));root.setBackgroundColor(BG);
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(18),dp(18),dp(18),dp(18));card.setBackground(rounded(PANEL,stroke,18));
        TextView title=new TextView(this);title.setText("Duck Force Companion");title.setTextColor(TEXT);title.setTextSize(20);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);card.addView(title);
        TextView body=new TextView(this);body.setText(message);body.setTextColor(MUTED);body.setTextSize(13);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);bp.topMargin=dp(7);card.addView(body,bp);
        root.addView(card,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));setContentView(root);
    }

    private void route(){
        final String target=getIntent().getStringExtra(EXTRA_TARGET);final String key=keyStore.load();
        if(key==null||key.trim().isEmpty()){showStatus("No saved Torn API key is available. Return to the Companion and sign in again.",BAD);return;}
        new Thread(()->{try{
            AuthSession session=TornApiClient.authenticate(key);
            runOnUiThread(()->launch(target,session));
        }catch(Exception e){String message=e.getMessage()==null?"Unable to verify faction scope.":e.getMessage();runOnUiThread(()->showStatus(message,BAD));}}).start();
    }

    private void launch(String target,AuthSession session){
        if(TARGET_DEVELOPER.equals(target)){
            if(!AppRoles.isOwner(session)){showStatus("Developer Console is restricted to the app owner.",BAD);return;}
            Intent i=new Intent(this,DeveloperConsoleActivity.class);putScope(i,session);startActivity(i);finish();return;
        }
        String mode;
        if(TARGET_WAR.equals(target))mode=FactionOpsActivity.MODE_WAR;
        else if(TARGET_CHAIN.equals(target))mode=FactionOpsActivity.MODE_CHAIN;
        else if(TARGET_OC.equals(target))mode=FactionOpsActivity.MODE_OC;
        else mode=FactionOpsActivity.MODE_ACTIVITY;
        Intent i=new Intent(this,FactionOpsActivity.class);i.putExtra(FactionOpsActivity.EXTRA_MODE,mode);putScope(i,session);startActivity(i);finish();
    }

    private void putScope(Intent i,AuthSession session){
        i.putExtra(FactionOpsActivity.EXTRA_FACTION_ID,session.factionId);
        i.putExtra(FactionOpsActivity.EXTRA_FACTION_NAME,session.factionName);
        i.putExtra(FactionOpsActivity.EXTRA_FACTION_API,session.factionApiAccess);
        i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_ID,session.factionId);
        i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_NAME,session.factionName);
        i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_API,session.factionApiAccess);
        i.putExtra(DeveloperConsoleActivity.EXTRA_POSITION,session.position);
    }
}
