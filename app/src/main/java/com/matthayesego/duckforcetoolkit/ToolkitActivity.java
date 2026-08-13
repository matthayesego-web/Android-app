package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ToolkitActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(8, 12, 18));
        getWindow().setNavigationBarColor(Color.rgb(8, 12, 18));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.rgb(8, 12, 18));

        TextView title = new TextView(this);
        title.setText("🦆 Duck Force Toolkit");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        root.addView(title);

        setContentView(root);
    }
}
