package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

/**
 * TornFCA launcher shim.
 * v0.9.29 keeps the old beta access-code wall removed, enforces the current legal acknowledgement,
 * and opens the consolidated current dashboard after the user has accepted the legal version.
 * Historical release-audit marker: TornFcaActivity.class remains the authenticated bootstrap parent
 * underneath TornFcaCurrentActivity, but is no longer the visible launcher destination.
 */
public class AccessGateActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(6,9,13));
        getWindow().setNavigationBarColor(Color.rgb(6,9,13));
        Intent i;
        if(LegalAcceptanceStore.hasAcceptedCurrent(this))i=new Intent(this,TornFcaCurrentActivity.class);
        else{i=new Intent(this,LegalActivity.class);i.putExtra(LegalActivity.EXTRA_REQUIRE_ACCEPTANCE,true);}
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
