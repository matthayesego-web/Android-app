package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

/**
 * TornFCA launcher shim.
 * v0.9.21 keeps the old beta access-code wall removed; this activity only clears stale tasks and
 * forwards directly into the current TornFCA shell.
 */
public class AccessGateActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(6,9,13));
        getWindow().setNavigationBarColor(Color.rgb(6,9,13));
        Intent i=new Intent(this,TornFcaActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
