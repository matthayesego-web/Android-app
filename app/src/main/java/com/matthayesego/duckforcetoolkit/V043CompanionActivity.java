package com.matthayesego.duckforcetoolkit;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * v0.4.3 image-reliability layer.
 *
 * Uses a real ImageView for the Duck Force badge instead of a compound
 * drawable attached to the old emoji placeholder. This avoids the missing
 * artwork regression seen in v0.4.2 while retaining the polished companion UI.
 */
public class V043CompanionActivity extends PolishedCompanionActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private int px(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void setContentView(View view) {
        restoreBadge(view);
        super.setContentView(view);
        stampVersion(view);
    }

    private boolean restoreBadge(View view) {
        if (!(view instanceof ViewGroup)) return false;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView) {
                CharSequence value = ((TextView) child).getText();
                if (value != null && "🦆".contentEquals(value)) {
                    ImageView badge = new ImageView(this);
                    badge.setImageResource(R.drawable.duckforce_noir_legacy);
                    badge.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    badge.setAdjustViewBounds(true);
                    badge.setContentDescription("Duck Force");
                    int size = px(142);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                    params.gravity = Gravity.CENTER_HORIZONTAL;
                    params.bottomMargin = px(12);
                    group.removeViewAt(i);
                    group.addView(badge, i, params);
                    return true;
                }
            }
            if (restoreBadge(child)) return true;
        }
        return false;
    }

    private void stampVersion(View view) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            CharSequence raw = text.getText();
            if (raw != null) {
                String value = raw.toString()
                        .replace("v0.4.0", "v0.4.3")
                        .replace("v0.4.1", "v0.4.3")
                        .replace("v0.4.2", "v0.4.3");
                text.setText(value);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) stampVersion(group.getChildAt(i));
        }
    }
}
