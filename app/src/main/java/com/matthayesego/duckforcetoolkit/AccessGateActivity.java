package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

/**
 * TornFCA launcher shim. v0.9.34 routes the side-by-side beta into the repaired command surface.
 * Historical release-audit marker: TornFcaActivity.class remains the authenticated bootstrap parent
 * underneath both the stable and beta shells; this launcher only chooses the visible shell.
 */
public class AccessGateActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);getWindow().setStatusBarColor(Color.rgb(6,9,13));getWindow().setNavigationBarColor(Color.rgb(6,9,13));
        if(BuildConfig.APPLICATION_ID.endsWith(".beta"))BetaSurfacePolish.install(getApplication());
        else CurrentShellVisualHotfix.install(getApplication());
        Intent i;if(LegalAcceptanceStore.hasAcceptedCurrent(this)){Class<?> shell=BuildConfig.APPLICATION_ID.endsWith(".beta")?BetaCommandActivity.class:TornFcaCurrentActivity.class;i=new Intent(this,shell);}else{i=new Intent(this,LegalActivity.class);i.putExtra(LegalActivity.EXTRA_REQUIRE_ACCEPTANCE,true);}i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);startActivity(i);finish();
    }
}
