package com.matthayesego.duckforcetoolkit;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

/** v0.9.1 presentation wrapper for the exception dashboard, including edge-to-edge inset safety. */
public class V091LeadershipAttentionActivity extends LeadershipAttentionActivity {
    @Override public void setContentView(View view){
        if(view instanceof ScrollView){
            ScrollView s=(ScrollView)view;
            final int l=s.getPaddingLeft(),t=s.getPaddingTop(),r=s.getPaddingRight(),b=s.getPaddingBottom();
            s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),b+i.getSystemWindowInsetBottom());return i;});
        }
        stamp(view);
        super.setContentView(view);
        view.requestApplyInsets();
    }

    private void stamp(View view){
        if(view instanceof TextView){TextView t=(TextView)view;CharSequence raw=t.getText();if(raw!=null){String v=raw.toString().replace("v0.9.0 preview rules","v0.9.1 beta rules");if(!v.equals(raw.toString()))t.setText(v);}}
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)stamp(g.getChildAt(i));}
    }
}
