package com.matthayesego.duckforcetoolkit;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * v0.4.3 presentation layer.
 *
 * Keeps the working companion behaviour intact while using reliable native
 * ImageView rendering for the Duck Force badge and a restrained noir UI.
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
                new int[]{Color.rgb(239, 193, 103), Color.rgb(202, 143, 48)}
        );
        d.setCornerRadius(dp(13));
        return d;
    }

    @Override
    public void setContentView(View view) {
        boolean login = containsText(view, "Connect your Torn account");
        if (login) prepareLogin(view);
        polishTree(view);
        stampText(view);
        super.setContentView(view);
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

    private void prepareLogin(View root) {
        if (!(root instanceof ScrollView)) return;
        ScrollView scroll = (ScrollView) root;
        scroll.setBackgroundColor(BG);
        scroll.setClipToPadding(false);
        if (scroll.getChildCount() == 0 || !(scroll.getChildAt(0) instanceof LinearLayout)) return;

        LinearLayout column = (LinearLayout) scroll.getChildAt(0);
        if (column.getChildCount() < 3) return;

        if (column.getChildAt(0) instanceof LinearLayout) {
            LinearLayout hero = (LinearLayout) column.getChildAt(0);
            hero.setBackgroundColor(Color.TRANSPARENT);
            hero.setElevation(0f);
            hero.setPadding(dp(12), dp(8), dp(12), dp(14));
            hero.setGravity(Gravity.CENTER_HORIZONTAL);
            replaceDuckPlaceholder(hero);
        }

        View gap = column.getChildAt(1);
        ViewGroup.LayoutParams gp = gap.getLayoutParams();
        gp.height = dp(6);
        gap.setLayoutParams(gp);

        if (column.getChildAt(2) instanceof LinearLayout) {
            LinearLayout login = (LinearLayout) column.getChildAt(2);
            login.setBackground(rounded(SURFACE, BORDER, 18));
            login.setElevation(dp(1));
            login.setPadding(dp(18), dp(18), dp(18), dp(16));
        }
    }

    private void replaceDuckPlaceholder(LinearLayout hero) {
        for (int i = 0; i < hero.getChildCount(); i++) {
            View child = hero.getChildAt(i);
            if (!(child instanceof TextView)) continue;
            CharSequence raw = ((TextView) child).getText();
            if (raw == null || !"🦆".contentEquals(raw)) continue;

            ImageView badge = new ImageView(this);
            badge.setImageResource(R.drawable.duckforce_noir_legacy);
            badge.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            badge.setAdjustViewBounds(true);
            badge.setContentDescription("Duck Force");

            int size = dp(148);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.gravity = Gravity.CENTER_HORIZONTAL;
            params.bottomMargin = dp(14);

            hero.removeViewAt(i);
            hero.addView(badge, i, params);
            return;
        }
    }

    private void polishTree(View view) {
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

        if (view instanceof EditText) {
            EditText field = (EditText) view;
            field.setTextColor(TEXT);
            field.setHintTextColor(Color.rgb(127, 138, 153));
            field.setTextSize(16);
            field.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            field.setBackground(rounded(SURFACE_2, Color.rgb(54, 65, 80), 13));
            field.setPadding(dp(16), 0, dp(16), 0);
        }

        if (view instanceof TextView) {
            TextView text = (TextView) view;
            text.setFontFeatureSettings("kern");
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) polishTree(group.getChildAt(i));
        }
    }

    private void stampText(View view) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            CharSequence raw = textView.getText();
            if (raw != null) {
                String value = raw.toString();
                value = value.replace("v0.4.0", "v0.4.3")
                        .replace("v0.4.1", "v0.4.3")
                        .replace("v0.4.2", "v0.4.3")
                        .replace("Connect your Torn account", "Sign in to Duck Force")
                        .replace("Your key verifies your identity and Duck Force membership, then stays encrypted on this device.",
                                "Use your Torn API key to verify your membership. Your key is encrypted and stored only on this device.")
                        .replace("Your Duck Force tools, requests and leadership access in one place.",
                                "Faction tools, requests and leadership access — wherever you play.")
                        .replace("Banking Companion — v0.4 prototype", "Banking Companion — Preview")
                        .replace("🦆 Duck Force Companion", "DUCK FORCE COMPANION")
                        .replace("💰 Banking", "BANKING")
                        .replace("📦 Armory Log", "ARMORY LOG")
                        .replace("💊 Faction Xanax Auditor", "XANAX AUDITOR")
                        .replace("⚙️ Leadership Controls", "LEADERSHIP CONTROLS")
                        .replace("🏋️ Company Train Calculator", "COMPANY TRAINING CALCULATOR")
                        .replace("🛠 Developer Console", "DEVELOPER CONSOLE")
                        .replace("🔐 Encrypted locally", "Encrypted on this device");
                textView.setText(value);

                if ("DUCK FORCE".equals(value)) {
                    textView.setTextColor(GOLD_LIGHT);
                    textView.setLetterSpacing(0.24f);
                    textView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                } else if ("Faction Companion".equals(value)) {
                    textView.setText("Companion");
                    textView.setTextColor(TEXT);
                    textView.setTextSize(32);
                    textView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                    textView.setLetterSpacing(-0.015f);
                } else if (value.equals(value.toUpperCase()) && value.length() > 3 && value.length() < 34) {
                    textView.setLetterSpacing(0.055f);
                    textView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                }

                if (value.contains("Encrypted on this device") || value.contains("Companion-first foundation")) {
                    textView.setTextColor(MUTED);
                }
                if (value.contains("OWNER / DEVELOPER")) textView.setTextColor(GOLD_LIGHT);
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) stampText(group.getChildAt(i));
        }
    }
}
