package com.matthayesego.duckforcetoolkit;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

/** Keeps the premium v0.9.7 shell architecture while stamping the current beta version dynamically. */
public class V098CompanionActivity extends V095CompanionActivity {
    @Override public void setContentView(View view){
        super.setContentView(view);
        ViewGroup host=findViewById(android.R.id.content);
        if(host==null)return;
        stamp(host);
        host.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
            @Override public void onGlobalLayout(){stamp(host);}
        });
    }
    private void stamp(View view){
        if(view instanceof TextView){TextView t=(TextView)view;CharSequence raw=t.getText();if(raw!=null){String s=raw.toString().replace("v0.9.7","v0.9.8");if(!s.equals(raw.toString()))t.setText(s);}}
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)stamp(g.getChildAt(i));}
    }
}
