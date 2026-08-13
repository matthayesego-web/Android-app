package com.matthayesego.duckforcetoolkit;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * v0.4.1 presentation layer.
 *
 * Keeps the proven v0.4.0 companion behaviour intact while tightening the
 * visual language into a quieter noir / premium faction-companion style.
 */
public class PolishedCompanionActivity extends CompanionActivity {
    private static final int BG = Color.rgb(7, 10, 15);
    private static final int TEXT = Color.rgb(242, 244, 247);
    private static final int MUTED = Color.rgb(152, 162, 179);
    private static final int GOLD = Color.rgb(215, 160, 68);
    private static final int GOLD_LIGHT = Color.rgb(242, 197, 107);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void setContentView(View view) {
        polish(view);
        super.setContentView(view);
    }

    private void polish(View view) {
        if (view == null) return;

        if (view instanceof TextView) {
            polishText((TextView) view);
        }

        if (view instanceof Button) {
            Button button = (Button) view;
            button.setAllCaps(false);
            button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            button.setLetterSpacing(0.015f);
            button.setStateListAnimator(null);
        }

        if (view instanceof LinearLayout && view.getBackground() instanceof GradientDrawable) {
            view.setElevation(dp(2));
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) polish(group.getChildAt(i));
        }
    }

    private void polishText(TextView textView) {
        CharSequence raw = textView.getText();
        if (raw == null) return;
        String value = raw.toString();

        if ("🦆".equals(value)) {
            textView.setText("");
            Drawable mark = getDrawable(R.drawable.ic_launcher_foreground);
            if (mark != null) {
                int size = dp(96);
                mark.setBounds(0, 0, size, size);
                textView.setCompoundDrawables(null, mark, null, null);
                textView.setGravity(Gravity.CENTER);
                textView.setMinHeight(size);
            }
            return;
        }

        value = value.replace("Duck Force Companion v0.4.0", "Duck Force Companion v0.4.1");
        value = value.replace("v0.4.0 begins", "v0.4.1 continues");
        value = value.replace("Banking Companion — v0.4 prototype", "Banking Companion — Preview");
        value = value.replace("🦆 Duck Force Companion", "DUCK FORCE COMPANION");
        value = value.replace("💰 Banking", "BANKING");
        value = value.replace("📦 Armory Log", "ARMORY LOG");
        value = value.replace("💊 Faction Xanax Auditor", "XANAX AUDITOR");
        value = value.replace("⚙️ Leadership Controls", "LEADERSHIP CONTROLS");
        value = value.replace("🏋️ Company Train Calculator", "COMPANY TRAINING CALCULATOR");
        value = value.replace("🛠 Developer Console", "DEVELOPER CONSOLE");
        textView.setText(value);

        if ("DUCK FORCE".equals(value)) {
            textView.setTextColor(GOLD_LIGHT);
            textView.setLetterSpacing(0.24f);
            textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        } else if ("Faction Companion".equals(value)) {
            textView.setText("Companion");
            textView.setTextColor(TEXT);
            textView.setTextSize(32);
            textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            textView.setLetterSpacing(-0.015f);
        } else if (value.equals(value.toUpperCase()) && value.length() > 3 && value.length() < 34) {
            textView.setLetterSpacing(0.055f);
            textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        if (value.contains("Encrypted locally") || value.contains("Companion-first foundation")) {
            textView.setTextColor(MUTED);
        }
        if (value.contains("OWNER / DEVELOPER")) textView.setTextColor(GOLD_LIGHT);
    }
}
