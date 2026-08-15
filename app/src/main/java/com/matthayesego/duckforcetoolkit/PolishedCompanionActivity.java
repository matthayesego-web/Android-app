package com.matthayesego.duckforcetoolkit;

import android.content.Intent;
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

/** Presentation layer for the compact quick-access dashboard and companion visual polish. */
public class PolishedCompanionActivity extends CompanionActivity {
    private static final int BG = Color.rgb(6, 9, 13);
    private static final int SURFACE = Color.rgb(15, 20, 28);
    private static final int SURFACE_2 = Color.rgb(10, 15, 22);
    private static final int BORDER = Color.rgb(45, 55, 69);
    private static final int TEXT = Color.rgb(244, 246, 249);
    private static final int MUTED = Color.rgb(154, 164, 178);
    private static final int GOLD_LIGHT = Color.rgb(241, 194, 106);
    private static final int BLUE = Color.rgb(88, 166, 255);
    private static final int GREEN = Color.rgb(63, 185, 80);

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private GradientDrawable rounded(int fill, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill); d.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke);
        return d;
    }

    private GradientDrawable goldButton() {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(239, 193, 103), Color.rgb(202, 143, 48)});
        d.setCornerRadius(dp(13));
        return d;
    }

    @Override public void setContentView(View view) {
        if (containsText(view, "Connect your Torn account")) prepareLogin(view);
        if (containsText(view, "Welcome back,")) {
            attachFactionIntelligence(view);
            retargetDeveloperConsole(view);
        }
        polishTree(view);
        stampText(view);
        if (containsText(view, "Welcome back,")) compactKnownHomeCards(view);
        super.setContentView(view);
    }

    private boolean containsText(View view, String needle) {
        if (view instanceof TextView) {
            CharSequence raw = ((TextView) view).getText();
            if (raw != null && raw.toString().contains(needle)) return true;
        }
        if (view instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) view;
            for (int i = 0; i < g.getChildCount(); i++) if (containsText(g.getChildAt(i), needle)) return true;
        }
        return false;
    }

    private void prepareLogin(View root) {
        if (!(root instanceof ScrollView)) return;
        ScrollView scroll = (ScrollView) root;
        scroll.setBackgroundColor(BG); scroll.setClipToPadding(false);
        if (scroll.getChildCount() == 0 || !(scroll.getChildAt(0) instanceof LinearLayout)) return;
        LinearLayout column = (LinearLayout) scroll.getChildAt(0);
        if (column.getChildCount() < 3) return;

        if (column.getChildAt(0) instanceof LinearLayout) {
            LinearLayout hero = (LinearLayout) column.getChildAt(0);
            hero.setBackgroundColor(Color.TRANSPARENT); hero.setElevation(0f);
            hero.setPadding(dp(12), dp(8), dp(12), dp(14)); hero.setGravity(Gravity.CENTER_HORIZONTAL);
            replaceDuckPlaceholder(hero);
        }
        View gap = column.getChildAt(1);
        ViewGroup.LayoutParams gp = gap.getLayoutParams(); gp.height = dp(6); gap.setLayoutParams(gp);
        if (column.getChildAt(2) instanceof LinearLayout) {
            LinearLayout login = (LinearLayout) column.getChildAt(2);
            login.setBackground(rounded(SURFACE, BORDER, 18)); login.setElevation(dp(1));
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
            badge.setImageResource(R.drawable.duckforce_noir_art);
            badge.setScaleType(ImageView.ScaleType.CENTER_INSIDE); badge.setAdjustViewBounds(true);
            badge.setContentDescription("Duck Force");
            int size = dp(148);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(size, size);
            p.gravity = Gravity.CENTER_HORIZONTAL; p.bottomMargin = dp(14);
            hero.removeViewAt(i); hero.addView(badge, i, p); return;
        }
    }

    private void attachFactionIntelligence(View root) {
        if (!(root instanceof ScrollView)) return;
        ScrollView scroll = (ScrollView) root;
        if (scroll.getChildCount() == 0 || !(scroll.getChildAt(0) instanceof LinearLayout)) return;
        LinearLayout column = (LinearLayout) scroll.getChildAt(0);
        if (containsText(column, "QUICK ACCESS")) return;

        int insertAt = Math.min(4, column.getChildCount());
        TextView section = sectionLabel("QUICK ACCESS");
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sectionParams.bottomMargin = dp(8); column.addView(section, insertAt++, sectionParams);

        LinearLayout grid = new LinearLayout(this); grid.setOrientation(LinearLayout.VERTICAL);
        boolean activity = DeveloperSettings.featureEnabled(this, DeveloperSettings.FEATURE_ACTIVITY);
        boolean war = DeveloperSettings.featureEnabled(this, DeveloperSettings.FEATURE_WAR);
        boolean chain = DeveloperSettings.featureEnabled(this, DeveloperSettings.FEATURE_CHAIN);
        boolean oc = DeveloperSettings.featureEnabled(this, DeveloperSettings.FEATURE_OC);
        boolean pulse = DeveloperSettings.featureEnabled(this, DeveloperSettings.FEATURE_PULSE);
        boolean lookup = DeveloperSettings.featureEnabled(this, DeveloperSettings.FEATURE_LOOKUP);

        addPair(grid,
                activity ? featureTile("📊 Activity", "30-day participation", FeatureRouterActivity.TARGET_ACTIVITY, BLUE) : null,
                war ? featureTile("⚔ War", "Live participation", FeatureRouterActivity.TARGET_WAR, GOLD_LIGHT) : null);
        addPair(grid,
                chain ? featureTile("⛓ Chain", "Status & readiness", FeatureRouterActivity.TARGET_CHAIN, GREEN) : null,
                oc ? featureTile("🧩 OC Tracker", "Open • plan • complete", FeatureRouterActivity.TARGET_OC, BLUE) : null);
        addPair(grid,
                pulse ? featureTile("◉ Faction Pulse", "Health at a glance", FeatureRouterActivity.TARGET_PULSE, GREEN) : null,
                lookup ? featureTile("⌕ Member Lookup", "Find anyone fast", FeatureRouterActivity.TARGET_LOOKUP, BLUE) : null);

        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gp.bottomMargin = dp(10); column.addView(grid, insertAt++, gp);

        if (DeveloperSettings.featureEnabled(this, DeveloperSettings.FEATURE_PREMIUM_PREVIEW)) {
            TextView premium = sectionLabel("PREMIUM PREVIEW"); premium.setTextColor(GOLD_LIGHT);
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pp.topMargin = dp(2); pp.bottomMargin = dp(8); column.addView(premium, insertAt++, pp);
            LinearLayout premiumGrid = new LinearLayout(this); premiumGrid.setOrientation(LinearLayout.VERTICAL);
            addPair(premiumGrid, premiumTile("🔔 Smart Alerts", "Locked • planned"), premiumTile("◆ Advanced Intel", "Locked • planned"));
            LinearLayout.LayoutParams pgp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pgp.bottomMargin = dp(8); column.addView(premiumGrid, insertAt++, pgp);
        }
    }

    private TextView sectionLabel(String value) {
        TextView section = new TextView(this);
        section.setText(value); section.setTextColor(MUTED); section.setTextSize(12);
        section.setTypeface(Typeface.DEFAULT, Typeface.BOLD); section.setLetterSpacing(.08f);
        return section;
    }

    private void addPair(LinearLayout grid, View left, View right) {
        if (left == null && right == null) return;
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        if (left != null) row.addView(left, tileParams(false));
        if (right != null) row.addView(right, tileParams(left != null));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.bottomMargin = dp(8); grid.addView(row, rp);
    }

    private LinearLayout.LayoutParams tileParams(boolean withLeftMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(92), 1f);
        if (withLeftMargin) p.leftMargin = dp(8);
        return p;
    }

    private LinearLayout featureTile(String title, String body, String target, int stroke) {
        LinearLayout tile = tileBase(title, body, stroke);
        tile.setOnClickListener(v -> openFeature(target));
        return tile;
    }

    private LinearLayout premiumTile(String title, String body) {
        LinearLayout tile = tileBase(title, body, GOLD_LIGHT);
        tile.setOnClickListener(v -> startActivity(new Intent(this, PremiumPreviewActivity.class)));
        return tile;
    }

    private LinearLayout tileBase(String title, String body, int stroke) {
        LinearLayout tile = new LinearLayout(this); tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_VERTICAL); tile.setPadding(dp(13), dp(10), dp(13), dp(10));
        tile.setBackground(rounded(SURFACE, stroke, 15)); tile.setClickable(true); tile.setFocusable(true);
        TextView heading = new TextView(this); heading.setText(title); heading.setTextColor(TEXT); heading.setTextSize(15); heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD); tile.addView(heading);
        TextView description = new TextView(this); description.setText(body); description.setTextColor(MUTED); description.setTextSize(11.5f); description.setMaxLines(2);
        LinearLayout.LayoutParams dpv = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); dpv.topMargin=dp(4); tile.addView(description, dpv);
        return tile;
    }

    private void openFeature(String target) {
        Intent i = new Intent(this, FeatureRouterActivity.class); i.putExtra(FeatureRouterActivity.EXTRA_TARGET, target); startActivity(i);
    }

    private boolean retargetDeveloperConsole(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) view;
            for (int i = 0; i < g.getChildCount(); i++) if (retargetDeveloperConsole(g.getChildAt(i))) return true;
            if (view instanceof LinearLayout && containsText(view, "🛠 Developer Console")) {
                view.setClickable(true); view.setOnClickListener(v -> openFeature(FeatureRouterActivity.TARGET_DEVELOPER)); return true;
            }
        }
        return false;
    }

    private void compactKnownHomeCards(View view) {
        if (view instanceof LinearLayout) {
            LinearLayout l = (LinearLayout) view;
            String title = directTitle(l);
            String[] known={"BANKING","ARMORY AUDITOR","LEADERSHIP CONTROLS","COMPANY TRAINING CALCULATOR","DEVELOPER CONSOLE"};
            for(String k:known) if(k.equals(title)){l.setPadding(dp(14),dp(11),dp(14),dp(11));break;}
        }
        if (view instanceof ViewGroup) {
            ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)compactKnownHomeCards(g.getChildAt(i));
        }
    }

    private String directTitle(LinearLayout layout) {
        for(int i=0;i<layout.getChildCount();i++){View child=layout.getChildAt(i);if(child instanceof TextView){CharSequence raw=((TextView)child).getText();if(raw!=null)return raw.toString();}}
        return "";
    }

    private void polishTree(View view) {
        if (view instanceof Button) {
            Button b = (Button) view; b.setAllCaps(false);
            b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            b.setLetterSpacing(0.01f); b.setStateListAnimator(null);
            CharSequence raw = b.getText();
            if (raw != null && raw.toString().contains("Connect to Duck Force")) {
                b.setTextColor(Color.rgb(24,17,8)); b.setTextSize(15); b.setBackground(goldButton());
            }
        }
        if (view instanceof EditText) {
            EditText f = (EditText) view; f.setTextColor(TEXT); f.setHintTextColor(Color.rgb(127,138,153));
            f.setTextSize(16); f.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            f.setBackground(rounded(SURFACE_2, Color.rgb(54,65,80), 13)); f.setPadding(dp(16),0,dp(16),0);
        }
        if (view instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) view;
            for (int i = 0; i < g.getChildCount(); i++) polishTree(g.getChildAt(i));
        }
    }

    private void stampText(View view) {
        if (view instanceof TextView) {
            TextView t = (TextView) view; CharSequence raw = t.getText();
            if (raw != null) {
                String v = raw.toString().replace("v0.6.0","v0.7.0").replace("v0.5.0","v0.7.0").replace("v0.4.0","v0.7.0").replace("v0.4.1","v0.7.0").replace("v0.4.2","v0.7.0").replace("v0.4.3","v0.7.0").replace("v0.4.4","v0.7.0")
                        .replace("Connect your Torn account","Sign in to Duck Force")
                        .replace("Your key verifies your identity and Duck Force membership, then stays encrypted on this device.","Use your Torn API key to verify your membership. Your key is encrypted and stored only on this device.")
                        .replace("Your Duck Force tools, requests and leadership access in one place.","Faction tools, intelligence and leadership access — wherever you play.")
                        .replace("Banking Companion — v0.4 prototype","Banking Companion — Preview")
                        .replace("🦆 Duck Force Companion","DUCK FORCE COMPANION").replace("💰 Banking","BANKING")
                        .replace("📦 Armory Auditor","ARMORY AUDITOR").replace("📦 Armory Log","ARMORY LOG").replace("💊 Faction Xanax Auditor","XANAX AUDITOR")
                        .replace("⚙️ Leadership Controls","LEADERSHIP CONTROLS").replace("🏋️ Company Train Calculator","COMPANY TRAINING CALCULATOR")
                        .replace("🛠 Developer Console","DEVELOPER CONSOLE").replace("🔐 Encrypted locally","Encrypted on this device");
                t.setText(v);
                if ("DUCK FORCE".equals(v)) { t.setTextColor(GOLD_LIGHT); t.setLetterSpacing(0.24f); t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); }
                else if ("Faction Companion".equals(v)) { t.setText("Companion"); t.setTextColor(TEXT); t.setTextSize(32); t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); t.setLetterSpacing(-0.015f); }
                if (v.contains("Encrypted on this device") || v.contains("Companion-first foundation")) t.setTextColor(MUTED);
                if (v.contains("OWNER / DEVELOPER")) t.setTextColor(GOLD_LIGHT);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) view;
            for (int i = 0; i < g.getChildCount(); i++) stampText(g.getChildAt(i));
        }
    }
}
