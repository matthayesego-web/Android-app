package com.matthayesego.duckforcetoolkit;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

/** Public TornFCA shell. Existing v0.9 functionality remains underneath this brand/theme layer. */
public class TornFcaActivity extends V098CompanionActivity {
    @Override public void setContentView(View view){
        super.setContentView(view);
        ViewGroup root=findViewById(android.R.id.content);
        if(root!=null)TornFcaBrand.apply(this,root);
    }

    @Override protected void onResume(){
        super.onResume();
        ViewGroup root=findViewById(android.R.id.content);
        if(root!=null)TornFcaBrand.apply(this,root);
    }

    @Override public void startActivity(Intent intent){
        super.startActivity(TornFcaBrand.retarget(this,intent));
    }
}
