package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

/**
 * TornFCA launcher shim.
 * v0.9.25 keeps the old beta access-code wall removed and routes first-time/current-legal-version
 * users through the legal acknowledgement screen before opening the Torn account sign-in shell.
 */
public class AccessGateActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(6,9,13));
        getWindow().setNavigationBarColor(Color.rgb(6,9,13));
        Intent i;
        if(LegalAcceptanceStore.hasAcceptedCurrent(this))i=new Intent(this,TornFcaActivity.class);
        else{i=new Intent(this,LegalActivity.class);i.putExtra(LegalActivity.EXTRA_REQUIRE_ACCEPTANCE,true);}
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
