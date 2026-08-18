package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

/** TornFCA launcher shim. v0.10.1 uses the approved command-center shell for both build variants. */
public class AccessGateActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);getWindow().setStatusBarColor(Color.rgb(6,9,13));getWindow().setNavigationBarColor(Color.rgb(6,9,13));
        TornFcaCommandRuntime.install(getApplication());
        Intent i;if(LegalAcceptanceStore.hasAcceptedCurrent(this)){i=TornFcaCommandRuntime.homeIntent(this,"Home");}else{i=new Intent(this,LegalActivity.class);i.putExtra(LegalActivity.EXTRA_REQUIRE_ACCEPTANCE,true);}i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);startActivity(i);finish();
    }
}
