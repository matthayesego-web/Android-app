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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Visual-only consistency pass for TornFCA's premium shell. It standardizes action cards,
 * labels, spacing, selected navigation and dynamic page positioning without changing routing,
 * permissions, API calls or feature logic.
 */
public final class PremiumActionPolish {
    private static final String ACTION_TAG = "tornfca-premium-action-label";
    private static final int SURFACE = Color.rgb(12,18,26);
    private static final int SURFACE_2 = Color.rgb(8,13,20);
    private static final int TEXT = Color.rgb(246,248,251);
    private static final int MUTED = Color.rgb(145,155,169);
    private static final int GOLD = Color.rgb(241,190,86);
    private static final int GOLD_DARK = Color.rgb(122,84,27);
    private static final int BLUE = Color.rgb(82,153,235);
    private static final int GREEN = Color.rgb(76,190,102);
    private static final int RED = Color.rgb(239,88,82);
    private static final int PURPLE = Color.rgb(164,122,246);

    private static final Map<View,Boolean> POLISHED = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View,Boolean> OBSERVED = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View,Boolean> PAGE_RESET = Collections.synchronizedMap(new WeakHashMap<>());

    private PremiumActionPolish() {}

    public static void apply(Activity activity, View root) {
        if (activity == null || root == null) return;
        installObserver(activity, root);
        polishTree(activity, root);
        polishPageHeading(activity, root, "Command center", 27f);
        polishPageHeading(activity, root, "Your faction hub", 27f);
    }

