package com.matthayesego.duckforcetoolkit;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * v0.4.2 visual layer.
 *
 * Keeps the working companion behaviour intact while presenting a cleaner,
 * more professional Duck Force login and companion interface.
 */
public class PolishedCompanionActivity extends CompanionActivity {
    private static final int BG = Color.rgb(6, 9, 13);
    private static final int SURFACE = Color.rgb(15, 20, 28);
    private static final int SURFACE_2 = Color.rgb(10, 15, 22);
    private static final int BORDER = Color.rgb(45, 55, 69);
    private static final int TEXT = Color.rgb(244, 246, 249);
    private static final int MUTED = Color.rgb(154, 164, 178);
    private static final int GOLD = Color.rgb(205, 148, 57);
    private static final int GOLD_LIGHT = Color.rgb(241, 194, 106);
    private static final int ERROR = Color.rgb(213, 98, 98);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable rounded(int fill, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke);
        return d;
    }

    private GradientDrawable goldButton() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(238, 190, 94), Color.rgb(202, 143, 48)}
        );
        d.setCornerRadius(dp(13));
        return d;
    }

    @Override
    public void setContentView(View view) {
        restyleStructure(view);
        polish(view);
        super.setContentView(view);
    }

    private void restyleStructure(View root) {
        if (!(root instanceof ScrollView)) return;
        ScrollView scroll = (ScrollView) root;
        scroll.setBackgroundColor(BG);
        scroll.setClipToPadding(false);
        if (scroll.getChildCount() == 0 || !(scroll.getChildAt(0) instanceof LinearLayout)) return;

        LinearLayout column = (LinearLayout) scroll.getChildAt(0);
        if (containsText(column, "Connect your Torn account")) {
            restyleLogin(column);
        }
    }

    private void restyleLogin(LinearLayout column) {
        // Hero: remove the oversized bordered panel and let the badge/brand breathe.
        if (column.getChildCount() > 0 && column.getChildAt(0) instanceof LinearLayout) {
            LinearLayout hero = (LinearLayout) column.getChildAt(0);
            hero.setBackgroundColor(Color.TRANSPARENT);
            hero.setElevation(0f);
            hero.setPadding(dp(14), dp(10), dp(14), dp(12));
            hero.setGravity(Gravity.CENTER_HORIZONTAL);
        }

        // Tighten the gap between identity and sign-in card.
        if (column.getChildCount() > 1) {
            View gap = column.getChildAt(1);
            ViewGroup.LayoutParams gp = gap.getLayoutParams();
            gp.height = dp(8);
            gap.setLayoutParams(gp);
        }

        // Sign-in card: quieter surface, subtle border, compact spacing.
        if (column.getChildCount() > 2 && column.getChildAt(2) instanceof LinearLayout) {
            LinearLayout login = (LinearLayout) column.getChildAt(2);
            login.setBackground(rounded(SURFACE, BORDER, 18));
            login.setElevation(dp(1));
            login.setPadding(dp(18), dp(18), dp(18), dp(16));

            for (int i = 0; i < login.getChildCount(); i++) {
                View child = login.getChildAt(i);
                if (child instanceof EditText) {
                    EditText field = (EditText) child;
                    field.setTextColor(TEXT);
                    field.setHintTextColor(Color.rgb(127, 138, 153));
                    field.setTextSize(16);
                    field.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                    field.setBackground(rounded(SURFACE_2, Color.rgb(54, 65, 80), 13));
                    field.setPadding(dp(16), 0, dp(16), 0);
                }
            }
        }
    }

    private boolean containsText(View view, String needle) {
        if (view instanceof TextView) {
            CharSequence raw = ((TextView) view).getText();
            if (raw != null && raw.toString().contains(needle)) return true;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsText(group.getChildAt(i), needle)) return true;
            }
        }
        return false;
    }

    private void polish(View view) {
        if (view == null) return;

        if (view instanceof TextView) polishText((TextView) view);

        if (view instanceof Button) {
            Button button = (Button) view;
            button.setAllCaps(false);
            button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            button.setLetterSpacing(0.01f);
            button.setStateListAnimator(null);
            CharSequence raw = button.getText();
            if (raw != null && raw.toString().contains("Connect to Duck Force")) {
                button.setTextColor(Color.rgb(24, 17, 8));
                button.setTextSize(15);
                button.setBackground(goldButton());
            }
        }

        if (view instanceof LinearLayout && view.getBackground() instanceof GradientDrawable) {
            if (view.getElevation() == 0f) view.setElevation(dp(1));
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
            Drawable mark = getDrawable(R.drawable.duckforce_noir);
            if (mark != null) {
                int size = dp(126);
                mark.setBounds(0, 0, size, size);
                textView.setCompoundDrawables(null, mark, null, null);
                textView.setGravity(Gravity.CENTER);
                textView.setMinHeight(size);
            }
            return;
        }

        value = value.replace("v0.4.0", "v0.4.2");
        value = value.replace("v0.4.1", "v0.4.2");
        value = value.replace("Connect your Torn account", "Sign in to Duck Force");
        value = value.replace(
                "Your key verifies your identity and Duck Force membership, then stays encrypted on this device.",
                "Use your Torn API key to verify your membership. Your key is encrypted and stored only on this device."
        );
        value = value.replace(
                "Your Duck Force tools, requests and leadership access in one place.",
                "Faction tools, requests and leadership access — wherever you play."
        );
        value = value.replace("Banking Companion — v0.4 prototype", "Banking Companion — Preview");
        value = value.replace("🦆 Duck Force Companion", "Duck Force Companion");
        value = value.replace("💰 Banking", "Banking");
        value = value.replace("📦 Armory Log", "Armory Log");
        value = value.replace("💊 Faction Xanax Auditor", "Faction Xanax Auditor");
        value = value.replace("⚙️ Leadership Controls", "Leadership Controls");
        value = value.replace("🏋️ Company Train Calculator", "Company Training Calculator");
        value = value.replace("🛠 Developer Console", "Developer Console");

        if (value.contains("Encrypted locally")) {
            value = "Encrypted on this device  •  Duck Force only  •  v0.4.2";
        }

        textView.setText(value);

        if ("DUCK FORCE".equals(value)) {
            textView.setTextColor(GOLD_LIGHT);
            textView.setTextSize(12);
            textView.setLetterSpacing(0.28f);
            textView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        } else if ("Faction Companion".equals(value)) {
            textView.setText("Companion");
            textView.setTextColor(TEXT);
            textView.setTextSize(34);
            textView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            textView.setLetterSpacing(-0.02f);
        } else if ("Sign in to Duck Force".equals(value)) {
            textView.setTextColor(TEXT);
            textView.setTextSize(20);
            textView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        } else if (value.startsWith("Faction tools, requests")) {
            textView.setTextColor(MUTED);
            textView.setTextSize(14);
            textView.setLineSpacing(0f, 1.12f);
        }

        if (value.contains("Encrypted on this device") || value.contains("Companion-first foundation")) {
            textView.setTextColor(MUTED);
            textView.setTextSize(11);
        }
        if (value.contains("OWNER / DEVELOPER")) textView.setTextColor(GOLD_LIGHT);
        if (value.toLowerCase().contains("unable to verify") || value.toLowerCase().contains("restricted to duck force")) {
            textView.setTextColor(ERROR);
        }
    }
}
