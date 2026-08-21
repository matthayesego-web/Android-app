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

/** v0.8 presentation shell: Home / Faction / Leadership over the working v0.7 feature layer. */
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

    private static final int TAB_HOME = 0;
    private static final int TAB_FACTION = 1;
    private static final int TAB_LEADERSHIP = 2;

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
        boolean login = containsText(view, "Connect your Torn account") || containsText(view, "Sign in to Duck Force");
        boolean home = containsText(view, "Welcome back,");
        if (login) prepareLogin(view);
        polishTree(view);
        stampText(view);
        if (home) installV080Shell(view);
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

    private void installV080Shell(View root) {
        if (!(root instanceof ScrollView)) return;
        ScrollView scroll = (ScrollView) root;
        if (scroll.getChildCount() == 0 || !(scroll.getChildAt(0) instanceof LinearLayout)) return;
        LinearLayout column = (LinearLayout) scroll.getChildAt(0);
        if ("v080-shell".equals(column.getTag())) return;

        ShellRefs refs = new ShellRefs();
        refs.column = column;
        refs.hero = findDirect(column, "Welcome back,");
        refs.war = findDirect(column, "WAR STATUS");
        refs.banking = findDirect(column, "BANKING");
        refs.armory = findDirect(column, "ARMORY AUDITOR");
        refs.leadership = findDirect(column, "LEADERSHIP CONTROLS");
        refs.train = findDirect(column, "COMPANY TRAINING CALCULATOR");
        refs.developer = findDirect(column, "DEVELOPER CONSOLE");
        refs.owner = refs.developer != null;
        refs.leadershipVisible = refs.owner || refs.leadership != null || refs.armory != null;
        if (refs.hero == null) return;

        column.setTag("v080-shell");
        renderShell(refs, TAB_HOME);
    }

    private View findDirect(LinearLayout parent, String needle) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (containsText(child, needle)) return child;
        }
        return null;
    }

    private void renderShell(ShellRefs refs, int selected) {
        LinearLayout column = refs.column;
        column.removeAllViews();
        addDetached(column, refs.hero, false);

        LinearLayout nav = new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(0, dp(12), 0, dp(12));
        nav.addView(navButton("Home", selected == TAB_HOME, () -> renderShell(refs, TAB_HOME)), navParams(false));
        nav.addView(navButton("Faction", selected == TAB_FACTION, () -> renderShell(refs, TAB_FACTION)), navParams(true));
        if (refs.leadershipVisible) nav.addView(navButton("Leadership", selected == TAB_LEADERSHIP, () -> renderShell(refs, TAB_LEADERSHIP)), navParams(true));
        column.addView(nav, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60)));

        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        if (selected == TAB_FACTION) renderFaction(refs, content);
        else if (selected == TAB_LEADERSHIP && refs.leadershipVisible) renderLeadership(refs, content);
        else renderHome(refs, content);
        column.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView footer = new TextView(this);
        footer.setText("TornFCA v"+TornFcaBrand.VERSION+" • faction operating layer");
        footer.setTextColor(MUTED); footer.setTextSize(11); footer.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fp.topMargin = dp(6); fp.bottomMargin = dp(4); column.addView(footer, fp);
    }

    private void renderHome(ShellRefs refs, LinearLayout content) {
        addSection(content, "WHAT NEEDS MY ATTENTION?");
        if (refs.war != null) addDetached(content, refs.war, true);
        addPair(content,
                featureTile("My OC", "My assignment, slot, CPR and item readiness", () -> openMember(MemberFactionActivity.MODE_OC), BLUE),
                featureTile("My War", "My ranked-war hits and current war status", () -> openMember(MemberFactionActivity.MODE_PARTICIPATION), GOLD_LIGHT));
        addPair(content,
                featureTile("Chain", "Current faction chain status", () -> openFeature(FeatureRouterActivity.TARGET_CHAIN), GREEN),
                featureTile("My Obligations", "What needs my action right now", () -> openMember(MemberFactionActivity.MODE_OVERVIEW), GOLD_LIGHT));
        LinearLayout digest = tileBase("While You Were Away", "Live/session digest now; durable cross-device history comes with the shared backend.", BLUE);
        digest.setOnClickListener(v -> openMember(MemberFactionActivity.MODE_OVERVIEW));
        addFull(content, digest);
    }

    private void renderFaction(ShellRefs refs, LinearLayout content) {
        addSection(content, "FACTION OVERVIEW");
        if (refs.war != null) addDetached(content, refs.war, true);
        addPair(content,
                featureTile("My Participation", "My own ranked-war activity only", () -> openMember(MemberFactionActivity.MODE_PARTICIPATION), BLUE),
                featureTile("My OC", "My own organized-crime details only", () -> openMember(MemberFactionActivity.MODE_OC), GREEN));
        addPair(content,
                featureTile("Intel", "Search the current Duck Force directory", () -> openFeature(FeatureRouterActivity.TARGET_LOOKUP), BLUE),
                featureTile("Chain Status", "Faction chain status and readiness", () -> openFeature(FeatureRouterActivity.TARGET_CHAIN), GREEN));

        addSection(content, "SHARED TOOLS");
        if (refs.banking != null) addDetached(content, refs.banking, true);
    }

    private void renderLeadership(ShellRefs refs, LinearLayout content) {
        addSection(content, "MEMBERS");
        addPair(content,
                featureTile("Activity Tracker", "Faction-wide participation scan", () -> openFeature(FeatureRouterActivity.TARGET_ACTIVITY), BLUE),
                featureTile("Faction Pulse", "Member health and availability", () -> openFeature(FeatureRouterActivity.TARGET_PULSE), GREEN));
        addFull(content, featureTile("Member Lookup", "Full leadership lookup from the current faction roster", () -> openFeature(FeatureRouterActivity.TARGET_LOOKUP), BLUE));

        addSection(content, "WAR");
        addPair(content,
                featureTile("War Participation", "Live or latest ranked-war participation", () -> openFeature(FeatureRouterActivity.TARGET_WAR), GOLD_LIGHT),
                featureTile("Chain Command", "Chain status and readiness", () -> openFeature(FeatureRouterActivity.TARGET_CHAIN), GREEN));

        addSection(content, "OC");
        addFull(content, featureTile("OC Management", "Open, planning and completed organized crimes", () -> openFeature(FeatureRouterActivity.TARGET_OC), BLUE));

        addSection(content, "OPERATIONS");
        if (refs.banking != null) addDetached(content, refs.banking, true);
        if (refs.armory != null) addDetached(content, refs.armory, true);
        if (refs.leadership != null) addDetached(content, refs.leadership, true);

        if (refs.owner) {
            addSection(content, "OWNER / DEVELOPER");
            if (refs.train != null) addDetached(content, refs.train, true);
            if (refs.developer != null) {
                refs.developer.setClickable(true);
                refs.developer.setOnClickListener(v -> openFeature(FeatureRouterActivity.TARGET_DEVELOPER));
                addDetached(content, refs.developer, true);
            }
        }
    }

    private void addSection(LinearLayout parent, String value) {
        TextView section = sectionLabel(value);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(4); p.bottomMargin = dp(8); parent.addView(section, p);
    }

    private Button navButton(String label, boolean selected, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(13);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); b.setStateListAnimator(null);
        b.setTextColor(selected ? Color.rgb(24,17,8) : TEXT);
        b.setBackground(selected ? goldButton() : rounded(SURFACE, BORDER, 13));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private LinearLayout.LayoutParams navParams(boolean withMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1f);
        if (withMargin) p.leftMargin = dp(7); return p;
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

    private void addFull(LinearLayout parent, View child) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(92));
        p.bottomMargin = dp(9); parent.addView(child, p);
    }

    private LinearLayout.LayoutParams tileParams(boolean withLeftMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(92), 1f);
        if (withLeftMargin) p.leftMargin = dp(8);
        return p;
    }

    private LinearLayout featureTile(String title, String body, Runnable action, int stroke) {
        LinearLayout tile = tileBase(title, body, stroke);
        tile.setOnClickListener(v -> action.run());
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

    private void addDetached(LinearLayout parent, View child, boolean marginBottom) {
        if (child == null) return;
        ViewGroup old = child.getParent() instanceof ViewGroup ? (ViewGroup) child.getParent() : null;
        if (old != null) old.removeView(child);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (marginBottom) p.bottomMargin = dp(10); parent.addView(child, p);
    }

    private void openFeature(String target) {
        Intent i = new Intent(this, FeatureRouterActivity.class); i.putExtra(FeatureRouterActivity.EXTRA_TARGET, target); startActivity(i);
    }

    private void openMember(String mode) {
        Intent i = new Intent(this, MemberFactionActivity.class); i.putExtra(MemberFactionActivity.EXTRA_MODE, mode); startActivity(i);
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
                String v = raw.toString().replace("v0.7.0","v0.8.0").replace("v0.6.0","v0.8.0").replace("v0.5.0","v0.8.0").replace("v0.4.0","v0.8.0").replace("v0.4.1","v0.8.0").replace("v0.4.2","v0.8.0").replace("v0.4.3","v0.8.0").replace("v0.4.4","v0.8.0")
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

    private static final class ShellRefs {
        LinearLayout column;
        View hero;
        View war;
        View banking;
        View armory;
        View leadership;
        View train;
        View developer;
        boolean owner;
        boolean leadershipVisible;
    }
}