    private static void installObserver(Activity activity, View root) {
        if (OBSERVED.put(root, Boolean.TRUE) != null) return;
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (activity.isFinishing()) return;
            try {
                polishTree(activity, root);
                polishPageHeading(activity, root, "Command center", 27f);
                polishPageHeading(activity, root, "Your faction hub", 27f);
            } catch (Exception ignored) {}
        });
    }

    private static void polishTree(Activity activity, View view) {
        if (view instanceof TextView) polishTextNode(activity, (TextView) view);
        if (view instanceof Button) polishButton(activity, (Button) view);
        if (view instanceof TextView) polishClickableText(activity, (TextView) view);
        if (view instanceof LinearLayout) {
            LinearLayout layout = (LinearLayout) view;
            if (!polishNavItem(activity, layout)) polishActionCard(activity, layout);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) polishTree(activity, group.getChildAt(i));
        }
    }

    private static void polishTextNode(Activity activity, TextView text) {
        String raw = value(text);
        if ("MY OBLIGATIONS".equals(raw)) {
            text.setText("MY DAY");
            raw = "MY DAY";
        }
        if (raw.startsWith("Duck Force Companion v") && raw.endsWith("preview")) {
            text.setText("TornFCA v0.9.19 preview");
            raw = "TornFCA v0.9.19 preview";
        }
        if (!(text instanceof Button) && looksLikeEyebrow(raw)) {
            float sp = text.getTextSize() / activity.getResources().getDisplayMetrics().scaledDensity;
            if (sp < 10f) text.setTextSize(10f);
            text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            if (Build.VERSION.SDK_INT >= 21) text.setLetterSpacing(.11f);
        }
    }

    private static void polishButton(Activity activity, Button button) {
        if (POLISHED.put(button, Boolean.TRUE) != null) return;
        button.setAllCaps(false);
        button.setTextSize(13f);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(activity, 46));
        button.setPadding(dp(activity, 14), 0, dp(activity, 14), 0);
        if (Build.VERSION.SDK_INT >= 21 && button.getElevation() < dp(activity, 2)) button.setElevation(dp(activity, 2));
    }

    private static boolean polishNavItem(Activity activity, LinearLayout item) {
        Object tag = item.getTag();
        if (!(tag instanceof String) || !((String) tag).startsWith("nav:")) return false;
        boolean selected = item.getBackground() != null;
        if (selected) {
            item.setBackground(navBackground(activity));
            for (int i = 0; i < item.getChildCount(); i++) {
                View child = item.getChildAt(i);
                if (child instanceof ImageView) ((ImageView) child).setColorFilter(GOLD);
                if (child instanceof TextView) ((TextView) child).setTextColor(GOLD);
            }
        }
        return true;
    }

    private static void polishClickableText(Activity activity, TextView text) {
        if (!text.isClickable()) return;
        String raw = value(text);
        if (!raw.toUpperCase(Locale.US).contains("OPEN WAR CENTER")) return;
        boolean first = POLISHED.put(text, Boolean.TRUE) == null;
        text.setText("Open War Center  →");
        text.setTextColor(GOLD);
        text.setBackground(actionBackground(activity, GOLD, 13));
        if (!first) return;
        text.setTextSize(13f);
        text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        text.setGravity(Gravity.CENTER);
        text.setMinHeight(dp(activity, 48));
        text.setPadding(dp(activity, 14), 0, dp(activity, 14), 0);
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
        boolean first = POLISHED.put(card, Boolean.TRUE) == null;

        String cleaned = cleanTitle(value(title));
        if (!cleaned.equals(value(title))) title.setText(cleaned);

        TextView body = null;
        TextView action = findActionLabel(card);
        texts = directTextChildren(card);
        if (texts.size() >= 3) {
            TextView third = texts.get(2);
            String thirdValue = value(third);
            if (thirdValue.toLowerCase(Locale.US).contains("tap to open") || ACTION_TAG.equals(third.getTag())) {
                action = third;
            } else {
                body = third;
            }
        }

        int accent = semanticAccent(value(eyebrow), value(title), eyebrow.getCurrentTextColor());
        eyebrow.setTextColor(accent);

        if (first) {
            eyebrow.setTextSize(10f);
            eyebrow.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            if (Build.VERSION.SDK_INT >= 21) eyebrow.setLetterSpacing(.11f);

            title.setTextColor(TEXT);
            title.setTextSize(compact ? 16.5f : 18f);
            title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            title.setMaxLines(2);
            title.setLineSpacing(0f, 1.04f);

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
        }

        if (action == null) {
            action = new TextView(activity);
            action.setTag(ACTION_TAG);
            card.addView(action, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        action.setTag(ACTION_TAG);
        action.setText(actionLabel(value(eyebrow), value(title)));
        action.setTextColor(accent);
        if (first) {
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
        }

        card.setBackground(actionBackground(activity, accent, 20));
        if (first) {
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(activity, 16), dp(activity, 13), dp(activity, 16), dp(activity, 13));
            if (Build.VERSION.SDK_INT >= 21 && card.getElevation() < dp(activity, 3)) card.setElevation(dp(activity, 3));
        }

        tuneCompactRow(activity, card, compact, body != null);
        card.setContentDescription(value(title) + ". " + value(action).replace("→", ""));
    }

    private static void tuneCompactRow(Activity activity, LinearLayout card, boolean compact, boolean hasBody) {
        if (!compact || !(card.getParent() instanceof LinearLayout)) return;
        LinearLayout row = (LinearLayout) card.getParent();
        ViewGroup.LayoutParams raw = row.getLayoutParams();
        if (!(raw instanceof LinearLayout.LayoutParams) || raw.height <= 0) return;
        int desired = dp(activity, hasBody ? 146 : 126);
        if (raw.height != desired) {
            raw.height = desired;
            row.setLayoutParams(raw);
        }
    }

    private static int semanticAccent(String eye, String title, int fallback) {
        String key = (eye + " " + title).toLowerCase(Locale.US);
        if (key.contains("leadership attention") || key.contains("priority") || key.contains("war payout")
                || key.contains("my day") || key.contains("personal") || key.contains("my status")) return GOLD;
        if (key.contains("activity tracker")) return PURPLE;
        if (key.contains("chain")) return GREEN;
        if (key.contains("faction pulse") || key.contains("readiness") || key.startsWith("participation ")) return GREEN;
        if (key.contains("armory")) return GREEN;
        if (key.startsWith("war ") || key.contains("war participation")) return RED;
        if (key.contains("member intelligence") || key.contains("dossier") || key.contains("ffscouter")
                || key.contains("intel") || key.contains("digest") || key.startsWith("oc ") || key.startsWith("my oc ")) return BLUE;
        return fallback;
    }

    private static GradientDrawable actionBackground(Activity activity, int accent, int radius) {
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{SURFACE, SURFACE_2});
        bg.setCornerRadius(dp(activity, radius));
        bg.setStroke(dp(activity, 1), Color.argb(185, Color.red(accent), Color.green(accent), Color.blue(accent)));
        return bg;
    }

    private static GradientDrawable navBackground(Activity activity) {
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(45,34,17), Color.rgb(18,20,22)});
        bg.setCornerRadius(dp(activity, 16));
        bg.setStroke(dp(activity, 1), GOLD_DARK);
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
        if ("War, OC and personal faction tasks".equals(title) || "Daily obligations".equals(title)) return "Daily readiness";
        if ("Who needs attention right now".equals(title)) return "Needs attention";
        if ("Participation command".equals(title)) return "War participation";
        if ("Assignment and readiness".equals(title)) return "OC assignment";
        if ("My ranked-war activity".equals(title)) return "My war activity";
        return title;
    }

    private static String actionLabel(String eye, String title) {
        String key = (eye + " " + title).toLowerCase(Locale.US);
        if (key.contains("war payout") || key.contains("payout calculator")) return "Open payout calculator  →";
        if (key.contains("my day") || key.contains("daily readiness") || key.contains("obligation")) return "Open My Day  →";
        if (key.contains("chain")) return "View chain  →";
        if (key.startsWith("my oc ") || key.startsWith("oc ") || key.contains("oc assignment")) return "Manage OC  →";
        if (key.startsWith("participation ") || key.contains("my war activity")) return "View war activity  →";
        if (key.contains("activity tracker")) return "Open tracker  →";
        if (key.contains("faction pulse") || key.contains("readiness")) return "View readiness  →";
        if (key.contains("dossier") || key.contains("member intelligence")) return "Search members  →";
        if (key.contains("attention")) return "Review now  →";
        if (key.contains("digest")) return "Review digest  →";
        if (key.startsWith("war ") || key.contains("war participation")) return "Open war tools  →";
        if (key.contains("armory")) return "Open auditor  →";
        if (key.contains("bank")) return "Open banking  →";
        if (key.contains("control") || key.contains("admin")) return "Open controls  →";
        if (key.contains("intel") || key.contains("strength")) return "View intelligence  →";
        if (key.contains("status") || key.contains("personal")) return "Review  →";
        return "Open  →";
    }

    private static void polishPageHeading(Activity activity, View root, String exact, float size) {
        TextView view = findText(root, exact);
        if (view == null) return;
        if (POLISHED.put(view, Boolean.TRUE) == null) {
            view.setTextSize(size);
            view.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            view.setLineSpacing(0f, 1.02f);
            if (view.getParent() instanceof LinearLayout) {
                LinearLayout header = (LinearLayout) view.getParent();
                header.setPadding(header.getPaddingLeft(), dp(activity, 8), header.getPaddingRight(), dp(activity, 16));
            }
        }
        if (PAGE_RESET.put(view, Boolean.TRUE) == null) {
            ScrollView scroll = findAncestorScroll(view);
            if (scroll != null) scroll.post(() -> scroll.scrollTo(0, 0));
        }
    }

    private static ScrollView findAncestorScroll(View start) {
        View current = start;
        while (current != null) {
            if (current instanceof ScrollView) return (ScrollView) current;
            if (!(current.getParent() instanceof View)) break;
            current = (View) current.getParent();
        }
        return null;
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
