package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Small visual-only pass that makes TornFCA's tappable surfaces read like one coherent action
 * system. It intentionally does not change routing, permissions, API calls, or feature logic.
 */
public final class PremiumActionPolish {
    private static final String ACTION_TAG = "tornfca-premium-action-label";
    private static final int SURFACE = Color.rgb(12,18,26);
    private static final int SURFACE_2 = Color.rgb(8,13,20);
    private static final int TEXT = Color.rgb(246,248,251);
    private static final int MUTED = Color.rgb(145,155,169);
    private static final Map<View,Boolean> APPLIED=Collections.synchronizedMap(new WeakHashMap<>());

    private PremiumActionPolish() {}

    public static void apply(Activity activity, View root) {
        if (activity == null || root == null) return;
        if (APPLIED.put(root, Boolean.TRUE) != null) return;
        polishTree(activity, root);
        polishNamedHeading(root, "Command center", 27f);
        polishNamedHeading(root, "Your faction hub", 27f);
    }

    private static void polishTree(Activity activity, View view) {
        if (view instanceof Button) polishButton(activity, (Button) view);
        if (view instanceof TextView) polishClickableText(activity, (TextView) view);
        if (view instanceof LinearLayout) polishActionCard(activity, (LinearLayout) view);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) polishTree(activity, group.getChildAt(i));
        }
    }

    private static void polishButton(Activity activity, Button button) {
        button.setAllCaps(false);
        button.setTextSize(13f);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(activity, 46));
        button.setPadding(dp(activity, 14), 0, dp(activity, 14), 0);
        if (Build.VERSION.SDK_INT >= 21 && button.getElevation() < dp(activity, 2)) button.setElevation(dp(activity, 2));
    }

    private static void polishClickableText(Activity activity, TextView text) {
        if (!text.isClickable()) return;
        String raw = value(text);
        if (raw.toUpperCase(Locale.US).contains("OPEN WAR CENTER")) {
            int accent = text.getCurrentTextColor();
            text.setText("Open War Center  →");
            text.setTextSize(13f);
            text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            text.setGravity(Gravity.CENTER);
            text.setMinHeight(dp(activity, 48));
            text.setPadding(dp(activity, 14), 0, dp(activity, 14), 0);
            text.setBackground(actionBackground(activity, accent, 13));
        }
    }

    private static void polishActionCard(Activity activity, LinearLayout card) {
        if (!card.isClickable() || card.getChildCount() < 2) return;
        Object tag = card.getTag();
        if (tag instanceof String && ((String) tag).startsWith("nav:")) return;

        List<TextView> texts = directTextChildren(card);
        if (texts.size() < 2) return;
        TextView eyebrow = texts.get(0);
        if (!looksLikeEyebrow(value(eyebrow))) return;

        TextView title = texts.get(1);
        boolean compact = card.getParent() instanceof LinearLayout
                && ((LinearLayout) card.getParent()).getOrientation() == LinearLayout.HORIZONTAL;
        int accent = eyebrow.getCurrentTextColor();

        eyebrow.setTextSize(10f);
        eyebrow.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        if (Build.VERSION.SDK_INT >= 21) eyebrow.setLetterSpacing(.11f);

        String cleaned = cleanTitle(value(title));
        if (!cleaned.equals(value(title))) title.setText(cleaned);
        title.setTextColor(TEXT);
        title.setTextSize(compact ? 16.5f : 18f);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setMaxLines(2);
        title.setLineSpacing(0f, 1.04f);

        TextView body = null;
        TextView action = findActionLabel(card);
        if (texts.size() >= 3) {
            TextView third = texts.get(2);
            String thirdValue = value(third);
            if (thirdValue.toLowerCase(Locale.US).contains("tap to open") || ACTION_TAG.equals(third.getTag())) {
                action = third;
            } else {
                body = third;
            }
        }

        if (body != null) {
            body.setTextColor(MUTED);
            body.setTextSize(12f);
            body.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            body.setMaxLines(2);
            body.setLineSpacing(0f, 1.08f);
            ViewGroup.LayoutParams raw = body.getLayoutParams();
            if (raw instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) raw;
                lp.topMargin = dp(activity, 4);
                body.setLayoutParams(lp);
            }
        }

        if (action == null) {
            action = new TextView(activity);
            action.setTag(ACTION_TAG);
            card.addView(action, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        action.setTag(ACTION_TAG);
        action.setText(actionLabel(value(eyebrow), value(title)));
        action.setTextColor(accent);
        action.setTextSize(11.5f);
        action.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        action.setGravity(Gravity.START);
        ViewGroup.LayoutParams rawAction = action.getLayoutParams();
        if (rawAction instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) rawAction;
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.topMargin = dp(activity, compact ? 7 : 6);
            action.setLayoutParams(lp);
        }

        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(activity, 16), dp(activity, 13), dp(activity, 16), dp(activity, 13));
        card.setBackground(actionBackground(activity, accent, 20));
        if (Build.VERSION.SDK_INT >= 21 && card.getElevation() < dp(activity, 3)) card.setElevation(dp(activity, 3));

        ViewGroup.LayoutParams rawCard = card.getLayoutParams();
        if (rawCard instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) rawCard;
            int minimum = dp(activity, compact ? 140 : 124);
            if (lp.height > 0 && lp.height < minimum) lp.height = minimum;
            card.setLayoutParams(lp);
        }
        card.setContentDescription(value(title) + ". " + value(action).replace("→", ""));
    }

    private static GradientDrawable actionBackground(Activity activity, int accent, int radius) {
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{SURFACE, SURFACE_2});
        bg.setCornerRadius(dp(activity, radius));
        bg.setStroke(dp(activity, 1), Color.argb(170, Color.red(accent), Color.green(accent), Color.blue(accent)));
        return bg;
    }

    private static List<TextView> directTextChildren(LinearLayout card) {
        List<TextView> out = new ArrayList<>();
        for (int i = 0; i < card.getChildCount(); i++) {
            View child = card.getChildAt(i);
            if (child instanceof TextView && !(child instanceof Button)) out.add((TextView) child);
        }
        return out;
    }

    private static TextView findActionLabel(LinearLayout card) {
        for (int i = 0; i < card.getChildCount(); i++) {
            View child = card.getChildAt(i);
            if (child instanceof TextView && ACTION_TAG.equals(child.getTag())) return (TextView) child;
        }
        return null;
    }

    private static String cleanTitle(String title) {
        if ("Current chain readiness".equals(title)) return "Chain readiness";
        if ("War, OC and personal faction tasks".equals(title)) return "Daily obligations";
        if ("Who needs attention right now".equals(title)) return "Needs attention";
        if ("Participation command".equals(title)) return "War participation";
        return title;
    }

    private static String actionLabel(String eye, String title) {
        String key = (eye + " " + title).toLowerCase(Locale.US);
        if (key.contains("activity tracker") || key.contains("activity")) return "Open tracker  →";
        if (key.contains("faction pulse") || key.contains("readiness")) return "View readiness  →";
        if (key.contains("dossier") || key.contains("member intelligence")) return "Search members  →";
        if (key.contains("attention")) return "Review now  →";
        if (key.contains("digest")) return "Review digest  →";
        if (key.contains("chain")) return "View chain  →";
        if (key.contains("oc")) return "Manage OC  →";
        if (key.contains("war") || key.contains("participation")) return "Open war tools  →";
        if (key.contains("armory")) return "Open auditor  →";
        if (key.contains("bank")) return "Open banking  →";
        if (key.contains("control") || key.contains("admin")) return "Open controls  →";
        if (key.contains("intel") || key.contains("strength")) return "View intelligence  →";
        if (key.contains("status") || key.contains("personal") || key.contains("obligation")) return "Review  →";
        return "Open  →";
    }

    private static void polishNamedHeading(View root, String exact, float size) {
        TextView view = findText(root, exact);
        if (view == null) return;
        view.setTextSize(size);
        view.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        view.setLineSpacing(0f, 1.02f);
    }

    private static TextView findText(View root, String exact) {
        if (root instanceof TextView) {
            TextView t = (TextView) root;
            if (exact.equals(value(t))) return t;
        }
        if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) {
                TextView found = findText(g.getChildAt(i), exact);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static boolean looksLikeEyebrow(String value) {
        if (value == null || value.length() < 2 || value.length() > 55) return false;
        boolean letter = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isLetter(c)) {
                letter = true;
                if (Character.isLowerCase(c)) return false;
            }
        }
        return letter;
    }

    private static String value(TextView text) {
        CharSequence raw = text.getText();
        return raw == null ? "" : raw.toString().trim();
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
