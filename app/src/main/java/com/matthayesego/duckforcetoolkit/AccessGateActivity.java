package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

/**
 * TornFCA launcher shim.
 * v0.9.33 keeps the old beta access-code wall removed, enforces the current legal acknowledgement,
 * and sends only the side-by-side .beta application into the new command-console architecture.
 * Historical release-audit marker: TornFcaActivity.class remains the authenticated bootstrap parent
 * underneath both the stable TornFcaCurrentActivity and beta BetaCommandActivity shells.
 */
public class AccessGateActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(6,9,13));
        getWindow().setNavigationBarColor(Color.rgb(6,9,13));
        // The v0.9.32 visual hotfix was a reskin. Keep its install hook only for non-beta compatibility;
        // the hotfix itself is beta-gated, so it cannot mutate the true beta command console.
        if(!BuildConfig.APPLICATION_ID.endsWith(".beta"))CurrentShellVisualHotfix.install(getApplication());
        Intent i;
        if(LegalAcceptanceStore.hasAcceptedCurrent(this)){
            Class<?> shell=BuildConfig.APPLICATION_ID.endsWith(".beta")?BetaCommandActivity.class:TornFcaCurrentActivity.class;
            i=new Intent(this,shell);
        }else{
            i=new Intent(this,LegalActivity.class);
            i.putExtra(LegalActivity.EXTRA_REQUIRE_ACCEPTANCE,true);
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
